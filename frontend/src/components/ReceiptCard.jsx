export default function ReceiptCard({ title = "Receipt", rows = [] }) {
  return (
    <div className="mt-4 rounded-md border border-line bg-surface px-4 py-3">
      <p className="eyebrow">{title}</p>
      <dl className="mt-2 divide-y divide-line">
        {rows.map(({ label, value }) => (
          <div key={label} className="flex justify-between gap-4 py-2 text-sm">
            <dt className="text-muted">{label}</dt>
            <dd className="tabular break-all text-right font-medium text-ink">{value || "—"}</dd>
          </div>
        ))}
      </dl>
    </div>
  );
}
