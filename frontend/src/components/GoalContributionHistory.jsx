import { useEffect, useState } from "react";
import { ChevronDown, History } from "lucide-react";
import { getApiErrorMessage } from "../api/axios";
import { getGoalContributions } from "../api/financeApi";
import { formatCurrency, formatDateTime } from "../utils/format";
import { humanize } from "../utils/status";
import LoadingSkeleton from "./LoadingSkeleton";

const SOURCE_LABELS = { INITIAL: "Starting amount", MANUAL: "Added manually", AUTOPAY: "Savings autopay", ADJUSTMENT: "Edited" };

export default function GoalContributionHistory({ goalId, refreshKey }) {
  const [open, setOpen] = useState(false);
  const [items, setItems] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!open) return undefined;
    let active = true;
    setLoading(true);
    setError("");
    getGoalContributions(goalId)
      .then(({ data }) => { if (active) setItems(data); })
      .catch((cause) => { if (active) setError(getApiErrorMessage(cause)); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [open, goalId, refreshKey]);

  return (
    <div className="mt-4 border-t border-slate-100 pt-4">
      <button type="button" className="flex items-center gap-2 text-sm font-semibold text-slate-600 hover:text-ink" onClick={() => setOpen(!open)} aria-expanded={open}>
        <History size={16} /> Contribution history
        <ChevronDown size={16} className={`transition-transform duration-200 ${open ? "rotate-180" : ""}`} />
      </button>
      {open && (
        <div className="motion-content mt-3">
          {loading && !items ? <LoadingSkeleton variant="rows" count={2} /> : error ? <p className="text-sm text-rose-700" role="alert">{error}</p> : items?.length ? (
            <ol className="space-y-2">
              {items.map((item) => (
                <li key={item.id} className="flex items-center justify-between gap-3 rounded-lg bg-slate-50 px-3 py-2 text-sm">
                  <div className="min-w-0">
                    <p className="font-medium text-ink">{SOURCE_LABELS[item.source] || humanize(item.source)}</p>
                    <p className="text-xs text-slate-500">{formatDateTime(item.createdAt)}{item.reference ? ` · ${item.reference}` : ""}</p>
                  </div>
                  <div className="text-right">
                    <p className={`amount font-semibold ${Number(item.amount) >= 0 ? "text-emerald-700" : "text-rose-700"}`}>{Number(item.amount) >= 0 ? "+" : "−"}{formatCurrency(Math.abs(Number(item.amount)))}</p>
                    <p className="amount text-xs text-slate-500">Total {formatCurrency(item.goalAmountAfter)}</p>
                  </div>
                </li>
              ))}
            </ol>
          ) : <p className="text-sm text-slate-500">No contributions recorded yet. History starts from when this ledger was enabled.</p>}
        </div>
      )}
    </div>
  );
}
