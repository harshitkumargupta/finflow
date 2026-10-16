const DECIMAL = /^[-+]?\d+(\.\d+)?$/;

const formatter = new Intl.NumberFormat("en-IN", {
  style: "currency",
  currency: "INR",
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

export function toDecimalString(value) {
  if (value == null || value === "") return null;
  if (typeof value === "number") return Number.isFinite(value) ? String(value) : null;
  const text = String(value).trim().replaceAll(",", "");
  return DECIMAL.test(text) ? text : null;
}

export function formatMoney(value, { signed = false } = {}) {
  const decimal = toDecimalString(value);
  if (decimal == null) return "—";
  const text = formatter.format(decimal);
  if (!signed || decimal.startsWith("-") || /^[+]?0+(\.0+)?$/.test(decimal)) return text;
  return `+${text}`;
}

export function moneyParts(value) {
  const decimal = toDecimalString(value);
  if (decimal == null) return null;
  const parts = formatter.formatToParts(decimal);
  const pick = (type) => parts.filter((part) => part.type === type).map((part) => part.value).join("");
  const whole = parts.filter((part) => part.type === "integer" || part.type === "group").map((part) => part.value).join("");
  return {
    negative: parts.some((part) => part.type === "minusSign"),
    symbol: pick("currency"),
    whole,
    fraction: pick("fraction"),
  };
}

export function isNegative(value) {
  const decimal = toDecimalString(value);
  return decimal != null && decimal.startsWith("-") && !/^-0+(\.0+)?$/.test(decimal);
}

export function sumAmounts(values) {
  return values.reduce((total, value) => addAmounts(total, value ?? "0") ?? total, "0.00");
}

export function compareAmounts(a, b) {
  const difference = addAmounts(a ?? "0", b ?? "0", -1);
  if (difference == null) return 0;
  if (/^-?0+(\.0+)?$/.test(difference)) return 0;
  return difference.startsWith("-") ? -1 : 1;
}

function toPaise(value) {
  const text = String(value ?? "").trim();
  const match = /^(-)?(\d+)(?:\.(\d{1,2}))?$/.exec(text);
  if (!match) return null;
  const paise = BigInt(match[2]) * 100n + BigInt((match[3] || "").padEnd(2, "0") || "0");
  return match[1] ? -paise : paise;
}

export function addAmounts(base, delta, sign = 1) {
  const a = toPaise(base);
  const b = toPaise(delta);
  if (a == null || b == null) return null;
  const total = a + (sign < 0 ? -b : b);
  const negative = total < 0n;
  const absolute = negative ? -total : total;
  return `${negative ? "-" : ""}${absolute / 100n}.${String(absolute % 100n).padStart(2, "0")}`;
}

export function divideAmount(value, parts) {
  const paise = toPaise(toDecimalString(value) ?? "");
  if (paise == null || !parts || parts < 1) return null;
  const share = paise / BigInt(Math.floor(parts));
  const negative = share < 0n;
  const absolute = negative ? -share : share;
  return `${negative ? "-" : ""}${absolute / 100n}.${String(absolute % 100n).padStart(2, "0")}`;
}
