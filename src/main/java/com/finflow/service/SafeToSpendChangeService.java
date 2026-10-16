package com.finflow.service;

import com.finflow.dto.SafeToSpendBreakdown;
import com.finflow.dto.SafeToSpendChangeDriver;
import com.finflow.dto.SafeToSpendChangeResponse;
import com.finflow.dto.SafeToSpendResponse;
import com.finflow.entity.SafeToSpendSnapshot;
import com.finflow.entity.User;
import com.finflow.repository.SafeToSpendSnapshotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class SafeToSpendChangeService {

    private static final Logger log = LoggerFactory.getLogger(SafeToSpendChangeService.class);
    private static final Map<String, String> LABELS = Map.of(
            "UPCOMING_BILL", "upcoming bill",
            "AUTOPAY", "autopay",
            "GOAL_CONTRIBUTION", "goal contribution",
            "BUDGET_RESERVE", "budget reserve",
            SafeToSpendService.EXPECTED_INCOME, "expected income");

    private final SafeToSpendService safeToSpendService;
    private final SafeToSpendSnapshotRepository snapshotRepository;
    private final SafeToSpendSnapshotRecorder recorder;
    private final CurrentUserService currentUserService;

    public SafeToSpendChangeService(SafeToSpendService safeToSpendService,
                                    SafeToSpendSnapshotRepository snapshotRepository,
                                    SafeToSpendSnapshotRecorder recorder,
                                    CurrentUserService currentUserService) {
        this.safeToSpendService = safeToSpendService;
        this.snapshotRepository = snapshotRepository;
        this.recorder = recorder;
        this.currentUserService = currentUserService;
    }

    public SafeToSpendResponse currentWithSnapshot() {
        User user = currentUserService.get();
        SafeToSpendResponse current = safeToSpendService.calculateForCurrentUser();
        recordQuietly(user.getId(), current);
        return current;
    }

    public SafeToSpendChangeResponse explainChange() {
        User user = currentUserService.get();
        LocalDate today = LocalDate.now();
        SafeToSpendResponse current = safeToSpendService.calculateForCurrentUser();
        Optional<SafeToSpendSnapshot> baseline = snapshotRepository
                .findFirstByUserIdAndSnapshotDateBeforeOrderBySnapshotDateDesc(user.getId(), today)
                .or(() -> snapshotRepository.findByUserIdAndSnapshotDate(user.getId(), today));
        recordQuietly(user.getId(), current);
        if (baseline.isEmpty()) {
            return new SafeToSpendChangeResponse(false, null, null, current.safeToSpend(), null,
                    "No earlier snapshot yet. FinFlow records one each day you view Safe-to-Spend, so check back tomorrow to see what changed.",
                    List.of());
        }
        SafeToSpendSnapshot snapshot = baseline.get();
        return explain(snapshot.getSnapshotDate(), snapshot.getCurrentBalance(), snapshot.getSafeToSpend(),
                parse(snapshot.getBreakdown()), current);
    }

    public static SafeToSpendChangeResponse explain(LocalDate baselineDate, BigDecimal previousBalance,
                                                    BigDecimal previousSafe, List<SafeToSpendBreakdown> previous,
                                                    SafeToSpendResponse current) {
        List<SafeToSpendChangeDriver> drivers = new ArrayList<>();
        BigDecimal balanceImpact = current.currentBalance().subtract(previousBalance);
        if (balanceImpact.signum() != 0) {
            drivers.add(new SafeToSpendChangeDriver("BALANCE", "Account balance", scale(previousBalance),
                    scale(current.currentBalance()), scale(balanceImpact),
                    "Your account balance " + (balanceImpact.signum() > 0 ? "rose" : "fell") + " by " + rupees(balanceImpact.abs()) + "."));
        }

        Map<String, BigDecimal> before = totals(previous);
        Map<String, BigDecimal> after = totals(current.breakdown());
        Map<String, Boolean> keys = new LinkedHashMap<>();
        before.keySet().forEach(key -> keys.put(key, true));
        after.keySet().forEach(key -> keys.put(key, true));
        for (String key : keys.keySet()) {
            BigDecimal was = before.getOrDefault(key, BigDecimal.ZERO);
            BigDecimal now = after.getOrDefault(key, BigDecimal.ZERO);
            if (was.compareTo(now) == 0) continue;
            String type = key.substring(0, key.indexOf('|'));
            String description = key.substring(key.indexOf('|') + 1);
            boolean income = SafeToSpendService.EXPECTED_INCOME.equals(type);
            BigDecimal impact = income ? now.subtract(was) : was.subtract(now);
            drivers.add(new SafeToSpendChangeDriver(type, description, scale(was), scale(now), scale(impact),
                    describe(type, description, was, now)));
        }
        drivers.sort(Comparator.comparing((SafeToSpendChangeDriver driver) -> driver.impact().abs()).reversed());

        BigDecimal change = current.safeToSpend().subtract(previousSafe);
        String summary = change.signum() == 0
                ? "Safe-to-Spend is unchanged since " + baselineDate + "."
                : "Safe-to-Spend is " + (change.signum() > 0 ? "up " : "down ") + rupees(change.abs()) + " since " + baselineDate + ".";
        return new SafeToSpendChangeResponse(true, baselineDate, scale(previousSafe), scale(current.safeToSpend()),
                scale(change), summary, List.copyOf(drivers));
    }

    private static String describe(String type, String description, BigDecimal was, BigDecimal now) {
        String label = LABELS.getOrDefault(type, type.toLowerCase().replace('_', ' '));
        if (was.signum() == 0) {
            return SafeToSpendService.EXPECTED_INCOME.equals(type)
                    ? "New " + label + " '" + description + "' of " + rupees(now) + " is expected within the next 30 days."
                    : "New " + label + " '" + description + "' of " + rupees(now) + " now falls within the next 30 days.";
        }
        if (now.signum() == 0) {
            return "The " + label + " '" + description + "' (" + rupees(was) + ") is no longer counted: it was paid, cancelled, or moved outside the 30-day window.";
        }
        return "The " + label + " '" + description + "' changed from " + rupees(was) + " to " + rupees(now) + ".";
    }

    private static Map<String, BigDecimal> totals(List<SafeToSpendBreakdown> items) {
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        for (SafeToSpendBreakdown item : items) {
            totals.merge(item.type() + "|" + item.description(), item.amount(), BigDecimal::add);
        }
        return totals;
    }

    private static BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static String rupees(BigDecimal value) {
        return "₹" + scale(value).toPlainString();
    }

    private List<SafeToSpendBreakdown> parse(String json) {
        return SafeToSpendSnapshotRecorder.JSON.readValue(json, new TypeReference<List<SafeToSpendBreakdown>>() { });
    }

    private void recordQuietly(Long userId, SafeToSpendResponse current) {
        try {
            recorder.recordIfAbsent(userId, LocalDate.now(), current);
        } catch (DataIntegrityViolationException duplicate) {
            log.debug("Safe-to-Spend snapshot already recorded for user {}", userId);
        }
    }
}
