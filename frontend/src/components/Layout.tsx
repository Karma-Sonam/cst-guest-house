import { useEffect, useRef, useState } from 'react'
import { Link, NavLink, Navigate, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { BedDouble, Building2, CalendarDays, ChevronRight, ClipboardList, LayoutDashboard, LogOut, Menu, UserRound, Users, X, ArrowUpRight, ShieldCheck } from 'lucide-react'
import { useAuth } from '../context/AuthContext'
import { today, date } from './ui'
export function Brand({ light = false }: { light?: boolean }) { return <Link to="/" className={`brand ${light ? 'brand-light' : ''}`}><img src="/images/logo.jpg" alt="CST logo" /><span>CST <b>Guest House</b><small>College of Science and Technology</small></span></Link> }
export function ScrollToTop() { const { pathname } = useLocation(); useEffect(() => { window.scrollTo(0, 0); document.title = 'CST Guest House' }, [pathname]); return null }
function useMenuEscape(open: boolean, close: () => void) {
  const trigger = useRef<HTMLButtonElement>(null)
  useEffect(() => {
    if (!open) return
    function escape(event: KeyboardEvent) { if (event.key === 'Escape') { event.preventDefault(); close(); trigger.current?.focus() } }
    document.addEventListener('keydown', escape)
    return () => document.removeEventListener('keydown', escape)
  }, [open, close])
  return trigger
}
export function PublicLayout() {
  const [open, setOpen] = useState(false); const { user } = useAuth(); const location = useLocation()
  const menuTrigger = useMenuEscape(open, () => setOpen(false))
  useEffect(() => { setOpen(false) }, [location.pathname])
  return <><a href="#main" className="skip-link">Skip to content</a><div className="institution-bar"><div className="container"><span>A welcoming stay. A purposeful visit.</span><span><ShieldCheck size={13} /> Official college guest house</span></div></div><header className="public-header"><div className="container header-inner"><Brand /><button ref={menuTrigger} className="icon-button mobile-menu" aria-controls="public-navigation" aria-label={open ? 'Close menu' : 'Open menu'} aria-expanded={open} onClick={() => setOpen(!open)}>{open ? <X /> : <Menu />}</button><nav id="public-navigation" className={`public-nav ${open ? 'is-open' : ''}`} aria-label="Main navigation">{[['/', 'Home'], ['/rooms', 'Rooms'], ['/facilities', 'Facilities'], ['/about', 'About'], ['/contact', 'Contact']].map(([to, text]) => <NavLink end to={to} key={to}>{text}</NavLink>)}<Link className="button small" to={user ? user.role === 'ADMIN' ? '/admin' : '/app' : '/login'}>{user ? 'My dashboard' : 'Sign in'}<ArrowUpRight size={16} /></Link></nav></div></header><main id="main"><Outlet /></main><footer className="public-footer"><div className="container footer-top"><div><Brand light /><p>A place to settle in, connect,<br />and make your visit count.</p></div><div><h3>Explore</h3><Link to="/rooms">Accommodation</Link><Link to="/facilities">Our facilities</Link><Link to="/about">About the guest house</Link></div><div><h3>Your stay</h3><Link to="/app/availability">Check availability</Link><Link to="/app/bookings/new">Request accommodation</Link><Link to="/contact">Contact & enquiries</Link></div><div><span className="footer-kicker">Here for your visit</span><p>For official visitors, college guests<br />and distinguished visitors.</p><Link to="/login" className="footer-action">Plan your stay <ChevronRight size={17} /></Link></div></div><div className="container footer-bottom"><span>© {new Date().getFullYear()} CST Guest House</span><span>College accommodation & facility management</span></div></footer></>
}
export function Protected({ admin = false }: { admin?: boolean }) {
  const { user } = useAuth(); const location = useLocation()
  if (!user) return <Navigate to="/login" state={{ from: location.pathname + location.search }} replace />
  if (admin && user.role !== 'ADMIN') return <Navigate to="/forbidden" replace />
  return <Outlet />
}
export function AppLayout({ admin = false }: { admin?: boolean }) {
  const { user, logout } = useAuth(); const navigate = useNavigate(); const location = useLocation(); const [open, setOpen] = useState(false)
  const menuTrigger = useMenuEscape(open, () => setOpen(false))
  useEffect(() => { setOpen(false) }, [location.pathname])
  const links = admin ? [
    ['/admin', 'Overview', LayoutDashboard], ['/admin/bookings', 'Booking requests', ClipboardList], ['/admin/rooms', 'Room management', BedDouble], ['/admin/special-rooms', 'Special rooms', ShieldCheck], ['/admin/convention-hall', 'Convention hall', Building2], ['/admin/dining-hall', 'Dining hall', Building2], ['/admin/current-guests', 'Current guests', Users], ['/admin/arrivals', 'Arrivals & departures', CalendarDays], ['/admin/users', 'User accounts', Users], ['/admin/reports', 'Reports', ClipboardList], ['/admin/profile', 'My profile', UserRound],
  ] as const : [['/app', 'Overview', LayoutDashboard], ['/app/availability', 'Find accommodation', BedDouble], ['/app/bookings', 'My bookings', ClipboardList], ['/app/facilities', 'Facility requests', Building2], ['/app/profile', 'My profile', UserRound]] as const
  return <div className="app-shell"><a href="#main" className="skip-link">Skip to content</a><aside id="portal-navigation" className={`sidebar ${open ? 'is-open' : ''}`}><Brand light /><p className="sidebar-caption">{admin ? 'Administration' : 'Your guest house'}</p><nav aria-label={admin ? 'Administrator navigation' : 'User navigation'}>{links.map(([to, text, Icon]) => <NavLink key={to} to={to} end={to === '/app' || to === '/admin'}><Icon size={19} /><span>{text}</span></NavLink>)}</nav>{!admin && user?.role === 'ADMIN' && <Link className="sidebar-switch" to="/admin"><ShieldCheck size={17} />Administration</Link>}<div className="sidebar-bottom"><div className="sidebar-help"><ShieldCheck size={21} /><strong>Every stay, thoughtfully managed.</strong><p>Your request is confirmed after administrative approval.</p></div><Link to="/">Back to website <ArrowUpRight size={15} /></Link><button onClick={async () => { try { await logout() } catch { /* Local session is cleared even if server is unavailable. */ } navigate('/login') }}><LogOut size={18} />Sign out</button></div></aside>{open && <button className="sidebar-overlay" aria-label="Close navigation" onClick={() => setOpen(false)} />}<div className="app-main"><header className="app-header"><div className="app-breadcrumb"><button ref={menuTrigger} className="icon-button mobile-menu" aria-controls="portal-navigation" aria-label={open ? 'Close navigation' : 'Open navigation'} aria-expanded={open} onClick={() => setOpen(!open)}><Menu /></button><span>Guest House</span><ChevronRight size={14} /><strong>{admin ? 'Administration' : 'Guest portal'}</strong></div><div className="account"><span className="header-date">{date(today())}</span><span className="avatar">{user?.fullName.trim().slice(0, 1).toUpperCase()}</span><span><strong>{user?.fullName}</strong><small>{admin ? 'Administrator' : 'Guest account'}</small></span></div></header><main id="main" className="workspace"><Outlet /></main><footer className="app-footer">CST Guest House <span>Thoughtful stays. Simple requests.</span></footer></div></div>
}
