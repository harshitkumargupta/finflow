import { useEffect, useRef, useState } from "react";

export default function HoldToConfirmButton({ children = "Hold to confirm", onConfirm, disabled = false, className = "button-primary" }) {
  const timer = useRef(null);
  const [progress, setProgress] = useState(0);
  function stop() { window.clearInterval(timer.current); timer.current = null; setProgress(0); }
  function start() {
    if (disabled) return;
    const started = performance.now();
    timer.current = window.setInterval(() => {
      const next = Math.min(1, (performance.now() - started) / 700);
      setProgress(next);
      if (next === 1) { stop(); onConfirm(); }
    }, 16);
  }
  useEffect(() => () => stop(), []);
  return <button type="button" className={`relative overflow-hidden ${className}`} disabled={disabled} onPointerDown={start} onPointerUp={stop} onPointerCancel={stop} onKeyDown={(event) => { if ((event.key === "Enter" || event.key === " ") && !disabled) { event.preventDefault(); onConfirm(); } }}>
    <span className="absolute inset-y-0 left-0 origin-left rounded-xl bg-white/15" style={{ transform: `scaleX(${progress})`, right: 0 }} aria-hidden="true" />
    <span className="relative">{children}</span>
  </button>;
}
