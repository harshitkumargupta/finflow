import { Link } from "react-router-dom";
import PageHeader from "../components/PageHeader";
import PlannerPanel from "../components/plan/PlannerPanel";

export default function GoalAssistant() {
  return (
    <div className="mx-auto max-w-3xl space-y-8">
      <PageHeader
        eyebrow="Plan"
        title="Goal assistant"
        description="Describe what you want and FinFlow works out whether it fits, using only your own numbers. No AI provider, no guesses."
        action={<Link to="/goals" className="button-secondary">Your goals</Link>}
      />
      <PlannerPanel initialMode="goal" />
    </div>
  );
}
