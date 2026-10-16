import { useEffect, useRef } from "react";
import { formatCurrency, formatDateTime, maskAccountNumber } from "../utils/format";
import { humanize } from "../utils/status";

export default function TransactionDetailsDialog({ transaction, onClose }) {
  const dialogRef = useRef(null);

  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) return;
    if (transaction && !dialog.open) dialog.showModal();
    if (!transaction && dialog.open) dialog.close();
  }, [transaction]);

  if (!transaction) {
    return <dialog ref={dialogRef} />;
  }

  const details = [
    ["Reference number", transaction.referenceNumber],
    ["Transaction type", humanize(transaction.type)],
    ["Category", transaction.category],
    ["Category source", humanize(transaction.categorySource)],
    ["Confidence", transaction.categoryConfidence],
    ["Category reason", transaction.categoryReason],
    ["Description", transaction.description],
    ["Status", transaction.status],
    ["Created", formatDateTime(transaction.createdAt)],
    ["Receiver account", maskAccountNumber(transaction.receiverAccountNumber)],
    [
      "Balance after",
      transaction.balanceAfter == null
        ? "—"
        : formatCurrency(transaction.balanceAfter),
    ],
  ];

  return (
    <dialog
      ref={dialogRef}
      onCancel={(event) => {
        event.preventDefault();
        onClose();
      }}
      className="sheet w-[min(100%-2rem,34rem)] rounded-xl p-0 text-ink shadow-raised"
      aria-labelledby="transaction-details-title"
    >
      <div className="p-6">
        <p className="eyebrow">Transaction details</p>
        <h2
          id="transaction-details-title"
          className="mt-2 font-display text-2xl font-medium"
        >
          {humanize(transaction.type)}
        </h2>
        <p className="display-number mt-3 text-3xl">
          {formatCurrency(transaction.amount)}
        </p>
        <dl className="mt-5 divide-y divide-line border-y border-line">
          {details.map(([label, value]) => (
            <div
              className="flex justify-between gap-4 py-3 text-sm"
              key={label}
            >
              <dt className="text-slate-500">{label}</dt>
              <dd className="max-w-[65%] break-words text-right font-medium">
                {value || "—"}
              </dd>
            </div>
          ))}
        </dl>
        <button
          type="button"
          className="button-secondary mt-6 w-full sm:w-auto"
          onClick={onClose}
        >
          Close details
        </button>
      </div>
    </dialog>
  );
}
