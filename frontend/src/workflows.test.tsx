import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { NewBookingPage } from './pages/UserPages'
import { AvailabilityForm } from './pages/PublicPages'
import { AuthPage } from './pages/AuthPages'
import { Protected } from './components/Layout'
import { api } from './services/api'
import { useAuth } from './context/AuthContext'
vi.mock('./services/api', () => ({ api: { post: vi.fn() }, errorMessage: (error: Error) => error.message }))
vi.mock('./context/AuthContext', () => ({ useAuth: vi.fn() }))
vi.mock('./hooks/useResource', () => ({ useResource: () => ({ data: { registrationEnabled: true }, loading: false, error: '', reload: vi.fn() }) }))
const guest = { id: 'guest-id', fullName: 'Test Visitor', email: 'visitor@example.test', phone: '', role: 'USER' as const }
beforeEach(() => { vi.clearAllMocks(); vi.mocked(useAuth).mockReturnValue({ user: guest, accept: vi.fn(), logout: vi.fn(), updateUser: vi.fn(), expired: false }) })
describe('Accommodation request', () => {
  it('updates departure limits on date input and remains usable when dates are cleared', () => {
    render(<MemoryRouter><AvailabilityForm /></MemoryRouter>)
    fireEvent.input(screen.getByLabelText('Check-in'), { target: { value: '2026-12-01' } })
    expect(screen.getByLabelText('Check-out')).toHaveAttribute('min', '2026-12-02')
    fireEvent.input(screen.getByLabelText('Check-in'), { target: { value: '' } })
    expect(screen.getByRole('button', { name: /Check availability/ })).toBeEnabled()
  })
  it('requires an extra bed for three guests and sends a pending request payload', async () => {
    vi.mocked(api.post).mockResolvedValue({ data: { id: 'booking-123' } })
    render(<MemoryRouter initialEntries={['/?roomId=room-101']}><Routes><Route path="/" element={<NewBookingPage />} /><Route path="/app/bookings/:id" element={<h1>Request received</h1>} /></Routes></MemoryRouter>)
    const user = userEvent.setup()
    await user.selectOptions(screen.getByLabelText(/Number of guests/), '3')
    expect(screen.getByRole('checkbox')).toBeChecked()
    expect(screen.getByRole('checkbox')).toBeDisabled()
    await user.type(screen.getByLabelText(/Purpose of visit/), 'Official workshop')
    await user.click(screen.getByRole('button', { name: /Submit booking request/ }))
    await screen.findByRole('heading', { name: 'Request received' })
    expect(api.post).toHaveBeenCalledWith('/bookings', expect.objectContaining({ guests: 3, extraBedRequested: true, preferredCategory: 'STANDARD', roomId: 'room-101' }))
  })
  it('does not request unsupported extra beds for special accommodation', async () => {
    vi.mocked(api.post).mockResolvedValue({ data: { id: 'special-123' } })
    render(<MemoryRouter><NewBookingPage /></MemoryRouter>)
    const user = userEvent.setup()
    await user.selectOptions(screen.getByLabelText(/Accommodation preference/), 'SPECIAL')
    expect(screen.queryByRole('checkbox')).not.toBeInTheDocument()
    await user.type(screen.getByLabelText(/Purpose of visit/), 'Official visit')
    await user.click(screen.getByRole('button', { name: /Submit booking request/ }))
    await waitFor(() => expect(api.post).toHaveBeenCalledWith('/bookings', expect.objectContaining({ preferredCategory: 'SPECIAL', extraBedRequested: false })))
  })
})
describe('Authentication and routes', () => {
  it('opens the login form after successful registration', async () => {
    vi.mocked(useAuth).mockReturnValue({ user: null, accept: vi.fn(), logout: vi.fn(), updateUser: vi.fn(), expired: false })
    vi.mocked(api.post).mockResolvedValue({ data: guest })
    render(<MemoryRouter initialEntries={['/register']}><Routes><Route path="/register" element={<AuthPage register />} /><Route path="/login" element={<AuthPage />} /></Routes></MemoryRouter>)
    const user = userEvent.setup()
    await user.type(screen.getByLabelText('Full name'), 'Test Visitor')
    await user.type(screen.getByLabelText('Email address'), 'visitor@example.test')
    await user.type(screen.getByLabelText(/Password/), 'a-long-password!')
    await user.click(screen.getByRole('button', { name: /Create account/ }))
    await user.click(await screen.findByRole('link', { name: /Continue to sign in/ }))
    expect(await screen.findByRole('heading', { name: 'Welcome back.' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /^Sign in/ })).toBeInTheDocument()
  })
  it('sends anonymous visitors to sign in', () => {
    vi.mocked(useAuth).mockReturnValue({ user: null, accept: vi.fn(), logout: vi.fn(), updateUser: vi.fn(), expired: false })
    render(<MemoryRouter initialEntries={['/private']}><Routes><Route element={<Protected />}><Route path="/private" element={<h1>Private</h1>} /></Route><Route path="/login" element={<h1>Sign in first</h1>} /></Routes></MemoryRouter>)
    expect(screen.getByRole('heading', { name: 'Sign in first' })).toBeInTheDocument()
  })
  it('blocks a USER from administrator screens', () => {
    render(<MemoryRouter initialEntries={['/admin']}><Routes><Route element={<Protected admin />}><Route path="/admin" element={<h1>Admin content</h1>} /></Route><Route path="/forbidden" element={<h1>Access restricted</h1>} /></Routes></MemoryRouter>)
    expect(screen.getByRole('heading', { name: 'Access restricted' })).toBeInTheDocument()
  })
  it('shows server login errors and permits password visibility toggle', async () => {
    vi.mocked(useAuth).mockReturnValue({ user: null, accept: vi.fn(), logout: vi.fn(), updateUser: vi.fn(), expired: false })
    vi.mocked(api.post).mockRejectedValue(new Error('Invalid credentials'))
    render(<MemoryRouter><AuthPage /></MemoryRouter>)
    const user = userEvent.setup(); const password = screen.getByLabelText('Password')
    await user.type(screen.getByLabelText('Email address'), 'visitor@example.test')
    await user.type(password, 'incorrect-password')
    await user.click(screen.getByRole('button', { name: 'Show password' }))
    expect(password).toHaveAttribute('type', 'text')
    await user.click(screen.getByRole('button', { name: /^Sign in/ }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Invalid credentials')
    expect(screen.getByRole('button', { name: /^Sign in/ })).toBeEnabled()
  })
})
