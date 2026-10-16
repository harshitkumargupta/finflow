import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { BarChart3 } from "lucide-react";
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { getAnalyticsSummary, getCategoryAnalytics, getMonthlyAnalytics } from "../api/analyticsApi";
import { getApiErrorMessage } from "../api/axios";
import EmptyState from "../components/EmptyState";
import ErrorMessage from "../components/ErrorMessage";
import PageHeader from "../components/PageHeader";
import StatCard from "../components/StatCard";
import StatSkeleton from "../components/StatSkeleton";
import { categoryLabel } from "../constants/categories";
import useTokenColors from "../hooks/useTokenColors";
import { addAmounts, formatMoney } from "../utils/money";

const monthShort = (value) => { const [y, m] = String(value).split("-").map(Number); return new Intl.DateTimeFormat("en-IN", { month: "short" }).format(new Date(y, m - 1, 1)); };
const compact = (value) => new Intl.NumberFormat("en-IN", { notation: "compact", maximumFractionDigits: 1 }).format(value);

function ChartTooltip({ active, payload, label }) {
  if (!active || !payload?.length) return null;
  return (
    <div className="rounded-md border border-line bg-surface px-3 py-2 text-[13px] shadow-raised">
      <p className="font-semibold text-ink">{label}</p>
      {payload.map((entry) => <p key={entry.dataKey} className="tabular text-ink-soft"><span className="mr-1.5 inline-block h-2 w-2 rounded-full" style={{ background: entry.color }} />{entry.name}: <span className="font-semibold text-ink">{formatMoney(entry.value)}</span></p>)}
    </div>
  );
}

export default function Analytics() {
  const [data, setData] = useState({ summary: null, categories: [], monthly: [] });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const colors = useTokenColors(["a-600", "i-400", "line", "muted"]);

  useEffect(() => {
    Promise.all([getAnalyticsSummary(), getCategoryAnalytics(), getMonthlyAnalytics()])
      .then(([summary, categories, monthly]) => setData({ summary: summary.data, categories: categories.data, monthly: monthly.data }))
      .catch((e) => setError(getApiErrorMessage(e)))
      .finally(() => setLoading(false));
  }, []);

  const header = <PageHeader eyebrow="Understand" title="Analytics" description="Where your money came from and where it went, from successful transactions only." action={<Link to="/reports" className="button-secondary">Month-by-month report</Link>} />;
  if (loading) return <div className="space-y-10">{header}<StatSkeleton count={4} /><div className="skeleton h-72 rounded-lg" /></div>;

  const summary = data.summary;
  const monthly = [...data.monthly].sort((a, b) => a.month.localeCompare(b.month)).slice(-6).map((item) => ({ ...item, label: monthShort(item.month), income: Number(item.income) || 0, expense: Number(item.expense) || 0 }));
  const kept = summary ? addAmounts(summary.totalIncome, summary.totalExpense, -1) : null;
  const income = Number(summary?.totalIncome) || 0;
  const rate = income > 0 ? (100 * Number(kept)) / income : null;
  const current = monthly.at(-1);
  const previous = monthly.at(-2);
  const change = previous && previous.expense ? ((current.expense - previous.expense) / previous.expense) * 100 : null;
  const categories = [...data.categories].sort((a, b) => Number(b.totalAmount) - Number(a.totalAmount));
  const topShare = Math.max(1, ...categories.map((item) => Number(item.percentage) || 0));
  const hasActivity = summary && (income > 0 || Number(summary.totalExpense) > 0);

  return (
    <div className="space-y-12">
      {header}
      <ErrorMessage message={error} />
      {!hasActivity ? (
        <EmptyState icon={BarChart3} title="Nothing to analyse yet" description="Once money moves in or out of your account, you’ll see where it goes here." action={<Link to="/deposit" className="button-secondary">Add money</Link>} />
      ) : (
        <>
          <p className="max-w-3xl font-display text-[1.6rem] leading-snug text-ink">
            <span className="tabular">{formatMoney(summary.totalIncome)}</span> came in and <span className="tabular">{formatMoney(summary.totalExpense)}</span> went out, so you kept <span className={`tabular ${Number(kept) < 0 ? "text-warm" : "text-accent-strong"}`}>{formatMoney(kept)}</span>.
            {change != null && <span className="text-ink-soft"> Spending in {current.label} was {Math.abs(change).toFixed(0)}% {change > 0 ? "higher" : "lower"} than {previous.label}.</span>}
          </p>

          <section aria-label="Key figures" className="grid grid-cols-2 gap-x-6 gap-y-6 xl:grid-cols-4">
            <StatCard label="Money in" value={formatMoney(summary.totalIncome)} helper={`${formatMoney(summary.totalDeposits)} deposits · ${formatMoney(summary.totalTransfersIn)} received`} />
            <StatCard label="Money out" value={formatMoney(summary.totalExpense)} helper={`${formatMoney(summary.totalWithdrawals)} withdrawn · ${formatMoney(summary.totalTransfersOut)} sent`} />
            <StatCard label="You kept" value={formatMoney(kept)} helper="Money in minus money out" />
            <StatCard label="Savings rate" value={rate == null ? "—" : `${rate.toFixed(1)}%`} helper={`${summary.totalTransactions} transactions`} />
          </section>

          <div className="grid grid-cols-1 gap-12 lg:grid-cols-[minmax(0,1.4fr)_minmax(0,1fr)] lg:gap-10">
            <section aria-labelledby="flow-heading">
              <div className="flex flex-wrap items-end justify-between gap-3">
                <h2 id="flow-heading" className="font-display text-2xl font-medium text-ink">In and out, by month</h2>
                <p className="flex gap-4 text-[13px] text-ink-soft"><span className="flex items-center gap-1.5"><span className="h-2 w-2 rounded-full bg-accent" />In</span><span className="flex items-center gap-1.5"><span className="h-2 w-2 rounded-full bg-ink-400" />Out</span></p>
              </div>
              {monthly.length ? (
                <div className="mt-4 h-72 w-full" role="img" aria-label={`Money in and out for the last ${monthly.length} months`}>
                  <ResponsiveContainer width="100%" height="100%">
                    <BarChart data={monthly} margin={{ top: 8, right: 0, left: -12, bottom: 0 }} barGap={3}>
                      <CartesianGrid stroke={colors.line} vertical={false} />
                      <XAxis dataKey="label" axisLine={false} tickLine={false} tick={{ fill: colors.muted, fontSize: 12 }} />
                      <YAxis axisLine={false} tickLine={false} tick={{ fill: colors.muted, fontSize: 11 }} tickFormatter={(value) => `₹${compact(value)}`} width={56} />
                      <Tooltip content={<ChartTooltip />} cursor={{ fill: colors.line, opacity: 0.4 }} />
                      <Bar dataKey="income" name="In" fill={colors["a-600"]} radius={[4, 4, 0, 0]} maxBarSize={28} animationDuration={600} />
                      <Bar dataKey="expense" name="Out" fill={colors["i-400"]} radius={[4, 4, 0, 0]} maxBarSize={28} animationDuration={600} />
                    </BarChart>
                  </ResponsiveContainer>
                </div>
              ) : <p className="mt-4 text-sm text-ink-soft">No monthly activity yet.</p>}
            </section>

            <section aria-labelledby="category-heading">
              <h2 id="category-heading" className="font-display text-2xl font-medium text-ink">Where it went</h2>
              {categories.length ? (
                <ul className="mt-4 space-y-4">
                  {categories.map((item) => (
                    <li key={item.category}>
                      <div className="flex items-baseline justify-between gap-3 text-sm"><span className="font-medium text-ink">{categoryLabel(item.category)}</span><span className="tabular text-ink-soft"><span className="font-semibold text-ink">{formatMoney(item.totalAmount)}</span> · {Number(item.percentage).toFixed(0)}%</span></div>
                      <div className="mt-1.5 h-1.5 overflow-hidden rounded-full bg-surface-2"><div className="h-full rounded-full bg-ink-400 transition-[width] duration-700 ease-out" style={{ width: `${(100 * (Number(item.percentage) || 0)) / topShare}%` }} /></div>
                      <p className="mt-1 text-xs text-muted">{item.transactionCount} {item.transactionCount === 1 ? "transaction" : "transactions"}</p>
                    </li>
                  ))}
                </ul>
              ) : <p className="mt-4 text-sm text-ink-soft">No categorised spending yet.</p>}
            </section>
          </div>
        </>
      )}
    </div>
  );
}
