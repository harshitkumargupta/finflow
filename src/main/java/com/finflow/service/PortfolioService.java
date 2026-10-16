package com.finflow.service;

import com.finflow.dto.PortfolioHoldingRequest;
import com.finflow.dto.PortfolioHoldingResponse;
import com.finflow.dto.PortfolioSummaryResponse;
import com.finflow.entity.PortfolioHolding;
import com.finflow.entity.User;
import com.finflow.exception.ResourceNotFoundException;
import com.finflow.investment.MarketPriceProvider;
import com.finflow.repository.PortfolioHoldingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PortfolioService {

    private final PortfolioHoldingRepository repository;
    private final CurrentUserService currentUserService;
    private final MarketPriceProvider priceProvider;

    public PortfolioService(PortfolioHoldingRepository repository, CurrentUserService currentUserService,
                            MarketPriceProvider priceProvider) {
        this.repository = repository;
        this.currentUserService = currentUserService;
        this.priceProvider = priceProvider;
    }

    @Transactional(readOnly = true)
    public PortfolioSummaryResponse summary() {
        User user = currentUserService.get();
        return build(repository.findByUserIdOrderByCreatedAtDescIdDesc(user.getId()));
    }

    @Transactional
    public PortfolioSummaryResponse create(PortfolioHoldingRequest request) {
        User user = currentUserService.get();
        PortfolioHolding holding = new PortfolioHolding();
        holding.setUser(user);
        holding.setCreatedAt(LocalDateTime.now());
        apply(holding, request);
        repository.save(holding);
        return summary();
    }

    @Transactional
    public PortfolioSummaryResponse update(Long id, PortfolioHoldingRequest request) {
        PortfolioHolding holding = findOwned(id);
        apply(holding, request);
        repository.save(holding);
        return summary();
    }

    @Transactional
    public PortfolioSummaryResponse delete(Long id) {
        repository.delete(findOwned(id));
        return summary();
    }

    public PortfolioSummaryResponse build(List<PortfolioHolding> holdings) {
        Map<String, BigDecimal> prices = holdings.isEmpty()
                ? Map.of()
                : priceProvider.pricesFor(holdings.stream().map(PortfolioHolding::getSymbol).collect(Collectors.toCollection(java.util.LinkedHashSet::new)));

        BigDecimal totalInvested = holdings.stream()
                .map(this::invested)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<PortfolioHoldingResponse> responses = holdings.stream()
                .map(holding -> toResponse(holding, prices.get(holding.getSymbol()), totalInvested))
                .toList();

        boolean allPriced = !holdings.isEmpty() && responses.stream().allMatch(PortfolioHoldingResponse::priceAvailable);
        BigDecimal totalCurrent = null;
        BigDecimal totalGain = null;
        BigDecimal totalGainPercent = null;
        if (allPriced) {
            totalCurrent = responses.stream().map(PortfolioHoldingResponse::currentValue).reduce(BigDecimal.ZERO, BigDecimal::add);
            totalGain = totalCurrent.subtract(totalInvested);
            totalGainPercent = percent(totalGain, totalInvested);
        }

        return new PortfolioSummaryResponse(priceProvider.providerName(), allPriced,
                scale2(totalInvested), totalCurrent, totalGain, totalGainPercent, responses);
    }

    private PortfolioHoldingResponse toResponse(PortfolioHolding holding, BigDecimal price, BigDecimal totalInvested) {
        BigDecimal invested = scale2(invested(holding));
        BigDecimal allocation = totalInvested == null || totalInvested.signum() == 0
                ? BigDecimal.ZERO
                : invested.multiply(BigDecimal.valueOf(100)).divide(totalInvested, 2, RoundingMode.HALF_UP);
        boolean priced = price != null;
        BigDecimal current = priced ? scale2(price.multiply(holding.getQuantity())) : null;
        BigDecimal gain = priced ? current.subtract(invested) : null;
        BigDecimal gainPercent = priced ? percent(gain, invested) : null;
        return new PortfolioHoldingResponse(holding.getId(), holding.getSymbol(), holding.getName(), holding.getCategory(),
                holding.getQuantity(), holding.getAveragePrice(), invested, priced,
                priced ? scale2(price) : null, current, gain, gainPercent, allocation,
                holding.getNotes(), holding.getUpdatedAt());
    }

    private BigDecimal invested(PortfolioHolding holding) {
        return holding.getAveragePrice().multiply(holding.getQuantity());
    }

    private BigDecimal percent(BigDecimal part, BigDecimal base) {
        if (base == null || base.signum() == 0) return null;
        return part.multiply(BigDecimal.valueOf(100)).divide(base, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal scale2(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }

    private void apply(PortfolioHolding holding, PortfolioHoldingRequest request) {
        holding.setSymbol(request.symbol().trim().toUpperCase());
        holding.setName(request.name().trim());
        holding.setCategory(request.category());
        holding.setQuantity(request.quantity());
        holding.setAveragePrice(request.averagePrice());
        holding.setNotes(request.notes() == null || request.notes().isBlank() ? null : request.notes().trim());
        holding.setUpdatedAt(LocalDateTime.now());
    }

    private PortfolioHolding findOwned(Long id) {
        User user = currentUserService.get();
        return repository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Holding not found"));
    }
}
