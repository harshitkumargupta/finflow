package com.finflow.service;

import org.springframework.data.domain.Pageable;
import com.finflow.dto.PageResponseMapper;
import com.finflow.dto.PageResponse;
import com.finflow.dto.*;
import com.finflow.entity.*;
import com.finflow.exception.*;
import com.finflow.payment.PaymentProvider;
import com.finflow.payment.PaymentResult;
import com.finflow.payment.PaymentStatus;
import com.finflow.repository.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentDomainService {

    private final CurrentUserService currentUserService;
    private final PinVerificationService pinVerificationService;
    private final MoneyNormalizer moneyNormalizer;
    private final PaymentRepository paymentRepository;
    private final PaymentAttemptRepository attemptRepository;
    private final PaymentWebhookEventRepository webhookEventRepository;
    private final ReconciliationRecordRepository reconciliationRecordRepository;
    private final AccountRepository accountRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final WalletPaymentRepository walletPaymentRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final FinanceNotificationRepository notificationRepository;
    private final SecurityAuditService auditService;
    private final PaymentProvider paymentProvider;

    public PaymentDomainService(
            CurrentUserService currentUserService,
            PinVerificationService pinVerificationService,
            MoneyNormalizer moneyNormalizer,
            PaymentRepository paymentRepository,
            PaymentAttemptRepository attemptRepository,
            PaymentWebhookEventRepository webhookEventRepository,
            ReconciliationRecordRepository reconciliationRecordRepository,
            AccountRepository accountRepository,
            WalletRepository walletRepository,
            WalletTransactionRepository walletTransactionRepository,
            WalletPaymentRepository walletPaymentRepository,
            TransactionRepository transactionRepository,
            UserRepository userRepository,
            FinanceNotificationRepository notificationRepository,
            SecurityAuditService auditService,
            PaymentProvider paymentProvider) {

        this.currentUserService = currentUserService;
        this.pinVerificationService = pinVerificationService;
        this.moneyNormalizer = moneyNormalizer;
        this.paymentRepository = paymentRepository;
        this.attemptRepository = attemptRepository;
        this.webhookEventRepository = webhookEventRepository;
        this.reconciliationRecordRepository = reconciliationRecordRepository;
        this.accountRepository = accountRepository;
        this.walletRepository = walletRepository;
        this.walletTransactionRepository = walletTransactionRepository;
        this.walletPaymentRepository = walletPaymentRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.auditService = auditService;
        this.paymentProvider = paymentProvider;
    }

    // ============================================================
    // RECIPIENT RESOLUTION
    // ============================================================

    @Transactional(readOnly = true)
    public RecipientResolutionResponse resolveRecipient(RecipientResolutionRequest request) {
        String type = request.recipientType().toUpperCase().trim();
        String identifier = request.identifier().trim();

        if ("FINFLOW_ID".equals(type) || identifier.toLowerCase().endsWith("@finflow")) {
            String cleanHandle = identifier.replace("@finflow", "").toLowerCase();
            Optional<Wallet> walletOpt = walletRepository.findByPaymentId(identifier)
                    .or(() -> walletRepository.findByWalletIdentifier(cleanHandle));

            if (walletOpt.isPresent()) {
                Wallet w = walletOpt.get();
                return new RecipientResolutionResponse(
                        true,
                        "FINFLOW_ID",
                        w.getPaymentId(),
                        w.getUser().getName(),
                        null,
                        "Verified FinFlow Wallet user",
                        true
                );
            }
            Optional<User> userOpt = userRepository.findByEmail(identifier)
                    .or(() -> userRepository.findByPhone(identifier));
            if (userOpt.isPresent()) {
                User u = userOpt.get();
                return new RecipientResolutionResponse(
                        true,
                        "FINFLOW_ID",
                        u.getEmail(),
                        u.getName(),
                        null,
                        "Verified FinFlow user account",
                        true
                );
            }
        }

        if ("UPI_ID".equals(type) || identifier.contains("@")) {
            return new RecipientResolutionResponse(
                    true,
                    "UPI_ID",
                    identifier.toLowerCase(),
                    "Sandbox VPA: " + identifier.split("@")[0].toUpperCase(),
                    null,
                    "Resolved via FinFlow UPI Sandbox Provider",
                    true
            );
        }

        if ("BANK_ACCOUNT".equals(type) || identifier.startsWith("ACC")) {
            Optional<Account> accOpt = accountRepository.findByAccountNumber(identifier);
            if (accOpt.isPresent()) {
                Account acc = accOpt.get();
                return new RecipientResolutionResponse(
                        true,
                        "BANK_ACCOUNT",
                        acc.getAccountNumber(),
                        acc.getUser().getName() + " (" + acc.getAccountType() + ")",
                        null,
                        "Internal FinFlow Bank Account",
                        true
                );
            }
            return new RecipientResolutionResponse(
                    true,
                    "BANK_ACCOUNT",
                    identifier.toUpperCase(),
                    "External Account: " + identifier,
                    null,
                    "External Bank Account (Sandbox Routing)",
                    true
            );
        }

        if ("QR_CODE".equals(type)) {
            return decodeQR(new QRDecodeRequest(identifier));
        }

        return new RecipientResolutionResponse(
                true,
                type,
                identifier,
                "Recipient " + identifier,
                null,
                "Sandbox Recipient",
                true
        );
    }

    // ============================================================
    // QR CODE UTILITIES
    // ============================================================

    @Transactional(readOnly = true)
    public QRGenerateResponse generateQR(QRGenerateRequest request) {
        User user = currentUserService.get();
        String paymentId = user.getEmail();
        BigDecimal amount = request.amount() != null ? request.amount() : BigDecimal.ZERO;
        String desc = request.description() != null ? request.description() : "FinFlow Payment Request";

        String qrPayload = String.format(
                "upi://pay?pa=%s&pn=%s&am=%s&cu=INR&tn=%s",
                paymentId,
                user.getName().replace(" ", "%20"),
                amount.stripTrailingZeros().toPlainString(),
                desc.replace(" ", "%20")
        );

        return new QRGenerateResponse(
                qrPayload,
                paymentId,
                user.getName(),
                amount,
                desc,
                true
        );
    }

    public RecipientResolutionResponse decodeQR(QRDecodeRequest request) {
        String payload = request.qrPayload().trim();
        if (payload.startsWith("upi://pay")) {
            String pa = extractQueryParam(payload, "pa");
            String pn = extractQueryParam(payload, "pn");
            String am = extractQueryParam(payload, "am");
            BigDecimal amount = null;
            if (am != null && !am.isBlank()) {
                try { amount = new BigDecimal(am); } catch (Exception ignored) {}
            }

            return new RecipientResolutionResponse(
                    true,
                    "UPI_ID",
                    pa != null ? pa : "sandbox@upi",
                    pn != null ? pn.replace("%20", " ") : "UPI Merchant",
                    amount,
                    "Decoded from UPI QR Payload (Sandbox)",
                    true
            );
        }

        return new RecipientResolutionResponse(
                true,
                "QR_CODE",
                payload,
                "Decoded QR Recipient",
                null,
                "Parsed raw QR string payload",
                true
        );
    }

    private String extractQueryParam(String url, String key) {
        try {
            int qIndex = url.indexOf('?');
            if (qIndex < 0) return null;
            String query = url.substring(qIndex + 1);
            for (String pair : query.split("&")) {
                String[] parts = pair.split("=");
                if (parts.length == 2 && parts[0].equalsIgnoreCase(key)) {
                    return parts[1];
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    // ============================================================
    // EXECUTE PAYMENT WITH IDEMPOTENCY
    // ============================================================

    @Transactional
    public PaymentDomainResponse executePayment(PaymentExecutionRequest request) {
        User user = currentUserService.get();
        pinVerificationService.verify(user, request.pin());
        BigDecimal amount = moneyNormalizer.normalizePositive(request.amount());

        // 1. Idempotency Check
        if (request.idempotencyKey() != null && !request.idempotencyKey().isBlank()) {
            Optional<Payment> existing = paymentRepository.findByIdempotencyKey(request.idempotencyKey());
            if (existing.isPresent()) {
                Payment p = existing.get();
                if (!p.getUser().getId().equals(user.getId())) {
                    throw new AccessDeniedException("Idempotency key conflict across users");
                }
                return toResponse(p);
            }
        }

        String refNumber = "FF-PAY-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        String sourceType = request.sourceType().toUpperCase().trim();
        String recipientType = request.recipientType().toUpperCase().trim();

        Payment payment = new Payment();
        payment.setReferenceNumber(refNumber);
        payment.setUser(user);
        payment.setSourceType(sourceType);
        payment.setSourceId(request.sourceId());
        payment.setRecipientType(recipientType);
        payment.setRecipientIdentifier(request.recipientIdentifier());
        payment.setRecipientName(request.recipientName() != null ? request.recipientName() : request.recipientIdentifier());
        payment.setAmount(amount);
        payment.setCurrency("INR");
        payment.setStatus("CREATED");
        payment.setCategory(request.category() != null ? request.category() : "PAYMENTS");
        payment.setDescription(request.description() != null ? request.description() : "FinFlow Sandbox Payment");
        payment.setIdempotencyKey(request.idempotencyKey());
        payment.setCreatedAt(LocalDateTime.now());

        Payment savedPayment = paymentRepository.save(payment);

        // 2. Perform Source Debit
        boolean isWalletSource = "FINFLOW_WALLET".equals(sourceType);
        Account sourceAccount = null;
        Wallet sourceWallet = null;

        if (isWalletSource) {
            sourceWallet = walletRepository.findByUserIdForUpdate(user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("FinFlow wallet not found"));
            if (sourceWallet.getBalance().compareTo(amount) < 0) {
                savedPayment.setStatus("FAILED");
                savedPayment.setFailureReason("Insufficient wallet balance");
                paymentRepository.save(savedPayment);
                throw new InsufficientBalanceException("Insufficient wallet balance for payment");
            }
            sourceWallet.setBalance(sourceWallet.getBalance().subtract(amount));
            walletRepository.save(sourceWallet);

            // Record Wallet Ledger Entry
            WalletTransaction wt = new WalletTransaction();
            wt.setWallet(sourceWallet);
            wt.setType("PAYMENT_OUT");
            wt.setAmount(amount);
            wt.setBalanceAfter(sourceWallet.getBalance());
            wt.setPaymentReference(refNumber);
            wt.setIdempotencyKey(request.idempotencyKey());
            wt.setStatus("SUCCESS");
            wt.setDescription(savedPayment.getDescription());
            wt.setCreatedAt(LocalDateTime.now());
            walletTransactionRepository.save(wt);

        } else {
            Long accId = request.sourceId();
            if (accId == null) {
                sourceAccount = accountRepository.findFirstByUserIdAndIsPrimaryTrue(user.getId())
                        .orElseGet(() -> accountRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream().findFirst()
                                .orElseThrow(() -> new ResourceNotFoundException("No valid FinFlow payment account found")));
            } else {
                sourceAccount = accountRepository.findByIdForUpdate(accId)
                        .orElseThrow(() -> new ResourceNotFoundException("Source account not found"));
            }

            if (!sourceAccount.getUser().getId().equals(user.getId())) {
                throw new AccessDeniedException("You are not authorized to use this source account");
            }
            if (sourceAccount.getBalance().compareTo(amount) < 0) {
                savedPayment.setStatus("FAILED");
                savedPayment.setFailureReason("Insufficient account balance");
                paymentRepository.save(savedPayment);
                throw new InsufficientBalanceException("Insufficient account balance for payment");
            }
            sourceAccount.setBalance(sourceAccount.getBalance().subtract(amount));
            accountRepository.save(sourceAccount);

            // Record Account Transaction
            Transaction tx = new Transaction();
            tx.setAccount(sourceAccount);
            tx.setAmount(amount);
            tx.setType("WITHDRAWAL");
            tx.setCategory(savedPayment.getCategory());
            tx.setDescription("Payment to " + savedPayment.getRecipientName());
            tx.setReferenceNumber(refNumber);
            tx.setIdempotencyKey(request.idempotencyKey());
            tx.setStatus("SUCCESS");
            tx.setBalanceAfter(sourceAccount.getBalance());
            tx.setCreatedAt(LocalDateTime.now());
            transactionRepository.save(tx);
        }

        // 3. Execute via Payment Provider Adapter
        savedPayment.setStatus("PROCESSING");
        paymentRepository.save(savedPayment);

        PaymentResult providerResult = paymentProvider.createPayment(
                user.getEmail(),
                request.recipientIdentifier(),
                amount,
                savedPayment.getCategory(),
                savedPayment.getDescription(),
                request.idempotencyKey()
        );

        // Record Payment Attempt
        attemptRepository.save(new PaymentAttempt(
                savedPayment.getId(),
                1,
                paymentProvider.providerName(),
                providerResult.status().name(),
                "Execute " + sourceType + " -> " + recipientType,
                providerResult.message(),
                LocalDateTime.now()
        ));

        // 4. Update Payment State
        savedPayment.setStatus("SUCCESS");
        savedPayment.setExternalProviderReference(providerResult.providerReference());
        savedPayment.setCompletedAt(LocalDateTime.now());
        Payment completedPayment = paymentRepository.save(savedPayment);

        // 5. Send Notification & Log Audit
        notificationRepository.createIfAbsent(
                user.getId(),
                "PAYMENT_SUCCESS:" + completedPayment.getReferenceNumber(),
                "PAYMENT_SUCCESS",
                "Payment Completed",
                "Payment of ₹" + amount + " to " + completedPayment.getRecipientName() + " succeeded.",
                "INFO"
        );

        auditService.log(
                user,
                "PAYMENT_EXECUTE",
                "Payment/" + completedPayment.getReferenceNumber(),
                completedPayment.getReferenceNumber(),
                "MEDIUM",
                "SUCCESS",
                "Executed sandbox payment of ₹" + amount + " to " + completedPayment.getRecipientIdentifier()
        );

        // Auto Reconciliation Record
        reconciliationRecordRepository.save(new ReconciliationRecord(
                completedPayment.getReferenceNumber(),
                completedPayment.getExternalProviderReference(),
                completedPayment.getAmount(),
                completedPayment.getAmount(),
                "MATCHED",
                "Automated sandbox payment verification matched ledger.",
                LocalDateTime.now()
        ));

        return toResponse(completedPayment);
    }

    // ============================================================
    // PAYMENT HISTORY & DETAILS
    // ============================================================

    @Transactional(readOnly = true)
    public List<PaymentDomainResponse> listMyPayments() {
        User user = currentUserService.get();
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<PaymentDomainResponse> listMyPayments(Pageable pageable) {
        User user = currentUserService.get();
        return PageResponseMapper.map(paymentRepository.findByUserIdOrderByCreatedAtDescIdDesc(user.getId(), pageable), this::toResponse);
    }

    @Transactional(readOnly = true)
    public PaymentDomainResponse getPayment(Long id) {
        User user = currentUserService.get();
        Optional<Payment> paymentOpt = paymentRepository.findById(id);
        if (paymentOpt.isPresent()) {
            Payment p = paymentOpt.get();
            if (!p.getUser().getId().equals(user.getId())) {
                throw new AccessDeniedException("Not authorized to view this payment");
            }
            return toResponse(p);
        }
        WalletPayment walletPayment = walletPaymentRepository.findVisibleById(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        return toResponse(walletPayment);
    }

    // ============================================================
    // RECONCILIATION
    // ============================================================

    @Transactional
    public ReconciliationPageResponse runReconciliation(LocalDateTime from, LocalDateTime to, int page, int size) {
        Page<Payment> payments = paymentRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(from, to, PageRequest.of(page, size));
        List<ReconciliationItemResponse> items = new ArrayList<>();

        for (Payment p : payments.getContent()) {
            String status = "SUCCESS".equalsIgnoreCase(p.getStatus()) ? "MATCHED" : "PENDING_REVIEW";
            String note = "MATCHED".equals(status)
                    ? "Verified payment " + p.getReferenceNumber() + " with provider reference " + p.getExternalProviderReference()
                    : "Payment status is " + p.getStatus();

            ReconciliationRecord rec = reconciliationRecordRepository
                    .findByPaymentReference(p.getReferenceNumber())
                    .orElseGet(() -> new ReconciliationRecord(
                            p.getReferenceNumber(),
                            p.getExternalProviderReference(),
                            p.getAmount(),
                            p.getAmount(),
                            status,
                            note,
                            LocalDateTime.now()
                    ));
            rec.setStatus(status);
            rec.setNotes(note);
            rec.setReconciledAt(LocalDateTime.now());
            ReconciliationRecord saved = reconciliationRecordRepository.save(rec);
            items.add(new ReconciliationItemResponse(saved.getId(), saved.getPaymentReference(), saved.getInternalAmount(),
                    p.getStatus(), status, note, saved.getReconciledAt()));
        }

        return new ReconciliationPageResponse(items, page, size, payments.getTotalElements(), payments.getTotalPages());
    }

    private PaymentDomainResponse toResponse(Payment p) {
        return new PaymentDomainResponse(
                p.getId(),
                p.getReferenceNumber(),
                p.getExternalProviderReference(),
                p.getSourceType(),
                p.getSourceId(),
                p.getRecipientType(),
                p.getRecipientIdentifier(),
                p.getRecipientName(),
                p.getAmount(),
                p.getCurrency(),
                p.getStatus(),
                p.getCategory(),
                p.getDescription(),
                p.getFailureReason(),
                p.getIdempotencyKey(),
                true,
                p.getCreatedAt(),
                p.getCompletedAt()
        );
    }

    private PaymentDomainResponse toResponse(WalletPayment p) {
        return new PaymentDomainResponse(
                p.getId(),
                p.getReferenceNumber(),
                p.getReferenceNumber(),
                "FINFLOW_WALLET",
                p.getSenderAccount().getId(),
                "FINFLOW_ID",
                p.getReceiverPaymentId(),
                p.getReceiver().getName(),
                p.getAmount(),
                "INR",
                p.getStatus(),
                p.getCategory(),
                p.getDescription(),
                null,
                p.getIdempotencyKey(),
                true,
                p.getCreatedAt(),
                p.getCreatedAt()
        );
    }
}
