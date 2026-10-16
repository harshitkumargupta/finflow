import { useMemo, useState } from "react";
import { Pencil, Plus, Target, Trash2 } from "lucide-react";
import { createBudget, deleteBudget, updateBudget } from "../api/financeApi";
import { getApiErrorMessage } from "../api/axios";
import EmptyState from "../components/EmptyState";
import ErrorMessage from "../components/ErrorMessage";
import LoadingSkeleton from "../components/LoadingSkeleton";
import PageHeader from "../components/PageHeader";
import StatusBadge from "../components/StatusBadge";
import PaceBar from "../components/plan/PaceBar";
import { BUDGET_CATEGORIES, categoryLabel } from "../constants/categories";
import { useToast } from "../context/ToastContext";
import { useBudgets } from "../hooks/useFinanceData";
import { longDate, monthBounds, parseISODate, startOfToday } from "../utils/dates";
import { toAmountString } from "../utils/format";
import { addAmounts, compareAmounts, divideAmount, formatMoney, sumAmounts } from "../utils/money";

const emptyForm = () => ({ category: "OVERALL", budgetAmount: "", ...monthBounds() });
const budgetName = (category) => (category === "OVERALL" ? "Everything" : categoryLabel(category));

function pace(budget) {
  const start = parseISODate(budget.startDate);
  const end = parseISODate(budget.endDate);
  const today = startOfToday();
  const total = Math.round((end - start) / 86400000) + 1;
  const elapsed = Math.min(total, Math.max(0, Math.round((today - start) / 86400000) + 1));
  return { total, elapsed, daysLeft: Math.max(0, total - elapsed + 1), percent: (elapsed / total) * 100 };
}

function story(budget, timing) {
  const remaining = addAmounts(budget.budgetAmount, budget.spentAmount, -1);
  const projected = budget.projectedMonthEnd;
  const over = projected != null ? addAmounts(projected, budget.budgetAmount, -1) : null;
  const perDay = compareAmounts(remaining, "0") > 0 && timing.daysLeft > 0 ? divideAmount(remaining, timing.daysLeft) : null;
  const endLabel = longDate(budget.endDate);
  if (budget.status === "EXCEEDED") return `You’re ${formatMoney(addAmounts(budget.spentAmount, budget.budgetAmount, -1))} over this budget. Anything more in ${budgetName(budget.category).toLowerCase()} adds to that.`;
  if (budget.status === "PROJECTED_OVER" && over) return `At this pace you’ll reach about ${formatMoney(projected)} by ${endLabel}, roughly ${formatMoney(over)} over.${perDay ? ` Keeping to ${formatMoney(perDay)} a day keeps you inside.` : ""}`;
  if (budget.status === "WARNING_80") return `${formatMoney(remaining)} left for ${timing.daysLeft} ${timing.daysLeft === 1 ? "day" : "days"}${perDay ? `, about ${formatMoney(perDay)} a day` : ""}.`;
  return `${formatMoney(remaining)} left.${projected != null ? ` At this pace you’ll spend about ${formatMoney(projected)} by ${endLabel}, within budget.` : ""}`;
}

export default function Budgets() {
  const notify = useToast();
  const { data: budgets, loading, error: loadError, reload: load } = useBudgets();
  const [form, setForm] = useState(emptyForm);
  const [editing, setEditing] = useState(null);
  const [formOpen, setFormOpen] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  const current = budgets.filter((item) => item.periodStatus === "ACTIVE");
  const other = budgets.filter((item) => item.periodStatus !== "ACTIVE");
  const totals = useMemo(() => {
    const overall = current.find((item) => item.category === "OVERALL");
    const included = overall ? [overall] : current.filter((item) => item.category !== "OVERALL");
    return { limit: sumAmounts(included.map((item) => item.budgetAmount)), spent: sumAmounts(included.map((item) => item.spentAmount)), count: included.length };
  }, [current]);

  function startEdit(item) {
    setEditing(item.id);
    setForm({ category: item.category, budgetAmount: String(item.budgetAmount), startDate: item.startDate, endDate: item.endDate });
    setFormOpen(true);
  }
  function closeForm() {
    setFormOpen(false);
    setEditing(null);
    setForm(emptyForm());
  }
  async function submit(event) {
    event.preventDefault();
    setError("");
    setBusy(true);
    try {
      const payload = { ...form, budgetAmount: toAmountString(form.budgetAmount, "Budget amount") };
      if (editing) await updateBudget(editing, payload);
      else await createBudget(payload);
      await load();
      notify(editing ? "Budget updated." : "Budget created.");
      closeForm();
    } catch (e) {
      setError(e?.isValidationError ? e.message : getApiErrorMessage(e));
    } finally {
      setBusy(false);
    }
  }
  async function remove(item) {
    if (!window.confirm(`Delete the ${budgetName(item.category)} budget?`)) return;
    try {
      await deleteBudget(item.id);
      await load();
      notify("Budget deleted.");
    } catch (e) {
      setError(getApiErrorMessage(e));
    }
  }

  const header = <PageHeader eyebrow="Plan" title="Budgets" description="A limit for a period. The marker shows where you’d be today if you spent evenly." action={!formOpen && <button type="button" className="button-primary" onClick={() => setFormOpen(true)}><Plus size={16} aria-hidden="true" /> New budget</button>} />;
  if (loading && !budgets.length) return <div className="space-y-10">{header}<LoadingSkeleton variant="rows" count={3} /></div>;

  const renderBudget = (item) => {
    const timing = pace(item);
    const tone = item.status === "EXCEEDED" ? "danger" : item.status === "WARNING_80" || item.status === "PROJECTED_OVER" ? "warm" : "accent";
    const spentPercent = Number(item.percentageUsed) || 0;
    const projectedPercent = item.projectedMonthEnd != null && Number(item.budgetAmount) > 0 ? (Number(item.projectedMonthEnd) / Number(item.budgetAmount)) * 100 : null;
    const active = item.periodStatus === "ACTIVE";
    return (
      <li key={item.id} className="py-5">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div className="min-w-0">
            <p className="flex flex-wrap items-center gap-2 text-[15px] font-semibold text-ink">{budgetName(item.category)} {active ? <StatusBadge status={item.status} /> : <StatusBadge status={item.periodStatus} />}</p>
            <p className="mt-0.5 text-xs text-muted">{longDate(item.startDate)} – {longDate(item.endDate)}</p>
          </div>
          <div className="flex items-start gap-3">
            <p className="text-right text-sm"><span className="tabular font-semibold text-ink">{formatMoney(item.spentAmount)}</span> <span className="text-muted">of</span> <span className="tabular text-ink-soft">{formatMoney(item.budgetAmount)}</span></p>
            <div className="-mr-2 -mt-2 flex">
              <button type="button" className="icon-action" aria-label={`Edit ${budgetName(item.category)} budget`} onClick={() => startEdit(item)}><Pencil size={15} /></button>
              <button type="button" className="icon-action hover:text-danger" aria-label={`Delete ${budgetName(item.category)} budget`} onClick={() => remove(item)}><Trash2 size={15} /></button>
            </div>
          </div>
        </div>
        <div className="mt-3">
          <PaceBar spentPercent={spentPercent} pacePercent={active ? timing.percent : null} projectedPercent={active ? projectedPercent : null} tone={tone} label={`${budgetName(item.category)}: ${Math.round(spentPercent)}% used, ${Math.round(timing.percent)}% of the period gone`} />
        </div>
        {active && <p className={`mt-3 text-sm leading-6 ${item.status === "EXCEEDED" ? "text-danger" : "text-ink-soft"}`}>{story(item, timing)}</p>}
      </li>
    );
  };

  return (
    <div className="space-y-10">
      {header}
      <ErrorMessage message={error || loadError} onDismiss={() => setError("")} />

      {formOpen && (
        <form onSubmit={submit} className="motion-content grid gap-4 rounded-lg border border-line bg-surface p-5 sm:grid-cols-2">
          <h2 className="section-title sm:col-span-2">{editing ? "Edit budget" : "New budget"}</h2>
          <label className="field-label">Category<select className="field-control" value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })}>{BUDGET_CATEGORIES.map((category) => <option key={category} value={category}>{budgetName(category)}</option>)}</select></label>
          <label className="field-label">Limit<input className="field-control tabular" required inputMode="decimal" placeholder="0.00" value={form.budgetAmount} onChange={(e) => setForm({ ...form, budgetAmount: e.target.value })} /></label>
          <label className="field-label">Starts<input className="field-control" type="date" required value={form.startDate} onChange={(e) => setForm({ ...form, startDate: e.target.value })} /></label>
          <label className="field-label">Ends<input className="field-control" type="date" required min={form.startDate} value={form.endDate} onChange={(e) => setForm({ ...form, endDate: e.target.value })} /></label>
          <p className="text-[13px] text-muted sm:col-span-2">“Everything” covers all spending. If you have one, Safe-to-Spend uses it instead of the category budgets.</p>
          <div className="flex gap-2 sm:col-span-2"><button className="button-primary" disabled={busy}>{busy ? "Saving…" : editing ? "Save changes" : "Create budget"}</button><button type="button" className="button-ghost" onClick={closeForm}>Cancel</button></div>
        </form>
      )}

      {current.length > 0 && (
        <p className="text-[15px] leading-6 text-ink-soft">This period you’ve spent <span className="tabular font-semibold text-ink">{formatMoney(totals.spent)}</span> of <span className="tabular font-semibold text-ink">{formatMoney(totals.limit)}</span>{totals.count > 1 ? ` across ${totals.count} budgets` : ""}.</p>
      )}

      {budgets.length ? (
        <>
          {current.length > 0 && <section aria-labelledby="current-heading"><h2 id="current-heading" className="sr-only">Current budgets</h2><ul className="divide-y divide-line border-y border-line">{current.map(renderBudget)}</ul></section>}
          {other.length > 0 && (
            <section aria-labelledby="other-heading">
              <h2 id="other-heading" className="font-display text-xl font-medium text-ink">Other periods</h2>
              <ul className="mt-2 divide-y divide-line border-y border-line">{other.map(renderBudget)}</ul>
            </section>
          )}
        </>
      ) : !formOpen && (
        <EmptyState icon={Target} title="No budgets yet" description="Start with one limit for everything this month. You can add categories later." action={<button type="button" className="button-primary" onClick={() => setFormOpen(true)}>Create a budget</button>} />
      )}
    </div>
  );
}
