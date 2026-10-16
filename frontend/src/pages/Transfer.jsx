import { useRef, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { getTransactionHistory, transfer } from "../api/accountApi";
import { getApiErrorMessage } from "../api/axios";
import AccountSelect from "../components/AccountSelect";
import AmountField from "../components/AmountField";
import ConfirmationDialog from "../components/ConfirmationDialog";
import EmptyState from "../components/EmptyState";
import ErrorMessage from "../components/ErrorMessage";
import PaymentSuccess from "../components/PaymentSuccess";
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

export default function Transfer() {
  const [searchParams] = useSearchParams();
  const { data: accounts, setData: setAccounts, loading, error: loadError, reload: reloadAccounts } = useAccounts();
  const [accountId, setAccountId] = useSelectedAccountId(accounts, searchParams.get("accountId"));
  const [form, setForm] = useState({ receiverAccountNumber: "", amount: "", category: "", description: "" });
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [reviewOpen, setReviewOpen] = useState(false);
  const [transferPin, setTransferPin] = useState("");
  const [reviewError, setReviewError] = useState("");
  const [completedTransfer, setCompletedTransfer] = useState(null);
  const notify = useToast();
  const attemptRef = useRef(null);

  function updateForm(next) {
    setForm(next);
    attemptRef.current = null;
    setCompletedTransfer(null);
  }

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

  async function confirmTransfer() {
    setBusy(true);
    setReviewError("");
    const request = {
      receiverAccountNumber: form.receiverAccountNumber.trim(),
      amount: toAmountString(form.amount),
      ...(form.category ? { category: form.category } : {}),
      ...(form.description.trim() ? { description: form.description.trim() } : {}),
      pin: transferPin,
    };
    const signature = JSON.stringify({ accountId, request });
    if (!attemptRef.current || attemptRef.current.signature !== signature) {
      attemptRef.current = { signature, key: crypto.randomUUID() };
    }
    try {
      const { data } = await transfer(accountId, request, attemptRef.current.key);
      setAccounts((current) =>
        current.map((account) => account.id === data.id ? data : account),
      );
      let referenceNumber = "";
      try {
        const { data: history } = await getTransactionHistory(accountId);
        const matchingTransaction = history.find(
          (transaction) =>
            transaction.type === "TRANSFER_OUT" &&
            Number(transaction.amount) === Number(request.amount) &&
            transaction.receiverAccountNumber === request.receiverAccountNumber &&
            (!request.category || transaction.category === request.category) &&
            transaction.description ===
              (request.description || "Transfer to another account"),
        );
        referenceNumber = matchingTransaction?.referenceNumber || "";
      } catch {
        notify(
          "Transfer succeeded, but its reference could not be loaded. Check transaction history.",
          "error",
        );
      }
      setCompletedTransfer({
        referenceNumber,
        amount: request.amount,
        receiver: request.receiverAccountNumber,
        balance: data.balance,
      });
      notify(`Sent ${formatMoney(request.amount)}.`);
      setForm({ receiverAccountNumber: "", amount: "", category: "", description: "" });
      attemptRef.current = null;
      setReviewOpen(false);
      try {
        await reloadAccounts();
      } catch {
        notify("Transfer succeeded, but account balances could not be refreshed. Reload to see the latest balance.", "error");
      }
    } catch (e) {
      setReviewError(getApiErrorMessage(e));
    } finally { setBusy(false); setTransferPin(""); }
  }
  const header = <PageHeader eyebrow="Move money" title="Send money" description="Send to any FinFlow account number. You’ll see the full picture before anything moves." />;
  if (loading && !accounts.length) return <div className="mx-auto max-w-xl space-y-8">{header}<LoadingSkeleton variant="rows" count={4} /></div>;
  const account = accounts.find((item) => String(item.id) === accountId);
  const balanceAfter = account && form.amount ? addAmounts(account.balance, form.amount, -1) : null;
  return (
    <div className="mx-auto max-w-xl space-y-8">
      {header}
      <StepIndicator steps={STEPS} activeStep={completedTransfer ? 2 : reviewOpen ? 1 : 0} />
      <ErrorMessage message={error || loadError} />
      {completedTransfer ? (
        <PaymentSuccess
          status="success"
          amount={completedTransfer.amount}
          fromLabel={`From · ${account ? maskAccountNumber(account.accountNumber) : "your account"}`}
          toLabel={`To · ${maskAccountNumber(completedTransfer.receiver)}`}
          newBalance={completedTransfer.balance}
          balanceLabel="Balance now"
          receiptTitle="Transfer receipt"
          rows={[
            { label: "Amount", value: formatMoney(completedTransfer.amount) },
            { label: "To", value: completedTransfer.receiver },
            { label: "Reference", value: completedTransfer.referenceNumber || "See transaction history" },
            { label: "Balance now", value: formatMoney(completedTransfer.balance) },
          ]}
          onAgain={() => setCompletedTransfer(null)}
          againLabel="Send another"
        />
      ) : !accounts.length ? (
        <EmptyState title="You need an account first" description="Create your FinFlow account, then you can send money from it." action={<Link to="/accounts" className="button-primary">Set up account</Link>} />
      ) : (
        <form onSubmit={submit} className="space-y-6">
          <AmountField value={form.amount} onChange={(amount) => updateForm({ ...form, amount })} available={account?.balance} availableLabel="In this account" autoFocus />
          <AccountSelect accounts={accounts} value={accountId} onChange={(id) => { setAccountId(id); attemptRef.current = null; }} />
          <label className="field-label">To account number<input className="field-control tabular" autoComplete="off" spellCheck="false" value={form.receiverAccountNumber} onChange={(e) => updateForm({ ...form, receiverAccountNumber: e.target.value })} required /></label>
          <div className="grid gap-4 sm:grid-cols-2">
            <label className="field-label"><span>Category <span className="text-xs font-normal text-muted">optional</span></span><select className="field-control" value={form.category} onChange={(e) => updateForm({ ...form, category: e.target.value })}><option value="">Decide for me</option>{categories.map((category) => <option key={category} value={category}>{categoryLabel(category)}</option>)}</select></label>
            <label className="field-label"><span>Note <span className="text-xs font-normal text-muted">optional</span></span><input className="field-control" maxLength={255} value={form.description} onChange={(e) => updateForm({ ...form, description: e.target.value })} /></label>
          </div>
          <p className="text-[13px] leading-5 text-muted">If something fails and you try again without changing anything, FinFlow reuses the same request, so you can’t send it twice.</p>
          <button className="button-primary w-full sm:w-auto" disabled={busy || !accountId}>Review transfer</button>
        </form>
      )}
      <ConfirmationDialog
        open={reviewOpen}
        title="Send money"
        amount={form.amount}
        details={[
          { label: "From", value: account ? `${account.accountType === "FINFLOW" ? "FinFlow account" : categoryLabel(account.accountType)} ${maskAccountNumber(account.accountNumber)}` : "" },
          { label: "To", value: form.receiverAccountNumber.trim() },
          { label: "Category", value: form.category ? categoryLabel(form.category) : "Decided automatically" },
          { label: "Note", value: form.description.trim() },
        ]}
        balanceAfter={balanceAfter}
        busy={busy}
        confirmLabel={`Send ${formatMoney(toSafeAmount(form.amount))}`}
        onConfirm={confirmTransfer}
        onCancel={() => { setReviewOpen(false); setReviewError(""); setTransferPin(""); }}
        pin={transferPin}
        onPinChange={(value) => { setTransferPin(value); setReviewError(""); }}
        pinError={reviewError}
      />
    </div>
  );
}

function toSafeAmount(value) {
  try { return toAmountString(value); } catch { return null; }
}
