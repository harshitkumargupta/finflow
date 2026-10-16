import { useCallback, useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { QRCodeSVG } from "qrcode.react";
import {
  ArrowDownToLine,
  ArrowUpRight,
  Check,
  CheckCircle2,
  Copy,
  Plus,
  QrCode,
  RefreshCw,
  Send,
  Share2,
  ShieldCheck,
  Trash2,
  X,
} from "lucide-react";
import { getApiErrorMessage } from "../api/axios";
import { getMyAccounts } from "../api/accountApi";
import {
  addWalletFunds,
  confirmWalletPayment,
  createMoneyReservation,
  createMoneyRule,
  createPaymentQr,
  createSpendingLimit,
  createWallet,
  deleteMoneyRule,
  deleteSpendingLimit,
  getFinancialSafety,
  getMoneyReservations,
  getMoneyRules,
  getSpendingLimits,
  getWallet,
  getWalletLedger,
  getWalletPayments,
  getWalletTransactions,
  releaseMoneyReservation,
  resolvePaymentId,
  scanPaymentQr,
  transferWalletFunds,
  updatePaymentIdentity,
} from "../api/walletApi";
import ErrorMessage from "../components/ErrorMessage";
import EmptyState from "../components/EmptyState";
import LoadingSkeleton from "../components/LoadingSkeleton";
import Money from "../components/Money";
import MoneyText from "../components/MoneyText";
import PinInput from "../components/PinInput";
import AmountField from "../components/AmountField";
import StatusBadge from "../components/StatusBadge";
import PageHeader from "../components/PageHeader";
import StatSkeleton from "../components/StatSkeleton";
import { categoryLabel } from "../constants/categories";
import { useToast } from "../context/ToastContext";
import { formatCurrency, formatDate, toAmountString, addAmounts } from "../utils/format";
import ScanFrame from "../components/ScanFrame";
import SuccessCheck from "../components/SuccessCheck";
import MoneyMoveLoader from "../components/MoneyMoveLoader";
import { SPENDING_CATEGORIES } from "../constants/categories";
import { humanize } from "../utils/status";

const categories = SPENDING_CATEGORIES;
const allocationTypes = ["SPENDING", "SAVINGS", "BILLS", "GOAL", "EMERGENCY"];

function makeIdempotencyKey() {
  if (window.crypto?.randomUUID) return window.crypto.randomUUID();
  return `${Date.now()}-${Math.random().toString(36).slice(2)}`;
}

export default function Wallet() {
  const notify = useToast();
  const videoRef = useRef(null);
  const [wallet, setWallet] = useState(null);
  const [safety, setSafety] = useState(null);
  const [accounts, setAccounts] = useState([]);
  const [reservations, setReservations] = useState([]);
  const [rules, setRules] = useState([]);
  const [limits, setLimits] = useState([]);
  const [transactions, setTransactions] = useState([]);
  const [walletLedger, setWalletLedger] = useState([]);
  const [payments, setPayments] = useState([]);
  const [qr, setQr] = useState(null);
  const [paymentIdInput, setPaymentIdInput] = useState("");
  const [copied, setCopied] = useState(false);

  // Modals & Flows
  const [showReceiveModal, setShowReceiveModal] = useState(false);
  const [showPayModal, setShowPayModal] = useState(false);
  const [payStep, setPayStep] = useState("RESOLVE"); // RESOLVE | DETAILS | CONFIRM | SUCCESS

  // Payment Form & State
  const [payRecipientInput, setPayRecipientInput] = useState("");
  const [resolvedRecipient, setResolvedRecipient] = useState(null); // { paymentId, recipientName }
  const [payAmount, setPayAmount] = useState("");
  const [payCategory, setPayCategory] = useState("OTHER");
  const [payDescription, setPayDescription] = useState("");
  const [paySenderAccountId, setPaySenderAccountId] = useState(""); // "" = Wallet Balance
  const [payPin, setPayPin] = useState("");
  const [paymentResult, setPaymentResult] = useState(null);
  const payIdempotencyKeyRef = useRef("");

  // Funding & Transfer forms
  const [fundingForm, setFundingForm] = useState({ accountId: "", amount: "", description: "" });
  const [walletTransferForm, setWalletTransferForm] = useState({ receiverWalletIdentifier: "", amount: "", description: "" });
  const [reserveForm, setReserveForm] = useState({ accountId: "", amount: "", bucket: "SAVINGS", description: "" });
  const [ruleForm, setRuleForm] = useState({ name: "", percentage: "", destinationType: "SAVINGS", destinationId: "", priority: 1 });
  const [limitForm, setLimitForm] = useState({ scopeType: "MONTHLY", category: "OVERALL", amount: "" });

  // Camera & Scan
  const [cameraOpen, setCameraOpen] = useState(false);
  const [cameraMessage, setCameraMessage] = useState("");

  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState("");
  const [error, setError] = useState("");
  const [payModalError, setPayModalError] = useState("");

  const fundingAttempt = useRef(null);
  const walletTransferAttempt = useRef(null);

  const load = useCallback(async () => {
    setError("");
    const results = await Promise.allSettled([
      getWallet(),
      getFinancialSafety(),
      getMyAccounts(),
      getMoneyReservations(),
      getMoneyRules(),
      getSpendingLimits(),
      getWalletTransactions(),
      getWalletPayments(),
      getWalletLedger(),
    ]);
    const failed = results.find(
      (result) => result.status === "rejected" && result.reason?.response?.status !== 404
    );
    if (failed) throw failed.reason;
    setWallet(results[0].status === "fulfilled" ? results[0].value.data : null);
    setSafety(results[1].status === "fulfilled" ? results[1].value.data : null);
    setAccounts(results[2].status === "fulfilled" ? results[2].value.data : []);
    setReservations(results[3].status === "fulfilled" ? results[3].value.data : []);
    setRules(results[4].status === "fulfilled" ? results[4].value.data : []);
    setLimits(results[5].status === "fulfilled" ? results[5].value.data : []);
    setTransactions(results[6].status === "fulfilled" ? results[6].value.data : []);
    setPayments(results[7].status === "fulfilled" ? results[7].value.data : []);
    setWalletLedger(results[8].status === "fulfilled" ? results[8].value.data : []);
  }, []);

  useEffect(() => {
    load()
      .catch((cause) => setError(getApiErrorMessage(cause)))
      .finally(() => setLoading(false));
  }, [load]);

  // Camera QR scanner effect
  useEffect(() => {
    if (!cameraOpen) return undefined;
    let stream;
    let timer;
    let stopped = false;
    async function startCamera() {
      if (!("BarcodeDetector" in window)) {
        setCameraMessage("QR camera scanner is unavailable in this browser. Enter the FinFlow Payment ID manually below.");
        return;
      }
      try {
        stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: "environment" } });
        if (stopped) {
          stream.getTracks().forEach((track) => track.stop());
          return;
        }
        const video = videoRef.current;
        if (!video) return;
        video.srcObject = stream;
        await video.play();
        const detector = new window.BarcodeDetector({ formats: ["qr_code"] });
        timer = window.setInterval(async () => {
          try {
            const codes = await detector.detect(video);
            if (codes[0]?.rawValue) {
              setPayRecipientInput(codes[0].rawValue);
              setCameraOpen(false);
              handleResolveRecipient(codes[0].rawValue);
            }
          } catch {
            setCameraMessage("Point your camera at a FinFlow QR code or type the Payment ID manually.");
          }
        }, 350);
        setCameraMessage("");
      } catch {
        setCameraMessage("Camera permission not granted. Enter the FinFlow Payment ID manually.");
      }
    }
    startCamera();
    return () => {
      stopped = true;
      window.clearInterval(timer);
      stream?.getTracks().forEach((track) => track.stop());
    };
  }, [cameraOpen]);

  async function runAction(name, action, successMsg) {
    setBusy(name);
    setError("");
    try {
      await action();
      await load();
      if (successMsg) notify(successMsg);
    } catch (cause) {
      setError(cause?.isValidationError ? cause.message : getApiErrorMessage(cause));
    } finally {
      setBusy("");
    }
  }

  function handleCopyPaymentId(idToCopy) {
    const target = idToCopy || wallet?.paymentId;
    if (!target) return;
    navigator.clipboard.writeText(target);
    setCopied(true);
    notify("Payment ID copied to clipboard!");
    setTimeout(() => setCopied(false), 2000);
  }

  async function handleShareQr() {
    if (!wallet) return;
    if (navigator.share) {
      try {
        await navigator.share({
          title: "My FinFlow Payment ID",
          text: `Pay me on FinFlow using my Payment ID: ${wallet.paymentId}`,
        });
      } catch {
        handleCopyPaymentId(wallet.paymentId);
      }
    } else {
      handleCopyPaymentId(wallet.paymentId);
    }
  }

  async function openReceiveModal() {
    setShowReceiveModal(true);
    setBusy("qr");
    try {
      const { data } = await createPaymentQr();
      setQr(data);
    } catch (cause) {
      setError(getApiErrorMessage(cause));
    } finally {
      setBusy("");
    }
  }

  function openPayModal(initialId = "") {
    setShowPayModal(true);
    setPayStep("RESOLVE");
    setPayRecipientInput(initialId);
    setResolvedRecipient(null);
    setPayAmount("");
    setPayCategory("OTHER");
    setPayDescription("");
    setPaySenderAccountId("");
    setPayPin("");
    setPayModalError("");
    setPaymentResult(null);
    payIdempotencyKeyRef.current = makeIdempotencyKey();
    if (initialId) {
      handleResolveRecipient(initialId);
    }
  }

  async function handleResolveRecipient(inputOverride) {
    const raw = (inputOverride || payRecipientInput).trim();
    if (!raw) {
      setPayModalError("Please enter a FinFlow Payment ID or QR payload");
      return;
    }
    setBusy("resolve");
    setPayModalError("");
    try {
      const { data } = await resolvePaymentId(raw);
      setResolvedRecipient(data);
      setPayStep("DETAILS");
    } catch (cause) {
      setPayModalError(getApiErrorMessage(cause));
      setResolvedRecipient(null);
    } finally {
      setBusy("");
    }
  }

  function handleReviewPayment(e) {
    e.preventDefault();
    setPayModalError("");
    let amountText;
    try {
      amountText = toAmountString(payAmount);
    } catch (cause) {
      setPayModalError(cause.message);
      return;
    }
    if (!paySenderAccountId && wallet && addAmounts(wallet.balance, amountText, -1)?.startsWith("-")) {
      setPayModalError(`Insufficient wallet balance (Available: ${formatCurrency(wallet.balance)})`);
      return;
    }
    setPayStep("CONFIRM");
  }

  async function handleConfirmPayment() {
    setBusy("confirm-payment");
    setPayModalError("");
    try {
      const payload = {
        receiverPaymentId: resolvedRecipient.paymentId,
        amount: toAmountString(payAmount),
        category: payCategory,
        description: payDescription,
        senderAccountId: paySenderAccountId ? Number(paySenderAccountId) : null,
        pin: payPin,
      };
      const { data } = await confirmWalletPayment(payload, payIdempotencyKeyRef.current);
      setPaymentResult(data);
      setPayStep("SUCCESS");
      await load();
      notify(`₹${payAmount} sent to ${resolvedRecipient.paymentId}`);
    } catch (cause) {
      setPayModalError(getApiErrorMessage(cause));
    } finally {
      setPayPin("");
      setBusy("");
    }
  }

  async function changeIdentity(event) {
    event.preventDefault();
    if (!paymentIdInput.trim()) return;
    await runAction(
      "identity",
      async () => {
        const { data } = await updatePaymentIdentity(paymentIdInput.trim().toLowerCase());
        setWallet(data);
        setPaymentIdInput("");
      },
      "FinFlow Payment ID updated."
    );
  }

  async function submitWalletFunding(event) {
    event.preventDefault();
    await runAction(
      "wallet-funding",
      async () => {
        const request = {
          ...fundingForm,
          accountId: Number(fundingForm.accountId),
          amount: toAmountString(fundingForm.amount),
        };
        const signature = JSON.stringify(request);
        if (fundingAttempt.current?.signature !== signature) {
          fundingAttempt.current = { signature, key: makeIdempotencyKey() };
        }
        await addWalletFunds(request, fundingAttempt.current.key);
        fundingAttempt.current = null;
        setFundingForm({ accountId: "", amount: "", description: "" });
      },
      "Funds added to your FinFlow wallet."
    );
  }

  async function submitWalletTransfer(event) {
    event.preventDefault();
    await runAction(
      "wallet-transfer",
      async () => {
        const request = {
          ...walletTransferForm,
          receiverWalletIdentifier: walletTransferForm.receiverWalletIdentifier.trim(),
          amount: toAmountString(walletTransferForm.amount),
        };
        const signature = JSON.stringify(request);
        if (walletTransferAttempt.current?.signature !== signature) {
          walletTransferAttempt.current = { signature, key: makeIdempotencyKey() };
        }
        await transferWalletFunds(request, walletTransferAttempt.current.key);
        walletTransferAttempt.current = null;
        setWalletTransferForm({ receiverWalletIdentifier: "", amount: "", description: "" });
      },
      "Wallet transfer completed."
    );
  }

  async function submitRule(event) {
    event.preventDefault();
    await runAction(
      "rule",
      async () => {
        await createMoneyRule({
          ...ruleForm,
          percentage: Number(ruleForm.percentage),
          priority: Number(ruleForm.priority),
          destinationId: ruleForm.destinationType === "GOAL" ? Number(ruleForm.destinationId) : null,
          active: true,
        });
        setRuleForm({ name: "", percentage: "", destinationType: "SAVINGS", destinationId: "", priority: 1 });
      },
      "Deposit allocation rule saved."
    );
  }

  async function submitLimit(event) {
    event.preventDefault();
    await runAction(
      "limit",
      async () => {
        await createSpendingLimit({
          ...limitForm,
          category: limitForm.scopeType === "CATEGORY_MONTHLY" ? limitForm.category : "OVERALL",
          amount: toAmountString(limitForm.amount),
        });
        setLimitForm({ ...limitForm, amount: "" });
      },
      "Spending limit saved."
    );
  }

  async function submitReservation(event) {
    event.preventDefault();
    await runAction(
      "reserve",
      async () => {
        await createMoneyReservation({
          ...reserveForm,
          accountId: Number(reserveForm.accountId),
          amount: toAmountString(reserveForm.amount),
        });
        setReserveForm({ ...reserveForm, amount: "", description: "" });
      },
      "Funds reserved successfully."
    );
  }

  useEffect(() => {
    if (!showReceiveModal && !showPayModal) return undefined;
    const onKeyDown = (event) => {
      if (event.key !== "Escape" || busy) return;
      setShowReceiveModal(false);
      setShowPayModal(false);
    };
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [showReceiveModal, showPayModal, busy]);

  if (loading) return <div className="space-y-7"><PageHeader eyebrow="INTERNAL FINFLOW PAYMENT SYSTEM" title="FinFlow Wallet & Payments" /><StatSkeleton count={3} /><LoadingSkeleton variant="rows" count={4} /></div>;

  return (
    <div className="space-y-7">
      <PageHeader
        eyebrow="INTERNAL FINFLOW PAYMENT SYSTEM"
        title="FinFlow Wallet & Payments"
        description="Send and receive money instantly across FinFlow users using your unique Payment ID and QR code."
        action={<button type="button" onClick={() => load().catch((cause) => setError(getApiErrorMessage(cause)))} className="button-secondary"><RefreshCw size={16} /> Refresh</button>}
      />

      <ErrorMessage message={error} onDismiss={() => setError("")} />

      {!wallet ? (
        <section className="card">
          <div className="flex items-start gap-3">
            <span className="rounded-xl bg-brand-50 p-3 text-brand-700">
              <ShieldCheck size={20} />
            </span>
            <div>
              <h2 className="section-title">Setup your FinFlow Payment Identity</h2>
              <p className="mt-1 text-sm text-slate-500">
                Creates your unique Payment ID (<code className="text-brand-700">username@finflow</code>) for instant internal payments.
              </p>
            </div>
          </div>
          <button
            className="button-primary mt-5"
            disabled={busy === "wallet"}
            onClick={() =>
              runAction(
                "wallet",
                async () => {
                  const { data } = await createWallet();
                  setWallet(data);
                },
                "FinFlow wallet identity created."
              )
            }
          >
            {busy === "wallet" ? "Creating Wallet…" : "Create FinFlow Wallet"}
          </button>
        </section>
      ) : (
        <>
          {/* Main Wallet Banner Card */}
          <section aria-labelledby="wallet-balance-heading" className="space-y-6">
            <div className="flex flex-wrap items-end justify-between gap-6">
              <div>
                <p id="wallet-balance-heading" className="eyebrow">Wallet balance</p>
                <MoneyText value={wallet.balance} size="hero" className="mt-3" />
                <div className="mt-4 flex flex-wrap items-center gap-2 text-sm">
                  <span className="text-muted">Your Payment ID</span>
                  <code className="tabular rounded-md border border-line bg-surface px-2.5 py-1 text-[13px] font-medium text-ink">{wallet.paymentId}</code>
                  <button type="button" onClick={() => handleCopyPaymentId(wallet.paymentId)} className="button-ghost min-h-8 px-2 text-xs" aria-live="polite">
                    {copied ? <Check size={14} aria-hidden="true" /> : <Copy size={14} aria-hidden="true" />}
                    {copied ? "Copied" : "Copy"}
                  </button>
                </div>
              </div>
              <div className="flex flex-wrap gap-2">
                <button type="button" onClick={() => openPayModal()} className="button-primary"><Send size={16} aria-hidden="true" /> Pay someone</button>
                <button type="button" onClick={openReceiveModal} className="button-secondary"><QrCode size={16} aria-hidden="true" /> Receive</button>
              </div>
            </div>
            <dl className="grid gap-x-8 gap-y-4 border-y border-line py-4 sm:grid-cols-3">
              <div><dt className="text-[13px] text-muted">Status</dt><dd className="mt-1"><StatusBadge status={wallet.status} /></dd></div>
              <div><dt className="text-[13px] text-muted">In your accounts</dt><dd className="tabular mt-1 text-lg font-semibold text-ink">{formatCurrency(wallet.totalBalance)}</dd></div>
              <div><dt className="text-[13px] text-muted">Set aside for goals &amp; bills</dt><dd className="tabular mt-1 text-lg font-semibold text-ink">{formatCurrency(wallet.reservedBalance)}</dd></div>
            </dl>
          </section>

          {/* Wallet Operations & Funding */}
          <section className="grid gap-5 xl:grid-cols-2">
            {/* Add Funds to Wallet */}
            <article className="card">
              <div className="flex items-center gap-3">
                <span className="rounded-xl bg-brand-50 p-3 text-brand-700">
                  <ArrowDownToLine size={20} />
                </span>
                <div>
                  <h2 className="section-title">Add Funds to Wallet</h2>
                  <p className="mt-1 text-sm text-slate-500">
                    Move funds from your linked bank account into your FinFlow wallet.
                  </p>
                </div>
              </div>
              <form onSubmit={submitWalletFunding} className="mt-4 grid gap-3 sm:grid-cols-2">
                <label className="field-label sm:col-span-2">
                  From Account
                  <select
                    required
                    className="field-control"
                    value={fundingForm.accountId}
                    onChange={(event) =>
                      setFundingForm({ ...fundingForm, accountId: event.target.value })
                    }
                  >
                    <option value="">Select source account</option>
                    {accounts.map((account) => (
                      <option key={account.id} value={account.id}>
                        {account.accountType} · •••• {account.accountNumber.slice(-4)} ·{" "}
                        {formatCurrency(account.balance)}
                      </option>
                    ))}
                  </select>
                </label>
                <label className="field-label">
                  Amount (INR)
                  <input
                    required
                    className="field-control"
                    type="number"
                    min="0.01"
                    step="0.01"
                    value={fundingForm.amount}
                    onChange={(event) =>
                      setFundingForm({ ...fundingForm, amount: event.target.value })
                    }
                    placeholder="0.00"
                  />
                </label>
                <label className="field-label">
                  Description
                  <input
                    maxLength="255"
                    className="field-control"
                    value={fundingForm.description}
                    onChange={(event) =>
                      setFundingForm({ ...fundingForm, description: event.target.value })
                    }
                    placeholder="Add funds note"
                  />
                </label>
                <button
                  className="button-primary sm:col-span-2"
                  disabled={busy === "wallet-funding" || wallet.status !== "ACTIVE"}
                >
                  {busy === "wallet-funding" ? "Adding funds…" : "Add Funds to Wallet"}
                </button>
              </form>
            </article>

            {/* Custom Payment ID Handle */}
            <article className="card">
              <div className="flex items-center gap-3">
                <span className="rounded-xl bg-brand-50 p-3 text-brand-700">
                  <QrCode size={20} />
                </span>
                <div>
                  <h2 className="section-title">FinFlow Payment Handle</h2>
                  <p className="mt-1 text-sm text-slate-500">
                    Your unique identifier for receiving payments. Custom handles are checked for uniqueness.
                  </p>
                </div>
              </div>
              <div className="mt-4 rounded-xl bg-slate-50 p-4 border border-slate-200">
                <p className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Current Payment ID</p>
                <div className="mt-1 flex items-center justify-between gap-3">
                  <span className="tabular break-all text-lg font-bold text-ink">{wallet.paymentId}</span>
                  <button
                    type="button"
                    onClick={() => handleCopyPaymentId(wallet.paymentId)}
                    className="button-secondary text-xs py-1 px-3"
                  >
                    Copy
                  </button>
                </div>
              </div>

              <form className="mt-4 flex flex-col gap-2 sm:flex-row sm:items-end" onSubmit={changeIdentity}>
                <label className="field-label flex-1">
                  Change Payment ID Handle
                  <input
                    className="field-control tabular break-all"
                    pattern="[a-z0-9._-]{3,30}@finflow"
                    required
                    placeholder="newhandle@finflow"
                    value={paymentIdInput}
                    onChange={(event) => setPaymentIdInput(event.target.value)}
                  />
                </label>
                <button className="button-secondary sm:self-end" disabled={busy === "identity"}>
                  {busy === "identity" ? "Updating…" : "Update ID"}
                </button>
              </form>
            </article>
          </section>

          {/* Recent Payments & History */}
          <section className="card">
            <div className="flex items-center justify-between gap-4 border-b border-slate-100 pb-4">
              <div>
                <h2 className="section-title">FinFlow Payment History</h2>
                <p className="mt-1 text-sm text-slate-500">
                  Recent internal payments sent and received via Payment ID and QR codes.
                </p>
              </div>
              <button type="button" onClick={() => openPayModal()} className="button-primary text-xs py-1.5 px-3">
                <Send size={14} /> Send Payment
              </button>
            </div>

            {payments.length ? (
              <div className="mt-4 divide-y divide-slate-100">
                {payments.map((payment) => {
                  const isSent = payment.senderPaymentId === wallet.paymentId;
                  return (
                    <div key={payment.id} className="flex flex-wrap items-center justify-between gap-4 py-3.5">
                      <div className="flex items-center gap-3">
                        <span
                          className={`rounded-full p-2.5 ${
                            isSent ? "bg-rose-50 text-rose-600" : "bg-emerald-50 text-emerald-600"
                          }`}
                        >
                          {isSent ? <ArrowUpRight size={18} /> : <ArrowDownToLine size={18} />}
                        </span>
                        <div>
                          <p className="text-sm font-semibold text-ink">
                            {isSent ? `To: ${payment.receiverPaymentId}` : `From: ${payment.senderPaymentId}`}
                          </p>
                          <p className="text-xs text-slate-500 mt-0.5">
                            Ref: <code className="tabular break-all text-slate-700">{payment.referenceNumber}</code> ·{" "}
                            {formatDate(payment.createdAt)}
                          </p>
                          {payment.description && (
                            <p className="text-xs text-slate-500 italic mt-0.5">"{payment.description}"</p>
                          )}
                        </div>
                      </div>

                      <div className="text-right">
                        <p className={`text-base font-bold ${isSent ? "text-rose-600" : "text-emerald-600"}`}>
                          {isSent ? "-" : "+"}{formatCurrency(payment.amount)}
                        </p>
                        <span className="inline-block rounded-full bg-slate-100 px-2 py-0.5 text-xs text-slate-600 font-medium">
                          {payment.category}
                        </span>
                      </div>
                    </div>
                  );
                })}
              </div>
            ) : (
              <div className="mt-4"><EmptyState title="No FinFlow payments yet" description="Send or receive money using your FinFlow Payment ID or QR code to see activity here." /></div>
            )}
          </section>

          {/* Wallet Ledger Activity */}
          <section className="card">
            <h2 className="section-title">Wallet Ledger Activity</h2>
            <p className="mt-1 text-sm text-slate-500">
              Detailed wallet balance changes, additions, withdrawals, and internal transfers.
            </p>
            {walletLedger.length ? (
              <div className="mt-4 divide-y divide-slate-100">
                {walletLedger.slice(0, 10).map((entry) => (
                  <div key={entry.id} className="flex flex-wrap items-center justify-between gap-3 py-3">
                    <div>
                      <p className="text-sm font-medium text-ink">
                        {humanize(entry.type)}
                        {entry.counterpartyWalletIdentifier
                          ? ` · ${entry.counterpartyWalletIdentifier}`
                          : entry.sourceAccountId
                          ? ` · Account ${entry.sourceAccountId}`
                          : ""}
                      </p>
                      <p className="mt-1 text-xs text-slate-500">
                        {entry.paymentReference} · {formatDate(entry.createdAt)} · Balance{" "}
                        {formatCurrency(entry.balanceAfter)}
                      </p>
                      {entry.description && (
                        <p className="mt-1 text-xs text-slate-500">{entry.description}</p>
                      )}
                    </div>
                    <p className="text-sm font-semibold text-ink">{formatCurrency(entry.amount)}</p>
                  </div>
                ))}
              </div>
            ) : (
              <div className="mt-4"><EmptyState title="No wallet ledger transactions yet" /></div>
            )}
          </section>
        </>
      )}

      {/* ========================================================= */}
      {/* MODAL: RECEIVE MONEY & QR CODE DISPLAY                    */}
      {/* ========================================================= */}
      {showReceiveModal && (
        <div className="modal-overlay fixed inset-0 z-50 flex items-center justify-center bg-black/45 p-4" role="dialog" aria-modal="true">
          <div className="modal-panel max-h-[90vh] w-full max-w-md overflow-y-auto rounded-xl border border-line bg-surface p-6 shadow-raised">
            <div className="flex items-center justify-between border-b border-slate-100 pb-3">
              <div className="flex items-center gap-2">
                <span className="rounded-lg bg-brand-50 p-2 text-brand-700">
                  <QrCode size={18} />
                </span>
                <h3 className="text-lg font-bold text-ink">Receive Money</h3>
              </div>
              <button
                type="button"
                onClick={() => setShowReceiveModal(false)}
                className="rounded-lg p-1 text-slate-400 hover:bg-slate-100 hover:text-slate-600"
              >
                <X size={20} />
              </button>
            </div>

            <div className="mt-6 text-center">
              <p className="text-xs font-semibold uppercase tracking-wider text-slate-400">Your FinFlow Payment ID</p>
              <div className="mt-2 inline-flex items-center gap-2 rounded-xl bg-slate-100 px-4 py-2 border border-slate-200">
                <code className="tabular break-all text-lg font-bold text-brand-700">{wallet?.paymentId}</code>
                <button
                  type="button"
                  onClick={() => handleCopyPaymentId(wallet?.paymentId)}
                  className="text-slate-500 hover:text-brand-700"
                  title="Copy Payment ID"
                >
                  {copied ? <Check size={16} className="text-emerald-600" /> : <Copy size={16} />}
                </button>
              </div>

              {/* QR Render */}
              <div className="mt-6 flex flex-col items-center justify-center">
                <div className="rounded-2xl border-2 border-brand-100 bg-white p-5 shadow-inner">
                  {qr ? (
                    <QRCodeSVG value={qr.payload} size={200} level="H" includeMargin />
                  ) : (
                    <div className="flex h-48 w-48 items-center justify-center text-slate-400">Loading QR…</div>
                  )}
                </div>
                <p className="mt-3 text-xs text-slate-500 max-w-xs">
                  Scan with any camera or FinFlow app to send money directly to this wallet.
                </p>
              </div>

              <div className="mt-6 flex gap-3">
                <button
                  type="button"
                  onClick={() => handleCopyPaymentId(wallet?.paymentId)}
                  className="button-secondary flex-1 py-2.5 text-sm"
                >
                  <Copy size={16} /> Copy Payment ID
                </button>
                <button
                  type="button"
                  onClick={handleShareQr}
                  className="button-primary flex-1 py-2.5 text-sm"
                >
                  <Share2 size={16} /> Share QR
                </button>
              </div>

              <p className="mt-4 text-xs text-slate-400">
                Anyone with a FinFlow account can pay you using this ID or QR code.
              </p>
            </div>
          </div>
        </div>
      )}

      {/* ========================================================= */}
      {/* MODAL: PAY / SEND MONEY MULTI-STEP WORKFLOW               */}
      {/* ========================================================= */}
      {showPayModal && (
        <div className="modal-overlay fixed inset-0 z-50 flex items-center justify-center bg-black/45 p-4" role="dialog" aria-modal="true">
          <div className="modal-panel max-h-[90vh] w-full max-w-lg overflow-y-auto rounded-xl border border-line bg-surface p-6 shadow-raised">
            {/* Modal Header */}
            <div className="flex items-center justify-between border-b border-slate-100 pb-3">
              <div className="flex items-center gap-2">
                <span className="rounded-lg bg-brand-50 p-2 text-brand-700">
                  <Send size={18} />
                </span>
                <h3 className="text-lg font-bold text-ink">
                  {payStep === "RESOLVE" && "Pay / Send Money"}
                  {payStep === "DETAILS" && "Enter Payment Details"}
                  {payStep === "CONFIRM" && "Confirm Payment"}
                  {payStep === "SUCCESS" && "Payment Successful"}
                </h3>
              </div>
              <button
                type="button"
                onClick={() => setShowPayModal(false)}
                className="rounded-lg p-1 text-slate-400 hover:bg-slate-100 hover:text-slate-600"
              >
                <X size={20} />
              </button>
            </div>

            <ErrorMessage message={payModalError} onDismiss={() => setPayModalError("")} />

            {/* STEP 1: RESOLVE RECIPIENT */}
            {payStep === "RESOLVE" && (
              <div className="mt-5 space-y-4">
                <p className="text-sm text-slate-500">
                  Enter the recipient's FinFlow Payment ID (<code className="text-brand-700">username@finflow</code>) or scan their FinFlow QR.
                </p>

                {/* Camera scan option */}
                <div className="rounded-xl bg-slate-50 p-3 border border-slate-200">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-semibold text-slate-600">Scan QR Code</span>
                    <button
                      type="button"
                      onClick={() => setCameraOpen((v) => !v)}
                      className="button-secondary text-xs py-1 px-2.5"
                    >
                      {cameraOpen ? "Close Camera" : "Open Camera Scanner"}
                    </button>
                  </div>
                  {cameraOpen && (
                    <div className="mt-3">
                      {cameraMessage && <p className="text-xs text-amber-700 mb-2">{cameraMessage}</p>}
                      <video
                        ref={videoRef}
                        className="max-h-48 w-full rounded-xl bg-black object-cover"
                        playsInline
                        muted
                      /><div className="relative -mt-48 h-48"><ScanFrame /></div>
                    </div>
                  )}
                </div>

                <div className="space-y-1.5">
                  <label className="field-label">Recipient Payment ID or QR payload</label>
                  <input
                    type="text"
                    className="field-control tabular break-all"
                    placeholder="rahul@finflow"
                    value={payRecipientInput}
                    onChange={(e) => setPayRecipientInput(e.target.value)}
                    onKeyDown={(e) => e.key === "Enter" && handleResolveRecipient()}
                  />
                </div>

                <div className="flex justify-end gap-3 pt-2">
                  <button
                    type="button"
                    onClick={() => setShowPayModal(false)}
                    className="button-secondary"
                  >
                    Cancel
                  </button>
                  <button
                    type="button"
                    onClick={() => handleResolveRecipient()}
                    disabled={!payRecipientInput.trim() || busy === "resolve"}
                    className="button-primary"
                  >
                    {busy === "resolve" ? <MoneyMoveLoader label="Verifying recipient" /> : "Next: Verify Recipient"}
                  </button>
                </div>
              </div>
            )}

            {/* STEP 2: PAYMENT DETAILS */}
            {payStep === "DETAILS" && resolvedRecipient && (
              <form onSubmit={handleReviewPayment} className="mt-5 space-y-4">
                {/* Resolved badge */}
                <div className="flex items-center justify-between gap-3 border-b border-line pb-4">
                  <div className="flex min-w-0 items-center gap-3">
                    <span className="grid h-9 w-9 shrink-0 place-items-center rounded-full bg-accent-soft text-accent-strong" aria-hidden="true"><CheckCircle2 size={18} /></span>
                    <div className="min-w-0">
                      <p className="truncate text-sm font-semibold text-ink">{resolvedRecipient.recipientName}</p>
                      <p className="tabular truncate text-xs text-muted">{resolvedRecipient.paymentId} · verified</p>
                    </div>
                  </div>
                  <button type="button" onClick={() => setPayStep("RESOLVE")} className="button-ghost min-h-8 px-2 text-xs">Change</button>
                </div>

                <div className="grid gap-4 sm:grid-cols-2">
                  <div className="sm:col-span-2">
                    <AmountField value={payAmount} onChange={setPayAmount} available={paySenderAccountId ? accounts.find((acc) => String(acc.id) === String(paySenderAccountId))?.balance : wallet?.balance} availableLabel="Available from this source" autoFocus />
                    <div className="mt-3 flex flex-wrap gap-2" aria-label="Quick amounts">
                      {["100", "500", "1000", "2000"].map((amt) => (
                        <button key={amt} type="button" onClick={() => setPayAmount(amt)} className="tabular rounded-full border border-line-strong px-3 py-1 text-xs font-medium text-ink-soft transition-colors duration-150 hover:bg-surface-2">{formatCurrency(amt)}</button>
                      ))}
                    </div>
                  </div>

                  <label className="field-label sm:col-span-2">
                    Payment Source
                    <select
                      className="field-control"
                      value={paySenderAccountId}
                      onChange={(e) => setPaySenderAccountId(e.target.value)}
                    >
                      <option value="">
                        FinFlow Wallet (Available: {formatCurrency(wallet?.balance || 0)})
                      </option>
                      {accounts.map((acc) => (
                        <option key={acc.id} value={acc.id}>
                          Account: {acc.accountType} (•••• {acc.accountNumber.slice(-4)}) - {formatCurrency(acc.balance)}
                        </option>
                      ))}
                    </select>
                  </label>

                  <label className="field-label">
                    Category
                    <select
                      className="field-control"
                      value={payCategory}
                      onChange={(e) => setPayCategory(e.target.value)}
                    >
                      {categories.map((cat) => (
                        <option key={cat} value={cat}>{categoryLabel(cat)}</option>
                      ))}
                    </select>
                  </label>

                  <label className="field-label">
                    Optional Note
                    <input
                      type="text"
                      maxLength="100"
                      className="field-control"
                      placeholder="e.g. Lunch, Dinner"
                      value={payDescription}
                      onChange={(e) => setPayDescription(e.target.value)}
                    />
                  </label>
                </div>

                <div className="flex justify-end gap-3 pt-2">
                  <button
                    type="button"
                    onClick={() => setPayStep("RESOLVE")}
                    className="button-secondary"
                  >
                    Back
                  </button>
                  <button type="submit" className="button-primary">
                    Review Payment
                  </button>
                </div>
              </form>
            )}

            {/* STEP 3: PAYMENT CONFIRMATION SCREEN (REQUIREMENT 11) */}
            {payStep === "CONFIRM" && resolvedRecipient && (
              <form className="mt-5 space-y-5" onSubmit={(event) => { event.preventDefault(); if (payPin.length === 6) handleConfirmPayment(); }}>
                <div>
                  <p className="eyebrow">Review before you pay</p>
                  <MoneyText value={payAmount} size="lg" className="mt-2" />
                  <p className="mt-1 text-sm text-ink-soft">to <span className="font-semibold text-ink">{resolvedRecipient.recipientName}</span> <span className="tabular text-muted">({resolvedRecipient.paymentId})</span></p>
                </div>
                <dl className="divide-y divide-line border-y border-line text-sm">
                  <div className="flex justify-between gap-4 py-2.5"><dt className="text-muted">From</dt><dd className="font-medium">{paySenderAccountId ? "Your FinFlow account" : "FinFlow wallet"}</dd></div>
                  <div className="flex justify-between gap-4 py-2.5"><dt className="text-muted">Category</dt><dd className="font-medium">{categoryLabel(payCategory)}</dd></div>
                  {payDescription && <div className="flex justify-between gap-4 py-2.5"><dt className="text-muted">Note</dt><dd className="max-w-[65%] break-words text-right font-medium">{payDescription}</dd></div>}
                  <div className="flex justify-between gap-4 py-2.5"><dt className="text-muted">Fee</dt><dd className="font-medium">No fee</dd></div>
                  <div className="flex justify-between gap-4 py-3"><dt className="text-ink-soft">Left afterwards</dt><dd className="tabular text-base font-semibold">{formatCurrency(addAmounts((paySenderAccountId ? accounts.find((acc) => String(acc.id) === String(paySenderAccountId))?.balance : wallet?.balance) ?? "0", payAmount, -1))}</dd></div>
                </dl>
                <PinInput value={payPin} onChange={(value) => { setPayPin(value); setPayModalError(""); }} autoFocus disabled={busy === "confirm-payment"} />
                <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
                  <button type="button" onClick={() => setPayStep("DETAILS")} className="button-secondary">Go back</button>
                  <button type="submit" disabled={busy === "confirm-payment" || payPin.length !== 6} className="button-primary sm:min-w-40">
                    {busy === "confirm-payment" ? <MoneyMoveLoader label="Paying" tone="on-accent" /> : `Pay ${formatCurrency(payAmount)}`}
                  </button>
                </div>
              </form>
            )}

            {/* STEP 4: SUCCESS SCREEN (REQUIREMENT 4) */}
            {payStep === "SUCCESS" && paymentResult && (
              <div className="mt-5 space-y-5 text-center">
                <SuccessCheck />

                <div>
                  <h4 className="text-2xl font-extrabold text-emerald-950">Payment Successful</h4>
                  <p className="mt-1 text-sm text-slate-500">Transaction processed atomically</p>
                </div>

                <div className="rounded-2xl bg-slate-50 p-4 border border-slate-200 text-left text-sm space-y-3">
                  <div className="flex justify-between">
                    <span className="text-slate-500">To:</span>
                    <span className="font-bold text-ink">{paymentResult.receiverPaymentId}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-500">Amount Paid:</span>
                    <span className="font-extrabold text-emerald-700 text-base">
                      {formatCurrency(paymentResult.amount)}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-500">Reference:</span>
                    <code className="tabular break-all font-bold text-slate-800">{paymentResult.referenceNumber}</code>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-500">Date/Time:</span>
                    <span className="text-slate-700">{formatDate(paymentResult.createdAt)}</span>
                  </div>
                  <div className="flex justify-between pt-2 border-t border-slate-200">
                    <span className="text-slate-500">Updated Wallet Balance:</span>
                    <span className="font-bold text-ink">{formatCurrency(wallet?.balance || 0)}</span>
                  </div>
                </div>

                <button
                  type="button"
                  onClick={() => setShowPayModal(false)}
                  className="button-primary w-full py-3"
                >
                  Done
                </button>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
