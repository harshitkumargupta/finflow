import { useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { Check, Minus } from "lucide-react";
import useReducedMotion from "../hooks/useReducedMotion";
import AnimatedNumber from "./AnimatedNumber";
import MoneyText from "./MoneyText";
import ReceiptCard from "./ReceiptCard";
import { formatMoney } from "../utils/money";

export default function PaymentSuccess({
  status = "success",
  amount,
  fromLabel,
  toLabel,
  newBalance,
  balanceLabel = "Balance now",
  receiptTitle = "Receipt",
  rows = [],
  onAgain,
  againLabel = "Make another",
}) {
  const reduced = useReducedMotion();
  const failed = status === "failed";
  const [settled, setSettled] = useState(reduced);
  const [revealed, setRevealed] = useState(reduced);
  const [skipped, setSkipped] = useState(reduced);
  const regionRef = useRef(null);

  useEffect(() => { regionRef.current?.focus(); }, []);

  useEffect(() => {
    if (reduced) return undefined;
    const t1 = setTimeout(() => setSettled(true), 740);
    const t2 = setTimeout(() => setRevealed(true), 1000);
    return () => { clearTimeout(t1); clearTimeout(t2); };
  }, [reduced]);

  function skip() {
    setSkipped(true);
    setSettled(true);
    setRevealed(true);
  }

  const animate = !reduced && !skipped;

  return (
    <section ref={regionRef} tabIndex={-1} aria-live="polite" className="focus:outline-none" onClick={() => !revealed && skip()}>
      <div className="flex items-start justify-between gap-4">
        <p className="eyebrow">{failed ? "Payment" : "Payment sent"}</p>
        {animate && !revealed && <button type="button" className="button-ghost min-h-8 px-2 text-xs" onClick={(event) => { event.stopPropagation(); skip(); }}>Skip</button>}
      </div>

      <div className={`pay-journey mt-6 ${animate ? "is-animate" : ""} ${failed ? "is-fail" : ""}`} aria-hidden="true">
        <span className="pay-dot pay-dot-from" />
        <span className="pay-track"><span className="pay-track-fill" /></span>
        {!failed && <span className="pay-dot pay-dot-to" />}
        {animate && <span className="pay-bead" />}
        {settled && (
          <span className={`pay-seal ${failed ? "pay-seal-fail" : "pay-seal-success"} ${animate ? "pay-seal-in" : ""}`}>
            {failed ? <Minus size={17} strokeWidth={2.5} /> : <Check size={17} strokeWidth={2.75} className={animate ? "pay-check" : ""} />}
          </span>
        )}
      </div>

      <div className="mt-2 flex items-baseline justify-between text-[13px] text-muted">
        <span className="truncate">{fromLabel}</span>
        <span className="truncate text-right">{toLabel}</span>
      </div>

      {settled && (
        <div className="animate-fade-in mt-7">
          {failed ? (
            <>
              <h2 className="font-display text-[1.75rem] font-medium leading-tight text-warm">Couldn’t send that</h2>
              <p className="mt-2 text-[15px] leading-6 text-ink-soft">Your money is untouched — nothing left your account. You can try again whenever you’re ready.</p>
            </>
          ) : (
            <>
              <h2 className="flex flex-wrap items-baseline gap-x-2 font-display text-[1.75rem] font-medium leading-tight text-ink">
                <MoneyText value={amount} size="md" /> <span className="font-normal text-ink-soft">sent</span>
              </h2>
              {newBalance != null && (
                <p className="mt-2 text-[15px] leading-6 text-ink-soft">{balanceLabel} <AnimatedNumber value={newBalance} format={formatMoney} duration={700} className="num font-semibold text-ink" /></p>
              )}
            </>
          )}
        </div>
      )}

      {revealed && (
        <div className="pay-reveal animate-fade-in">
          <ReceiptCard title={receiptTitle} rows={rows} />
          <div className="mt-6 flex flex-wrap gap-2">
            <button type="button" className="button-primary" onClick={onAgain}>{againLabel}</button>
            <Link to="/transactions" className="button-secondary">View transactions</Link>
            <Link to="/dashboard" className="button-ghost">Back to overview</Link>
          </div>
        </div>
      )}
    </section>
  );
}
