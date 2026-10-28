import { useEffect, useState } from 'react'
import { api, errorMessage } from '../services/api'
export function useResource<T>(url: string | null) {
  const [data, setData] = useState<T | null>(null)
  const [loading, setLoading] = useState(!!url)
  const [error, setError] = useState('')
  const [revision, setRevision] = useState(0)
  useEffect(() => {
    const controller = new AbortController()
    setData(null); setError(''); setLoading(!!url)
    if (url) api.get<T>(url, { signal: controller.signal }).then(response => { if (!controller.signal.aborted) setData(response.data) }).catch(err => { if (!controller.signal.aborted) setError(errorMessage(err)) }).finally(() => { if (!controller.signal.aborted) setLoading(false) })
    return () => controller.abort()
  }, [url, revision])
  return { data, loading, error, reload: () => setRevision(value => value + 1) }
}
