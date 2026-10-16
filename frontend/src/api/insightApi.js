import api from "./axios";

export const getInsightSummary = () => api.get("/api/insights/narration/summary");

export const getSpendingWarnings = () => api.get("/api/insights/narration/spending-warnings");

export const suggestCategory = (description, type) => api.post("/api/insights/narration/categorize", { description, type });
