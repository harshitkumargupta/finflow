import { useRef, useState } from "react";
import { Camera, ImageUp, UploadCloud } from "lucide-react";

const ACCEPTED = ["image/png", "image/jpeg", "image/jpg", "image/webp"];

export default function ReceiptDropzone({ onFile, disabled = false, busy = false }) {
  const fileRef = useRef(null);
  const cameraRef = useRef(null);
  const [dragging, setDragging] = useState(false);
  const [localError, setLocalError] = useState("");

  function handleFiles(files) {
    setLocalError("");
    const file = files?.[0];
    if (!file) return;
    if (!ACCEPTED.includes(file.type)) {
      setLocalError("Choose a PNG, JPEG or WebP image.");
      return;
    }
    onFile(file);
  }

  return (
    <div>
      <div
        role="button"
        tabIndex={disabled ? -1 : 0}
        aria-disabled={disabled}
        onClick={() => !disabled && fileRef.current?.click()}
        onKeyDown={(event) => { if (!disabled && (event.key === "Enter" || event.key === " ")) { event.preventDefault(); fileRef.current?.click(); } }}
        onDragOver={(event) => { event.preventDefault(); if (!disabled) setDragging(true); }}
        onDragLeave={() => setDragging(false)}
        onDrop={(event) => { event.preventDefault(); setDragging(false); if (!disabled) handleFiles(event.dataTransfer.files); }}
        className={`flex flex-col items-center justify-center gap-3 rounded-xl border-2 border-dashed px-6 py-10 text-center transition-colors duration-150 ${disabled ? "cursor-not-allowed border-line bg-surface-2 opacity-60" : "cursor-pointer"} ${dragging ? "border-accent bg-accent-soft" : "border-line-strong bg-surface hover:border-accent"}`}
      >
        <span className="flex h-12 w-12 items-center justify-center rounded-full bg-accent-soft text-accent-strong"><UploadCloud size={22} strokeWidth={1.75} aria-hidden="true" /></span>
        <div>
          <p className="text-sm font-semibold text-ink">{busy ? "Scanning…" : "Drag a receipt here, or tap to choose"}</p>
          <p className="mt-0.5 text-[13px] text-muted">PNG, JPEG or WebP · up to 5 MB</p>
        </div>
      </div>
      <div className="mt-3 flex flex-wrap gap-2">
        <button type="button" className="button-secondary min-h-9 px-3 py-1.5 text-[13px]" onClick={() => fileRef.current?.click()} disabled={disabled}><ImageUp size={15} aria-hidden="true" /> Choose image</button>
        <button type="button" className="button-ghost min-h-9 px-3 py-1.5 text-[13px] sm:hidden" onClick={() => cameraRef.current?.click()} disabled={disabled}><Camera size={15} aria-hidden="true" /> Use camera</button>
      </div>
      {localError && <p role="alert" className="mt-2 text-[13px] text-danger">{localError}</p>}
      <input ref={fileRef} type="file" accept="image/png,image/jpeg,image/webp" className="sr-only" onChange={(event) => { handleFiles(event.target.files); event.target.value = ""; }} />
      <input ref={cameraRef} type="file" accept="image/*" capture="environment" className="sr-only" onChange={(event) => { handleFiles(event.target.files); event.target.value = ""; }} />
    </div>
  );
}
