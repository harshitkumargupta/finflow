import { useEffect, useState } from "react";
import { Download, Mail } from "lucide-react";
import { Link } from "react-router-dom";
import { getMonthlyAnalytics } from "../api/analyticsApi";
import { getApiErrorMessage } from "../api/axios";
import { emailMonthlyReport, getFinancialReport } from "../api/financeApi";
import { getInsightSummary } from "../api/insightApi";
import { useToast } from "../context/ToastContext";
import ErrorMessage from "../components/ErrorMessage";
import InsightCard from "../components/InsightCard";
import EmptyState from "../components/EmptyState";
import LoadingSkeleton from "../components/LoadingSkeleton";
import PageHeader from "../components/PageHeader";
import StatCard from "../components/StatCard";
import StatSkeleton from "../components/StatSkeleton";
import { categoryLabel } from "../constants/categories";
import { formatMoney } from "../utils/money";
import { formatDate } from "../utils/format";

function escapeCsv(value) {
  const text = String(value ?? "");
  return `"${text.replaceAll('"', '""')}"`;
}

export default function Reports() {
  const [report, setReport] = useState(null);
  const [monthly, setMonthly] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [narration, setNarration] = useState({ loading: true, data: null, error: "" });
  const [emailing, setEmailing] = useState(false);
  const notify = useToast();

  async function emailReport() {
    setEmailing(true);
    try {
      const { data } = await emailMonthlyReport();
      notify(data?.message || "Report emailed.", data?.delivered ? "success" : "error");
    } catch (requestError) {
      notify(getApiErrorMessage(requestError), "error");
    } finally {
      setEmailing(false);
    }
  }

  useEffect(() => {
    Promise.all([getFinancialReport(), getMonthlyAnalytics()])
      .then(([reportResponse, monthlyResponse]) => {
        setReport(reportResponse.data);
        setMonthly(monthlyResponse.data);
      })
      .catch((requestError) => setError(getApiErrorMessage(requestError)))
      .finally(() => setLoading(false));
  }, []);
  useEffect(() => {
    getInsightSummary()
      .then((response) => setNarration({ loading: false, data: response.data, error: "" }))
      .catch((requestError) => setNarration({ loading: false, data: null, error: getApiErrorMessage(requestError) }));
  }, []);

  function exportCsv() {
    if (!report) return;
    const rows = [
      ["FinFlow financial report"],
      ["Generated at", new Date().toISOString()],
      [],
      ["Summary", "Amount"],
      ["Current balance", report.summary.currentBalance],
      ["Income", report.summary.totalIncome],
      ["Expenses", report.summary.totalExpense],
      ["Deposits", report.summary.totalDeposits],
      ["Withdrawals", report.summary.totalWithdrawals],
      ["Transfers in", report.summary.totalTransfersIn],
      ["Transfers out", report.summary.totalTransfersOut],
      ["Transaction count", report.summary.totalTransactions],
      ["Upcoming bills this month", report.monthlyObligations],
      [],
      ["Monthly activity", "Income", "Expenses", "Net", "Deposits", "Withdrawals", "Transfers in", "Transfers out", "Transactions"],
      ...monthly.map((item) => [item.month, item.income, item.expense, item.net, item.deposits, item.withdrawals, item.transfersIn, item.transfersOut, item.transactionCount]),
      [],
      ["Budgets", "Limit", "Spent", "Remaining", "Percentage used"],
      ...report.budgets.map((item) => [item.category, item.budgetAmount, item.spentAmount, item.remainingAmount, item.percentageUsed]),
      [],
      ["Goals", "Target", "Saved", "Remaining", "Target date"],
      ...report.goals.map((item) => [item.name, item.targetAmount, item.currentAmount, item.remainingAmount, item.targetDate || ""]),
      [],
      ["Upcoming bills", "Amount", "Category", "Frequency", "Due date"],
      ...report.upcomingBills.map((item) => [item.name, item.amount, item.category, item.frequency, item.nextDueDate]),
    ];
    const csv = rows.map((row) => row.map(escapeCsv).join(",")).join("\r\n");
    const url = URL.createObjectURL(new Blob([csv], { type: "text/csv;charset=utf-8" }));
    const link = document.createElement("a");
    link.href = url;
    link.download = `finflow-report-${new Date().toISOString().slice(0, 10)}.csv`;
    link.click();
    URL.revokeObjectURL(url);
  }

  const header = <PageHeader eyebrow="Understand" title="Reports" description="Your money over time, in one place. Download it as a spreadsheet whenever you like." action={report && <div className="flex flex-wrap gap-2"><button type="button" className="button-secondary" onClick={emailReport} disabled={emailing}><Mail size={16} aria-hidden="true" /> {emailing ? "Emailing…" : "Email me last month"}</button><button type="button" className="button-secondary" onClick={exportCsv}><Download size={16} aria-hidden="true" /> Download CSV</button></div>} />;
  if (loading) return <div className="space-y-10">{header}<StatSkeleton count={4} /><LoadingSkeleton variant="rows" count={4} /></div>;
  const months = [...monthly].sort((a, b) => b.month.localeCompare(a.month));
  const peak = Math.max(1, ...months.map((item) => Math.max(Number(item.income) || 0, Number(item.expense) || 0)));
  const monthName = (value) => { const [y, m] = value.split("-").map(Number); return new Intl.DateTimeFormat("en-IN", { month: "long", year: "numeric" }).format(new Date(y, m - 1, 1)); };
  return (
    <div className="space-y-12">
      {header}
      <ErrorMessage message={error} />
      {!report && !error ? <EmptyState title="No report yet" description="Reports appear once there’s some activity on your account." action={<Link to="/deposit" className="button-secondary">Add money</Link>} /> : report && (
        <>
          <section aria-label="Summary" className="grid grid-cols-2 gap-x-6 gap-y-6 xl:grid-cols-4">
            <StatCard label="Balance" value={formatMoney(report.summary.currentBalance)} />
            <StatCard label="Money in" value={formatMoney(report.summary.totalIncome)} />
            <StatCard label="Money out" value={formatMoney(report.summary.totalExpense)} />
            <StatCard label="Bills this month" value={formatMoney(report.monthlyObligations)} />
          </section>

          <InsightCard title="Summary & savings ideas" loading={narration.loading} error={narration.error} source={narration.data?.source} empty={!narration.loading && !narration.error && !narration.data}>
            {narration.data && (
              <div className="space-y-3">
                <p className="text-[15px] leading-6 text-ink-soft">{narration.data.summary}</p>
                {narration.data.suggestions?.length > 0 && (
                  <ul className="space-y-1.5 border-t border-line pt-3">
                    {narration.data.suggestions.map((suggestion, index) => (
                      <li key={index} className="flex items-start gap-2 text-[13px] leading-5 text-ink-soft"><span aria-hidden="true" className="text-accent-strong">→</span><span>{suggestion}</span></li>
                    ))}
                  </ul>
                )}
              </div>
            )}
          </InsightCard>

          <section aria-labelledby="monthly-heading">
            <h2 id="monthly-heading" className="font-display text-2xl font-medium text-ink">Month by month</h2>
            {months.length ? (
              <ol className="mt-3 divide-y divide-line border-y border-line">
                {months.map((item) => {
                  const net = Number(item.net);
                  return (
                    <li key={item.month} className="grid gap-3 py-4 sm:grid-cols-[11rem_minmax(0,1fr)_auto] sm:items-center">
                      <div><p className="text-sm font-semibold text-ink">{monthName(item.month)}</p><p className="tabular text-xs text-muted">{item.transactionCount} transactions</p></div>
                      <div className="space-y-1.5" aria-hidden="true">
                        <div className="h-1.5 rounded-full bg-accent transition-[width] duration-700 ease-out" style={{ width: `${(100 * (Number(item.income) || 0)) / peak}%` }} />
                        <div className="h-1.5 rounded-full bg-ink-400 transition-[width] duration-700 ease-out" style={{ width: `${(100 * (Number(item.expense) || 0)) / peak}%` }} />
                      </div>
                      <dl className="grid grid-cols-3 gap-4 text-right text-[13px] sm:min-w-[20rem]">
                        <div><dt className="text-muted">In</dt><dd className="tabular font-semibold text-accent-strong">{formatMoney(item.income)}</dd></div>
                        <div><dt className="text-muted">Out</dt><dd className="tabular font-semibold text-ink">{formatMoney(item.expense)}</dd></div>
                        <div><dt className="text-muted">Net</dt><dd className={`tabular font-semibold ${net < 0 ? "text-warm" : "text-ink"}`}>{formatMoney(item.net)}</dd></div>
                      </dl>
                    </li>
                  );
                })}
              </ol>
            ) : <p className="mt-3 text-sm text-ink-soft">No monthly activity yet.</p>}
          </section>

          <div className="grid grid-cols-1 gap-12 lg:grid-cols-3 lg:gap-10">
            {[
              ["Budgets", report.budgets, (item) => [categoryLabel(item.category === "OVERALL" ? "Everything" : item.category), `${formatMoney(item.spentAmount)} of ${formatMoney(item.budgetAmount)}`, `${Number(item.percentageUsed).toFixed(0)}% used`], "/budgets", "No budgets yet."],
              ["Goals", report.goals, (item) => [item.name, `${formatMoney(item.currentAmount)} of ${formatMoney(item.targetAmount)}`, item.targetDate ? `by ${formatDate(item.targetDate)}` : "No deadline"], "/goals", "No goals yet."],
              ["Upcoming bills", report.upcomingBills, (item) => [item.name, formatMoney(item.amount), `due ${formatDate(item.nextDueDate)}`], "/bills", "Nothing due soon."],
            ].map(([title, list, row, href, empty]) => (
              <section key={title} aria-label={title}>
                <div className="flex items-end justify-between gap-3"><h2 className="font-display text-xl font-medium text-ink">{title}</h2><Link to={href} className="button-ghost -mr-3 text-[13px]">Open</Link></div>
                {list.length ? (
                  <ul className="mt-2 divide-y divide-line border-y border-line">
                    {list.map((item) => { const [name, amount, note] = row(item); return <li key={item.id} className="py-3"><p className="flex justify-between gap-3 text-sm"><span className="truncate font-medium text-ink">{name}</span><span className="tabular shrink-0 text-ink-soft">{amount}</span></p><p className="mt-0.5 text-xs text-muted">{note}</p></li>; })}
                  </ul>
                ) : <p className="mt-2 text-sm text-ink-soft">{empty}</p>}
              </section>
            ))}
          </div>
        </>
      )}
    </div>
  );
}
