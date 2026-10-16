import { useId, useRef } from "react";

export default function PinInput({ value = "", onChange, length = 6, label = "Transaction PIN", error, disabled = false, autoFocus = false }) {
  const refs = useRef([]);
  const id = useId();
  const digits = Array.from({ length }, (_, index) => value[index] || "");

  function setAt(index, digit) {
    const next = digits.slice();
    next[index] = digit;
    onChange(next.join("").slice(0, length));
  }

  function handleChange(index, event) {
    const typed = event.target.value.replace(/\D/g, "");
    if (!typed) { setAt(index, ""); return; }
    if (typed.length > 1) { fill(index, typed); return; }
    setAt(index, typed);
    if (index < length - 1) refs.current[index + 1]?.focus();
  }

  function fill(index, text) {
    const next = digits.slice();
    text.split("").slice(0, length - index).forEach((digit, offset) => { next[index + offset] = digit; });
    onChange(next.join(""));
    refs.current[Math.min(length - 1, index + text.length)]?.focus();
  }

  function handleKeyDown(index, event) {
    if (event.key === "Backspace" && !digits[index] && index > 0) {
      event.preventDefault();
      setAt(index - 1, "");
      refs.current[index - 1]?.focus();
    } else if (event.key === "ArrowLeft" && index > 0) {
      refs.current[index - 1]?.focus();
    } else if (event.key === "ArrowRight" && index < length - 1) {
      refs.current[index + 1]?.focus();
    }
  }

  return (
    <fieldset disabled={disabled} aria-describedby={error ? `${id}-error` : `${id}-hint`}>
      <legend className="text-sm font-medium text-ink-soft">{label}</legend>
      <div className="mt-2 flex gap-2" onPaste={(event) => { const text = event.clipboardData.getData("text").replace(/\D/g, ""); if (text) { event.preventDefault(); fill(0, text); } }}>
        {digits.map((digit, index) => (
          <input
            key={index}
            ref={(node) => { refs.current[index] = node; }}
            type="password"
            inputMode="numeric"
            autoComplete={index === 0 ? "one-time-code" : "off"}
            maxLength={length}
            value={digit}
            autoFocus={autoFocus && index === 0}
            data-autofocus={autoFocus && index === 0 ? "" : undefined}
            onChange={(event) => handleChange(index, event)}
            onKeyDown={(event) => handleKeyDown(index, event)}
            onFocus={(event) => event.target.select()}
            aria-label={`${label}, digit ${index + 1} of ${length}`}
            aria-invalid={Boolean(error)}
            className={`tabular h-12 w-11 rounded-md border bg-surface text-center text-xl font-semibold text-ink transition-colors duration-150 focus:border-accent focus:outline-none focus:ring-2 focus:ring-accent/25 sm:h-14 sm:w-12 ${error ? "border-danger" : digit ? "border-ink-soft/50" : "border-line-strong"}`}
          />
        ))}
      </div>
      {error ? <p id={`${id}-error`} className="mt-2 text-sm text-danger" role="alert">{error}</p> : <p id={`${id}-hint`} className="mt-2 text-xs text-muted">The 6-digit PIN you set when you created your FinFlow account.</p>}
    </fieldset>
  );
}
