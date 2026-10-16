import { useState } from "react";
import { Link } from "react-router-dom";
import { ArrowRight } from "lucide-react";
import { getApiErrorMessage } from "../../api/axios";
import { createGoal, createPurchasePlan, planGoal } from "../../api/financeApi";
import { PLANNING_CATEGORIES, categoryLabel } from "../../constants/categories";
import { useToast } from "../../context/ToastContext";
import { longDate } from "../../utils/dates";
import { toAmountString } from "../../utils/format";
import { addAmounts, compareAmounts, formatMoney } from "../../utils/money";
import { humanize } from "../../utils/status";
import MoneyMoveLoader from "../MoneyMoveLoader";

const tidy = (text) => (/^[A-Z_]+$/.test(text) ? humanize(text) : text);

const MODES = [
  { key: "afford", label: "Can I afford it?" },
  { key: "goal", label: "Save for something" },
];

function Figure({ label, value, tone = "text-ink", note }) {
  return (
    <div className="min-w-0">
      <dt className="text-[13px] text-muted">{label}</dt>
      <dd className={`tabular mt-0.5 text-base font-semibold ${tone}`}>{value}</dd>
      {note && <dd className="mt-0.5 text-xs leading-5 text-muted">{note}</dd>}
    </div>
  );
}

function AffordResult({ result, description }) {
  const verdict = {
    SAFE: { tone: "text-accent-strong", line: "Yes, comfortably." },
    CAUTION: { tone: "text-warm", line: "You can, but it’s tight." },
    NOT_RECOMMENDED: { tone: "text-danger", line: "Not right now." },
  }[result.recommendation] || { tone: "text-ink", line: humanize(result.recommendation) };
  const goalCommitments = result.goalImpact != null ? addAmounts(result.goalImpact, result.purchaseAmount, -1) : null;
  return (
    <div className="motion-content mt-6 border-t border-line pt-5" aria-live="polite">
      <p className={`font-display text-[1.75rem] font-medium leading-tight ${verdict.tone}`}>{verdict.line}</p>
      <p className="mt-2 text-[15px] leading-6 text-ink-soft">
        {description ? <>Spending <span className="tabular font-semibold text-ink">{formatMoney(result.purchaseAmount)}</span> on {description}</> : <>Spending <span className="tabular font-semibold text-ink">{formatMoney(result.purchaseAmount)}</span></>} would leave you <span className="tabular font-semibold text-ink">{formatMoney(result.safeToSpendAfter)}</span> free to spend over the next 30 days. {result.explanation}
      </p>
      <dl className="mt-5 grid gap-4 sm:grid-cols-3">
        <Figure label="Free to spend now" value={formatMoney(result.safeToSpendBefore)} />
        <Figure label="Afterwards" value={formatMoney(result.safeToSpendAfter)} tone={compareAmounts(result.safeToSpendAfter, "0") < 0 ? "text-danger" : "text-ink"} />
        {result.budgetImpact != null
          ? <Figure label="Left in that budget" value={formatMoney(result.budgetImpact)} tone={compareAmounts(result.budgetImpact, "0") < 0 ? "text-danger" : "text-ink"} />
          : <Figure label="Budget" value="No active budget" tone="text-muted" note="Pick a category with a budget to see its effect." />}
      </dl>
      {goalCommitments != null && <p className="mt-4 text-[13px] leading-5 text-muted">Your scheduled goal contributions ({formatMoney(goalCommitments)}) are already set aside, so this purchase doesn’t take from them.</p>}
    </div>
  );
}

function GoalResult({ plan, onCreate, creating, created }) {
  const rec = plan.recommendation;
  const snap = plan.snapshot;
  return (
    <div className="motion-content mt-6 border-t border-line pt-5" aria-live="polite">
      <p className={`font-display text-[1.75rem] font-medium leading-tight ${rec.achievable ? "text-accent-strong" : "text-warm"}`}>
        {rec.achievable ? "Yes, that’s within reach." : "Not on that timeline."}
      </p>
      <p className="mt-2 text-[15px] leading-6 text-ink-soft">
        {rec.goalName ? <>For <span className="font-semibold text-ink">{rec.goalName}</span>, saving </> : "Saving "}
        <span className="tabular font-semibold text-ink">{formatMoney(rec.chosenMonthlySaving ?? rec.requiredMonthlySavings)}</span> a month gets you to <span className="tabular font-semibold text-ink">{formatMoney(rec.targetAmount)}</span>
        {rec.projectedCompletionDate || rec.estimatedCompletionDate ? <> by {longDate(rec.projectedCompletionDate || rec.estimatedCompletionDate)}</> : null}. {rec.recommendation}
      </p>
      <dl className="mt-5 grid gap-4 sm:grid-cols-3">
        <Figure label="Still to save" value={formatMoney(rec.remainingAmount)} />
        <Figure label={rec.targetDate ? `Needed per month to hit ${longDate(rec.targetDate)}` : "Needed per month"} value={rec.requiredMonthlySavings != null ? formatMoney(rec.requiredMonthlySavings) : "—"} />
        <Figure label="Typically left over each month" value={rec.estimatedAvailableMonthlySurplus != null ? formatMoney(rec.estimatedAvailableMonthlySurplus) : "—"} note="Average income minus spending, bills and autopay." />
      </dl>
      {(rec.keepProtected?.length > 0 || rec.reduceIfNeeded?.length > 0) && (
        <div className="mt-5 grid gap-5 sm:grid-cols-2">
          {rec.keepProtected?.length > 0 && <div><p className="text-[13px] font-semibold text-ink">Keep protected</p><ul className="mt-1 space-y-1 text-sm text-ink-soft">{rec.keepProtected.map((item) => <li key={item}>{tidy(item)}</li>)}</ul></div>}
          {rec.reduceIfNeeded?.length > 0 && <div><p className="text-[13px] font-semibold text-ink">Could trim if needed</p><ul className="mt-1 space-y-1 text-sm text-ink-soft">{rec.reduceIfNeeded.map((item) => <li key={item}>{tidy(item)}</li>)}</ul></div>}
        </div>
      )}
      {rec.alternatives?.length > 0 && <p className="mt-4 text-[13px] leading-5 text-muted">Other ways: {rec.alternatives.join(" · ")}</p>}
      {snap && <p className="mt-4 text-xs leading-5 text-muted">Based on {formatMoney(snap.currentAvailableMoney)} available now, average income {formatMoney(snap.averageMonthlyIncome)} and spending {formatMoney(snap.averageMonthlySpending)} a month.</p>}
      {onCreate && (
        <div className="mt-5">
          {created ? <Link to="/goals" className="button-secondary">Goal created, see it in Goals <ArrowRight size={15} aria-hidden="true" /></Link>
            : <button type="button" className="button-primary" disabled={creating || !rec.targetAmount} onClick={() => onCreate(rec)}>{creating ? "Creating…" : "Save this as a goal"}</button>}
        </div>
      )}
    </div>
  );
}

export default function PlannerPanel({ initialMode = "afford", allowGoalCreation = true }) {
  const notify = useToast();
  const [mode, setMode] = useState(initialMode);
  const [afford, setAfford] = useState({ amount: "", description: "", category: "" });
  const [goal, setGoal] = useState({ prompt: "", targetAmount: "", targetDate: "", chosenMonthlySaving: "" });
  const [moreOpen, setMoreOpen] = useState(false);
  const [result, setResult] = useState(null);
  const [busy, setBusy] = useState(false);
  const [creating, setCreating] = useState(false);
  const [created, setCreated] = useState(false);
  const [error, setError] = useState("");

  function switchMode(next) {
    setMode(next);
    setResult(null);
    setError("");
  }

  async function submit(event) {
    event.preventDefault();
    setBusy(true);
    setError("");
    setCreated(false);
    try {
      if (mode === "afford") {
        const { data } = await createPurchasePlan({ amount: toAmountString(afford.amount), description: afford.description.trim() || undefined, category: afford.category || undefined });
        setResult({ mode, data, description: afford.description.trim() });
      } else {
        const payload = {
          prompt: goal.prompt.trim(),
          ...(goal.targetAmount ? { targetAmount: toAmountString(goal.targetAmount, "Target amount") } : {}),
          ...(goal.targetDate ? { targetDate: goal.targetDate } : {}),
          ...(goal.chosenMonthlySaving ? { chosenMonthlySaving: toAmountString(goal.chosenMonthlySaving, "Monthly saving") } : {}),
        };
        const { data } = await planGoal(payload);
        setResult({ mode, data });
      }
    } catch (cause) {
      setError(cause?.isValidationError ? cause.message : getApiErrorMessage(cause));
    } finally {
      setBusy(false);
    }
  }

  async function createFromPlan(rec) {
    setCreating(true);
    try {
      await createGoal({ name: (rec.goalName || "New goal").slice(0, 120), targetAmount: String(rec.targetAmount), currentAmount: "0.00", targetDate: rec.targetDate || null, priority: rec.necessity === "NECESSARY" ? "NECESSARY" : "IMPORTANT", description: goal.prompt.trim().slice(0, 500) });
      setCreated(true);
      notify("Goal created.");
    } catch (cause) {
      setError(getApiErrorMessage(cause));
    } finally {
      setCreating(false);
    }
  }

  return (
    <section className="rounded-lg border border-line bg-surface p-5 sm:p-6" aria-label="Planner">
      <div role="tablist" aria-label="What do you want to check?" className="inline-flex rounded-md border border-line bg-paper p-0.5">
        {MODES.map((item) => (
          <button key={item.key} type="button" role="tab" aria-selected={mode === item.key} onClick={() => switchMode(item.key)} className={`rounded-[7px] px-3 py-1.5 text-[13px] font-medium transition-colors duration-150 ${mode === item.key ? "bg-ink text-paper" : "text-ink-soft hover:text-ink"}`}>{item.label}</button>
        ))}
      </div>

      <form onSubmit={submit} className="mt-5">
        {mode === "afford" ? (
          <>
            <label htmlFor="afford-amount" className="font-display text-xl text-ink">How much is it, and what for?</label>
            <div className="mt-3 flex flex-col gap-3 sm:flex-row">
              <div className="flex items-baseline gap-1.5 rounded-md border border-line-strong bg-surface px-3 focus-within:border-accent focus-within:ring-2 focus-within:ring-accent/25 sm:w-44">
                <span className="text-muted" aria-hidden="true">₹</span>
                <input id="afford-amount" className="tabular min-h-11 w-full bg-transparent text-[15px] text-ink placeholder:text-muted focus:outline-none" inputMode="decimal" required placeholder="0.00" value={afford.amount} onChange={(e) => setAfford({ ...afford, amount: e.target.value })} />
              </div>
              <input aria-label="What is it for" className="field-control flex-1" maxLength={120} placeholder="New headphones" value={afford.description} onChange={(e) => setAfford({ ...afford, description: e.target.value })} />
            </div>
            <label className="mt-3 flex flex-wrap items-center gap-2 text-[13px] text-ink-soft">Count it against
              <select className="field-control min-h-9 w-auto py-1 text-[13px]" value={afford.category} onChange={(e) => setAfford({ ...afford, category: e.target.value })}>
                <option value="">no particular budget</option>
                {PLANNING_CATEGORIES.map((category) => <option key={category} value={category}>{categoryLabel(category)}</option>)}
              </select>
            </label>
          </>
        ) : (
          <>
            <label htmlFor="goal-prompt" className="font-display text-xl text-ink">What are you saving for?</label>
            <input id="goal-prompt" className="field-control mt-3" maxLength={255} required placeholder="A laptop for ₹80,000 in 6 months" value={goal.prompt} onChange={(e) => setGoal({ ...goal, prompt: e.target.value })} />
            <button type="button" className="button-ghost -ml-3 mt-1 text-[13px]" aria-expanded={moreOpen} onClick={() => setMoreOpen(!moreOpen)}>{moreOpen ? "Hide details" : "Add exact numbers"}</button>
            {moreOpen && (
              <div className="motion-content mt-2 grid gap-3 sm:grid-cols-3">
                <label className="field-label">Target amount<input className="field-control tabular" inputMode="decimal" value={goal.targetAmount} onChange={(e) => setGoal({ ...goal, targetAmount: e.target.value })} /></label>
                <label className="field-label">By when<input className="field-control" type="date" value={goal.targetDate} onChange={(e) => setGoal({ ...goal, targetDate: e.target.value })} /></label>
                <label className="field-label">I can save a month<input className="field-control tabular" inputMode="decimal" value={goal.chosenMonthlySaving} onChange={(e) => setGoal({ ...goal, chosenMonthlySaving: e.target.value })} /></label>
              </div>
            )}
          </>
        )}
        <div className="mt-4 flex flex-wrap items-center gap-3">
          <button className="button-primary" disabled={busy}>{busy ? <MoneyMoveLoader label="Working it out" tone="on-accent" /> : mode === "afford" ? "Check" : "Make a plan"}</button>
          <p className="text-xs text-muted">Worked out from your own balances, bills, budgets and goals. Nothing is moved or blocked.</p>
        </div>
      </form>

      {error && <p className="mt-4 text-sm text-danger" role="alert">{error}</p>}
      {result?.mode === "afford" && mode === "afford" && <AffordResult result={result.data} description={result.description} />}
      {result?.mode === "goal" && mode === "goal" && <GoalResult plan={result.data} onCreate={allowGoalCreation ? createFromPlan : null} creating={creating} created={created} />}
    </section>
  );
}
