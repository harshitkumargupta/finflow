export default function ProgressBar({ value = 0, tone = "brand", label }) {
  const width = Math.max(0, Math.min(100, Number(value) || 0));
  return (
    <div className="progress-track" role="progressbar" aria-label={label} aria-valuemin={0} aria-valuemax={100} aria-valuenow={Math.round(width)}>
      <div className={`progress-fill progress-${tone}`} style={{ width: `${width}%` }} />
    </div>
  );
}
