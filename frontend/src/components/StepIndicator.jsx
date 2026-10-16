export default function StepIndicator({ steps, activeStep }) {
  return (
    <ol className="flex items-center gap-3 text-[13px]" aria-label="Progress">
      {steps.map((step, index) => (
        <li key={step.id} className="flex items-center gap-3" aria-current={index === activeStep ? "step" : undefined}>
          <span className={`flex items-center gap-2 font-medium transition-colors duration-200 ${index === activeStep ? "text-ink" : index < activeStep ? "text-accent-strong" : "text-muted"}`}>
            <span className={`tabular grid h-5 w-5 place-items-center rounded-full text-[11px] font-semibold transition-colors duration-200 ${index < activeStep ? "bg-accent text-accent-on" : index === activeStep ? "bg-ink text-paper" : "border border-line-strong"}`}>{index + 1}</span>
            {step.label}
          </span>
          {index < steps.length - 1 && <span className={`h-px w-6 sm:w-10 ${index < activeStep ? "bg-accent" : "bg-line-strong"}`} aria-hidden="true" />}
        </li>
      ))}
    </ol>
  );
}
