import api from "./axios";

export const getAutopayAuthorizations = () => api.get("/api/autopay");
export const getUpcomingAutopays = () => api.get("/api/autopay/upcoming");
export const getAutopayHistory = (autopayId) => api.get(autopayId ? `/api/autopay/${autopayId}/history` : "/api/autopay/history");
export const createAutopayAuthorization = (request) => api.post("/api/autopay", request);
export const simulateIncomingMandate = (request) => api.post("/api/autopay/simulate-incoming", request);
export const approveMandate = (id, accountId, pin) =>
  api.put(`/api/autopay/${id}/approve`, { accountId: accountId || null, pin });
export const rejectMandate = (id) => api.put(`/api/autopay/${id}/reject`);
export const editAutopayAuthorization = (id, request) => api.put(`/api/autopay/${id}`, request);
export const pauseAutopay = (id) => api.put(`/api/autopay/${id}/pause`);
export const resumeAutopay = (id) => api.put(`/api/autopay/${id}/resume`);
export const cancelAutopay = (id) => api.delete(`/api/autopay/${id}`);
export const executeAutopay = (id) => api.post(`/api/autopay/${id}/execute`);
