import { useEffect, useState } from "react";
import { Smartphone } from "lucide-react";
import { getApiErrorMessage } from "../api/axios";
import { sendPhoneOtp, verifyPhoneOtp } from "../api/phoneApi";
import { useAuth } from "../context/AuthContext";
import { useToast } from "../context/ToastContext";
import OtpInput from "./OtpInput";
import StatusBadge from "./StatusBadge";

export default function PhoneVerifyCard() {
  const { user, refreshUser } = useAuth();
  const notify = useToast();
  const [phase, setPhase] = useState("idle");
  const [code, setCode] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [note, setNote] = useState(null);
  const [cooldown, setCooldown] = useState(0);

  useEffect(() => {
    if (cooldown <= 0) return undefined;
    const timer = setInterval(() => setCooldown((value) => Math.max(0, value - 1)), 1000);
    return () => clearInterval(timer);
  }, [cooldown]);

  if (!user) return null;

  const verified = user.phoneVerified === true;

  async function requestCode() {
    setBusy(true);
    setError("");
    setNote(null);
    try {
      const { data } = await sendPhoneOtp();
      if (data?.phoneVerified) {
        await refreshUser();
        return;
      }
      setPhase("sent");
      setCode("");
      setCooldown(60);
      setNote({ tone: data?.delivered ? "ok" : "warn", text: data?.message });
    } catch (requestError) {
      setNote({ tone: "warn", text: getApiErrorMessage(requestError) });
    } finally {
      setBusy(false);
    }
  }

  async function submitCode(submitted) {
    const value = submitted || code;
    if (value.length !== 6 || busy) return;
    setBusy(true);
    setError("");
    setNote(null);
    try {
      const { data } = await verifyPhoneOtp(value);
      if (data?.phoneVerified) {
        await refreshUser();
        notify("Your mobile number is verified.");
        setPhase("idle");
        setCode("");
      }
    } catch (verifyError) {
      const status = verifyError?.response?.data?.status;
      setError(getApiErrorMessage(verifyError));
      setCode("");
      if (status === "LOCKED" || status === "EXPIRED") setCooldown(0);
    } finally {
      setBusy(false);
    }
  }

  return (
    <section id="verify-phone" aria-labelledby="verify-phone-heading" className="scroll-mt-24 rounded-xl border border-line bg-surface p-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-2.5">
          <Smartphone size={18} strokeWidth={1.75} className="shrink-0 text-ink-soft" aria-hidden="true" />
          <h3 id="verify-phone-heading" className="text-sm font-semibold text-ink">Mobile number</h3>
        </div>
        <StatusBadge status={verified ? "VERIFIED" : "PENDING"} label={verified ? "Verified" : "Not verified"} />
      </div>
      <p className="mt-2 text-[13px] leading-5 text-ink-soft">
        {verified
          ? <>Your mobile number <span className="font-medium text-ink">{user.phone}</span> is verified.</>
          : <>Confirm <span className="font-medium text-ink">{user.phone}</span> with a 6-digit code. In this sandbox the code is emailed and also written to the server log.</>}
      </p>

      {!verified && (
        <div className="mt-4">
          {phase === "idle" ? (
            <button type="button" className="button-primary" onClick={requestCode} disabled={busy}>{busy ? "Sending…" : "Send code"}</button>
          ) : (
            <div className="space-y-4">
              <OtpInput value={code} onChange={setCode} onComplete={submitCode} error={error} disabled={busy} autoFocus label="6-digit code" />
              <div className="flex flex-wrap items-center gap-3">
                <button type="button" className="button-primary" onClick={() => submitCode()} disabled={busy || code.length !== 6}>{busy ? "Verifying…" : "Verify"}</button>
                <button type="button" className="button-ghost" onClick={requestCode} disabled={busy || cooldown > 0}>{cooldown > 0 ? `Resend in ${cooldown}s` : "Resend code"}</button>
              </div>
            </div>
          )}
          {note?.text && <p role="status" className={`mt-3 text-[13px] leading-5 ${note.tone === "ok" ? "text-accent-strong" : "text-warm"}`}>{note.text}</p>}
        </div>
      )}
    </section>
  );
}
