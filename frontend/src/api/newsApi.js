import api from "./axios";

export const getLatestNews = () => api.get("/api/news");
export const getMarketNewsFeed = () => api.get("/api/news/markets");
export const getInvestmentNewsFeed = () => api.get("/api/news/investments");
export const searchNews = (q) => api.get("/api/news/search", { params: { q } });
