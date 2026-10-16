import { Circle, X } from "lucide-react";
import { Link } from "react-router-dom";
import { useState } from "react";

const SKIP_BANK_KEY = "finflow.onboarding.bank-link-skipped";

export default function OnboardingProgress({ accounts = [], bankLinks = [], budgets = [], goals = [] }) {
  const [bankSkipped, setBankSkipped] = useState(() => localStorage.getItem(SKIP_BANK_KEY) === "true");
  const steps = [
    {
      label: "Secure sign-in",
      detail: "Your FinFlow profile is protected behind your sign-in.",
      complete: true,
      href: "/profile",
      action: "Review profile",
    },
    {
      label: "Create your FinFlow account and PIN",
      detail: "Set up the single FinFlow account used for balances and money movement.",
      complete: accounts.length > 0,
      href: "/accounts",
      action: accounts.length ? "View account" : "Set up account",
    },
    {
      label: "Link a sandbox bank account",
      detail: "Optional. Use a safe demo connection; FinFlow never asks for bank passwords, OTPs, or PINs.",
      complete: bankLinks.length > 0 || bankSkipped,
      href: "/accounts",
      action: bankLinks.length ? "View connection" : "Link optionally",
      optional: true,
    },
    {
      label: "Choose a first money plan",
      detail: "Start with a budget or a savings goal so your dashboard can explain your next steps.",
      complete: budgets.length > 0 || goals.length > 0,
      href: budgets.length > 0 ? "/goals" : "/budgets",
      action: budgets.length > 0 || goals.length > 0 ? "View plan" : "Create a plan",
    },
  ];
  const completed = steps.filter((step) => step.complete).length;
  const progress = Math.round((completed / steps.length) * 100);

  if (completed === steps.length) return null;

  function skipBankLink() {
    localStorage.setItem(SKIP_BANK_KEY, "true");
    setBankSkipped(true);
  }

  return (
    <section className="rounded-lg border border-line bg-surface px-5 py-4" aria-labelledby="onboarding-title">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h2 id="onboarding-title" className="section-title">Finish setting up</h2>
        <p className="tabular text-xs font-medium text-muted">{completed} of {steps.length} done</p>
      </div>
      <div className="mt-2 h-1 overflow-hidden rounded-full bg-surface-2" aria-hidden="true"><div className="h-full rounded-full bg-accent transition-[width] duration-700 ease-out" style={{ width: `${progress}%` }} /></div>
      <ol className="mt-3 divide-y divide-line">
        {steps.filter((step) => !step.complete).map((step) => (
          <li key={step.label} className="flex flex-col gap-2 py-3 sm:flex-row sm:items-center sm:justify-between">
            <div className="flex min-w-0 items-start gap-3">
              <Circle size={16} className="mt-0.5 shrink-0 text-line-strong" aria-hidden="true" />
              <div className="min-w-0">
                <p className="text-sm font-medium text-ink">{step.label}{step.optional && <span className="ml-2 text-xs font-normal text-muted">Optional</span>}</p>
                <p className="mt-0.5 text-[13px] leading-5 text-ink-soft">{step.detail}</p>
              </div>
            </div>
            <div className="flex shrink-0 items-center gap-1 pl-7 sm:pl-0">
              <Link to={step.href} className="button-secondary min-h-9 px-3 text-[13px]">{step.action}</Link>
              {step.optional && <button type="button" onClick={skipBankLink} className="button-ghost min-h-9 px-3 text-[13px] text-muted"><X size={13} aria-hidden="true" /> Skip</button>}
            </div>
          </li>
        ))}
      </ol>
    </section>
  );
}
