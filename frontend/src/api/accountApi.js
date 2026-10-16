import api from "./axios";

export const getMyAccounts = () => api.get("/api/accounts/mine");
export const getPrimaryAccount = () => api.get("/api/accounts/primary");
export const setPrimaryAccount = (accountId) =>
  api.put(`/api/accounts/${accountId}/primary`);
export const getAccount = (accountId) => api.get(`/api/accounts/${accountId}`);
export const createAccount = (userId, request) =>
  api.post(`/api/accounts/user/${userId}`, request);
export const deposit = (accountId, request, idempotencyKey) =>
  api.post(`/api/accounts/${accountId}/deposit`, request, { headers: { "Idempotency-Key": idempotencyKey } });
export const withdraw = (accountId, request, idempotencyKey) =>
  api.post(`/api/accounts/${accountId}/withdraw`, request, { headers: { "Idempotency-Key": idempotencyKey } });
export const transfer = (accountId, request, idempotencyKey) =>
  api.post(`/api/accounts/${accountId}/transfer`, request, {
    headers: { "Idempotency-Key": idempotencyKey },
  });
export const getTransactionHistory = (accountId, params) =>
  api.get(`/api/accounts/${accountId}/transactions`, params ? { params } : undefined);
export const updateTransactionCategory = (accountId, transactionId, category) =>
  api.put(`/api/accounts/${accountId}/transactions/${transactionId}/category`, { category });

export const getBankLinks = () => api.get("/api/bank-links");
export const initiateBankLink = (request) => api.post("/api/bank-links", request);
export const verifyBankLink = (id, request) => api.post(`/api/bank-links/${id}/verify`, request);
export const refreshBankLinkStatus = (id) => api.post(`/api/bank-links/${id}/refresh-status`);
export const disconnectBankLink = (id) => api.post(`/api/bank-links/${id}/disconnect`);
