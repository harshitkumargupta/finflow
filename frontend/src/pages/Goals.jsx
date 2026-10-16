import { useState } from "react";
import { Link } from "react-router-dom";
import { PiggyBank, Pencil, Plus, Repeat2, Sparkles, Trash2 } from "lucide-react";
import { getApiErrorMessage } from "../api/axios";
import { contributeToGoal, createGoal, createGoalSavingsAutopay, deleteGoal, updateGoal } from "../api/financeApi";
import { accountName } from "../components/AccountSelect";
import EmptyState from "../components/EmptyState";
import ErrorMessage from "../components/ErrorMessage";
import GoalContributionHistory from "../components/GoalContributionHistory";
import LoadingSkeleton from "../components/LoadingSkeleton";
import MoneyText from "../components/MoneyText";
import PageHeader from "../components/PageHeader";
import StatusBadge from "../components/StatusBadge";
import Drawer from "../components/plan/Drawer";
import GoalPath from "../components/plan/GoalPath";
import { useToast } from "../context/ToastContext";
import { combineResources, useAccounts, useGoals } from "../hooks/useFinanceData";
import { longDate, todayISO } from "../utils/dates";
import { maskAccountNumber, toAmountString } from "../utils/format";
import { compareAmounts, formatMoney } from "../utils/money";
import { humanize } from "../utils/status";

const PRIORITIES = ["NECESSARY", "IMPORTANT", "OPTIONAL"];
const PRIORITY_STYLE = { NECESSARY: "border-ink/20 text-ink", IMPORTANT: "border-accent/30 text-accent-strong", OPTIONAL: "border-line-strong text-muted" };
const emptyForm = () => ({ name: "", targetAmount: "", currentAmount: "0", targetDate: "", description: "", priority: "IMPORTANT", accountId: "" });

function goalStory(goal) {
  if (goal.status === "COMPLETED" || compareAmounts(goal.remainingAmount, "0") <= 0) return { tone: "text-accent-strong", text: `Done. You saved the full ${formatMoney(goal.targetAmount)}.` };
  if (!goal.targetDate) return { tone: "text-ink-soft", text: `${formatMoney(goal.remainingAmount)} to go, with no deadline. Add a target date to get a monthly figure.` };
  if (goal.onTrack) return { tone: "text-accent-strong", text: `On track. ${formatMoney(goal.requiredMonthlySavings)}/month keeps you on schedule for ${longDate(goal.targetDate)}.` };
  return { tone: "text-warm", text: `Behind. You’d need ${formatMoney(goal.requiredMonthlySavings)}/month from now to make ${longDate(goal.targetDate)}.` };
}

export default function Goals() {
  const notify = useToast();
  const goalsResource = useGoals();
  const accountsResource = useAccounts();
  const goals = goalsResource.data;
  const accounts = accountsResource.data;
  const { loading, error: loadError, reload: load } = combineResources(goalsResource, accountsResource);
  const [form, setForm] = useState(emptyForm);
  const [editing, setEditing] = useState(null);
  const [formOpen, setFormOpen] = useState(false);
  const [panel, setPanel] = useState({});
  const [contributions, setContributions] = useState({});
  const [mandates, setMandates] = useState({});
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  const togglePanel = (id, name) => setPanel((current) => ({ ...current, [id]: current[id] === name ? null : name }));
  const fail = (cause) => setError(cause?.isValidationError ? cause.message : getApiErrorMessage(cause));

  function edit(goal) {
    setEditing(goal.id);
    setForm({ name: goal.name, targetAmount: String(goal.targetAmount), currentAmount: String(goal.currentAmount), targetDate: goal.targetDate || "", description: goal.description || "", priority: goal.priority || "IMPORTANT", accountId: goal.accountId || "" });
    setFormOpen(true);
  }
  function closeForm() {
    setFormOpen(false);
    setEditing(null);
    setForm(emptyForm());
  }
  async function submit(event) {
    event.preventDefault();
    setBusy(true);
    setError("");
    try {
      const payload = { ...form, targetAmount: toAmountString(form.targetAmount, "Target amount"), currentAmount: toAmountString(form.currentAmount, "Already saved", { allowZero: true }), targetDate: form.targetDate || null, accountId: form.accountId ? Number(form.accountId) : null };
      if (editing) await updateGoal(editing, payload);
      else await createGoal(payload);
      await load();
      notify(editing ? "Goal updated." : "Goal created.");
      closeForm();
    } catch (cause) { fail(cause); } finally { setBusy(false); }
  }
  async function addProgress(goal) {
    setBusy(true);
    setError("");
    try {
      await contributeToGoal(goal.id, toAmountString(contributions[goal.id], "Amount"));
      await load();
      setContributions((current) => ({ ...current, [goal.id]: "" }));
      notify("Progress added. No money was moved between accounts.");
    } catch (cause) { fail(cause); } finally { setBusy(false); }
  }
  function mandateFor(goal) {
    return mandates[goal.id] || { accountId: goal.accountId || accounts[0]?.id || "", amount: goal.requiredMonthlySavings && Number(goal.requiredMonthlySavings) > 0 ? String(goal.requiredMonthlySavings) : "", frequency: "MONTHLY", nextPaymentDate: todayISO() };
  }
  const updateMandate = (goal, changes) => setMandates((current) => ({ ...current, [goal.id]: { ...mandateFor(goal), ...changes } }));
  async function startAutoSaving(goal) {
    const request = mandateFor(goal);
    setBusy(true);
    setError("");
    try {
      await createGoalSavingsAutopay(goal.id, { accountId: Number(request.accountId), amount: toAmountString(request.amount), frequency: request.frequency, nextPaymentDate: request.nextPaymentDate });
      await load();
      setPanel((current) => ({ ...current, [goal.id]: null }));
      notify("Automatic saving set up (sandbox).");
    } catch (cause) { fail(cause); } finally { setBusy(false); }
  }
  async function remove(goal) {
    if (!window.confirm(`Delete “${goal.name}”? Its history goes too.`)) return;
    try { await deleteGoal(goal.id); await load(); notify("Goal deleted."); } catch (cause) { fail(cause); }
  }

  const header = <PageHeader eyebrow="Plan" title="Goals" description="Money you’re putting towards something. Adding progress here records it; it doesn’t move money between accounts." action={!formOpen && <><button type="button" className="button-primary" onClick={() => setFormOpen(true)}><Plus size={16} aria-hidden="true" /> New goal</button><Link to="/goals/assistant" className="button-secondary"><Sparkles size={16} aria-hidden="true" /> Plan one with numbers</Link></>} />;
  if (loading && !goals.length) return <div className="space-y-10">{header}<LoadingSkeleton count={2} /></div>;

  return (
    <div className="space-y-10">
      {header}
      <ErrorMessage message={error || loadError} onDismiss={() => setError("")} />

      {formOpen && (
        <form onSubmit={submit} className="motion-content grid gap-4 rounded-lg border border-line bg-surface p-5 sm:grid-cols-2">
          <h2 className="section-title sm:col-span-2">{editing ? "Edit goal" : "New goal"}</h2>
          <label className="field-label sm:col-span-2">What for<input className="field-control" maxLength={120} required placeholder="Emergency fund, laptop, trip…" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} /></label>
          <label className="field-label">Target<input className="field-control tabular" inputMode="decimal" required placeholder="0.00" value={form.targetAmount} onChange={(e) => setForm({ ...form, targetAmount: e.target.value })} /></label>
          <label className="field-label">Already saved<input className="field-control tabular" inputMode="decimal" required value={form.currentAmount} onChange={(e) => setForm({ ...form, currentAmount: e.target.value })} /></label>
          <label className="field-label"><span>By when <span className="text-xs font-normal text-muted">optional</span></span><input className="field-control" type="date" min={todayISO()} value={form.targetDate} onChange={(e) => setForm({ ...form, targetDate: e.target.value })} /></label>
          <label className="field-label">Priority<select className="field-control" value={form.priority} onChange={(e) => setForm({ ...form, priority: e.target.value })}>{PRIORITIES.map((value) => <option key={value} value={value}>{humanize(value)}</option>)}</select></label>
          <label className="field-label sm:col-span-2"><span>Note <span className="text-xs font-normal text-muted">optional</span></span><input className="field-control" maxLength={500} value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} /></label>
          <div className="flex gap-2 sm:col-span-2"><button className="button-primary" disabled={busy}>{busy ? "Saving…" : editing ? "Save changes" : "Create goal"}</button><button type="button" className="button-ghost" onClick={closeForm}>Cancel</button></div>
        </form>
      )}

      {goals.length ? (
        <ul className="space-y-4">
          {goals.map((goal) => {
            const percent = Math.max(0, Math.min(100, Number(goal.percentageComplete || 0)));
            const story = goalStory(goal);
            const completed = goal.status === "COMPLETED" || compareAmounts(goal.remainingAmount, "0") <= 0;
            const behind = Boolean(goal.targetDate) && !goal.onTrack && !completed;
            const mandate = mandateFor(goal);
            const open = panel[goal.id];
            return (
              <li key={goal.id} className="rounded-lg border border-line bg-surface p-5 sm:p-6">
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div className="min-w-0">
                    <h2 className="flex flex-wrap items-center gap-2 font-display text-xl font-medium text-ink">{goal.name}
                      <span className={`rounded-full border px-2 py-0.5 font-sans text-[11px] font-semibold uppercase tracking-wider ${PRIORITY_STYLE[goal.priority] || PRIORITY_STYLE.OPTIONAL}`}>{humanize(goal.priority || "IMPORTANT")}</span>
                      {completed && <StatusBadge status="COMPLETED" />}
                    </h2>
                    <p className="mt-1 text-xs text-muted">{goal.targetDate ? `Target ${longDate(goal.targetDate)}` : "No deadline"}{goal.estimatedCompletionDate && !completed ? ` · at this pace, done by ${longDate(goal.estimatedCompletionDate)}` : ""}</p>
                  </div>
                  <div className="-mr-2 -mt-1 flex">
                    <button type="button" className="icon-action" aria-label={`Edit ${goal.name}`} onClick={() => edit(goal)}><Pencil size={15} /></button>
                    <button type="button" className="icon-action hover:text-danger" aria-label={`Delete ${goal.name}`} onClick={() => remove(goal)}><Trash2 size={15} /></button>
                  </div>
                </div>

                <div className="mt-5 flex flex-wrap items-end justify-between gap-x-6 gap-y-1">
                  <MoneyText value={goal.currentAmount} size="md" />
                  <p className="text-sm text-muted">of <span className="tabular font-semibold text-ink-soft">{formatMoney(goal.targetAmount)}</span> · <span className="tabular">{percent.toFixed(0)}%</span></p>
                </div>
                <div className="mt-2"><GoalPath percent={percent} behind={behind} completed={completed} label={`${goal.name}: ${percent.toFixed(0)}% saved`} /></div>
                <p className={`mt-3 text-sm leading-6 ${story.tone}`}>{story.text}</p>

                <div className="mt-4 flex flex-wrap items-center gap-x-4 gap-y-2 border-t border-line pt-4 text-[13px]">
                  <span className="flex items-center gap-1.5 text-ink-soft"><Repeat2 size={14} aria-hidden="true" />
                    {goal.activeSavingsAutopayStatus ? <>Saving automatically · {humanize(goal.activeSavingsAutopayStatus).toLowerCase()}{goal.nextScheduledContribution ? <>, next on <span className="font-semibold text-ink">{longDate(goal.nextScheduledContribution)}</span></> : null}</> : "No automatic saving"}
                  </span>
                  {!completed && (
                    <div className="ml-auto flex flex-wrap gap-1">
                      <button type="button" className="button-secondary min-h-9 px-3 text-[13px]" aria-expanded={open === "add"} onClick={() => togglePanel(goal.id, "add")}>Add progress</button>
                      {!goal.activeSavingsAutopayId && <button type="button" className="button-ghost min-h-9 px-3 text-[13px]" aria-expanded={open === "auto"} onClick={() => togglePanel(goal.id, "auto")}>Save automatically</button>}
                    </div>
                  )}
                </div>

                <Drawer open={open === "add"}>
                  <div className="flex flex-wrap items-end gap-2 pt-4">
                    <label className="field-label w-40">Amount<input className="field-control tabular" inputMode="decimal" placeholder="0.00" value={contributions[goal.id] || ""} onChange={(e) => setContributions((current) => ({ ...current, [goal.id]: e.target.value }))} /></label>
                    <button type="button" className="button-primary" disabled={busy || !contributions[goal.id]} onClick={() => addProgress(goal)}>Add</button>
                    <p className="basis-full text-xs text-muted">Records money you’ve already put aside. Up to {formatMoney(goal.remainingAmount)} more.</p>
                  </div>
                </Drawer>
                <Drawer open={open === "auto"}>
                  <div className="grid gap-3 pt-4 sm:grid-cols-2 lg:grid-cols-4">
                    <label className="field-label">From<select className="field-control" value={mandate.accountId} onChange={(e) => updateMandate(goal, { accountId: e.target.value })}>{accounts.map((account) => <option key={account.id} value={account.id}>{accountName(account)} {maskAccountNumber(account.accountNumber)}</option>)}</select></label>
                    <label className="field-label">Amount<input className="field-control tabular" inputMode="decimal" value={mandate.amount} onChange={(e) => updateMandate(goal, { amount: e.target.value })} /></label>
                    <label className="field-label">How often<select className="field-control" value={mandate.frequency} onChange={(e) => updateMandate(goal, { frequency: e.target.value })}><option value="DAILY">Daily</option><option value="WEEKLY">Weekly</option><option value="MONTHLY">Monthly</option></select></label>
                    <label className="field-label">Starting<input className="field-control" type="date" min={todayISO()} value={mandate.nextPaymentDate} onChange={(e) => updateMandate(goal, { nextPaymentDate: e.target.value })} /></label>
                    <div className="flex flex-wrap items-center gap-3 sm:col-span-2 lg:col-span-4">
                      <button type="button" className="button-primary" disabled={busy || !mandate.accountId || !mandate.amount || !accounts.length} onClick={() => startAutoSaving(goal)}>Start saving</button>
                      <p className="text-xs text-muted">{goal.requiredMonthlySavings ? `${formatMoney(goal.requiredMonthlySavings)} a month would keep this on schedule. ` : ""}Sandbox: simulated debits only.</p>
                    </div>
                  </div>
                </Drawer>
                <GoalContributionHistory goalId={goal.id} refreshKey={goal.currentAmount} />
              </li>
            );
          })}
        </ul>
      ) : !formOpen && (
        <EmptyState icon={PiggyBank} title="No goals yet" description="Pick something you’re saving for. FinFlow will tell you what each month needs to look like." action={<div className="flex flex-wrap gap-2"><button type="button" className="button-primary" onClick={() => setFormOpen(true)}>Create a goal</button><Link to="/goals/assistant" className="button-secondary">Plan one with numbers</Link></div>} />
      )}
    </div>
  );
}
