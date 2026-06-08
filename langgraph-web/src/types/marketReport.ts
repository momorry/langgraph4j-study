export interface MarketReportRequest {
  startDate: string
  endDate: string
  stockCodes: string[]
}

export type ChatMessageRole = 'user' | 'assistant' | 'error'

export interface ChatMessage {
  id: string
  role: ChatMessageRole
  content: string
  streaming?: boolean
}

export interface GenerateMarketReportOptions {
  signal?: AbortSignal
  /** 当前已接收全文快照（由 SSE 增量帧在读取层拼接） */
  onEvent: (data: string) => void
}

/** 工作流节点进度事件（对应后端 NodeDetail） */
export interface TimelineNode {
  name: string
  message: string
  data: string
  status: 'running' | 'done' | 'error'
  costTime: string
}

export interface GenerateMarketReportV3Options {
  signal?: AbortSignal
  /** 节点进度回调 */
  onNode: (node: TimelineNode) => void
  /** 内容增量回调（打字机效果） */
  onContent: (snapshot: string) => void
}
