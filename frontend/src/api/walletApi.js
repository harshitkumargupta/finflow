import api from "./axios";

export const getWallet = () => api.get("/api/wallet");
export const createWallet = () => api.post("/api/wallet");
export const updatePaymentIdentity = (paymentId) =>
  api.put("/api/wallet/payment-identity", { paymentId });
export const getWalletTransactions = () => api.get("/api/wallet/transactions");
export const getWalletLedger = () => api.get("/api/wallet/ledger");
export const addWalletFunds = (request, idempotencyKey) =>
  api.post("/api/wallet/fund", request, {
    headers: { "Idempotency-Key": idempotencyKey },
  });
export const transferWalletFunds = (request, idempotencyKey) =>
  api.post("/api/wallet/transfer", request, {
    headers: { "Idempotency-Key": idempotencyKey },
  });
export const getFinancialSafety = () => api.get("/api/money/safety");
export const getMoneyReservations = () => api.get("/api/money/reservations");
export const createMoneyReservation = (request) =>
  api.post("/api/money/reservations", request);
export const releaseMoneyReservation = (id) =>
  api.delete(`/api/money/reservations/${id}`);
export const getMoneyRules = () => api.get("/api/money-rules");
export const createMoneyRule = (request) => api.post("/api/money-rules", request);
export const deleteMoneyRule = (id) => api.delete(`/api/money-rules/${id}`);
export const getSpendingLimits = () => api.get("/api/spending-limits");
export const createSpendingLimit = (request) =>
  api.post("/api/spending-limits", request);
export const deleteSpendingLimit = (id) =>
  api.delete(`/api/spending-limits/${id}`);
export const createPaymentQr = () => api.post("/api/payments/qr/create");
export const scanPaymentQr = (payload) =>
  api.post("/api/payments/qr/scan", { payload });
export const confirmWalletPayment = (request, idempotencyKey) =>
  api.post("/api/payments/confirm", request, {
    headers: { "Idempotency-Key": idempotencyKey },
  });
export const resolvePaymentId = (paymentId) =>
  api.post("/api/payments/resolve", { paymentId });
export const getWalletPayments = () => api.get("/api/payments/history");
