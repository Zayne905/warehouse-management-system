import request from './request'

export interface ChatMessage {
  role: 'user' | 'assistant' | 'system'
  content: string
  timestamp?: number
}

/**
 * Send a message to AI via SSE (streaming).
 * Returns an AbortController to cancel the stream.
 */
export function streamChat(
  message: string,
  sessionId: string,
  onChunk: (text: string) => void,
  onDone: () => void,
  onError: (err: string) => void
): AbortController {
  const controller = new AbortController()

  fetch('/api/ai/chat', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${localStorage.getItem('token')}`,
    },
    body: JSON.stringify({ message, sessionId }),
    signal: controller.signal,
  })
    .then(async (response) => {
      if (!response.ok) {
        onError('AI 服务请求失败: ' + response.status)
        return
      }
      const reader = response.body?.getReader()
      if (!reader) {
        onError('无法读取响应流')
        return
      }
      const decoder = new TextDecoder()
      let buffer = ''

      while (true) {
        const { done, value } = await reader.read()
        if (done) break

        buffer += decoder.decode(value, { stream: true })
        // Parse SSE events
        const lines = buffer.split('\n')
        buffer = lines.pop() || ''

        for (const line of lines) {
          if (line.startsWith('data:')) {
            const data = line.substring(5).trim()
            if (data === '[DONE]' || data.startsWith('[DONE]')) {
              onDone()
              return
            }
            onChunk(data)
          } else if (line.startsWith('event:done')) {
            onDone()
            return
          } else if (line.startsWith('event:error')) {
            // next data line will be the error message
          }
        }
      }
      onDone()
    })
    .catch((err) => {
      if (err.name !== 'AbortError') {
        onError('AI 服务连接失败: ' + err.message)
      }
    })

  return controller
}

/**
 * Non-streaming chat (fallback).
 */
export async function sendChat(message: string, sessionId: string): Promise<string> {
  const res = await request.post<{ reply: string; sessionId: string }>('/ai/chat/send', {
    message,
    sessionId,
  })
  return res.data.reply
}

/**
 * Clear conversation history.
 */
export function clearSession(sessionId: string) {
  return request.post('/ai/session/clear', { sessionId })
}
