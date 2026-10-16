export default function Loading({ label = "Loading your FinFlow data..." }) {
  return (
    <div className="flex min-h-48 items-center justify-center gap-3 text-sm text-slate-500" role="status">
      <span className="h-5 w-5 animate-spin rounded-full border-2 border-brand-100 border-t-brand-600" aria-hidden="true" />
      {label}
    </div>
  );
}
