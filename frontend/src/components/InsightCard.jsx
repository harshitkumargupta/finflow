import { Info, Sparkles } from "lucide-react";

function Badge({ source }) {
  if (source === "AI") {
    return <span className="status-pill status-success"><Sparkles size={12} aria-hidden="true" /> AI-generated · informational</span>;
  }
  return <span className="status-pill status-neutral"><Info size={12} aria-hidden="true" /> Based on your figures</span>;
}

export default function InsightCard({ title = "Insights", icon: Icon = Sparkles, loading = false, error = "", source = "NONE", children, empty = false }) {
  return (
    <section aria-labelledby="insight-card-heading" className="rounded-xl border border-line bg-surface p-5">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div className="flex items-center gap-2.5">
          <Icon size={18} strokeWidth={1.75} className="shrink-0 text-accent-strong" aria-hidden="true" />
          <h2 id="insight-card-heading" className="text-sm font-semibold text-ink">{title}</h2>
        </div>
        {!loading && !error && !empty && <Badge source={source} />}
      </div>

      <div className="mt-3">
        {loading ? (
          <div className="space-y-2" role="status" aria-live="polite">
            <div className="h-3 w-3/4 animate-pulse rounded bg-line" />
            <div className="h-3 w-full animate-pulse rounded bg-line" />
            <div className="h-3 w-2/3 animate-pulse rounded bg-line" />
            <span className="sr-only">Loading insights…</span>
          </div>
        ) : error ? (
          <p className="text-[13px] leading-5 text-muted">Insights couldn’t be loaded right now. Your numbers elsewhere are unaffected.</p>
        ) : empty ? (
          <p className="text-[13px] leading-5 text-muted">No insights to show yet. Add a few transactions to see a summary here.</p>
        ) : (
          children
        )}
      </div>
    </section>
  );
}
