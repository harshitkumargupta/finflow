import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { Bell, LogOut, ShieldCheck } from "lucide-react";
import { getMyAccounts } from "../api/accountApi";
import { getApiErrorMessage } from "../api/axios";
import { getSecurityActivity } from "../api/financeApi";
import AccountCard from "../components/AccountCard";
import EmptyState from "../components/EmptyState";
import ErrorMessage from "../components/ErrorMessage";
import LoadingSkeleton from "../components/LoadingSkeleton";
import PageHeader from "../components/PageHeader";
import PhoneVerifyCard from "../components/PhoneVerifyCard";
import StatusBadge from "../components/StatusBadge";
import { useAuth } from "../context/AuthContext";
import useTheme from "../hooks/useTheme";
import { formatDate, formatDateTime } from "../utils/format";
import { humanize } from "../utils/status";

export default function Profile() {
  const { user, logout } = useAuth();
  const [accounts, setAccounts] = useState([]);
  const [activities, setActivities] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [showAll, setShowAll] = useState(false);
  const { preference, setPreference } = useTheme();

  useEffect(() => {
    Promise.allSettled([getMyAccounts(), getSecurityActivity()]).then(([accountsResult, activityResult]) => {
      if (accountsResult.status === "fulfilled") setAccounts(accountsResult.value.data);
      if (activityResult.status === "fulfilled") setActivities(activityResult.value.data || []);
      const failed = [accountsResult, activityResult].find((result) => result.status === "rejected");
      if (failed) setError(getApiErrorMessage(failed.reason));
      setLoading(false);
    });
  }, []);

  const details = [
    ["Name", user?.name],
    ["Email", user?.email],
    ["Mobile", user?.phone],
    ["Member since", formatDate(user?.createdAt)],
  ];
  const visible = showAll ? activities : activities.slice(0, 8);

  return (
    <div className="mx-auto max-w-3xl space-y-12">
      <PageHeader eyebrow="Account" title={user?.name || "Profile"} description="Your details, your account, and a record of sign-ins and sensitive actions." action={<button type="button" className="button-secondary" onClick={() => logout()}><LogOut size={16} aria-hidden="true" /> Sign out</button>} />
      <ErrorMessage message={error} onDismiss={() => setError("")} />

      <section aria-labelledby="details-heading">
        <h2 id="details-heading" className="font-display text-2xl font-medium text-ink">Your details</h2>
        <dl className="mt-3 divide-y divide-line border-y border-line">
          {details.map(([label, value]) => (
            <div key={label} className="grid gap-1 py-3 sm:grid-cols-[10rem_1fr]">
              <dt className="text-sm text-muted">{label}</dt>
              <dd className="break-words text-sm font-medium text-ink">{value || "—"}</dd>
            </div>
          ))}
        </dl>
        <p className="mt-3 flex items-start gap-2 text-[13px] leading-5 text-muted"><ShieldCheck size={15} className="mt-0.5 shrink-0" aria-hidden="true" />These can’t be edited in the sandbox. FinFlow never stores bank passwords, OTPs or card CVVs.</p>
        <div className="mt-6"><PhoneVerifyCard /></div>
      </section>

      <section aria-labelledby="appearance-heading">
        <h2 id="appearance-heading" className="font-display text-2xl font-medium text-ink">Appearance</h2>
        <p className="mt-1 text-sm text-ink-soft">“System” follows your device’s light or dark setting.</p>
        <div role="radiogroup" aria-labelledby="appearance-heading" className="mt-3 inline-flex rounded-md border border-line bg-surface p-0.5">
          {[["system", "System"], ["light", "Light"], ["dark", "Dark"]].map(([value, label]) => (
            <button key={value} type="button" role="radio" aria-checked={preference === value} onClick={() => setPreference(value)} className={`rounded-[7px] px-3 py-1.5 text-[13px] font-medium transition-colors duration-150 ${preference === value ? "bg-ink text-paper" : "text-ink-soft hover:text-ink"}`}>{label}</button>
          ))}
        </div>
      </section>

      <section aria-labelledby="profile-accounts-heading">
        <div className="flex items-end justify-between gap-3">
          <h2 id="profile-accounts-heading" className="font-display text-2xl font-medium text-ink">Your account</h2>
          <Link to="/notifications" className="button-ghost -mr-3"><Bell size={15} aria-hidden="true" /> Notifications</Link>
        </div>
        <div className="mt-3">
          {loading ? <LoadingSkeleton count={1} /> : accounts.length ? <div className="space-y-4">{accounts.map((account) => <AccountCard key={account.id} account={account} />)}</div> : <EmptyState title="No account yet" description="Open your FinFlow account to start adding money and making payments." action={<Link to="/accounts" className="button-primary">Open account</Link>} />}
        </div>
      </section>

      <section aria-labelledby="security-heading">
        <h2 id="security-heading" className="font-display text-2xl font-medium text-ink">Security activity</h2>
        <p className="mt-1 text-sm text-ink-soft">Sign-ins, PIN checks and other sensitive actions on your account.</p>
        {loading ? <div className="mt-4"><LoadingSkeleton variant="rows" count={3} /></div> : activities.length ? (
          <>
            <ol className="mt-4 divide-y divide-line border-y border-line">
              {visible.map((item) => (
                <li key={item.id} className="flex flex-wrap items-start justify-between gap-x-4 gap-y-1 py-3">
                  <div className="min-w-0">
                    <p className="flex flex-wrap items-center gap-2 text-sm font-medium text-ink">{humanize(item.action)} <StatusBadge status={item.status} />{item.riskLevel && item.riskLevel !== "LOW" && <span className="text-xs font-medium text-warm">{humanize(item.riskLevel)} risk</span>}</p>
                    {(item.details || item.resource) && <p className="mt-0.5 text-[13px] leading-5 text-ink-soft">{item.details || item.resource}</p>}
                  </div>
                  <time className="tabular shrink-0 text-xs text-muted" dateTime={item.createdAt}>{formatDateTime(item.createdAt)}</time>
                </li>
              ))}
            </ol>
            {activities.length > 8 && <button type="button" className="button-ghost -ml-3 mt-2" onClick={() => setShowAll(!showAll)}>{showAll ? "Show less" : `Show all ${activities.length}`}</button>}
          </>
        ) : <p className="mt-4 text-sm text-ink-soft">Nothing recorded yet.</p>}
      </section>
    </div>
  );
}
