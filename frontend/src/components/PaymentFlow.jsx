import StepIndicator from "./StepIndicator";

export default function PaymentFlow({ steps, activeStep, children, className = "" }) {
  return <div className={className}>
    <StepIndicator steps={steps} activeStep={activeStep} />
    <div key={activeStep} className="motion-content mt-5">{children}</div>
  </div>;
}
