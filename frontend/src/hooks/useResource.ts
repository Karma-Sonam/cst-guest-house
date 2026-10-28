import { useEffect, useState } from 'react'
import { api, errorMessage } from '../services/api'
export function useResource<T>(url: string | null) {
  const [data, setData] = useState<T | null>(null)
  const [loading, setLoading] = useState(!!url)
  const [error, setError] = useState('')
  const [revision, setRevision] = useState(0)
  useEffect(() => {
    const controller = new AbortController(); let pending = false
    setData(null); setError(''); setLoading(!!url)
    async function refresh() {
      if (!url || pending || controller.signal.aborted) return
      pending = true
      try { const response = await api.get<T>(url, { signal: controller.signal }); if (!controller.signal.aborted) { setData(response.data); setError('') } }
      catch (err) { if (!controller.signal.aborted) setError(errorMessage(err)) }
      finally { pending = false; if (!controller.signal.aborted) setLoading(false) }
    }
    const visibleRefresh = () => { if (document.visibilityState !== 'hidden') void refresh() }
    void refresh()
    const timer = window.setInterval(visibleRefresh, 3000)
    window.addEventListener('focus', visibleRefresh)
    window.addEventListener('data-changed', visibleRefresh)
    document.addEventListener('visibilitychange', visibleRefresh)
    return () => { controller.abort(); window.clearInterval(timer); window.removeEventListener('focus', visibleRefresh); window.removeEventListener('data-changed', visibleRefresh); document.removeEventListener('visibilitychange', visibleRefresh) }
  }, [url, revision])
  return { data, loading, error, reload: () => setRevision(value => value + 1) }
}
