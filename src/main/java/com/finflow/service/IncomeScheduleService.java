package com.finflow.service;

import com.finflow.dto.IncomeScheduleRequest;
import com.finflow.dto.IncomeScheduleResponse;
import com.finflow.entity.IncomeSchedule;
import com.finflow.entity.User;
import com.finflow.exception.ResourceNotFoundException;
import com.finflow.repository.IncomeScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class IncomeScheduleService {

    public static final int HORIZON_DAYS = 30;

    private final IncomeScheduleRepository repository;
    private final CurrentUserService currentUserService;
    private final MoneyNormalizer moneyNormalizer;

    public IncomeScheduleService(IncomeScheduleRepository repository, CurrentUserService currentUserService,
                                 MoneyNormalizer moneyNormalizer) {
        this.repository = repository;
        this.currentUserService = currentUserService;
        this.moneyNormalizer = moneyNormalizer;
    }

    @Transactional(readOnly = true)
    public List<IncomeScheduleResponse> list() {
        User user = currentUserService.get();
        LocalDate today = LocalDate.now();
        return repository.findByUserIdOrderByNextExpectedDateAscIdAsc(user.getId()).stream()
                .map(schedule -> toResponse(schedule, today)).toList();
    }

    @Transactional
    public IncomeScheduleResponse create(IncomeScheduleRequest request) {
        IncomeSchedule schedule = new IncomeSchedule();
        schedule.setUser(currentUserService.get());
        schedule.setCreatedAt(LocalDateTime.now());
        apply(schedule, request);
        return toResponse(repository.save(schedule), LocalDate.now());
    }

    @Transactional
    public IncomeScheduleResponse update(Long id, IncomeScheduleRequest request) {
        IncomeSchedule schedule = findOwned(id);
        apply(schedule, request);
        return toResponse(repository.save(schedule), LocalDate.now());
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(findOwned(id));
    }

    public static List<LocalDate> occurrencesWithin(IncomeSchedule schedule, LocalDate today, LocalDate horizon) {
        List<LocalDate> dates = new ArrayList<>();
        if (!schedule.isActive() || schedule.getNextExpectedDate() == null) return dates;
        LocalDate start = schedule.getNextExpectedDate();
        long first = 0;
        if (start.isBefore(today)) {
            first = switch (schedule.getFrequency()) {
                case "WEEKLY" -> ChronoUnit.WEEKS.between(start, today);
                case "BIWEEKLY" -> ChronoUnit.WEEKS.between(start, today) / 2;
                case "MONTHLY" -> ChronoUnit.MONTHS.between(start, today);
                default -> 0;
            };
        }
        for (long step = first; step < first + 40; step++) {
            LocalDate date = switch (schedule.getFrequency()) {
                case "WEEKLY" -> start.plusWeeks(step);
                case "BIWEEKLY" -> start.plusWeeks(2 * step);
                case "MONTHLY" -> start.plusMonths(step);
                default -> step == 0 ? start : null;
            };
            if (date == null || date.isAfter(horizon)) break;
            if (date.isAfter(today)) dates.add(date);
        }
        return dates;
    }

    private void apply(IncomeSchedule schedule, IncomeScheduleRequest request) {
        schedule.setSourceName(request.sourceName().trim());
        schedule.setAmount(moneyNormalizer.normalizePositive(request.amount()));
        schedule.setFrequency(request.frequency());
        schedule.setNextExpectedDate(request.nextExpectedDate());
        schedule.setActive(request.active() == null || request.active());
        schedule.setUpdatedAt(LocalDateTime.now());
    }

    private IncomeSchedule findOwned(Long id) {
        User user = currentUserService.get();
        return repository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Income schedule not found"));
    }

    private IncomeScheduleResponse toResponse(IncomeSchedule schedule, LocalDate today) {
        List<LocalDate> dates = occurrencesWithin(schedule, today, today.plusDays(HORIZON_DAYS));
        BigDecimal expected = schedule.getAmount().multiply(BigDecimal.valueOf(dates.size()));
        return new IncomeScheduleResponse(schedule.getId(), schedule.getSourceName(), schedule.getAmount(),
                schedule.getFrequency(), schedule.getNextExpectedDate(), schedule.isActive(), dates, expected);
    }
}
