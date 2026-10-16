import { useState } from "react";
import { ArrowRight } from "lucide-react";
import AuthLayout from "../components/AuthLayout";
import { Link, Navigate, useLocation } from "react-router-dom";
import { getApiErrorMessage } from "../api/axios";
import { useAuth } from "../context/AuthContext";

export default function Login() {
  const { login, isAuthenticated, ready } = useAuth();
  const location = useLocation();
  const [form, setForm] = useState({
    email: location.state?.registeredEmail || "",
    password: "",
  });
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  if (ready && isAuthenticated) return <Navigate to="/dashboard" replace />;

  async function submit(event) {
    event.preventDefault();
    setBusy(true);
    setError("");
    try {
      await login(form, location.state?.from?.pathname || "/dashboard");
    } catch (requestError) {
      setError(getApiErrorMessage(requestError));
    } finally {
      setBusy(false);
    }
  }

  return (
    <AuthLayout
      eyebrow="Welcome back"
      title="Sign in"
      description="Use the email and password you registered with."
      footer={<>New to FinFlow? <Link className="font-semibold text-accent-strong underline-offset-2 hover:underline" to="/register">Create an account</Link></>}
    >
      {location.state?.registeredEmail && <p className="success-message mb-6" role="status">Your account is ready. We sent a link to confirm your email; you can sign in now and verify any time.</p>}
      <form onSubmit={submit} className="space-y-5" noValidate={false}>
        <label className="field-label">Email<input className="field-control" type="email" name="email" autoComplete="email" value={form.email} onChange={(event) => setForm({ ...form, email: event.target.value })} required /></label>
        <label className="field-label">Password<input className="field-control" type="password" name="password" autoComplete="current-password" value={form.password} onChange={(event) => setForm({ ...form, password: event.target.value })} required /></label>
        {error && <p className="rounded-md border border-danger/25 bg-danger-soft px-3 py-2 text-sm text-danger" role="alert">{error}</p>}
        <button className="button-primary w-full" disabled={busy}>{busy ? "Signing in…" : <>Sign in <ArrowRight size={17} aria-hidden="true" /></>}</button>
      </form>
    </AuthLayout>
  );
}
