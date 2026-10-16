import { Link } from "react-router-dom";
import { SearchX } from "lucide-react";

export default function NotFound() {
  return (
    <main className="grid min-h-screen place-items-center bg-canvas px-4">
      <section className="card max-w-lg text-center">
        <SearchX className="mx-auto text-brand-600" size={34} />
        <p className="eyebrow mt-4">404 · PAGE NOT FOUND</p>
        <h1 className="mt-2 text-2xl font-semibold text-ink">
          We couldn’t find that page
        </h1>
        <p className="mt-2 text-sm text-slate-600">
          The address may have changed or the page may no longer be available.
        </p>
        <Link to="/dashboard" className="button-primary mt-5">
          Back to Dashboard
        </Link>
      </section>
    </main>
  );
}
