import { ref } from 'vue'
import { getAuthHeaders } from '@/api/client'

export function useSSE() {
  const state = ref<'idle' | 'streaming' | 'done' | 'error'>('idle')
  const text = ref('')
  const thinking = ref('')
  const error = ref<string | null>(null)
  const metadata = ref<Record<string, any>>({})
  let abortController: AbortController | null = null

  async function send(url: string, body: unknown) {
    state.value = 'streaming'; text.value = ''; error.value = null
    abortController = new AbortController()
    try {
      const response = await fetch(url, {
        method: 'POST', headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
        body: JSON.stringify(body), signal: abortController.signal
      })
      if (!response.ok) throw new Error(`HTTP ${response.status}`)
      const reader = response.body!.getReader()
      const decoder = new TextDecoder()
      let buffer = '', currentEvent = ''
      while (true) {
        const { done, value } = await reader.read()
        if (done) break
        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        buffer = lines.pop() || ''
        for (const line of lines) {
          if (line.startsWith('event:')) currentEvent = line.slice(6).trim()
          else if (line.startsWith('data:')) {
            const data = line.slice(5).trim()
            if (currentEvent === 'chunk') text.value += data
            else if (currentEvent === 'thinking') thinking.value += data
            else if (currentEvent === 'metadata') { try { metadata.value = JSON.parse(data) } catch {} }
            else text.value += data
          }
        }
      }
      state.value = 'done'
    } catch (e: any) {
      if (e.name === 'AbortError') state.value = 'idle'
      else { error.value = e.message; state.value = 'error' }
    }
  }
  function abort() { abortController?.abort() }
  return { state, text, thinking, error, metadata, send, abort }
}
