import {
  ArrowDownToLine,
  ArrowLeftRight,
  ArrowUpFromLine,
  BarChart3,
  CalendarClock,
  CreditCard,
  FileText,
  Landmark,
  LayoutDashboard,
  LogOut,
  PiggyBank,
  ReceiptText,
  Repeat2,
  ScanLine,
  Sparkles,
  Target,
  TrendingUp,
  UserRound,
  Users,
  Wallet,
  X,
} from "lucide-react";
import { NavLink } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import BrandMark from "./BrandMark";
import SandboxTag from "./SandboxTag";

export const navigation = [
  { group: "Today", items: [{ to: "/dashboard", label: "Overview", icon: LayoutDashboard }] },
  {
    group: "Money",
    items: [
      { to: "/accounts", label: "Accounts", icon: Landmark },
      { to: "/wallet", label: "Wallet & pay", icon: Wallet },
      { to: "/transactions", label: "Transactions", icon: ReceiptText },
      { to: "/receipts", label: "Scan receipt", icon: ScanLine },
    ],
  },
  {
    group: "Move",
    items: [
      { to: "/transfer", label: "Transfer", icon: ArrowLeftRight },
      { to: "/deposit", label: "Add money", icon: ArrowDownToLine },
      { to: "/withdraw", label: "Withdraw", icon: ArrowUpFromLine },
    ],
  },
  {
    group: "Plan",
    items: [
      { to: "/budgets", label: "Budgets", icon: Target },
      { to: "/bills", label: "Bills & income", icon: CalendarClock },
      { to: "/autopay", label: "Autopay", icon: Repeat2 },
      { to: "/goals", label: "Goals", icon: PiggyBank },
      { to: "/goals/assistant", label: "Goal assistant", icon: Sparkles },
    ],
  },
  {
    group: "Understand",
    items: [
      { to: "/analytics", label: "Analytics", icon: BarChart3 },
      { to: "/reports", label: "Reports", icon: FileText },
      { to: "/investments", label: "Investments", icon: TrendingUp },
    ],
  },
  {
    group: "Household",
    items: [
      { to: "/family", label: "Family", icon: Users },
      { to: "/cards", label: "Cards", icon: CreditCard },
    ],
  },
];

export default function Sidebar({ open, onClose }) {
  const { logout, user } = useAuth();
  return (
    <>
      {open && <button type="button" className="fixed inset-0 z-40 bg-black/40 lg:hidden" onClick={onClose} aria-label="Close navigation" />}
      <aside
        className={`fixed inset-y-0 left-0 z-50 flex w-[264px] flex-col border-r border-line bg-surface transition-transform duration-200 ease-out lg:z-30 lg:translate-x-0 ${open ? "translate-x-0 shadow-raised" : "-translate-x-full"}`}
        aria-label="Primary"
      >
        <div className="flex h-16 shrink-0 items-center justify-between px-5">
          <NavLink to="/dashboard" onClick={onClose} className="flex items-center gap-2.5 rounded-md">
            <BrandMark />
            <span className="font-display text-[1.35rem] font-medium tracking-tight text-ink">FinFlow</span>
          </NavLink>
          <button type="button" onClick={onClose} className="icon-action lg:hidden" aria-label="Close navigation"><X size={18} /></button>
        </div>
        <nav className="flex-1 overflow-y-auto px-3 pb-6" aria-label="Main navigation">
          {navigation.map((section) => (
            <div key={section.group} className="mt-5 first:mt-2">
              <p className="mb-1.5 px-3 text-[11px] font-semibold uppercase tracking-[0.14em] text-muted">{section.group}</p>
              <ul className="space-y-0.5">
                {section.items.map((item) => (
                  <li key={item.to}>
                    <NavLink end={item.to === "/goals"} to={item.to} onClick={onClose} className={({ isActive }) => `nav-link ${isActive ? "nav-link-active" : ""}`}>
                      <item.icon size={17} strokeWidth={1.75} aria-hidden="true" />
                      <span>{item.label}</span>
                    </NavLink>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </nav>
        <div className="shrink-0 border-t border-line px-3 py-3">
          <NavLink to="/profile" onClick={onClose} className={({ isActive }) => `nav-link ${isActive ? "nav-link-active" : ""}`}>
            <UserRound size={17} strokeWidth={1.75} aria-hidden="true" />
            <span className="min-w-0 flex-1 truncate">{user?.name || "Profile"}</span>
          </NavLink>
          <button type="button" onClick={() => logout()} className="nav-link w-full">
            <LogOut size={17} strokeWidth={1.75} aria-hidden="true" /> Sign out
          </button>
          <div className="mt-2 px-3"><SandboxTag /></div>
        </div>
      </aside>
    </>
  );
}
