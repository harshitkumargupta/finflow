import { addAmounts, formatMoney } from "./money";

export { addAmounts };

export function formatCurrency(value) {
  return formatMoney(value);
}

export function maskAccountNumber(value) {
  if (!value) return "—";
  const accountNumber = String(value);
  return `•••• ${accountNumber.slice(-4)}`;
}

export function formatDate(value) {
  if (!value) return "—";
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? "—"
    : new Intl.DateTimeFormat("en-IN", { dateStyle: "medium" }).format(date);
}

export function formatDateTime(value) {
  if (!value) return "—";
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? "—"
    : new Intl.DateTimeFormat("en-IN", {
        dateStyle: "medium",
        timeStyle: "short",
      }).format(date);
}

const AMOUNT_PATTERN = /^\d{1,17}(\.\d{1,2})?$/;

export function toAmountString(value, label = "Amount", { allowZero = false } = {}) {
  const text = String(value ?? "").trim();
  if (!AMOUNT_PATTERN.test(text) || (!allowZero && !/[1-9]/.test(text))) {
    const error = new Error(`${label} must be ${allowZero ? "zero or more" : "a positive number"} with at most 2 decimal places.`);
    error.isValidationError = true;
    throw error;
  }
  const [whole, fraction = ""] = text.split(".");
  return `${whole.replace(/^0+(?=\d)/, "")}.${fraction.padEnd(2, "0")}`;
}
