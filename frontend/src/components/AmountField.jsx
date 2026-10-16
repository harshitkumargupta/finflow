import { useId } from "react";
import { formatMoney } from "../utils/money";

export default function AmountField({ label = "Amount", value, onChange, available, availableLabel = "Available", required = true, autoFocus = false }) {
  const id = useId();
  return (
    <div>
      <label htmlFor={id} className="text-sm font-medium text-ink-soft">{label}</label>
      <div className="mt-1.5 flex items-baseline gap-2 border-b-2 border-line-strong pb-2 transition-colors duration-150 focus-within:border-accent">
        <span className="font-display text-3xl text-muted" aria-hidden="true">₹</span>
        <input
          id={id}
          type="text"
          inputMode="decimal"
          autoComplete="off"
          placeholder="0.00"
          required={required}
          autoFocus={autoFocus}
          value={value}
          onChange={(event) => onChange(event.target.value.replace(/[^\d.]/g, "").replace(/(\..*)\./g, "$1").replace(/(\.\d{2})\d+/, "$1"))}
          className="tabular w-full min-w-0 bg-transparent font-display text-[2.5rem] font-medium leading-none text-ink placeholder:text-line-strong focus:outline-none"
          aria-describedby={available != null ? `${id}-available` : undefined}
        />
      </div>
      {available != null && <p id={`${id}-available`} className="mt-2 text-[13px] text-muted">{availableLabel}: <span className="tabular font-semibold text-ink-soft">{formatMoney(available)}</span></p>}
    </div>
  );
}
