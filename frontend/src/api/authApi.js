import api from "./axios";

export const loginRequest = (credentials) =>
  api.post("/api/auth/login", {
    email: credentials.email.trim(),
    password: credentials.password,
  });

export const registerRequest = (registration) =>
  api.post("/api/auth/register", {
    name: registration.name.trim(),
    email: registration.email.trim(),
    phone: registration.phone,
    password: registration.password,
  });

export const getCurrentUser = () => api.get("/api/users/me");

export const verifyEmailRequest = (token) => api.post("/api/auth/verify-email", { token });

export const resendVerificationEmail = () => api.post("/api/users/me/email-verification");
