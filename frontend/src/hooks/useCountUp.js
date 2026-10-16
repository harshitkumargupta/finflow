import { useEffect, useRef, useState } from "react";
import useReducedMotion from "./useReducedMotion";

export default function useCountUp(value, duration = 650) {
  const reduced = useReducedMotion();
  const [display, setDisplay] = useState(value);
  const from = useRef(0);
  useEffect(() => {
    const target = Number(value);
    if (value == null || !Number.isFinite(target) || reduced) {
      setDisplay(value);
      return undefined;
    }
    const start = from.current;
    const began = performance.now();
    let frame;
    const tick = (now) => {
      const progress = Math.min(1, (now - began) / duration);
      const eased = 1 - (1 - progress) ** 3;
      if (progress < 1) {
        setDisplay((start + (target - start) * eased).toFixed(2));
        frame = requestAnimationFrame(tick);
      } else {
        from.current = target;
        setDisplay(value);
      }
    };
    frame = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(frame);
  }, [value, duration, reduced]);
  return display;
}
