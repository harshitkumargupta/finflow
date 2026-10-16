import { useEffect, useState } from "react";
import { CreditCard, Plus, Trash2, Shield, Power, CheckCircle, AlertCircle, RefreshCw } from "lucide-react";
import { getApiErrorMessage } from "../api/axios";
import { addCard, deleteCard, getCards, toggleCardStatus } from "../api/cardApi";
import ErrorMessage from "../components/ErrorMessage";
import EmptyState from "../components/EmptyState";
import LoadingSkeleton from "../components/LoadingSkeleton";
import PageHeader from "../components/PageHeader";
import StatusBadge from "../components/StatusBadge";
import { humanize } from "../utils/status";
import { useToast } from "../context/ToastContext";

const currentYear = new Date().getFullYear();
const years = Array.from({ length: 15 }, (_, i) => String(currentYear + i));
const months = Array.from({ length: 12 }, (_, i) => String(i + 1).padStart(2, '0'));

const emptyForm = () => ({
  nickname: "",
  cardholderName: "",
  cardNetwork: "VISA",
  lastFourDigits: "",
  expiryMonth: months[0],
  expiryYear: String(currentYear),
});

export default function Cards() {
  const notify = useToast();
  const [cards, setCards] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const [form, setForm] = useState(emptyForm());
  const [showAdd, setShowAdd] = useState(false);

  async function load() {
    const { data } = await getCards();
    setCards(data);
  }

  useEffect(() => {
    load().catch((e) => setError(getApiErrorMessage(e))).finally(() => setLoading(false));
  }, []);

  async function submit(event) {
    event.preventDefault();
    setError("");
    setBusy(true);
    try {
      await addCard(form);
      await load();
      notify("Payment source added successfully.");
      setForm(emptyForm());
      setShowAdd(false);
    } catch (e) {
      setError(getApiErrorMessage(e));
    } finally {
      setBusy(false);
    }
  }

  async function handleToggleStatus(id, nickname, currentStatus) {
    setBusy(true);
    try {
      const { data } = await toggleCardStatus(id);
      await load();
      notify(`Payment source '${nickname || 'Card'}' ${data.status === 'ACTIVE' ? 'enabled' : 'disabled'}.`);
    } catch (e) {
      setError(getApiErrorMessage(e));
    } finally {
      setBusy(false);
    }
  }

  async function remove(id, nickname) {
    if (!window.confirm(`Are you sure you want to remove '${nickname || 'this card'}'? Any connected Autopays will no longer be able to use this card.`)) return;
    setBusy(true);
    try {
      await deleteCard(id);
      await load();
      notify("Payment source removed.");
    } catch (e) {
      setError(getApiErrorMessage(e));
    } finally {
      setBusy(false);
    }
  }

  const header = <PageHeader eyebrow="Household" title="Cards" description="Debit and credit cards you use for the wallet and autopay. FinFlow keeps only the last four digits and expiry, never the full number or CVV." />;
  if (loading) return <div className="space-y-10">{header}<LoadingSkeleton count={3} /></div>;
  return (
    <div className="space-y-10">
      {header}
      <ErrorMessage message={error} onDismiss={() => setError("")} />

      {!showAdd && (
        <button className="button-primary" onClick={() => setShowAdd(true)}>
          <Plus size={16} /> Add Payment Source
        </button>
      )}

      {showAdd && (
        <section className="card border-brand-200">
          <div className="flex items-center justify-between">
            <h2 className="section-title">Add Payment Source</h2>
            <button className="text-sm font-medium text-slate-500" onClick={() => setShowAdd(false)}>Cancel</button>
          </div>
          <form onSubmit={submit} className="mt-5 grid gap-4 sm:grid-cols-2">
            <label className="field-label">
              Nickname / Label
              <input 
                placeholder="e.g. Salary Debit Card, Personal Visa" 
                maxLength="50" 
                className="field-control" 
                value={form.nickname} 
                onChange={(e) => setForm({ ...form, nickname: e.target.value })} 
              />
            </label>
            <label className="field-label">
              Card network
              <select className="field-control" value={form.cardNetwork} onChange={(e) => setForm({ ...form, cardNetwork: e.target.value })}>
                <option>VISA</option><option>MASTERCARD</option><option>AMEX</option><option>DISCOVER</option><option>RUPAY</option>
              </select>
            </label>
            <label className="field-label sm:col-span-2">
              Cardholder Name
              <input required maxLength="100" placeholder="Name on card" className="field-control" value={form.cardholderName} onChange={(e) => setForm({ ...form, cardholderName: e.target.value })} />
            </label>
            <label className="field-label sm:col-span-2">
              Last four digits
              <input required pattern="^[0-9]{4}$" maxLength="4" inputMode="numeric" title="Enter exactly the last four digits" placeholder="1234" className="field-control" value={form.lastFourDigits} onChange={(e) => setForm({ ...form, lastFourDigits: e.target.value.replace(/\D/g, '').slice(0, 4) })} />
            </label>
            <div className="flex gap-4">
              <label className="field-label flex-1">
                Expiry Month
                <select className="field-control" value={form.expiryMonth} onChange={(e) => setForm({ ...form, expiryMonth: e.target.value })}>
                  {months.map(m => <option key={m} value={m}>{m}</option>)}
                </select>
              </label>
              <label className="field-label flex-1">
                Expiry Year
                <select className="field-control" value={form.expiryYear} onChange={(e) => setForm({ ...form, expiryYear: e.target.value })}>
                  {years.map(y => <option key={y} value={y}>{y}</option>)}
                </select>
              </label>
            </div>
            <div className="sm:col-span-2 mt-2 flex items-center justify-between">
              <span className="flex items-center gap-1.5 text-xs text-slate-500">
                <Shield size={14} className="text-emerald-600" /> CVV and full card numbers are never stored in FinFlow.
              </span>
              <button className="button-primary w-full sm:w-auto" disabled={busy}>
                {busy ? "Saving..." : "Save Payment Source"}
              </button>
            </div>
          </form>
        </section>
      )}

      {cards.length === 0 && !showAdd ? (
        <EmptyState icon={CreditCard} title="No cards saved" description="Add a card to use it for autopay or wallet payments." action={<button type="button" className="button-primary" onClick={() => setShowAdd(true)}>Add a card</button>} />
      ) : (
        <ul className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {cards.map((card) => {
            const isActive = card.status === "ACTIVE";
            const name = card.nickname || `${card.cardNetwork} card`;
            return (
              <li key={card.id} className={`flex flex-col rounded-lg border border-line bg-surface p-5 ${isActive ? "" : "opacity-70"}`}>
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0">
                    <p className="truncate text-sm font-semibold text-ink">{name}</p>
                    <p className="mt-0.5 text-xs text-muted">{humanize(card.cardNetwork)} · {humanize(card.cardType || "DEBIT")}</p>
                  </div>
                  <StatusBadge status={card.status} label={isActive ? "Active" : "Disabled"} />
                </div>
                <p className="tabular mt-6 font-display text-2xl tracking-[0.12em] text-ink" aria-label={`Card ending ${card.lastFourDigits}`}>•••• {card.lastFourDigits}</p>
                <dl className="mt-4 grid grid-cols-3 gap-3 border-t border-line pt-3 text-xs">
                  <div className="min-w-0"><dt className="text-muted">Name</dt><dd className="truncate font-medium text-ink">{card.cardholderName}</dd></div>
                  <div><dt className="text-muted">Expires</dt><dd className="tabular font-medium text-ink">{card.expiryMonth}/{String(card.expiryYear).slice(-2)}</dd></div>
                  <div><dt className="text-muted">Autopay</dt><dd className="tabular font-medium text-ink">{card.autopayCount || 0} active</dd></div>
                </dl>
                <div className="mt-4 flex gap-1">
                  <button type="button" onClick={() => handleToggleStatus(card.id, card.nickname, card.status)} disabled={busy} className="button-ghost min-h-9 px-2.5 text-[13px] text-ink-soft"><Power size={14} aria-hidden="true" /> {isActive ? "Disable" : "Enable"}<span className="sr-only"> {name}</span></button>
                  <button type="button" onClick={() => remove(card.id, card.nickname)} disabled={busy} className="button-ghost min-h-9 px-2.5 text-[13px] text-danger hover:bg-danger-soft"><Trash2 size={14} aria-hidden="true" /> Remove<span className="sr-only"> {name}</span></button>
                </div>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}
