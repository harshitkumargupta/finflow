import { useCallback, useEffect, useState } from "react";
import { Edit3, History, MoreHorizontal, Pause, Play, Plus, Repeat2, XCircle, Zap } from "lucide-react";
import {
  createAutopayAuthorization, editAutopayAuthorization,
  pauseAutopay, resumeAutopay, cancelAutopay, executeAutopay, getAutopayHistory,
  simulateIncomingMandate, approveMandate, rejectMandate
} from "../api/autopayApi";
import { getCards } from "../api/cardApi";
import { getApiErrorMessage } from "../api/axios";
import { accountName } from "../components/AccountSelect";
import EmptyState from "../components/EmptyState";
import ErrorMessage from "../components/ErrorMessage";
import LoadingSkeleton from "../components/LoadingSkeleton";
import Modal from "../components/Modal";
import PageHeader from "../components/PageHeader";
import PinInput from "../components/PinInput";
import StatusBadge from "../components/StatusBadge";
import Drawer from "../components/plan/Drawer";
import LifecycleStepper, { lifecycleStage } from "../components/plan/LifecycleStepper";
import { useToast } from "../context/ToastContext";
import { useAccounts, useAutopays } from "../hooks/useFinanceData";
import { parseISODate, shortDate, longDate, startOfToday, toISODate, todayISO } from "../utils/dates";
import { formatDateTime, maskAccountNumber, toAmountString } from "../utils/format";
import { formatMoney, sumAmounts } from "../utils/money";
import { formatStatus, humanize } from "../utils/status";

const FREQUENCIES = ["DAILY", "WEEKLY", "MONTHLY", "QUARTERLY", "YEARLY"];

export default function Autopay() {
  const notify = useToast();
  const autopaysResource = useAutopays({ immediate: false });
  const accountsResource = useAccounts({ immediate: false });
  const authorizations = autopaysResource.data;
  const accounts = accountsResource.data;
  const [history, setHistory] = useState([]);
  const [cards, setCards] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [activeTab, setActiveTab] = useState("ACTIVE");
  const [openHistory, setOpenHistory] = useState(null);
  const [showIncoming, setShowIncoming] = useState(false);
  
  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [busy, setBusy] = useState(false);
  const [cancelModalId, setCancelModalId] = useState(null);
  const [approvalModal, setApprovalModal] = useState(null);
  const [approvalPin, setApprovalPin] = useState("");

  const [form, setForm] = useState({
    accountId: "",
    cardId: "",
    merchantName: "",
    amount: "",
    maximumAmount: "",
    frequency: "MONTHLY",
    nextPaymentDate: "",
    description: "",
  });
  const [incomingForm, setIncomingForm] = useState({
    merchantName: "Netflix",
    amount: "649",
    maximumAmount: "649",
    frequency: "MONTHLY",
    nextPaymentDate: todayISO(),
    description: "Sandbox incoming mandate request",
    provider: "FINFLOW_SANDBOX",
  });

  const load = useCallback(async () => {
    try {
      const [, , cardRes, histRes] = await Promise.all([
        autopaysResource.reload(),
        accountsResource.reload(),
        getCards(),
        getAutopayHistory()
      ]);
      setCards(cardRes.data);
      setHistory(histRes.data);
    } catch (e) {
      setError(getApiErrorMessage(e));
    }
  }, []);

  useEffect(() => {
    load().finally(() => setLoading(false));
  }, [load]);

  const openNewForm = () => {
    setEditingId(null);
    setForm({
      accountId: accounts.length > 0 ? String(accounts[0].id) : "",
      cardId: cards.length > 0 ? String(cards[0].id) : "",
      merchantName: "",
      amount: "",
      maximumAmount: "",
      frequency: "MONTHLY",
      nextPaymentDate: todayISO(),
      description: "",
    });
    setShowForm(true);
  };

  const openEditForm = (item) => {
    setEditingId(item.id);
    setForm({
      accountId: item.accountId ? String(item.accountId) : (accounts[0] ? String(accounts[0].id) : ""),
      cardId: item.cardId ? String(item.cardId) : "",
      merchantName: item.merchantName,
      amount: String(item.amount),
      maximumAmount: String(item.maximumAmount),
      frequency: item.frequency,
      nextPaymentDate: item.nextPaymentDate,
      description: item.description || "",
    });
    setShowForm(true);
  };

  const submitForm = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError("");
    try {
      const card = cards.find(c => String(c.id) === form.cardId);
      const providerName = card ? `CARD_${card.id}_${card.cardNetwork}` : "FINFLOW_INTERNAL";

      if (editingId) {
        await editAutopayAuthorization(editingId, {
          merchantName: form.merchantName,
          amount: toAmountString(form.amount),
          maximumAmount: toAmountString(form.maximumAmount, "Maximum amount"),
          frequency: form.frequency,
          nextPaymentDate: form.nextPaymentDate,
          accountId: form.accountId ? parseInt(form.accountId) : null,
          cardId: form.cardId ? parseInt(form.cardId) : null,
          description: form.description
        });
        notify("Autopay authorization updated successfully.");
      } else {
        await createAutopayAuthorization({
          accountId: parseInt(form.accountId),
          cardId: form.cardId ? parseInt(form.cardId) : null,
          merchantName: form.merchantName,
          provider: providerName,
          amount: toAmountString(form.amount),
          maximumAmount: toAmountString(form.maximumAmount, "Maximum amount"),
          frequency: form.frequency,
          nextPaymentDate: form.nextPaymentDate,
          description: form.description
        });
        notify("New Autopay authorization created.");
      }

      setShowForm(false);
      await load();
    } catch (err) {
      setError(err?.isValidationError ? err.message : getApiErrorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  const handleAction = async (action, id) => {
    setBusy(true);
    setError("");
    try {
      if (action === 'pause') await pauseAutopay(id);
      if (action === 'resume') await resumeAutopay(id);
      if (action === 'cancel') {
        await cancelAutopay(id);
        setCancelModalId(null);
      }
      await load();
      notify(`Autopay authorization ${action}d.`);
    } catch (e) {
      setError(getApiErrorMessage(e));
    } finally {
      setBusy(false);
    }
  };

  const handleMandateDecision = async (action, authorization, pin = approvalPin) => {
    setBusy(true);
    setError("");
    try {
      if (action === "approve") {
        await approveMandate(authorization.id, authorization.accountId || (accounts[0] && accounts[0].id), pin);
      } else {
        await rejectMandate(authorization.id);
      }
      await load();
      setApprovalModal(null);
      setApprovalPin("");
      notify(`Mandate ${action === "approve" ? "approved" : "rejected"}.`);
    } catch (e) {
      setError(getApiErrorMessage(e));
    } finally {
      setBusy(false);
    }
  };

  const createIncomingMandate = async (event) => {
    event.preventDefault();
    setBusy(true);
    setError("");
    try {
      await simulateIncomingMandate({
        ...incomingForm,
        amount: toAmountString(incomingForm.amount),
        maximumAmount: toAmountString(incomingForm.maximumAmount, "Maximum amount"),
      });
      await load();
      setShowIncoming(false);
      notify("Incoming sandbox mandate request created.");
    } catch (e) {
      setError(e?.isValidationError ? e.message : getApiErrorMessage(e));
    } finally {
      setBusy(false);
    }
  };

  const handleSimulatePayment = async (id, merchant) => {
    setBusy(true);
    setError("");
    try {
      const res = await executeAutopay(id);
      await load();
      notify(`Simulated payment of ₹${res.data.amount} to ${merchant} processed! Ref: ${res.data.transactionReference}`);
    } catch (e) {
      setError(getApiErrorMessage(e));
    } finally {
      setBusy(false);
    }
  };

  const stageOf = (status) => lifecycleStage(status);
  const pending = authorizations.filter((item) => item.status === "PENDING_APPROVAL");
  const views = [
    { key: "ACTIVE", label: "Active", match: (item) => stageOf(item.status) === "ACTIVE" },
    { key: "PAUSED", label: "Paused", match: (item) => stageOf(item.status) === "PAUSED" },
    { key: "CLOSED", label: "Closed", match: (item) => stageOf(item.status) === "CLOSED" },
    { key: "ALL", label: "All", match: (item) => item.status !== "PENDING_APPROVAL" },
  ];
  const view = views.find((item) => item.key === activeTab) || views[3];
  const visible = authorizations.filter(view.match);
  const active = authorizations.filter((item) => item.status === "ACTIVE");
  const reserved = sumAmounts(active.map((item) => item.maximumAmount));
  const weeks = (() => {
    const today = startOfToday();
    const monday = new Date(today); monday.setDate(today.getDate() - ((today.getDay() + 6) % 7));
    const groups = new Map();
    active.filter((item) => item.nextPaymentDate).forEach((item) => {
      const date = parseISODate(item.nextPaymentDate);
      const offset = Math.floor((date - monday) / (7 * 86400000));
      if (offset < 0 || offset > 7) return;
      if (!groups.has(offset)) groups.set(offset, []);
      groups.get(offset).push(item);
    });
    return [...groups.entries()].sort((a, b) => a[0] - b[0]).map(([offset, items]) => {
      const start = new Date(monday); start.setDate(monday.getDate() + offset * 7);
      const title = offset === 0 ? "This week" : offset === 1 ? "Next week" : `Week of ${shortDate(toISODate(start))}`;
      return { offset, title, items: items.sort((a, b) => a.nextPaymentDate.localeCompare(b.nextPaymentDate)), total: sumAmounts(items.map((item) => item.amount)) };
    });
  })();
  const fromAccount = (item) => !item.accountName || item.accountName.toUpperCase() === "FINFLOW" ? "FinFlow account" : `${humanize(item.accountName)} account`;
  const sourceLabel = (item) => item.cardLastFourDigits ? `${item.cardNickname || item.cardNetwork || "Card"} ••${item.cardLastFourDigits}` : `from ${fromAccount(item)}`;
  const historyFor = (id) => history.filter((run) => run.autopayId === id);

  function primaryAction(item) {
    if (item.status === "PENDING_APPROVAL") return <button type="button" className="button-primary min-h-9 px-3 text-[13px]" onClick={() => { setApprovalModal(item); setApprovalPin(""); }}>Review</button>;
    if (item.status === "ACTIVE") return <button type="button" className="button-primary min-h-9 px-3 text-[13px]" disabled={busy} onClick={() => handleSimulatePayment(item.id, item.merchantName)}><Zap size={14} aria-hidden="true" /> Run now</button>;
    if (item.status === "PAUSED") return <button type="button" className="button-primary min-h-9 px-3 text-[13px]" disabled={busy} onClick={() => handleAction("resume", item.id)}><Play size={14} aria-hidden="true" /> Resume</button>;
    return null;
  }

  const header = (
    <PageHeader
      eyebrow="Plan"
      title="Autopay"
      description="Recurring payments you’ve allowed to run on their own. Each one can take at most its limit, and you can pause or cancel any time."
      action={<><button className="button-primary" onClick={openNewForm}><Plus size={16} aria-hidden="true" /> New autopay</button><button className="button-secondary" onClick={() => setShowIncoming(!showIncoming)} aria-expanded={showIncoming}>Simulate a request</button></>}
    />
  );

  if (loading) return <div className="space-y-10">{header}<LoadingSkeleton variant="rows" count={4} /></div>;

  return (
    <div className="space-y-12">
      {header}
      <ErrorMessage message={error} onDismiss={() => setError("")} />

      <p className="text-[15px] leading-6 text-ink-soft">
        <span className="tabular font-semibold text-ink">{active.length}</span> active {active.length === 1 ? "mandate" : "mandates"}, able to take up to <span className="tabular font-semibold text-ink">{formatMoney(reserved)}</span> between them.
        {pending.length > 0 && <> <span className="font-semibold text-warm">{pending.length} waiting for your approval.</span></>}
      </p>

      {pending.length > 0 && (
        <section aria-labelledby="pending-heading" className="space-y-3">
          <h2 id="pending-heading" className="font-display text-2xl font-medium text-ink">Waiting for you</h2>
          {pending.map((item) => (
            <article key={item.id} className="rounded-lg border border-warm/30 bg-warm-soft/60 p-5">
              <div className="flex flex-wrap items-start justify-between gap-4">
                <div className="min-w-0">
                  <p className="text-sm font-semibold text-ink">{item.merchantName} wants to charge you {humanize(item.frequency).toLowerCase()}</p>
                  <p className="mt-1 text-sm leading-6 text-ink-soft"><span className="tabular font-semibold text-ink">{formatMoney(item.amount)}</span> each time, never more than <span className="tabular font-semibold text-ink">{formatMoney(item.maximumAmount)}</span>. First payment {longDate(item.nextPaymentDate)}.</p>
                </div>
                <div className="flex shrink-0 gap-2">
                  <button type="button" className="button-secondary" disabled={busy} onClick={() => handleMandateDecision("reject", item)}>Reject</button>
                  <button type="button" className="button-primary" onClick={() => { setApprovalModal(item); setApprovalPin(""); }}>Approve…</button>
                </div>
              </div>
            </article>
          ))}
        </section>
      )}

      {showIncoming && (
        <form onSubmit={createIncomingMandate} className="motion-content space-y-4 rounded-lg border border-line bg-surface p-5">
          <div>
            <h2 className="section-title">Simulate a merchant request</h2>
            <p className="mt-1 text-sm text-ink-soft">Pretend a merchant asked to set up autopay. It arrives under “Waiting for you”, where you approve or reject it.</p>
          </div>
          <div className="grid gap-4 sm:grid-cols-2">
            <label className="field-label">Merchant<input className="field-control" required maxLength="120" value={incomingForm.merchantName} onChange={(e) => setIncomingForm({ ...incomingForm, merchantName: e.target.value })} /></label>
            <label className="field-label">How often<select className="field-control" value={incomingForm.frequency} onChange={(e) => setIncomingForm({ ...incomingForm, frequency: e.target.value })}>{FREQUENCIES.map((value) => <option key={value} value={value}>{humanize(value)}</option>)}</select></label>
            <label className="field-label">Amount each time<input className="field-control tabular" required inputMode="decimal" value={incomingForm.amount} onChange={(e) => setIncomingForm({ ...incomingForm, amount: e.target.value })} /></label>
            <label className="field-label">Never more than<input className="field-control tabular" required inputMode="decimal" value={incomingForm.maximumAmount} onChange={(e) => setIncomingForm({ ...incomingForm, maximumAmount: e.target.value })} /></label>
            <label className="field-label">First payment<input className="field-control" type="date" required min={todayISO()} value={incomingForm.nextPaymentDate} onChange={(e) => setIncomingForm({ ...incomingForm, nextPaymentDate: e.target.value })} /></label>
          </div>
          <div className="flex gap-2"><button className="button-primary" disabled={busy}>Send request</button><button type="button" className="button-ghost" onClick={() => setShowIncoming(false)}>Cancel</button></div>
        </form>
      )}

      {showForm && (
        <form onSubmit={submitForm} className="motion-content space-y-4 rounded-lg border border-line bg-surface p-5">
          <h2 className="section-title">{editingId ? "Edit autopay" : "New autopay"}</h2>
          <div className="grid gap-4 sm:grid-cols-2">
            <label className="field-label sm:col-span-2">Who gets paid<input required maxLength="120" placeholder="Electricity, gym, streaming…" className="field-control" value={form.merchantName} onChange={(e) => setForm({ ...form, merchantName: e.target.value })} /></label>
            <label className="field-label">Pay from<select className="field-control" required value={form.accountId} onChange={(e) => setForm({ ...form, accountId: e.target.value })}>{accounts.map((account) => <option key={account.id} value={account.id}>{accountName(account)} {maskAccountNumber(account.accountNumber)}</option>)}</select></label>
            <label className="field-label">Card <span className="text-xs font-normal text-muted">optional</span><select className="field-control" value={form.cardId} onChange={(e) => setForm({ ...form, cardId: e.target.value })}><option value="">No card</option>{cards.map((card) => <option key={card.id} value={card.id}>{card.nickname || card.cardNetwork} ••{card.lastFourDigits}</option>)}</select></label>
            <label className="field-label">Amount each time<input className="field-control tabular" required inputMode="decimal" value={form.amount} onChange={(e) => setForm({ ...form, amount: e.target.value })} /></label>
            <label className="field-label">Never more than<input className="field-control tabular" required inputMode="decimal" value={form.maximumAmount} onChange={(e) => setForm({ ...form, maximumAmount: e.target.value })} /></label>
            <label className="field-label">How often<select className="field-control" value={form.frequency} onChange={(e) => setForm({ ...form, frequency: e.target.value })}>{FREQUENCIES.map((value) => <option key={value} value={value}>{humanize(value)}</option>)}</select></label>
            <label className="field-label">Next payment<input className="field-control" type="date" required min={todayISO()} value={form.nextPaymentDate} onChange={(e) => setForm({ ...form, nextPaymentDate: e.target.value })} /></label>
            <label className="field-label sm:col-span-2">Note <span className="text-xs font-normal text-muted">optional</span><input className="field-control" maxLength="255" value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} /></label>
          </div>
          <p className="text-[13px] text-muted">The limit is held back from Safe-to-Spend while the mandate is active.</p>
          <div className="flex gap-2"><button className="button-primary" disabled={busy}>{busy ? "Saving…" : editingId ? "Save changes" : "Create autopay"}</button><button type="button" className="button-ghost" onClick={() => setShowForm(false)}>Cancel</button></div>
        </form>
      )}

      <div className="grid grid-cols-1 gap-12 lg:grid-cols-[minmax(0,1.6fr)_minmax(0,1fr)] lg:gap-10">
        <section aria-labelledby="mandates-heading">
          <div className="flex flex-wrap items-end justify-between gap-3">
            <h2 id="mandates-heading" className="font-display text-2xl font-medium text-ink">Your mandates</h2>
            <div role="tablist" aria-label="Filter mandates" className="flex rounded-md border border-line bg-surface p-0.5">
              {views.map((item) => (
                <button key={item.key} role="tab" aria-selected={view.key === item.key} onClick={() => setActiveTab(item.key)} className={`rounded-[7px] px-3 py-1.5 text-[13px] font-medium transition-colors duration-150 ${view.key === item.key ? "bg-ink text-paper" : "text-ink-soft hover:text-ink"}`}>{item.label}</button>
              ))}
            </div>
          </div>
          {visible.length ? (
            <ul className="mt-4 space-y-3">
              {visible.map((item) => {
                const runs = historyFor(item.id);
                const closed = stageOf(item.status) === "CLOSED";
                return (
                  <li key={item.id} className={`overflow-hidden rounded-lg border border-line bg-surface ${closed ? "opacity-75" : ""}`}>
                    <div className="flex">
                      <div className="flex w-16 shrink-0 flex-col items-center justify-center border-r border-dashed border-line-strong bg-surface-2/60 py-4 text-center sm:w-20" aria-hidden="true">
                        {item.nextPaymentDate && !closed ? <><span className="text-[11px] font-semibold uppercase tracking-wider text-muted">{parseISODate(item.nextPaymentDate).toLocaleString("en-IN", { month: "short" })}</span><span className="display-number text-2xl leading-none">{parseISODate(item.nextPaymentDate).getDate()}</span></> : <Repeat2 size={18} className="text-muted" />}
                      </div>
                      <div className="min-w-0 flex-1 p-4">
                        <div className="flex flex-wrap items-start justify-between gap-x-4 gap-y-2">
                          <div className="min-w-0">
                            <p className="truncate text-sm font-semibold text-ink">{item.merchantName}</p>
                            <p className="mt-0.5 text-xs text-muted">{humanize(item.frequency)} · {sourceLabel(item)}</p>
                          </div>
                          <div className="text-right">
                            <p className="tabular text-sm font-semibold text-ink">{formatMoney(item.amount)}</p>
                            <p className="tabular text-xs text-muted">max {formatMoney(item.maximumAmount)}</p>
                          </div>
                        </div>
                        <div className="mt-3 flex flex-wrap items-center justify-between gap-3">
                          <LifecycleStepper status={item.status} />
                          <div className="flex items-center gap-1">
                            {primaryAction(item)}
                            <details className="relative">
                              <summary className="icon-action cursor-pointer list-none [&::-webkit-details-marker]:hidden" aria-label={`More actions for ${item.merchantName}`}><MoreHorizontal size={18} /></summary>
                              <div className="absolute right-0 z-20 mt-1 w-44 rounded-md border border-line bg-surface p-1 shadow-raised">
                                <button type="button" className="nav-link w-full" onClick={(e) => { e.currentTarget.closest("details").open = false; setOpenHistory(openHistory === item.id ? null : item.id); }}><History size={15} aria-hidden="true" /> History ({runs.length})</button>
                                {!closed && item.status !== "PENDING_APPROVAL" && <button type="button" className="nav-link w-full" onClick={(e) => { e.currentTarget.closest("details").open = false; openEditForm(item); }}><Edit3 size={15} aria-hidden="true" /> Edit</button>}
                                {item.status === "ACTIVE" && <button type="button" className="nav-link w-full" disabled={busy} onClick={(e) => { e.currentTarget.closest("details").open = false; handleAction("pause", item.id); }}><Pause size={15} aria-hidden="true" /> Pause</button>}
                                {!closed && <button type="button" className="nav-link w-full text-danger hover:text-danger" onClick={(e) => { e.currentTarget.closest("details").open = false; setCancelModalId(item.id); }}><XCircle size={15} aria-hidden="true" /> Cancel mandate</button>}
                              </div>
                            </details>
                          </div>
                        </div>
                        {item.lastPaymentStatus && <p className="mt-2 text-xs text-muted">Last run {item.lastPaymentDate ? shortDate(item.lastPaymentDate) : ""}: {formatStatus(item.lastPaymentStatus).toLowerCase()}</p>}
                      </div>
                    </div>
                    <Drawer open={openHistory === item.id}>
                      <div className="border-t border-line bg-surface-2/40 px-4 py-3">
                        <p className="text-xs font-semibold uppercase tracking-wider text-muted">Execution history</p>
                        {runs.length ? (
                          <ul className="mt-2 divide-y divide-line">
                            {runs.map((run) => (
                              <li key={run.id} className="flex flex-wrap items-center justify-between gap-2 py-2 text-[13px]">
                                <span className="text-ink-soft">{formatDateTime(run.executedAt)}{run.failureReason ? ` · ${run.failureReason}` : ""}</span>
                                <span className="flex items-center gap-2"><span className="tabular font-semibold text-ink">{formatMoney(run.amount)}</span><StatusBadge status={run.status} /></span>
                              </li>
                            ))}
                          </ul>
                        ) : <p className="mt-2 text-[13px] text-ink-soft">No runs yet.{item.status === "ACTIVE" ? " Use “Run now” to try one in the sandbox." : ""}</p>}
                      </div>
                    </Drawer>
                  </li>
                );
              })}
            </ul>
          ) : (
            <div className="mt-4"><EmptyState icon={Repeat2} title={view.key === "ACTIVE" ? "No active autopay" : `Nothing ${view.label.toLowerCase()}`} description={view.key === "ACTIVE" ? "Set one up for a bill you pay every month, or simulate a merchant asking." : "Mandates appear here when they reach this stage."} action={view.key === "ACTIVE" ? <button type="button" className="button-secondary" onClick={openNewForm}>New autopay</button> : null} /></div>
          )}
        </section>

        <aside aria-labelledby="upcoming-runs-heading">
          <h2 id="upcoming-runs-heading" className="font-display text-2xl font-medium text-ink">Coming up</h2>
          {weeks.length ? (
            <div className="mt-4 space-y-6">
              {weeks.map((week) => (
                <section key={week.offset}>
                  <div className="flex items-baseline justify-between border-b border-line pb-1.5">
                    <h3 className="text-sm font-semibold text-ink">{week.title}</h3>
                    <span className="tabular text-xs text-muted">{formatMoney(week.total)}</span>
                  </div>
                  <ul className="mt-2 space-y-2">
                    {week.items.map((item) => (
                      <li key={item.id} className="flex items-center justify-between gap-3 text-sm">
                        <span className="min-w-0"><span className="block truncate text-ink">{item.merchantName}</span><span className="text-xs text-muted">{longDate(item.nextPaymentDate)}</span></span>
                        <span className="tabular shrink-0 font-medium text-ink">{formatMoney(item.amount)}</span>
                      </li>
                    ))}
                  </ul>
                </section>
              ))}
            </div>
          ) : <p className="mt-3 text-sm leading-6 text-ink-soft">No autopay runs in the next eight weeks.</p>}
        </aside>
      </div>

      <Modal
        open={Boolean(approvalModal)}
        title={approvalModal ? `Approve ${approvalModal.merchantName}?` : "Approve"}
        description={approvalModal ? `${approvalModal.merchantName} will be able to take ${formatMoney(approvalModal.amount)} ${humanize(approvalModal.frequency).toLowerCase()}, never more than ${formatMoney(approvalModal.maximumAmount)}. You can pause or cancel it any time.` : ""}
        busy={busy}
        onClose={() => setApprovalModal(null)}
        footer={<><button className="button-secondary" onClick={() => setApprovalModal(null)}>Not now</button><button className="button-primary" disabled={busy || approvalPin.length !== 6} onClick={() => handleMandateDecision("approve", approvalModal)}>{busy ? "Approving…" : "Approve"}</button></>}
      >
        <PinInput value={approvalPin} onChange={setApprovalPin} disabled={busy} autoFocus />
      </Modal>
      <Modal
        open={Boolean(cancelModalId)}
        tone="danger"
        title="Cancel this autopay?"
        description="It won’t run again, and the amount it was allowed to take goes back into Safe-to-Spend. You can’t undo this; you’d need to set it up again."
        busy={busy}
        onClose={() => setCancelModalId(null)}
        footer={<><button className="button-secondary" onClick={() => setCancelModalId(null)}>Keep it</button><button className="button-primary border-danger bg-danger hover:bg-danger" onClick={() => handleAction("cancel", cancelModalId)} disabled={busy}>{busy ? "Cancelling…" : "Cancel autopay"}</button></>}
      />
    </div>
  );
}
