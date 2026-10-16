package com.finflow.investment;

import com.finflow.dto.MarketNewsArticle;
import com.finflow.exception.MarketProviderUnavailableException;

import java.util.List;

public interface MarketNewsProvider {
    List<MarketNewsArticle> latest()
            throws MarketProviderUnavailableException;
}
