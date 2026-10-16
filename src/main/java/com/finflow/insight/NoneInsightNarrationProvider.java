package com.finflow.insight;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "finflow.insight.provider", havingValue = "NONE", matchIfMissing = true)
public class NoneInsightNarrationProvider implements InsightNarrationProvider {

    @Override
    public String name() {
        return "NONE";
    }

    @Override
    public boolean available() {
        return false;
    }

    @Override
    public String narrate(String prompt) {
        throw new NarrationUnavailableException("Narration provider is not configured.");
    }
}
