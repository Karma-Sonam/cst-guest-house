import { act, renderHook } from '@testing-library/react'
import { afterEach, expect, it, vi } from 'vitest'
import { useResource } from './hooks/useResource'
import { api } from './services/api'
vi.mock('./services/api', () => ({ api: { get: vi.fn() }, errorMessage: (error: Error) => error.message }))
afterEach(() => { vi.useRealTimers(); vi.clearAllMocks() })

it('refreshes live data every three seconds without clearing the existing view', async () => {
  vi.useFakeTimers()
  vi.mocked(api.get).mockResolvedValueOnce({ data: { status: 'PENDING' } }).mockResolvedValue({ data: { status: 'APPROVED' } })
  const { result, unmount } = renderHook(() => useResource<{ status: string }>('/bookings/example'))
  await act(async () => { await Promise.resolve() })
  expect(result.current.data?.status).toBe('PENDING')
  await act(async () => { await vi.advanceTimersByTimeAsync(3000) })
  expect(result.current.data?.status).toBe('APPROVED')
  expect(result.current.loading).toBe(false)
  unmount()
  await act(async () => { await vi.advanceTimersByTimeAsync(6000) })
  expect(api.get).toHaveBeenCalledTimes(2)
})

it('refreshes immediately after a mutation and ignores an old URL response', async () => {
  let finishOld: (value: { data: string }) => void = () => {}
  vi.mocked(api.get).mockImplementationOnce(() => new Promise(resolve => { finishOld = resolve })).mockResolvedValue({ data: 'new data' })
  const { result, rerender } = renderHook(({ url }) => useResource<string>(url), { initialProps: { url: '/old' } })
  rerender({ url: '/new' })
  await act(async () => { await Promise.resolve(); finishOld({ data: 'stale data' }); await Promise.resolve() })
  expect(result.current.data).toBe('new data')
  await act(async () => { window.dispatchEvent(new Event('data-changed')); await Promise.resolve() })
  expect(api.get).toHaveBeenCalledTimes(3)
})
