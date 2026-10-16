package com.finflow.service;

import org.springframework.data.domain.Pageable;
import com.finflow.dto.PageResponseMapper;
import com.finflow.dto.PageResponse;
import com.finflow.dto.*;
import com.finflow.entity.*;
import com.finflow.exception.IdempotencyConflictException;
import com.finflow.exception.InsufficientBalanceException;
import com.finflow.exception.InvalidTransferException;
import com.finflow.exception.ResourceNotFoundException;
import com.finflow.payment.PaymentCommand;
import com.finflow.payment.PaymentProvider;
import com.finflow.repository.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class WalletPaymentService {
    private static final String QR_PREFIX = "finflow:pay:v1:";
    private static final Pattern PAYMENT_ID =
            Pattern.compile("^[a-z0-9._-]{3,30}@finflow$");

    private final CurrentUserService currentUserService;
    private final WalletRepository walletRepository;
    private final WalletPaymentRepository paymentRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final FinanceNotificationRepository notificationRepository;
    private final PaymentProvider paymentProvider;
    private final TransactionRiskService riskService;
    private final SecurityAuditService auditService;
    private final TransactionCategorizationService categorizationService;
    private final PinVerificationService pinVerificationService;
    private final MoneyNormalizer moneyNormalizer;

    public WalletPaymentService(CurrentUserService currentUserService,
                                WalletRepository walletRepository,
                                WalletPaymentRepository paymentRepository,
                                AccountRepository accountRepository,
                                TransactionRepository transactionRepository,
                                FinanceNotificationRepository notificationRepository,
                                PaymentProvider paymentProvider,
                                TransactionRiskService riskService,
                                SecurityAuditService auditService,
                                TransactionCategorizationService categorizationService,
                                PinVerificationService pinVerificationService,
                                MoneyNormalizer moneyNormalizer) {
        this.currentUserService = currentUserService;
        this.walletRepository = walletRepository;
        this.paymentRepository = paymentRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.notificationRepository = notificationRepository;
        this.paymentProvider = paymentProvider;
        this.riskService = riskService;
        this.auditService = auditService;
        this.categorizationService = categorizationService;
        this.pinVerificationService = pinVerificationService;
        this.moneyNormalizer = moneyNormalizer;
    }

    @Transactional(readOnly = true)
    public PaymentQrResponse createQr() {
        User user = currentUserService.get();
        Wallet wallet = walletRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("FinFlow wallet not found"));
        return new PaymentQrResponse(QR_PREFIX + wallet.getPaymentId(), wallet.getPaymentId(), "FINFLOW_QR_V1");
    }

    @Transactional(readOnly = true)
    public PaymentQrScanResponse scan(PaymentQrScanRequest request) {
        return resolve(request.payload());
    }

    @Transactional(readOnly = true)
    public PaymentQrScanResponse resolve(String paymentIdOrPayload) {
        if (paymentIdOrPayload == null || paymentIdOrPayload.isBlank()) {
            throw new InvalidTransferException("Payment ID or QR payload is required");
        }
        String clean = paymentIdOrPayload.trim();
        if (clean.startsWith(QR_PREFIX)) {
            clean = clean.substring(QR_PREFIX.length());
        }
        if (!clean.contains("@")) {
            clean = clean + "@finflow";
        }
        clean = clean.toLowerCase(Locale.ROOT);

        if (!PAYMENT_ID.matcher(clean).matches()) {
            throw new InvalidTransferException("The FinFlow Payment ID format is invalid");
        }

        Wallet receiver = walletRepository.findByPaymentId(clean)
                .orElseThrow(() -> new ResourceNotFoundException("FinFlow payment identity not found"));

        User current = currentUserService.get();
        if (current != null && current.getId().equals(receiver.getUser().getId())) {
            throw new InvalidTransferException("You cannot send money to yourself.");
        }

        return new PaymentQrScanResponse(receiver.getPaymentId(), receiver.getUser().getName());
    }

    @Transactional
    public WalletPaymentResponse confirm(PaymentConfirmRequest request, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new InvalidTransferException("Idempotency-Key header is required");
        }
        if (idempotencyKey.length() > 100) {
            throw new InvalidTransferException("Idempotency-Key must not exceed 100 characters");
        }
        BigDecimal amount = moneyNormalizer.normalizePositive(request.amount());

        User sender = currentUserService.get();
        Wallet senderIdentity = walletRepository.findByUserId(sender.getId())
                .orElseThrow(() -> new ResourceNotFoundException("FinFlow wallet not found"));

        String receiverId = request.receiverPaymentId().trim().toLowerCase(Locale.ROOT);
        Wallet receiverIdentity = walletRepository.findByPaymentId(receiverId)
                .orElseThrow(() -> new ResourceNotFoundException("FinFlow payment identity not found"));

        Long firstWalletId = Math.min(senderIdentity.getId(), receiverIdentity.getId());
        Long secondWalletId = Math.max(senderIdentity.getId(), receiverIdentity.getId());
        Wallet firstWallet = walletRepository.findByIdForUpdate(firstWalletId)
                .orElseThrow(() -> new ResourceNotFoundException("FinFlow wallet not found"));
        Wallet secondWallet = walletRepository.findByIdForUpdate(secondWalletId)
                .orElseThrow(() -> new ResourceNotFoundException("FinFlow wallet not found"));
        Wallet senderWallet = firstWallet.getId().equals(senderIdentity.getId()) ? firstWallet : secondWallet;
        Wallet receiverWallet = firstWallet.getId().equals(receiverIdentity.getId()) ? firstWallet : secondWallet;

        if (sender.getId().equals(receiverWallet.getUser().getId())) {
            throw new InvalidTransferException("You cannot make a FinFlow payment to your own payment identity");
        }
        if (!"ACTIVE".equalsIgnoreCase(senderWallet.getStatus()) || !"ACTIVE".equalsIgnoreCase(receiverWallet.getStatus())) {
            throw new InvalidTransferException("The FinFlow wallet is not active");
        }

        pinVerificationService.verify(sender, request.pin());

        String categoryText = request.description() == null || request.description().isBlank()
                ? receiverWallet.getPaymentId()
                : request.description() + " " + receiverWallet.getPaymentId();
        TransactionCategorizationService.CategorySuggestion categorySuggestion =
                request.category() == null || request.category().isBlank()
                        ? categorizationService.categorize(categoryText, "TRANSFER_OUT")
                        : new TransactionCategorizationService.CategorySuggestion(
                                categorizationService.normalizeSupportedCategory(request.category()),
                                "HIGH",
                                "User selected category");
        String category = categorySuggestion.category();

        // Risk Engine Check
        TransactionRiskService.RiskAssessment assessment = riskService.evaluateTransaction(
                sender, amount, receiverWallet.getPaymentId(), category);

        if (assessment.level() == TransactionRiskService.RiskLevel.BLOCKED) {
            auditService.log(sender, "TRANSFER_BLOCKED", "PaymentIdentity/" + receiverWallet.getPaymentId(),
                    idempotencyKey, "BLOCKED", "BLOCKED", assessment.reason());
            notificationRepository.createIfAbsent(
                    sender.getId(),
                    "RISK_BLOCKED:" + System.currentTimeMillis(),
                    "TRANSACTION_BLOCKED",
                    "Transaction Blocked",
                    "Payment of ₹" + amount + " blocked: " + assessment.reason(),
                    "WARNING"
            );
            throw new InvalidTransferException("Payment blocked by FinFlow Risk Engine: " + assessment.reason());
        }

        // Idempotency check
        WalletPayment previous = paymentRepository
                .findBySender_IdAndIdempotencyKey(sender.getId(), idempotencyKey)
                .orElse(null);
        if (previous != null) {
            validateSamePayment(previous, request);
            return toResponse(previous);
        }

        Account senderAccount = null;
        if (request.senderAccountId() != null) {
            senderAccount = accountRepository.findById(request.senderAccountId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sender account not found"));
            if (!senderAccount.getUser().getId().equals(sender.getId())) {
                throw new AccessDeniedException("You are not authorized to use this account");
            }
        } else {
            // Find sender's active account for required schema relation
            senderAccount = accountRepository.findByUserIdOrderByCreatedAtDesc(sender.getId())
                    .stream()
                    .filter(a -> "ACTIVE".equalsIgnoreCase(a.getStatus()))
                    .findFirst()
                    .orElseGet(() -> {
                        Account acc = new Account();
                        acc.setUser(sender);
                        acc.setAccountNumber("ACC-" + System.currentTimeMillis());
                        acc.setAccountType("CHECKING");
                        acc.setBalance(BigDecimal.ZERO);
                        acc.setStatus("ACTIVE");
                        acc.setCreatedAt(LocalDateTime.now());
                        return accountRepository.save(acc);
                    });
        }

        Account receiverAccount = accountRepository
                .findByUserIdOrderByCreatedAtDesc(receiverWallet.getUser().getId())
                .stream()
                .filter(account -> "ACTIVE".equalsIgnoreCase(account.getStatus()))
                .findFirst()
                .orElseGet(() -> {
                    Account acc = new Account();
                    acc.setUser(receiverWallet.getUser());
                    acc.setAccountNumber("ACC-REC-" + System.currentTimeMillis());
                    acc.setAccountType("CHECKING");
                    acc.setBalance(BigDecimal.ZERO);
                    acc.setStatus("ACTIVE");
                    acc.setCreatedAt(LocalDateTime.now());
                    return accountRepository.save(acc);
                });

        String ledgerIdempotencyKey = ledgerIdempotencyKey(sender.getId(), idempotencyKey);
        
        if (request.senderAccountId() != null) {
            // Execute via account
            paymentProvider.executeInternal(new PaymentCommand(
                    senderAccount.getId(),
                    receiverAccount.getAccountNumber(),
                    amount,
                    category,
                    request.description(),
                    ledgerIdempotencyKey));
        } else {
            // Execute via Wallet balance directly
            if (senderWallet.getBalance().compareTo(amount) < 0) {
                notificationRepository.createIfAbsent(
                        sender.getId(),
                        "PAYMENT_FAILED_BALANCE:" + System.currentTimeMillis(),
                        "INSUFFICIENT_BALANCE",
                        "Payment failed",
                        "Payment of ₹" + amount + " to " + receiverWallet.getPaymentId() + " failed due to insufficient wallet balance.",
                        "WARNING"
                );
                throw new InsufficientBalanceException("Insufficient available wallet balance");
            }
            senderWallet.setBalance(senderWallet.getBalance().subtract(amount));
            receiverWallet.setBalance(receiverWallet.getBalance().add(amount));
            walletRepository.save(senderWallet);
            walletRepository.save(receiverWallet);

            // Record transaction
            Transaction tx = new Transaction();
            tx.setAccount(senderAccount);
            tx.setAmount(amount);
            tx.setType("WITHDRAWAL");
            tx.setCategory(category);
            tx.setCategoryConfidence(categorySuggestion.confidence());
            tx.setCategorySource(request.category() == null || request.category().isBlank()
                    ? "AUTO_DETECTED"
                    : "USER_SELECTED");
            tx.setCategoryReason(categorySuggestion.reason());
            tx.setDescription("Internal payment to " + receiverWallet.getPaymentId());
            tx.setReferenceNumber("FIN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase());
            tx.setStatus("SUCCESS");
            tx.setIdempotencyKey(ledgerIdempotencyKey);
            tx.setBalanceAfter(senderWallet.getBalance());
            tx.setCreatedAt(LocalDateTime.now());
            transactionRepository.save(tx);
        }

        WalletPayment concurrentRetry = paymentRepository
                .findBySender_IdAndIdempotencyKey(sender.getId(), idempotencyKey)
                .orElse(null);
        if (concurrentRetry != null) {
            validateSamePayment(concurrentRetry, request);
            return toResponse(concurrentRetry);
        }

        Transaction ledgerTransaction = transactionRepository.findByIdempotencyKey(ledgerIdempotencyKey)
                .orElseThrow(() -> new IllegalStateException(
                        "The account ledger did not persist the successful FinFlow payment"));

        String finalReferenceNumber = ledgerTransaction.getReferenceNumber().startsWith("FIN-") 
                ? ledgerTransaction.getReferenceNumber() 
                : "FIN-" + ledgerTransaction.getReferenceNumber().replace("REF-", "").replace("WLT-", "");

        WalletPayment payment = new WalletPayment();
        payment.setSender(sender);
        payment.setReceiver(receiverWallet.getUser());
        payment.setSenderAccount(senderAccount);
        payment.setSenderPaymentId(senderWallet.getPaymentId());
        payment.setReceiverPaymentId(receiverWallet.getPaymentId());
        payment.setReferenceNumber(finalReferenceNumber);
        payment.setAmount(amount);
        payment.setCategory(category);
        payment.setDescription(request.description());
        payment.setProvider(paymentProvider.providerName());
        payment.setStatus("SUCCESS");
        payment.setIdempotencyKey(idempotencyKey);
        payment.setCreatedAt(ledgerTransaction.getCreatedAt());

        WalletPayment savedPayment;
        try {
            savedPayment = paymentRepository.saveAndFlush(payment);
        } catch (DataIntegrityViolationException exception) {
            WalletPayment existing = paymentRepository
                    .findBySender_IdAndIdempotencyKey(sender.getId(), idempotencyKey)
                    .orElseThrow(() -> exception);
            validateSamePayment(existing, request);
            return toResponse(existing);
        }

        auditService.log(sender, "TRANSFER_CREATED", "WalletPayment", String.valueOf(savedPayment.getId()),
                assessment.level().name(), "SUCCESS", "Payment of ₹" + amount + " sent to " + receiverWallet.getPaymentId());

        // Send notifications
        notificationRepository.createIfAbsent(
                sender.getId(),
                "PAY_SENT:" + savedPayment.getId(),
                "PAYMENT_SENT",
                "Money Sent",
                "₹" + amount + " sent to " + receiverWallet.getPaymentId(),
                "INFO"
        );

        notificationRepository.createIfAbsent(
                receiverWallet.getUser().getId(),
                "PAY_RECV:" + savedPayment.getId(),
                "PAYMENT_RECEIVED",
                "Money Received",
                "₹" + amount + " received from " + senderWallet.getPaymentId(),
                "INFO"
        );

        return toResponse(savedPayment);
    }

    @Transactional(readOnly = true)
    public List<WalletPaymentResponse> history() {
        Long userId = currentUserService.get().getId();
        return paymentRepository.findBySender_IdOrReceiver_IdOrderByCreatedAtDesc(userId, userId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<WalletPaymentResponse> history(Pageable pageable) {
        Long userId = currentUserService.get().getId();
        return PageResponseMapper.map(paymentRepository.findBySender_IdOrReceiver_IdOrderByCreatedAtDescIdDesc(userId, userId, pageable), this::toResponse);
    }

    @Transactional(readOnly = true)
    public WalletPaymentResponse get(Long id) {
        Long userId = currentUserService.get().getId();
        return paymentRepository.findVisibleById(id, userId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("FinFlow payment not found"));
    }

    private void validateSamePayment(WalletPayment previous, PaymentConfirmRequest request) {
        if (!previous.getReceiverPaymentId().equalsIgnoreCase(request.receiverPaymentId())
                || previous.getAmount().compareTo(request.amount()) != 0
                || (request.category() != null && !previous.getCategory().equals(request.category()))
                || !Objects.equals(previous.getDescription(), request.description())) {
            throw new IdempotencyConflictException(
                    "Idempotency key was already used for a different FinFlow payment");
        }
    }

    private String ledgerIdempotencyKey(Long userId, String requestKey) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest((userId + ":" + requestKey).getBytes(StandardCharsets.UTF_8));
            return "PAY-" + HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private WalletPaymentResponse toResponse(WalletPayment payment) {
        return new WalletPaymentResponse(
                payment.getId(),
                payment.getReferenceNumber(),
                payment.getSenderPaymentId(),
                payment.getReceiverPaymentId(),
                payment.getAmount(),
                payment.getCategory(),
                payment.getDescription(),
                payment.getProvider(),
                payment.getStatus(),
                payment.getCreatedAt());
    }
}
