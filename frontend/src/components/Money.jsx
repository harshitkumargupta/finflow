import AnimatedNumber from "./AnimatedNumber";
import { formatCurrency } from "../utils/format";

export default function Money({ value, className = "" }) {
  if (value == null || value === "" || !Number.isFinite(Number(value))) return <span className={className}>—</span>;
  return <span className={`amount ${className}`}><AnimatedNumber value={Number(value)} formatter={formatCurrency} /></span>;
}
