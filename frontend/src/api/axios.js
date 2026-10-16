import axios from "axios";

export const TOKEN_KEY = "finflow_token";

function getApiBaseUrl() {
  const configuredUrl = (
    import.meta.env.VITE_API_BASE_URL || "http://localhost:8080"
  ).trim();
  const withoutTrailingSlashes = configuredUrl.replace(/\/+$/, "");
  return withoutTrailingSlashes.replace(/\/api$/i, "");
}

const api = axios.create({
  baseURL: getApiBaseUrl(),
  headers: { "Content-Type": "application/json" },
});

let unauthorizedHandler;
let unauthorizedHandled = false;

export function setUnauthorizedHandler(handler) {
  unauthorizedHandler = handler;
}

export function resetUnauthorizedState() {
  unauthorizedHandled = false;
}

api.interceptors.request.use((config) => {
  const isAuthenticationRequest = /^\/api\/auth\/(login|register)$/.test(
    config.url || "",
  );
  const token = isAuthenticationRequest ? null : localStorage.getItem(TOKEN_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const isAuthRequest = /\/api\/auth\/(login|register)$/.test(
      error.config?.url || "",
    );
    if (error.response?.status === 401 && !isAuthRequest && !unauthorizedHandled) {
      unauthorizedHandled = true;
      localStorage.removeItem(TOKEN_KEY);
      unauthorizedHandler?.();
    }
    return Promise.reject(error);
  },
);

export function getApiErrorMessage(error) {
  const body = error?.response?.data;
  const status = error?.response?.status;
  const requestUrl = error?.config?.url || "";

  if (status === 401) {
    if (/\/api\/auth\/login$/.test(requestUrl)) {
      return "Email or password is incorrect.";
    }
    return "Your session has expired. Please sign in again.";
  }

  if (status === 403) {
    return "You do not have permission to perform this action.";
  }

  if (body?.errors && typeof body.errors === "object") {
    const details = Object.values(body.errors).filter(Boolean).join(" ");
    if (details) return details;
  }

  if (status === 400) {
    return body?.message || "Please check the submitted information and try again.";
  }

  if (status === 404) {
    return body?.message || "The requested FinFlow resource could not be found.";
  }

  if (status === 409) {
    return body?.message || "This request conflicts with existing data. Review it and try again.";
  }

  if (status >= 500) {
    if (status === 503 && body?.message) return body.message;
    return "The server could not complete the request. Please try again.";
  }

  if (status) {
    return body?.message || `FinFlow could not complete the request (HTTP ${status}).`;
  }

  if (
    !error?.response &&
    (error?.request ||
      ["ECONNREFUSED", "ERR_NETWORK", "ETIMEDOUT"].includes(error?.code))
  ) {
    return "Unable to connect to FinFlow backend. Check that the backend is running.";
  }

  return error?.message || "Request failed.";
}

export default api;
