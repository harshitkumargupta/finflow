import useCountUp from "../hooks/useCountUp";
import { moneyParts } from "../utils/money";

const sizes = {
  hero: "text-[clamp(3rem,8vw,4.75rem)] leading-[0.92]",
  xl: "text-[2.5rem] leading-none",
  lg: "text-[2rem] leading-none",
  md: "text-[1.5rem] leading-tight",
  sm: "text-[1.125rem] leading-tight",
};

export default function MoneyText({ value, size = "lg", className = "", tone, animate = false, duration }) {
  const live = useCountUp(animate ? value : null, duration);
  const parts = moneyParts(animate ? live : value);
  if (!parts) return <span className={`money font-semibold text-muted ${sizes[size] || ""} ${className}`}>—</span>;
  const toneClass = tone || (parts.negative ? "text-danger" : "text-ink");
  return (
    <span className={`money inline-flex items-baseline font-semibold ${sizes[size] || ""} ${toneClass} ${className}`}>
      {parts.negative && <span aria-hidden="true">−</span>}
      <span className="mr-[0.05em] inline-block translate-y-[-0.42em] text-[0.4em] font-medium text-muted" aria-hidden="true">{parts.symbol}</span>
      <span>{parts.whole}</span>
      <span className="ml-[0.02em] inline-block translate-y-[-0.46em] text-[0.4em] font-medium text-muted" aria-hidden="true">{parts.fraction}</span>
      <span className="sr-only">{parts.negative ? "minus " : ""}{parts.symbol}{parts.whole}.{parts.fraction}</span>
    </span>
  );
}
