import { createContext, useContext, useEffect, useRef, useState, type ReactNode } from 'react'
import { api, setAccessToken } from '../services/api'
import type { Token, User } from '../types'
interface Auth { user: User | null; accept: (token: Token) => void; logout: () => Promise<void>; updateUser: (user: User) => void; expired: boolean }
const Context = createContext<Auth | null>(null)
export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [expired, setExpired] = useState(false)
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null)
  function clear() { setAccessToken(null); setUser(null); if (timer.current) clearTimeout(timer.current) }
  useEffect(() => { const onExpired = () => { clear(); setExpired(true) }; window.addEventListener('session-expired', onExpired); return () => { window.removeEventListener('session-expired', onExpired); if (timer.current) clearTimeout(timer.current) } }, [])
  function accept(token: Token) { clear(); setAccessToken(token.token); setUser(token.user); setExpired(false); timer.current = setTimeout(() => { clear(); setExpired(true) }, token.expiresIn * 1000) }
  async function logout() { try { await api.post('/auth/logout') } finally { clear() } }
  return <Context.Provider value={{ user, accept, logout, updateUser: setUser, expired }}>{children}</Context.Provider>
}
export function useAuth() { const value = useContext(Context); if (!value) throw new Error('Missing AuthProvider'); return value }
