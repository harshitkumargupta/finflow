package com.finflow.service;

import org.springframework.data.domain.Pageable;
import com.finflow.dto.PageResponseMapper;
import com.finflow.dto.PageResponse;
import com.finflow.dto.BillRequest;
import com.finflow.dto.BillResponse;
import com.finflow.entity.Account;
import com.finflow.entity.Bill;
import com.finflow.entity.User;
import com.finflow.exception.ResourceNotFoundException;
import com.finflow.repository.AccountRepository;
import com.finflow.repository.BillRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class BillService {
    private final BillRepository billRepository;
    private final AccountRepository accountRepository;
    private final CurrentUserService currentUserService;

    public BillService(BillRepository billRepository, AccountRepository accountRepository,
                       CurrentUserService currentUserService) {
        this.billRepository = billRepository;
        this.accountRepository = accountRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public BillResponse create(BillRequest request) {
        User user = currentUserService.get();
        Bill bill = new Bill();
        bill.setUser(user);
        apply(bill, request, user);
        bill.setCreatedAt(LocalDateTime.now());
        bill.setUpdatedAt(LocalDateTime.now());
        return toResponse(billRepository.save(bill));
    }

    @Transactional(readOnly = true)
    public List<BillResponse> list() {
        User user = currentUserService.get();
        return billRepository.findByUserIdOrderByNextDueDateAsc(user.getId())
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<BillResponse> list(Pageable pageable) {
        User user = currentUserService.get();
        return PageResponseMapper.map(billRepository.findByUserIdOrderByNextDueDateAscIdAsc(user.getId(), pageable), this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<BillResponse> upcoming() {
        User user = currentUserService.get();
        LocalDate today = LocalDate.now();
        return billRepository.findByUserIdAndActiveTrueAndNextDueDateBetweenOrderByNextDueDateAsc(
                        user.getId(), today, today.plusDays(30))
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public BillResponse get(Long id) {
        return toResponse(findOwned(id));
    }

    @Transactional
    public BillResponse update(Long id, BillRequest request) {
        Bill bill = findOwned(id);
        apply(bill, request, bill.getUser());
        bill.setUpdatedAt(LocalDateTime.now());
        return toResponse(billRepository.save(bill));
    }

    @Transactional
    public void delete(Long id) {
        billRepository.delete(findOwned(id));
    }

    @Transactional(readOnly = true)
    public List<BillResponse> dueWithinDays(int days) {
        User user = currentUserService.get();
        LocalDate today = LocalDate.now();
        return billRepository.findByUserIdAndActiveTrueAndNextDueDateLessThanEqualOrderByNextDueDateAsc(
                        user.getId(), today.plusDays(days))
                .stream().map(this::toResponse).toList();
    }

    private Bill findOwned(Long id) {
        User user = currentUserService.get();
        return billRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found"));
    }

    private void apply(Bill bill, BillRequest request, User user) {
        bill.setName(request.getName().trim());
        bill.setAmount(request.getAmount());
        bill.setCategory(request.getCategory());
        bill.setFrequency(request.getFrequency());
        bill.setNextDueDate(request.getNextDueDate());
        bill.setDescription(request.getDescription());
        bill.setReminderEnabled(request.isReminderEnabled());
        bill.setActive(request.isActive());
        if (request.getAccountId() == null) {
            bill.setAccount(null);
        } else {
            Account account = accountRepository.findById(request.getAccountId())
                    .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
            if (!account.getUser().getId().equals(user.getId())) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "You are not authorized to use this account");
            }
            bill.setAccount(account);
        }
    }

    private BillResponse toResponse(Bill bill) {
        long days = ChronoUnit.DAYS.between(LocalDate.now(), bill.getNextDueDate());
        return new BillResponse(
                bill.getId(), bill.getName(), bill.getAmount(), bill.getCategory(),
                bill.getFrequency(), bill.getNextDueDate(), bill.getDescription(),
                bill.getAccount() == null ? null : bill.getAccount().getId(),
                bill.getAccount() == null ? null : bill.getAccount().getAccountNumber(),
                bill.isReminderEnabled(), bill.isActive(), days);
    }
}
