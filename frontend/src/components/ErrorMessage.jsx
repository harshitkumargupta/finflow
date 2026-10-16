import { AlertCircle, X } from "lucide-react";

export default function ErrorMessage({
  message,
  onDismiss,
  onRetry,
  className = "",
}) {
  if (!message) return null;
  return (
    <div
      className={`flex items-start gap-3 rounded-md border border-danger/25 bg-danger-soft px-4 py-3 text-sm text-danger ${className}`}
      role="alert"
    >
      <AlertCircle size={18} className="mt-0.5 shrink-0" />
      <p className="flex-1">{message}</p>
      <button
        type="button"
        onClick={onRetry || (() => window.location.reload())}
        className="shrink-0 font-semibold underline underline-offset-2"
      >
        Retry
      </button>
      {onDismiss && (
        <button type="button" onClick={onDismiss} aria-label="Dismiss error">
          <X size={16} />
        </button>
      )}
    </div>
  );
}
