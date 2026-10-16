import { useState } from "react";
import { Pencil, Trash2 } from "lucide-react";
import { getApiErrorMessage } from "../api/axios";
import { createIncomeSchedule, deleteIncomeSchedule, getIncomeSchedules, updateIncomeSchedule } from "../api/financeApi";
import useApiResource from "../hooks/useApiResource";
import { formatCurrency, formatDate, toAmountString } from "../utils/format";
import { humanize } from "../utils/status";
import EmptyState from "./EmptyState";
import ErrorMessage from "./ErrorMessage";
import LoadingSkeleton from "./LoadingSkeleton";

const FREQUENCIES = ["MONTHLY", "BIWEEKLY", "WEEKLY", "ONE_TIME"];
const today = () => new Date().toISOString().split("T")[0];
const emptyForm = () => ({ sourceName: "", amount: "", frequency: "MONTHLY", nextExpectedDate: today(), active: true });

export default function IncomeSchedulePanel({ onChange }) {
  const { data: schedules, loading, error: loadError, reload } = useApiResource(getIncomeSchedules, { initialData: [] });
  const [form, setForm] = useState(emptyForm);
  const [editing, setEditing] = useState(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  async function submit(event) {
    event.preventDefault();
    setBusy(true);
    setError("");
    try {
      const request = { ...form, sourceName: form.sourceName.trim(), amount: toAmountString(form.amount) };
      if (editing) await updateIncomeSchedule(editing, request);
      else await createIncomeSchedule(request);
      setEditing(null);
      setForm(emptyForm());
      await reload();
      onChange?.();
    } catch (cause) {
      setError(cause?.isValidationError ? cause.message : getApiErrorMessage(cause));
    } finally {
      setBusy(false);
    }
  }

  async function toggleActive(schedule) {
    setBusy(true);
    setError("");
    try {
      await updateIncomeSchedule(schedule.id, { sourceName: schedule.sourceName, amount: String(schedule.amount), frequency: schedule.frequency, nextExpectedDate: schedule.nextExpectedDate, active: !schedule.active });
      await reload();
      onChange?.();
    } catch (cause) {
      setError(getApiErrorMessage(cause));
    } finally {
      setBusy(false);
    }
  }

  async function remove(id) {
    if (!window.confirm("Delete this expected income?")) return;
    setError("");
    try {
      await deleteIncomeSchedule(id);
      await reload();
      onChange?.();
    } catch (cause) {
      setError(getApiErrorMessage(cause));
    }
  }

  function edit(schedule) {
    setEditing(schedule.id);
    setForm({ sourceName: schedule.sourceName, amount: String(schedule.amount), frequency: schedule.frequency, nextExpectedDate: schedule.nextExpectedDate, active: schedule.active });
  }

  return (
    <section aria-labelledby="income-heading" className="border-t border-line pt-10">
      <div>
        <div>
          <h2 id="income-heading" className="font-display text-2xl font-medium text-ink">Expected income</h2>
          <p className="mt-1 max-w-2xl text-sm leading-6 text-ink-soft">Scheduled income dated after today and within 30 days is added to Safe-to-Spend. Nothing is deposited automatically.</p>
        </div>
      </div>
      <ErrorMessage message={error || loadError} className="mt-4" onDismiss={() => setError("")} />
      <form onSubmit={submit} className="mt-5 grid gap-4 rounded-lg border border-line bg-surface p-5 sm:grid-cols-2 xl:grid-cols-4">
        <label className="field-label">Source<input className="field-control" required maxLength={120} placeholder="Salary" value={form.sourceName} onChange={(e) => setForm({ ...form, sourceName: e.target.value })} /></label>
        <label className="field-label">Amount<input className="field-control" required type="number" inputMode="decimal" min="0.01" step="0.01" value={form.amount} onChange={(e) => setForm({ ...form, amount: e.target.value })} /></label>
        <label className="field-label">Frequency<select className="field-control" value={form.frequency} onChange={(e) => setForm({ ...form, frequency: e.target.value })}>{FREQUENCIES.map((value) => <option key={value} value={value}>{humanize(value)}</option>)}</select></label>
        <label className="field-label">Next expected date<input className="field-control" required type="date" value={form.nextExpectedDate} onChange={(e) => setForm({ ...form, nextExpectedDate: e.target.value })} /></label>
        <div className="flex gap-2 sm:col-span-2 xl:col-span-4">
          <button className="button-primary" disabled={busy}>{busy ? "Saving…" : editing ? "Save income" : "Add expected income"}</button>
          {editing && <button type="button" className="button-secondary" onClick={() => { setEditing(null); setForm(emptyForm()); }}>Cancel</button>}
        </div>
      </form>
      <div className="mt-5">
        {loading && !schedules.length ? <LoadingSkeleton variant="rows" count={2} /> : schedules.length ? (
          <ul className="stagger space-y-2">
            {schedules.map((schedule) => (
              <li key={schedule.id} className={`flex flex-wrap items-center justify-between gap-3 border-b border-line py-3 last:border-b-0 ${schedule.active ? "" : "opacity-60"}`}>
                <div className="min-w-0">
                  <p className="font-medium text-ink">{schedule.sourceName} <span className="amount text-emerald-700">+{formatCurrency(schedule.amount)}</span></p>
                  <p className="text-xs text-slate-500">{humanize(schedule.frequency)} · next {formatDate(schedule.nextExpectedDate)} · {schedule.active ? `${formatCurrency(schedule.expectedWithinHorizon)} counted in the next 30 days` : "Paused, not counted"}</p>
                </div>
                <div className="flex items-center gap-1">
                  <button type="button" className="button-secondary px-3 py-1.5 text-xs" disabled={busy} onClick={() => toggleActive(schedule)}>{schedule.active ? "Pause" : "Resume"}</button>
                  <button type="button" className="icon-action" aria-label={`Edit ${schedule.sourceName}`} onClick={() => edit(schedule)}><Pencil size={16} /></button>
                  <button type="button" className="icon-action text-rose-600" aria-label={`Delete ${schedule.sourceName}`} onClick={() => remove(schedule.id)}><Trash2 size={16} /></button>
                </div>
              </li>
            ))}
          </ul>
        ) : <EmptyState title="No expected income yet" description="Add a salary or other expected payment to include it in Safe-to-Spend." />}
      </div>
    </section>
  );
}
