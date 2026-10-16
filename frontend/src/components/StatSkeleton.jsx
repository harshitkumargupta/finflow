export default function StatSkeleton({ count = 3 }) {
  return (
    <div className="grid gap-x-8 gap-y-6 sm:grid-cols-2 xl:grid-cols-4" role="status" aria-label="Loading">
      <span className="sr-only">Loading</span>
      {Array.from({ length: count }, (_, index) => (
        <div key={index} className="space-y-3 border-t border-line pt-4">
          <div className="skeleton h-3 w-24 rounded-full" />
          <div className="skeleton h-7 w-36 rounded-md" />
        </div>
      ))}
    </div>
  );
}
