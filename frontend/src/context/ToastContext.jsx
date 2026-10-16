import { createContext, useCallback, useContext, useEffect, useRef, useState } from "react";
import { CheckCircle2, CircleAlert, X } from "lucide-react";

const ToastContext = createContext(null);
let nextToastId = 0;

function ToastItem({ toast, onDismiss }) {
  const isError = toast.type === "error";
  const [paused, setPaused] = useState(false);
  const [remaining, setRemaining] = useState(5000);
  const startedAt = useRef(Date.now());

  useEffect(() => {
    if (paused) return undefined;
    startedAt.current = Date.now();
    const timeoutId = window.setTimeout(() => onDismiss(toast.id), remaining);
    return () => window.clearTimeout(timeoutId);
  }, [onDismiss, toast.id, paused, remaining]);

  return (
    <div
      className={`toast-enter relative flex min-w-72 max-w-md items-start gap-3 rounded-md border px-4 py-3 text-sm shadow-raised ${
        isError
          ? "border-danger/25 bg-surface text-danger"
          : "border-accent/25 bg-surface text-accent-strong"
      }`}
      role={isError ? "alert" : "status"}
      onMouseEnter={() => { setRemaining((value) => Math.max(0, value - (Date.now() - startedAt.current))); setPaused(true); }}
      onMouseLeave={() => setPaused(false)}
    >
      {isError ? (
        <CircleAlert size={18} className="mt-0.5 shrink-0" />
      ) : (
        <CheckCircle2 size={18} className="mt-0.5 shrink-0" />
      )}
      <p className="flex-1">{toast.message}</p>
      <button
        type="button"
        onClick={() => onDismiss(toast.id)}
        className="rounded p-0.5 hover:bg-surface-2"
        aria-label="Dismiss notification"
      >
        <X size={16} />
      </button>
      <span className="toast-progress absolute bottom-0 left-0 h-0.5 w-full bg-current opacity-30" style={{ animationPlayState: paused ? "paused" : "running", animationDuration: `${remaining}ms` }} aria-hidden="true" />
    </div>
  );
}

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);

  const dismiss = useCallback((id) => {
    setToasts((current) => current.filter((toast) => toast.id !== id));
  }, []);

  const notify = useCallback((message, type = "success") => {
    const normalizedMessage = String(message || "").trim();
    if (!normalizedMessage) return;

    const id = ++nextToastId;
    setToasts((current) => {
      if (
        current.some(
          (toast) => toast.type === type && toast.message === normalizedMessage,
        )
      ) {
        return current;
      }
      return [...current, { id, message: normalizedMessage, type }];
    });
  }, []);

  return (
    <ToastContext.Provider value={notify}>
      {children}
      <div
        className="pointer-events-none fixed right-4 top-4 z-[100] flex max-w-[calc(100vw-2rem)] flex-col gap-2"
        aria-live="polite"
      >
        {toasts.map((toast) => (
          <div className="pointer-events-auto" key={toast.id}>
            <ToastItem toast={toast} onDismiss={dismiss} />
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}

export function useToast() {
  const notify = useContext(ToastContext);
  if (!notify) throw new Error("useToast must be used inside ToastProvider");
  return notify;
}
