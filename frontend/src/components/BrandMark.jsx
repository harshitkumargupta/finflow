export default function BrandMark({ size = 28 }) {
  return (
    <svg width={size} height={size} viewBox="0 0 32 32" aria-hidden="true" className="shrink-0">
      <rect width="32" height="32" rx="9" className="fill-accent" />
      <path d="M7 19c3.2 0 4.4-6 8.2-6s4.6 6 9.8 6" fill="none" className="stroke-accent-on" strokeWidth="2.4" strokeLinecap="round" />
      <path d="M7 24h18" fill="none" className="stroke-accent-on" strokeOpacity=".45" strokeWidth="2.4" strokeLinecap="round" />
    </svg>
  );
}
