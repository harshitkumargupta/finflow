import { useState } from "react";
import { Link, Navigate, useNavigate } from "react-router-dom";
import { getApiErrorMessage } from "../api/axios";
import { useAuth } from "../context/AuthContext";
import { useToast } from "../context/ToastContext";
import AuthLayout from "../components/AuthLayout";
import MoneyMoveLoader from "../components/MoneyMoveLoader";

export default function Register() {
  const { register, isAuthenticated, ready } = useAuth();
  const notify = useToast();
  const navigate = useNavigate();
  const [form, setForm] = useState({ name: "", email: "", phone: "", password: "" });
  const [fieldErrors, setFieldErrors] = useState({});
  const [busy, setBusy] = useState(false);

  if (ready && isAuthenticated) return <Navigate to="/dashboard" replace />;

  async function submit(event) {
    event.preventDefault();
    const trimmedName = form.name.trim();
    if (!trimmedName) {
      setFieldErrors({ name: "Name is required." });
      return;
    }
    setFieldErrors({});
    setBusy(true);
    try {
      await register({
        name: trimmedName,
        email: form.email.trim(),
        phone: form.phone,
        password: form.password,
      });
      setForm((current) => ({ ...current, password: "" }));
      navigate("/login", {
        replace: true,
        state: { registeredEmail: form.email.trim() },
      });
    } catch (requestError) {
      const errors = requestError?.response?.data?.errors;
      if (requestError?.response?.status === 400 && errors) {
        setFieldErrors(errors);
        notify(getApiErrorMessage(requestError), "error");
      } else {
        notify(getApiErrorMessage(requestError), "error");
      }
    } finally {
      setBusy(false);
    }
  }

  function updateField(field, value) {
    setForm((current) => ({ ...current, [field]: value }));
    setFieldErrors((current) => {
      if (!current[field]) return current;
      const next = { ...current };
      delete next[field];
      return next;
    });
  }

  return (
    <AuthLayout
      eyebrow="Get started"
      title="Create your account"
      description="It takes a minute. This is a sandbox, so no real bank details are ever asked for."
      footer={<>Already have an account? <Link to="/login" className="font-semibold text-accent-strong underline-offset-2 hover:underline">Sign in</Link></>}
    >
          <form onSubmit={submit} className="grid gap-5 sm:grid-cols-2">
            <label className="field-label sm:col-span-2">
              Your name
              <input className="field-control" name="name" autoComplete="name" value={form.name} onChange={(e) => updateField("name", e.target.value)} required aria-invalid={Boolean(fieldErrors.name)} aria-describedby={fieldErrors.name ? "name-error" : undefined} />
              {fieldErrors.name && <span id="name-error" className="text-xs font-normal text-danger">{fieldErrors.name}</span>}
            </label>
            <label className="field-label sm:col-span-2">
              Email
              <input className="field-control" type="email" name="email" autoComplete="email" value={form.email} onChange={(e) => updateField("email", e.target.value)} required aria-invalid={Boolean(fieldErrors.email)} aria-describedby={fieldErrors.email ? "email-error" : undefined} />
              {fieldErrors.email && <span id="email-error" className="text-xs font-normal text-danger">{fieldErrors.email}</span>}
            </label>
            <label className="field-label">
              Mobile number
              <input className="field-control" type="tel" name="phone" inputMode="numeric" pattern="[0-9]{10}" minLength={10} maxLength={10} autoComplete="tel" value={form.phone} onChange={(e) => updateField("phone", e.target.value.replace(/\D/g, "").slice(0, 10))} required aria-invalid={Boolean(fieldErrors.phone)} aria-describedby={fieldErrors.phone ? "phone-error" : "phone-hint"} />
              {fieldErrors.phone ? <span id="phone-error" className="text-xs font-normal text-danger">{fieldErrors.phone}</span> : <span id="phone-hint" className="text-xs font-normal text-muted">Enter a unique 10-digit number.</span>}
            </label>
            <label className="field-label">
              Password
              <input className="field-control" type="password" name="password" autoComplete="new-password" minLength={8} maxLength={72} value={form.password} onChange={(e) => updateField("password", e.target.value)} required aria-invalid={Boolean(fieldErrors.password)} aria-describedby={fieldErrors.password ? "password-error" : "password-hint"} />
              {fieldErrors.password ? <span id="password-error" className="text-xs font-normal text-danger">{fieldErrors.password}</span> : <span id="password-hint" className="text-xs font-normal text-muted">At least 8 characters.</span>}
            </label>
            <button className="button-primary sm:col-span-2" disabled={busy}>
              {busy && <MoneyMoveLoader label="" tone="on-accent" />}
              {busy ? "Creating account..." : "Create account"}
            </button>
          </form>
    </AuthLayout>
  );
}
