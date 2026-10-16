import { useCallback, useEffect, useRef, useState } from "react";
import { Users, Plus, ShieldCheck, Snowflake, Unlock, Trash2 } from "lucide-react";
import { getApiErrorMessage } from "../api/axios";
import { getMyAccounts } from "../api/accountApi";
import {
  addFamilyAllowance,
  createFamilyMember,
  deleteFamilyCategoryLimit,
  deleteFamilyMember,
  getFamily,
  getFamilyMembers,
  getFamilyTransactions,
  setFamilyMemberFrozen,
  spendFamilyAllowance,
  upsertFamilyCategoryLimit,
} from "../api/familyApi";
import ErrorMessage from "../components/ErrorMessage";
import EmptyState from "../components/EmptyState";
import LoadingSkeleton from "../components/LoadingSkeleton";
import PageHeader from "../components/PageHeader";
import { useToast } from "../context/ToastContext";
import { formatCurrency, formatDate } from "../utils/format";
import { PLANNING_CATEGORIES } from "../constants/categories";

const categories = PLANNING_CATEGORIES;
const key = () => window.crypto?.randomUUID?.() || `${Date.now()}-${Math.random().toString(36).slice(2)}`;

export default function Family() {
  const notify = useToast();
  const [family, setFamily] = useState(null);
  const [members, setMembers] = useState([]);
  const [accounts, setAccounts] = useState([]);
  const [transactions, setTransactions] = useState([]);
  const [selectedId, setSelectedId] = useState("");
  const [memberForm, setMemberForm] = useState({ displayName: "", monthlyAllowance: "", approvalRequired: true });
  const [allowanceForm, setAllowanceForm] = useState({ accountId: "", amount: "", description: "" });
  const [limitForm, setLimitForm] = useState({ category: "FOOD", monthlyAmount: "", pin: "" });
  const [spendForm, setSpendForm] = useState({ accountId: "", amount: "", category: "FOOD", description: "", pin: "" });
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState("");
  const [error, setError] = useState("");
  const allowanceKey = useRef(null);
  const spendKey = useRef(null);

  const load = useCallback(async () => {
    const [familyResult, memberResult, accountResult] = await Promise.allSettled([
      getFamily(), getFamilyMembers(), getMyAccounts(),
    ]);
    const failed = [familyResult, memberResult, accountResult].find((result) =>
      result.status === "rejected" && result.reason?.response?.status !== 404);
    if (failed) throw failed.reason;
    setFamily(familyResult.status === "fulfilled" ? familyResult.value.data : null);
    const nextMembers = memberResult.status === "fulfilled" ? memberResult.value.data : [];
    setMembers(nextMembers);
    setAccounts(accountResult.status === "fulfilled" ? accountResult.value.data : []);
    setSelectedId((current) => nextMembers.some((member) => String(member.id) === current)
      ? current : nextMembers.length ? String(nextMembers[0].id) : "");
  }, []);

  useEffect(() => {
    load().catch((cause) => setError(getApiErrorMessage(cause))).finally(() => setLoading(false));
  }, [load]);

  const selectedMember = members.find((member) => String(member.id) === selectedId);
  useEffect(() => {
    if (!selectedMember) {
      setTransactions([]);
      return;
    }
    getFamilyTransactions(selectedMember.id)
      .then(({ data }) => setTransactions(data))
      .catch((cause) => setError(getApiErrorMessage(cause)));
  }, [selectedMember?.id]);

  async function action(name, request, message) {
    setBusy(name);
    setError("");
    try {
      await request();
      await load();
      if (message) notify(message);
      return true;
    } catch (cause) {
      setError(getApiErrorMessage(cause));
      return false;
    } finally {
      setBusy("");
    }
  }

  async function addMember(event) {
    event.preventDefault();
    await action("member", async () => {
      const { data } = await createFamilyMember({
        ...memberForm,
        monthlyAllowance: Number(memberForm.monthlyAllowance),
      });
      setSelectedId(String(data.id));
      setMemberForm({ displayName: "", monthlyAllowance: "", approvalRequired: true });
    }, "Controlled wallet profile created.");
  }

  async function addAllowance(event) {
    event.preventDefault();
    if (!selectedMember) return;
    const request = {
      ...allowanceForm,
      accountId: Number(allowanceForm.accountId),
      amount: Number(allowanceForm.amount),
    };
    const signature = JSON.stringify(request);
    if (allowanceKey.current?.signature !== signature) {
      allowanceKey.current = { signature, value: key() };
    }
    const succeeded = await action("allowance", () => addFamilyAllowance(selectedMember.id,
      request, allowanceKey.current.value), "Allowance reserved from your account.");
    if (succeeded) {
      allowanceKey.current = null;
      setAllowanceForm((current) => ({ ...current, amount: "", description: "" }));
    }
  }

  async function addLimit(event) {
    event.preventDefault();
    if (!selectedMember) return;
    await action("limit", () => upsertFamilyCategoryLimit(selectedMember.id, {
      ...limitForm, monthlyAmount: Number(limitForm.monthlyAmount),
    }), "Category limit saved.");
    setLimitForm((current) => ({ ...current, monthlyAmount: "" }));
  }

  async function recordSpend(event) {
    event.preventDefault();
    if (!selectedMember) return;
    const approved = window.confirm(
      `Confirm this ${formatCurrency(spendForm.amount)} controlled-wallet spend for ${selectedMember.displayName}? It will debit your FinFlow account ledger; it will not make an external card or UPI payment.`,
    );
    if (!approved) return;
    const request = {
      ...spendForm,
      accountId: Number(spendForm.accountId),
      amount: Number(spendForm.amount),
      parentApproved: true,
      pin: spendForm.pin,
    };
    const signature = JSON.stringify(request);
    if (spendKey.current?.signature !== signature) {
      spendKey.current = { signature, value: key() };
    }
    const succeeded = await action("spend", () => spendFamilyAllowance(selectedMember.id,
      request, spendKey.current.value), "Controlled-wallet spend recorded in the account ledger.");
    if (succeeded) {
      spendKey.current = null;
      setSpendForm((current) => ({ ...current, amount: "", description: "", pin: "" }));
    }
  }

  if (loading) return <div className="space-y-10"><PageHeader eyebrow="Household" title="Family" /><LoadingSkeleton count={2} /></div>;
  return (
    <div className="space-y-7">
      <PageHeader eyebrow="Household" title="Family" description="Create nickname-only profiles and reserve allowance from your own FinFlow accounts. Profiles have no separate login or child contact details." />
      <ErrorMessage message={error} onDismiss={() => setError("")} />
      {family && <p className="text-xs text-slate-500">Family workspace · {family.memberCount} controlled profiles</p>}
      <section className="card">
        <div><div><h2 className="font-display text-2xl font-medium text-ink">Add a controlled profile</h2><p className="mt-1 text-sm text-slate-500">Use a nickname only; do not enter a child’s email, phone number, or other personal data.</p></div></div>
        <form onSubmit={addMember} className="mt-5 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          <label className="field-label">Child label or nickname<input required maxLength="60" className="field-control" value={memberForm.displayName} onChange={(event) => setMemberForm({ ...memberForm, displayName: event.target.value })} /></label>
          <label className="field-label">Monthly allowance limit<input required type="number" min="0.01" step="0.01" className="field-control" value={memberForm.monthlyAllowance} onChange={(event) => setMemberForm({ ...memberForm, monthlyAllowance: event.target.value })} /></label>
          <label className="flex items-center gap-3 self-end rounded-xl border border-slate-200 px-3 py-3 text-sm"><input type="checkbox" checked={memberForm.approvalRequired} onChange={(event) => setMemberForm({ ...memberForm, approvalRequired: event.target.checked })} /> Require explicit parent confirmation</label>
          <button className="button-primary self-end" disabled={busy === "member"}><Plus size={16} /> Create profile</button>
        </form>
      </section>
      {members.length === 0 ? (
        <EmptyState icon={Users} title="No family profiles yet" description="Add a nickname-only profile, then set a monthly limit and reserve an allowance for it." />
      ) : (
        <>
          <section className="flex flex-wrap items-end gap-3">
            <label className="field-label min-w-64">Selected profile<select className="field-control" value={selectedId} onChange={(event) => setSelectedId(event.target.value)}>{members.map((member) => <option key={member.id} value={member.id}>{member.displayName}</option>)}</select></label>
            {selectedMember && (
              <div className="flex items-center gap-2">
                <button type="button" className="button-secondary" disabled={busy === "freeze"} onClick={() => action("freeze", () => setFamilyMemberFrozen(selectedMember.id, selectedMember.status === "ACTIVE"), `Wallet ${selectedMember.status === "ACTIVE" ? "frozen" : "unfrozen"}.`)}>{selectedMember.status === "ACTIVE" ? <><Snowflake size={16} /> Freeze wallet</> : <><Unlock size={16} /> Unfreeze wallet</>}</button>
                <button type="button" className="button-secondary text-rose-600 border-rose-200 hover:bg-rose-50" disabled={busy === "delete-member"} onClick={() => { if (window.confirm(`Deactivate profile ${selectedMember.displayName}?`)) action("delete-member", () => deleteFamilyMember(selectedMember.id), "Controlled profile deactivated."); }}><Trash2 size={16} /> Delete profile</button>
              </div>
            )}
          </section>
          {selectedMember && <>
            <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
              {[["Reserved allowance", selectedMember.reservedBalance], ["Monthly allowance", selectedMember.monthlyAllowance], ["Spent this month", selectedMember.spentThisMonth], ["Allowance remaining", selectedMember.remainingAllowance]].map(([label, value]) => <article className="card" key={label}><p className="text-sm text-slate-500">{label}</p><p className="mt-2 text-2xl font-semibold text-ink">{formatCurrency(value)}</p></article>)}
            </section>
            <section className="grid gap-5 xl:grid-cols-2">
              <article className="card"><h2 className="section-title">Reserve allowance</h2><p className="mt-1 text-sm text-slate-500">This earmarks funds in your account. It does not transfer money to a child-owned bank account.</p><form onSubmit={addAllowance} className="mt-4 grid gap-3 sm:grid-cols-2"><label className="field-label">Fund from account<select required className="field-control" value={allowanceForm.accountId} onChange={(event) => setAllowanceForm({ ...allowanceForm, accountId: event.target.value })}><option value="">Select account</option>{accounts.map((account) => <option key={account.id} value={account.id}>{account.accountType} · •••• {account.accountNumber.slice(-4)} · {formatCurrency(account.balance)}</option>)}</select></label><label className="field-label">Amount<input required type="number" min="0.01" step="0.01" className="field-control" value={allowanceForm.amount} onChange={(event) => setAllowanceForm({ ...allowanceForm, amount: event.target.value })} /></label><label className="field-label sm:col-span-2">Description<input maxLength="255" className="field-control" value={allowanceForm.description} onChange={(event) => setAllowanceForm({ ...allowanceForm, description: event.target.value })} /></label><button className="button-primary sm:col-span-2" disabled={busy === "allowance"}><Plus size={16} /> Reserve allowance</button></form></article>
            <article className="card"><h2 className="section-title">Category controls</h2><p className="mt-1 text-sm text-slate-500">A controlled spend is rejected if it exceeds this monthly category limit.</p><form onSubmit={addLimit} className="mt-4 flex flex-wrap items-end gap-3"><label className="field-label flex-1">Category<select className="field-control" value={limitForm.category} onChange={(event) => setLimitForm({ ...limitForm, category: event.target.value })}>{categories.map((category) => <option key={category}>{category}</option>)}</select></label><label className="field-label flex-1">Monthly limit<input required type="number" min="0.01" step="0.01" className="field-control" value={limitForm.monthlyAmount} onChange={(event) => setLimitForm({ ...limitForm, monthlyAmount: event.target.value })} /></label><label className="field-label flex-1">Transaction PIN<input required type="password" inputMode="numeric" maxLength="6" className="field-control" value={limitForm.pin} onChange={(event) => setLimitForm({ ...limitForm, pin: event.target.value.replace(/\D/g, "").slice(0, 6) })} /></label><button className="button-primary" disabled={busy === "limit"}>Save limit</button></form><div className="mt-4 space-y-2">{selectedMember.categoryLimits.map((limit) => <div key={limit.id} className="flex items-center justify-between gap-3 rounded-xl bg-slate-50 px-3 py-2 text-sm"><span>{limit.category} · {formatCurrency(limit.spentThisMonth)} of {formatCurrency(limit.monthlyAmount)} · {formatCurrency(limit.remainingThisMonth)} left</span><button className="icon-action text-rose-600" aria-label={`Delete ${limit.category} limit`} onClick={() => action(`limit-${limit.id}`, () => deleteFamilyCategoryLimit(selectedMember.id, limit.id), "Category limit deleted.")}><Trash2 size={16} /></button></div>)}{!selectedMember.categoryLimits.length && <p className="text-sm text-slate-500">No category limits configured.</p>}</div></article>
            </section>
            <section className="card"><div className="flex items-start gap-3"><ShieldCheck className="mt-0.5 text-brand-700" size={20} /><div><h2 className="section-title">Record a controlled-wallet spend</h2><p className="mt-1 text-sm text-slate-500">Parent-confirmed ledger entry only; FinFlow does not process an external merchant/card/UPI payment.</p></div></div><form onSubmit={recordSpend} className="mt-4 grid gap-3 sm:grid-cols-2 xl:grid-cols-4"><label className="field-label">Account<select required className="field-control" value={spendForm.accountId} onChange={(event) => setSpendForm({ ...spendForm, accountId: event.target.value })}><option value="">Select account</option>{accounts.map((account) => <option key={account.id} value={account.id}>{account.accountType} · •••• {account.accountNumber.slice(-4)}</option>)}</select></label><label className="field-label">Amount<input required type="number" min="0.01" step="0.01" className="field-control" value={spendForm.amount} onChange={(event) => setSpendForm({ ...spendForm, amount: event.target.value })} /></label><label className="field-label">Category<select className="field-control" value={spendForm.category} onChange={(event) => setSpendForm({ ...spendForm, category: event.target.value })}>{categories.map((category) => <option key={category}>{category}</option>)}</select></label><label className="field-label">Description<input maxLength="160" className="field-control" value={spendForm.description} onChange={(event) => setSpendForm({ ...spendForm, description: event.target.value })} /></label><label className="field-label sm:col-span-2 xl:col-span-4">Transaction PIN<input required type="password" inputMode="numeric" maxLength="6" className="field-control" value={spendForm.pin} onChange={(event) => setSpendForm({ ...spendForm, pin: event.target.value.replace(/\D/g, "").slice(0, 6) })} /></label><button className="button-primary sm:col-span-2 xl:col-span-4" disabled={busy === "spend" || selectedMember.status !== "ACTIVE"}>Confirm and record spend</button></form></section>
            <section className="card"><h2 className="section-title">Controlled-wallet activity</h2>{transactions.length ? <div className="mt-4 divide-y divide-slate-100">{transactions.map((transaction) => <div key={transaction.id} className="flex flex-wrap items-center justify-between gap-3 py-3"><div><p className="text-sm font-medium">{transaction.type} · {transaction.category || "ALLOWANCE"}</p><p className="mt-1 text-xs text-slate-500">{transaction.referenceNumber || "Allowance reserve"} · {formatDate(transaction.createdAt)}</p></div><p className="text-sm font-semibold">{formatCurrency(transaction.amount)}</p></div>)}</div> : <p className="mt-4 text-sm text-slate-500">No allowance or spend history yet.</p>}</section>
          </>}
        </>
      )}
    </div>
  );
}
