import { Component } from "react";
import { Link } from "react-router-dom";
import { CircleAlert } from "lucide-react";

export default class ErrorBoundary extends Component {
  state = { hasError: false };

  static getDerivedStateFromError() {
    return { hasError: true };
  }

  componentDidCatch(error, errorInfo) {
    console.error("FinFlow interface error", error, errorInfo);
  }

  render() {
    if (this.state.hasError) {
      return (
        <main className="grid min-h-screen place-items-center bg-canvas px-4">
          <section className="card max-w-lg text-center">
            <CircleAlert className="mx-auto text-rose-600" size={32} />
            <h1 className="mt-4 text-xl font-semibold text-ink">
              This page could not be displayed
            </h1>
            <p className="mt-2 text-sm text-slate-600">
              Your account data is unchanged. Try returning to your dashboard.
            </p>
            <Link className="button-primary mt-5" to="/dashboard">
              Back to Dashboard
            </Link>
          </section>
        </main>
      );
    }

    return this.props.children;
  }
}
