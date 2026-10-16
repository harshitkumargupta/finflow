import { useState } from "react";
import { Pencil, Plus, TrendingUp, Trash2 } from "lucide-react";
import { getApiErrorMessage } from "../api/axios";
import { addHolding, deleteHolding, getPortfolio, updateHolding } from "../api/investmentApi";
import useApiResource from "../hooks/useApiResource";
import EmptyState from "./EmptyState";
import ErrorMessage from "./ErrorMessage";
import LoadingSkeleton from "./LoadingSkeleton";
import MoneyText from "./MoneyText";
import { toAmountString } from "../utils/format";
import { formatMoney } from "../utils/money";
import { humanize } from "../utils/status";

const CATEGORIES = ["STOCKS", "MUTUAL_FUNDS", "INDEX_FUNDS", "BONDS", "FIXED_DEPOSITS", "GOLD", "GOVERNMENT_SECURITIES", "OTHER"];
const SWATCHES = ["bg-accent", "bg-ink-500", "bg-warm-400", "bg-ink-400", "bg-ink-600", "bg-ink-300"];
const emptyForm = () => ({ symbol: "", name: "", category: "STOCKS", quantity: "", averagePrice: "", notes: "" });
const trimQty = (value) => String(value).trim();

function Stat({ label, children }) {
  return (
    <div className="min-w-0 border-t border-line pt-3">
      <p className="text-[13px] text-muted">{label}</p>
      <div className="mt-1">{children}</div>
    </div>
  );
}

export default function PortfolioPanel() {
  const { data, loading, error: loadError, reload } = useApiResource(getPortfolio, { initialData: null });
  const [form, setForm] = useState(emptyForm);
  const [editing, setEditing] = useState(null);
  const [formOpen, setFormOpen] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  const holdings = data?.holdings || [];
  const priced = Boolean(data?.priceAvailable);
  const gain = data?.totalGainLoss;
  const gainUp = gain != null && !String(gain).startsWith("-");

  function closeForm() {
    setFormOpen(false);
    setEditing(null);
    setForm(emptyForm());
  }
  function edit(holding) {
    setEditing(holding.id);
    setForm({ symbol: holding.symbol, name: holding.name, category: holding.category, quantity: String(holding.quantity), averagePrice: String(holding.averagePrice), notes: holding.notes || "" });
    setFormOpen(true);
  }
  async function submit(event) {
    event.preventDefault();
    setBusy(true);
    setError("");
    try {
      const payload = { symbol: form.symbol.trim(), name: form.name.trim(), category: form.category, quantity: trimQty(form.quantity), averagePrice: toAmountString(form.averagePrice, "Average price"), notes: form.notes.trim() || null };
      if (editing) await updateHolding(editing, payload);
      else await addHolding(payload);
      await reload();
      closeForm();
    } catch (cause) {
      setError(cause?.isValidationError ? cause.message : getApiErrorMessage(cause));
    } finally {
      setBusy(false);
    }
  }
  async function remove(holding) {
    if (!window.confirm(`Remove ${holding.symbol} from your portfolio?`)) return;
    try {
      await deleteHolding(holding.id);
      await reload();
    } catch (cause) {
      setError(getApiErrorMessage(cause));
    }
  }

  return (
    <section aria-labelledby="portfolio-heading">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h2 id="portfolio-heading" className="flex items-center gap-2 font-display text-2xl font-medium text-ink"><TrendingUp size={20} className="text-accent" aria-hidden="true" />Portfolio</h2>
          <p className="mt-1 text-sm text-ink-soft">Record what you hold to track what you’ve put in. Current value needs a connected price provider.</p>
        </div>
        {!formOpen && <button type="button" className="button-secondary" onClick={() => setFormOpen(true)}><Plus size={16} aria-hidden="true" /> Add holding</button>}
      </div>

      <ErrorMessage message={error || loadError} className="mt-4" onDismiss={() => setError("")} />

      {formOpen && (
        <form onSubmit={submit} className="motion-content mt-4 grid gap-4 rounded-lg border border-line bg-surface p-5 sm:grid-cols-2 lg:grid-cols-3">
          <label className="field-label">Symbol<input required maxLength={24} pattern="[A-Za-z0-9._\-]{1,24}" className="field-control uppercase" value={form.symbol} onChange={(e) => setForm({ ...form, symbol: e.target.value })} placeholder="NIFTYBEES" /></label>
          <label className="field-label sm:col-span-1 lg:col-span-1">Name<input required maxLength={120} className="field-control" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} placeholder="Nifty 50 ETF" /></label>
          <label className="field-label">Type<select className="field-control" value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })}>{CATEGORIES.map((c) => <option key={c} value={c}>{humanize(c)}</option>)}</select></label>
          <label className="field-label">Quantity<input required inputMode="decimal" className="field-control tabular" value={form.quantity} onChange={(e) => setForm({ ...form, quantity: e.target.value.replace(/[^\d.]/g, "") })} placeholder="10" /></label>
          <label className="field-label">Average buy price<input required inputMode="decimal" className="field-control tabular" value={form.averagePrice} onChange={(e) => setForm({ ...form, averagePrice: e.target.value.replace(/[^\d.]/g, "") })} placeholder="0.00" /></label>
          <label className="field-label"><span>Notes <span className="text-xs font-normal text-muted">optional</span></span><input maxLength={255} className="field-control" value={form.notes} onChange={(e) => setForm({ ...form, notes: e.target.value })} /></label>
          <div className="flex gap-2 sm:col-span-2 lg:col-span-3"><button className="button-primary" disabled={busy}>{busy ? "Saving…" : editing ? "Save changes" : "Add holding"}</button><button type="button" className="button-ghost" onClick={closeForm}>Cancel</button></div>
        </form>
      )}

      {loading && !data ? (
        <div className="mt-5"><LoadingSkeleton variant="rows" count={3} /></div>
      ) : holdings.length ? (
        <>
          <div className="mt-5 grid gap-x-8 gap-y-5 sm:grid-cols-3">
            <Stat label="Total invested"><MoneyText value={data.totalInvested} size="md" /></Stat>
            <Stat label="Current value">{priced ? <MoneyText value={data.totalCurrentValue} size="md" /> : <p className="text-[15px] font-medium text-muted">Not available</p>}</Stat>
            <Stat label="Gain / loss">{priced ? <p className={`num text-[1.5rem] font-semibold ${gainUp ? "text-accent-strong" : "text-warm"}`}>{gainUp ? "+" : "−"}{formatMoney(String(gain).replace("-", ""))}<span className="ml-2 text-sm font-medium">{data.totalGainLossPercent}%</span></p> : <p className="text-[15px] font-medium text-muted">Not available</p>}</Stat>
          </div>

          {!priced && <p className="mt-4 border-l-2 border-warm pl-4 text-[13px] leading-5 text-ink-soft">Live prices aren’t connected{data?.priceProvider && data.priceProvider !== "NONE" ? ` for every holding` : ""}, so current value and gain/loss stay hidden rather than guessed. Your invested totals are exact.</p>}

          <div className="mt-6">
            <p className="text-[13px] font-medium text-muted">Allocation by amount invested</p>
            <div className="mt-2 flex h-2.5 w-full gap-[2px] overflow-hidden rounded-full bg-surface-2" aria-hidden="true">
              {holdings.map((holding, index) => <span key={holding.id} className={`${SWATCHES[index % SWATCHES.length]} first:rounded-l-full last:rounded-r-full`} style={{ flex: `0 0 ${Math.max(1, Number(holding.allocationPercent) || 0)}%` }} />)}
            </div>
          </div>

          <ul className="mt-5 divide-y divide-line border-y border-line">
            {holdings.map((holding, index) => {
              const up = holding.gainLoss != null && !String(holding.gainLoss).startsWith("-");
              return (
                <li key={holding.id} className="flex flex-wrap items-center gap-x-4 gap-y-2 py-3.5">
                  <span className={`h-2.5 w-2.5 shrink-0 rounded-full ${SWATCHES[index % SWATCHES.length]}`} aria-hidden="true" />
                  <div className="min-w-0 flex-1">
                    <p className="text-sm font-semibold text-ink"><span className="tabular">{holding.symbol}</span> <span className="font-normal text-ink-soft">{holding.name}</span></p>
                    <p className="num text-xs text-muted">{holding.quantity} × {formatMoney(holding.averagePrice)} · {humanize(holding.category)}{Number(holding.allocationPercent) ? ` · ${holding.allocationPercent}%` : ""}</p>
                  </div>
                  <div className="text-right">
                    <p className="num text-sm font-semibold text-ink">{holding.priceAvailable ? formatMoney(holding.currentValue) : formatMoney(holding.invested)}</p>
                    {holding.priceAvailable
                      ? <p className={`num text-xs font-medium ${up ? "text-accent-strong" : "text-warm"}`}>{up ? "+" : "−"}{formatMoney(String(holding.gainLoss).replace("-", ""))} · {holding.gainLossPercent}%</p>
                      : <p className="num text-xs text-muted">invested</p>}
                  </div>
                  <div className="flex">
                    <button type="button" className="icon-action" aria-label={`Edit ${holding.symbol}`} onClick={() => edit(holding)}><Pencil size={15} /></button>
                    <button type="button" className="icon-action hover:text-danger" aria-label={`Remove ${holding.symbol}`} onClick={() => remove(holding)}><Trash2 size={15} /></button>
                  </div>
                </li>
              );
            })}
          </ul>
        </>
      ) : !formOpen && (
        <div className="mt-5"><EmptyState icon={TrendingUp} title="No holdings yet" description="Record a stock, fund or ETF with the quantity and your average buy price. FinFlow tracks what you’ve invested, and current value once a price provider is connected." action={<button type="button" className="button-primary" onClick={() => setFormOpen(true)}>Add your first holding</button>} /></div>
      )}
    </section>
  );
}
