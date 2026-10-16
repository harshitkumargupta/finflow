export default function StatCard({ label, value, helper, icon: Icon }) {
  return (
    <article className="flex min-w-0 flex-col gap-2 border-t border-line pt-4">
      <p className="flex items-center gap-2 text-[13px] font-medium text-muted">
        {Icon && <Icon size={15} strokeWidth={1.75} aria-hidden="true" />}
        {label}
      </p>
      <p className="display-number truncate text-[1.75rem] leading-none">{value}</p>
      {helper && <p className="text-xs leading-5 text-muted">{helper}</p>}
    </article>
  );
}
