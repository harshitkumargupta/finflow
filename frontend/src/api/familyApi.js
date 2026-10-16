import api from "./axios";

export const getFamily = () => api.get("/api/family");
export const createFamily = () => api.post("/api/family");
export const getFamilyMembers = () => api.get("/api/family/members");
export const createFamilyMember = (request) => api.post("/api/family/members", request);
export const updateFamilyMember = (id, request) =>
  api.put(`/api/family/members/${id}`, request);
export const setFamilyMemberFrozen = (id, frozen) =>
  api.put(`/api/family/members/${id}/${frozen ? "freeze" : "unfreeze"}`);
export const addFamilyAllowance = (id, request, idempotencyKey) =>
  api.post(`/api/family/members/${id}/allowances`, request, {
    headers: { "Idempotency-Key": idempotencyKey },
  });
export const spendFamilyAllowance = (id, request, idempotencyKey) =>
  api.post(`/api/family/members/${id}/spends`, request, {
    headers: { "Idempotency-Key": idempotencyKey },
  });
export const getFamilyTransactions = (id) =>
  api.get(`/api/family/members/${id}/transactions`);
export const upsertFamilyCategoryLimit = (id, request) =>
  api.post(`/api/family/members/${id}/category-limits`, request);
export const deleteFamilyCategoryLimit = (id, limitId) =>
  api.delete(`/api/family/members/${id}/category-limits/${limitId}`);
export const deleteFamilyMember = (id) =>
  api.delete(`/api/family/members/${id}`);

