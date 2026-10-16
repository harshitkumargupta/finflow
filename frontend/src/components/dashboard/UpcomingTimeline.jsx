import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { ArrowDownLeft, CalendarClock, PiggyBank, ReceiptText, Repeat2 } from "lucide-react";
import { formatMoney, sumAmounts } from "../../utils/money";
import EmptyState from "../EmptyState";

const HORIZON = 30;
const KINDS = {
  bill: { label: "Bill", dot: "bg-ink-700", icon: ReceiptText, href: "/bills" },
  autopay: { label: "Autopay", dot: "bg-ink-400", icon: Repeat2, href: "/autopay" },
  goal: { label: "Goal contribution", dot: "bg-accent-300", icon: PiggyBank, href: "/goals" },
  income: { label: "Expected income", dot: "bg-accent", icon: ArrowDownLeft, href: "/bills" },
};

const startOfToday = () => { const now = new Date(); return new Date(now.getFullYear(), now.getMonth(), now.getDate()); };
const parseDay = (value) => { const [y, m, d] = String(value).split("-").map(Number); return new Date(y, m - 1, d); };
const dayKey = (date) => `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
const short = new Intl.DateTimeFormat("en-IN", { day: "numeric", month: "short" });
const full = new Intl.DateTimeFormat("en-IN", { weekday: "short", day: "numeric", month: "short" });

function relative(offset) {
  if (offset === 0) return "Today";
  if (offset === 1) return "Tomorrow";
  return `In ${offset} days`;
}

export default function UpcomingTimeline({ bills = [], autopays = [], incomes = [], loading, error }) {
  const [selected, setSelected] = useState(null);
  const [showAll, setShowAll] = useState(false);
  const today = startOfToday();

  const events = useMemo(() => {
    const within = (date) => { const offset = Math.round((date - today) / 86400000); return offset >= 0 && offset <= HORIZON ? offset : null; };
    const list = [];
    bills.filter((bill) => bill.active && bill.nextDueDate).forEach((bill) => {
      const date = parseDay(bill.nextDueDate); const offset = within(date);
      if (offset != null) list.push({ id: `bill-${bill.id}`, kind: "bill", title: bill.name, amount: bill.amount, date, offset });
    });
    autopays.filter((item) => item.status === "ACTIVE" && item.nextPaymentDate).forEach((item) => {
      const date = parseDay(item.nextPaymentDate); const offset = within(date);
      if (offset != null) list.push({ id: `autopay-${item.id}`, kind: item.savingsGoalId ? "goal" : "autopay", title: item.savingsGoalId ? `Into ${item.savingsGoalName || "a goal"}` : item.merchantName, amount: item.amount, date, offset });
    });
    incomes.filter((income) => income.active).forEach((income) => {
      (income.expectedDatesWithinHorizon || []).forEach((day) => {
        const date = parseDay(day); const offset = within(date);
        if (offset != null) list.push({ id: `income-${income.id}-${day}`, kind: "income", title: income.sourceName, amount: income.amount, date, offset });
      });
    });
    return list.sort((a, b) => a.offset - b.offset || a.title.localeCompare(b.title));
  }, [bills, autopays, incomes, today.getTime()]);

  const byDay = useMemo(() => {
    const map = new Map();
    events.forEach((event) => { const key = dayKey(event.date); if (!map.has(key)) map.set(key, []); map.get(key).push(event); });
    return map;
  }, [events]);

  const outgoing = sumAmounts(events.filter((event) => event.kind !== "income").map((event) => event.amount));
  const incoming = sumAmounts(events.filter((event) => event.kind === "income").map((event) => event.amount));
  const groups = [...byDay.entries()];
  const visibleGroups = showAll ? groups : groups.slice(0, 6);

  return (
    <section aria-labelledby="timeline-heading">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h2 id="timeline-heading" className="font-display text-2xl font-medium text-ink">Next 30 days</h2>
          {!loading && !error && events.length > 0 && (
            <p className="mt-1 text-sm text-ink-soft"><span className="tabular font-semibold text-ink">{formatMoney(outgoing)}</span> going out{incoming !== "0.00" && <>, <span className="tabular font-semibold text-accent-strong">{formatMoney(incoming)}</span> coming in</>}.</p>
          )}
        </div>
        <Link to="/bills" className="button-ghost -mr-3">Manage</Link>
      </div>

      {loading ? (
        <div className="mt-5 space-y-4" role="status" aria-label="Loading upcoming payments"><div className="skeleton h-10 w-full rounded-md" /><div className="skeleton h-12 w-full rounded-md" /><div className="skeleton h-12 w-full rounded-md" /></div>
      ) : error ? (
        <p className="mt-5 text-sm text-danger" role="alert">{error}</p>
      ) : !events.length ? (
        <div className="mt-5"><EmptyState icon={CalendarClock} title="Nothing scheduled in the next 30 days" description="Add bills, autopay or expected income and they’ll line up here, so you can see when money moves." action={<div className="flex flex-wrap gap-2"><Link to="/bills" className="button-secondary">Add a bill</Link><Link to="/autopay" className="button-ghost">Set up autopay</Link></div>} /></div>
      ) : (
        <>
          <div className="mt-5 grid grid-cols-[repeat(31,minmax(0,1fr))] items-end gap-px" role="group" aria-label="Thirty day calendar strip">
            {Array.from({ length: HORIZON + 1 }, (_, offset) => {
              const date = new Date(today.getFullYear(), today.getMonth(), today.getDate() + offset);
              const key = dayKey(date);
              const dayEvents = byDay.get(key) || [];
              const label = offset % 7 === 0;
              const marker = (
                <>
                  <span className="flex h-7 flex-col-reverse items-center gap-0.5">
                    {dayEvents.slice(0, 3).map((event) => <span key={event.id} className={`h-1.5 w-1.5 rounded-full ${KINDS[event.kind].dot}`} />)}
                  </span>
                  <span className={`mt-1 block h-2 w-px ${offset === 0 ? "bg-accent" : label ? "bg-line-strong" : "bg-line"}`} />
                </>
              );
              return (
                <div key={key} className="relative flex flex-col items-center">
                  {dayEvents.length ? (
                    <button type="button" onClick={() => setSelected(selected === key ? null : key)} aria-pressed={selected === key} aria-label={`${full.format(date)}: ${dayEvents.length} ${dayEvents.length === 1 ? "item" : "items"}`} className={`flex w-full flex-col items-center rounded-sm transition-colors duration-150 hover:bg-surface-2 ${selected === key ? "bg-accent-soft" : ""}`}>{marker}</button>
                  ) : <div className="flex w-full flex-col items-center" aria-hidden="true">{marker}</div>}
                  <span className={`mt-1 h-4 whitespace-nowrap text-[10px] ${offset === 0 ? "font-semibold text-accent-strong" : "text-muted"}`} aria-hidden="true">{label ? (offset === 0 ? "Today" : short.format(date)) : ""}</span>
                </div>
              );
            })}
          </div>

          <ol className="mt-6 divide-y divide-line border-y border-line">
            {visibleGroups.map(([key, dayEvents]) => (
              <li key={key} className={`grid gap-1 py-3 transition-colors duration-200 sm:grid-cols-[9rem_1fr] sm:gap-4 ${selected && selected !== key ? "opacity-45" : ""}`}>
                <div className="pt-0.5">
                  <p className="text-sm font-semibold text-ink">{relative(dayEvents[0].offset)}</p>
                  <p className="text-xs text-muted">{full.format(dayEvents[0].date)}</p>
                </div>
                <ul className="space-y-2">
                  {dayEvents.map((event) => {
                    const kind = KINDS[event.kind];
                    return (
                      <li key={event.id}>
                        <Link to={kind.href} className="group flex items-center gap-3 rounded-md py-1 hover:bg-surface-2/60">
                          <span className="grid h-8 w-8 shrink-0 place-items-center rounded-md bg-surface-2 text-ink-soft" aria-hidden="true"><kind.icon size={15} strokeWidth={1.75} /></span>
                          <span className="min-w-0 flex-1">
                            <span className="block truncate text-sm font-medium text-ink group-hover:underline">{event.title}</span>
                            <span className="flex items-center gap-1.5 text-xs text-muted"><span className={`h-1.5 w-1.5 rounded-full ${kind.dot}`} aria-hidden="true" />{kind.label}</span>
                          </span>
                          <span className={`tabular shrink-0 text-sm font-semibold ${event.kind === "income" ? "text-accent-strong" : "text-ink"}`}>{event.kind === "income" ? "+" : "−"}{formatMoney(event.amount)}</span>
                        </Link>
                      </li>
                    );
                  })}
                </ul>
              </li>
            ))}
          </ol>
          {groups.length > 6 && <button type="button" className="button-ghost -ml-3 mt-2" onClick={() => setShowAll(!showAll)}>{showAll ? "Show fewer days" : `Show all ${groups.length} days`}</button>}
        </>
      )}
    </section>
  );
}
