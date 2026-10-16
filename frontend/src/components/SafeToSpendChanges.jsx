import { useState } from "react";
import { ChevronDown, HelpCircle } from "lucide-react";
import { getApiErrorMessage } from "../api/axios";
import { getSafeToSpendChanges } from "../api/financeApi";
import { formatCurrency } from "../utils/format";
import LoadingSkeleton from "./LoadingSkeleton";

function signed(value) {
  const amount = Number(value);
  if (!Number.isFinite(amount) || amount === 0) return formatCurrency(0);
  return `${amount > 0 ? "+" : "−"}${formatCurrency(Math.abs(amount))}`;
}

export default function SafeToSpendChanges() {
  const [open, setOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [data, setData] = useState(null);

  async function toggle() {
    const next = !open;
    setOpen(next);
    if (!next || data) return;
    setLoading(true);
    setError("");
    try {
      const response = await getSafeToSpendChanges();
      setData(response.data);
    } catch (cause) {
      setError(getApiErrorMessage(cause));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="border-t border-line pt-4">
      <button type="button" className="button-ghost -ml-3" onClick={toggle} aria-expanded={open}>
        <HelpCircle size={16} /> Why did this change?
        <ChevronDown size={16} className={`transition-transform duration-200 ${open ? "rotate-180" : ""}`} />
      </button>
      {open && (
        <div className="motion-content mt-3" aria-live="polite">
          {loading ? <LoadingSkeleton variant="rows" count={2} /> : error ? <p className="text-sm text-rose-700" role="alert">{error}</p> : data && (
            <>
              <p className="text-sm text-slate-600">{data.summary}</p>
              {data.drivers?.length > 0 && (
                <ul className="stagger mt-3 space-y-2">
                  {data.drivers.map((driver) => (
                    <li key={`${driver.type}-${driver.description}`} className="flex items-start justify-between gap-3 border-b border-line py-2.5 last:border-b-0">
                      <p className="text-sm text-slate-700">{driver.explanation}</p>
                      <span className={`amount shrink-0 text-sm font-semibold ${Number(driver.impact) >= 0 ? "text-emerald-700" : "text-rose-700"}`}>{signed(driver.impact)}</span>
                    </li>
                  ))}
                </ul>
              )}
              {data.baselineAvailable && !data.drivers?.length && <p className="mt-2 text-xs text-slate-500">Nothing that feeds Safe-to-Spend has changed since then.</p>}
            </>
          )}
        </div>
      )}
    </div>
  );
}
