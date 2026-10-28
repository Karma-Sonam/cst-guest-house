import { Children, cloneElement, isValidElement, useEffect, useId, useRef, type ReactNode, type FormEvent } from 'react'
import { ArrowRight, LoaderCircle, X, AlertCircle, CalendarDays } from 'lucide-react'
import { Link } from 'react-router-dom'
export function label(value: string) { return value.toLowerCase().replaceAll('_', ' ').replace(/\b\w/g, char => char.toUpperCase()) }
export function date(value: string) { return new Intl.DateTimeFormat('en-GB', { day: 'numeric', month: 'short', year: 'numeric', timeZone: 'Asia/Thimphu' }).format(new Date(value.length === 10 ? `${value}T00:00:00+06:00` : value)) }
export function today() { return new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Thimphu', year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date()) }
export function tomorrow(value = today()) { let d = new Date(`${value}T12:00:00Z`); if (Number.isNaN(d.getTime())) d = new Date(`${today()}T12:00:00Z`); d.setUTCDate(d.getUTCDate() + 1); return d.toISOString().slice(0, 10) }
export function Badge({ status }: { status: string }) { return <span className={`badge badge-${status.toLowerCase()}`}>{label(status)}</span> }
export function Loading() { return <div className="loading" role="status"><LoaderCircle className="spin" size={22} /> Loading…</div> }
export function ErrorBox({ message, retry }: { message: string; retry?: () => void }) { return <div className="error-box" role="alert"><AlertCircle size={19} /><div>{message}{retry && <button className="text-button" onClick={retry}>Try again</button>}</div></div> }
export function Empty({ title = 'Nothing here yet', description, action }: { title?: string; description: string; action?: ReactNode }) { return <div className="empty"><CalendarDays size={32} /><h3>{title}</h3><p>{description}</p>{action}</div> }
export function PageHeader({ eyebrow, title, description, action }: { eyebrow?: string; title: string; description?: string; action?: ReactNode }) { return <div className="page-header"><div>{eyebrow && <p className="eyebrow">{eyebrow}</p>}<h1>{title}</h1>{description && <p className="muted">{description}</p>}</div>{action}</div> }
export function Field({ label: text, children, hint }: { label: string; children: ReactNode; hint?: string }) {
  const generated = useId(); const hintId = `${generated}-hint`; let controlId = generated; let found = false
  function associate(nodes: ReactNode): ReactNode {
    return Children.map(nodes, child => {
      if (!isValidElement<{ id?: string; children?: ReactNode; 'aria-describedby'?: string }>(child)) return child
      if (!found && typeof child.type === 'string' && ['input', 'select', 'textarea'].includes(child.type)) {
        found = true; controlId = child.props.id || generated
        return cloneElement(child, { id: controlId, 'aria-describedby': [child.props['aria-describedby'], hint ? hintId : null].filter(Boolean).join(' ') || undefined })
      }
      return child.props.children ? cloneElement(child, { children: associate(child.props.children) }) : child
    })
  }
  const controls = associate(children)
  return <div className="field"><label htmlFor={controlId}>{text}</label>{controls}{hint && <small id={hintId}>{hint}</small>}</div>
}
export function Submit({ busy, disabled = false, children = 'Save changes' }: { busy: boolean; disabled?: boolean; children?: ReactNode }) { return <button className="button" type="submit" disabled={busy || disabled}>{busy ? <><LoaderCircle size={17} className="spin" />Saving…</> : children}</button> }
export function Pagination({ page, total, size, setPage }: { page: number; total: number; size: number; setPage: (page: number) => void }) { return <div className="pagination"><span>{total} records</span><button className="button secondary small" disabled={page === 0} onClick={() => setPage(page - 1)}>Previous</button><span>Page {page + 1} of {Math.max(1, Math.ceil(total / size))}</span><button className="button secondary small" disabled={(page + 1) * size >= total} onClick={() => setPage(page + 1)}>Next</button></div> }
export function Timeline({ events }: { events: { newStatus: string; remark: string; changedAt: string }[] }) { return <ol className="timeline">{events.map((event, i) => <li key={i}><span className="timeline-dot" /><div><strong>{label(event.newStatus)}</strong><p>{event.remark}</p><small>{date(event.changedAt)} · {new Date(event.changedAt).toLocaleTimeString('en-GB', { timeZone: 'Asia/Thimphu', hour: '2-digit', minute: '2-digit' })}</small></div></li>)}</ol> }
export function DetailRows({ rows }: { rows: [string, ReactNode][] }) { return <dl className="detail-rows">{rows.map(([key, value]) => <div key={key}><dt>{key}</dt><dd>{value ?? '—'}</dd></div>)}</dl> }
export function ActionLink({ to, children }: { to: string; children: ReactNode }) { return <Link className="text-link" to={to}>{children}<ArrowRight size={16} /></Link> }
export function ConfirmDialog({ title, description, actionLabel = 'Confirm', busy, error, onClose, onConfirm }: { title: string; description: string; actionLabel?: string; busy: boolean; error?: string; onClose: () => void; onConfirm: (event: FormEvent<HTMLFormElement>) => void }) {
  const dialog = useRef<HTMLDialogElement>(null)
  const titleId = useId(); const descriptionId = useId()
  useEffect(() => { dialog.current?.showModal(); return () => dialog.current?.close() }, [])
  return <dialog ref={dialog} aria-labelledby={titleId} aria-describedby={descriptionId} className="dialog" onCancel={event => { if (busy) event.preventDefault(); else onClose() }}><form onSubmit={onConfirm}><div className="dialog-title"><h2 id={titleId}>{title}</h2><button type="button" className="icon-button" aria-label="Close dialog" onClick={onClose} disabled={busy}><X /></button></div><p id={descriptionId}>{description}</p>{error && <ErrorBox message={error} />}<div className="button-row"><button type="button" className="button secondary" disabled={busy} onClick={onClose}>Go back</button><Submit busy={busy}>{actionLabel}</Submit></div></form></dialog>
}
