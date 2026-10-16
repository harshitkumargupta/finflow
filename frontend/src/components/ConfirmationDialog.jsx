import { useEffect, useRef } from "react";
import { X } from "lucide-react";
import { compareAmounts, formatMoney } from "../utils/money";
import MoneyMoveLoader from "./MoneyMoveLoader";
import MoneyText from "./MoneyText";
import PinInput from "./PinInput";

export default function ConfirmationDialog({
  open,
  title,
  amount,
  details = [],
  fee = "0.00",
  balanceAfter,
  balanceLabel = "Your balance afterwards",
  busy = false,
  confirmLabel = "Confirm",
  onConfirm,
  onCancel,
  pin = "",
  onPinChange,
  pinError = "",
}) {
  const dialogRef = useRef(null);

  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) return;
    if (open && !dialog.open) {
      dialog.showModal();
      requestAnimationFrame(() => dialog.querySelector("[data-autofocus]")?.focus());
    }
    if (!open && dialog.open) dialog.close();
  }, [open]);

  const needsPin = Boolean(onPinChange);
  const pinReady = !needsPin || pin.length === 6;
  const goesNegative = balanceAfter != null && compareAmounts(balanceAfter, "0") < 0;

  return (
    <dialog
      ref={dialogRef}
      onCancel={(event) => { event.preventDefault(); if (!busy) onCancel(); }}
      className="sheet w-[min(100%-2rem,28rem)] overflow-y-auto rounded-xl p-0 text-ink shadow-raised"
      aria-labelledby="confirmation-title"
    >
      <form method="dialog" onSubmit={(event) => { event.preventDefault(); if (pinReady && !busy) onConfirm(); }} className="p-6">
        <div className="flex items-start justify-between gap-4">
          <div>
            <p className="eyebrow">Review before you confirm</p>
            <h2 id="confirmation-title" className="mt-1 font-display text-2xl font-medium">{title}</h2>
          </div>
          <button type="button" className="icon-action -mr-2 -mt-1" onClick={onCancel} disabled={busy} aria-label="Close review"><X size={18} /></button>
        </div>

        <div className="mt-5"><MoneyText value={amount} size="lg" /></div>

        <dl className="mt-5 divide-y divide-line border-y border-line text-sm">
          {details.filter((row) => row.value).map(({ label, value }) => (
            <div className="flex justify-between gap-4 py-2.5" key={label}>
              <dt className="text-muted">{label}</dt>
              <dd className="max-w-[65%] break-words text-right font-medium">{value}</dd>
            </div>
          ))}
          <div className="flex justify-between gap-4 py-2.5">
            <dt className="text-muted">Fee</dt>
            <dd className="tabular text-right font-medium">{compareAmounts(fee, "0") === 0 ? "No fee" : formatMoney(fee)}</dd>
          </div>
          {balanceAfter != null && (
            <div className="flex justify-between gap-4 py-3">
              <dt className="text-ink-soft">{balanceLabel}</dt>
              <dd className={`tabular text-right text-base font-semibold ${goesNegative ? "text-warm" : "text-ink"}`}>{formatMoney(balanceAfter)}</dd>
            </div>
          )}
        </dl>
        {goesNegative && <p className="mt-3 text-[13px] leading-5 text-warm">This is more than the balance shown. FinFlow will check again when you confirm and stop it if there isn’t enough.</p>}

        {needsPin && <div className="mt-5"><PinInput value={pin} onChange={onPinChange} error={pinError} disabled={busy} autoFocus /></div>}
        {!needsPin && pinError && <p className="mt-4 rounded-md border border-danger/25 bg-danger-soft px-3 py-2 text-sm text-danger" role="alert">{pinError}</p>}

        <div className="mt-6 flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
          <button type="button" className="button-secondary" onClick={onCancel} disabled={busy}>Go back</button>
          <button type="submit" className="button-primary sm:min-w-40" disabled={busy || !pinReady}>
            {busy ? <MoneyMoveLoader label="Confirming" tone="on-accent" /> : confirmLabel}
          </button>
        </div>
        <p className="mt-4 text-center text-xs text-muted">Sandbox: no real money moves.</p>
      </form>
    </dialog>
  );
}
