import { useEffect, useState } from "react";

export default function PaceBar({ spentPercent, pacePercent, projectedPercent, tone = "accent", label }) {
  const [drawn, setDrawn] = useState(false);
  useEffect(() => { const id = requestAnimationFrame(() => setDrawn(true)); return () => cancelAnimationFrame(id); }, []);
  const clamp = (value) => Math.max(0, Math.min(100, Number(value) || 0));
  const fill = { accent: "bg-accent", warm: "bg-warm-400", danger: "bg-danger" }[tone];
  return (
    <div className="relative pt-4" role="img" aria-label={label}>
      {pacePercent != null && (
        <span className="absolute top-0 -translate-x-1/2 text-[10px] font-semibold uppercase tracking-wider text-muted transition-[left] duration-500 ease-out" style={{ left: `${clamp(pacePercent)}%` }} aria-hidden="true">Today</span>
      )}
      <div className="relative h-2.5 overflow-hidden rounded-full bg-surface-2">
        {projectedPercent != null && <span className="absolute inset-y-0 left-0 rounded-full bg-line-strong/60 transition-[width] duration-700 ease-out" style={{ width: drawn ? `${clamp(projectedPercent)}%` : "0%" }} aria-hidden="true" />}
        <span className={`absolute inset-y-0 left-0 rounded-full ${fill} transition-[width] duration-700 ease-out`} style={{ width: drawn ? `${clamp(spentPercent)}%` : "0%" }} aria-hidden="true" />
      </div>
      {pacePercent != null && <span className="absolute bottom-[-3px] top-3.5 w-0.5 -translate-x-1/2 rounded-full bg-ink transition-[left] duration-500 ease-out" style={{ left: `${clamp(pacePercent)}%` }} aria-hidden="true" />}
    </div>
  );
}
