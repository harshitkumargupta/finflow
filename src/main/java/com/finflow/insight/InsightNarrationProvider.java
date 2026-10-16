package com.finflow.insight;

public interface InsightNarrationProvider {
    String name();

    boolean available();

    String narrate(String prompt);
}
