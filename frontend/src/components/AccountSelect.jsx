import { maskAccountNumber } from "../utils/format";
import { formatMoney } from "../utils/money";
import { humanize } from "../utils/status";

export const accountName = (account) => (account.accountType === "FINFLOW" ? "FinFlow account" : `${humanize(account.accountType)} account`);

export default function AccountSelect({ accounts, value, onChange, required = true, label = "From" }) {
  return (
    <label className="field-label">
      {label}
      <select className="field-control" value={value} onChange={(event) => onChange(event.target.value)} required={required} disabled={!accounts.length}>
        {!accounts.length && <option value="">No accounts available</option>}
        {accounts.map((account) => (
          <option key={account.id} value={account.id}>
            {accountName(account)} {maskAccountNumber(account.accountNumber)} · {formatMoney(account.balance)}
          </option>
        ))}
      </select>
    </label>
  );
}
