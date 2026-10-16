const STAGES = [
  { key: "PENDING", label: "Pending" },
  { key: "ACTIVE", label: "Active" },
  { key: "PAUSED", label: "Paused" },
  { key: "CLOSED", label: "Cancelled" },
];

export function lifecycleStage(status) {
  if (status === "PENDING_APPROVAL") return "PENDING";
  if (status === "ACTIVE") return "ACTIVE";
  if (status === "PAUSED") return "PAUSED";
  return "CLOSED";
}

export default function LifecycleStepper({ status }) {
  const current = lifecycleStage(status);
  const index = STAGES.findIndex((stage) => stage.key === current);
  const closedLabel = status === "REJECTED" ? "Rejected" : status === "FAILED" ? "Failed" : status === "BLOCKED" ? "Blocked" : "Cancelled";
  return (
    <ol className="flex items-center gap-1.5" aria-label={`Lifecycle: ${current === "CLOSED" ? closedLabel : STAGES[index].label}`}>
      {STAGES.map((stage, position) => {
        const isCurrent = position === index;
        const passed = position < index && !(current === "ACTIVE" && stage.key === "PAUSED");
        const label = stage.key === "CLOSED" ? closedLabel : stage.label;
        return (
          <li key={stage.key} className="flex items-center gap-1.5" aria-current={isCurrent ? "step" : undefined}>
            <span className={`flex items-center gap-1.5 text-[11px] font-medium ${isCurrent ? (stage.key === "CLOSED" ? "text-muted" : stage.key === "PAUSED" ? "text-warm" : "text-accent-strong") : passed ? "text-ink-soft" : "text-muted"}`}>
              <span className={`h-2 w-2 rounded-full ${isCurrent ? (stage.key === "CLOSED" ? "bg-muted" : stage.key === "PAUSED" ? "bg-warm" : "bg-accent") : passed ? "bg-ink-soft/50" : "border border-line-strong"}`} aria-hidden="true" />
              <span className={isCurrent ? "" : "hidden sm:inline"}>{label}</span>
            </span>
            {position < STAGES.length - 1 && <span className="h-px w-3 bg-line-strong sm:w-5" aria-hidden="true" />}
          </li>
        );
      })}
    </ol>
  );
}
