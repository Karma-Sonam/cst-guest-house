import { useEffect, useState, type FormEvent } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { ArrowLeft, ArrowRight, Eye, EyeOff, ShieldCheck } from 'lucide-react'
import { useAuth } from '../context/AuthContext'
import { useResource } from '../hooks/useResource'
import { api, errorMessage } from '../services/api'
import type { Config, Token } from '../types'
import { Field, ErrorBox, Submit, Loading } from '../components/ui'
export function AuthPage({ register = false }: { register?: boolean }) {
  const { user, accept, expired } = useAuth(); const navigate = useNavigate(); const location = useLocation(); const config = useResource<Config>('/public/config')
  const [visible, setVisible] = useState(false); const [busy, setBusy] = useState(false); const [error, setError] = useState(''); const [registered, setRegistered] = useState(false)
  useEffect(() => { setRegistered(false); setError(''); setVisible(false) }, [register])
  if (user) return <Navigate to={user.role === 'ADMIN' ? '/admin' : '/app'} replace />
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); const form = new FormData(event.currentTarget); setBusy(true); setError('')
    const email = String(form.get('email')).trim(); const password = String(form.get('password'))
    try {
      if (register) {
        if (new TextEncoder().encode(password).length > 72) throw new Error('Your password must be at most 72 UTF-8 bytes.')
        await api.post('/auth/register', { fullName: form.get('fullName'), phone: form.get('phone'), email, password })
        setRegistered(true)
      } else {
        const response = await api.post<Token>('/auth/login', { email, password }); accept(response.data)
        const from = location.state?.from
        navigate(typeof from === 'string' && from.startsWith('/') && !from.startsWith('//') && !from.startsWith('/login') ? from : response.data.user.role === 'ADMIN' ? '/admin' : '/app', { replace: true })
      }
    } catch (err) { setError(errorMessage(err)) } finally { setBusy(false) }
  }
  return <div className="container auth-page"><Link to="/" className="quiet-link"><ArrowLeft size={16} />Back to the guest house</Link><div className="auth-grid"><div className="auth-story"><img src="/images/campus.jpg" alt="College campus" /><div><span className="eyebrow">YOUR VISIT STARTS HERE</span><h2>A place to stay.<br />A community to connect with.</h2><p>Request accommodation, arrange shared spaces, and follow every step of your visit.</p><span><ShieldCheck size={19} /> Managed by the college administration</span></div></div><section className="auth-card">{registered ? <><span className="success-mark"><ShieldCheck size={32} /></span><h1>Your account is ready.</h1><p>Sign in with your email and password to start planning your visit.</p><Link className="button" to="/login" state={location.state}>Continue to sign in<ArrowRight size={17} /></Link></> : <><p className="eyebrow">CST GUEST HOUSE</p><h1>{register ? 'Make yourself at home.' : 'Welcome back.'}</h1><p className="muted">{register ? 'Create an account to request accommodation and facilities.' : 'Sign in to manage your stay and facility requests.'}</p>{expired && <div className="notice small-notice" role="status">Your session has ended. Please sign in again.</div>}{config.loading && register ? <Loading /> : config.error && register ? <ErrorBox message={config.error} retry={config.reload} /> : register && !config.data?.registrationEnabled ? <div className="notice">Registration is currently closed. Please contact the college administrator for an account.</div> : <form onSubmit={submit} className="stack-form">{register && <Field label="Full name"><input name="fullName" autoComplete="name" required maxLength={120} /></Field>}<Field label="Email address"><input name="email" type="email" autoComplete="email" placeholder="you@example.com" required maxLength={254} /></Field>{register && <Field label="Phone number (optional)"><input name="phone" type="tel" autoComplete="tel" maxLength={30} /></Field>}<Field label="Password" hint={register ? 'At least 12 characters. Use a strong, unique password.' : undefined}><span className="password-field"><input name="password" type={visible ? 'text' : 'password'} autoComplete={register ? 'new-password' : 'current-password'} minLength={register ? 12 : undefined} maxLength={72} required /><button type="button" aria-label={visible ? 'Hide password' : 'Show password'} onClick={() => setVisible(!visible)}>{visible ? <EyeOff size={19} /> : <Eye size={19} />}</button></span></Field>{error && <ErrorBox message={error} />}<Submit busy={busy}>{register ? 'Create account' : 'Sign in'}<ArrowRight size={17} /></Submit></form>}<p className="auth-switch">{register ? <>Already have an account? <Link to="/login" state={location.state}>Sign in</Link></> : config.data?.registrationEnabled ? <>New to the guest house? <Link to="/register" state={location.state}>Create an account</Link></> : 'Need an account? Contact the college administrator.'}</p><p className="auth-note">Your session ends when this page is refreshed or closed.</p></>}</section></div></div>
}
