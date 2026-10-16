import { ArrowLeftRight, LayoutDashboard, Menu, PiggyBank, Wallet } from "lucide-react";
import { NavLink } from "react-router-dom";

const tabs = [
  { to: "/dashboard", label: "Today", icon: LayoutDashboard },
  { to: "/wallet", label: "Wallet", icon: Wallet },
  { to: "/transfer", label: "Move", icon: ArrowLeftRight, primary: true },
  { to: "/goals", label: "Plan", icon: PiggyBank },
];

export default function MobileNav({ onMore, moreOpen }) {
  return (
    <nav className="fixed inset-x-0 bottom-0 z-30 border-t border-line bg-surface pb-[env(safe-area-inset-bottom)] lg:hidden" aria-label="Quick navigation">
      <ul className="mx-auto grid h-16 max-w-md grid-cols-5 items-stretch">
        {tabs.map((tab) => (
          <li key={tab.to} className="flex">
            <NavLink
              to={tab.to}
              className={({ isActive }) => `flex flex-1 flex-col items-center justify-center gap-1 text-[11px] font-medium transition-colors duration-150 ${isActive ? "text-accent-strong" : "text-muted hover:text-ink"}`}
            >
              {({ isActive }) => tab.primary ? (
                <>
                  <span className={`grid h-9 w-12 place-items-center rounded-full transition-colors duration-150 ${isActive ? "bg-accent-strong text-accent-on" : "bg-accent text-accent-on"}`}><tab.icon size={18} strokeWidth={2} aria-hidden="true" /></span>
                  {tab.label}
                </>
              ) : (
                <>
                  <span className={`grid h-7 w-12 place-items-center rounded-full transition-colors duration-150 ${isActive ? "bg-accent-soft" : ""}`}><tab.icon size={19} strokeWidth={1.75} aria-hidden="true" /></span>
                  {tab.label}
                </>
              )}
            </NavLink>
          </li>
        ))}
        <li className="flex">
          <button type="button" onClick={onMore} aria-expanded={moreOpen} className="flex flex-1 flex-col items-center justify-center gap-1 text-[11px] font-medium text-muted transition-colors duration-150 hover:text-ink">
            <span className="grid h-7 w-12 place-items-center rounded-full"><Menu size={19} strokeWidth={1.75} aria-hidden="true" /></span>
            More
          </button>
        </li>
      </ul>
    </nav>
  );
}
