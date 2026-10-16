import { useEffect, useRef, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import AuthLayout from "../components/AuthLayout";
import { verifyEmailRequest } from "../api/authApi";
import { useAuth } from "../context/AuthContext";

export default function VerifyEmail() {
  const [params, setParams] = useSearchParams();
  const { isAuthenticated, refreshUser } = useAuth();
  const [token] = useState(() => params.get("token") || "");
  const [state, setState] = useState(token ? "checking" : "missing");
  const started = useRef(false);

  useEffect(() => {
    if (!token || started.current) return;
    started.current = true;
    setParams({}, { replace: true });
    verifyEmailRequest(token)
      .then(() => {
        setState("verified");
        if (isAuthenticated) refreshUser().catch(() => {});
      })
      .catch(() => setState("invalid"));
  }, [token, setParams, isAuthenticated, refreshUser]);

  const copy = {
    checking: ["One moment", "Checking your link", "Confirming your email address…"],
    verified: ["All set", "Email verified", "Thanks for confirming. Your FinFlow email is verified."],
    invalid: ["Link didn't work", "This link can't be used", "It may be expired, already used, or replaced by a newer link. Sign in and choose “Resend link” to get a fresh one."],
    missing: ["Link didn't work", "No verification link found", "Open the link from your email again, or sign in and choose “Resend link”."],
  }[state];

  return (
    <AuthLayout eyebrow={copy[0]} title={copy[1]} description={copy[2]}>
      <div role="status" aria-live="polite" className="sr-only">{copy[1]}</div>
      {state !== "checking" && (
        <Link to={isAuthenticated ? "/dashboard" : "/login"} className="button-primary w-full">{isAuthenticated ? "Go to overview" : "Sign in"}</Link>
      )}
    </AuthLayout>
  );
}
