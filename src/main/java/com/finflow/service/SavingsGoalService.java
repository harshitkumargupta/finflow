package com.finflow.service;

import org.springframework.data.domain.Pageable;
import com.finflow.dto.PageResponseMapper;
import com.finflow.dto.PageResponse;
import com.finflow.dto.GoalContributionRequest;
import com.finflow.dto.SavingsGoalRequest;
import com.finflow.dto.SavingsGoalResponse;
import com.finflow.entity.Account;
import com.finflow.entity.AutopayAuthorization;
import com.finflow.entity.SavingsGoal;
import com.finflow.entity.User;
import com.finflow.exception.ResourceNotFoundException;
import com.finflow.repository.AccountRepository;
import com.finflow.repository.AutopayAuthorizationRepository;
import com.finflow.repository.SavingsGoalRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class SavingsGoalService {
    private final SavingsGoalRepository goalRepository;
    private final AccountRepository accountRepository;
    private final AutopayAuthorizationRepository autopayRepository;
    private final CurrentUserService currentUserService;
    private final GoalLedgerService goalLedgerService;

    public SavingsGoalService(SavingsGoalRepository goalRepository,
                              AccountRepository accountRepository,
                              AutopayAuthorizationRepository autopayRepository,
                              CurrentUserService currentUserService,
                              GoalLedgerService goalLedgerService) {
        this.goalRepository = goalRepository;
        this.accountRepository = accountRepository;
        this.autopayRepository = autopayRepository;
        this.currentUserService = currentUserService;
        this.goalLedgerService = goalLedgerService;
    }

    @Transactional
    public SavingsGoalResponse create(SavingsGoalRequest request) {
        User user = currentUserService.get();
        validateDate(request.getTargetDate());
        SavingsGoal goal = new SavingsGoal();
        goal.setUser(user);
        apply(goal, request, user);
        goal.setCreatedAt(LocalDateTime.now());
        goal.setUpdatedAt(LocalDateTime.now());
        SavingsGoal saved = goalRepository.save(goal);
        goalLedgerService.record(saved, saved.getCurrentAmount(), "INITIAL", null);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<SavingsGoalResponse> list() {
        User user = currentUserService.get();
        return goalRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<SavingsGoalResponse> list(Pageable pageable) {
        User user = currentUserService.get();
        return PageResponseMapper.map(goalRepository.findByUserIdOrderByCreatedAtDescIdDesc(user.getId(), pageable), this::toResponse);
    }

    @Transactional(readOnly = true)
    public SavingsGoalResponse get(Long id) {
        return toResponse(findOwned(id));
    }

    @Transactional
    public SavingsGoalResponse update(Long id, SavingsGoalRequest request) {
        validateDate(request.getTargetDate());
        SavingsGoal goal = findOwned(id);
        BigDecimal previousAmount = goal.getCurrentAmount();
        apply(goal, request, goal.getUser());
        goal.setUpdatedAt(LocalDateTime.now());
        SavingsGoal saved = goalRepository.save(goal);
        goalLedgerService.record(saved, saved.getCurrentAmount().subtract(previousAmount), "ADJUSTMENT", null);
        return toResponse(saved);
    }

    @Transactional
    public SavingsGoalResponse contribute(Long id, GoalContributionRequest request) {
        SavingsGoal goal = findOwned(id);
        BigDecimal nextAmount = goal.getCurrentAmount().add(request.getAmount());
        if (nextAmount.compareTo(goal.getTargetAmount()) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Goal contributions cannot exceed the target amount");
        }
        goal.setCurrentAmount(nextAmount);
        goal.setStatus(goal.getCurrentAmount().compareTo(goal.getTargetAmount()) >= 0
                ? "COMPLETED" : "ACTIVE");
        goal.setUpdatedAt(LocalDateTime.now());
        SavingsGoal saved = goalRepository.save(goal);
        goalLedgerService.record(saved, request.getAmount(), "MANUAL", null);
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        goalRepository.delete(findOwned(id));
    }

    private SavingsGoal findOwned(Long id) {
        User user = currentUserService.get();
        return goalRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Savings goal not found"));
    }

    private void validateDate(LocalDate targetDate) {
        if (targetDate != null && !targetDate.isAfter(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Target date must be in the future");
        }
    }

    private void apply(SavingsGoal goal, SavingsGoalRequest request, User user) {
        if (request.getCurrentAmount().signum() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current amount cannot be negative");
        }
        if (request.getCurrentAmount().compareTo(request.getTargetAmount()) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current amount cannot exceed the target amount");
        }
        goal.setName(request.getName().trim());
        goal.setTargetAmount(request.getTargetAmount());
        goal.setCurrentAmount(request.getCurrentAmount());
        goal.setTargetDate(request.getTargetDate());
        goal.setDescription(request.getDescription());
        goal.setPriority(normalizePriority(request.getPriority()));
        goal.setStatus(request.getCurrentAmount().compareTo(request.getTargetAmount()) >= 0
                ? "COMPLETED" : "ACTIVE");
        if (request.getAccountId() == null) {
            goal.setAccount(null);
        } else {
            Account account = accountRepository.findById(request.getAccountId())
                    .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
            if (!account.getUser().getId().equals(user.getId())) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "You are not authorized to use this account");
            }
            goal.setAccount(account);
        }
    }

    private SavingsGoalResponse toResponse(SavingsGoal goal) {
        BigDecimal remaining = goal.getTargetAmount()
                .subtract(goal.getCurrentAmount()).max(BigDecimal.ZERO);
        BigDecimal percentage = goal.getCurrentAmount()
                .multiply(BigDecimal.valueOf(100))
                .divide(goal.getTargetAmount(), 2, RoundingMode.HALF_UP)
                .min(BigDecimal.valueOf(100));
        Long days = goal.getTargetDate() == null ? null
                : ChronoUnit.DAYS.between(LocalDate.now(), goal.getTargetDate());
        BigDecimal monthly = BigDecimal.ZERO;
        BigDecimal weekly = BigDecimal.ZERO;
        BigDecimal daily = BigDecimal.ZERO;
        LocalDate estimatedCompletionDate = null;
        boolean onTrack = remaining.signum() == 0;
        if (remaining.signum() > 0 && days != null && days > 0) {
            long months = ChronoUnit.MONTHS.between(LocalDate.now(), goal.getTargetDate());
            if (LocalDate.now().plusMonths(months).isBefore(goal.getTargetDate())) months++;
            months = Math.max(months, 1);
            monthly = remaining.divide(BigDecimal.valueOf(months), 2, RoundingMode.CEILING);
            weekly = remaining.divide(BigDecimal.valueOf(Math.max(1, (long) Math.ceil(days / 7.0))), 2, RoundingMode.CEILING);
            daily = remaining.divide(BigDecimal.valueOf(days), 2, RoundingMode.CEILING);
            estimatedCompletionDate = goal.getTargetDate();
            long totalPlanDays = Math.max(1,
                    ChronoUnit.DAYS.between(goal.getCreatedAt().toLocalDate(), goal.getTargetDate()));
            long elapsedDays = Math.max(0,
                    ChronoUnit.DAYS.between(goal.getCreatedAt().toLocalDate(), LocalDate.now()));
            BigDecimal expectedProgress = BigDecimal.valueOf(elapsedDays)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalPlanDays), 2, RoundingMode.HALF_UP);
            onTrack = percentage.compareTo(expectedProgress) >= 0;
        } else if (remaining.signum() > 0 && goal.getTargetDate() == null) {
            onTrack = true;
        } else if (remaining.signum() > 0) {
            onTrack = false;
        } else {
            estimatedCompletionDate = LocalDate.now();
        }
        AutopayAuthorization activeAutopay = autopayRepository
                .findBySavingsGoalIdAndUserIdOrderByNextPaymentDateAsc(goal.getId(), goal.getUser().getId())
                .stream()
                .filter(a -> "ACTIVE".equalsIgnoreCase(a.getStatus()))
                .findFirst()
                .orElse(null);
        return new SavingsGoalResponse(
                goal.getId(), goal.getName(), goal.getTargetAmount(),
                goal.getCurrentAmount(), remaining, percentage, monthly, weekly, daily, days,
                estimatedCompletionDate,
                onTrack, goal.getTargetDate(), goal.getDescription(),
                goal.getPriority() == null ? "IMPORTANT" : goal.getPriority(),
                goal.getAccount() == null ? null : goal.getAccount().getId(),
                activeAutopay == null ? null : activeAutopay.getId(),
                activeAutopay == null ? null : activeAutopay.getStatus(),
                activeAutopay == null ? null : activeAutopay.getNextPaymentDate(),
                goal.getStatus());
    }

    private String normalizePriority(String priority) {
        if (priority == null || priority.isBlank()) {
            return "IMPORTANT";
        }
        String normalized = priority.trim().toUpperCase();
        if (!List.of("NECESSARY", "IMPORTANT", "OPTIONAL").contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported goal priority");
        }
        return normalized;
    }
}
