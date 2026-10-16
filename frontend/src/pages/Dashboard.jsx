import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { ArrowDownLeft, ArrowUpRight, Sparkles } from "lucide-react";
import { getBankLinks, getTransactionHistory } from "../api/accountApi";
import { getApiErrorMessage } from "../api/axios";
import { getUpcomingAutopays } from "../api/autopayApi";
import { getBudgets, getFinancialHealth, getFinancialSummary, getGoals, getIncomeSchedules, getSafeToSpend, getUpcomingBills } from "../api/financeApi";
import { getInsightSummary, getSpendingWarnings } from "../api/insightApi";
import EmptyState from "../components/EmptyState";
import ErrorMessage from "../components/ErrorMessage";
import InsightCard from "../components/InsightCard";
import OnboardingProgress from "../components/OnboardingProgress";
import ProgressBar from "../components/ProgressBar";
import SafeToSpendHero from "../components/dashboard/SafeToSpendHero";
import PlannerPanel from "../components/plan/PlannerPanel";
import UpcomingTimeline from "../components/dashboard/UpcomingTimeline";
import SpendingSummary from "../components/dashboard/SpendingSummary";
import { categoryLabel } from "../constants/categories";
import { useAccounts } from "../hooks/useFinanceData";
import { formatDateTime, maskAccountNumber } from "../utils/format";
import { formatMoney } from "../utils/money";
import { humanize } from "../utils/status";

const CREDIT_TYPES = ["DEPOSIT", "TRANSFER_IN"];
const settled = (result) => (result.status === "fulfilled" ? result.value.data : null);

export default function Dashboard() {
  const { data: accounts, loading: accountsLoading, error: accountsError } = useAccounts();
  const [safe, setSafe] = useState({ data: null, loading: true, error: "" });
  const [plan, setPlan] = useState({ loading: true, bills: [], autopays: [], incomes: [], budgets: [], goals: [], bankLinks: [], error: "" });
  const [insights, setInsights] = useState({ summary: null, health: null });
  const [activity, setActivity] = useState({ loading: true, items: [], error: "" });
  const [narration, setNarration] = useState({ loading: true, summary: null, warnings: [], error: "" });

  useEffect(() => {
    let active = true;
    Promise.allSettled([getSafeToSpend(), getUpcomingBills(), getUpcomingAutopays(), getIncomeSchedules(), getBudgets(), getGoals(), getBankLinks(), getFinancialSummary(), getFinancialHealth()])
      .then((results) => {
        if (!active) return;
        const [safeResult, billsResult, autopaysResult, incomesResult, budgetsResult, goalsResult, linksResult, summaryResult, healthResult] = results;
        setSafe({ data: settled(safeResult), loading: false, error: safeResult.status === "rejected" ? getApiErrorMessage(safeResult.reason) : "" });
        const timelineFailure = [billsResult, autopaysResult, incomesResult].find((result) => result.status === "rejected");
        setPlan({
          loading: false,
          bills: settled(billsResult) || [],
          autopays: settled(autopaysResult) || [],
          incomes: settled(incomesResult) || [],
          budgets: settled(budgetsResult) || [],
          goals: settled(goalsResult) || [],
          bankLinks: settled(linksResult) || [],
          error: timelineFailure ? `Some upcoming items couldn’t load: ${getApiErrorMessage(timelineFailure.reason)}` : "",
        });
        setInsights({ summary: settled(summaryResult), health: settled(healthResult) });
      });
    return () => { active = false; };
  }, []);

  useEffect(() => {
    if (accountsLoading) return undefined;
    let active = true;
    if (!accounts.length) { setActivity({ loading: false, items: [], error: "" }); return undefined; }
    Promise.all(accounts.map((account) => getTransactionHistory(account.id, { page: 0, size: 6 })))
      .then((responses) => {
        if (!active) return;
        const items = responses.flatMap((response) => response.data.content || []).sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt)).slice(0, 6);
        setActivity({ loading: false, items, error: "" });
      })
      .catch((cause) => { if (active) setActivity({ loading: false, items: [], error: getApiErrorMessage(cause) }); });
    return () => { active = false; };
  }, [accounts, accountsLoading]);

  useEffect(() => {
    if (accountsLoading) return undefined;
    let active = true;
    if (!accounts.length) { setNarration({ loading: false, summary: null, warnings: [], error: "" }); return undefined; }
    Promise.allSettled([getInsightSummary(), getSpendingWarnings()])
      .then(([summaryResult, warningsResult]) => {
        if (!active) return;
        setNarration({
          loading: false,
          summary: summaryResult.status === "fulfilled" ? summaryResult.value.data : null,
          warnings: warningsResult.status === "fulfilled" ? (warningsResult.value.data.warnings || []) : [],
          error: summaryResult.status === "rejected" ? getApiErrorMessage(summaryResult.reason) : "",
        });
      });
    return () => { active = false; };
  }, [accounts, accountsLoading]);

  const dateLabel = new Intl.DateTimeFormat("en-IN", { weekday: "long", day: "numeric", month: "long" }).format(new Date());
  const health = insights.health;
  const observations = insights.summary?.insights || [];

  return (
    <div className="space-y-12">
      <p className="text-sm text-muted">{dateLabel}</p>
      <ErrorMessage message={accountsError} />

      <SafeToSpendHero data={safe.data} loading={safe.loading || accountsLoading} error={safe.error} hasAccount={accounts.length > 0} />

      {!plan.loading && <OnboardingProgress accounts={accounts} bankLinks={plan.bankLinks} budgets={plan.budgets} goals={plan.goals} />}

      {accounts.length > 0 && (
        <InsightCard title="This month" loading={narration.loading} error={narration.error} source={narration.summary?.source} empty={!narration.loading && !narration.error && !narration.summary}>
          {narration.summary && (
            <div className="space-y-3">
              <p className="text-[15px] leading-6 text-ink-soft">{narration.summary.summary}</p>
              {narration.warnings.length > 0 && (
                <ul className="space-y-1.5">
                  {narration.warnings.slice(0, 2).map((warning) => (
                    <li key={warning.category} className="flex items-start gap-2 text-[13px] leading-5 text-warm"><span aria-hidden="true">⚠</span><span>{warning.message}</span></li>
                  ))}
                </ul>
              )}
              {narration.summary.suggestions?.length > 0 && (
                <ul className="space-y-1.5 border-t border-line pt-3">
                  {narration.summary.suggestions.map((suggestion, index) => (
                    <li key={index} className="flex items-start gap-2 text-[13px] leading-5 text-ink-soft"><span aria-hidden="true" className="text-accent-strong">→</span><span>{suggestion}</span></li>
                  ))}
                </ul>
              )}
            </div>
          )}
        </InsightCard>
      )}

      <div className="grid grid-cols-1 gap-12 lg:grid-cols-[minmax(0,1.6fr)_minmax(0,1fr)] lg:gap-10">
        <UpcomingTimeline bills={plan.bills} autopays={plan.autopays} incomes={plan.incomes} loading={plan.loading} error={plan.error} />

        <aside className="space-y-10">
          <section aria-labelledby="accounts-heading">
            <div className="flex items-end justify-between gap-3">
              <h2 id="accounts-heading" className="font-display text-2xl font-medium text-ink">Your accounts</h2>
              <Link to="/accounts" className="button-ghost -mr-3">All accounts</Link>
            </div>
            {accountsLoading ? <div className="mt-4 skeleton h-14 rounded-md" /> : accounts.length ? (
              <ul className="mt-3 divide-y divide-line border-y border-line">
                {accounts.map((account) => (
                  <li key={account.id}>
                    <Link to={`/accounts`} className="flex items-center justify-between gap-3 py-3 hover:bg-surface-2/60">
                      <span className="min-w-0">
                        <span className="block text-sm font-medium text-ink">{account.accountType === "FINFLOW" ? "FinFlow account" : humanize(account.accountType)}</span>
                        <span className="text-xs text-muted">{maskAccountNumber(account.accountNumber)}{account.primary ? " · Primary" : ""}</span>
                      </span>
                      <span className="tabular text-sm font-semibold text-ink">{formatMoney(account.balance)}</span>
                    </Link>
                  </li>
                ))}
              </ul>
            ) : <p className="mt-3 text-sm text-ink-soft">No account yet. <Link className="font-semibold text-accent-strong underline-offset-2 hover:underline" to="/accounts">Create one</Link> to start.</p>}
          </section>

          <section aria-labelledby="planner-heading">
            <h2 id="planner-heading" className="mb-3 font-display text-2xl font-medium text-ink">Thinking of buying something?</h2>
            <PlannerPanel initialMode="afford" />
          </section>
        </aside>
      </div>

      <div className="grid grid-cols-1 gap-12 lg:grid-cols-[minmax(0,1.6fr)_minmax(0,1fr)] lg:gap-10">
        <section aria-labelledby="activity-heading">
          <div className="flex items-end justify-between gap-3">
            <h2 id="activity-heading" className="font-display text-2xl font-medium text-ink">Recent activity</h2>
            <Link to="/transactions" className="button-ghost -mr-3">All transactions</Link>
          </div>
          {activity.loading ? (
            <div className="mt-4 space-y-2" role="status" aria-label="Loading recent activity">{[0, 1, 2].map((key) => <div key={key} className="skeleton h-12 rounded-md" />)}</div>
          ) : activity.error ? (
            <p className="mt-4 text-sm text-danger" role="alert">{activity.error}</p>
          ) : activity.items.length ? (
            <ul className="mt-3 divide-y divide-line border-y border-line">
              {activity.items.map((item) => {
                const credit = CREDIT_TYPES.includes(item.type);
                const Icon = credit ? ArrowDownLeft : ArrowUpRight;
                return (
                  <li key={item.id} className="flex items-center gap-3 py-3">
                    <span className={`grid h-8 w-8 shrink-0 place-items-center rounded-md ${credit ? "bg-accent-soft text-accent-strong" : "bg-surface-2 text-ink-soft"}`} aria-hidden="true"><Icon size={15} strokeWidth={1.75} /></span>
                    <span className="min-w-0 flex-1">
                      <span className="block truncate text-sm font-medium text-ink">{item.description || humanize(item.type)}</span>
                      <span className="text-xs text-muted">{categoryLabel(item.category)} · {formatDateTime(item.createdAt)}</span>
                    </span>
                    <span className={`tabular shrink-0 text-sm font-semibold ${credit ? "text-accent-strong" : "text-ink"}`}>{credit ? "+" : "−"}{formatMoney(item.amount)}</span>
                  </li>
                );
              })}
            </ul>
          ) : (
            <div className="mt-4"><EmptyState title="No activity yet" description="Add sandbox money or make a transfer and it will appear here." action={<Link to="/deposit" className="button-secondary">Add money</Link>} /></div>
          )}
        </section>

        <aside className="space-y-10">
          <SpendingSummary />

          <section aria-labelledby="notice-heading">
            <h2 id="notice-heading" className="flex items-center gap-2 font-display text-2xl font-medium text-ink"><Sparkles size={18} className="text-accent" aria-hidden="true" />Worth knowing</h2>
            {observations.length ? (
              <ul className="mt-3 space-y-4">
                {observations.slice(0, 3).map((item) => (
                  <li key={`${item.type}-${item.title}`} className="border-l-2 border-line pl-4">
                    <p className="text-sm font-semibold text-ink">{item.title}</p>
                    <p className="mt-0.5 text-sm leading-6 text-ink-soft">{item.description}</p>
                  </li>
                ))}
              </ul>
            ) : <p className="mt-3 text-sm text-ink-soft">Observations appear once there’s enough recorded activity to say something useful.</p>}
          </section>

          <section aria-labelledby="health-heading">
            <div className="flex items-baseline justify-between gap-3">
              <h2 id="health-heading" className="font-display text-2xl font-medium text-ink">Financial health</h2>
              {health && <p className="display-number text-2xl">{health.score}<span className="text-sm text-muted">/100</span></p>}
            </div>
            {health ? (
              <>
                <p className="mt-1 text-sm text-ink-soft">{humanize(health.rating)}. A planning indicator, not a credit score.</p>
                <ul className="mt-4 space-y-3">
                  {health.factors.map((factor) => (
                    <li key={factor.name}>
                      <div className="flex justify-between text-[13px]"><span className="font-medium text-ink-soft">{factor.name}</span><span className="tabular text-muted">{factor.score}/{factor.maximum}</span></div>
                      <div className="mt-1.5"><ProgressBar value={(100 * factor.score) / factor.maximum} label={`${factor.name} score`} /></div>
                    </li>
                  ))}
                </ul>
              </>
            ) : <p className="mt-2 text-sm text-ink-soft">Your health score is unavailable right now.</p>}
          </section>
        </aside>
      </div>
    </div>
  );
}
