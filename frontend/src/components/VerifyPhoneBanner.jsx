import { Link } from "react-router-dom";
import { Smartphone } from "lucide-react";
import { useAuth } from "../context/AuthContext";

export default function VerifyPhoneBanner() {
  const { user } = useAuth();

  if (!user || user.phoneVerified !== false) return null;

  return (
    <section aria-labelledby="verify-phone-heading" className="mb-8 flex flex-wrap items-start gap-x-4 gap-y-3 rounded-lg border border-warm/30 bg-warm-soft px-4 py-3.5">
      <Smartphone size={18} strokeWidth={1.75} className="mt-0.5 shrink-0 text-warm" aria-hidden="true" />
      <div className="min-w-0 flex-1">
        <h2 id="verify-phone-heading" className="text-sm font-semibold text-ink">Verify your mobile number</h2>
        <p className="mt-0.5 text-[13px] leading-5 text-ink-soft">Confirm <span className="font-medium text-ink">{user.phone}</span> with a 6-digit code. Everything works while you wait.</p>
      </div>
      <Link to="/profile#verify-phone" className="button-secondary min-h-9 px-3 py-1.5 text-[13px]">Verify number</Link>
    </section>
  );
}
