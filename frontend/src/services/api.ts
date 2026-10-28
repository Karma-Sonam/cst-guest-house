import axios from 'axios'
export const api = axios.create({ baseURL: '/api', timeout: 20000 })
let accessToken: string | null = null
export function setAccessToken(token: string | null) { accessToken = token }
api.interceptors.request.use(config => { if (accessToken) config.headers.Authorization = `Bearer ${accessToken}`; return config })
api.interceptors.response.use(response => response, error => {
  if (error.response?.status === 401 && accessToken) { accessToken = null; window.dispatchEvent(new Event('session-expired')) }
  return Promise.reject(error)
})
export function errorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const fields = error.response?.data?.fieldErrors
    if (fields && Object.keys(fields).length) return Object.entries(fields).map(([field, message]) => `${field}: ${message}`).join('. ')
    return error.response?.data?.message || ({ 401: 'Your email or password is incorrect, or your session has expired.', 403: 'You do not have permission to perform this action.', 404: 'This record could not be found.', 409: 'This action conflicts with the current booking or schedule.' } as Record<number, string>)[error.response?.status || 0] || (error.response ? 'The server could not complete this request. Please try again.' : 'Unable to connect. Check your connection and try again.')
  }
  return error instanceof Error ? error.message : 'Something went wrong. Please try again.'
}
