export default function LoadingSkeleton({ variant = "cards", count = 3 }) {
  if (variant === "rows") {
    return (
      <div className="divide-y divide-line" role="status" aria-label="Loading">
        <span className="sr-only">Loading</span>
        {Array.from({ length: count }, (_, index) => (
          <div key={index} className="flex items-center gap-4 py-4">
            <div className="skeleton h-9 w-9 shrink-0 rounded-md" />
            <div className="flex-1 space-y-2"><div className="skeleton h-3 w-2/5 rounded-full" /><div className="skeleton h-3 w-1/4 rounded-full" /></div>
            <div className="skeleton h-4 w-20 rounded-full" />
          </div>
        ))}
      </div>
    );
  }
  return (
    <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3" role="status" aria-label="Loading">
      <span className="sr-only">Loading</span>
      {Array.from({ length: count }, (_, index) => (
        <div key={index} className="panel space-y-3 p-5">
          <div className="skeleton h-3 w-24 rounded-full" />
          <div className="skeleton h-7 w-36 rounded-md" />
          <div className="skeleton h-3 w-3/5 rounded-full" />
        </div>
      ))}
    </div>
  );
}
