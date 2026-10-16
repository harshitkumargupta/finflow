package com.finflow.investment;

import com.finflow.dto.MarketSnapshotResponse;
import com.finflow.exception.MarketProviderUnavailableException;

public interface MarketDataProvider {
    MarketSnapshotResponse latestSnapshot()
            throws MarketProviderUnavailableException;
}
