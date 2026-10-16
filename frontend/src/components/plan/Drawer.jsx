export default function Drawer({ open, id, children }) {
  return (
    <div id={id} className="drawer" data-open={open} aria-hidden={!open} inert={open ? undefined : ""}>
      <div>{children}</div>
    </div>
  );
}
