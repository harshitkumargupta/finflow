package com.finflow.service;

import com.finflow.dto.*;
import com.finflow.entity.InvestmentWatchlistItem;
import com.finflow.entity.User;
import com.finflow.exception.MarketProviderUnavailableException;
import com.finflow.exception.ResourceAlreadyExistsException;
import com.finflow.exception.ResourceNotFoundException;
import com.finflow.investment.MarketDataProvider;
import com.finflow.repository.InvestmentWatchlistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Service
public class InvestmentInformationService {
    private static final String MARKET_UNAVAILABLE = "Market data temporarily unavailable.";
    private final CurrentUserService currentUserService;
    private final InvestmentWatchlistRepository watchlistRepository;
    private final NewsService newsService;
    private final Optional<MarketDataProvider> marketDataProvider;

    public InvestmentInformationService(
            CurrentUserService currentUserService,
            InvestmentWatchlistRepository watchlistRepository,
            NewsService newsService,
            Optional<MarketDataProvider> marketDataProvider) {
        this.currentUserService = currentUserService;
        this.watchlistRepository = watchlistRepository;
        this.newsService = newsService;
        this.marketDataProvider = marketDataProvider;
    }

    public InvestmentEducationResponse education() {
        return new InvestmentEducationResponse(
                "Educational information only. This is not personalized investment, tax, or legal advice. "
                        + "Values can fall as well as rise; no return is guaranteed.",
                List.of(
                        category("Stocks", "HIGH", "Long term", "High", "Ownership interest in a company; value can be volatile."),
                        category("Mutual funds", "LOW to HIGH", "Varies by fund", "Varies", "A pooled investment whose risk depends on its holdings and mandate."),
                        category("Index funds", "MODERATE to HIGH", "Long term", "Usually daily", "Tracks an index and can provide broad exposure; it still carries market risk."),
                        category("Bonds", "LOW to MODERATE", "Short to long term", "Varies", "Debt instruments with credit, interest-rate, and liquidity risks."),
                        category("Fixed deposits", "LOW", "Short to medium term", "May have withdrawal conditions", "A deposit with stated terms; early withdrawal can have penalties and coverage varies."),
                        category("Gold", "MODERATE", "Medium to long term", "Varies by product", "A commodity exposure with price, storage, and product-specific risks."),
                        category("Government securities", "LOW to MODERATE", "Short to long term", "Varies", "Sovereign debt can still have price and liquidity risk before maturity."),
                        category("Other", "Varies", "Varies", "Varies", "Understand product structure, fees, counterparty risk, and regulation before investing.")
                ),
                List.of(
                        topic("emergency-fund", "Emergency fund", "Keep a liquid reserve for essential expenses and unexpected events before taking investment risk.", "LOW", "Short term"),
                        topic("budgeting", "Budgeting", "Plan income and recurring costs so essential needs, savings, and discretionary spending are visible.", "LOW", "Ongoing"),
                        topic("compounding", "Compounding", "Returns can earn further returns over time, but compounding also works against borrowers and never guarantees growth.", "Varies", "Long term"),
                        topic("inflation", "Inflation", "Rising prices reduce purchasing power; compare nominal returns with inflation and costs.", "Varies", "Long term"),
                        topic("diversification", "Diversification", "Spreading exposure can reduce concentration risk, but cannot eliminate market losses.", "Varies", "Medium to long term"),
                        topic("risk-return", "Risk and return", "Potential return and uncertainty generally move together; match risk to capacity and time horizon.", "Varies", "Varies"),
                        topic("sip", "Systematic investing", "Investing at intervals can spread purchase timing; it does not ensure a profit or protect against losses.", "Moderate to high", "Long term"),
                        topic("index-investing", "Index investing", "An index strategy follows a stated benchmark; understand tracking error, holdings, fees, and market risk.", "Moderate to high", "Long term"),
                        topic("bonds", "Bonds", "Bond values respond to interest rates, issuer credit, and liquidity; holding to maturity does not remove every risk.", "Low to moderate", "Short to long term"),
                        topic("fixed-deposits", "Fixed deposits", "Review rate terms, maturity, premature withdrawal rules, institution protections, and tax treatment.", "Low", "Short to medium term"),
                        topic("gold", "Gold", "Gold prices fluctuate and may not produce income; product form affects cost, liquidity, and custody.", "Moderate", "Medium to long term"),
                        topic("long-term", "Long-term investing", "Longer horizons may allow time to manage volatility, but past performance is not a promise of future results.", "Varies", "Long term"),
                        topic("goal-horizon", "Short-term vs long-term goals", "For near-term goals, liquidity and capital preservation may matter more; longer horizons can still involve substantial loss risk.", "Varies", "Depends on goal")
                ));
    }

    @Transactional(readOnly = true)
    public MarketNewsResponse latestNews() {
        NewsResponse response = newsService.markets();
        return new MarketNewsResponse(response.available(), response.message(),
                response.articles().stream()
                        .map(article -> new MarketNewsArticle(
                                article.title(),
                                article.source(),
                                article.publishedAt(),
                                Objects.requireNonNullElse(article.category(), "MARKETS"),
                                article.articleUrl(),
                                article.description()))
                        .toList());
    }

    @Transactional(readOnly = true)
    public MarketSnapshotResponse marketSnapshot() {
        if (marketDataProvider.isEmpty()) {
            return new MarketSnapshotResponse(false, MARKET_UNAVAILABLE, null, List.of());
        }
        try {
            return marketDataProvider.get().latestSnapshot();
        } catch (MarketProviderUnavailableException exception) {
            return new MarketSnapshotResponse(false, MARKET_UNAVAILABLE, null, List.of());
        }
    }

    @Transactional(readOnly = true)
    public List<WatchlistResponse> watchlist() {
        User user = currentUserService.get();
        return watchlistRepository.findByUserIdOrderByAddedAtDesc(user.getId())
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public WatchlistResponse addToWatchlist(WatchlistRequest request) {
        User user = currentUserService.get();
        String symbol = request.symbol().toUpperCase(Locale.ROOT);
        if (watchlistRepository.existsByUserIdAndSymbol(user.getId(), symbol)) {
            throw new ResourceAlreadyExistsException("This instrument is already on your watchlist");
        }
        InvestmentWatchlistItem item = new InvestmentWatchlistItem();
        item.setUser(user);
        item.setSymbol(symbol);
        item.setName(request.name().trim());
        item.setCategory(request.category().toUpperCase(Locale.ROOT));
        item.setAddedAt(LocalDateTime.now());
        return toResponse(watchlistRepository.save(item));
    }

    @Transactional
    public void removeFromWatchlist(Long id) {
        User user = currentUserService.get();
        InvestmentWatchlistItem item = watchlistRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Watchlist item not found"));
        watchlistRepository.delete(item);
    }

    private InvestmentEducationResponse.InvestmentCategory category(
            String name, String risk, String horizon, String liquidity, String characteristics) {
        return new InvestmentEducationResponse.InvestmentCategory(
                name, risk, horizon, liquidity, characteristics);
    }

    private InvestmentEducationResponse.EducationTopic topic(
            String key, String title, String summary, String risk, String horizon) {
        return new InvestmentEducationResponse.EducationTopic(
                key, title, summary, risk, horizon);
    }

    private WatchlistResponse toResponse(InvestmentWatchlistItem item) {
        return new WatchlistResponse(item.getId(), item.getSymbol(), item.getName(),
                item.getCategory(), item.getAddedAt(), false, MARKET_UNAVAILABLE);
    }
}
