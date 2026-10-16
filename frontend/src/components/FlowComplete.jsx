import { Link } from "react-router-dom";
import ReceiptCard from "./ReceiptCard";
import SuccessCheck from "./SuccessCheck";

export default function FlowComplete({ headline, detail, rows, receiptTitle = "Receipt", onAgain, againLabel = "Start another" }) {
  return (
    <section className="motion-content" aria-live="polite">
      <SuccessCheck />
      <h2 className="mt-4 font-display text-[1.75rem] font-medium leading-tight text-ink">{headline}</h2>
      {detail && <p className="mt-2 text-[15px] leading-6 text-ink-soft">{detail}</p>}
      <ReceiptCard title={receiptTitle} rows={rows} />
      <div className="mt-6 flex flex-wrap gap-2">
        <button type="button" className="button-primary" onClick={onAgain}>{againLabel}</button>
        <Link to="/transactions" className="button-secondary">View transactions</Link>
        <Link to="/dashboard" className="button-ghost">Back to overview</Link>
      </div>
    </section>
  );
}
