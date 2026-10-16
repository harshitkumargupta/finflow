import api from "./axios";

export const getAnalyticsSummary = () => api.get("/api/analytics/summary");
export const getCategoryAnalytics = () => api.get("/api/analytics/categories");
export const getMonthlyAnalytics = () => api.get("/api/analytics/monthly");
