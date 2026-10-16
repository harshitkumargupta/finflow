export default function PageHeader({ eyebrow, title, description, action }) {
  return (
    <header className="page-header pb-2">
      <div className="min-w-0">
        {eyebrow && <p className="eyebrow">{eyebrow}</p>}
        <h1 className={`page-title ${eyebrow ? "mt-2" : ""}`}>{title}</h1>
        {description && <p className="mt-3 max-w-2xl text-[15px] leading-6 text-ink-soft">{description}</p>}
      </div>
      {action && <div className="flex shrink-0 flex-wrap gap-2">{action}</div>}
    </header>
  );
}
