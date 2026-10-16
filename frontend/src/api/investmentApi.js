import api from "./axios";

export const getInvestmentEducation = () => api.get("/api/investments/education");
export const getMarketNews = () => api.get("/api/investments/market-news");
export const getMarketSnapshot = () => api.get("/api/investments/market-snapshot");
export const getWatchlist = () => api.get("/api/investments/watchlist");
export const addWatchlistItem = (request) => api.post("/api/investments/watchlist", request);
export const removeWatchlistItem = (id) => api.delete(`/api/investments/watchlist/${id}`);

export const getPortfolio = () => api.get("/api/investments/portfolio");
export const addHolding = (request) => api.post("/api/investments/portfolio", request);
export const updateHolding = (id, request) => api.put(`/api/investments/portfolio/${id}`, request);
export const deleteHolding = (id) => api.delete(`/api/investments/portfolio/${id}`);
