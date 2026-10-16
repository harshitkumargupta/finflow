import { useEffect, useRef, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { deposit, getBankLinks } from "../api/accountApi";
import { getApiErrorMessage } from "../api/axios";
import AmountField from "../components/AmountField";
import ConfirmationDialog from "../components/ConfirmationDialog";
import EmptyState from "../components/EmptyState";
import ErrorMessage from "../components/ErrorMessage";
import FlowComplete from "../components/FlowComplete";
import LoadingSkeleton from "../components/LoadingSkeleton";
import PageHeader from "../components/PageHeader";
import StepIndicator from "../components/StepIndicator";
import { useToast } from "../context/ToastContext";
import { useAccounts, useSelectedAccountId } from "../hooks/useFinanceData";
import { maskAccountNumber, toAmountString } from "../utils/format";
import { addAmounts, formatMoney } from "../utils/money";

const STEPS = [{ id: "details", label: "Details" }, { id: "review", label: "Review" }, { id: "done", label: "Done" }];

export default function Deposit() {
  const [searchParams] = useSearchParams();
  const { data: accounts, setData: setAccounts, loading, error: loadError, reload: reloadAccounts } = useAccounts();
  const [bankLinks, setBankLinks] = useState([]);
  const [accountId, setAccountId] = useSelectedAccountId(accounts, searchParams.get("accountId"));
  const [amount, setAmount] = useState("");
  const [description, setDescription] = useState("");
  const [sourceType, setSourceType] = useState("SANDBOX_BANK_DEPOSIT");
  const [bankLinkId, setBankLinkId] = useState("");
  const [reviewOpen, setReviewOpen] = useState(false);
  const [result, setResult] = useState(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [reviewError, setReviewError] = useState("");
  const notify = useToast();
  const attemptRef = useRef(null);
  useEffect(() => {
    getBankLinks().then(({ data }) => {
      setBankLinks(data);
      const verified = data.find((link) => link.status === "VERIFIED");
      setBankLinkId(verified ? String(verified.id) : "");
    }).catch((e) => setError(getApiErrorMessage(e)));
  }, []);

  async function submit(event) {
    event.preventDefault();
    setError("");
    try {
      toAmountString(amount);
      setReviewOpen(true);
    } catch (cause) {
      setError(cause.message);
    }
  }

  async function confirmDeposit() {
    setBusy(true); setError(""); setReviewError("");
    try {
      const source = selectedSourceLabel();
      const request = { amount: toAmountString(amount), description: description.trim() || `Sandbox deposit from ${source}` };
      const signature = JSON.stringify({ accountId, request });
      if (!attemptRef.current || attemptRef.current.signature !== signature) attemptRef.current = { signature, key: crypto.randomUUID() };
      const { data } = await deposit(accountId, request, attemptRef.current.key);
      setAccounts((current) =>
        current.map((account) => account.id === data.id ? data : account),
      );
      setResult({ amount: request.amount, source, balance: data.balance, timestamp: new Date().toISOString() });
      notify(`Added ${formatMoney(request.amount)}.`);
      setAmount(""); setDescription(""); attemptRef.current = null;
      setReviewOpen(false);
      try {
        await reloadAccounts();
      } catch {
        notify("Deposit succeeded, but account balances could not be refreshed. Reload to see the latest balance.", "error");
      }
    } catch (e) { setReviewError(getApiErrorMessage(e)); }
    finally { setBusy(false); }
  }
  function selectedSourceLabel() {
    const link = bankLinks.find((item) => String(item.id) === bankLinkId);
    if (sourceType === "LINKED_BANK" && link) return `${link.bankName} ${link.maskedAccountNumber}`;
    if (sourceType === "SANDBOX_AUTOPAY") return "Sandbox autopay funding";
    if (sourceType === "OTHER") return "Other demo source";
    return "Sandbox bank deposit";
  }
  const header = <PageHeader eyebrow="Move money" title="Add money" description="Top up your FinFlow account from a sandbox source. No real bank is charged." />;
  if (loading && !accounts.length) return <div className="mx-auto max-w-xl space-y-8">{header}<LoadingSkeleton variant="rows" count={4} /></div>;
  const account = accounts.find((item) => String(item.id) === accountId) || accounts[0];
  const projectedBalance = account && amount ? addAmounts(account.balance, amount) : null;
  const verifiedLinks = bankLinks.filter((link) => link.status === "VERIFIED");
  return (
    <div className="mx-auto max-w-xl space-y-8">
      {header}
      <StepIndicator steps={STEPS} activeStep={result ? 2 : reviewOpen ? 1 : 0} />
      <ErrorMessage message={error || loadError} />
      {result ? (
        <FlowComplete
          headline={`${formatMoney(result.amount)} added`}
          detail={`From ${result.source}. Your FinFlow account now holds ${formatMoney(result.balance)}.`}
          receiptTitle="Deposit receipt"
          rows={[
            { label: "Amount", value: formatMoney(result.amount) },
            { label: "From", value: result.source },
            { label: "Balance now", value: formatMoney(result.balance) },
            { label: "Time", value: new Date(result.timestamp).toLocaleString() },
          ]}
          onAgain={() => setResult(null)}
          againLabel="Add more"
        />
      ) : !accounts.length ? (
        <EmptyState title="You need an account first" description="Create your FinFlow account, then add sandbox money to it." action={<Link to="/accounts" className="button-primary">Set up account</Link>} />
      ) : (
        <form onSubmit={submit} className="space-y-6">
          <AmountField value={amount} onChange={setAmount} available={account?.balance} availableLabel={`In FinFlow account ${account?.accountNumber ? maskAccountNumber(account.accountNumber) : ""}`} autoFocus />
          <label className="field-label">From<select className="field-control" value={sourceType} onChange={(e) => setSourceType(e.target.value)}><option value="SANDBOX_BANK_DEPOSIT">Sandbox bank deposit</option><option value="LINKED_BANK" disabled={!verifiedLinks.length}>Linked bank account{verifiedLinks.length ? "" : " (none linked yet)"}</option><option value="SANDBOX_AUTOPAY">Sandbox autopay</option><option value="OTHER">Other demo source</option></select></label>
          {sourceType === "LINKED_BANK" && <label className="field-label">Which bank account<select className="field-control" value={bankLinkId} onChange={(e) => setBankLinkId(e.target.value)}>{verifiedLinks.map((link) => <option key={link.id} value={link.id}>{link.bankName} {link.maskedAccountNumber}</option>)}</select></label>}
          <label className="field-label"><span>Note <span className="text-xs font-normal text-muted">optional</span></span><input className="field-control" maxLength={255} value={description} onChange={(e) => setDescription(e.target.value)} /></label>
          <button className="button-primary w-full sm:w-auto" disabled={busy || !accountId}>Review deposit</button>
        </form>
      )}
      <ConfirmationDialog
        open={reviewOpen}
        title="Add money"
        amount={amount}
        details={[
          { label: "From", value: selectedSourceLabel() },
          { label: "To", value: account?.accountNumber ? `FinFlow account ${maskAccountNumber(account.accountNumber)}` : "FinFlow account" },
          { label: "Note", value: description.trim() },
        ]}
        balanceAfter={projectedBalance}
        busy={busy}
        confirmLabel="Add money"
        onConfirm={confirmDeposit}
        onCancel={() => { setReviewOpen(false); setReviewError(""); }}
        pinError={reviewError}
      />
    </div>
  );
}
