package com.finflow;

import com.finflow.dto.CategorySuggestionResponse;
import com.finflow.insight.InsightNarrationProvider;
import com.finflow.insight.NarrationUnavailableException;
import com.finflow.service.InsightNarrationService;
import com.finflow.service.TransactionCategorizationService;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class InsightNarrationServiceTest {

    private static class StubProvider implements InsightNarrationProvider {
        private final boolean available;
        private final Supplier<String> narration;

        StubProvider(boolean available, Supplier<String> narration) {
            this.available = available;
            this.narration = narration;
        }

        public String name() { return "STUB"; }
        public boolean available() { return available; }
        public String narrate(String prompt) { return narration.get(); }
    }

    private InsightNarrationService service(InsightNarrationProvider provider) {
        return new InsightNarrationService(provider, new TransactionCategorizationService(), null, null, null);
    }

    @Test
    void highConfidenceRuleIsUsedWithoutAi() {
        CategorySuggestionResponse result = service(new StubProvider(true, () -> "GROCERIES"))
                .categorize("Swiggy dinner order", "WITHDRAWAL");
        assertEquals("FOOD", result.category());
        assertEquals("RULE", result.source());
        assertTrue(result.overridable());
    }

    @Test
    void lowConfidenceUsesAiSuggestionWhenAvailable() {
        CategorySuggestionResponse result = service(new StubProvider(true, () -> "the category is GROCERIES"))
                .categorize("mystery vendor xyz", "WITHDRAWAL");
        assertEquals("GROCERIES", result.category());
        assertEquals("AI", result.source());
    }

    @Test
    void lowConfidenceFallsBackWhenProviderUnavailable() {
        CategorySuggestionResponse result = service(new StubProvider(false, () -> "GROCERIES"))
                .categorize("mystery vendor xyz", "WITHDRAWAL");
        assertEquals("OTHER", result.category());
        assertEquals("RULE", result.source());
    }

    @Test
    void providerErrorNeverPropagates() {
        CategorySuggestionResponse result = service(new StubProvider(true, () -> { throw new NarrationUnavailableException("rate limited"); }))
                .categorize("mystery vendor xyz", "WITHDRAWAL");
        assertEquals("OTHER", result.category());
        assertEquals("RULE", result.source());
        assertTrue(result.overridable());
    }
}
