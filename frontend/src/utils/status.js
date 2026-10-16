const LABELS = {
  OK: "On track",
  WARNING_80: "Approaching limit",
  EXCEEDED: "Exceeded",
  PROJECTED_OVER: "Projected over",
  ACTIVE: "Active",
  PAUSED: "Paused",
  PENDING_APPROVAL: "Pending approval",
  PENDING_RECONCILIATION: "Pending reconciliation",
  COMPLETED: "Completed",
  SUCCESS: "Successful",
  FAILED: "Failed",
  CANCELLED: "Cancelled",
  REVOKED: "Revoked",
  FROZEN: "Frozen",
  MERGED: "Merged",
};

const TONES = {
  success: ["OK", "ACTIVE", "COMPLETED", "SUCCESS", "VERIFIED", "LINKED", "RECEIVED"],
  warning: ["WARNING_80", "PENDING_APPROVAL", "PENDING_RECONCILIATION", "PAUSED", "PENDING", "PROCESSING", "FROZEN"],
  danger: ["EXCEEDED", "PROJECTED_OVER", "FAILED", "CANCELLED", "REVOKED", "BLOCKED", "REJECTED"],
};

export function humanize(value, fallback = "—") {
  if (value == null || value === "") return fallback;
  const text = String(value).toLowerCase().replaceAll("_", " ");
  return text.charAt(0).toUpperCase() + text.slice(1);
}

export function formatStatus(status) {
  if (!status) return "Unavailable";
  return LABELS[String(status).toUpperCase()] || humanize(status);
}

export function statusTone(status) {
  const key = String(status || "").toUpperCase();
  if (TONES.success.includes(key)) return "success";
  if (TONES.warning.includes(key)) return "warning";
  if (TONES.danger.includes(key)) return "danger";
  return "neutral";
}
