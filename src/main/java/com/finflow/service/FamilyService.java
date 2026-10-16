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

@Service
public class FamilyService {
    private static final List<String> CATEGORIES =
            List.of("FOOD", "GROCERIES", "TRANSPORT", "SHOPPING", "BILLS",
                    "ENTERTAINMENT", "HEALTH", "EDUCATION", "OTHER");

    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final FamilyRepository familyRepository;
    private final FamilyMemberRepository memberRepository;
    private final FamilyCategoryLimitRepository categoryLimitRepository;
    private final FamilyWalletTransactionRepository familyTransactionRepository;
    private final MoneyReservationRepository reservationRepository;
    private final AccountRepository accountRepository;
    private final MoneyManagementService moneyManagementService;
    private final PinVerificationService pinVerificationService;

    public FamilyService(CurrentUserService currentUserService,
                         UserRepository userRepository,
                         FamilyRepository familyRepository,
                         FamilyMemberRepository memberRepository,
                         FamilyCategoryLimitRepository categoryLimitRepository,
                         FamilyWalletTransactionRepository familyTransactionRepository,
                         MoneyReservationRepository reservationRepository,
                         AccountRepository accountRepository,
                         MoneyManagementService moneyManagementService,
                         PinVerificationService pinVerificationService) {
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
        this.familyRepository = familyRepository;
        this.memberRepository = memberRepository;
        this.categoryLimitRepository = categoryLimitRepository;
        this.familyTransactionRepository = familyTransactionRepository;
        this.reservationRepository = reservationRepository;
        this.accountRepository = accountRepository;
        this.moneyManagementService = moneyManagementService;
        this.pinVerificationService = pinVerificationService;
    }

    @Transactional
    public FamilyResponse createOrGetFamily() {
        User user = lockCurrentUser();
        Family family = familyRepository.findByParentId(user.getId()).orElseGet(() -> {
            Family created = new Family();
            created.setParent(user);
            created.setCreatedAt(LocalDateTime.now());
            return familyRepository.save(created);
        });
        return toFamilyResponse(family);
    }

    @Transactional
    public FamilyResponse getFamily() {
        User user = currentUserService.get();
        Family family = familyRepository.findByParentId(user.getId()).orElseGet(() -> {
            Family created = new Family();
            created.setParent(user);
            created.setCreatedAt(LocalDateTime.now());
            return familyRepository.save(created);
        });
        return toFamilyResponse(family);
    }

    @Transactional
    public FamilyMemberResponse createMember(FamilyMemberRequest request) {
        User user = lockCurrentUser();
        Family family = familyRepository.findByParentId(user.getId()).orElseGet(() -> {
            Family created = new Family();
            created.setParent(user);
            created.setCreatedAt(LocalDateTime.now());
            return familyRepository.save(created);
        });
        FamilyMember member = new FamilyMember();
        member.setFamily(family);
        member.setParent(user);
        member.setDisplayName(request.displayName().trim());
        member.setMonthlyAllowance(request.monthlyAllowance().setScale(2, RoundingMode.UNNECESSARY));
        member.setApprovalRequired(request.approvalRequired());
        member.setStatus("ACTIVE");
        member.setCreatedAt(LocalDateTime.now());
        member.setUpdatedAt(LocalDateTime.now());
        return toMemberResponse(memberRepository.save(member));
    }

    @Transactional(readOnly = true)
    public List<FamilyMemberResponse> listMembers() {
        Long parentId = currentUserService.get().getId();
        return memberRepository.findByParentIdAndStatusNotOrderByCreatedAtDesc(parentId, "DEACTIVATED")
                .stream().map(this::toMemberResponse).toList();
    }

    @Transactional(readOnly = true)
    public FamilyMemberResponse getMember(Long memberId) {
        return toMemberResponse(findOwnedMember(memberId));
    }

    @Transactional
    public FamilyMemberResponse updateMember(Long memberId, FamilyMemberRequest request) {
        User user = lockCurrentUser();
        FamilyMember member = memberRepository.findByIdAndParentId(memberId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Family member not found"));
        member.setDisplayName(request.displayName().trim());
        member.setMonthlyAllowance(request.monthlyAllowance().setScale(2, RoundingMode.UNNECESSARY));
        member.setApprovalRequired(request.approvalRequired());
        member.setUpdatedAt(LocalDateTime.now());
        return toMemberResponse(memberRepository.save(member));
    }

    @Transactional
    public FamilyMemberResponse setFrozen(Long memberId, boolean frozen) {
        User user = lockCurrentUser();
        FamilyMember member = memberRepository.findByIdAndParentId(memberId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Family member not found"));
        member.setStatus(frozen ? "FROZEN" : "ACTIVE");
        member.setUpdatedAt(LocalDateTime.now());
        return toMemberResponse(memberRepository.save(member));
    }

    @Transactional
    public FamilyWalletTransactionResponse addAllowance(
            Long memberId, FamilyAllowanceRequest request, String idempotencyKey) {
        validateIdempotencyKey(idempotencyKey);
        User user = currentUserService.get();
        FamilyMember member = memberRepository.findByIdAndParentId(memberId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Family member not found"));
        FamilyWalletTransaction prior = familyTransactionRepository
                .findByFamilyMemberIdAndIdempotencyKey(memberId, "ALLOWANCE:" + idempotencyKey)
                .orElse(null);
        if (prior != null) {
            if (!prior.getAccount().getId().equals(request.accountId())
                    || prior.getAmount().compareTo(request.amount()) != 0) {
                throw new IdempotencyConflictException(
                        "Idempotency key was already used for a different allowance");
            }
            return toFamilyTransactionResponse(prior);
        }
        if (!"ACTIVE".equals(member.getStatus())) {
            throw new InvalidMoneyRuleException("A frozen controlled wallet cannot receive an allowance");
        }
        Account account = accountRepository.findById(request.accountId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        if (!account.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You are not authorized to fund this controlled wallet");
        }
        MoneyReservationResponse reservation = moneyManagementService.reserveFamilyAllowance(
                account, member, request.amount(),
                request.description() == null || request.description().isBlank()
                        ? "Allowance for " + member.getDisplayName()
                        : request.description());
        FamilyWalletTransaction transaction = new FamilyWalletTransaction();
        transaction.setFamilyMember(member);
        transaction.setParent(user);
        transaction.setAccount(account);
        transaction.setReservation(findReservation(reservation.id()));
        transaction.setType("ALLOWANCE");
        transaction.setAmount(request.amount().setScale(2, RoundingMode.UNNECESSARY));
        transaction.setIdempotencyKey("ALLOWANCE:" + idempotencyKey);
        transaction.setDescription(request.description());
        transaction.setStatus("SUCCESS");
        transaction.setCreatedAt(LocalDateTime.now());
        return toFamilyTransactionResponse(familyTransactionRepository.save(transaction));
    }

    @Transactional
    public FamilyMember consumeAllowance(Long memberId, Account account,
                                         FamilySpendRequest request, String idempotencyKey) {
        User user = currentUserService.get();
        FamilyMember member = memberRepository.findByIdAndParentId(memberId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Family member not found"));
        if (!"ACTIVE".equals(member.getStatus())) {
            throw new InvalidMoneyRuleException("This controlled wallet is frozen");
        }
        if (member.isApprovalRequired() && !request.parentApproved()) {
            throw new InvalidTransferException("Parent approval is required for this controlled-wallet spend");
        }
        if (!account.getId().equals(request.accountId())) {
            throw new InvalidTransferException("The spend account does not match the request");
        }
        LocalDateTime start = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime end = start.plusMonths(1);
        BigDecimal spent = zero(familyTransactionRepository.sumMonthlySpends(memberId, start, end));
        if (spent.add(request.amount()).compareTo(member.getMonthlyAllowance()) > 0) {
            throw new SpendingLimitExceededException(
                    "This spend would exceed the controlled wallet's monthly allowance.");
        }
        String category = request.category().toUpperCase(Locale.ROOT);
        if (!CATEGORIES.contains(category)) {
            throw new InvalidMoneyRuleException("Unsupported family spending category");
        }
        FamilyCategoryLimit limit = categoryLimitRepository
                .findByFamilyMemberIdAndCategory(memberId, category).orElse(null);
        if (limit != null) {
            BigDecimal categorySpent = zero(familyTransactionRepository
                    .sumMonthlySpendsByCategory(memberId, category, start, end));
            if (categorySpent.add(request.amount()).compareTo(limit.getMonthlyAmount()) > 0) {
                throw new SpendingLimitExceededException(
                        "This spend would exceed the controlled wallet's " + category + " limit.");
            }
        }
        moneyManagementService.consumeFamilyAllowance(account, member, request.amount());
        return member;
    }

    @Transactional(readOnly = true)
    public void validateSpendRetry(Long memberId, String idempotencyKey,
                                   FamilySpendRequest request, Transaction transaction) {
        User user = currentUserService.get();
        FamilyMember member = memberRepository.findByIdAndParentId(memberId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Family member not found"));
        FamilyWalletTransaction prior = familyTransactionRepository
                .findByFamilyMemberIdAndIdempotencyKey(memberId, "SPEND:" + idempotencyKey)
                .orElseThrow(() -> new IdempotencyConflictException(
                        "Idempotency key was already used for a different financial operation"));
        if (!prior.getParent().getId().equals(user.getId())
                || !prior.getAccount().getId().equals(request.accountId())
                || prior.getAmount().compareTo(request.amount()) != 0
                || !request.category().equalsIgnoreCase(prior.getCategory())
                || !java.util.Objects.equals(request.description(), prior.getDescription())
                || prior.getAccountTransaction() == null
                || !prior.getAccountTransaction().getId().equals(transaction.getId())) {
            throw new IdempotencyConflictException(
                    "Idempotency key was already used for a different controlled-wallet spend");
        }
    }

    @Transactional
    public void recordSpend(Long memberId, String idempotencyKey, Account account,
                            FamilySpendRequest request, Transaction accountTransaction) {
        User user = currentUserService.get();
        FamilyMember member = memberRepository.findByIdAndParentId(memberId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Family member not found"));
        FamilyWalletTransaction transaction = new FamilyWalletTransaction();
        transaction.setFamilyMember(member);
        transaction.setParent(user);
        transaction.setAccount(account);
        transaction.setAccountTransaction(accountTransaction);
        transaction.setType("SPEND");
        transaction.setAmount(request.amount().setScale(2, RoundingMode.UNNECESSARY));
        transaction.setCategory(request.category().toUpperCase(Locale.ROOT));
        transaction.setIdempotencyKey("SPEND:" + idempotencyKey);
        transaction.setDescription(request.description());
        transaction.setStatus("SUCCESS");
        transaction.setCreatedAt(accountTransaction.getCreatedAt());
        familyTransactionRepository.save(transaction);
    }

    @Transactional
    public FamilyCategoryLimitResponse upsertCategoryLimit(
            Long memberId, FamilyCategoryLimitRequest request) {
        User user = lockCurrentUser();
        pinVerificationService.verify(user, request.pin());
        FamilyMember member = memberRepository.findByIdAndParentId(memberId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Family member not found"));
        String category = request.category().toUpperCase(Locale.ROOT);
        FamilyCategoryLimit limit = categoryLimitRepository
                .findByFamilyMemberIdAndCategory(memberId, category)
                .orElseGet(FamilyCategoryLimit::new);
        limit.setFamilyMember(member);
        limit.setCategory(category);
        limit.setMonthlyAmount(request.monthlyAmount().setScale(2, RoundingMode.UNNECESSARY));
        limit.setCreatedAt(limit.getCreatedAt() == null ? LocalDateTime.now() : limit.getCreatedAt());
        return toCategoryLimit(categoryLimitRepository.save(limit));
    }

    @Transactional
    public void deleteCategoryLimit(Long memberId, Long limitId) {
        User user = lockCurrentUser();
        memberRepository.findByIdAndParentId(memberId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Family member not found"));
        FamilyCategoryLimit limit = categoryLimitRepository.findByIdAndFamilyMemberId(limitId, memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Family category limit not found"));
        categoryLimitRepository.delete(limit);
    }

    @Transactional
    public void deleteMember(Long memberId) {
        User user = lockCurrentUser();
        FamilyMember member = memberRepository.findByIdAndParentId(memberId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Family member not found"));
        member.setStatus("DEACTIVATED");
        member.setUpdatedAt(LocalDateTime.now());
        memberRepository.save(member);
    }

    @Transactional(readOnly = true)
    public List<FamilyWalletTransactionResponse> transactions(Long memberId) {
        findOwnedMember(memberId);
        return familyTransactionRepository.findByFamilyMemberIdOrderByCreatedAtDesc(memberId)
                .stream().map(this::toFamilyTransactionResponse).toList();
    }

    private FamilyMember findOwnedMember(Long memberId) {
        User user = currentUserService.get();
        return memberRepository.findByIdAndParentId(memberId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Family member not found"));
    }

    private FamilyMemberResponse toMemberResponse(FamilyMember member) {
        LocalDateTime start = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime end = start.plusMonths(1);
        BigDecimal spent = zero(familyTransactionRepository.sumMonthlySpends(member.getId(), start, end));
        List<FamilyCategoryLimitResponse> limits =
                categoryLimitRepository.findByFamilyMemberIdOrderByCategoryAsc(member.getId())
                        .stream().map(this::toCategoryLimit).toList();
        return new FamilyMemberResponse(
                member.getId(), member.getDisplayName(), member.getMonthlyAllowance(), spent,
                member.getMonthlyAllowance().subtract(spent).max(BigDecimal.ZERO),
                zero(reservationRepository.sumActiveByFamilyMemberId(member.getId())),
                member.getStatus(), member.isApprovalRequired(), limits,
                member.getCreatedAt(), member.getUpdatedAt());
    }

    private FamilyCategoryLimitResponse toCategoryLimit(FamilyCategoryLimit limit) {
        LocalDateTime start = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime end = start.plusMonths(1);
        BigDecimal spent = zero(familyTransactionRepository.sumMonthlySpendsByCategory(
                limit.getFamilyMember().getId(), limit.getCategory(), start, end));
        return new FamilyCategoryLimitResponse(limit.getId(), limit.getCategory(),
                limit.getMonthlyAmount(), spent, limit.getMonthlyAmount().subtract(spent).max(BigDecimal.ZERO));
    }

    private FamilyWalletTransactionResponse toFamilyTransactionResponse(
            FamilyWalletTransaction transaction) {
        String reference = transaction.getAccountTransaction() == null
                ? null : transaction.getAccountTransaction().getReferenceNumber();
        return new FamilyWalletTransactionResponse(
                transaction.getId(), transaction.getFamilyMember().getId(), transaction.getType(),
                transaction.getAmount(), transaction.getCategory(), transaction.getStatus(),
                reference, transaction.getDescription(), transaction.getCreatedAt());
    }

    private FamilyResponse toFamilyResponse(Family family) {
        return new FamilyResponse(family.getId(),
                memberRepository.findByParentIdAndStatusNotOrderByCreatedAtDesc(family.getParent().getId(), "DEACTIVATED").size(),
                family.getCreatedAt());
    }

    private MoneyReservation findReservation(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Allowance reservation not found"));
    }

    private User lockCurrentUser() {
        User current = currentUserService.get();
        return userRepository.findByIdForUpdate(current.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }

    private void validateIdempotencyKey(String key) {
        if (key == null || key.isBlank() || key.length() > 90) {
            throw new InvalidTransferException("Idempotency-Key is required and must not exceed 90 characters");
        }
    }

    private BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
