export const LEDGER_CATEGORIES = [
  "FOOD", "GROCERIES", "SHOPPING", "CLOTHING", "MEDICAL", "TRANSPORT", "BILLS",
  "RENT", "EDUCATION", "ENTERTAINMENT", "INVESTMENT", "TRANSFER", "DEPOSIT", "OTHER",
];

export const SPENDING_CATEGORIES = LEDGER_CATEGORIES.filter((category) => !["TRANSFER", "DEPOSIT"].includes(category));

export const PLANNING_CATEGORIES = [
  "FOOD", "GROCERIES", "TRANSPORT", "SHOPPING", "BILLS", "ENTERTAINMENT", "HEALTH", "EDUCATION", "OTHER",
];

export const BUDGET_CATEGORIES = ["OVERALL", ...PLANNING_CATEGORIES];

export function categoryLabel(category) {
  if (!category) return "Uncategorized";
  const text = String(category).toLowerCase().replaceAll("_", " ");
  return text.charAt(0).toUpperCase() + text.slice(1);
}
