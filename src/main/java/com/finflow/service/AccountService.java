package com.finflow.service;

import com.finflow.dto.AccountResponse;
import com.finflow.dto.CreateAccountRequest;
import com.finflow.dto.MoneyRequest;
import com.finflow.dto.FamilySpendRequest;
import com.finflow.dto.TransactionResponse;
import com.finflow.dto.TransferRequest;
import com.finflow.entity.Account;
import com.finflow.entity.FamilyMember;
import com.finflow.entity.Transaction;
import com.finflow.entity.User;
import com.finflow.exception.AccountInactiveException;
import com.finflow.exception.IdempotencyConflictException;
import com.finflow.exception.InsufficientBalanceException;
import com.finflow.exception.InvalidTransferException;
import com.finflow.exception.ResourceAlreadyExistsException;
import com.finflow.exception.ResourceNotFoundException;
import com.finflow.repository.AccountRepository;
import com.finflow.repository.TransactionRepository;
import com.finflow.repository.UserRepository;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import com.finflow.email.WelcomeEmailRequested;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import com.finflow.dto.PageResponse;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final MoneyManagementService moneyManagementService;
    private final FamilyService familyService;
    private final TransactionCategorizationService transactionCategorizationService;
    private final PasswordEncoder passwordEncoder;
    private final PinVerificationService pinVerificationService;
    private final MoneyNormalizer moneyNormalizer;
    private final ApplicationEventPublisher events;

    public AccountService(
            AccountRepository accountRepository,
            UserRepository userRepository,
            TransactionRepository transactionRepository,
            MoneyManagementService moneyManagementService,
            FamilyService familyService,
            TransactionCategorizationService transactionCategorizationService,
            PasswordEncoder passwordEncoder,
            PinVerificationService pinVerificationService,
            MoneyNormalizer moneyNormalizer,
            ApplicationEventPublisher events) {

        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.moneyManagementService = moneyManagementService;
        this.familyService = familyService;
        this.transactionCategorizationService = transactionCategorizationService;
        this.passwordEncoder = passwordEncoder;
        this.pinVerificationService = pinVerificationService;
        this.moneyNormalizer = moneyNormalizer;
        this.events = events;
    }

    // ============================================================
    // CREATE ACCOUNT
    // ============================================================

    @Transactional
    public AccountResponse createAccount(
            Long userId,
            CreateAccountRequest request) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
        User authenticatedUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new AccessDeniedException("Authenticated user not found"));

        if (!authenticatedUser.getId().equals(userId)) {
            throw new AccessDeniedException(
                    "You are not authorized to create an account for this user"
            );
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with id: " + userId
                        ));

        List<Account> existingAccounts = consolidateUserAccounts(userId);
        if (existingAccounts.stream().anyMatch(this::isActiveFinFlowAccount)) {
            throw new ResourceAlreadyExistsException(
                    "A primary FinFlow account already exists for this user. Link external bank accounts instead."
            );
        }
        if (!Objects.equals(request.getTransactionPin(), request.getConfirmTransactionPin())) {
            throw new InvalidTransferException("Transaction PIN confirmation does not match");
        }
        user.setTransactionPinHash(passwordEncoder.encode(request.getTransactionPin()));
        userRepository.save(user);

        Account account = new Account();

        account.setAccountNumber(generateAccountNumber());
        account.setBalance(BigDecimal.ZERO);

        account.setAccountType("FINFLOW");

        account.setStatus("ACTIVE");
        account.setCreatedAt(LocalDateTime.now());
        account.setUser(user);

        account.setPrimary(true);

        Account savedAccount =
                accountRepository.save(account);

        events.publishEvent(new WelcomeEmailRequested(user.getId(), user.getEmail(), user.getName(), savedAccount.getAccountNumber()));
        return toResponse(savedAccount);
    }

    // ============================================================
    // GET ACCOUNT
    // ============================================================

    @Transactional(readOnly = true)
    public AccountResponse getAccount(Long accountId) {

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Account not found with id: " + accountId
                        ));

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
        User authenticatedUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new AccessDeniedException("Authenticated user not found"));

        if (!authenticatedUser.getId().equals(account.getUser().getId())) {
            throw new AccessDeniedException("You are not authorized to access this account");
        }

        return toResponse(account);
    }

    @Transactional
    public List<AccountResponse> getMyAccounts() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
        User authenticatedUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new AccessDeniedException("Authenticated user not found"));

        return consolidateUserAccounts(authenticatedUser.getId())
                .stream()
                .filter(this::isActiveFinFlowAccount)
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AccountResponse setPrimaryAccount(Long accountId) {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
        User authenticatedUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("Authenticated user not found"));

        List<Account> accounts = consolidateUserAccounts(authenticatedUser.getId());
        Account target = null;
        for (Account a : accounts) {
            if (a.getId().equals(accountId)) {
                a.setPrimary(true);
                target = a;
            } else {
                a.setPrimary(false);
            }
        }
        if (target == null) {
            throw new ResourceNotFoundException("Account not found with id: " + accountId);
        }
        accountRepository.saveAll(accounts);
        return toResponse(target);
    }

    @Transactional
    public AccountResponse getPrimaryAccount() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
        User authenticatedUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("Authenticated user not found"));

        List<Account> accounts = consolidateUserAccounts(authenticatedUser.getId());
        accounts = accounts.stream().filter(this::isActiveFinFlowAccount).toList();
        if (accounts.isEmpty()) {
            throw new ResourceNotFoundException("No accounts found for user");
        }
        Account primary = accounts.stream()
                .filter(Account::isPrimary)
                .findFirst()
                .orElse(accounts.get(0));
        return toResponse(primary);
    }

    // ============================================================
    // DEPOSIT
    // ============================================================

    @Transactional
    public AccountResponse deposit(
            Long accountId,
            MoneyRequest request,
            String idempotencyKey) {

        Account account = accountRepository
                .findByIdForUpdate(accountId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Account not found with id: "
                                        + accountId
                        ));

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
        User authenticatedUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new AccessDeniedException("Authenticated user not found"));

        if (!authenticatedUser.getId().equals(account.getUser().getId())) {
            throw new AccessDeniedException("You are not authorized to access this account");
        }

        validateAccountIsActive(account);

        BigDecimal amount = moneyNormalizer.normalizePositive(request.getAmount());
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Transaction previous = transactionRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
            if (previous != null) return toResponse(account);
        }

        account.setBalance(
                account.getBalance().add(amount)
        );

        Transaction transaction = new Transaction();

        transaction.setReferenceNumber(
                generateTransactionReference()
        );

        transaction.setAmount(amount);
        transaction.setType("DEPOSIT");
        transaction.setCategory("DEPOSIT");
        transaction.setCategoryConfidence("HIGH");
        transaction.setCategorySource("SYSTEM");
        transaction.setCategoryReason("Deposit transaction type");
        transaction.setDescription(
                request.getDescription() == null || request.getDescription().isBlank()
                        ? "Account deposit"
                        : request.getDescription()
        );
        transaction.setStatus("SUCCESS");
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setIdempotencyKey(idempotencyKey);

        transaction.setBalanceAfter(
                account.getBalance()
        );

        transaction.setAccount(account);

        Transaction savedTransaction = transactionRepository.save(transaction);
        moneyManagementService.allocateIncomingDeposit(account, savedTransaction);

        return toResponse(account);
    }

    // ============================================================
    // WITHDRAW
    // ============================================================

    @Transactional
    public AccountResponse withdraw(
            Long accountId,
            MoneyRequest request,
            String idempotencyKey) {

        Account account = accountRepository
                .findByIdForUpdate(accountId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Account not found with id: "
                                        + accountId
                        ));

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
        User authenticatedUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new AccessDeniedException("Authenticated user not found"));

        if (!authenticatedUser.getId().equals(account.getUser().getId())) {
            throw new AccessDeniedException("You are not authorized to access this account");
        }

        validateAccountIsActive(account);
        pinVerificationService.verify(authenticatedUser, request.getPin());

        BigDecimal amount = moneyNormalizer.normalizePositive(request.getAmount());
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Transaction previous = transactionRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
            if (previous != null) return toResponse(account);
        }
        String description = request.getDescription() == null || request.getDescription().isBlank()
                ? "Cash withdrawal"
                : request.getDescription();
        TransactionCategorizationService.CategorySuggestion suggestion =
                resolveCategorySuggestion(request.getCategory(), description, "WITHDRAWAL");
        String category = suggestion.category();

        moneyManagementService.assertSpendAllowed(account, amount, category);

        if (account.getBalance().compareTo(amount) < 0) {

            throw new InsufficientBalanceException(
                    "Insufficient account balance"
            );
        }

        account.setBalance(
                account.getBalance().subtract(amount)
        );

        Transaction transaction = new Transaction();

        transaction.setReferenceNumber(
                generateTransactionReference()
        );

        transaction.setAmount(amount);
        transaction.setType("WITHDRAWAL");
        transaction.setCategory(category);
        applyCategorySuggestion(transaction, suggestion, request.getCategory());
        transaction.setDescription(description);
        transaction.setStatus("SUCCESS");
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setIdempotencyKey(idempotencyKey);

        transaction.setBalanceAfter(
                account.getBalance()
        );

        transaction.setAccount(account);

        transactionRepository.save(transaction);

        return toResponse(account);
    }

    // ============================================================
    // TRANSFER
    // ============================================================

    @Transactional
    public AccountResponse transfer(
            Long senderAccountId,
            TransferRequest request,
            String idempotencyKey) {
        return transfer(senderAccountId, request, idempotencyKey, true);
    }

    @Transactional
    public AccountResponse transferAfterPinVerification(
            Long senderAccountId,
            TransferRequest request,
            String idempotencyKey) {
        return transfer(senderAccountId, request, idempotencyKey, false);
    }

    private AccountResponse transfer(
            Long senderAccountId,
            TransferRequest request,
            String idempotencyKey,
            boolean verifyPin) {

        // --------------------------------------------------------
        // Validate idempotency key
        // --------------------------------------------------------

        if (idempotencyKey == null ||
                idempotencyKey.isBlank()) {

            throw new InvalidTransferException(
                    "Idempotency-Key header is required"
            );
        }

        if (idempotencyKey.length() > 100) {

            throw new InvalidTransferException(
                    "Idempotency-Key must not exceed 100 characters"
            );
        }

        BigDecimal amount = moneyNormalizer.normalizePositive(request.getAmount());

        String receiverAccountNumber =
                request.getReceiverAccountNumber();

        // --------------------------------------------------------
        // Find receiver
        // --------------------------------------------------------

        Long receiverId = accountRepository
                .findIdByAccountNumber(receiverAccountNumber)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Receiver account not found"
                        ));

        // --------------------------------------------------------
        // Prevent self-transfer
        // --------------------------------------------------------

        if (senderAccountId.equals(receiverId)) {

            throw new InvalidTransferException(
                    "Sender and receiver accounts must be different"
            );
        }

        // --------------------------------------------------------
        // Lock accounts in deterministic order
        // Prevents deadlocks during concurrent transfers
        // --------------------------------------------------------

        Long firstLockId;
        Long secondLockId;

        if (senderAccountId < receiverId) {

            firstLockId = senderAccountId;
            secondLockId = receiverId;

        } else {

            firstLockId = receiverId;
            secondLockId = senderAccountId;
        }

        Account firstAccount = accountRepository
                .findByIdForUpdate(firstLockId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Account not found"
                        ));

        Account secondAccount = accountRepository
                .findByIdForUpdate(secondLockId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Account not found"
                        ));

        Account sender;
        Account lockedReceiver;

        if (firstAccount.getId().equals(senderAccountId)) {

            sender = firstAccount;
            lockedReceiver = secondAccount;

        } else {

            sender = secondAccount;
            lockedReceiver = firstAccount;
        }

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
        User authenticatedUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new AccessDeniedException("Authenticated user not found"));

        if (!authenticatedUser.getId().equals(sender.getUser().getId())) {
            throw new AccessDeniedException("You are not authorized to access this account");
        }

        if (verifyPin) {
            pinVerificationService.verify(authenticatedUser, request.getPin());
        }

        // --------------------------------------------------------
        // Check idempotency
        // --------------------------------------------------------

        Transaction previousTransaction =
                transactionRepository
                        .findByIdempotencyKey(idempotencyKey)
                        .orElse(null);

        if (previousTransaction != null) {

            validateSameTransfer(
                    previousTransaction,
                    sender,
                    receiverAccountNumber,
                    amount,
                    resolveCategory(request.getCategory(), resolveTransferDescription(request), "TRANSFER_OUT"),
                    resolveTransferDescription(request)
            );

            return toResponseWithBalance(
                    sender,
                    previousTransaction.getBalanceAfter()
            );
        }

        // --------------------------------------------------------
        // Validate accounts
        // --------------------------------------------------------

        validateAccountIsActive(sender);
        validateAccountIsActive(lockedReceiver);
        String description = resolveTransferDescription(request);
        TransactionCategorizationService.CategorySuggestion suggestion =
                resolveCategorySuggestion(request.getCategory(), description, "TRANSFER_OUT");
        String category = suggestion.category();
        moneyManagementService.assertSpendAllowed(sender, amount, category);

        // --------------------------------------------------------
        // Validate balance
        // --------------------------------------------------------

        if (sender.getBalance().compareTo(amount) < 0) {

            throw new InsufficientBalanceException(
                    "Insufficient account balance"
            );
        }

        // --------------------------------------------------------
        // Debit sender
        // --------------------------------------------------------

        sender.setBalance(
                sender.getBalance().subtract(amount)
        );

        // --------------------------------------------------------
        // Credit receiver
        // --------------------------------------------------------

        lockedReceiver.setBalance(
                lockedReceiver.getBalance().add(amount)
        );

        // --------------------------------------------------------
        // Create sender transaction
        // --------------------------------------------------------

        Transaction debitTransaction =
                new Transaction();

        debitTransaction.setReferenceNumber(
                generateTransactionReference()
        );

        debitTransaction.setAmount(amount);
        debitTransaction.setType("TRANSFER_OUT");
        debitTransaction.setCategory(category);
        applyCategorySuggestion(debitTransaction, suggestion, request.getCategory());
        debitTransaction.setDescription(description);
        debitTransaction.setStatus("SUCCESS");
        debitTransaction.setCreatedAt(LocalDateTime.now());

        debitTransaction.setIdempotencyKey(
                idempotencyKey
        );

        debitTransaction.setReceiverAccountNumber(
                receiverAccountNumber
        );

        debitTransaction.setBalanceAfter(
                sender.getBalance()
        );

        debitTransaction.setAccount(sender);

        transactionRepository.save(debitTransaction);

        // --------------------------------------------------------
        // Create receiver transaction
        // --------------------------------------------------------

        Transaction creditTransaction =
                new Transaction();

        creditTransaction.setReferenceNumber(
                generateTransactionReference()
        );

        creditTransaction.setAmount(amount);
        creditTransaction.setType("TRANSFER_IN");
        creditTransaction.setCategory("TRANSFER");
        creditTransaction.setCategoryConfidence("HIGH");
        creditTransaction.setCategorySource("SYSTEM");
        creditTransaction.setCategoryReason("Incoming transfer transaction type");
        creditTransaction.setDescription("Money received from another account");
        creditTransaction.setStatus("SUCCESS");
        creditTransaction.setCreatedAt(LocalDateTime.now());

        creditTransaction.setBalanceAfter(
                lockedReceiver.getBalance()
        );

        creditTransaction.setAccount(
                lockedReceiver
        );

        transactionRepository.save(creditTransaction);

        return toResponse(sender);
    }

    @Transactional
    public TransactionResponse spendFamilyAllowance(
            Long senderAccountId,
            Long familyMemberId,
            FamilySpendRequest familyRequest,
            String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 90) {
            throw new InvalidTransferException(
                    "Idempotency-Key is required and must not exceed 90 characters");
        }
        Account account = accountRepository.findByIdForUpdate(senderAccountId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account not found with id: " + senderAccountId));
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User authenticatedUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("Authenticated user not found"));
        if (!authenticatedUser.getId().equals(account.getUser().getId())) {
            throw new AccessDeniedException("You are not authorized to access this account");
        }
        pinVerificationService.verify(authenticatedUser, familyRequest.pin());
        Transaction previous = transactionRepository.findByIdempotencyKey("FAM:" + idempotencyKey)
                .orElse(null);
        if (previous != null) {
            familyService.validateSpendRetry(
                    familyMemberId, idempotencyKey, familyRequest, previous);
            return toTransactionResponse(previous);
        }

        validateAccountIsActive(account);
        FamilyMember member = familyService.consumeAllowance(
                familyMemberId, account, familyRequest, idempotencyKey);
        moneyManagementService.assertSpendAllowed(account, familyRequest.amount(), familyRequest.category());
        if (account.getBalance().compareTo(familyRequest.amount()) < 0) {
            throw new InsufficientBalanceException("Insufficient account balance");
        }

        account.setBalance(account.getBalance().subtract(familyRequest.amount()));
        Transaction transaction = new Transaction();
        transaction.setReferenceNumber(generateTransactionReference());
        transaction.setAmount(familyRequest.amount());
        transaction.setType("WITHDRAWAL");
        transaction.setCategory(familyRequest.category());
        String transactionDescription = "Family wallet spend: " + member.getDisplayName();
        if (familyRequest.description() != null && !familyRequest.description().isBlank()) {
            transactionDescription += " · " + familyRequest.description();
        }
        transaction.setDescription(transactionDescription);
        transaction.setStatus("SUCCESS");
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setIdempotencyKey("FAM:" + idempotencyKey);
        transaction.setBalanceAfter(account.getBalance());
        transaction.setAccount(account);
        Transaction savedTransaction = transactionRepository.save(transaction);
        familyService.recordSpend(
                familyMemberId, idempotencyKey, account, familyRequest, savedTransaction);
        return toTransactionResponse(savedTransaction);
    }

    // ============================================================
    // TRANSACTION HISTORY
    // ============================================================

    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactionHistory(
            Long accountId) {

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Account not found with id: " + accountId
                        ));

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
        User authenticatedUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new AccessDeniedException("Authenticated user not found"));

        if (!authenticatedUser.getId().equals(account.getUser().getId())) {
            throw new AccessDeniedException("You are not authorized to access this account");
        }

        List<Transaction> transactions =
                transactionRepository
                        .findByAccountIdOrderByCreatedAtDesc(
                                accountId
                        );

        return transactions.stream()
                .map(this::toTransactionResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> getTransactionHistory(Long accountId, int page, int size) {
        getAccount(accountId);
        var result = transactionRepository.findByAccountIdOrderByCreatedAtDesc(accountId, PageRequest.of(page, size));
        List<TransactionResponse> content = result.map(this::toTransactionResponse).getContent();
        return new PageResponse<>(content, page, size, result.getTotalElements(), result.getTotalPages());
    }

    @Transactional
    public TransactionResponse updateTransactionCategory(Long accountId, Long transactionId, String category) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + accountId));
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User authenticatedUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("Authenticated user not found"));
        if (!authenticatedUser.getId().equals(account.getUser().getId())) {
            throw new AccessDeniedException("You are not authorized to access this account");
        }
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
        if (!transaction.getAccount().getId().equals(account.getId())) {
            throw new AccessDeniedException("You are not authorized to update this transaction");
        }
        String normalized = transactionCategorizationService.normalizeSupportedCategory(category);
        if (!normalized.equals(category == null ? null : category.toUpperCase())) {
            throw new InvalidTransferException("Unsupported transaction category");
        }
        if (!"USER_SELECTED".equals(transaction.getCategorySource())) {
            transaction.setOriginalDetectedCategory(transaction.getCategory());
        }
        transaction.setCategory(normalized);
        transaction.setCategorySource("USER_SELECTED");
        transaction.setCategoryConfidence("HIGH");
        transaction.setCategoryReason("User selected category");
        transaction.setCategoryCorrectedAt(LocalDateTime.now());
        return toTransactionResponse(transactionRepository.save(transaction));
    }

    // ============================================================
    // VALIDATE RETRIED TRANSFER
    // ============================================================

    private void validateSameTransfer(
            Transaction previousTransaction,
            Account sender,
            String receiverAccountNumber,
            BigDecimal amount,
            String category,
            String description) {

        // Same sender
        if (!previousTransaction
                .getAccount()
                .getId()
                .equals(sender.getId())) {

            throw new IdempotencyConflictException(
                    "Idempotency key was already used by another account"
            );
        }

        // Same amount
        if (previousTransaction
                .getAmount()
                .compareTo(amount) != 0) {

            throw new IdempotencyConflictException(
                    "Idempotency key was already used with a different amount"
            );
        }

        // Same receiver
        if (!receiverAccountNumber.equals(
                previousTransaction
                        .getReceiverAccountNumber())) {

            throw new IdempotencyConflictException(
                    "Idempotency key was already used with a different receiver"
            );
        }

        if (!Objects.equals(category, previousTransaction.getCategory())) {
            throw new IdempotencyConflictException(
                    "Idempotency key was already used with a different category"
            );
        }

        if (!Objects.equals(description, previousTransaction.getDescription())) {
            throw new IdempotencyConflictException(
                    "Idempotency key was already used with a different description"
            );
        }
    }

    private String resolveTransferDescription(TransferRequest request) {
        return request.getDescription() == null || request.getDescription().isBlank()
                ? "Transfer to another account"
                : request.getDescription();
    }

    private String resolveCategory(String requestedCategory, String description, String transactionType) {
        return resolveCategorySuggestion(requestedCategory, description, transactionType).category();
    }

    private TransactionCategorizationService.CategorySuggestion resolveCategorySuggestion(
            String requestedCategory, String description, String transactionType) {
        if (requestedCategory != null && !requestedCategory.isBlank()) {
            String normalized = transactionCategorizationService.normalizeSupportedCategory(requestedCategory);
            if (!normalized.equals(requestedCategory.toUpperCase())) {
                throw new InvalidTransferException("Unsupported transaction category");
            }
            return new TransactionCategorizationService.CategorySuggestion(
                    normalized, "HIGH", "User selected category");
        }
        return transactionCategorizationService.categorize(description, transactionType);
    }

    private void applyCategorySuggestion(
            Transaction transaction,
            TransactionCategorizationService.CategorySuggestion suggestion,
            String requestedCategory) {
        transaction.setCategoryConfidence(suggestion.confidence());
        transaction.setCategoryReason(suggestion.reason());
        transaction.setCategorySource(requestedCategory == null || requestedCategory.isBlank()
                ? "AUTO_DETECTED"
                : "USER_SELECTED");
    }

    private List<Account> consolidateUserAccounts(Long userId) {
        List<Account> accounts = accountRepository.findByUserIdForUpdate(userId);
        List<Account> activeAccounts = accounts.stream()
                .filter(this::isActiveFinFlowAccount)
                .sorted(Comparator
                        .comparing(Account::isPrimary).reversed()
                        .thenComparing(Account::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Account::getId))
                .toList();
        if (activeAccounts.size() <= 1) {
            return accounts;
        }

        Account primary = activeAccounts.get(0);
        BigDecimal consolidatedBalance = primary.getBalance() == null ? BigDecimal.ZERO : primary.getBalance();

        for (int index = 1; index < activeAccounts.size(); index++) {
            Account duplicate = activeAccounts.get(index);
            consolidatedBalance = consolidatedBalance.add(
                    duplicate.getBalance() == null ? BigDecimal.ZERO : duplicate.getBalance());
            for (Transaction transaction : transactionRepository.findByAccountId(duplicate.getId())) {
                transaction.setAccount(primary);
            }
            duplicate.setBalance(BigDecimal.ZERO);
            duplicate.setPrimary(false);
            duplicate.setStatus("MERGED");
        }

        primary.setBalance(consolidatedBalance);
        primary.setPrimary(true);
        accountRepository.saveAll(accounts);
        return accounts;
    }

    private boolean isActiveFinFlowAccount(Account account) {
        return "ACTIVE".equalsIgnoreCase(account.getStatus());
    }

    // ============================================================
    // VALIDATE ACCOUNT STATUS
    // ============================================================

    private void validateAccountIsActive(
            Account account) {

        if (!"ACTIVE".equalsIgnoreCase(
                account.getStatus())) {

            throw new AccountInactiveException(
                    "Account is not active"
            );
        }
    }

    // ============================================================
    // GENERATE ACCOUNT NUMBER
    // ============================================================

    private String generateAccountNumber() {

        return "FF" +
                UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .substring(0, 12)
                        .toUpperCase();
    }

    // ============================================================
    // GENERATE TRANSACTION REFERENCE
    // ============================================================

    private String generateTransactionReference() {

        return "FF-TXN-" +
                UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .substring(0, 12)
                        .toUpperCase();
    }

    // ============================================================
    // ACCOUNT RESPONSE
    // ============================================================

    private AccountResponse toResponse(
            Account account) {

        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getBalance(),
                account.getAccountType(),
                account.getStatus(),
                account.isPrimary(),
                account.getCreatedAt()
        );
    }

    // ============================================================
    // TRANSACTION RESPONSE
    // ============================================================

    private TransactionResponse toTransactionResponse(
            Transaction transaction) {

        return new TransactionResponse(
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
                transaction.getBalanceAfter()
        );
    }

    // ============================================================
    // IDEMPOTENT TRANSFER RESPONSE
    // ============================================================

    private AccountResponse toResponseWithBalance(
            Account account,
            BigDecimal balance) {

        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                balance,
                account.getAccountType(),
                account.getStatus(),
                account.isPrimary(),
                account.getCreatedAt()
        );
    }
}
