export function toISODate(date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
}

export function parseISODate(value) {
  if (!value) return null;
  const [y, m, d] = String(value).slice(0, 10).split("-").map(Number);
  return new Date(y, m - 1, d);
}

export function todayISO() {
  return toISODate(new Date());
}

export function startOfToday() {
  const now = new Date();
  return new Date(now.getFullYear(), now.getMonth(), now.getDate());
}

export function daysFromToday(value) {
  const date = parseISODate(value);
  if (!date) return null;
  return Math.round((date - startOfToday()) / 86400000);
}

export function monthBounds(reference = new Date()) {
  return {
    startDate: toISODate(new Date(reference.getFullYear(), reference.getMonth(), 1)),
    endDate: toISODate(new Date(reference.getFullYear(), reference.getMonth() + 1, 0)),
  };
}

export function addFrequency(value, frequency) {
  const date = parseISODate(value);
  if (!date) return value;
  const day = date.getDate();
  const shiftMonths = (months) => {
    const next = new Date(date.getFullYear(), date.getMonth() + months, 1);
    const last = new Date(next.getFullYear(), next.getMonth() + 1, 0).getDate();
    next.setDate(Math.min(day, last));
    return next;
  };
  if (frequency === "DAILY") return toISODate(new Date(date.getFullYear(), date.getMonth(), day + 1));
  if (frequency === "WEEKLY") return toISODate(new Date(date.getFullYear(), date.getMonth(), day + 7));
  if (frequency === "MONTHLY") return toISODate(shiftMonths(1));
  if (frequency === "QUARTERLY") return toISODate(shiftMonths(3));
  if (frequency === "YEARLY") return toISODate(shiftMonths(12));
  return value;
}

export function dueLabel(days) {
  if (days == null) return "";
  if (days === 0) return "Due today";
  if (days === 1) return "Due tomorrow";
  if (days > 1) return `Due in ${days} days`;
  if (days === -1) return "1 day overdue";
  return `${Math.abs(days)} days overdue`;
}

export const shortDate = (value) => {
  const date = parseISODate(value);
  return date ? new Intl.DateTimeFormat("en-IN", { day: "numeric", month: "short" }).format(date) : "—";
};

export const longDate = (value) => {
  const date = parseISODate(value);
  return date ? new Intl.DateTimeFormat("en-IN", { weekday: "short", day: "numeric", month: "short", year: "numeric" }).format(date) : "—";
};
