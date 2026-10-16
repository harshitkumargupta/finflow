import { useMemo, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { Bell, CheckCheck } from "lucide-react";
import { getApiErrorMessage } from "../api/axios";
import { getNotifications, markAllNotificationsRead, updateNotificationState } from "../api/financeApi";
import EmptyState from "../components/EmptyState";
import ErrorMessage from "../components/ErrorMessage";
import LoadingSkeleton from "../components/LoadingSkeleton";
import PageHeader from "../components/PageHeader";
import useApiResource from "../hooks/useApiResource";
import { formatDateTime } from "../utils/format";
import { humanize } from "../utils/status";

const dayKey = (value) => new Date(value).toDateString();
function dayHeading(value) {
  const date = new Date(value);
  const today = new Date();
  const yesterday = new Date(); yesterday.setDate(today.getDate() - 1);
  if (date.toDateString() === today.toDateString()) return "Today";
  if (date.toDateString() === yesterday.toDateString()) return "Yesterday";
  return new Intl.DateTimeFormat("en-IN", { weekday: "long", day: "numeric", month: "long" }).format(date);
}

export default function Notifications() {
  const { data: items, setData, loading, error: loadError, reload } = useApiResource(getNotifications, { initialData: [] });
  const [searchParams, setSearchParams] = useSearchParams();
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const view = searchParams.get("view") === "unread" ? "unread" : "all";
  const unread = items.filter((item) => !item.read).length;
  const visible = view === "unread" ? items.filter((item) => !item.read) : items;
  const groups = useMemo(() => {
    const map = new Map();
    [...visible].sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt)).forEach((item) => {
      const key = dayKey(item.createdAt);
      if (!map.has(key)) map.set(key, []);
      map.get(key).push(item);
    });
    return [...map.values()];
  }, [visible]);

  async function toggle(item) {
    setError("");
    try {
      await updateNotificationState(item.id, !item.read);
      setData((current) => current.map((entry) => (entry.id === item.id ? { ...entry, read: !item.read } : entry)));
    } catch (cause) {
      setError(getApiErrorMessage(cause));
    }
  }
  async function readAll() {
    setBusy(true);
    setError("");
    try {
      await markAllNotificationsRead();
      await reload();
    } catch (cause) {
      setError(getApiErrorMessage(cause));
    } finally {
      setBusy(false);
    }
  }
  const setView = (next) => setSearchParams(next === "all" ? {} : { view: next }, { replace: true });

  const header = <PageHeader eyebrow="Account" title="Notifications" description="Bill reminders, budget alerts, low balances and autopay results, newest first." action={unread > 0 && <button type="button" className="button-secondary" disabled={busy} onClick={readAll}><CheckCheck size={16} aria-hidden="true" /> Mark all read</button>} />;
  if (loading && !items.length) return <div className="mx-auto max-w-3xl space-y-8">{header}<LoadingSkeleton variant="rows" count={5} /></div>;

  return (
    <div className="mx-auto max-w-3xl space-y-8">
      {header}
      <ErrorMessage message={error || loadError} onDismiss={() => setError("")} />
      {items.length > 0 && (
        <div role="tablist" aria-label="Show notifications" className="inline-flex rounded-md border border-line bg-surface p-0.5">
          {[["all", `All ${items.length}`], ["unread", `Unread ${unread}`]].map(([key, label]) => (
            <button key={key} role="tab" aria-selected={view === key} onClick={() => setView(key)} className={`tabular rounded-[7px] px-3 py-1.5 text-[13px] font-medium transition-colors duration-150 ${view === key ? "bg-ink text-paper" : "text-ink-soft hover:text-ink"}`}>{label}</button>
          ))}
        </div>
      )}
      {groups.length ? (
        <div className="space-y-8">
          {groups.map((group) => (
            <section key={dayKey(group[0].createdAt)} aria-label={dayHeading(group[0].createdAt)}>
              <h2 className="border-b border-line pb-2 text-sm font-semibold text-ink">{dayHeading(group[0].createdAt)}</h2>
              <ul className="divide-y divide-line">
                {group.map((item) => (
                  <li key={item.id} className="flex items-start gap-3 py-4">
                    <span className={`mt-1.5 h-2 w-2 shrink-0 rounded-full ${item.read ? "bg-transparent ring-1 ring-line-strong" : item.severity === "WARNING" ? "bg-warm" : item.severity === "CRITICAL" ? "bg-danger" : "bg-accent"}`} aria-hidden="true" />
                    <div className="min-w-0 flex-1">
                      <p className={`text-sm ${item.read ? "font-medium text-ink-soft" : "font-semibold text-ink"}`}>{item.title}<span className="sr-only">{item.read ? ", read" : ", unread"}</span></p>
                      <p className="mt-0.5 text-sm leading-6 text-ink-soft">{item.message}</p>
                      <p className="mt-1 text-xs text-muted">{humanize(item.type)} · {formatDateTime(item.createdAt)}</p>
                    </div>
                    <button type="button" className="button-ghost min-h-8 shrink-0 px-2 text-xs" onClick={() => toggle(item)}>{item.read ? "Mark unread" : "Mark read"}</button>
                  </li>
                ))}
              </ul>
            </section>
          ))}
        </div>
      ) : (
        <EmptyState icon={Bell} title={view === "unread" ? "You’re all caught up" : "No notifications yet"} description={view === "unread" ? "Nothing new since you last looked." : "Reminders and alerts will appear here as bills come due and budgets fill up."} action={view === "unread" && items.length ? <button type="button" className="button-secondary" onClick={() => setView("all")}>Show all</button> : null} />
      )}
    </div>
  );
}
