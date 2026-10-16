package com.finflow.service;

import com.finflow.dto.WalletFundingRequest;
import com.finflow.dto.WalletTransactionResponse;
import com.finflow.dto.WalletTransferRequest;
import com.finflow.entity.Account;
import com.finflow.entity.Transaction;
import com.finflow.entity.User;
import com.finflow.entity.Wallet;
import com.finflow.entity.WalletTransaction;
import com.finflow.exception.AccountInactiveException;
import com.finflow.exception.IdempotencyConflictException;
import com.finflow.exception.InsufficientBalanceException;
import com.finflow.exception.InvalidTransferException;
import com.finflow.exception.ResourceNotFoundException;
import com.finflow.repository.AccountRepository;
import com.finflow.repository.TransactionRepository;
import com.finflow.repository.WalletRepository;
import com.finflow.repository.WalletTransactionRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class WalletLedgerService {
    private final CurrentUserService currentUserService;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final MoneyManagementService moneyManagementService;
    private final PinVerificationService pinVerificationService;
    private final MoneyNormalizer moneyNormalizer;

    public WalletLedgerService(
            CurrentUserService currentUserService,
            WalletRepository walletRepository,
            WalletTransactionRepository walletTransactionRepository,
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            MoneyManagementService moneyManagementService,
            PinVerificationService pinVerificationService,
            MoneyNormalizer moneyNormalizer) {
        this.currentUserService = currentUserService;
        this.walletRepository = walletRepository;
        this.walletTransactionRepository = walletTransactionRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.moneyManagementService = moneyManagementService;
        this.pinVerificationService = pinVerificationService;
        this.moneyNormalizer = moneyNormalizer;
    }

    @Transactional
    public WalletTransactionResponse addFunds(
            WalletFundingRequest request, String idempotencyKey) {
        validateIdempotencyKey(idempotencyKey, 70);
        BigDecimal amount = moneyNormalizer.normalizePositive(request.amount());
        User user = currentUserService.get();
        Account account = accountRepository.findByIdForUpdate(request.accountId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        if (!account.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You are not authorized to fund this wallet from that account");
        }
        moneyManagementService.getWallet();
        Wallet wallet = walletRepository.findByUserIdForUpdate(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("FinFlow wallet not found"));
        requireActiveWallet(wallet);

        String storedKey = "WF:" + wallet.getId() + ":" + idempotencyKey;
        WalletTransaction previous = walletTransactionRepository
                .findByWalletIdAndIdempotencyKey(wallet.getId(), storedKey).orElse(null);
        if (previous != null) {
            if (previous.getSourceAccount() == null
                    || !previous.getSourceAccount().getId().equals(account.getId())
                    || previous.getAmount().compareTo(amount) != 0
                    || !sameDescription(previous.getDescription(), request.description())) {
                throw new IdempotencyConflictException(
                        "Idempotency key was already used for a different wallet funding request");
            }
            return toResponse(previous);
        }
        if (!"ACTIVE".equalsIgnoreCase(account.getStatus())) {
            throw new AccountInactiveException("Account is not active");
        }
        moneyManagementService.assertSpendAllowed(account, amount, "OTHER");
        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Insufficient account balance");
        }

        account.setBalance(account.getBalance().subtract(amount));
        wallet.setBalance(wallet.getBalance().add(amount));
        wallet.setUpdatedAt(LocalDateTime.now());

        LocalDateTime now = LocalDateTime.now();
        String paymentReference = paymentReference();
        Transaction accountTransaction = new Transaction();
        accountTransaction.setReferenceNumber(paymentReference);
        accountTransaction.setAmount(amount);
        accountTransaction.setType("WITHDRAWAL");
        accountTransaction.setCategory("OTHER");
        accountTransaction.setDescription(request.description() == null || request.description().isBlank()
                ? "FinFlow wallet funding"
                : "FinFlow wallet funding: " + request.description());
        accountTransaction.setStatus("SUCCESS");
        accountTransaction.setCreatedAt(now);
        accountTransaction.setIdempotencyKey(storedKey);
        accountTransaction.setBalanceAfter(account.getBalance());
        accountTransaction.setAccount(account);
        transactionRepository.save(accountTransaction);

        WalletTransaction entry = new WalletTransaction();
        entry.setWallet(wallet);
        entry.setSourceAccount(account);
        entry.setType("FUNDING");
        entry.setAmount(amount);
        entry.setBalanceAfter(wallet.getBalance());
        entry.setPaymentReference(paymentReference);
        entry.setIdempotencyKey(storedKey);
        entry.setStatus("SUCCESS");
        entry.setDescription(request.description());
        entry.setCreatedAt(now);
        return toResponse(walletTransactionRepository.save(entry));
    }

    @Transactional
    public WalletTransactionResponse transfer(
            WalletTransferRequest request, String idempotencyKey) {
        validateIdempotencyKey(idempotencyKey, 88);
        BigDecimal amount = moneyNormalizer.normalizePositive(request.amount());
        User user = currentUserService.get();
        moneyManagementService.getWallet();
        Wallet currentWallet = walletRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("FinFlow wallet not found"));
        Wallet receiverSnapshot = walletRepository.findByWalletIdentifier(
                        request.receiverWalletIdentifier().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Receiver wallet not found"));
        if (currentWallet.getId().equals(receiverSnapshot.getId())) {
            throw new InvalidTransferException("Sender and receiver wallets must be different");
        }

        Wallet first = walletRepository.findByIdForUpdate(
                        Math.min(currentWallet.getId(), receiverSnapshot.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("FinFlow wallet not found"));
        Wallet second = walletRepository.findByIdForUpdate(
                        Math.max(currentWallet.getId(), receiverSnapshot.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Receiver wallet not found"));
        Wallet sender = first.getId().equals(currentWallet.getId()) ? first : second;
        Wallet receiver = first.getId().equals(receiverSnapshot.getId()) ? first : second;

        if (!sender.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You are not authorized to use this wallet");
        }
        requireActiveWallet(sender);
        requireActiveWallet(receiver);
        pinVerificationService.verify(user, request.pin());

        WalletTransaction previous = walletTransactionRepository
                .findByWalletIdAndIdempotencyKey(sender.getId(), idempotencyKey).orElse(null);
        if (previous != null) {
            if (!"TRANSFER_OUT".equals(previous.getType())
                    || previous.getCounterpartyWallet() == null
                    || !previous.getCounterpartyWallet().getId().equals(receiver.getId())
                    || previous.getAmount().compareTo(amount) != 0
                    || !sameDescription(previous.getDescription(), request.description())) {
                throw new IdempotencyConflictException(
                        "Idempotency key was already used for a different wallet transfer");
            }
            return toResponse(previous);
        }
        if (sender.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Insufficient wallet balance");
        }

        sender.setBalance(sender.getBalance().subtract(amount));
        receiver.setBalance(receiver.getBalance().add(amount));
        LocalDateTime now = LocalDateTime.now();
        String reference = paymentReference();

        WalletTransaction debit = new WalletTransaction();
        debit.setWallet(sender);
        debit.setCounterpartyWallet(receiver);
        debit.setType("TRANSFER_OUT");
        debit.setAmount(amount);
        debit.setBalanceAfter(sender.getBalance());
        debit.setPaymentReference(reference);
        debit.setIdempotencyKey(idempotencyKey);
        debit.setStatus("SUCCESS");
        debit.setDescription(request.description());
        debit.setCreatedAt(now);
        walletTransactionRepository.save(debit);

        WalletTransaction credit = new WalletTransaction();
        credit.setWallet(receiver);
        credit.setCounterpartyWallet(sender);
        credit.setType("TRANSFER_IN");
        credit.setAmount(amount);
        credit.setBalanceAfter(receiver.getBalance());
        credit.setPaymentReference(reference);
        credit.setIdempotencyKey("TRANSFER_IN:" + idempotencyKey);
        credit.setStatus("SUCCESS");
        credit.setDescription(request.description());
        credit.setCreatedAt(now);
        walletTransactionRepository.save(credit);

        sender.setUpdatedAt(now);
        receiver.setUpdatedAt(now);
        return toResponse(debit);
    }

    @Transactional(readOnly = true)
    public List<WalletTransactionResponse> history() {
        Long userId = currentUserService.get().getId();
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("FinFlow wallet not found"));
        return walletTransactionRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId())
                .stream().map(this::toResponse).toList();
    }

    private WalletTransactionResponse toResponse(WalletTransaction entry) {
        return new WalletTransactionResponse(
                entry.getId(),
                entry.getPaymentReference(),
                entry.getType(),
                entry.getAmount(),
                entry.getBalanceAfter(),
                entry.getWallet().getCurrency(),
                entry.getStatus(),
                entry.getDescription(),
                entry.getCounterpartyWallet() == null
                        ? null : entry.getCounterpartyWallet().getWalletIdentifier(),
                entry.getSourceAccount() == null ? null : entry.getSourceAccount().getId(),
                entry.getCreatedAt());
    }

    private void requireActiveWallet(Wallet wallet) {
        if (!"ACTIVE".equalsIgnoreCase(wallet.getStatus())) {
            throw new InvalidTransferException("Wallet is not active");
        }
    }

    private void validateIdempotencyKey(String key, int maxLength) {
        if (key == null || key.isBlank() || key.length() > maxLength) {
            throw new InvalidTransferException(
                    "Idempotency-Key is required and must not exceed " + maxLength + " characters");
        }
    }

    private boolean sameDescription(String original, String requested) {
        return (original == null ? "" : original).equals(requested == null ? "" : requested);
    }

    private String paymentReference() {
        return "WLT-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }
}
