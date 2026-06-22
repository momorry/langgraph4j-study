import { flushSseRemainder, processSseChunk } from '../utils/sseStream'

export interface CreateChatResponse {
  code: number
  msg: string
  data: {
    id: string
    appId: string
    chatName: string
    modelName: string
  }
}

export interface ChatItem {
  id: string
  appId: string
  chatName: string
  modelName: string
  createTime: string
}

export interface ChatListResponse {
  code: number
  msg: string
  data: {
    records: ChatItem[]
    total: number
    size: number
    current: number
    pages: number
  }
}

export interface MessageItem {
  id: string
  chatId: string
  appId: string
  modelName: string
  messageType: string
  message: string
  thinkMessage: string | null
  sort: string
  createTime: string
}

export interface MessageListResponse {
  code: number
  msg: string
  data: MessageItem[]
}

export interface StreamChatRequest {
  appId: string
  modelName?: string
  question: string
  chatId: string
  messageId?: string
}

export interface StreamChatOptions {
  signal?: AbortSignal
  onChunk: (text: string) => void
  onDone: () => void
}

const CREATE_CHAT_URL = '/api/chat/create-chat'
const CHAT_LIST_URL = '/api/chat/list'
const CHAT_MESSAGES_URL = '/api/chat'
const STREAM_CHAT_URL = '/api/stream-chat'

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

/** 创建会话 */
export async function createChat(appId: string, chatName: string, modelName?: string): Promise<CreateChatResponse> {
  const response = await fetch(CREATE_CHAT_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ appId, chatName, modelName }),
  })

  if (!response.ok) {
    throw new Error(`创建会话失败: ${response.status} ${response.statusText}`)
  }

  return response.json() as Promise<CreateChatResponse>
}

/** 查询会话列表（分页） */
export async function listChats(appId: string, pageNum = 1, pageSize = 50): Promise<ChatListResponse> {
  const url = `${CHAT_LIST_URL}?appId=${appId}&pageNum=${pageNum}&pageSize=${pageSize}`
  const response = await fetch(url, { headers: { 'Content-Type': 'application/json' } })
  if (!response.ok) {
    throw new Error(`查询会话列表失败: ${response.status}`)
  }
  return response.json() as Promise<ChatListResponse>
}

/** 查询会话消息历史 */
export async function listMessages(chatId: string): Promise<MessageListResponse> {
  const url = `${CHAT_MESSAGES_URL}/${chatId}/messages`
  const response = await fetch(url, { headers: { 'Content-Type': 'application/json' } })
  if (!response.ok) {
    throw new Error(`查询消息历史失败: ${response.status}`)
  }
  return response.json() as Promise<MessageListResponse>
}

/** 流式对话 */
export async function streamChat(request: StreamChatRequest, options: StreamChatOptions): Promise<void> {
  const response = await fetch(STREAM_CHAT_URL, {
    method: 'POST',
    headers: buildHeaders(),
    body: JSON.stringify(request),
    signal: options.signal,
  })

  if (!response.ok) {
    const errorText = await response.text().catch(() => '')
    throw new Error(`请求失败: ${response.status} ${response.statusText}${errorText ? `\n${errorText}` : ''}`)
  }

  const reader = response.body?.getReader()
  if (!reader) {
    throw new Error('无法获取响应流')
  }

  const decoder = new TextDecoder()
  let sseRemainder = ''
  let fullText = ''

  const handleEvent = (piece: string): void => {
    // 后端直接返回纯文本 chunk
    fullText += piece
    options.onChunk(fullText)
  }

  while (true) {
    const { done, value } = await reader.read()
    if (done) break

    const chunk = decoder.decode(value, { stream: true })
    sseRemainder += chunk
    sseRemainder = processSseChunk(sseRemainder, handleEvent)
  }

  // 处理尾部残留
  if (sseRemainder.trim()) {
    flushSseRemainder(sseRemainder, handleEvent)
  }

  options.onDone()
}
