import { useEffect, useRef, useState } from "react";
import { Link, useLocation, useNavigate, useSearchParams } from "react-router-dom";
import { getBankLinks, withdraw } from "../api/accountApi";
import { getApiErrorMessage } from "../api/axios";
import { suggestCategory } from "../api/insightApi";
import AmountField from "../components/AmountField";
import ConfirmationDialog from "../components/ConfirmationDialog";
import EmptyState from "../components/EmptyState";
import ErrorMessage from "../components/ErrorMessage";
import FlowComplete from "../components/FlowComplete";
import LoadingSkeleton from "../components/LoadingSkeleton";
import PageHeader from "../components/PageHeader";
import StepIndicator from "../components/StepIndicator";
import { SPENDING_CATEGORIES, categoryLabel } from "../constants/categories";
import { useToast } from "../context/ToastContext";
import { useAccounts, useSelectedAccountId } from "../hooks/useFinanceData";
import { maskAccountNumber, toAmountString } from "../utils/format";
import { addAmounts, formatMoney } from "../utils/money";

const categories = SPENDING_CATEGORIES;
const STEPS = [{ id: "details", label: "Details" }, { id: "review", label: "Review" }, { id: "done", label: "Done" }];

export default function Withdraw() {
  const [searchParams] = useSearchParams();
  const location = useLocation();
  const navigate = useNavigate();
  const { data: accounts, setData: setAccounts, loading, error: loadError, reload: reloadAccounts } = useAccounts();
  const [bankLinks, setBankLinks] = useState([]);
  const [accountId, setAccountId] = useSelectedAccountId(accounts, searchParams.get("accountId"));
  const [form, setForm] = useState({ amount: "", category: "", description: "", destinationType: "LINKED_BANK", destination: "" });
  const [result, setResult] = useState(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [reviewOpen, setReviewOpen] = useState(false);
  const [withdrawPin, setWithdrawPin] = useState("");
  const [reviewError, setReviewError] = useState("");
  const [suggesting, setSuggesting] = useState(false);
  const [suggestNote, setSuggestNote] = useState("");
  const notify = useToast();

  async function suggestCat() {
    const description = form.description.trim();
    if (!description || suggesting) return;
    setSuggesting(true);
    setSuggestNote("");
    try {
      const { data } = await suggestCategory(description, "WITHDRAWAL");
      if (data?.category) setForm((current) => ({ ...current, category: data.category }));
      setSuggestNote(data?.message || "");
    } catch (cause) {
      setSuggestNote(getApiErrorMessage(cause));
    } finally {
      setSuggesting(false);
    }
  }
  const attemptRef = useRef(null);
  useEffect(() => {
    getBankLinks().then(({ data }) => {
      setBankLinks(data);
      const verified = data.find((link) => link.status === "VERIFIED");
      setForm((current) => ({ ...current, destinationType: verified ? "LINKED_BANK" : "SANDBOX_UPI", destination: verified ? String(verified.id) : "" }));
    }).catch((e) => setError(getApiErrorMessage(e)));
  }, []);

  useEffect(() => {
    const prefill = location.state?.receiptDraft;
    if (!prefill) return;
    setForm((current) => ({
      ...current,
      amount: prefill.amount ? String(prefill.amount) : current.amount,
      category: prefill.category || current.category,
      description: prefill.description || current.description,
    }));
    navigate(location.pathname, { replace: true, state: null });
  }, [location, navigate]);

  async function submit(event) {
    event.preventDefault();
    setError("");
    try {
      toAmountString(form.amount);
      setReviewOpen(true);
    } catch (cause) {
      setError(cause.message);
    }
  }

  async function confirmWithdrawal() {
    setBusy(true);
    setReviewError("");
    try {
      const request = {
        amount: toAmountString(form.amount),
        ...(form.category ? { category: form.category } : {}),
        ...(form.description.trim() ? { description: form.description.trim() } : {}),
        pin: withdrawPin,
      };
      const signature = JSON.stringify({ accountId, request });
      if (!attemptRef.current || attemptRef.current.signature !== signature) attemptRef.current = { signature, key: crypto.randomUUID() };
      const { data } = await withdraw(accountId, request, attemptRef.current.key);
      setAccounts((current) =>
        current.map((account) => account.id === data.id ? data : account),
      );
      setResult({ amount: request.amount, destination: destinationLabel(), category: form.category ? categoryLabel(form.category) : "Auto-categorized", balance: data.balance, timestamp: new Date().toISOString() });
      notify(`Withdrew ${formatMoney(request.amount)}.`);
      setForm({ ...form, amount: "", description: "" }); attemptRef.current = null;
      setReviewOpen(false);
      try {
        await reloadAccounts();
      } catch {
        notify("Withdrawal succeeded, but account balances could not be refreshed. Reload to see the latest balance.", "error");
      }
    } catch (e) { setReviewError(getApiErrorMessage(e)); }
    finally { setBusy(false); setWithdrawPin(""); }
  }
  function destinationLabel() {
    if (form.destinationType === "LINKED_BANK") {
      const link = bankLinks.find((item) => String(item.id) === form.destination);
      return link ? `${link.bankName} ${link.maskedAccountNumber}` : "Linked bank account";
    }
    if (form.destinationType === "SANDBOX_UPI") return form.destination || "Sandbox UPI ID";
    return form.destination || "Sandbox external account";
  }
  const header = <PageHeader eyebrow="Move money" title="Withdraw" description="Move money out of FinFlow to a sandbox destination. No real bank transfer happens." />;
  if (loading && !accounts.length) return <div className="mx-auto max-w-xl space-y-8">{header}<LoadingSkeleton variant="rows" count={4} /></div>;
  const account = accounts.find((item) => String(item.id) === accountId) || accounts[0];
  const remainingBalance = account && form.amount ? addAmounts(account.balance, form.amount, -1) : null;
  const verifiedLinks = bankLinks.filter((link) => link.status === "VERIFIED");
  return (
    <div className="mx-auto max-w-xl space-y-8">
      {header}
      <StepIndicator steps={STEPS} activeStep={result ? 2 : reviewOpen ? 1 : 0} />
      <ErrorMessage message={error || loadError} />
      {result ? (
        <FlowComplete
          headline={`${formatMoney(result.amount)} withdrawn`}
          detail={`To ${result.destination}. You have ${formatMoney(result.balance)} left in FinFlow.`}
          receiptTitle="Withdrawal receipt"
          rows={[
            { label: "Amount", value: formatMoney(result.amount) },
            { label: "To", value: result.destination },
            { label: "Category", value: result.category },
            { label: "Balance now", value: formatMoney(result.balance) },
            { label: "Time", value: new Date(result.timestamp).toLocaleString() },
          ]}
          onAgain={() => setResult(null)}
          againLabel="Withdraw again"
        />
      ) : !accounts.length ? (
        <EmptyState title="You need an account first" description="Create your FinFlow account before withdrawing." action={<Link to="/accounts" className="button-primary">Set up account</Link>} />
      ) : (
        <form onSubmit={submit} className="space-y-6">
          <AmountField value={form.amount} onChange={(amount) => setForm({ ...form, amount })} available={account?.balance} availableLabel="Available to withdraw" autoFocus />
          <div className="grid gap-4 sm:grid-cols-2">
            <label className="field-label">Send it to<select className="field-control" value={form.destinationType} onChange={(e) => setForm({ ...form, destinationType: e.target.value, destination: e.target.value === "LINKED_BANK" && verifiedLinks[0] ? String(verifiedLinks[0].id) : "" })}><option value="LINKED_BANK" disabled={!verifiedLinks.length}>Linked bank account{verifiedLinks.length ? "" : " (none yet)"}</option><option value="SANDBOX_UPI">Sandbox UPI ID</option><option value="SANDBOX_EXTERNAL">Sandbox external account</option></select></label>
            {form.destinationType === "LINKED_BANK" ? <label className="field-label">Bank account<select className="field-control" value={form.destination} onChange={(e) => setForm({ ...form, destination: e.target.value })}>{verifiedLinks.map((link) => <option key={link.id} value={link.id}>{link.bankName} {link.maskedAccountNumber}</option>)}</select></label> : <label className="field-label">{form.destinationType === "SANDBOX_UPI" ? "UPI ID" : "Reference"}<input className="field-control" maxLength={80} value={form.destination} onChange={(e) => setForm({ ...form, destination: e.target.value })} placeholder={form.destinationType === "SANDBOX_UPI" ? "name@demo" : "Demo reference"} /></label>}
          </div>
          <div className="grid gap-4 sm:grid-cols-2">
            <div className="field-label">
              <span className="flex items-center justify-between gap-2"><span>Category <span className="text-xs font-normal text-muted">optional</span></span><button type="button" className="text-[12px] font-medium text-accent-strong disabled:opacity-50" onClick={suggestCat} disabled={!form.description.trim() || suggesting}>{suggesting ? "Suggesting…" : "Suggest a category"}</button></span>
              <select className="field-control" value={form.category} onChange={(e) => { setForm({ ...form, category: e.target.value }); setSuggestNote(""); }}><option value="">Decide for me</option>{categories.map((category) => <option key={category} value={category}>{categoryLabel(category)}</option>)}</select>
              {suggestNote && <span className="mt-1 block text-[12px] font-normal text-muted">{suggestNote}</span>}
            </div>
            <label className="field-label"><span>Note <span className="text-xs font-normal text-muted">optional</span></span><input className="field-control" maxLength={255} value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} /></label>
          </div>
          <button className="button-primary w-full sm:w-auto" disabled={busy || !accountId}>Review withdrawal</button>
        </form>
      )}
      <ConfirmationDialog
        open={reviewOpen}
        title="Withdraw"
        amount={form.amount}
        details={[
          { label: "From", value: account?.accountNumber ? `FinFlow account ${maskAccountNumber(account.accountNumber)}` : "" },
          { label: "To", value: destinationLabel() },
          { label: "Category", value: form.category ? categoryLabel(form.category) : "Decided automatically" },
          { label: "Note", value: form.description.trim() },
        ]}
        balanceAfter={remainingBalance}
        balanceLabel="Left in FinFlow"
        busy={busy}
        confirmLabel={`Withdraw ${formatMoney(toSafeAmount(form.amount))}`}
        onConfirm={confirmWithdrawal}
        onCancel={() => { setReviewOpen(false); setReviewError(""); setWithdrawPin(""); }}
        pin={withdrawPin}
        onPinChange={(value) => { setWithdrawPin(value); setReviewError(""); }}
        pinError={reviewError}
      />
    </div>
  );
}

function toSafeAmount(value) {
  try { return toAmountString(value); } catch { return null; }
}
