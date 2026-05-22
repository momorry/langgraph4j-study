export interface EssayReviewRequest {
  studentName: string
  essayTopic: string
}

/**
 * 多轮对话请求
 */
export interface EssayChatRequest {
  /** 会话 ID（可选，首次请求可为空） */
  sessionId?: string
  /** 学生姓名（首次请求时提供） */
  studentName?: string
  /** 作文题目（首次请求时提供） */
  essayTopic?: string
  /** 用户消息（首次请求时为作文内容，后续为追问） */
  message: string
}

/**
 * 会话信息
 */
export interface EssaySession {
  sessionId: string
  studentName: string
  essayTopic: string
  essayContent: string
  teacherComment: string
  essayGrade: string
  state: 'INITIAL' | 'SUBMITTED' | 'REVIEWED' | 'FOLLOWING_UP'
  createdAt: string
  updatedAt: string
}

export type EssayMessageRole = 'user' | 'assistant' | 'error' | 'info'

export interface EssayMessage {
  id: string
  role: EssayMessageRole
  content: string
  streaming?: boolean
}

export interface GenerateEssayReviewOptions {
  signal?: AbortSignal
  /** 当前已接收全文快照（由 SSE 增量帧在读取层拼接） */
  onEvent: (data: string) => void
}
