import type { GenerateEssayReviewOptions, EssayReviewRequest, EssayChatRequest, EssaySession } from '../types/essayReview'
import { flushSseRemainder, processSseChunk } from '../utils/sseStream'

const ESSAY_REVIEW_URL = '/api/essay/review'
const ESSAY_CHAT_URL = '/api/essay/chat'
const ESSAY_SESSION_URL = '/api/essay/session'

function buildHeaders(): HeadersInit {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    Accept: 'text/event-stream',
    'Cache-Control': 'no-cache',
  }

  const token = import.meta.env.VITE_API_TOKEN?.trim()
  if (token) {
    headers.Authorization = token.startsWith('Bearer ') ? token : `Bearer ${token}`
  }

  return headers
}

async function readStreamBody(
  reader: ReadableStreamDefaultReader<Uint8Array>,
  isSse: boolean,
  onEvent: (data: string) => void,
): Promise<void> {
  const decoder = new TextDecoder()
  let sseRemainder = ''
  let snapshot = ''

  const emitEvent = (piece: string): void => {
    if (!piece) {
      return
    }
    snapshot += piece
    onEvent(snapshot)
  }

  const ingestSse = (raw: string): void => {
    sseRemainder += raw
    sseRemainder = processSseChunk(sseRemainder, emitEvent)
  }

  while (true) {
    const { done, value } = await reader.read()
    if (done) {
      break
    }

    const chunk = decoder.decode(value, { stream: true })
    if (isSse) {
      ingestSse(chunk)
    } else {
      emitEvent(chunk)
    }
  }

  const tail = decoder.decode()
  if (tail) {
    if (isSse) {
      ingestSse(tail)
    } else {
      emitEvent(tail)
    }
  }

  if (isSse && sseRemainder.trim()) {
    flushSseRemainder(sseRemainder, emitEvent)
  }
}

export async function generateEssayReview(
  request: EssayReviewRequest,
  options: GenerateEssayReviewOptions,
): Promise<void> {
  // 使用 POST 请求，传递 JSON body
  const response = await fetch(ESSAY_REVIEW_URL, {
    method: 'POST',
    headers: buildHeaders(),
    body: JSON.stringify(request),
    signal: options.signal,
  })

  if (!response.ok) {
    const errorText = await response.text().catch(() => '')
    throw new Error(
      `Request failed: ${response.status} ${response.statusText}${errorText ? `\n${errorText}` : ''}`,
    )
  }

  const reader = response.body?.getReader()
  if (!reader) {
    const text = await response.text()
    if (text) {
      options.onEvent(text)
    }
    return
  }

  const contentType = (response.headers.get('content-type') ?? '').toLowerCase()
  const isSse = contentType.includes('text/event-stream')

  await readStreamBody(reader, isSse, options.onEvent)
}

/**
 * 多轮对话（支持追问）
 */
export async function chatWithEssayReview(
  request: EssayChatRequest,
  options: GenerateEssayReviewOptions & { onSessionId?: (sessionId: string) => void },
): Promise<void> {
  const response = await fetch(ESSAY_CHAT_URL, {
    method: 'POST',
    headers: buildHeaders(),
    body: JSON.stringify(request),
    signal: options.signal,
  })

  if (!response.ok) {
    const errorText = await response.text().catch(() => '')
    throw new Error(
      `Request failed: ${response.status} ${response.statusText}${errorText ? `\n${errorText}` : ''}`,
    )
  }

  const reader = response.body?.getReader()
  if (!reader) {
    const text = await response.text()
    if (text) {
      options.onEvent(text)
    }
    return
  }

  const contentType = (response.headers.get('content-type') ?? '').toLowerCase()
  const isSse = contentType.includes('text/event-stream')

  // 自定义读取逻辑，解析第一个事件获取 sessionId
  const decoder = new TextDecoder()
  let sseRemainder = ''
  let snapshot = ''
  let sessionIdExtracted = false

  const emitEvent = (piece: string): void => {
    if (!piece) {
      return
    }
    snapshot += piece
    options.onEvent(snapshot)
  }

  const ingestSse = (raw: string): void => {
    sseRemainder += raw
    
    // 按行分割，但保留最后一行（可能不完整）
    const lines = sseRemainder.split('\n')
    // 最后一行可能是不完整的，保留到下次处理
    sseRemainder = lines.pop() || ''
    
    for (const line of lines) {
      if (line.startsWith('data: ')) {
        const data = line.substring(6)
        
        // 尝试解析 sessionId（第一个事件）
        if (!sessionIdExtracted) {
          try {
            const parsed = JSON.parse(data)
            if (parsed.sessionId) {
              options.onSessionId?.(parsed.sessionId)
              sessionIdExtracted = true
              continue // 跳过这个事件，不传递给 onEvent
            }
          } catch (e) {
            // 不是 JSON，正常内容
          }
        }
        
        // 正常内容事件
        emitEvent(data)
      } else if (line.trim() === '') {
        // 空行，忽略
        continue
      }
    }
  }

  while (true) {
    const { done, value } = await reader.read()
    if (done) {
      break
    }

    const chunk = decoder.decode(value, { stream: true })
    if (isSse) {
      ingestSse(chunk)
    } else {
      emitEvent(chunk)
    }
  }

  const tail = decoder.decode()
  if (tail) {
    if (isSse) {
      ingestSse(tail)
    } else {
      emitEvent(tail)
    }
  }

  // 处理最后剩余的不完整行
  if (isSse && sseRemainder.trim()) {
    // 如果剩余的是 data: 开头，需要提取内容
    if (sseRemainder.startsWith('data: ')) {
      const data = sseRemainder.substring(6)
      
      // 检查是否是 sessionId
      if (!sessionIdExtracted) {
        try {
          const parsed = JSON.parse(data)
          if (parsed.sessionId) {
            options.onSessionId?.(parsed.sessionId)
            sessionIdExtracted = true
          } else {
            emitEvent(data)
          }
        } catch (e) {
          emitEvent(data)
        }
      } else {
        emitEvent(data)
      }
    } else if (sseRemainder.trim()) {
      // 其他内容，直接输出
      emitEvent(sseRemainder)
    }
  }
}

/**
 * 获取会话信息
 */
export async function getEssaySession(sessionId: string): Promise<EssaySession> {
  const response = await fetch(`${ESSAY_SESSION_URL}/${sessionId}`)
  
  if (!response.ok) {
    throw new Error(`Failed to get session: ${response.status} ${response.statusText}`)
  }
  
  return response.json()
}

/**
 * 清除会话
 */
export async function clearEssaySession(sessionId: string): Promise<void> {
  const response = await fetch(`${ESSAY_SESSION_URL}/${sessionId}`, {
    method: 'DELETE',
  })
  
  if (!response.ok) {
    throw new Error(`Failed to clear session: ${response.status} ${response.statusText}`)
  }
}
