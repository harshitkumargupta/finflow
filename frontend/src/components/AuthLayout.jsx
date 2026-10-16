import { Link } from "react-router-dom";
import BrandMark from "./BrandMark";
import SandboxTag from "./SandboxTag";

const promises = [
  ["See what’s free to spend", "after bills, autopay, goals and budgets are counted."],
  ["Review before money moves", "every payment shows the amount, fee and balance afterwards."],
  ["Nothing is invented", "if a figure isn’t available, FinFlow says so."],
];

export default function AuthLayout({ eyebrow, title, description, children, footer }) {
  return (
    <div className="grid min-h-screen bg-paper lg:grid-cols-[minmax(0,1fr)_minmax(0,1.1fr)]">
      <aside className="hidden flex-col justify-between border-r border-line bg-surface px-12 py-12 lg:flex xl:px-16">
        <Link to="/login" className="flex items-center gap-2.5"><BrandMark size={32} /><span className="font-display text-2xl font-medium text-ink">FinFlow</span></Link>
        <div className="max-w-md">
          <p className="font-display text-[2.75rem] font-medium leading-[1.08] text-ink">Your money,<br /><span className="italic text-accent">in good flow.</span></p>
          <ul className="mt-10 space-y-5">
            {promises.map(([lead, rest]) => (
              <li key={lead} className="border-l-2 border-line pl-4 text-[15px] leading-6 text-ink-soft"><span className="font-semibold text-ink">{lead}</span> {rest}</li>
            ))}
          </ul>
        </div>
        <div><SandboxTag /></div>
      </aside>
      <main className="flex flex-col px-5 py-8 sm:px-10">
        <div className="flex items-center justify-between lg:hidden">
          <Link to="/login" className="flex items-center gap-2"><BrandMark size={28} /><span className="font-display text-xl font-medium text-ink">FinFlow</span></Link>
          <SandboxTag />
        </div>
        <div className="mx-auto flex w-full max-w-md flex-1 flex-col justify-center py-10">
          {eyebrow && <p className="eyebrow">{eyebrow}</p>}
          <h1 className="page-title mt-2">{title}</h1>
          {description && <p className="mt-3 text-[15px] leading-6 text-ink-soft">{description}</p>}
          <div className="mt-8">{children}</div>
          {footer && <div className="mt-8 border-t border-line pt-6 text-sm text-ink-soft">{footer}</div>}
        </div>
      </main>
    </div>
  );
}
