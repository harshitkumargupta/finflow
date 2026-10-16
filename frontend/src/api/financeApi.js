import api from "./axios";

export const getBudgets = () => api.get("/api/budgets");
export const createBudget = (request) => api.post("/api/budgets", request);
export const updateBudget = (id, request) => api.put(`/api/budgets/${id}`, request);
export const deleteBudget = (id) => api.delete(`/api/budgets/${id}`);

export const getBills = () => api.get("/api/bills");
export const getUpcomingBills = () => api.get("/api/bills/upcoming");
export const createBill = (request) => api.post("/api/bills", request);
export const updateBill = (id, request) => api.put(`/api/bills/${id}`, request);
export const deleteBill = (id) => api.delete(`/api/bills/${id}`);

export const getGoals = () => api.get("/api/goals");
export const createGoal = (request) => api.post("/api/goals", request);
export const updateGoal = (id, request) => api.put(`/api/goals/${id}`, request);
export const contributeToGoal = (id, amount) =>
  api.post(`/api/goals/${id}/contributions`, { amount });
export const createGoalSavingsAutopay = (id, request) =>
  api.post(`/api/goals/${id}/savings-autopay`, request);
export const deleteGoal = (id) => api.delete(`/api/goals/${id}`);
export const planGoal = (request) => api.post("/api/goals/assistant/plan", request);

export const getFinancialHealth = () => api.get("/api/financial-health");
export const getNotifications = () => api.get("/api/notifications");
export const getUnreadNotifications = () => api.get("/api/notifications/unread");
export const getUnreadNotificationCount = () =>
  api.get("/api/notifications/unread-count");
export const markNotificationRead = (id) =>
  api.put(`/api/notifications/${id}/read`);
export const markAllNotificationsRead = () =>
  api.put("/api/notifications/read-all");
export const getFinancialReport = () => api.get("/api/reports");
export const getMonthlyReport = (month) => api.get("/api/reports/monthly", { params: month ? { month } : {} });
export const emailMonthlyReport = (month) => api.post("/api/reports/monthly/email", null, { params: month ? { month } : {} });
export const getSecurityActivity = () => api.get("/api/security/activity");
export const getSafeToSpend = () => api.get("/api/insights/safe-to-spend");
export const getFinancialSummary = () => api.get("/api/insights/financial-summary");
export const createPurchasePlan = (request) => api.post("/api/insights/purchase-plan", request);
export const updateNotificationState = (id, read) =>
  api.patch(`/api/notifications/${id}`, { read });
export const getSafeToSpendChanges = () => api.get("/api/insights/safe-to-spend/changes");
export const getGoalContributions = (id) => api.get(`/api/goals/${id}/contributions`);
export const getIncomeSchedules = () => api.get("/api/income-schedules");
export const createIncomeSchedule = (request) => api.post("/api/income-schedules", request);
export const updateIncomeSchedule = (id, request) => api.put(`/api/income-schedules/${id}`, request);
export const deleteIncomeSchedule = (id) => api.delete(`/api/income-schedules/${id}`);
