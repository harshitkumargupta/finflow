import { useState } from "react";
import { MailCheck } from "lucide-react";
import { getApiErrorMessage } from "../api/axios";
import { resendVerificationEmail } from "../api/authApi";
import { useAuth } from "../context/AuthContext";

export default function VerifyEmailBanner() {
  const { user, refreshUser } = useAuth();
  const [busy, setBusy] = useState(false);
  const [note, setNote] = useState(null);

  if (!user || user.emailVerified !== false) return null;

  async function resend() {
    setBusy(true);
    setNote(null);
    try {
      const { data } = await resendVerificationEmail();
      if (data?.emailVerified) await refreshUser();
      setNote({ tone: data?.delivered ? "ok" : "warn", text: data?.message });
    } catch (error) {
      setNote({ tone: "warn", text: getApiErrorMessage(error) });
    } finally {
      setBusy(false);
    }
  }

  return (
    <section aria-labelledby="verify-email-heading" className="mb-8 flex flex-wrap items-start gap-x-4 gap-y-3 rounded-lg border border-warm/30 bg-warm-soft px-4 py-3.5">
      <MailCheck size={18} strokeWidth={1.75} className="mt-0.5 shrink-0 text-warm" aria-hidden="true" />
      <div className="min-w-0 flex-1">
        <h2 id="verify-email-heading" className="text-sm font-semibold text-ink">Verify your email</h2>
        <p className="mt-0.5 text-[13px] leading-5 text-ink-soft">We sent a confirmation link to <span className="font-medium text-ink">{user.email}</span>. Everything works while you wait.</p>
        {note?.text && <p role="status" className={`mt-1.5 text-[13px] leading-5 ${note.tone === "ok" ? "text-accent-strong" : "text-warm"}`}>{note.text}</p>}
      </div>
      <button type="button" className="button-secondary min-h-9 px-3 py-1.5 text-[13px]" onClick={resend} disabled={busy}>{busy ? "Sending…" : "Resend link"}</button>
    </section>
  );
}
