import useCountUp from "../hooks/useCountUp";

export default function AnimatedNumber({ value, format = (current) => current, duration, className = "" }) {
  const display = useCountUp(value, duration);
  return <span className={className}>{format(display)}</span>;
}
