import { Link } from "react-router-dom";
import { getAnalyticsSummary, getCategoryAnalytics, getMonthlyAnalytics } from "../../api/analyticsApi";
import useApiResource from "../../hooks/useApiResource";
import { categoryLabel } from "../../constants/categories";
import { compareAmounts, formatMoney } from "../../utils/money";
import MoneyText from "../MoneyText";

const monthKey = (date) => `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}`;
const monthName = (date) => new Intl.DateTimeFormat("en-IN", { month: "long" }).format(date);

async function fetchSpending() {
  const [summary, monthly, categories] = await Promise.all([getAnalyticsSummary(), getMonthlyAnalytics(), getCategoryAnalytics()]);
  return { data: { summary: summary.data, monthly: monthly.data, categories: categories.data } };
}

export default function SpendingSummary() {
  const { data, loading, error, reload } = useApiResource(fetchSpending);
  const now = new Date();
  const lastMonthDate = new Date(now.getFullYear(), now.getMonth() - 1, 1);

  const header = (
    <div className="flex items-end justify-between gap-3">
      <h2 id="spending-heading" className="font-display text-2xl font-medium text-ink">Total spent</h2>
      <Link to="/analytics" className="button-ghost -mr-3">Analytics</Link>
    </div>
  );

  if (loading && !data) {
    return (
      <section aria-labelledby="spending-heading">
        {header}
        <div className="mt-4 space-y-3" role="status" aria-label="Loading spending"><div className="skeleton h-9 w-40 rounded-md" /><div className="skeleton h-4 w-full rounded-full" /><div className="skeleton h-4 w-3/4 rounded-full" /></div>
      </section>
    );
  }

  if (error && !data) {
    return (
      <section aria-labelledby="spending-heading">
        {header}
        <p className="mt-3 text-sm text-ink-soft">Spending figures couldn’t load right now.</p>
        <button type="button" className="button-secondary mt-3" onClick={() => reload().catch(() => {})}>Try again</button>
      </section>
    );
  }

  const monthly = data?.monthly || [];
  const expenseFor = (key) => monthly.find((item) => item.month === key)?.expense ?? "0.00";
  const thisMonth = expenseFor(monthKey(now));
  const lastMonth = expenseFor(monthKey(lastMonthDate));
  const allTime = data?.summary?.totalExpense ?? "0.00";
  const top = (data?.categories || []).slice(0, 4);
  const peak = Math.max(Number(thisMonth) || 0, Number(lastMonth) || 0, 1);

  if (compareAmounts(allTime, "0") === 0) {
    return (
      <section aria-labelledby="spending-heading">
        {header}
        <p className="mt-3 text-sm leading-6 text-ink-soft">Nothing spent yet. Withdrawals and money you send show up here.</p>
      </section>
    );
  }

  return (
    <section aria-labelledby="spending-heading">
      {header}
      <p className="mt-3 text-[13px] text-muted">{monthName(now)} so far</p>
      <MoneyText value={thisMonth} size="lg" animate className="mt-1" />

      <ul className="mt-5 space-y-3">
        {[[`This month`, thisMonth, "bg-ink-500"], [monthName(lastMonthDate), lastMonth, "bg-ink-300"]].map(([label, value, swatch]) => (
          <li key={label}>
            <div className="flex items-baseline justify-between gap-3 text-sm"><span className="text-ink-soft">{label}</span><span className="num font-semibold text-ink">{formatMoney(value)}</span></div>
            <div className="mt-1.5 h-1.5 overflow-hidden rounded-full bg-surface-2" aria-hidden="true"><div className={`h-full rounded-full ${swatch} transition-[width] duration-700 ease-out`} style={{ width: `${(100 * (Number(value) || 0)) / peak}%` }} /></div>
          </li>
        ))}
        <li className="flex items-baseline justify-between gap-3 border-t border-line pt-3 text-sm"><span className="text-ink-soft">All time</span><span className="num font-semibold text-ink">{formatMoney(allTime)}</span></li>
      </ul>

      {top.length > 0 && (
        <>
          <h3 className="mt-6 text-[13px] font-medium text-muted">Top categories, all time</h3>
          <ul className="mt-2 divide-y divide-line border-y border-line">
            {top.map((item) => (
              <li key={item.category} className="flex items-baseline justify-between gap-3 py-2.5 text-sm">
                <span className="truncate text-ink">{categoryLabel(item.category)}</span>
                <span className="num shrink-0 text-ink-soft"><span className="font-semibold text-ink">{formatMoney(item.totalAmount)}</span> · {Number(item.percentage).toFixed(0)}%</span>
              </li>
            ))}
          </ul>
        </>
      )}
    </section>
  );
}
