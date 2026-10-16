import { ArrowDownToLine, ArrowLeftRight, ArrowUpFromLine } from "lucide-react";
import { Link } from "react-router-dom";
import { formatDate, maskAccountNumber } from "../utils/format";
import { humanize } from "../utils/status";
import MoneyText from "./MoneyText";
import StatusBadge from "./StatusBadge";

export default function AccountCard({ account }) {
  const name = account.accountType === "FINFLOW" ? "FinFlow account" : `${humanize(account.accountType)} account`;
  return (
    <article className="rounded-lg border border-line bg-surface p-5 sm:p-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="text-sm font-medium text-ink">{name}{account.primary && <span className="ml-2 text-xs font-normal text-muted">Primary</span>}</p>
          <p className="tabular mt-0.5 text-[13px] tracking-wide text-muted">{maskAccountNumber(account.accountNumber)} · opened {formatDate(account.createdAt)}</p>
        </div>
        <StatusBadge status={account.status} />
      </div>
      <p className="mt-6 text-[13px] text-muted">Balance</p>
      <MoneyText value={account.balance} size="lg" className="mt-1" />
      <div className="mt-6 flex flex-wrap gap-2 border-t border-line pt-4">
        <Link to={`/deposit?accountId=${account.id}`} className="button-secondary min-h-9 px-3 text-[13px]"><ArrowDownToLine size={15} aria-hidden="true" /> Add money</Link>
        <Link to={`/transfer?accountId=${account.id}`} className="button-secondary min-h-9 px-3 text-[13px]"><ArrowLeftRight size={15} aria-hidden="true" /> Send</Link>
        <Link to={`/withdraw?accountId=${account.id}`} className="button-secondary min-h-9 px-3 text-[13px]"><ArrowUpFromLine size={15} aria-hidden="true" /> Withdraw</Link>
      </div>
    </article>
  );
}
