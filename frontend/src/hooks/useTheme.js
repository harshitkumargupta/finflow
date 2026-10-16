import { useCallback, useEffect, useState } from "react";

const KEY = "finflow-theme";
const media = () => window.matchMedia?.("(prefers-color-scheme: dark)");

function readPreference() {
  try {
    const stored = localStorage.getItem(KEY);
    return stored === "light" || stored === "dark" ? stored : "system";
  } catch {
    return "system";
  }
}

function apply(preference) {
  const root = document.documentElement;
  if (preference === "system") delete root.dataset.theme;
  else root.dataset.theme = preference;
}

export default function useTheme() {
  const [preference, setPreferenceState] = useState(readPreference);
  const [systemDark, setSystemDark] = useState(() => Boolean(media()?.matches));

  useEffect(() => {
    const query = media();
    if (!query) return undefined;
    const update = () => setSystemDark(query.matches);
    query.addEventListener?.("change", update);
    return () => query.removeEventListener?.("change", update);
  }, []);

  useEffect(() => {
    const sync = (event) => { if (event.key === KEY) setPreferenceState(readPreference()); };
    window.addEventListener("storage", sync);
    return () => window.removeEventListener("storage", sync);
  }, []);

  const setPreference = useCallback((next) => {
    try {
      if (next === "system") localStorage.removeItem(KEY);
      else localStorage.setItem(KEY, next);
    } catch {
      return;
    } finally {
      apply(next);
      setPreferenceState(next);
    }
  }, []);

  const resolved = preference === "system" ? (systemDark ? "dark" : "light") : preference;
  return { preference, resolved, setPreference };
}
