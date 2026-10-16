import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { AlertTriangle, ArrowUpFromLine, CalendarClock, ScanLine } from "lucide-react";
import { scanReceipt } from "../api/receiptApi";
import { getApiErrorMessage } from "../api/axios";
import AmountField from "../components/AmountField";
import EmptyState from "../components/EmptyState";
import ErrorMessage from "../components/ErrorMessage";
import PageHeader from "../components/PageHeader";
import ReceiptDropzone from "../components/ReceiptDropzone";
import { PLANNING_CATEGORIES, categoryLabel } from "../constants/categories";
import { useToast } from "../context/ToastContext";
import { toDecimalString } from "../utils/money";

function todayISO() {
  return new Date().toISOString().slice(0, 10);
}

function FieldFlag({ field }) {
  if (!field || field.value == null) return <span className="status-pill status-warning">Not found</span>;
  if (field.flagged) return <span className="status-pill status-warning"><AlertTriangle size={12} aria-hidden="true" /> Please verify</span>;
  return <span className="status-pill status-success">Looks good</span>;
}

export default function Receipts() {
  const navigate = useNavigate();
  const notify = useToast();
  const [status, setStatus] = useState("idle");
  const [error, setError] = useState("");
  const [draft, setDraft] = useState(null);

  async function handleFile(file) {
    setStatus("scanning");
    setError("");
    try {
      const { data } = await scanReceipt(file);
      if (!data.available) {
        setStatus("unavailable");
        return;
      }
      setDraft({
        merchant: data.merchant?.value || "",
        amount: toDecimalString(data.total?.value) || "",
        date: data.date?.value || todayISO(),
        upiId: data.upiId?.value || "",
        category: "OTHER",
        fields: { merchant: data.merchant, date: data.date, total: data.total, upiId: data.upiId },
      });
      setStatus("draft");
      notify("Receipt scanned. Review the draft.");
    } catch (scanError) {
      setError(getApiErrorMessage(scanError));
      setStatus("error");
    }
  }

  function reset() {
    setDraft(null);
    setError("");
    setStatus("idle");
  }

  function describe(prefix) {
    const parts = [draft.merchant, draft.upiId].filter(Boolean);
    return parts.length ? `${prefix} — ${parts.join(" · ")}` : prefix;
  }

  function createBill() {
    navigate("/bills", { state: { receiptDraft: { name: draft.merchant, amount: draft.amount, category: draft.category, nextDueDate: draft.date, description: describe("Scanned receipt") } } });
  }

  function logExpense() {
    navigate("/withdraw", { state: { receiptDraft: { amount: draft.amount, category: draft.category, description: describe("Receipt") } } });
  }

  const header = <PageHeader eyebrow="Scan" title="Scan a receipt" description="Upload a bill or receipt to pull out the merchant, amount and date. Nothing is saved until you confirm, and the image is discarded after scanning." />;

  return (
    <div className="mx-auto max-w-xl space-y-8">
      {header}

      {status === "unavailable" ? (
        <EmptyState
          title="Scanning isn’t available"
          description="Receipt scanning is turned off on this server. An administrator can enable an OCR provider to switch it on."
          action={<button type="button" className="button-secondary" onClick={reset}>Back</button>}
        />
      ) : status === "draft" && draft ? (
        <section aria-labelledby="draft-heading" className="space-y-6">
          <div className="flex items-center gap-2 text-sm text-ink-soft"><ScanLine size={16} aria-hidden="true" /> <h2 id="draft-heading" className="font-medium">Extracted draft</h2></div>

          <div className="space-y-5 rounded-xl border border-line bg-surface p-5">
            <div>
              <div className="flex items-center justify-between gap-2"><label className="field-label" htmlFor="draft-merchant">Merchant</label><FieldFlag field={draft.fields.merchant} /></div>
              <input id="draft-merchant" className="field-control mt-1.5" maxLength={120} value={draft.merchant} onChange={(e) => setDraft({ ...draft, merchant: e.target.value })} placeholder="Store or biller name" />
            </div>

            <div>
              <div className="flex items-center justify-between gap-2"><span className="field-label">Total amount</span><FieldFlag field={draft.fields.total} /></div>
              <div className="mt-1.5"><AmountField label="" value={draft.amount} onChange={(amount) => setDraft({ ...draft, amount })} /></div>
            </div>

            <div>
              <div className="flex items-center justify-between gap-2"><label className="field-label" htmlFor="draft-upi">UPI ID <span className="text-xs font-normal text-muted">if present</span></label><FieldFlag field={draft.fields.upiId} /></div>
              <input id="draft-upi" className="field-control mt-1.5" value={draft.upiId} onChange={(e) => setDraft({ ...draft, upiId: e.target.value })} placeholder="name@bank" />
            </div>

            <div className="grid gap-4 sm:grid-cols-2">
              <div>
                <div className="flex items-center justify-between gap-2"><label className="field-label" htmlFor="draft-date">Date</label><FieldFlag field={draft.fields.date} /></div>
                <input id="draft-date" type="date" className="field-control mt-1.5" value={draft.date} onChange={(e) => setDraft({ ...draft, date: e.target.value })} />
              </div>
              <label className="field-label">Category<select className="field-control mt-1.5" value={draft.category} onChange={(e) => setDraft({ ...draft, category: e.target.value })}>{PLANNING_CATEGORIES.map((category) => <option key={category} value={category}>{categoryLabel(category)}</option>)}</select></label>
            </div>

            <p className="text-[13px] leading-5 text-muted">Flagged fields were read with low confidence — check them before saving. FinFlow never creates a transaction on its own.</p>
          </div>

          <div className="flex flex-wrap gap-2">
            <button type="button" className="button-primary" onClick={createBill}><CalendarClock size={16} aria-hidden="true" /> Create a bill</button>
            <button type="button" className="button-secondary" onClick={logExpense}><ArrowUpFromLine size={16} aria-hidden="true" /> Log as expense</button>
            <button type="button" className="button-ghost" onClick={reset}>Scan another</button>
          </div>
        </section>
      ) : (
        <div className="space-y-4">
          <ErrorMessage message={error} onDismiss={() => setError("")} />
          <ReceiptDropzone onFile={handleFile} busy={status === "scanning"} disabled={status === "scanning"} />
          {status === "scanning" && <p role="status" className="text-[13px] text-muted">Reading your receipt…</p>}
        </div>
      )}
    </div>
  );
}
