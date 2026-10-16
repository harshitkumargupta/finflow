import { useEffect, useMemo, useState } from "react";
import { useLocation, useNavigate, useSearchParams } from "react-router-dom";
import { CalendarClock, Check, Pencil, Plus, Trash2 } from "lucide-react";
import { getApiErrorMessage } from "../api/axios";
import { createBill, deleteBill, updateBill } from "../api/financeApi";
import { accountName } from "../components/AccountSelect";
import EmptyState from "../components/EmptyState";
import ErrorMessage from "../components/ErrorMessage";
import IncomeSchedulePanel from "../components/IncomeSchedulePanel";
import LoadingSkeleton from "../components/LoadingSkeleton";
import PageHeader from "../components/PageHeader";
import { PLANNING_CATEGORIES, categoryLabel } from "../constants/categories";
import { useToast } from "../context/ToastContext";
import { combineResources, useAccounts, useBills } from "../hooks/useFinanceData";
import { addFrequency, daysFromToday, dueLabel, longDate, todayISO } from "../utils/dates";
import { maskAccountNumber, toAmountString } from "../utils/format";
import { formatMoney, sumAmounts } from "../utils/money";
import { humanize } from "../utils/status";

const FREQUENCIES = ["MONTHLY", "WEEKLY", "QUARTERLY", "YEARLY", "ONE_TIME"];
const emptyForm = () => ({ name: "", amount: "", category: "BILLS", frequency: "MONTHLY", nextDueDate: todayISO(), description: "", accountId: "", reminderEnabled: true, active: true });
const VIEWS = [
  { key: "all", label: "All" },
  { key: "overdue", label: "Overdue" },
  { key: "soon", label: "Due soon" },
  { key: "upcoming", label: "Upcoming" },
  { key: "paid", label: "Paid & paused" },
];

function groupOf(bill) {
  if (!bill.active) return "paid";
  const days = daysFromToday(bill.nextDueDate);
  if (days < 0) return "overdue";
  if (days <= 7) return "soon";
  return "upcoming";
}

const toRequest = (bill, changes = {}) => ({
  name: bill.name,
  amount: String(bill.amount),
  category: bill.category,
  frequency: bill.frequency,
  nextDueDate: bill.nextDueDate,
  description: bill.description,
  accountId: bill.accountId,
  reminderEnabled: bill.reminderEnabled,
  active: bill.active,
  ...changes,
});

export default function Bills() {
  const notify = useToast();
  const location = useLocation();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const billsResource = useBills();
  const accountsResource = useAccounts();
  const bills = billsResource.data;
  const accounts = accountsResource.data;
  const { loading, error: loadError, reload: load } = combineResources(billsResource, accountsResource);
  const [form, setForm] = useState(emptyForm);
  const [editing, setEditing] = useState(null);
  const [formOpen, setFormOpen] = useState(false);
  const [busyId, setBusyId] = useState(null);
  const [error, setError] = useState("");
  const view = VIEWS.some((item) => item.key === searchParams.get("view")) ? searchParams.get("view") : "all";

  useEffect(() => {
    const prefill = location.state?.receiptDraft;
    if (!prefill) return;
    setForm({ ...emptyForm(), ...prefill, amount: prefill.amount ? String(prefill.amount) : "" });
    setEditing(null);
    setFormOpen(true);
    navigate(location.pathname, { replace: true, state: null });
  }, [location, navigate]);

  const grouped = useMemo(() => {
    const groups = { overdue: [], soon: [], upcoming: [], paid: [] };
    [...bills].sort((a, b) => a.nextDueDate.localeCompare(b.nextDueDate)).forEach((bill) => groups[groupOf(bill)].push(bill));
    return groups;
  }, [bills]);
  const dueIn30 = sumAmounts(bills.filter((bill) => bill.active && daysFromToday(bill.nextDueDate) <= 30).map((bill) => bill.amount));

  function setView(next) {
    const params = new URLSearchParams(searchParams);
    if (next === "all") params.delete("view"); else params.set("view", next);
    setSearchParams(params, { replace: true });
  }
  function edit(item) {
    setEditing(item.id);
    setForm({ name: item.name, amount: String(item.amount), category: item.category, frequency: item.frequency, nextDueDate: item.nextDueDate, description: item.description || "", accountId: item.accountId || "", reminderEnabled: item.reminderEnabled, active: item.active });
    setFormOpen(true);
  }
  function closeForm() {
    setFormOpen(false);
    setEditing(null);
    setForm(emptyForm());
  }
  async function submit(event) {
    event.preventDefault();
    setBusyId("form");
    setError("");
    try {
      const payload = { ...form, amount: toAmountString(form.amount), accountId: form.accountId ? Number(form.accountId) : null };
      if (editing) await updateBill(editing, payload);
      else await createBill(payload);
      await load();
      notify(editing ? "Bill updated." : "Bill added.");
      closeForm();
    } catch (e) {
      setError(e?.isValidationError ? e.message : getApiErrorMessage(e));
    } finally {
      setBusyId(null);
    }
  }
  async function markPaid(bill) {
    setBusyId(bill.id);
    setError("");
    try {
      if (bill.frequency === "ONE_TIME") {
        await updateBill(bill.id, toRequest(bill, { active: false }));
        notify(`${bill.name} marked paid.`);
      } else {
        const next = addFrequency(bill.nextDueDate, bill.frequency);
        await updateBill(bill.id, toRequest(bill, { nextDueDate: next }));
        notify(`${bill.name} marked paid. Next due ${longDate(next)}.`);
      }
      await load();
    } catch (e) {
      setError(getApiErrorMessage(e));
    } finally {
      setBusyId(null);
    }
  }
  async function reactivate(bill) {
    setBusyId(bill.id);
    try {
      await updateBill(bill.id, toRequest(bill, { active: true, nextDueDate: bill.nextDueDate < todayISO() ? todayISO() : bill.nextDueDate }));
      await load();
      notify(`${bill.name} is active again.`);
    } catch (e) {
      setError(getApiErrorMessage(e));
    } finally {
      setBusyId(null);
    }
  }
  async function remove(bill) {
    if (!window.confirm(`Delete ${bill.name}?`)) return;
    try {
      await deleteBill(bill.id);
      await load();
      notify("Bill deleted.");
    } catch (e) {
      setError(getApiErrorMessage(e));
    }
  }

  const header = <PageHeader eyebrow="Plan" title="Bills & income" description="What’s due and when. Marking a bill paid moves it to its next due date; it doesn’t move money." action={!formOpen && <button type="button" className="button-primary" onClick={() => setFormOpen(true)}><Plus size={16} aria-hidden="true" /> Add a bill</button>} />;
  if (loading && !bills.length) return <div className="space-y-10">{header}<LoadingSkeleton variant="rows" count={4} /></div>;

  const sections = [
    { key: "overdue", title: "Overdue", tone: "text-danger" },
    { key: "soon", title: "Due in the next 7 days", tone: "text-warm" },
    { key: "upcoming", title: "Upcoming", tone: "text-ink" },
    { key: "paid", title: "Paid & paused", tone: "text-muted" },
  ].filter((section) => (view === "all" || view === section.key) && grouped[section.key].length);

  return (
    <div className="space-y-10">
      {header}
      <ErrorMessage message={error || loadError} onDismiss={() => setError("")} />

      {formOpen && (
        <form onSubmit={submit} className="motion-content grid gap-4 rounded-lg border border-line bg-surface p-5 sm:grid-cols-2">
          <h2 className="section-title sm:col-span-2">{editing ? "Edit bill" : "Add a bill"}</h2>
          <label className="field-label">Name<input className="field-control" maxLength={120} required placeholder="Rent, electricity…" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} /></label>
          <label className="field-label">Amount<input className="field-control tabular" inputMode="decimal" required placeholder="0.00" value={form.amount} onChange={(e) => setForm({ ...form, amount: e.target.value })} /></label>
          <label className="field-label">How often<select className="field-control" value={form.frequency} onChange={(e) => setForm({ ...form, frequency: e.target.value })}>{FREQUENCIES.map((value) => <option key={value} value={value}>{humanize(value)}</option>)}</select></label>
          <label className="field-label">Next due<input className="field-control" type="date" required value={form.nextDueDate} onChange={(e) => setForm({ ...form, nextDueDate: e.target.value })} /></label>
          <label className="field-label">Category<select className="field-control" value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })}>{PLANNING_CATEGORIES.map((category) => <option key={category} value={category}>{categoryLabel(category)}</option>)}</select></label>
          <label className="field-label"><span>Paid from <span className="text-xs font-normal text-muted">optional</span></span><select className="field-control" value={form.accountId} onChange={(e) => setForm({ ...form, accountId: e.target.value })}><option value="">Not linked</option>{accounts.map((account) => <option key={account.id} value={account.id}>{accountName(account)} {maskAccountNumber(account.accountNumber)}</option>)}</select></label>
          <label className="field-label sm:col-span-2"><span>Note <span className="text-xs font-normal text-muted">optional</span></span><input className="field-control" maxLength={255} value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} /></label>
          <label className="flex items-center gap-2 text-sm text-ink-soft sm:col-span-2"><input type="checkbox" className="h-4 w-4 accent-[rgb(var(--accent))]" checked={form.reminderEnabled} onChange={(e) => setForm({ ...form, reminderEnabled: e.target.checked })} /> Remind me before it’s due</label>
          <div className="flex gap-2 sm:col-span-2"><button className="button-primary" disabled={busyId === "form"}>{busyId === "form" ? "Saving…" : editing ? "Save changes" : "Add bill"}</button><button type="button" className="button-ghost" onClick={closeForm}>Cancel</button></div>
        </form>
      )}

      {bills.length > 0 && (
        <div className="space-y-4">
          <p className="text-[15px] leading-6 text-ink-soft"><span className="tabular font-semibold text-ink">{formatMoney(dueIn30)}</span> due in the next 30 days{grouped.overdue.length ? <>, and <span className="font-semibold text-danger">{grouped.overdue.length} overdue</span></> : ""}.</p>
          <div role="tablist" aria-label="Show bills" className="-mx-1 flex flex-wrap gap-1">
            {VIEWS.map((item) => {
              const count = item.key === "all" ? bills.length : grouped[item.key].length;
              return <button key={item.key} role="tab" aria-selected={view === item.key} onClick={() => setView(item.key)} className={`rounded-full px-3 py-1.5 text-[13px] font-medium transition-colors duration-150 ${view === item.key ? "bg-ink text-paper" : "text-ink-soft hover:bg-surface-2"}`}>{item.label} <span className="tabular opacity-70">{count}</span></button>;
            })}
          </div>
        </div>
      )}

      {!bills.length && !formOpen ? (
        <EmptyState icon={CalendarClock} title="No bills yet" description="Add rent, utilities or subscriptions. FinFlow holds them back from Safe-to-Spend and reminds you before they’re due." action={<button type="button" className="button-primary" onClick={() => setFormOpen(true)}>Add your first bill</button>} />
      ) : sections.length ? (
        <div className="space-y-8">
          {sections.map((section) => (
            <section key={section.key} aria-labelledby={`bills-${section.key}`}>
              <div className="flex items-baseline justify-between gap-3 border-b border-line pb-2">
                <h2 id={`bills-${section.key}`} className={`text-sm font-semibold ${section.tone}`}>{section.title}</h2>
                {section.key !== "paid" && <span className="tabular text-xs text-muted">{formatMoney(sumAmounts(grouped[section.key].map((bill) => bill.amount)))}</span>}
              </div>
              <ul className="divide-y divide-line">
                {grouped[section.key].map((bill) => {
                  const days = daysFromToday(bill.nextDueDate);
                  const closedLabel = bill.frequency === "ONE_TIME" ? "Paid" : "Paused";
                  return (
                    <li key={bill.id} className="flex flex-wrap items-center gap-x-4 gap-y-2 py-3.5">
                      <div className="min-w-0 flex-1">
                        <p className="truncate text-sm font-medium text-ink">{bill.name}</p>
                        <p className="mt-0.5 text-xs text-muted">{humanize(bill.frequency)} · {categoryLabel(bill.category)}{bill.accountNumber ? ` · ${maskAccountNumber(bill.accountNumber)}` : ""}</p>
                      </div>
                      <div className="text-right">
                        <p className="tabular text-sm font-semibold text-ink">{formatMoney(bill.amount)}</p>
                        <p className={`text-xs ${bill.active ? (days < 0 ? "font-medium text-danger" : days <= 7 ? "font-medium text-warm" : "text-muted") : "text-muted"}`}>{bill.active ? `${dueLabel(days)} · ${longDate(bill.nextDueDate)}` : closedLabel}</p>
                      </div>
                      <div className="flex w-full items-center justify-end gap-1 sm:w-auto">
                        {bill.active ? (
                          <button type="button" className="button-secondary min-h-9 px-3 text-[13px]" disabled={busyId === bill.id} onClick={() => markPaid(bill)}><Check size={14} aria-hidden="true" /> {busyId === bill.id ? "Saving…" : "Mark paid"}</button>
                        ) : (
                          <button type="button" className="button-ghost min-h-9 px-3 text-[13px]" disabled={busyId === bill.id} onClick={() => reactivate(bill)}>Make active</button>
                        )}
                        <button type="button" className="icon-action" aria-label={`Edit ${bill.name}`} onClick={() => edit(bill)}><Pencil size={15} /></button>
                        <button type="button" className="icon-action hover:text-danger" aria-label={`Delete ${bill.name}`} onClick={() => remove(bill)}><Trash2 size={15} /></button>
                      </div>
                    </li>
                  );
                })}
              </ul>
            </section>
          ))}
        </div>
      ) : bills.length > 0 && (
        <EmptyState icon={CalendarClock} title={`Nothing ${VIEWS.find((item) => item.key === view).label.toLowerCase()}`} description="Good news, or try another view." action={<button type="button" className="button-secondary" onClick={() => setView("all")}>Show all bills</button>} />
      )}

      <IncomeSchedulePanel />
    </div>
  );
}
