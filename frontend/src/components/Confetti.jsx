export default function Confetti() {
  return <span className="pointer-events-none absolute inset-0 overflow-hidden" aria-hidden="true">{Array.from({ length: 10 }, (_, index) => <i key={index} className="confetti-piece absolute h-2 w-1.5 rounded-sm" style={{ left: `${8 + index * 9}%`, top: `${8 + (index % 3) * 8}%`, backgroundColor: ["#16856c", "#f59e0b", "#3b82f6"][index % 3], animationDelay: `${index * 35}ms` }} />)}</span>;
}
