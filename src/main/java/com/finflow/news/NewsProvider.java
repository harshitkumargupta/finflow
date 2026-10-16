package com.finflow.news;

import com.finflow.dto.NewsArticleResponse;
import com.finflow.exception.MarketProviderUnavailableException;

import java.util.List;

public interface NewsProvider {
    List<NewsArticleResponse> topBusinessNews() throws MarketProviderUnavailableException;
    List<NewsArticleResponse> marketNews() throws MarketProviderUnavailableException;
    List<NewsArticleResponse> investmentNews(List<String> symbols) throws MarketProviderUnavailableException;
    List<NewsArticleResponse> search(String query) throws MarketProviderUnavailableException;
}
