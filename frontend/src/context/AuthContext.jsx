import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";
import { useNavigate } from "react-router-dom";
import {
  getCurrentUser,
  loginRequest,
  registerRequest,
} from "../api/authApi";
import {
  resetUnauthorizedState,
  setUnauthorizedHandler,
  TOKEN_KEY,
} from "../api/axios";
import { useToast } from "./ToastContext";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const navigate = useNavigate();
  const notify = useToast();
  const [user, setUser] = useState(null);
  const [ready, setReady] = useState(false);

  const logout = useCallback(
    (redirect = true, message = "You have signed out.") => {
      localStorage.removeItem(TOKEN_KEY);
      setUser(null);
      setReady(true);
      if (message) notify(message, "success");
      if (redirect) navigate("/login", { replace: true });
    },
    [navigate, notify],
  );

  useEffect(() => {
    setUnauthorizedHandler(() =>
      logout(true, "Your session has expired. Please sign in again."),
    );
    return () => setUnauthorizedHandler(undefined);
  }, [logout]);

  useEffect(() => {
    const token = localStorage.getItem(TOKEN_KEY);
    if (!token) {
      setReady(true);
      return;
    }
    getCurrentUser()
      .then(({ data }) => setUser(data))
      .catch(() => {
        localStorage.removeItem(TOKEN_KEY);
        setUser(null);
      })
      .finally(() => setReady(true));
  }, []);

  const login = useCallback(
    async (credentials, destination = "/dashboard") => {
      const { data } = await loginRequest(credentials);
      if (!data?.token) throw new Error("The server did not return a JWT.");
      localStorage.setItem(TOKEN_KEY, data.token);
      resetUnauthorizedState();
      try {
        const profile = await getCurrentUser();
        setUser(profile.data);
        notify("Welcome back to FinFlow.");
      } catch (error) {
        localStorage.removeItem(TOKEN_KEY);
        throw error;
      }
      navigate(destination, { replace: true });
    },
    [navigate, notify],
  );

  const register = useCallback(async (registration) => {
    await registerRequest(registration);
  }, []);

  const refreshUser = useCallback(async () => {
    const { data } = await getCurrentUser();
    setUser(data);
    return data;
  }, []);

  const value = useMemo(
    () => ({ user, ready, isAuthenticated: Boolean(user), login, logout, register, refreshUser }),
    [user, ready, login, logout, register, refreshUser],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth must be used inside AuthProvider");
  return context;
}
