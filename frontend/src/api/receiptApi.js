import api from "./axios";

export const scanReceipt = (file) => {
  const form = new FormData();
  form.append("file", file);
  return api.post("/api/receipts/scan", form, { headers: { "Content-Type": "multipart/form-data" } });
};
