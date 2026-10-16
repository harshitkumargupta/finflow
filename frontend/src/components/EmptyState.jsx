import { Inbox } from "lucide-react";

export default function EmptyState({ title = "Nothing here yet", description, action, icon: Icon = Inbox }) {
  return (
    <div className="empty-state animate-fade-in flex flex-col gap-4 sm:flex-row sm:items-start">
      <span className="grid h-10 w-10 shrink-0 place-items-center rounded-md bg-surface-2 text-muted" aria-hidden="true"><Icon size={18} strokeWidth={1.75} /></span>
      <div className="min-w-0 flex-1">
        <p className="font-semibold text-ink">{title}</p>
        {description && <p className="mt-1 max-w-prose text-sm leading-6 text-ink-soft">{description}</p>}
        {action && <div className="mt-4">{action}</div>}
      </div>
    </div>
  );
}
