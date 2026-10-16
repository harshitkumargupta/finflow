import { formatStatus, statusTone } from "../utils/status";

export default function StatusBadge({ status, label }) {
  return <span className={`status-pill status-${statusTone(status)}`}>{label || formatStatus(status)}</span>;
}
