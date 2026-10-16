import { useEffect, useState } from "react";

export default function FlowBar({ segments, active, onActive, short = false, className = "" }) {
  const [drawn, setDrawn] = useState(false);
  useEffect(() => {
    setDrawn(false);
    const id = requestAnimationFrame(() => requestAnimationFrame(() => setDrawn(true)));
    return () => cancelAnimationFrame(id);
  }, [segments]);

  let running = 0;
  const placed = segments.map((segment) => {
    const center = running + segment.width / 2;
    running += segment.width;
    return { ...segment, center };
  });
  const activeSeg = placed.find((segment) => segment.key === active);
  const caretLeft = activeSeg ? activeSeg.center : short ? Math.min(96, running + (100 - running) / 2) : 50;

  return (
    <div className={`relative ${className}`}>
      <div className="flowbar" data-drawn={drawn} data-dim={active ? "true" : undefined} aria-hidden="true" onMouseLeave={() => onActive(null)}>
        {placed.map((segment, index) => (
          <div key={segment.key} className="flowseg" data-active={active === segment.key || undefined} style={{ flex: `0 0 ${segment.width}%`, minWidth: "3px" }} onMouseEnter={() => onActive(segment.key)}>
            <span className={`flowseg-fill ${segment.swatch}`} style={{ transitionDelay: drawn ? `${index * 70}ms` : "0ms" }} />
          </div>
        ))}
        {short && (
          <div className="flowseg" style={{ flex: "1 1 auto", minWidth: "8%" }}>
            <span className="flowseg-fill flowseg-hatch" style={{ transitionDelay: drawn ? `${segments.length * 70}ms` : "0ms" }} />
          </div>
        )}
      </div>
      <span className="flowbar-caret" style={{ left: `${caretLeft}%`, opacity: active ? 1 : 0 }} aria-hidden="true" />
    </div>
  );
}
