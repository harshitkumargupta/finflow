/** @type {import('tailwindcss').Config} */
const steps = [50, 100, 200, 300, 400, 500, 600, 700, 800, 900];
const scale = (name) => Object.fromEntries([...steps.map((step) => [step, `rgb(var(--${name}-${step}) / <alpha-value>)`]), [950, `rgb(var(--${name}-900) / <alpha-value>)`]]);
const token = (name) => `rgb(var(--${name}) / <alpha-value>)`;

const neutral = scale("n");
const accent = scale("a");
const warm = scale("w");
const danger = scale("d");
const inkScale = scale("i");

export default {
  content: ["./index.html", "./src/**/*.{js,jsx}"],
  theme: {
    extend: {
      colors: {
        slate: neutral,
        gray: neutral,
        stone: neutral,
        zinc: neutral,
        neutral,
        brand: accent,
        accent: { ...accent, DEFAULT: token("accent"), strong: token("accent-strong"), soft: token("accent-soft"), on: token("on-accent") },
        emerald: accent,
        green: accent,
        teal: accent,
        amber: warm,
        orange: warm,
        yellow: warm,
        warm: { ...warm, DEFAULT: token("warm"), soft: token("warm-soft") },
        rose: danger,
        red: danger,
        danger: { ...danger, DEFAULT: token("danger"), soft: token("danger-soft") },
        blue: inkScale,
        sky: inkScale,
        indigo: inkScale,
        violet: inkScale,
        purple: inkScale,
        ink: { ...inkScale, DEFAULT: token("ink"), soft: token("ink-soft") },
        muted: token("muted"),
        paper: token("paper"),
        canvas: token("paper"),
        surface: { DEFAULT: token("surface"), 2: token("surface-2") },
        line: { DEFAULT: token("line"), strong: token("line-strong") },
      },
      fontFamily: {
        sans: ["var(--font-text)"],
        display: ["var(--font-display)"],
      },
      borderRadius: {
        sm: "var(--radius-sm)",
        md: "var(--radius-md)",
        lg: "var(--radius-lg)",
        xl: "var(--radius-md)",
        "2xl": "var(--radius-lg)",
        "3xl": "var(--radius-xl)",
      },
      boxShadow: {
        card: "var(--shadow-soft)",
        soft: "var(--shadow-soft)",
        raised: "var(--shadow-raised)",
        sm: "var(--shadow-soft)",
        md: "var(--shadow-soft)",
        lg: "var(--shadow-raised)",
        xl: "var(--shadow-raised)",
        "2xl": "var(--shadow-raised)",
      },
      transitionTimingFunction: {
        out: "var(--ease-out)",
      },
    },
  },
  plugins: [],
};
