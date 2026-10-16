import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import Loading from "./Loading";

export default function ProtectedRoute() {
  const { ready, isAuthenticated } = useAuth();
  const location = useLocation();

  if (!ready) return <Loading label="Checking your secure session..." />;
  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }
  return <Outlet />;
}
