/**
 * AI 服务 API — 统一 Agent
 */
const AI = '/ai'

export interface StreamEvent {
  type: 'token' | 'tool_start' | 'tool_end' | 'done'
  content?: string; name?: string; input?: Record<string, unknown>
}

export function aiChatStream(question: string, threadId: string,
  onEvent: (e: StreamEvent) => void,
  onError?: (err: Error) => void, onDone?: () => void,
): () => void {
  const ctrl = new AbortController()
  fetch(`${AI}/chat/stream`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ question, thread_id: threadId }),
    signal: ctrl.signal,
  }).then(async resp => {
    if (!resp.ok) throw new Error(`HTTP ${resp.status}`)
    const reader = resp.body!.getReader()
    const dec = new TextDecoder()
    let buf = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buf += dec.decode(value, { stream: true })

      // 按 \n 拆行，保留不完整的最后一行在 buf 中
      let idx: number
      while ((idx = buf.indexOf('\n')) !== -1) {
        let line = buf.slice(0, idx).trim()  // trim 去掉 \r 和首尾空白
        buf = buf.slice(idx + 1)

        if (!line) continue
        if (!line.startsWith('data:')) continue

        const payload = line.slice(5).trim()  // 去掉 "data:" 前缀
        if (payload === '[DONE]') {
          onEvent({ type: 'done' })
          onDone?.()
          return
        }

        try {
          const evt = JSON.parse(payload) as StreamEvent
          onEvent(evt)
        } catch {
          // 忽略解析失败的行
        }
      }
    }
    onDone?.()
  }).catch(err => {
    if (err.name !== 'AbortError') onError?.(err instanceof Error ? err : new Error(String(err)))
  })
  return () => ctrl.abort()
}

export function aiChat(question: string, threadId: string) {
  return fetch(`${AI}/chat`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ question, thread_id: threadId }),
  }).then(r => r.json())
}

export function aiSessions() {
  return fetch(`${AI}/sessions`).then(r => r.json())
}

// AI 全局搜索（暂未实现）
export interface SearchResult { results: any[]; total: number }
export async function aiSearch(query: string, type?: string): Promise<{ results: any[]; total: number }> {
  return request.get('/ai/search', { params: { query, type } })
}

export function aiSessionDelete(tid: string) {
  return fetch(`${AI}/sessions/${tid}`, { method: 'DELETE' })
}
