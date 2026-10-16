export default function PinDots({ length = 4, value = "" }) {
  return <div className="flex gap-2" aria-label={`${value.length} of ${length} PIN digits entered`}>
    {Array.from({ length }, (_, index) => <span key={index} className={`h-3 w-3 rounded-full border ${index < value.length ? "border-brand-600 bg-brand-600" : "border-slate-300 bg-surface"}`} />)}
  </div>;
}
