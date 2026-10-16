package com.finflow.service;

import org.springframework.data.domain.Pageable;
import com.finflow.dto.PageResponseMapper;
import com.finflow.dto.PageResponse;
import com.finflow.bank.BankAccountProvider;
import com.finflow.dto.BankLinkInitiateRequest;
import com.finflow.dto.BankLinkVerifyRequest;
import com.finflow.dto.LinkedBankAccountResponse;
import com.finflow.entity.LinkedBankAccount;
import com.finflow.entity.User;
import com.finflow.exception.InvalidMoneyRuleException;
import com.finflow.exception.ResourceAlreadyExistsException;
import com.finflow.exception.ResourceNotFoundException;
import com.finflow.repository.FinanceNotificationRepository;
import com.finflow.repository.LinkedBankAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class LinkedBankAccountService {
    private final CurrentUserService currentUserService;
    private final LinkedBankAccountRepository repository;
    private final BankAccountProvider provider;
    private final FinanceNotificationRepository notificationRepository;
    private final SecurityAuditService auditService;

    public LinkedBankAccountService(CurrentUserService currentUserService,
                                    LinkedBankAccountRepository repository,
                                    BankAccountProvider provider,
                                    FinanceNotificationRepository notificationRepository,
                                    SecurityAuditService auditService) {
        this.currentUserService = currentUserService;
        this.repository = repository;
        this.provider = provider;
        this.notificationRepository = notificationRepository;
        this.auditService = auditService;
    }

    @Transactional
    public LinkedBankAccountResponse initiate(BankLinkInitiateRequest request) {
        User user = currentUserService.get();
        String bankCode = request.bankCode().trim().toUpperCase(Locale.ROOT);
        String masked = maskAccountNumber(request.accountNumber());
        if (repository.existsByUserIdAndBankCodeAndMaskedAccountNumberAndStatusNot(
                user.getId(), bankCode, masked, "DISCONNECTED")) {
            throw new ResourceAlreadyExistsException("This bank account is already linked or awaiting verification");
        }

        BankAccountProvider.LinkResult result = provider.initiateLink(new BankAccountProvider.BankLinkCommand(
                request.bankName().trim(),
                bankCode,
                request.accountHolderName().trim(),
                request.accountNumber().trim(),
                request.accountType().trim().toUpperCase(Locale.ROOT)));

        LocalDateTime now = LocalDateTime.now();
        LinkedBankAccount account = new LinkedBankAccount();
        account.setUser(user);
        account.setBankName(request.bankName().trim());
        account.setBankCode(bankCode);
        account.setAccountHolderName(request.accountHolderName().trim());
        account.setMaskedAccountNumber(masked);
        account.setAccountType(request.accountType().trim().toUpperCase(Locale.ROOT));
        account.setProviderName(provider.providerName());
        account.setProviderLinkReference(result.providerLinkReference());
        account.setProviderAccountReference(result.providerAccountReference());
        account.setStatus(result.status());
        account.setVerificationHint(result.verificationHint());
        account.setCreatedAt(now);
        account.setUpdatedAt(now);

        LinkedBankAccount saved = repository.save(account);
        notificationRepository.createIfAbsent(user.getId(), "BANK_LINK_STARTED:" + saved.getId(),
                "BANK_LINK_STARTED", "Bank link started",
                "Sandbox verification started for " + saved.getBankName() + " " + saved.getMaskedAccountNumber() + ".",
                "INFO");
        auditService.log(user, "BANK_LINK_INITIATED", "LinkedBankAccount/" + saved.getId(),
                saved.getProviderLinkReference(), "MEDIUM", "PENDING",
                "Sandbox bank-link flow initiated; no bank password, PIN, or OTP stored.");

        return toResponse(saved);
    }

    @Transactional
    public LinkedBankAccountResponse verify(Long id, BankLinkVerifyRequest request) {
        User user = currentUserService.get();
        LinkedBankAccount account = findOwned(user, id);
        if (!"PENDING".equalsIgnoreCase(account.getStatus()) && !"FAILED".equalsIgnoreCase(account.getStatus())) {
            throw new InvalidMoneyRuleException("Only pending or failed bank links can be verified");
        }

        BankAccountProvider.VerificationResult result = provider.verifyLink(
                account.getProviderLinkReference(), request.sandboxConfirmation());
        account.setStatus(result.status());
        account.setFailureReason("FAILED".equalsIgnoreCase(result.status()) ? result.message() : null);
        account.setUpdatedAt(LocalDateTime.now());
        if ("VERIFIED".equalsIgnoreCase(result.status())) {
            account.setVerifiedAt(LocalDateTime.now());
        }

        LinkedBankAccount saved = repository.save(account);
        notificationRepository.createIfAbsent(user.getId(), "BANK_LINK_VERIFIED:" + saved.getId() + ":" + saved.getStatus(),
                "BANK_LINK_" + saved.getStatus(), "Bank link " + saved.getStatus().toLowerCase(Locale.ROOT),
                "Bank link for " + saved.getBankName() + " " + saved.getMaskedAccountNumber() + " is " + saved.getStatus() + ".",
                "VERIFIED".equalsIgnoreCase(saved.getStatus()) ? "INFO" : "WARNING");
        auditService.log(user, "BANK_LINK_VERIFY", "LinkedBankAccount/" + saved.getId(),
                saved.getProviderLinkReference(), "MEDIUM", saved.getStatus(), result.message());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<LinkedBankAccountResponse> list() {
        User user = currentUserService.get();
        return repository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<LinkedBankAccountResponse> list(Pageable pageable) {
        User user = currentUserService.get();
        return PageResponseMapper.map(repository.findByUserIdOrderByCreatedAtDescIdDesc(user.getId(), pageable), this::toResponse);
    }

    @Transactional(readOnly = true)
    public LinkedBankAccountResponse get(Long id) {
        return toResponse(findOwned(currentUserService.get(), id));
    }

    @Transactional
    public LinkedBankAccountResponse refreshStatus(Long id) {
        User user = currentUserService.get();
        LinkedBankAccount account = findOwned(user, id);
        BankAccountProvider.VerificationResult result = provider.getAccountStatus(account.getProviderLinkReference());
        account.setStatus(result.status());
        account.setUpdatedAt(LocalDateTime.now());
        if ("VERIFIED".equalsIgnoreCase(result.status()) && account.getVerifiedAt() == null) {
            account.setVerifiedAt(LocalDateTime.now());
        }
        auditService.log(user, "BANK_LINK_STATUS", "LinkedBankAccount/" + account.getId(),
                account.getProviderLinkReference(), "LOW", result.status(), result.message());
        return toResponse(repository.save(account));
    }

    @Transactional
    public LinkedBankAccountResponse disconnect(Long id) {
        User user = currentUserService.get();
        LinkedBankAccount account = findOwned(user, id);
        if ("DISCONNECTED".equalsIgnoreCase(account.getStatus())) {
            return toResponse(account);
        }
        BankAccountProvider.VerificationResult result = provider.unlinkAccount(account.getProviderLinkReference());
        account.setStatus("DISCONNECTED".equalsIgnoreCase(result.status()) ? "DISCONNECTED" : "FAILED");
        account.setFailureReason("FAILED".equalsIgnoreCase(account.getStatus()) ? result.message() : null);
        account.setUpdatedAt(LocalDateTime.now());
        account.setDisconnectedAt(LocalDateTime.now());
        LinkedBankAccount saved = repository.save(account);
        auditService.log(user, "BANK_LINK_DISCONNECT", "LinkedBankAccount/" + saved.getId(),
                saved.getProviderLinkReference(), "MEDIUM", saved.getStatus(), result.message());
        return toResponse(saved);
    }

    private LinkedBankAccount findOwned(User user, Long id) {
        return repository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Linked bank account not found"));
    }

    private String maskAccountNumber(String accountNumber) {
        String clean = accountNumber.trim();
        String last4 = clean.substring(clean.length() - 4);
        return "••••" + last4;
    }

    private LinkedBankAccountResponse toResponse(LinkedBankAccount account) {
        return new LinkedBankAccountResponse(
                account.getId(),
                account.getBankName(),
                account.getBankCode(),
                account.getAccountHolderName(),
                account.getMaskedAccountNumber(),
                account.getAccountType(),
                account.getProviderName(),
                account.getProviderLinkReference(),
                account.getStatus(),
                account.getVerificationHint(),
                account.getFailureReason(),
                account.getProviderName() != null && account.getProviderName().toUpperCase(Locale.ROOT).contains("SANDBOX"),
                account.getCreatedAt(),
                account.getUpdatedAt(),
                account.getVerifiedAt(),
                account.getDisconnectedAt());
    }
}
