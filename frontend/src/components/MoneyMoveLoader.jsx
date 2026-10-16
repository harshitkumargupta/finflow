export default function MoneyMoveLoader({ label = "Processing securely", tone }) {
  const dot = tone === "on-accent" ? "bg-accent-on" : "bg-accent";
  return (
    <span className={`inline-flex items-center gap-2.5 text-sm ${tone === "on-accent" ? "text-accent-on" : "text-ink-soft"}`} role="status">
      <span className="flex gap-1" aria-hidden="true">{[0, 1, 2].map((key) => <i key={key} className={`money-loader-dot h-1.5 w-1.5 rounded-full ${dot}`} />)}</span>
      {label}
    </span>
  );
}
