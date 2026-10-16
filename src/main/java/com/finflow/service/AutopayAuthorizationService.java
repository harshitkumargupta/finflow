package com.finflow.service;

import org.springframework.data.domain.Pageable;
import com.finflow.dto.PageResponseMapper;
import com.finflow.dto.PageResponse;
import com.finflow.autopay.AutopayProvider;
import com.finflow.dto.*;
import com.finflow.entity.*;
import com.finflow.exception.*;
import com.finflow.repository.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class AutopayAuthorizationService {
    private static final List<String> FREQUENCIES =
            List.of("DAILY", "WEEKLY", "MONTHLY", "QUARTERLY", "YEARLY");

    private final CurrentUserService currentUserService;
    private final AccountRepository accountRepository;
    private final CardRepository cardRepository;
    private final AutopayAuthorizationRepository authorizationRepository;
    private final AutopayExecutionRepository executionRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final ReconciliationRecordRepository reconciliationRecordRepository;
    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;
    private final FinanceNotificationRepository notificationRepository;
    private final SavingsGoalRepository savingsGoalRepository;
    private final MoneyManagementService moneyManagementService;
    private final SecurityAuditService auditService;
    private final Optional<AutopayProvider> provider;
    private final PinVerificationService pinVerificationService;

    private final MoneyNormalizer moneyNormalizer;
    private final GoalLedgerService goalLedgerService;

    public AutopayAuthorizationService(
            CurrentUserService currentUserService,
            AccountRepository accountRepository,
            CardRepository cardRepository,
            AutopayAuthorizationRepository authorizationRepository,
            AutopayExecutionRepository executionRepository,
            PaymentRepository paymentRepository,
            PaymentAttemptRepository paymentAttemptRepository,
            ReconciliationRecordRepository reconciliationRecordRepository,
            TransactionRepository transactionRepository,
            BudgetRepository budgetRepository,
            FinanceNotificationRepository notificationRepository,
            SavingsGoalRepository savingsGoalRepository,
            MoneyManagementService moneyManagementService,
            SecurityAuditService auditService,
            Optional<AutopayProvider> provider,
            PinVerificationService pinVerificationService,
            MoneyNormalizer moneyNormalizer,
            GoalLedgerService goalLedgerService) {
        this.moneyNormalizer = moneyNormalizer;
        this.goalLedgerService = goalLedgerService;
        this.currentUserService = currentUserService;
        this.accountRepository = accountRepository;
        this.cardRepository = cardRepository;
        this.authorizationRepository = authorizationRepository;
        this.executionRepository = executionRepository;
        this.paymentRepository = paymentRepository;
        this.paymentAttemptRepository = paymentAttemptRepository;
        this.reconciliationRecordRepository = reconciliationRecordRepository;
        this.transactionRepository = transactionRepository;
        this.budgetRepository = budgetRepository;
        this.notificationRepository = notificationRepository;
        this.savingsGoalRepository = savingsGoalRepository;
        this.moneyManagementService = moneyManagementService;
        this.auditService = auditService;
        this.provider = provider;
        this.pinVerificationService = pinVerificationService;
    }

    @Transactional
    public AutopayAuthorizationResponse createSavingsGoalMandate(Long goalId, GoalSavingsMandateRequest request) {
        User user = currentUserService.get();
        SavingsGoal goal = savingsGoalRepository.findByIdAndUserId(goalId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Savings goal not found"));
        Account account = accountRepository.findById(request.accountId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        if (!account.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You are not authorized to use this account");
        }
        if (!"ACTIVE".equalsIgnoreCase(account.getStatus())) {
            throw new AccountInactiveException("Account is not active");
        }
        validateRequest(request.amount(), request.amount(), request.frequency(), request.nextPaymentDate());
        BigDecimal amount = moneyNormalizer.normalizePositive(request.amount());

        var reservation = moneyManagementService.reserve(new MoneyReservationRequest(
                account.getId(), amount, "AUTOPAY",
                "Sandbox savings mandate for goal " + goal.getName()));
        AutopayProvider.Authorization created = requireProvider("FINFLOW_SANDBOX")
                .createAuthorization("Savings goal: " + goal.getName(), "FINFLOW_SANDBOX", account.getId(),
                        amount, amount, request.frequency().toUpperCase(Locale.ROOT),
                        request.nextPaymentDate());

        AutopayAuthorization authorization = new AutopayAuthorization();
        authorization.setUser(user);
        authorization.setAccount(account);
        authorization.setSavingsGoal(goal);
        authorization.setMerchantName("Savings goal: " + goal.getName());
        authorization.setProvider("FINFLOW_SANDBOX");
        authorization.setTokenReference(created.tokenReference());
        authorization.setProviderAuthorizationReference(created.providerReference());
        authorization.setAmount(amount);
        authorization.setMaximumAmount(amount);
        authorization.setFrequency(request.frequency().toUpperCase(Locale.ROOT));
        authorization.setNextPaymentDate(request.nextPaymentDate());
        authorization.setStatus("ACTIVE");
        authorization.setDescription("SANDBOX savings autopay for goal '" + goal.getName() + "'. No real bank debit is performed.");
        authorization.setReservationId(reservation.id());
        authorization.setCategory("SAVINGS");
        authorization.setCreatedAt(LocalDateTime.now());
        authorization.setUpdatedAt(LocalDateTime.now());

        AutopayAuthorization saved = authorizationRepository.save(authorization);
        auditService.log(user, "GOAL_SAVINGS_AUTOPAY_CREATED", "AutopayAuthorization/" + saved.getId(),
                saved.getProviderAuthorizationReference(), "MEDIUM", "ACTIVE",
                "Sandbox savings mandate created for goal " + goal.getName() + ".");
        notificationRepository.createIfAbsent(user.getId(), "GOAL_SAVINGS_AUTOPAY_CREATED:" + saved.getId(),
                "GOAL_SAVINGS_AUTOPAY_CREATED", "Savings autopay created",
                "Sandbox savings autopay of ₹" + saved.getAmount() + " is active for " + goal.getName() + ".",
                "INFO");
        return toResponse(saved);
    }

    @Transactional
    public AutopayAuthorizationResponse create(AutopayRequest request) {
        AutopayProvider autopayProvider = requireProvider(request.provider());
        User user = currentUserService.get();
        Account account = accountRepository.findById(request.accountId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        if (!account.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You are not authorized to use this account");
        }
        if (!"ACTIVE".equalsIgnoreCase(account.getStatus())) {
            throw new AccountInactiveException("Account is not active");
        }

        Card card = null;
        if (request.cardId() != null) {
            card = cardRepository.findByIdAndUserId(request.cardId(), user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Payment source (card) not found"));
            if ("REMOVED".equalsIgnoreCase(card.getStatus())) {
                throw new InvalidMoneyRuleException("Payment source (card) is removed");
            }
        }

        validateRequest(request.amount(), request.maximumAmount(), request.frequency(), request.nextPaymentDate());
        BigDecimal amount = moneyNormalizer.normalizePositive(request.amount());
        BigDecimal maximumAmount = moneyNormalizer.normalizePositive(request.maximumAmount());

        var reservation = moneyManagementService.reserve(new MoneyReservationRequest(
                account.getId(), maximumAmount, "AUTOPAY",
                "Maximum reserved amount for " + request.merchantName()));
        AutopayProvider.Authorization created = autopayProvider.createAuthorization(
                request.merchantName().trim(), request.provider().trim(), account.getId(),
                amount, maximumAmount, request.frequency().toUpperCase(Locale.ROOT),
                request.nextPaymentDate());
        if (!"ACTIVE".equalsIgnoreCase(created.status())
                || created.tokenReference() == null || created.tokenReference().isBlank()
                || created.providerReference() == null || created.providerReference().isBlank()) {
            moneyManagementService.releaseReservation(reservation.id());
            throw new ProviderNotConfiguredException(
                    "The payment provider did not confirm an active autopay authorization.");
        }

        AutopayAuthorization authorization = new AutopayAuthorization();
        authorization.setUser(user);
        authorization.setAccount(account);
        authorization.setCard(card);
        authorization.setMerchantName(request.merchantName().trim());
        authorization.setProvider(request.provider().trim());
        authorization.setTokenReference(created.tokenReference());
        authorization.setProviderAuthorizationReference(created.providerReference());
        authorization.setAmount(amount);
        authorization.setMaximumAmount(maximumAmount);
        authorization.setFrequency(request.frequency().toUpperCase(Locale.ROOT));
        authorization.setNextPaymentDate(request.nextPaymentDate());
        authorization.setStatus("ACTIVE");
        authorization.setDescription(request.description());
        authorization.setReservationId(reservation.id());
        authorization.setCreatedAt(LocalDateTime.now());
        authorization.setUpdatedAt(LocalDateTime.now());

        AutopayAuthorization saved = authorizationRepository.save(authorization);
        auditService.log(user, "AUTOPAY_CREATED", "AutopayAuthorization/" + saved.getId(),
                saved.getProviderAuthorizationReference(), "MEDIUM", "ACTIVE",
                "User-created sandbox autopay authorization is active.");

        notificationRepository.createIfAbsent(
                user.getId(),
                "AUTOPAY_CREATED:" + saved.getId(),
                "AUTOPAY_CREATED",
                "Autopay set up",
                "Autopay for " + saved.getMerchantName() + " (₹" + saved.getAmount() + ") configured successfully.",
                "INFO"
        );

        return toResponse(saved);
    }

    @Transactional
    public AutopayAuthorizationResponse simulateIncomingMandate(SimulateMandateRequest request) {
        BigDecimal amount = moneyNormalizer.normalizePositive(request.amount());
        BigDecimal maximumAmount = moneyNormalizer.normalizePositive(request.maximumAmount());
        User user = currentUserService.get();
        Account primaryAccount = accountRepository.findFirstByUserIdAndIsPrimaryTrue(user.getId())
                .orElseGet(() -> accountRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream().findFirst()
                        .orElseThrow(() -> new ResourceNotFoundException("User has no FinFlow account to receive mandate")));

        String provRef = "MND-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String tokenRef = "TOK-MND-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        AutopayAuthorization authorization = new AutopayAuthorization();
        authorization.setUser(user);
        authorization.setAccount(primaryAccount);
        authorization.setMerchantName(request.merchantName().trim());
        authorization.setProvider(request.provider() != null ? request.provider().trim() : "FINFLOW_SANDBOX");
        authorization.setTokenReference(tokenRef);
        authorization.setProviderAuthorizationReference(provRef);
        authorization.setAmount(amount);
        authorization.setMaximumAmount(maximumAmount);
        authorization.setFrequency(request.frequency().toUpperCase(Locale.ROOT));
        authorization.setNextPaymentDate(request.nextPaymentDate());
        authorization.setStatus("PENDING_APPROVAL");
        authorization.setDescription(request.description() != null ? request.description() : "Incoming mandate request from provider");
        authorization.setCreatedAt(LocalDateTime.now());
        authorization.setUpdatedAt(LocalDateTime.now());

        AutopayAuthorization saved = authorizationRepository.save(authorization);
        auditService.log(user, "MANDATE_RECEIVED", "AutopayAuthorization/" + saved.getId(),
                saved.getProviderAuthorizationReference(), "MEDIUM", "PENDING_APPROVAL",
                "Incoming sandbox mandate request is pending user approval.");

        notificationRepository.createIfAbsent(
                user.getId(),
                "MANDATE_RECEIVED:" + saved.getId(),
                "MANDATE_RECEIVED",
                "New Mandate Request",
                "Mandate request from " + saved.getMerchantName() + " (₹" + saved.getAmount() + ") is pending your approval.",
                "WARNING"
        );

        return toResponse(saved);
    }

    @Transactional
    public AutopayAuthorizationResponse approveMandate(Long id, AutopayApprovalRequest request) {
        AutopayAuthorization authorization = findOwned(id);
        if (!"PENDING_APPROVAL".equalsIgnoreCase(authorization.getStatus()) && !"PAUSED".equalsIgnoreCase(authorization.getStatus())) {
            throw new InvalidMoneyRuleException("Only pending approval or paused mandates can be approved/activated");
        }

        User user = currentUserService.get();
        pinVerificationService.verify(user, request.pin());
        Account targetAccount = request.accountId() != null
                ? accountRepository.findById(request.accountId()).orElseThrow(() -> new ResourceNotFoundException("Selected account not found"))
                : authorization.getAccount();

        if (!targetAccount.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You are not authorized to assign this account");
        }

        var reservation = moneyManagementService.reserve(new MoneyReservationRequest(
                targetAccount.getId(), authorization.getMaximumAmount(), "AUTOPAY",
                "Maximum reserved amount for approved mandate " + authorization.getMerchantName()));

        authorization.setAccount(targetAccount);
        authorization.setStatus("ACTIVE");
        authorization.setReservationId(reservation.id());
        authorization.setUpdatedAt(LocalDateTime.now());

        AutopayAuthorization saved = authorizationRepository.save(authorization);
        auditService.log(user, "MANDATE_APPROVED", "AutopayAuthorization/" + saved.getId(),
                saved.getProviderAuthorizationReference(), "MEDIUM", "ACTIVE",
                "Sandbox mandate approved and activated by user.");

        notificationRepository.createIfAbsent(
                user.getId(),
                "MANDATE_APPROVED:" + saved.getId(),
                "MANDATE_APPROVED",
                "Mandate Approved",
                "Mandate for " + saved.getMerchantName() + " is now ACTIVE.",
                "INFO"
        );

        return toResponse(saved);
    }

    @Transactional
    public AutopayAuthorizationResponse rejectMandate(Long id) {
        AutopayAuthorization authorization = findOwned(id);
        authorization.setStatus("REJECTED");
        authorization.setCancelledAt(LocalDateTime.now());
        authorization.setUpdatedAt(LocalDateTime.now());
        releaseReservation(authorization);

        AutopayAuthorization saved = authorizationRepository.save(authorization);
        auditService.log(authorization.getUser(), "MANDATE_REJECTED", "AutopayAuthorization/" + saved.getId(),
                saved.getProviderAuthorizationReference(), "MEDIUM", "REJECTED",
                "Sandbox mandate rejected by user.");

        notificationRepository.createIfAbsent(
                authorization.getUser().getId(),
                "MANDATE_REJECTED:" + saved.getId(),
                "MANDATE_REJECTED",
                "Mandate Rejected",
                "Mandate request for " + saved.getMerchantName() + " was rejected.",
                "INFO"
        );

        return toResponse(saved);
    }

    @Transactional
    public AutopayAuthorizationResponse edit(Long id, AutopayEditRequest request) {
        AutopayAuthorization authorization = findOwned(id);
        if ("CANCELLED".equals(authorization.getStatus())) {
            throw new InvalidMoneyRuleException("Cancelled autopay authorizations cannot be edited");
        }

        User user = currentUserService.get();
        if (request.accountId() != null) {
            Account account = accountRepository.findById(request.accountId())
                    .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
            if (!account.getUser().getId().equals(user.getId())) {
                throw new AccessDeniedException("You are not authorized to use this account");
            }
            authorization.setAccount(account);
        }

        if (request.cardId() != null) {
            Card card = cardRepository.findByIdAndUserId(request.cardId(), user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Card not found"));
            authorization.setCard(card);
        }

        validateRequest(request.amount(), request.maximumAmount(), request.frequency(), request.nextPaymentDate());
        BigDecimal amount = moneyNormalizer.normalizePositive(request.amount());
        BigDecimal maximumAmount = moneyNormalizer.normalizePositive(request.maximumAmount());

        // Update reservation if max amount changed and active
        if ("ACTIVE".equals(authorization.getStatus())) {
            releaseReservation(authorization);
            var reservation = moneyManagementService.reserve(new MoneyReservationRequest(
                    authorization.getAccount().getId(), maximumAmount, "AUTOPAY",
                    "Maximum reserved amount for " + request.merchantName()));
            authorization.setReservationId(reservation.id());
        }

        authorization.setMerchantName(request.merchantName().trim());
        authorization.setAmount(amount);
        authorization.setMaximumAmount(maximumAmount);
        authorization.setFrequency(request.frequency().toUpperCase(Locale.ROOT));
        authorization.setNextPaymentDate(request.nextPaymentDate());
        if (request.description() != null) {
            authorization.setDescription(request.description());
        }
        authorization.setUpdatedAt(LocalDateTime.now());

        return toResponse(authorizationRepository.save(authorization));
    }

    @Transactional(readOnly = true)
    public List<AutopayAuthorizationResponse> list() {
        Long userId = currentUserService.get().getId();
        return authorizationRepository.findByUserIdOrderByNextPaymentDateAsc(userId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public AutopayAuthorizationResponse get(Long id) {
        return toResponse(findOwned(id));
    }

    @Transactional
    public AutopayAuthorizationResponse pause(Long id) {
        AutopayAuthorization authorization = findOwned(id);
        if (!"ACTIVE".equals(authorization.getStatus())) {
            throw new InvalidMoneyRuleException("Only active autopay authorizations can be paused");
        }
        AutopayProvider.Authorization result = requireProvider()
                .pauseAuthorization(authorization.getProviderAuthorizationReference());
        if (!"PAUSED".equalsIgnoreCase(result.status())) {
            throw new ProviderNotConfiguredException("The provider did not confirm the paused status.");
        }
        releaseReservation(authorization);
        authorization.setStatus("PAUSED");
        authorization.setUpdatedAt(LocalDateTime.now());
        auditService.log(authorization.getUser(), "AUTOPAY_PAUSED", "AutopayAuthorization/" + authorization.getId(),
                authorization.getProviderAuthorizationReference(), "LOW", "PAUSED",
                "Autopay authorization paused.");
        
        notificationRepository.createIfAbsent(
                authorization.getUser().getId(),
                "AUTOPAY_PAUSED:" + authorization.getId() + ":" + System.currentTimeMillis(),
                "AUTOPAY_PAUSED",
                "Autopay paused",
                "Autopay authorization for " + authorization.getMerchantName() + " has been paused.",
                "INFO"
        );

        return toResponse(authorizationRepository.save(authorization));
    }

    @Transactional
    public AutopayAuthorizationResponse resume(Long id) {
        AutopayAuthorization authorization = findOwned(id);
        if (!"PAUSED".equals(authorization.getStatus())) {
            throw new InvalidMoneyRuleException("Only paused autopay authorizations can be resumed");
        }

        if (authorization.getCard() != null && "DISABLED".equalsIgnoreCase(authorization.getCard().getStatus())) {
            notificationRepository.createIfAbsent(
                    authorization.getUser().getId(),
                    "AUTOPAY_CARD_DISABLED:" + authorization.getId() + ":" + System.currentTimeMillis(),
                    "AUTOPAY_CARD_DISABLED",
                    "Payment source disabled",
                    "Cannot resume autopay for " + authorization.getMerchantName() + " because payment source is disabled.",
                    "WARNING"
            );
            throw new InvalidMoneyRuleException("Cannot resume autopay because payment source is disabled");
        }

        var reservation = moneyManagementService.reserve(new MoneyReservationRequest(
                authorization.getAccount().getId(), authorization.getMaximumAmount(), "AUTOPAY",
                "Maximum reserved amount for " + authorization.getMerchantName()));
        AutopayProvider.Authorization result = requireProvider()
                .resumeAuthorization(authorization.getProviderAuthorizationReference());
        if (!"ACTIVE".equalsIgnoreCase(result.status())) {
            moneyManagementService.releaseReservation(reservation.id());
            throw new ProviderNotConfiguredException("The provider did not confirm the active status.");
        }
        authorization.setReservationId(reservation.id());
        authorization.setStatus("ACTIVE");
        authorization.setUpdatedAt(LocalDateTime.now());
        auditService.log(authorization.getUser(), "AUTOPAY_RESUMED", "AutopayAuthorization/" + authorization.getId(),
                authorization.getProviderAuthorizationReference(), "LOW", "ACTIVE",
                "Autopay authorization resumed.");

        notificationRepository.createIfAbsent(
                authorization.getUser().getId(),
                "AUTOPAY_RESUMED:" + authorization.getId() + ":" + System.currentTimeMillis(),
                "AUTOPAY_RESUMED",
                "Autopay resumed",
                "Autopay authorization for " + authorization.getMerchantName() + " is now active.",
                "INFO"
        );

        return toResponse(authorizationRepository.save(authorization));
    }

    @Transactional
    public AutopayAuthorizationResponse cancel(Long id) {
        AutopayAuthorization authorization = findOwned(id);
        if ("CANCELLED".equals(authorization.getStatus())) {
            throw new InvalidMoneyRuleException("Autopay authorization is already cancelled");
        }
        AutopayProvider.Authorization result = requireProvider()
                .cancelAuthorization(authorization.getProviderAuthorizationReference());
        if (!"CANCELLED".equalsIgnoreCase(result.status())) {
            throw new ProviderNotConfiguredException("The provider did not confirm the cancelled status.");
        }
        releaseReservation(authorization);
        authorization.setStatus("CANCELLED");
        authorization.setCancelledAt(LocalDateTime.now());
        authorization.setUpdatedAt(LocalDateTime.now());
        auditService.log(authorization.getUser(), "AUTOPAY_CANCELLED", "AutopayAuthorization/" + authorization.getId(),
                authorization.getProviderAuthorizationReference(), "MEDIUM", "CANCELLED",
                "Autopay authorization cancelled and reservation released.");

        notificationRepository.createIfAbsent(
                authorization.getUser().getId(),
                "AUTOPAY_CANCELLED:" + authorization.getId() + ":" + System.currentTimeMillis(),
                "AUTOPAY_CANCELLED",
                "Autopay cancelled",
                "Autopay authorization for " + authorization.getMerchantName() + " was cancelled.",
                "INFO"
        );

        return toResponse(authorizationRepository.save(authorization));
    }

    @Transactional
    public void executeScheduledMandate(Long id) {
        AutopayAuthorization authorization = authorizationRepository.findByIdForUpdate(id).orElse(null);
        if (authorization == null || !"ACTIVE".equalsIgnoreCase(authorization.getStatus())) {
            return;
        }
        User user = authorization.getUser();
        BigDecimal amount = authorization.getAmount();
        String idempotencyKey = cycleIdempotencyKey(authorization);
        if (executionRepository.existsByIdempotencyKey(idempotencyKey)) {
            return;
        }

        if (amount.compareTo(authorization.getMaximumAmount()) > 0) {
            authorization.setStatus("BLOCKED");
            authorizationRepository.save(authorization);
            recordExecutionFailure(authorization, user, amount, idempotencyKey, "Amount exceeds configured maximum limit");
            notificationRepository.createIfAbsent(
                    user.getId(),
                    "AUTOPAY_BLOCKED_MAX:" + authorization.getId() + ":" + System.currentTimeMillis(),
                    "AUTOPAY_BLOCKED",
                    "Autopay Blocked",
                    "Autopay blocked because the requested amount ₹" + amount + " exceeded your ₹" + authorization.getMaximumAmount() + " safety limit.",
                    "WARNING"
            );
            return;
        }

        Account account = accountRepository.findByIdForUpdate(authorization.getAccount().getId()).orElse(null);
        if (account == null || !"ACTIVE".equalsIgnoreCase(account.getStatus())) {
            recordExecutionFailure(authorization, user, amount, idempotencyKey, "Source account is not active");
            return;
        }

        if (amount.compareTo(authorization.getMaximumAmount()) > 0) {
            recordExecutionFailure(authorization, user, amount, idempotencyKey, "Amount exceeds configured maximum limit");
            return;
        }
        // Maximum funds are already reserved for this mandate; validate against the
        // configured spending budgets without counting its own reservation twice.
        for (Budget budget : budgetRepository.findByUserIdOrderByStartDateDesc(user.getId())) {
            if ("ACTIVE".equalsIgnoreCase(budget.getStatus())
                    && ("OVERALL".equalsIgnoreCase(budget.getCategory())
                    || budget.getCategory().equalsIgnoreCase(authorization.getCategory()))
                    && !LocalDate.now().isBefore(budget.getStartDate())
                    && !LocalDate.now().isAfter(budget.getEndDate())) {
                BigDecimal spent = transactionRepository.sumExpensesForBudget(user.getId(),
                        budget.getCategory(), budget.getStartDate().atStartOfDay(),
                        budget.getEndDate().plusDays(1).atStartOfDay().minusNanos(1));
                if (spent == null) spent = BigDecimal.ZERO;
                if (spent.add(amount).compareTo(budget.getBudgetAmount()) > 0) {
                    recordExecutionFailure(authorization, user, amount,
                            idempotencyKey,
                            "Budget restriction prevented payment for category " + budget.getCategory());
                    return;
                }
            }
        }
        if (account.getBalance().compareTo(amount) < 0) {
            recordExecutionFailure(authorization, user, amount, idempotencyKey, "Insufficient account balance");
            return;
        }

        AutopayProvider.Authorization providerResult;
        try {
            providerResult = requireProvider()
                    .executePayment(authorization.getProviderAuthorizationReference(), amount);
        } catch (RuntimeException exception) {
            recordExecutionPending(authorization, user, amount, idempotencyKey,
                    "Provider response was unavailable; reconciliation is required");
            return;
        }
        if ("FAILED".equalsIgnoreCase(providerResult.status())) {
            recordExecutionFailure(authorization, user, amount, idempotencyKey,
                    "Provider did not confirm mandate execution: " + providerResult.status());
            return;
        }
        if (!"SUCCESS".equalsIgnoreCase(providerResult.status())
                && !"ACTIVE".equalsIgnoreCase(providerResult.status())) {
            recordExecutionPending(authorization, user, amount, idempotencyKey,
                    "Provider returned an unresolved execution status");
            return;
        }

        Payment payment = createAutopayPayment(authorization, user, amount, idempotencyKey);
        paymentAttemptRepository.save(new PaymentAttempt(
                payment.getId(),
                1,
                authorization.getProvider(),
                "SUCCESS",
                "Execute mandate " + authorization.getProviderAuthorizationReference(),
                "Sandbox provider confirmed mandate execution",
                LocalDateTime.now()
        ));

        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);

        Transaction tx = new Transaction();
        tx.setAccount(account);
        tx.setAmount(amount);
        tx.setType("WITHDRAWAL");
        tx.setCategory(authorization.getCategory());
        tx.setDescription("Scheduled AutoPay: " + authorization.getMerchantName());
        tx.setReferenceNumber("AUTOPAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        tx.setIdempotencyKey("AUTOPAY:" + idempotencyKey);
        tx.setStatus("SUCCESS");
        tx.setBalanceAfter(account.getBalance());
        tx.setCreatedAt(LocalDateTime.now());
        transactionRepository.save(tx);

        payment.setStatus("SUCCESS");
        payment.setExternalProviderReference(providerResult.providerReference());
        payment.setCompletedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        AutopayExecution execution = new AutopayExecution();
        execution.setAutopayAuthorization(authorization);
        execution.setUser(user);
        execution.setAmount(amount);
        execution.setStatus("SUCCESS");
        execution.setTransactionReference(tx.getReferenceNumber());
        execution.setTransactionId(tx.getId());
        execution.setProviderReference(authorization.getProviderAuthorizationReference());
        execution.setIdempotencyKey(idempotencyKey);
        execution.setSimulated(true);
        execution.setExecutedAt(LocalDateTime.now());
        executionRepository.save(execution);
        if (authorization.getSavingsGoal() != null) {
            SavingsGoal goal = authorization.getSavingsGoal();
            goal.setCurrentAmount(goal.getCurrentAmount().add(amount));
            goal.setStatus(goal.getCurrentAmount().compareTo(goal.getTargetAmount()) >= 0
                    ? "COMPLETED" : "ACTIVE");
            goal.setUpdatedAt(LocalDateTime.now());
            savingsGoalRepository.save(goal);
            goalLedgerService.record(goal, amount, "AUTOPAY", tx.getReferenceNumber());
        }
        reconciliationRecordRepository.save(new ReconciliationRecord(
                payment.getReferenceNumber(),
                payment.getExternalProviderReference(),
                payment.getAmount(),
                payment.getAmount(),
                "MATCHED",
                "Autopay sandbox execution matched account ledger transaction " + tx.getReferenceNumber() + ".",
                LocalDateTime.now()
        ));
        auditService.log(user, "AUTOPAY_EXECUTED", "AutopayAuthorization/" + authorization.getId(),
                idempotencyKey, "MEDIUM", "SUCCESS",
                "Autopay sandbox execution completed for " + authorization.getMerchantName() + ".");
        if (authorization.isNotifyOnSuccess()) {
            notificationRepository.createIfAbsent(user.getId(), "AUTOPAY_SUCCESS:" + idempotencyKey,
                    "AUTOPAY_SUCCESS", "Autopay payment processed",
                    "Payment of ₹" + amount + " to " + authorization.getMerchantName()
                            + " completed in FinFlow sandbox.", "INFO");
        }
        authorization.setLastPaymentDate(LocalDate.now());
        authorization.setLastPaymentStatus("SUCCESS");
        advanceNextPaymentDate(authorization);
        authorizationRepository.save(authorization);
    }

    @Transactional
    public AutopayExecutionResponse executeSimulatedPayment(Long id) {
        AutopayAuthorization authorization = findOwned(id);

        if (!"ACTIVE".equals(authorization.getStatus())) {
            throw new InvalidMoneyRuleException("Only active autopay authorizations can be executed");
        }
        if (authorization.getNextPaymentDate().isAfter(LocalDate.now())) {
            throw new InvalidMoneyRuleException("This autopay mandate is not due for execution yet");
        }

        executeScheduledMandate(id);
        AutopayExecution saved = executionRepository.findByAutopayAuthorizationIdOrderByExecutedAtDesc(id)
                .stream().findFirst().orElseThrow(() -> new InvalidMoneyRuleException("Autopay execution was not recorded"));
        return toExecutionResponse(saved);

    }

    @Transactional(readOnly = true)
    public List<AutopayAuthorizationResponse> getUpcomingPayments() {
        Long userId = currentUserService.get().getId();
        return authorizationRepository.findByUserIdOrderByNextPaymentDateAsc(userId)
                .stream()
                .filter(a -> "ACTIVE".equalsIgnoreCase(a.getStatus()))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AutopayExecutionResponse> getExecutionHistory(Long autopayId) {
        Long userId = currentUserService.get().getId();
        List<AutopayExecution> executions = (autopayId != null)
                ? executionRepository.findByAutopayAuthorizationIdAndUserIdOrderByExecutedAtDesc(autopayId, userId)
                : executionRepository.findByUserIdOrderByExecutedAtDesc(userId);
        return executions.stream().map(this::toExecutionResponse).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<AutopayExecutionResponse> getExecutionHistory(Long autopayId, Pageable pageable) {
        Long userId = currentUserService.get().getId();
        return PageResponseMapper.map(autopayId != null
                ? executionRepository.findByAutopayAuthorizationIdAndUserIdOrderByExecutedAtDescIdDesc(autopayId, userId, pageable)
                : executionRepository.findByUserIdOrderByExecutedAtDescIdDesc(userId, pageable), this::toExecutionResponse);
    }

    @Transactional
    public void assertPaymentWithinMaximum(Long id, BigDecimal amount) {
        AutopayAuthorization authorization = findOwned(id);
        if (amount.compareTo(authorization.getMaximumAmount()) > 0) {
            throw new SpendingLimitExceededException(
                    "Autopay amount exceeds your configured maximum.");
        }
        if (!"ACTIVE".equals(authorization.getStatus())) {
            throw new InvalidMoneyRuleException("Autopay authorization is not active");
        }
        requireProvider().executePayment(authorization.getProviderAuthorizationReference(), amount);
    }

    private void recordExecutionFailure(AutopayAuthorization authorization, User user, BigDecimal amount,
                                        String idempotencyKey, String reason) {
        if (executionRepository.existsByIdempotencyKey(idempotencyKey)) {
            return;
        }
        authorization.setLastPaymentDate(LocalDate.now());
        authorization.setLastPaymentStatus("FAILED");
        authorizationRepository.save(authorization);

        Payment payment = createAutopayPayment(authorization, user, amount, idempotencyKey);
        payment.setStatus("FAILED");
        payment.setFailureReason(reason);
        payment.setCompletedAt(LocalDateTime.now());
        paymentRepository.save(payment);
        paymentAttemptRepository.save(new PaymentAttempt(
                payment.getId(),
                1,
                authorization.getProvider(),
                "FAILED",
                "Execute mandate " + authorization.getProviderAuthorizationReference(),
                reason,
                LocalDateTime.now()
        ));

        AutopayExecution execution = new AutopayExecution();
        execution.setAutopayAuthorization(authorization);
        execution.setUser(user);
        execution.setAmount(amount);
        execution.setStatus("FAILED");
        execution.setFailureReason(reason);
        execution.setProviderReference(authorization.getProviderAuthorizationReference());
        execution.setIdempotencyKey(idempotencyKey);
        execution.setSimulated(true);
        execution.setExecutedAt(LocalDateTime.now());
        executionRepository.save(execution);
        auditService.log(user, "AUTOPAY_EXECUTION_FAILED", "AutopayAuthorization/" + authorization.getId(),
                idempotencyKey, "MEDIUM", "FAILED", reason);
        if (authorization.isNotifyOnFailure()) {
            notificationRepository.createIfAbsent(user.getId(), "AUTOPAY_FAILED:" + execution.getIdempotencyKey(),
                    "AUTOPAY_FAILED", "Autopay payment failed",
                    "Payment of ₹" + amount + " to " + authorization.getMerchantName()
                            + " could not be processed: " + reason, "WARNING");
        }
        authorization.setNextPaymentDate(calculateNextPaymentDate(
                authorization.getNextPaymentDate(), authorization.getFrequency()));
        authorizationRepository.save(authorization);
    }

    private void recordExecutionPending(AutopayAuthorization authorization, User user, BigDecimal amount,
                                        String idempotencyKey, String reason) {
        if (executionRepository.existsByIdempotencyKey(idempotencyKey)) return;
        Payment payment = createAutopayPayment(authorization, user, amount, idempotencyKey);
        payment.setStatus("PENDING_RECONCILIATION");
        paymentRepository.save(payment);
        paymentAttemptRepository.save(new PaymentAttempt(payment.getId(), 1, authorization.getProvider(),
                "PENDING_RECONCILIATION", "Execute mandate", reason, LocalDateTime.now()));
        AutopayExecution execution = new AutopayExecution();
        execution.setAutopayAuthorization(authorization);
        execution.setUser(user);
        execution.setAmount(amount);
        execution.setStatus("PENDING_RECONCILIATION");
        execution.setFailureReason(reason);
        execution.setProviderReference(authorization.getProviderAuthorizationReference());
        execution.setIdempotencyKey(idempotencyKey);
        execution.setSimulated(true);
        execution.setExecutedAt(LocalDateTime.now());
        executionRepository.save(execution);
        authorization.setLastPaymentStatus("PENDING_RECONCILIATION");
        authorizationRepository.save(authorization);
    }

    private Payment createAutopayPayment(AutopayAuthorization authorization, User user,
                                         BigDecimal amount, String idempotencyKey) {
        String paymentIdempotencyKey = "AUTOPAY_PAYMENT:" + idempotencyKey;
        return paymentRepository.findByIdempotencyKey(paymentIdempotencyKey).orElseGet(() -> {
            Payment payment = new Payment();
            payment.setReferenceNumber("FF-AP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase());
            payment.setUser(user);
            payment.setSourceType("AUTOPAY_ACCOUNT");
            payment.setSourceId(authorization.getAccount().getId());
            payment.setRecipientType("MERCHANT");
            payment.setRecipientIdentifier(authorization.getProviderAuthorizationReference());
            payment.setRecipientName(authorization.getMerchantName());
            payment.setAmount(amount);
            payment.setCurrency("INR");
            payment.setStatus("PROCESSING");
            payment.setCategory(authorization.getCategory());
            payment.setDescription("Autopay mandate execution: " + authorization.getMerchantName());
            payment.setIdempotencyKey(paymentIdempotencyKey);
            payment.setCreatedAt(LocalDateTime.now());
            return paymentRepository.save(payment);
        });
    }

    private String cycleIdempotencyKey(AutopayAuthorization authorization) {
        return "AP:" + authorization.getId() + ":" + authorization.getNextPaymentDate();
    }

    private LocalDate calculateNextPaymentDate(LocalDate current, String frequency) {
        return switch (frequency.toUpperCase(Locale.ROOT)) {
            case "DAILY" -> current.plusDays(1);
            case "WEEKLY" -> current.plusWeeks(1);
            case "QUARTERLY" -> current.plusMonths(3);
            case "YEARLY" -> current.plusYears(1);
            default -> current.plusMonths(1);
        };
    }

    private void advanceNextPaymentDate(AutopayAuthorization authorization) {
        authorization.setNextPaymentDate(calculateNextPaymentDate(
                authorization.getNextPaymentDate(), authorization.getFrequency()));
        if (authorization.getEndDate() != null
                && authorization.getNextPaymentDate().isAfter(authorization.getEndDate())) {
            authorization.setStatus("COMPLETED");
            moneyManagementService.releaseReservationForSystem(authorization.getReservationId());
            authorization.setReservationId(null);
        }
        authorization.setUpdatedAt(LocalDateTime.now());
    }

    private void validateRequest(BigDecimal amount, BigDecimal maxAmount, String frequency, LocalDate nextPaymentDate) {
        if (amount == null || maxAmount == null || frequency == null || nextPaymentDate == null
                || amount.signum() <= 0 || maxAmount.signum() <= 0) {
            throw new InvalidMoneyRuleException("Autopay amount, frequency, and next payment date are required");
        }
        if (amount.compareTo(maxAmount) > 0) {
            throw new InvalidMoneyRuleException("Maximum amount must be at least the scheduled amount");
        }
        if (!FREQUENCIES.contains(frequency.toUpperCase(Locale.ROOT))) {
            throw new InvalidMoneyRuleException("Unsupported autopay frequency");
        }
        if (nextPaymentDate.isBefore(LocalDate.now())) {
            throw new InvalidMoneyRuleException("Next payment date cannot be in the past");
        }
    }

    private AutopayAuthorization findOwned(Long id) {
        User user = currentUserService.get();
        return authorizationRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Autopay authorization not found"));
    }

    private AutopayProvider requireProvider() {
        return requireProvider(null);
    }

    private AutopayProvider requireProvider(String providerName) {
        AutopayProvider p = provider.orElseThrow(() ->
                new ProviderNotConfiguredException(
                        "Autopay is unavailable until a legitimate payment provider is configured."));
        if (providerName != null && !isSupportedProvider(providerName)) {
            throw new ProviderNotConfiguredException(
                    "Payment provider " + providerName + " is not configured.");
        }
        return p;
    }

    private boolean isSupportedProvider(String providerName) {
        String p = providerName.trim().toUpperCase(Locale.ROOT);
        return p.startsWith("FINFLOW") || p.startsWith("INTERNAL") || p.startsWith("CARD") || p.equals("MANUAL");
    }

    private void releaseReservation(AutopayAuthorization authorization) {
        if (authorization.getReservationId() != null) {
            moneyManagementService.releaseReservation(authorization.getReservationId());
            authorization.setReservationId(null);
        }
    }

    private AutopayAuthorizationResponse toResponse(AutopayAuthorization authorization) {
        Long accountId = authorization.getAccount() != null ? authorization.getAccount().getId() : null;
        String accountName = authorization.getAccount() != null ? authorization.getAccount().getAccountType() : null;
        
        Long cardId = authorization.getCard() != null ? authorization.getCard().getId() : null;
        String cardNickname = authorization.getCard() != null ? authorization.getCard().getNickname() : null;
        String cardLast4 = authorization.getCard() != null ? authorization.getCard().getLastFourDigits() : null;
        String cardNetwork = authorization.getCard() != null ? authorization.getCard().getCardNetwork() : null;
        Long savingsGoalId = authorization.getSavingsGoal() != null ? authorization.getSavingsGoal().getId() : null;
        String savingsGoalName = authorization.getSavingsGoal() != null ? authorization.getSavingsGoal().getName() : null;

        return new AutopayAuthorizationResponse(
                authorization.getId(),
                authorization.getMerchantName(),
                authorization.getProvider(),
                authorization.getAmount(),
                authorization.getMaximumAmount(),
                authorization.getFrequency(),
                authorization.getNextPaymentDate(),
                authorization.getStatus(),
                authorization.getDescription(),
                authorization.getProviderAuthorizationReference(),
                accountId,
                accountName,
                cardId,
                cardNickname,
                cardLast4,
                cardNetwork,
                savingsGoalId,
                savingsGoalName,
                authorization.getLastPaymentDate(),
                authorization.getLastPaymentStatus(),
                true, // FinFlow simulated payment
                authorization.getCreatedAt(),
                authorization.getUpdatedAt(),
                authorization.getCancelledAt()
        );
    }

    private AutopayExecutionResponse toExecutionResponse(AutopayExecution execution) {
        return new AutopayExecutionResponse(
                execution.getId(),
                execution.getAutopayAuthorization().getId(),
                execution.getAutopayAuthorization().getMerchantName(),
                execution.getAmount(),
                execution.getStatus(),
                execution.getFailureReason(),
                execution.getTransactionReference(),
                execution.isSimulated(),
                execution.getExecutedAt()
        );
    }
}
