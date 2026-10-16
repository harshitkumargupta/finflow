import { useEffect, useState } from "react";

const read = (names) => {
  if (typeof window === "undefined") return {};
  const styles = getComputedStyle(document.documentElement);
  return Object.fromEntries(names.map((name) => [name, `rgb(${styles.getPropertyValue(`--${name}`).trim().split(/\s+/).join(", ")})`]));
};

export default function useTokenColors(names) {
  const key = names.join("|");
  const [colors, setColors] = useState(() => read(names));
  useEffect(() => {
    setColors(read(names));
    const observer = new MutationObserver(() => setColors(read(names)));
    observer.observe(document.documentElement, { attributes: true, attributeFilter: ["data-theme"] });
    return () => observer.disconnect();
  }, [key]);
  return colors;
}
