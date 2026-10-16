import { Bell, CheckCheck, Moon, Sun } from "lucide-react";
import useTheme from "../hooks/useTheme";
import { Link } from "react-router-dom";
import BrandMark from "./BrandMark";
import SandboxTag from "./SandboxTag";
import { useEffect, useState } from "react";
import { getUnreadNotificationCount, getUnreadNotifications, markAllNotificationsRead, updateNotificationState } from "../api/financeApi";
import { getApiErrorMessage } from "../api/axios";
import { useAuth } from "../context/AuthContext";
import { useToast } from "../context/ToastContext";

export default function Navbar() {
  const { user } = useAuth();
  const { resolved, setPreference } = useTheme();
  const notify = useToast();
  const [notificationsOpen, setNotificationsOpen] = useState(false);
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [notificationError, setNotificationError] = useState("");
  const [notificationsLoading, setNotificationsLoading] = useState(false);
  const initials = user?.name
    ?.split(/\s+/)
    .map((part) => part[0])
    .join("")
    .slice(0, 2)
    .toUpperCase();

  useEffect(() => {
    getUnreadNotificationCount()
      .then(({ data }) => setUnreadCount(data.count))
      .catch((error) => setNotificationError(getApiErrorMessage(error)));
  }, []);

  useEffect(() => {
    if (!notificationsOpen) return;
    setNotificationsLoading(true);
    getUnreadNotifications()
      .then(({ data }) => {
        setNotifications(data);
        setUnreadCount(data.length);
        setNotificationError("");
      })
      .catch((error) => setNotificationError(getApiErrorMessage(error)))
      .finally(() => setNotificationsLoading(false));
  }, [notificationsOpen]);

  async function markRead(id) {
    try {
      await updateNotificationState(id, true);
      setNotifications((current) => current.filter((item) => item.id !== id));
      setUnreadCount((current) => Math.max(0, current - 1));
    } catch (error) {
      setNotificationError(getApiErrorMessage(error));
    }
  }

  async function markAllRead() {
    try {
      await markAllNotificationsRead();
      setNotifications([]);
      setUnreadCount(0);
      notify("Notifications marked as read.");
    } catch (error) {
      setNotificationError(getApiErrorMessage(error));
    }
  }

  return (
    <header className="sticky top-0 z-20 flex h-16 items-center justify-between gap-3 border-b border-line bg-paper px-4 md:px-8">
      <div className="flex min-w-0 items-center gap-2">
        <Link to="/dashboard" className="flex items-center gap-2 rounded-md lg:hidden">
          <BrandMark size={26} />
          <span className="font-display text-xl font-medium tracking-tight text-ink">FinFlow</span>
        </Link>
        <SandboxTag className="lg:hidden" />
        <p className="hidden font-display text-[1.05rem] italic text-muted lg:block">Your money, in good flow.</p>
      </div>
      <div className="flex items-center gap-1 md:gap-2">
        <button type="button" className="icon-action" onClick={() => setPreference(resolved === "dark" ? "light" : "dark")} aria-label={resolved === "dark" ? "Switch to light theme" : "Switch to dark theme"} title={resolved === "dark" ? "Light theme" : "Dark theme"}>
          {resolved === "dark" ? <Sun size={18} strokeWidth={1.75} /> : <Moon size={18} strokeWidth={1.75} />}
        </button>
        <div className="relative">
          <button type="button" className="icon-action relative" onClick={() => setNotificationsOpen((open) => !open)} aria-label={`Notifications, ${unreadCount} unread`} aria-expanded={notificationsOpen} aria-haspopup="true">
            <Bell size={18} strokeWidth={1.75} />
            {unreadCount > 0 && <span className="tabular absolute right-1.5 top-1.5 grid min-h-[18px] min-w-[18px] place-items-center rounded-full bg-danger px-1 text-[10px] font-semibold text-surface">{unreadCount > 9 ? "9+" : unreadCount}</span>}
          </button>
          {notificationsOpen && <section className="modal-panel absolute right-0 top-12 z-50 w-[min(24rem,calc(100vw-2rem))] overflow-hidden rounded-lg border border-line bg-surface shadow-raised" aria-label="Notification center">
            <div className="flex items-center justify-between border-b border-line px-4 py-3"><div><h2 className="text-sm font-semibold text-ink">Notifications</h2><p className="text-xs text-muted">{unreadCount ? `${unreadCount} unread` : "Nothing new"}</p></div>{notifications.length > 0 && <button type="button" className="button-ghost min-h-8 px-2 text-xs" onClick={markAllRead}><CheckCheck size={14} /> Mark all read</button>}</div>
            {notificationError && <p className="px-4 py-3 text-sm text-danger" role="alert">{notificationError}</p>}
            {notificationsLoading ? <div className="space-y-2 p-4" role="status" aria-label="Loading notifications"><div className="skeleton h-12 rounded-md" /><div className="skeleton h-12 rounded-md" /></div> : notifications.length ? <ul className="max-h-[min(65vh,28rem)] divide-y divide-line overflow-y-auto">{notifications.map((item) => <li key={item.id} className="flex items-start gap-3 px-4 py-3"><span className={`mt-1.5 h-2 w-2 shrink-0 rounded-full ${item.severity === "WARNING" ? "bg-warm" : "bg-accent"}`} aria-hidden="true" /><div className="min-w-0 flex-1"><p className="text-sm font-medium text-ink">{item.title}</p><p className="mt-0.5 text-[13px] leading-5 text-ink-soft">{item.message}</p><p className="mt-1 text-xs text-muted">{new Date(item.createdAt).toLocaleString()}</p></div><button type="button" className="button-ghost min-h-8 px-2 text-xs" onClick={() => markRead(item.id)}>Mark read</button></li>)}</ul> : !notificationError && <p className="px-4 py-8 text-center text-sm text-muted">You’re all caught up.</p>}
            <Link to="/notifications" onClick={() => setNotificationsOpen(false)} className="block border-t border-line px-4 py-2.5 text-center text-[13px] font-semibold text-accent-strong hover:bg-surface-2">See all notifications</Link>
          </section>}
        </div>
        <Link to="/profile" className="flex items-center gap-2 rounded-md py-1 pl-1 pr-2 hover:bg-surface-2" aria-label={`Profile for ${user?.name || "you"}`}>
          <span className="grid h-8 w-8 place-items-center rounded-full bg-accent-soft text-xs font-semibold text-accent-strong" aria-hidden="true">{initials || "U"}</span>
          <span className="hidden text-sm font-medium text-ink-soft md:block">{user?.name?.split(" ")[0]}</span>
        </Link>
      </div>
    </header>
  );
}
