import { useEffect, useId, useRef } from "react";

export default function Modal({ open, title, description, onClose, busy = false, tone, children, footer }) {
  const dialogRef = useRef(null);
  const titleId = useId();

  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) return;
    if (open && !dialog.open) dialog.showModal();
    if (!open && dialog.open) dialog.close();
  }, [open]);

  return (
    <dialog
      ref={dialogRef}
      aria-labelledby={titleId}
      onCancel={(event) => {
        event.preventDefault();
        if (!busy) onClose();
      }}
      onClick={(event) => {
        if (event.target === dialogRef.current && !busy) onClose();
      }}
      className="w-[min(100%-2rem,30rem)] rounded-2xl p-0 text-ink shadow-2xl"
    >
      <div className="p-6">
        <h2 id={titleId} className={`text-xl font-semibold ${tone === "danger" ? "text-rose-700" : "text-ink"}`}>{title}</h2>
        {description && <p className="mt-2 text-sm leading-6 text-slate-500">{description}</p>}
        {children && <div className="mt-5">{children}</div>}
        {footer && <div className="mt-6 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">{footer}</div>}
      </div>
    </dialog>
  );
}
