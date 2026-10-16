import { FlaskConical } from "lucide-react";

export default function SandboxTag({ className = "" }) {
  return (
    <span className={`tag ${className}`} title="Sandbox mode: balances and payments are simulated. No real money moves.">
      <FlaskConical size={12} strokeWidth={2} aria-hidden="true" />
      Sandbox
      <span className="sr-only">: balances and payments are simulated. No real money moves.</span>
    </span>
  );
}
