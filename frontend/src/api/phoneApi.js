import api from "./axios";

export const sendPhoneOtp = () => api.post("/api/users/me/phone-verification");

export const verifyPhoneOtp = (code) => api.post("/api/users/me/phone-verification/verify", { code });
