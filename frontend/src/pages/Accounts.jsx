import { useEffect, useState } from "react";
import { Building2, CheckCircle, Link2, RefreshCw, ShieldCheck, Unlink } from "lucide-react";
import {
  createAccount,
  disconnectBankLink,
  getBankLinks,
  getMyAccounts,
  initiateBankLink,
  refreshBankLinkStatus,
  verifyBankLink,
} from "../api/accountApi";
import { getApiErrorMessage } from "../api/axios";
import AccountCard from "../components/AccountCard";
import ErrorMessage from "../components/ErrorMessage";
import EmptyState from "../components/EmptyState";
import LoadingSkeleton from "../components/LoadingSkeleton";
import PinInput from "../components/PinInput";
import StatusBadge from "../components/StatusBadge";
import { humanize } from "../utils/status";
import { useAuth } from "../context/AuthContext";
import { useToast } from "../context/ToastContext";
import PageHeader from "../components/PageHeader";

export default function Accounts() {
  const { user } = useAuth();
  const notify = useToast();
  const [accounts, setAccounts] = useState([]);
  const [bankLinks, setBankLinks] = useState([]);
  const [accountSetup, setAccountSetup] = useState({
    sandboxIdentityConfirmation: "",
    transactionPin: "",
    confirmTransactionPin: "",
  });
  const [bankForm, setBankForm] = useState({
    bankName: "",
    bankCode: "",
    accountHolderName: "",
    accountNumber: "",
    accountType: "SAVINGS",
  });
  const [confirmations, setConfirmations] = useState({});
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [linkFormOpen, setLinkFormOpen] = useState(false);

  async function load() {
    const [accountsResponse, bankLinksResponse] = await Promise.all([getMyAccounts(), getBankLinks()]);
    setAccounts(accountsResponse.data);
    setBankLinks(bankLinksResponse.data);
  }
  useEffect(() => {
    load().catch((e) => setError(getApiErrorMessage(e))).finally(() => setLoading(false));
  }, []);

  async function submit(event) {
    event.preventDefault();
    setError("");
    setBusy(true);
    try {
      await createAccount(user.id, { accountType: "FINFLOW", ...accountSetup });
      await load();
      setAccountSetup({ sandboxIdentityConfirmation: "", transactionPin: "", confirmTransactionPin: "" });
      notify("FinFlow account created securely in sandbox mode.");
    } catch (e) {
      setError(getApiErrorMessage(e));
    } finally {
      setBusy(false);
    }
  }

  async function linkBank(event) {
    event.preventDefault();
    setError("");
    setBusy(true);
    try {
      await initiateBankLink(bankForm);
      await load();
      setBankForm({ bankName: "", bankCode: "", accountHolderName: "", accountNumber: "", accountType: "SAVINGS" });
      setLinkFormOpen(false);
      notify("Sandbox bank-link verification started.");
    } catch (e) {
      notify(getApiErrorMessage(e), "error");
    } finally {
      setBusy(false);
    }
  }

  async function verifyLink(linkId) {
    setBusy(true);
    try {
      await verifyBankLink(linkId, { sandboxConfirmation: confirmations[linkId] || "" });
      await load();
      notify("Bank link verification updated.");
    } catch (e) {
      notify(getApiErrorMessage(e), "error");
    } finally {
      setBusy(false);
    }
  }

  async function refreshLink(linkId) {
    setBusy(true);
    try {
      await refreshBankLinkStatus(linkId);
      await load();
      notify("Bank link status refreshed.");
    } catch (e) {
      notify(getApiErrorMessage(e), "error");
    } finally {
      setBusy(false);
    }
  }

  async function disconnectLink(linkId) {
    setBusy(true);
    try {
      await disconnectBankLink(linkId);
      await load();
      notify("Bank connection disconnected.");
    } catch (e) {
      notify(getApiErrorMessage(e), "error");
    } finally {
      setBusy(false);
    }
  }

  const header = <PageHeader eyebrow="Money" title="Accounts" description="Your FinFlow account holds your balance. Linked banks are sandbox funding sources you can add money from." />;
  if (loading) return <div className="space-y-10">{header}<LoadingSkeleton count={2} /></div>;
  const finflowAccount = accounts[0];
  const pinsMatch = accountSetup.transactionPin.length === 6 && accountSetup.transactionPin === accountSetup.confirmTransactionPin;
  return (
    <div className="space-y-12">
      {header}
      <ErrorMessage message={error} onDismiss={() => setError("")} />

      <section aria-labelledby="finflow-account-heading" className="max-w-2xl">
        <h2 id="finflow-account-heading" className="sr-only">Your FinFlow account</h2>
        {finflowAccount ? <AccountCard account={finflowAccount} /> : (
          <form onSubmit={submit} className="space-y-6 rounded-lg border border-line bg-surface p-5 sm:p-6">
            <div>
              <h3 className="font-display text-2xl font-medium text-ink">Open your FinFlow account</h3>
              <p className="mt-1 text-sm leading-6 text-ink-soft">One account per person. You’ll use a 6-digit PIN to approve payments. This is sandbox only, so there’s no real KYC, OTP or bank login.</p>
            </div>
            <PinInput label="Choose a 6-digit PIN" value={accountSetup.transactionPin} onChange={(value) => setAccountSetup({ ...accountSetup, transactionPin: value })} />
            <PinInput label="Type it again" value={accountSetup.confirmTransactionPin} onChange={(value) => setAccountSetup({ ...accountSetup, confirmTransactionPin: value })} error={accountSetup.confirmTransactionPin.length === 6 && !pinsMatch ? "These PINs don’t match." : ""} />
            <label className="field-label max-w-xs">Type CONFIRM to open a sandbox account
              <input required pattern="CONFIRM" className="field-control uppercase" value={accountSetup.sandboxIdentityConfirmation} onChange={(e) => setAccountSetup({ ...accountSetup, sandboxIdentityConfirmation: e.target.value.toUpperCase() })} placeholder="CONFIRM" />
            </label>
            <button className="button-primary" disabled={busy || !pinsMatch}>{busy ? "Opening…" : "Open account"}</button>
          </form>
        )}
      </section>

      <section aria-labelledby="links-heading" className="max-w-3xl">
        <div className="flex flex-wrap items-end justify-between gap-3">
          <div>
            <h2 id="links-heading" className="font-display text-2xl font-medium text-ink">Linked banks</h2>
            <p className="mt-1 max-w-xl text-sm leading-6 text-ink-soft">FinFlow keeps only a masked number and a provider reference. It never asks for bank passwords, PINs or OTPs.</p>
          </div>
          {!linkFormOpen && <button type="button" className="button-secondary" onClick={() => setLinkFormOpen(true)}><Link2 size={16} aria-hidden="true" /> Link a bank</button>}
        </div>

        {linkFormOpen && (
          <form onSubmit={linkBank} className="motion-content mt-5 grid gap-4 rounded-lg border border-line bg-surface p-5 sm:grid-cols-2">
            <label className="field-label">Bank name<input required maxLength="120" className="field-control" value={bankForm.bankName} onChange={(e) => setBankForm({ ...bankForm, bankName: e.target.value })} placeholder="HDFC Bank" /></label>
            <label className="field-label">Bank code<input required maxLength="40" pattern="^[A-Za-z0-9_-]{2,40}$" className="field-control uppercase" value={bankForm.bankCode} onChange={(e) => setBankForm({ ...bankForm, bankCode: e.target.value.toUpperCase() })} placeholder="HDFC" /></label>
            <label className="field-label">Account holder<input required maxLength="120" className="field-control" value={bankForm.accountHolderName} onChange={(e) => setBankForm({ ...bankForm, accountHolderName: e.target.value })} /></label>
            <label className="field-label">Account number<input required inputMode="numeric" pattern="^[0-9]{6,18}$" className="field-control tabular" value={bankForm.accountNumber} onChange={(e) => setBankForm({ ...bankForm, accountNumber: e.target.value.replace(/\D/g, "") })} placeholder="Stored masked" /></label>
            <label className="field-label">Type<select className="field-control" value={bankForm.accountType} onChange={(e) => setBankForm({ ...bankForm, accountType: e.target.value })}><option value="SAVINGS">Savings</option><option value="CURRENT">Current</option><option value="CHECKING">Checking</option></select></label>
            <div className="flex items-end gap-2 sm:col-span-2">
              <button className="button-primary" disabled={busy}>{busy ? "Starting…" : "Start sandbox link"}</button>
              <button type="button" className="button-ghost" onClick={() => setLinkFormOpen(false)}>Cancel</button>
            </div>
          </form>
        )}

        {bankLinks.length ? (
          <ul className="mt-5 divide-y divide-line border-y border-line">
            {bankLinks.map((link) => (
              <li key={link.id} className="py-4">
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div className="flex min-w-0 items-start gap-3">
                    <span className="grid h-9 w-9 shrink-0 place-items-center rounded-md bg-surface-2 text-ink-soft" aria-hidden="true"><Building2 size={16} strokeWidth={1.75} /></span>
                    <div className="min-w-0">
                      <p className="flex flex-wrap items-center gap-2 text-sm font-medium text-ink">{link.bankName} <span className="tabular text-muted">{link.maskedAccountNumber}</span> <StatusBadge status={link.status} /></p>
                      <p className="mt-0.5 text-xs text-muted">{humanize(link.accountType)} · {link.providerName}</p>
                      {link.verificationHint && link.status !== "VERIFIED" && <p className="mt-2 text-sm text-ink-soft">{link.verificationHint}</p>}
                      {link.failureReason && <p className="mt-2 text-sm text-danger">{link.failureReason}</p>}
                    </div>
                  </div>
                  <div className="flex gap-1">
                    <button className="button-ghost min-h-9 px-2.5 text-[13px] text-ink-soft" type="button" disabled={busy} onClick={() => refreshLink(link.id)}><RefreshCw size={14} aria-hidden="true" /> Refresh</button>
                    {link.status !== "DISCONNECTED" && <button className="button-ghost min-h-9 px-2.5 text-[13px] text-danger hover:bg-danger-soft" type="button" disabled={busy} onClick={() => disconnectLink(link.id)}><Unlink size={14} aria-hidden="true" /> Disconnect</button>}
                  </div>
                </div>
                {link.status !== "VERIFIED" && link.status !== "DISCONNECTED" && (
                  <div className="mt-3 flex flex-col gap-2 pl-12 sm:flex-row sm:items-end">
                    <label className="field-label flex-1">Type CONFIRM to verify this sandbox link<input className="field-control uppercase" value={confirmations[link.id] || ""} onChange={(e) => setConfirmations({ ...confirmations, [link.id]: e.target.value.toUpperCase() })} placeholder="CONFIRM" /></label>
                    <button className="button-primary" type="button" disabled={busy} onClick={() => verifyLink(link.id)}><CheckCircle size={16} aria-hidden="true" /> Verify</button>
                  </div>
                )}
              </li>
            ))}
          </ul>
        ) : !linkFormOpen && (
          <div className="mt-5"><EmptyState icon={ShieldCheck} title="No linked banks" description="Linking is optional. A sandbox link gives you another source to add money from." action={<button type="button" className="button-secondary" onClick={() => setLinkFormOpen(true)}>Link a bank</button>} /></div>
        )}
      </section>
    </div>
  );
}
