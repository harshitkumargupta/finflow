import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { ArrowDownToLine } from "lucide-react";
import AnimatedNumber from "../AnimatedNumber";
import MoneyText from "../MoneyText";
import SafeToSpendChanges from "../SafeToSpendChanges";
import FlowBar from "./FlowBar";
import { compareAmounts, formatMoney, sumAmounts } from "../../utils/money";

const GROUPS = [
  { key: "UPCOMING_BILL", label: "Bills", swatch: "bg-ink-600", note: "Bills due in the next 30 days." },
  { key: "AUTOPAY", label: "Autopay", swatch: "bg-ink-500", note: "Active mandates scheduled to run." },
  { key: "GOAL_CONTRIBUTION", label: "Goals", swatch: "bg-ink-400", note: "Automatic contributions to your savings goals." },
  { key: "BUDGET_RESERVE", label: "Reserves", swatch: "bg-warm-400", note: "What's left in active budgets, held so you can still spend it as planned." },
];
const FREE = { key: "FREE", label: "Free to spend", swatch: "bg-accent", note: "Yours to use without touching anything already planned." };

const longDate = (value) => new Intl.DateTimeFormat("en-IN", { day: "numeric", month: "short" }).format(new Date(`${value}T00:00:00`));

export default function SafeToSpendHero({ data, loading, error, hasAccount }) {
  const [active, setActive] = useState(null);

  const model = useMemo(() => {
    if (!data) return null;
    const items = data.breakdown || [];
    const income = items.filter((item) => item.type === "EXPECTED_INCOME");
    const incomeTotal = sumAmounts(income.map((item) => item.amount));
    const groups = GROUPS.map((group) => {
      const groupItems = items.filter((item) => item.type === group.key);
      return { ...group, items: groupItems, total: sumAmounts(groupItems.map((item) => item.amount)) };
    }).filter((group) => compareAmounts(group.total, "0") > 0);
    const committed = sumAmounts(groups.map((group) => group.total));
    const available = sumAmounts([data.currentBalance, incomeTotal]);
    const short = compareAmounts(data.safeToSpend, "0") < 0;
    const free = short ? "0.00" : data.safeToSpend;
    const scale = Number(short ? committed : available) || 1;
    const segments = [...groups, ...(short ? [] : [{ ...FREE, items: [], total: free }])]
      .filter((segment) => compareAmounts(segment.total, "0") > 0)
      .map((segment) => ({ ...segment, width: Math.max(1.5, (Number(segment.total) / scale) * 100) }));
    return { income, incomeTotal, groups, committed, available, short, segments, shortBy: short ? data.safeToSpend.toString().replace("-", "") : null };
  }, [data]);

  if (loading) {
    return (
      <section aria-label="Safe to spend" role="status" className="space-y-6">
        <span className="sr-only">Loading safe to spend</span>
        <div className="skeleton h-3 w-48 rounded-full" />
        <div className="skeleton h-20 w-80 max-w-full rounded-lg" />
        <div className="skeleton h-4 w-64 rounded-full" />
        <div className="skeleton mt-6 h-3.5 w-full rounded-full" />
        <div className="flex gap-6"><div className="skeleton h-4 w-24 rounded-full" /><div className="skeleton h-4 w-24 rounded-full" /><div className="skeleton h-4 w-24 rounded-full" /></div>
      </section>
    );
  }

  if (error || !data) {
    return (
      <section aria-label="Safe to spend" className="rounded-lg border border-dashed border-line-strong p-6">
        <p className="eyebrow">Safe to spend</p>
        <p className="mt-3 font-display text-2xl text-ink">We couldn’t work this out right now.</p>
        <p className="mt-2 text-sm text-ink-soft">{error || "Planning data is unavailable."} Your balances are unaffected.</p>
        <button type="button" className="button-secondary mt-4" onClick={() => window.location.reload()}>Try again</button>
      </section>
    );
  }

  const nothingYet = !hasAccount && compareAmounts(data.currentBalance, "0") === 0 && !(data.breakdown || []).length;
  if (nothingYet) {
    return (
      <section aria-label="Safe to spend" className="space-y-4">
        <p className="eyebrow">Safe to spend</p>
        <h1 className="font-display text-[2.25rem] font-medium leading-[1.05] text-ink sm:text-5xl">Nothing to plan with yet.</h1>
        <p className="max-w-xl text-[15px] leading-6 text-ink-soft">Open your FinFlow account and add some sandbox money. You’ll then see what’s free to spend after bills, autopay, goals and budgets.</p>
        <Link to="/accounts" className="button-primary">Set up your account</Link>
      </section>
    );
  }

  const legend = [...model.segments, ...(model.short ? [{ key: "SHORT", label: "Short by", swatch: "bg-warm-400", total: model.shortBy, warm: true }] : [])];
  const activeSegment = model.segments.find((segment) => segment.key === active);
  const maxItems = Math.max(0, ...model.segments.map((segment) => segment.items?.length || 0));

  return (
    <section aria-labelledby="safe-heading" className="relative">
      <div className="flex items-baseline justify-between gap-4">
        <p className="eyebrow">Safe to spend</p>
        <p className="num text-[12px] text-muted">Next 30 days · through {longDate(data.calculatedThrough)}</p>
      </div>

      {model.short ? (
        <>
          <h1 id="safe-heading" className="mt-4 flex flex-wrap items-baseline gap-x-3">
            <MoneyText value={model.shortBy} size="hero" tone="text-warm" />
            <span className="font-display text-2xl italic text-warm sm:text-3xl">short</span>
          </h1>
          <p className="mt-4 max-w-xl text-[15px] leading-6 text-ink-soft">What’s already promised over the next 30 days is more than your balance plus expected income. Nothing is blocked — the breakdown shows where it goes, so you can decide what to move.</p>
        </>
      ) : (
        <>
          <h1 id="safe-heading" className="mt-4"><MoneyText value={data.safeToSpend} animate duration={850} size="hero" /><span className="sr-only"> free to spend</span></h1>
          <p className="mt-4 max-w-xl text-[15px] leading-6 text-ink-soft">
            free to spend after everything already promised — about <AnimatedNumber value={data.safePerDay} format={formatMoney} className="num font-semibold text-ink" /> a day.
          </p>
        </>
      )}

      <div className="mt-9">
        <FlowBar segments={model.segments} active={active} onActive={setActive} short={model.short} />

        <ul className="mt-5 flex flex-wrap gap-x-7 gap-y-3" onMouseLeave={() => setActive(null)}>
          {legend.map((segment) => {
            const dim = active && active !== segment.key;
            const selectable = segment.key !== "SHORT";
            return (
              <li key={segment.key}>
                <button
                  type="button"
                  disabled={!selectable}
                  aria-pressed={selectable ? active === segment.key : undefined}
                  onMouseEnter={() => selectable && setActive(segment.key)}
                  onFocus={() => selectable && setActive(segment.key)}
                  onClick={() => selectable && setActive(active === segment.key ? null : segment.key)}
                  className={`group flex items-baseline gap-2 rounded-sm text-left transition-opacity duration-150 ${dim ? "opacity-45" : ""} ${selectable ? "" : "cursor-default"}`}
                >
                  <span className={`h-2.5 w-2.5 translate-y-[1px] rounded-full ${segment.swatch} ${active === segment.key ? "ring-2 ring-offset-1 ring-offset-paper ring-ink/15" : ""}`} aria-hidden="true" />
                  <span className="flex flex-col leading-tight">
                    <span className={`text-[11px] font-medium uppercase tracking-[0.08em] ${segment.warm ? "text-warm" : "text-muted"} ${selectable ? "group-hover:text-ink" : ""}`}>{segment.label}</span>
                    <span className={`num text-[15px] font-semibold ${segment.warm ? "text-warm" : "text-ink"}`}>{formatMoney(segment.total)}</span>
                  </span>
                </button>
              </li>
            );
          })}
        </ul>

        <div className="mt-5 border-t border-line pt-4" style={{ minHeight: `${2 + 1.5 * maxItems}rem` }} aria-live="polite">
          {activeSegment ? (
            <div key={activeSegment.key} className="motion-content">
              <p className="text-sm text-ink-soft">{activeSegment.note}</p>
              {activeSegment.items?.length > 0 && (
                <ul className="mt-2.5 space-y-1.5">
                  {activeSegment.items.map((item, index) => (
                    <li key={`${item.description}-${index}`} className="flex max-w-md items-baseline justify-between gap-4 text-sm">
                      <span className="truncate text-ink">{item.description}</span>
                      <span className="num shrink-0 font-medium text-ink-soft">{formatMoney(item.amount)}</span>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          ) : (
            <p className="max-w-2xl text-sm leading-6 text-ink-soft">
              <span className="num font-semibold text-ink">{formatMoney(data.currentBalance)}</span> in your accounts
              {compareAmounts(model.incomeTotal, "0") > 0 && <> plus <span className="num font-semibold text-accent-strong">{formatMoney(model.incomeTotal)}</span> expected income</>}
              {compareAmounts(model.committed, "0") > 0 ? <>, with <span className="num font-semibold text-ink">{formatMoney(model.committed)}</span> already promised.</> : <>. Nothing is promised yet.</>}
              <span className="hidden text-muted sm:inline"> Hover a segment to see what’s inside.</span>
            </p>
          )}
        </div>
      </div>

      <div className="mt-8 flex flex-wrap items-center gap-2">
        <Link to="/transfer" className="button-primary">Send money</Link>
        <Link to="/wallet" className="button-secondary">Pay with wallet</Link>
        <Link to="/deposit" className="button-secondary"><ArrowDownToLine size={16} aria-hidden="true" /> Add money</Link>
        <Link to="/bills" className="button-ghost">Plan income &amp; bills</Link>
      </div>

      <SafeToSpendChanges />
    </section>
  );
}
