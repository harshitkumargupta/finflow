package com.finflow.service;

import com.finflow.dto.*;
import com.finflow.entity.*;
import com.finflow.exception.*;
import com.finflow.repository.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class MoneyManagementService {
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);
    private static final List<String> ALLOCATION_DESTINATIONS =
            List.of("SPENDING", "SAVINGS", "BILLS", "GOAL", "EMERGENCY");
    private static final List<String> RESERVATION_BUCKETS =
            List.of("SAVINGS", "BILLS", "GOALS", "EMERGENCY", "AUTOPAY", "OTHER");
    private static final List<String> CATEGORIES =
            List.of("FOOD", "GROCERIES", "TRANSPORT", "SHOPPING", "BILLS",
                    "ENTERTAINMENT", "HEALTH", "EDUCATION", "OTHER");

    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final WalletRepository walletRepository;
    private final MoneyAllocationRuleRepository ruleRepository;
    private final MoneyReservationRepository reservationRepository;
    private final SpendingLimitRepository spendingLimitRepository;
    private final SavingsGoalRepository savingsGoalRepository;
    private final TransactionRepository transactionRepository;

    public MoneyManagementService(
            CurrentUserService currentUserService,
            UserRepository userRepository,
            AccountRepository accountRepository,
            WalletRepository walletRepository,
            MoneyAllocationRuleRepository ruleRepository,
            MoneyReservationRepository reservationRepository,
            SpendingLimitRepository spendingLimitRepository,
            SavingsGoalRepository savingsGoalRepository,
            TransactionRepository transactionRepository) {
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.walletRepository = walletRepository;
        this.ruleRepository = ruleRepository;
        this.reservationRepository = reservationRepository;
        this.spendingLimitRepository = spendingLimitRepository;
        this.savingsGoalRepository = savingsGoalRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public WalletResponse createWallet() {
        User current = currentUserService.get();
        User user = lockUser(current.getId());
        if (walletRepository.findByUserId(user.getId()).isPresent()) {
            throw new ResourceAlreadyExistsException("A FinFlow wallet already exists");
        }

        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setWalletIdentifier(uniqueValue("FFW-", false));
        wallet.setPaymentId(generateDefaultPaymentId(user));
        wallet.setCurrency("INR");
        wallet.setStatus("ACTIVE");
        wallet.setCreatedAt(LocalDateTime.now());
        wallet.setUpdatedAt(LocalDateTime.now());
        return toWallet(walletRepository.save(wallet));
    }

    @Transactional
    public WalletResponse getWallet() {
        User user = currentUserService.get();
        Wallet wallet = walletRepository.findByUserId(user.getId()).orElseGet(() -> {
            User lockedUser = lockUser(user.getId());
            return walletRepository.findByUserId(lockedUser.getId()).orElseGet(() -> {
                Wallet created = new Wallet();
                created.setUser(lockedUser);
                created.setWalletIdentifier(uniqueValue("FFW-", false));
                created.setPaymentId(generateDefaultPaymentId(lockedUser));
                created.setCurrency("INR");
                created.setStatus("ACTIVE");
                created.setCreatedAt(LocalDateTime.now());
                created.setUpdatedAt(LocalDateTime.now());
                return walletRepository.save(created);
            });
        });
        return toWallet(wallet);
    }

    @Transactional(readOnly = true)
    public WalletResponse getWallet(Long walletId) {
        User user = currentUserService.get();
        Wallet wallet = walletRepository.findByIdAndUserId(walletId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("FinFlow wallet not found"));
        return toWallet(wallet);
    }

    @Transactional
    public WalletResponse updatePaymentIdentity(PaymentIdentityRequest request) {
        User current = currentUserService.get();
        User user = lockUser(current.getId());
        Wallet wallet = walletRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("FinFlow wallet not found"));
        String paymentId = request.paymentId().trim().toLowerCase(Locale.ROOT);
        if (!paymentId.contains("@")) {
            paymentId = paymentId + "@finflow";
        }
        if (!Pattern.compile("^[a-z0-9._-]{3,30}@finflow$").matcher(paymentId).matches()) {
            throw new InvalidMoneyRuleException("Invalid FinFlow Payment ID format. Must be 3-30 characters ending in @finflow");
        }

        String finalPaymentId = paymentId;
        walletRepository.findByPaymentId(finalPaymentId)
                .filter(existing -> !existing.getId().equals(wallet.getId()))
                .ifPresent(existing -> {
                    throw new ResourceAlreadyExistsException("Payment ID is already in use");
                });
        wallet.setPaymentId(finalPaymentId);
        wallet.setUpdatedAt(LocalDateTime.now());
        return toWallet(walletRepository.save(wallet));
    }

    private String generateDefaultPaymentId(User user) {
        String base = user.getEmail() != null && user.getEmail().contains("@")
                ? user.getEmail().split("@")[0].replaceAll("[^a-zA-Z0-9._-]", "").toLowerCase(Locale.ROOT)
                : user.getName() != null ? user.getName().replaceAll("[^a-zA-Z0-9._-]", "").toLowerCase(Locale.ROOT) : "user" + user.getId();
        if (base.length() < 3) base = "user" + user.getId();
        if (base.length() > 25) base = base.substring(0, 25);
        String candidate = base + "@finflow";
        if (!walletRepository.existsByPaymentId(candidate)) {
            return candidate;
        }
        int suffix = 1;
        while (walletRepository.existsByPaymentId(base + suffix + "@finflow")) {
            suffix++;
        }
        return base + suffix + "@finflow";
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> walletTransactions() {
        User user = currentUserService.get();
        walletRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("FinFlow wallet not found"));
        return transactionRepository.findSuccessfulByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(transaction -> new TransactionResponse(
                        transaction.getId(),
                        transaction.getReferenceNumber(),
                        transaction.getAmount(),
                        transaction.getType(),
                        transaction.getCategory(),
                        transaction.getCategoryConfidence(),
                        transaction.getCategorySource(),
                        transaction.getCategoryReason(),
                        transaction.getDescription(),
                        transaction.getStatus(),
                        transaction.getCreatedAt(),
                        transaction.getReceiverAccountNumber(),
                        transaction.getBalanceAfter()))
                .toList();
    }

    @Transactional
    public MoneyAllocationRuleResponse createRule(MoneyAllocationRuleRequest request) {
        User current = currentUserService.get();
        User user = lockUser(current.getId());
        MoneyAllocationRule rule = new MoneyAllocationRule();
        applyRule(rule, user, request);
        rule.setCreatedAt(LocalDateTime.now());
        rule.setUpdatedAt(LocalDateTime.now());
        return toRule(ruleRepository.save(rule));
    }

    @Transactional(readOnly = true)
    public List<MoneyAllocationRuleResponse> listRules() {
        User user = currentUserService.get();
        return ruleRepository.findByUserIdOrderByPriorityAscIdAsc(user.getId())
                .stream().map(this::toRule).toList();
    }

    @Transactional
    public MoneyAllocationRuleResponse updateRule(Long id, MoneyAllocationRuleRequest request) {
        User current = currentUserService.get();
        User user = lockUser(current.getId());
        MoneyAllocationRule rule = ruleRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Money allocation rule not found"));
        applyRule(rule, user, request);
        rule.setUpdatedAt(LocalDateTime.now());
        return toRule(ruleRepository.save(rule));
    }

    @Transactional
    public void deleteRule(Long id) {
        User current = currentUserService.get();
        User user = lockUser(current.getId());
        MoneyAllocationRule rule = ruleRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Money allocation rule not found"));
        ruleRepository.delete(rule);
    }

    @Transactional(readOnly = true)
    public List<MoneyReservationResponse> listReservations() {
        User user = currentUserService.get();
        return reservationRepository.findByUserIdAndStatusOrderByCreatedAtDesc(user.getId(), "ACTIVE")
                .stream().map(this::toReservation).toList();
    }

    @Transactional
    public MoneyReservationResponse reserve(MoneyReservationRequest request) {
        User current = currentUserService.get();
        Account account = accountRepository.findByIdForUpdate(request.accountId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        requireOwner(account.getUser().getId(), current.getId());
        User user = lockUser(current.getId());
        requireActiveAccount(account);
        String bucket = request.bucket().toUpperCase(Locale.ROOT);
        if (!RESERVATION_BUCKETS.contains(bucket)) {
            throw new InvalidMoneyRuleException("Unsupported reservation bucket");
        }
        BigDecimal available = account.getBalance()
                .subtract(reservationRepository.sumActiveByAccountId(account.getId()));
        if (available.compareTo(request.amount()) < 0) {
            throw new InsufficientAvailableBalanceException(
                    "The reservation exceeds the account's available balance.");
        }
        MoneyReservation reservation = new MoneyReservation();
        reservation.setUser(user);
        reservation.setAccount(account);
        reservation.setBucket(bucket);
        reservation.setAmount(request.amount().setScale(2, RoundingMode.UNNECESSARY));
        reservation.setStatus("ACTIVE");
        reservation.setSourceKey("MANUAL:" + UUID.randomUUID());
        reservation.setDescription(request.description());
        reservation.setCreatedAt(LocalDateTime.now());
        return toReservation(reservationRepository.save(reservation));
    }

    @Transactional
    public MoneyReservationResponse reserveFamilyAllowance(
            Account account, FamilyMember member, BigDecimal amount, String description) {
        User current = currentUserService.get();
        Account lockedAccount = accountRepository.findByIdForUpdate(account.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        requireOwner(lockedAccount.getUser().getId(), current.getId());
        if (!member.getParent().getId().equals(current.getId())) {
            throw new AccessDeniedException("You are not authorized to fund this family wallet");
        }
        User user = lockUser(current.getId());
        requireActiveAccount(lockedAccount);
        BigDecimal available = lockedAccount.getBalance()
                .subtract(reservationRepository.sumActiveByAccountId(lockedAccount.getId()));
        if (available.compareTo(amount) < 0) {
            throw new InsufficientAvailableBalanceException(
                    "The allowance exceeds the account's available balance.");
        }
        MoneyReservation reservation = new MoneyReservation();
        reservation.setUser(user);
        reservation.setAccount(lockedAccount);
        reservation.setFamilyMember(member);
        reservation.setBucket("FAMILY");
        reservation.setAmount(amount.setScale(2, RoundingMode.UNNECESSARY));
        reservation.setStatus("ACTIVE");
        reservation.setSourceKey("FAMILY:" + member.getId() + ":" + UUID.randomUUID());
        reservation.setDescription(description);
        reservation.setCreatedAt(LocalDateTime.now());
        return toReservation(reservationRepository.save(reservation));
    }

    @Transactional
    public void consumeFamilyAllowance(Account account, FamilyMember member, BigDecimal amount) {
        lockUser(member.getParent().getId());
        List<MoneyReservation> reservations = reservationRepository
                .findByFamilyMemberIdAndAccountIdAndStatusOrderByCreatedAtAsc(
                        member.getId(), account.getId(), "ACTIVE");
        BigDecimal reserved = reservations.stream()
                .map(MoneyReservation::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (reserved.compareTo(amount) < 0) {
            throw new InsufficientAvailableBalanceException(
                    "The controlled wallet does not have enough reserved allowance.");
        }
        BigDecimal remaining = amount;
        for (MoneyReservation reservation : reservations) {
            if (remaining.signum() <= 0) break;
            BigDecimal consumed = reservation.getAmount().min(remaining);
            BigDecimal balance = reservation.getAmount().subtract(consumed);
            reservation.setAmount(balance);
            remaining = remaining.subtract(consumed);
            if (balance.signum() == 0) {
                reservation.setStatus("RELEASED");
                reservation.setReleasedAt(LocalDateTime.now());
            }
        }
        reservationRepository.saveAll(reservations);
    }

    @Transactional
    public MoneyReservationResponse releaseReservation(Long id) {
        User current = currentUserService.get();
        MoneyReservation reservation = reservationRepository.findByIdAndUserId(id, current.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Money reservation not found"));
        accountRepository.findByIdForUpdate(reservation.getAccount().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        lockUser(current.getId());
        if (!"ACTIVE".equals(reservation.getStatus())) {
            throw new InvalidMoneyRuleException("Money reservation is already released");
        }
        reservation.setStatus("RELEASED");
        reservation.setReleasedAt(LocalDateTime.now());
        return toReservation(reservationRepository.save(reservation));
    }

    @Transactional
    public void releaseReservationForSystem(Long id) {
        if (id == null) return;
        MoneyReservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Money reservation not found"));
        if ("ACTIVE".equals(reservation.getStatus())) {
            reservation.setStatus("RELEASED");
            reservation.setReleasedAt(LocalDateTime.now());
            reservationRepository.save(reservation);
        }
    }

    @Transactional
    public SpendingLimitResponse createSpendingLimit(SpendingLimitRequest request) {
        User current = currentUserService.get();
        User user = lockUser(current.getId());
        SpendingLimit limit = new SpendingLimit();
        applySpendingLimit(limit, user, request);
        if (spendingLimitRepository.findByUserIdAndScopeTypeAndCategory(
                user.getId(), limit.getScopeType(), limit.getCategory()).isPresent()) {
            throw new ResourceAlreadyExistsException("A spending limit already exists for this scope");
        }
        limit.setCreatedAt(LocalDateTime.now());
        limit.setUpdatedAt(LocalDateTime.now());
        return toSpendingLimit(spendingLimitRepository.save(limit));
    }

    @Transactional(readOnly = true)
    public List<SpendingLimitResponse> listSpendingLimits() {
        User user = currentUserService.get();
        return spendingLimitRepository.findByUserIdOrderByScopeTypeAscCategoryAsc(user.getId())
                .stream().map(this::toSpendingLimit).toList();
    }

    @Transactional
    public SpendingLimitResponse updateSpendingLimit(Long id, SpendingLimitRequest request) {
        User current = currentUserService.get();
        User user = lockUser(current.getId());
        SpendingLimit limit = spendingLimitRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Spending limit not found"));
        applySpendingLimit(limit, user, request);
        spendingLimitRepository.findByUserIdAndScopeTypeAndCategory(
                        user.getId(), limit.getScopeType(), limit.getCategory())
                .filter(existing -> !existing.getId().equals(limit.getId()))
                .ifPresent(existing -> {
                    throw new ResourceAlreadyExistsException("A spending limit already exists for this scope");
                });
        limit.setUpdatedAt(LocalDateTime.now());
        return toSpendingLimit(spendingLimitRepository.save(limit));
    }

    @Transactional
    public void deleteSpendingLimit(Long id) {
        User current = currentUserService.get();
        User user = lockUser(current.getId());
        SpendingLimit limit = spendingLimitRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Spending limit not found"));
        spendingLimitRepository.delete(limit);
    }

    @Transactional(readOnly = true)
    public FinancialSafetyResponse safetySummary() {
        User user = currentUserService.get();
        BigDecimal total = zero(accountRepository.sumBalancesByUserId(user.getId()));
        BigDecimal reserved = zero(reservationRepository.sumActiveByUserId(user.getId()));
        BigDecimal available = total.subtract(reserved).max(ZERO);
        LocalDate today = LocalDate.now();
        BigDecimal monthlySpent = outgoingSpent(user.getId(), "OVERALL", today);
        BigDecimal monthlyLimit = spendingLimitRepository
                .findByUserIdAndScopeTypeAndCategory(user.getId(), "MONTHLY", "OVERALL")
                .map(SpendingLimit::getAmount)
                .orElse(null);
        BigDecimal allowance = monthlyLimit == null
                ? available
                : monthlyLimit.subtract(monthlySpent).max(ZERO);
        return new FinancialSafetyResponse(
                total, available, reserved, monthlyLimit, monthlySpent, allowance,
                available.min(allowance).max(ZERO));
    }

    @Transactional
    public void allocateIncomingDeposit(Account account, Transaction transaction) {
        User user = lockUser(account.getUser().getId());
        List<MoneyAllocationRule> rules =
                ruleRepository.findByUserIdAndActiveTrueOrderByPriorityAscIdAsc(user.getId());
        for (MoneyAllocationRule rule : rules) {
            if ("SPENDING".equals(rule.getDestinationType())) continue;
            BigDecimal allocation = transaction.getAmount()
                    .multiply(rule.getPercentage())
                    .divide(HUNDRED, 2, RoundingMode.DOWN);
            if (allocation.signum() <= 0) continue;
            String sourceKey = "DEPOSIT:" + transaction.getId() + ":RULE:" + rule.getId();
            if (reservationRepository.existsByUserIdAndSourceKey(user.getId(), sourceKey)) continue;
            MoneyReservation reservation = new MoneyReservation();
            reservation.setUser(user);
            reservation.setAccount(account);
            reservation.setBucket(toReservationBucket(rule.getDestinationType()));
            reservation.setAmount(allocation);
            reservation.setStatus("ACTIVE");
            reservation.setSourceKey(sourceKey);
            reservation.setDescription("Automatic allocation: " + rule.getName());
            reservation.setCreatedAt(LocalDateTime.now());
            reservationRepository.save(reservation);
        }
    }

    @Transactional
    public void assertSpendAllowed(Account account, BigDecimal amount, String category) {
        User user = lockUser(account.getUser().getId());
        requireActiveAccount(account);
        BigDecimal available = account.getBalance()
                .subtract(reservationRepository.sumActiveByAccountId(account.getId()));
        if (available.compareTo(amount) < 0) {
            throw new InsufficientAvailableBalanceException(
                    "The payment exceeds the account's available balance after reserved funds.");
        }

        LocalDate today = LocalDate.now();
        for (SpendingLimit limit : spendingLimitRepository.findByUserIdOrderByScopeTypeAscCategoryAsc(user.getId())) {
            if ("PER_TRANSACTION".equals(limit.getScopeType())) {
                if (matchesCategory(limit.getCategory(), category)
                        && amount.compareTo(limit.getAmount()) > 0) {
                    throw exceeded(limit.getAmount());
                }
            } else if ("MONTHLY".equals(limit.getScopeType())) {
                BigDecimal spent = outgoingSpent(user.getId(), "OVERALL", today);
                if (spent.add(amount).compareTo(limit.getAmount()) > 0) {
                    throw exceeded(limit.getAmount());
                }
            } else if ("CATEGORY_MONTHLY".equals(limit.getScopeType())
                    && matchesCategory(limit.getCategory(), category)) {
                BigDecimal spent = outgoingSpent(user.getId(), limit.getCategory(), today);
                if (spent.add(amount).compareTo(limit.getAmount()) > 0) {
                    throw exceeded(limit.getAmount());
                }
            }
        }
    }

    private void applyRule(MoneyAllocationRule rule, User user, MoneyAllocationRuleRequest request) {
        String destination = request.destinationType().toUpperCase(Locale.ROOT);
        if (!ALLOCATION_DESTINATIONS.contains(destination)) {
            throw new InvalidMoneyRuleException("Unsupported allocation destination");
        }
        Long destinationId = request.destinationId();
        if ("GOAL".equals(destination)) {
            if (destinationId == null
                    || savingsGoalRepository.findByIdAndUserId(destinationId, user.getId()).isEmpty()) {
                throw new ResourceNotFoundException("Savings goal not found");
            }
        } else if (destinationId != null) {
            throw new InvalidMoneyRuleException("Only GOAL allocation rules can include a destination ID");
        }
        BigDecimal activePercentages = ruleRepository
                .findByUserIdAndActiveTrueOrderByPriorityAscIdAsc(user.getId())
                .stream()
                .filter(existing -> !existing.getId().equals(rule.getId()))
                .map(MoneyAllocationRule::getPercentage)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean active = request.active() == null || request.active();
        if (active) activePercentages = activePercentages.add(request.percentage());
        if (activePercentages.compareTo(HUNDRED) > 0) {
            throw new InvalidMoneyRuleException("Active money allocation percentages cannot exceed 100%");
        }
        rule.setUser(user);
        rule.setName(request.name().trim());
        rule.setPercentage(request.percentage());
        rule.setDestinationType(destination);
        rule.setDestinationId(destinationId);
        rule.setPriority(request.priority());
        rule.setActive(active);
    }

    private void applySpendingLimit(SpendingLimit limit, User user, SpendingLimitRequest request) {
        String scope = request.scopeType().toUpperCase(Locale.ROOT);
        if (!List.of("MONTHLY", "CATEGORY_MONTHLY", "PER_TRANSACTION").contains(scope)) {
            throw new InvalidMoneyRuleException("Unsupported spending limit scope");
        }
        String category = request.category() == null || request.category().isBlank()
                ? "OVERALL" : request.category().toUpperCase(Locale.ROOT);
        if ("CATEGORY_MONTHLY".equals(scope) && !CATEGORIES.contains(category)) {
            throw new InvalidMoneyRuleException("A supported transaction category is required");
        }
        if (!"CATEGORY_MONTHLY".equals(scope) && !category.equals("OVERALL")
                && !CATEGORIES.contains(category)) {
            throw new InvalidMoneyRuleException("Unsupported transaction category");
        }
        if ("MONTHLY".equals(scope)) category = "OVERALL";
        limit.setUser(user);
        limit.setScopeType(scope);
        limit.setCategory(category);
        limit.setAmount(request.amount().setScale(2, RoundingMode.UNNECESSARY));
    }

    private SpendingLimitResponse toSpendingLimit(SpendingLimit limit) {
        BigDecimal spent = "PER_TRANSACTION".equals(limit.getScopeType())
                ? ZERO
                : outgoingSpent(limit.getUser().getId(),
                        "CATEGORY_MONTHLY".equals(limit.getScopeType()) ? limit.getCategory() : "OVERALL",
                        LocalDate.now());
        return new SpendingLimitResponse(
                limit.getId(), limit.getScopeType(), limit.getCategory(), limit.getAmount(),
                spent, limit.getAmount().subtract(spent).max(ZERO),
                limit.getCreatedAt(), limit.getUpdatedAt());
    }

    private BigDecimal outgoingSpent(Long userId, String category, LocalDate today) {
        LocalDate first = today.withDayOfMonth(1);
        LocalDate last = first.plusMonths(1).minusDays(1);
        return zero(transactionRepository.sumExpensesForBudget(
                userId, category, first.atStartOfDay(), last.atTime(23, 59, 59, 999_999_999)));
    }

    private WalletResponse toWallet(Wallet wallet) {
        BigDecimal total = zero(accountRepository.sumBalancesByUserId(wallet.getUser().getId()));
        BigDecimal reserved = zero(reservationRepository.sumActiveByUserId(wallet.getUser().getId()));
        return new WalletResponse(wallet.getId(), wallet.getWalletIdentifier(), wallet.getPaymentId(),
                total, total.subtract(reserved).max(ZERO), reserved, zero(wallet.getBalance()), wallet.getCurrency(),
                wallet.getStatus(), wallet.getCreatedAt());
    }

    private MoneyAllocationRuleResponse toRule(MoneyAllocationRule rule) {
        return new MoneyAllocationRuleResponse(rule.getId(), rule.getName(), rule.getPercentage(),
                rule.getDestinationType(), rule.getDestinationId(), rule.getPriority(),
                rule.isActive(), rule.getCreatedAt(), rule.getUpdatedAt());
    }

    private MoneyReservationResponse toReservation(MoneyReservation reservation) {
        return new MoneyReservationResponse(reservation.getId(), reservation.getAccount().getId(),
                reservation.getBucket(), reservation.getAmount(), reservation.getStatus(),
                reservation.getDescription(), reservation.getCreatedAt(), reservation.getReleasedAt());
    }

    private String uniqueValue(String prefix, boolean lowercase) {
        String value;
        do {
            value = prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
            value = lowercase ? value.toLowerCase(Locale.ROOT) : value.toUpperCase(Locale.ROOT);
        } while (walletRepository.existsByPaymentId(value)
                || walletRepository.existsByWalletIdentifier(value));
        return value;
    }

    private User lockUser(Long userId) {
        return userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }

    private void requireOwner(Long accountOwnerId, Long userId) {
        if (!accountOwnerId.equals(userId)) {
            throw new AccessDeniedException("You are not authorized to access this account");
        }
    }

    private void requireActiveAccount(Account account) {
        if (!"ACTIVE".equalsIgnoreCase(account.getStatus())) {
            throw new AccountInactiveException("Account is not active");
        }
    }

    private String toReservationBucket(String destination) {
        return "GOAL".equals(destination) ? "GOALS" : destination;
    }

    private boolean matchesCategory(String configured, String actual) {
        return "OVERALL".equals(configured) || configured.equalsIgnoreCase(actual);
    }

    private SpendingLimitExceededException exceeded(BigDecimal limit) {
        return new SpendingLimitExceededException(
                "This payment would exceed your configured spending limit of "
                        + limit.toPlainString() + ".");
    }

    private BigDecimal zero(BigDecimal value) {
        return value == null ? ZERO : value;
    }
}
