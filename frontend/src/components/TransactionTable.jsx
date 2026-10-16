import { useState } from "react";
import { ArrowDownLeft, ArrowLeftRight, ArrowUpRight, ReceiptText } from "lucide-react";
import TransactionDetailsDialog from "./TransactionDetailsDialog";
import EmptyState from "./EmptyState";
import StatusBadge from "./StatusBadge";
import { LEDGER_CATEGORIES, categoryLabel } from "../constants/categories";
import { formatDateTime, maskAccountNumber } from "../utils/format";
import { formatMoney } from "../utils/money";
import { humanize } from "../utils/status";

export const transactionCategories = LEDGER_CATEGORIES;
const CREDIT_TYPES = ["DEPOSIT", "TRANSFER_IN"];
const DEBIT_TYPES = ["WITHDRAWAL", "TRANSFER_OUT"];

function TypeIcon({ type }) {
  const Icon = CREDIT_TYPES.includes(type) ? ArrowDownLeft : DEBIT_TYPES.includes(type) ? ArrowUpRight : ArrowLeftRight;
  return <Icon size={15} strokeWidth={1.75} aria-hidden="true" />;
}

function categorySource(transaction) {
  if (transaction.categorySource === "USER_SELECTED") return "set by you";
  if (transaction.categoryConfidence === "LOW") return "suggested";
  return "auto";
}

export default function TransactionTable({ transactions = [], compact = false, onCategoryChange }) {
  const [selectedTransaction, setSelectedTransaction] = useState(null);
  const [editing, setEditing] = useState(null);

  if (!transactions.length) {
    return <EmptyState icon={ReceiptText} title="No transactions yet" description="Deposits, withdrawals and transfers will appear here." />;
  }
  const rows = compact ? transactions.slice(0, 5) : transactions;
  return (
    <>
      <ul className="divide-y divide-line border-y border-line">
        {rows.map((transaction) => {
          const credit = CREDIT_TYPES.includes(transaction.type);
          return (
            <li key={transaction.id} className="grid grid-cols-[auto_minmax(0,1fr)_auto] items-start gap-x-3 gap-y-2 py-3.5">
              <span className={`mt-0.5 grid h-9 w-9 place-items-center rounded-md ${credit ? "bg-accent-soft text-accent-strong" : "bg-surface-2 text-ink-soft"}`}><TypeIcon type={transaction.type} /></span>
              <div className="min-w-0">
                <button type="button" className="block max-w-full truncate text-left text-sm font-medium text-ink hover:underline" onClick={() => setSelectedTransaction(transaction)}>
                  {transaction.description || humanize(transaction.type)}
                  <span className="sr-only">, view details</span>
                </button>
                <p className="mt-0.5 flex flex-wrap items-center gap-x-2 gap-y-0.5 text-xs text-muted">
                  <span>{categoryLabel(transaction.category)} <span className="text-muted">({categorySource(transaction)})</span></span>
                  <span aria-hidden="true">·</span>
                  <span>{formatDateTime(transaction.createdAt)}</span>
                  {transaction.receiverAccountNumber && <><span aria-hidden="true">·</span><span>to {maskAccountNumber(transaction.receiverAccountNumber)}</span></>}
                  {!compact && transaction.referenceNumber && <><span aria-hidden="true">·</span><span className="tabular">{transaction.referenceNumber}</span></>}
                </p>
                {!compact && onCategoryChange && (editing === transaction.id ? (
                  <label className="mt-2 flex max-w-xs items-center gap-2 text-xs text-ink-soft">
                    <span className="sr-only">Category for {transaction.description || transaction.referenceNumber}</span>
                    <select autoFocus className="field-control min-h-8 py-1 text-[13px]" value={transaction.category || "OTHER"} onChange={(event) => { onCategoryChange(transaction, event.target.value); setEditing(null); }} onBlur={() => setEditing(null)}>
                      {transactionCategories.map((category) => <option key={category} value={category}>{categoryLabel(category)}</option>)}
                    </select>
                  </label>
                ) : (
                  <button type="button" className="mt-1 text-xs font-semibold text-accent-strong hover:underline" onClick={() => setEditing(transaction.id)}>Change category</button>
                ))}
              </div>
              <div className="text-right">
                <p className={`tabular whitespace-nowrap text-sm font-semibold ${credit ? "text-accent-strong" : "text-ink"}`}>{credit ? "+" : "−"}{formatMoney(transaction.amount)}</p>
                {!compact && transaction.balanceAfter != null && <p className="tabular mt-0.5 whitespace-nowrap text-xs text-muted">{formatMoney(transaction.balanceAfter)} after</p>}
                {transaction.status !== "SUCCESS" && <div className="mt-1"><StatusBadge status={transaction.status} /></div>}
              </div>
            </li>
          );
        })}
      </ul>
      <TransactionDetailsDialog transaction={selectedTransaction} onClose={() => setSelectedTransaction(null)} />
    </>
  );
}
