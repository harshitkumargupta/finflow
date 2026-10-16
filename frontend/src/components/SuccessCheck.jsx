export default function SuccessCheck({ size = 44 }) {
  return (
    <span className="motion-success inline-grid place-items-center rounded-full bg-accent-soft text-accent-strong" style={{ width: size, height: size }} role="img" aria-label="Success">
      <svg viewBox="0 0 24 24" width={size * 0.55} height={size * 0.55} fill="none" aria-hidden="true"><path className="success-tick" d="m5 12.5 4.5 4.5L19 7.5" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round" /></svg>
    </span>
  );
}
