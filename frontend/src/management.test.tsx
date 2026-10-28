import { beforeEach, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { UsersPage, ReportsPage } from './pages/ManagementPages'
import { AdminBookingsPage } from './pages/AdminPages'
import { RoomDetailsEditor } from './components/RoomDetailsEditor'
import { MemoryRouter } from 'react-router-dom'
import { api } from './services/api'
import { useResource } from './hooks/useResource'
const reload = vi.fn()
vi.mock('./context/AuthContext', () => ({ useAuth: () => ({ user: { id: 'admin' } }) }))
vi.mock('./services/api', () => ({ api: { patch: vi.fn(), put: vi.fn() }, errorMessage: (error: Error) => error.message }))
vi.mock('./hooks/useResource', () => ({ useResource: vi.fn() }))
beforeEach(() => { vi.clearAllMocks() })
it('saves descriptive room fields without changing standard capacity or category', async () => {
  vi.mocked(api.put).mockResolvedValue({ data: {} })
  const saved = vi.fn()
  render(<RoomDetailsEditor room={{ id: 'room', roomNumber: '101', category: 'STANDARD', beds: 2, normalCapacity: 2, maxCapacity: 3, supportsExtraBed: true, serviceStatus: 'ACTIVE', extraBedRequired: false }} onSaved={saved} onClose={vi.fn()} />)
  expect(screen.getByLabelText(/Regular beds/)).toBeDisabled()
  fireEvent.change(screen.getByLabelText(/Location/), { target: { value: 'Test wing' } })
  fireEvent.change(screen.getByLabelText('Description'), { target: { value: 'Verified description' } })
  fireEvent.click(screen.getByRole('button', { name: 'Save room details' }))
  await waitFor(() => expect(saved).toHaveBeenCalled())
  expect(api.put).toHaveBeenCalledWith('/admin/rooms/room', { roomNumber: '101', beds: 2, location: 'Test wing', description: 'Verified description', amenities: '' })
})
it('combines encoded booking search with the selected status', () => {
  vi.mocked(useResource).mockReturnValue({ data: null, loading: false, error: '', reload })
  render(<MemoryRouter><AdminBookingsPage /></MemoryRouter>)
  fireEvent.change(screen.getByLabelText('Search bookings'), { target: { value: 'guest+team@example.test' } })
  fireEvent.change(screen.getByLabelText('Booking status'), { target: { value: 'APPROVED' } })
  expect(useResource).toHaveBeenCalledWith('/admin/bookings?page=0&size=10&status=APPROVED&search=guest%2Bteam%40example.test')
})
it('requires confirmation before deactivation and prevents changing your own access', async () => {
  vi.mocked(useResource).mockReturnValue({ data: { content: [{ id: 'admin', fullName: 'Local Admin', email: 'admin@example.test', role: 'ADMIN', active: true }, { id: 'guest', fullName: 'Guest Account', email: 'guest@example.test', role: 'USER', active: true }], totalElements: 2 }, loading: false, error: '', reload })
  vi.mocked(api.patch).mockResolvedValue({ data: {} })
  render(<UsersPage />)
  expect(screen.getByRole('button', { name: 'Your account' })).toBeDisabled()
  fireEvent.click(screen.getByRole('button', { name: 'Deactivate' }))
  expect(api.patch).not.toHaveBeenCalled()
  expect(screen.getByText(/Existing bookings will not be cancelled/)).toBeInTheDocument()
  fireEvent.click(screen.getByRole('button', { name: 'Confirm change' }))
  await waitFor(() => expect(api.patch).toHaveBeenCalledWith('/admin/users/guest/active', { active: false }))
  await screen.findByRole('status')
  expect(reload).toHaveBeenCalled()
})
it('generates the requested report with explicit end-date semantics', () => {
  vi.mocked(useResource).mockReturnValue({ data: null, loading: false, error: '', reload })
  render(<ReportsPage />)
  fireEvent.input(screen.getByLabelText('Start date (included)'), { target: { value: '2026-10-01' } })
  fireEvent.input(screen.getByLabelText('End date (excluded)'), { target: { value: '2026-11-01' } })
  fireEvent.click(screen.getByRole('button', { name: 'Generate report' }))
  expect(useResource).toHaveBeenLastCalledWith('/admin/reports/summary?from=2026-10-01&to=2026-11-01')
})
