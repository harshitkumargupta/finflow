import { useEffect, useRef, useState } from "react";

const W = 300;
const H = 48;
const PATH = "M4 40 C 40 40, 52 12, 92 14 S 150 34, 190 22 S 250 6, 296 8";

export default function GoalPath({ percent = 0, behind = false, completed = false, label }) {
  const ref = useRef(null);
  const [length, setLength] = useState(0);
  const [drawn, setDrawn] = useState(false);
  const value = Math.max(0, Math.min(100, Number(percent) || 0));
  useEffect(() => { if (ref.current) setLength(ref.current.getTotalLength()); }, []);
  useEffect(() => { const frame = requestAnimationFrame(() => setDrawn(true)); return () => cancelAnimationFrame(frame); }, []);
  const point = ref.current && length ? ref.current.getPointAtLength((length * value) / 100) : { x: 4, y: 40 };
  const stroke = behind && !completed ? "stroke-warm-400" : "stroke-accent";
  const dot = (x, y, className, style) => <span className={`absolute h-2.5 w-2.5 -translate-x-1/2 -translate-y-1/2 rounded-full ${className}`} style={{ left: `${(x / W) * 100}%`, top: `${(y / H) * 100}%`, ...style }} aria-hidden="true" />;
  return (
    <div className="relative h-12 w-full" role="img" aria-label={label}>
      <svg viewBox={`0 0 ${W} ${H}`} preserveAspectRatio="none" className="absolute inset-0 h-full w-full overflow-visible" aria-hidden="true">
        <path d={PATH} fill="none" className="stroke-line-strong" strokeWidth="2.5" strokeLinecap="round" strokeDasharray="1 7" vectorEffect="non-scaling-stroke" />
        <path ref={ref} d={PATH} fill="none" className={`${stroke} transition-[stroke-dashoffset] duration-1000 ease-out`} strokeWidth="4" strokeLinecap="round" vectorEffect="non-scaling-stroke" pathLength="100" style={{ strokeDasharray: 100, strokeDashoffset: drawn ? 100 - value : 100 }} />
      </svg>
      {dot(296, 8, completed ? "bg-accent" : "border-2 border-line-strong bg-surface")}
      {length > 0 && value > 0 && value < 100 && dot(point.x, point.y, `${behind ? "bg-warm-400" : "bg-accent"} ring-2 ring-surface transition-opacity duration-300 ${drawn ? "opacity-100" : "opacity-0"}`, { transitionDelay: "700ms" })}
    </div>
  );
}
