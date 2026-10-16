import { useEffect, useState } from "react";
import { Outlet, useLocation } from "react-router-dom";
import MobileNav from "../components/MobileNav";
import Navbar from "../components/Navbar";
import Sidebar from "../components/Sidebar";
import VerifyEmailBanner from "../components/VerifyEmailBanner";
import VerifyPhoneBanner from "../components/VerifyPhoneBanner";

export default function DashboardLayout() {
  const [menuOpen, setMenuOpen] = useState(false);
  const location = useLocation();

  useEffect(() => {
    if (!menuOpen) return undefined;
    const onKeyDown = (event) => { if (event.key === "Escape") setMenuOpen(false); };
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [menuOpen]);

  useEffect(() => { window.scrollTo({ top: 0 }); }, [location.pathname]);

  return (
    <div className="min-h-screen bg-paper">
      <a href="#main" className="sr-only focus:not-sr-only focus:fixed focus:left-4 focus:top-4 focus:z-[60] focus:rounded-md focus:bg-surface focus:px-4 focus:py-2 focus:text-sm focus:font-semibold focus:text-ink focus:shadow-raised">Skip to content</a>
      <Sidebar open={menuOpen} onClose={() => setMenuOpen(false)} />
      <div className="min-h-screen lg:pl-[264px]">
        <Navbar />
        <main id="main" className="mx-auto w-full max-w-[1200px] px-4 pb-28 pt-6 sm:px-6 md:pt-10 lg:px-10 lg:pb-16">
          <VerifyEmailBanner />
          <VerifyPhoneBanner />
          <div key={location.pathname} className="animate-page"><Outlet /></div>
        </main>
      </div>
      <MobileNav onMore={() => setMenuOpen(true)} moreOpen={menuOpen} />
    </div>
  );
}
