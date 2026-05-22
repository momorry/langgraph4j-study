<script setup lang="ts">
import { nextTick, ref } from 'vue'
import { chatWithEssayReview, clearEssaySession } from '../api/essayReview'
import type { EssayMessage, EssayChatRequest } from '../types/essayReview'

const studentName = ref('')
const essayTopic = ref('')
const userMessage = ref('')
const messages = ref<EssayMessage[]>([])
const isGenerating = ref(false)
const messagesContainer = ref<HTMLElement | null>(null)
const sessionId = ref<string | null>(null)
const hasSession = ref(false)

const streamingMessageId = ref<string | null>(null)

let abortController: AbortController | null = null
let messageCounter = 0

function createMessageId(): string {
  messageCounter += 1
  return `msg-${Date.now()}-${messageCounter}`
}

function appendInfo(text: string): void {
  messages.value.push({
    id: createMessageId(),
    role: 'info',
    content: text,
  })
}

function appendError(text: string): void {
  messages.value.push({
    id: createMessageId(),
    role: 'error',
    content: text,
  })
}

function validateForm(): string | null {
  // 首次请求需要填写学生姓名和作文题目
  if (!hasSession.value) {
    if (!studentName.value || !studentName.value.trim()) {
      return '请填写学生姓名。'
    }
    if (!essayTopic.value || !essayTopic.value.trim()) {
      return '请填写作文题目。'
    }
  }
  
  // 所有情况都需要填写消息
  if (!userMessage.value || !userMessage.value.trim()) {
    return '请输入消息内容。'
  }

  return null
}

function isMessageStreaming(message: EssayMessage): boolean {
  return message.role === 'assistant' && message.id === streamingMessageId.value && isGenerating.value
}

async function scrollToBottom(): Promise<void> {
  await nextTick()
  const container = messagesContainer.value
  if (container) {
    container.scrollTop = container.scrollHeight
  }
}

function stopGeneration(): void {
  abortController?.abort()
}

function clearCurrentSession(): void {
  if (sessionId.value) {
    clearEssaySession(sessionId.value).catch(err => {
      console.error('清除会话失败:', err)
    })
  }
  
  sessionId.value = null
  hasSession.value = false
  studentName.value = ''
  essayTopic.value = ''
  messages.value = []
  userMessage.value = ''
  
  appendInfo('🔄 已清除会话，可以开始新的对话')
}

async function handleSendMessage(): Promise<void> {
  const validationMessage = validateForm()
  if (validationMessage) {
    appendError(validationMessage)
    await scrollToBottom()
    return
  }

  const message = userMessage.value.trim()
  userMessage.value = ''  // 清空输入框

  // 添加用户消息
  messages.value.push({
    id: createMessageId(),
    role: 'user',
    content: message,
  })

  // 添加助手消息（流式）
  const assistantId = createMessageId()
  streamingMessageId.value = assistantId

  messages.value.push({
    id: assistantId,
    role: 'assistant',
    content: '',
    streaming: true,
  })
  const assistantIndex = messages.value.length - 1
  await scrollToBottom()

  abortController = new AbortController()
  isGenerating.value = true

  try {
    // 构建请求
    const request: EssayChatRequest = {
      sessionId: sessionId.value || undefined,
      studentName: studentName.value.trim() || undefined,
      essayTopic: essayTopic.value.trim() || undefined,
      message: message,
    }

    await chatWithEssayReview(request, {
      signal: abortController.signal,
      onSessionId: (newSessionId) => {
        // 保存 sessionId
        sessionId.value = newSessionId
        hasSession.value = true
        console.log('会话 ID 已保存:', newSessionId)
        appendInfo('✅ 会话已创建，您可以继续追问')
      },
      onEvent: (data) => {
        const assistant = messages.value[assistantIndex]
        if (assistant) {
          assistant.content = data
        }
        void scrollToBottom()
      },
    })
  } catch (error) {
    const assistant = messages.value[assistantIndex]
    if (error instanceof DOMException && error.name === 'AbortError') {
      // 用户主动停止
      if (!assistant?.content.trim()) {
        messages.value = messages.value.filter((m) => m.id !== assistantId)
      }
      appendInfo('⏸️ 已停止生成')
      return
    }

    // 其他错误
    messages.value = messages.value.filter((m) => m.id !== assistantId)
    appendError(error instanceof Error ? error.message : String(error))
  } finally {
    streamingMessageId.value = null
    const assistant = messages.value[assistantIndex]
    if (assistant) {
      assistant.streaming = false
    }
    isGenerating.value = false
    abortController = null
    await scrollToBottom()
  }
}

// 处理 Enter 键发送
function handleKeyPress(event: KeyboardEvent): void {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    handleSendMessage()
  }
}
</script>

<template>
  <div class="essay-chat-layout">
    <header class="chat-header">
      <div class="header-content">
        <h1>📝 作文批改系统（多轮对话）</h1>
        <div class="session-info" v-if="hasSession">
          <span class="session-badge">会话中</span>
          <button class="clear-btn" @click="clearCurrentSession" :disabled="isGenerating">
            清除会话
          </button>
        </div>
      </div>
      <p class="header-desc">
        支持多轮对话：首次请求批改作文，后续可以追问细节、提出修改建议等
      </p>
    </header>

    <section ref="messagesContainer" class="messages" aria-live="polite">
      <div v-if="messages.length === 0" class="welcome">
        <div class="welcome-icon">👨‍🏫</div>
        <h2>欢迎使用作文批改系统</h2>
        <p>填写学生姓名和作文题目，开始批改流程</p>
        
        <div class="quick-start">
          <h3>快速开始</h3>
          <div class="steps">
            <div class="step">
              <span class="step-number">1</span>
              <span class="step-text">填写学生姓名和作文题目</span>
            </div>
            <div class="step">
              <span class="step-number">2</span>
              <span class="step-text">输入"请批改这篇作文"</span>
            </div>
            <div class="step">
              <span class="step-number">3</span>
              <span class="step-text">收到评语后，可以继续追问</span>
            </div>
          </div>
        </div>

        <div class="examples">
          <h3>示例对话</h3>
          <div class="example-item">
            <strong>首次：</strong>学生：张三，题目：生活中的美好，消息：请批改这篇作文
          </div>
          <div class="example-item">
            <strong>追问1：</strong>能详细分析一下主题立意吗？
          </div>
          <div class="example-item">
            <strong>追问2：</strong>这篇文章有哪些优点和不足？
          </div>
          <div class="example-item">
            <strong>追问3：</strong>如何改进结尾部分？
          </div>
        </div>
      </div>

      <article
        v-for="message in messages"
        :key="message.id"
        class="msg"
        :class="[message.role, { streaming: isMessageStreaming(message) }]"
      >
        <div v-if="message.role === 'info'" class="info-content">
          {{ message.content }}
        </div>
        <div v-else-if="message.role === 'error'" class="error-content">
          ❌ {{ message.content }}
        </div>
        <div v-else class="message-content">
          <div v-if="message.role === 'user'" class="user-label">👤 你</div>
          <div v-if="message.role === 'assistant'" class="assistant-label">👨‍🏫 教师</div>
          <pre class="message-text">{{ message.content }}</pre>
        </div>
      </article>
    </section>

    <footer class="chat-footer">
      <!-- 首次请求时显示表单 -->
      <div v-if="!hasSession" class="initial-form">
        <div class="form-row">
          <label class="field">
            <span>学生姓名</span>
            <input
              v-model="studentName"
              type="text"
              placeholder="例如：张三"
              :disabled="isGenerating"
              required
            />
          </label>
          <label class="field">
            <span>作文题目</span>
            <input
              v-model="essayTopic"
              type="text"
              placeholder="例如：生活中的美好"
              :disabled="isGenerating"
              required
            />
          </label>
        </div>
      </div>

      <!-- 消息输入框 -->
      <div class="message-input">
        <textarea
          v-model="userMessage"
          placeholder="输入消息...（首次请求：请批改这篇作文 | 追问：能详细点吗？）"
          :disabled="isGenerating"
          @keydown="handleKeyPress"
          rows="2"
        />
        <div class="input-actions">
          <button 
            type="button" 
            class="stop-btn" 
            :disabled="!isGenerating" 
            @click="stopGeneration"
          >
            停止
          </button>
          <button 
            type="button" 
            class="send-btn" 
            :disabled="isGenerating || !userMessage.trim()"
            @click="handleSendMessage"
          >
            {{ isGenerating ? '生成中...' : '发送' }}
          </button>
        </div>
      </div>
    </footer>
  </div>
</template>

<style scoped>
.essay-chat-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.chat-header {
  padding: 0.75rem 1.25rem;
  border-bottom: 1px solid var(--border);
  background: var(--panel);
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.35rem;
}

.chat-header h1 {
  margin: 0;
  font-size: 1.2rem;
  font-weight: 600;
}

.session-info {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.session-badge {
  padding: 0.25rem 0.75rem;
  background: #10b981;
  color: white;
  border-radius: 12px;
  font-size: 0.75rem;
  font-weight: 600;
}

.clear-btn {
  padding: 0.25rem 0.5rem;
  background: transparent;
  color: var(--muted);
  border: 1px solid var(--border);
  border-radius: 6px;
  font-size: 0.75rem;
  cursor: pointer;
  transition: all 0.2s;
}

.clear-btn:hover:not(:disabled) {
  color: var(--error);
  border-color: var(--error);
}

.clear-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.header-desc {
  margin: 0;
  font-size: 0.8rem;
  color: var(--muted);
}

.messages {
  flex: 1;
  overflow-y: auto;
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  max-width: 52rem;
  width: 100%;
  margin: 0 auto;
}

.welcome {
  text-align: center;
  padding: 2rem 1rem;
  color: var(--muted);
}

.welcome-icon {
  font-size: 4rem;
  margin-bottom: 1rem;
}

.welcome h2 {
  margin: 0 0 0.5rem;
  font-size: 1.5rem;
  color: var(--text);
}

.welcome p {
  margin: 0 0 2rem;
  font-size: 0.9rem;
}

.quick-start,
.examples {
  text-align: left;
  max-width: 32rem;
  margin: 1.5rem auto;
  padding: 1rem;
  background: var(--panel);
  border-radius: 8px;
  border: 1px solid var(--border);
}

.quick-start h3,
.examples h3 {
  margin: 0 0 0.75rem;
  font-size: 1rem;
  color: var(--text);
}

.steps {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.step {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.85rem;
}

.step-number {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  background: var(--accent);
  color: #0f1419;
  border-radius: 50%;
  font-weight: 600;
  font-size: 0.75rem;
  flex-shrink: 0;
}

.example-item {
  padding: 0.5rem;
  margin-bottom: 0.5rem;
  background: var(--bg);
  border-radius: 6px;
  font-size: 0.85rem;
  line-height: 1.5;
}

.example-item:last-child {
  margin-bottom: 0;
}

.msg {
  max-width: 85%;
  padding: 0.65rem 0.9rem;
  border-radius: 12px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
}

.msg.user {
  align-self: flex-end;
  background: var(--user);
  color: #fff;
}

.msg.assistant {
  align-self: flex-start;
  max-width: 100%;
  background: var(--assistant);
  border: 1px solid var(--border);
}

.msg.info {
  align-self: center;
  background: rgba(59, 130, 246, 0.1);
  border: 1px solid rgba(59, 130, 246, 0.3);
  color: #3b82f6;
  font-size: 0.85rem;
  padding: 0.5rem 0.75rem;
}

.msg.error {
  align-self: stretch;
  max-width: none;
  background: rgba(248, 113, 113, 0.12);
  border: 1px solid var(--error);
  color: var(--error);
  font-size: 0.9rem;
}

.message-content {
  width: 100%;
}

.user-label,
.assistant-label {
  font-size: 0.75rem;
  font-weight: 600;
  margin-bottom: 0.35rem;
  opacity: 0.8;
}

.message-text {
  margin: 0;
  font-family: inherit;
  font-size: 0.9rem;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.msg.assistant.streaming::after {
  content: "";
  display: inline-block;
  width: 6px;
  height: 1em;
  margin-left: 2px;
  vertical-align: text-bottom;
  background: var(--accent);
  border-radius: 2px;
  animation: blink 0.9s ease-in-out infinite;
}

@keyframes blink {
  0%,
  100% {
    opacity: 1;
  }

  50% {
    opacity: 0.2;
  }
}

.chat-footer {
  padding: 0.75rem 1rem 1rem;
  border-top: 1px solid var(--border);
  background: var(--panel);
}

.initial-form {
  margin-bottom: 0.75rem;
}

.form-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.65rem;
  margin-bottom: 0.75rem;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  flex: 1;
  min-width: 10rem;
  font-size: 0.8rem;
  color: var(--muted);
}

.field input[type="text"] {
  padding: 0.55rem 0.7rem;
  border-radius: 10px;
  border: 1px solid var(--border);
  background: var(--bg);
  color: var(--text);
  line-height: 1.4;
}

.field input[type="text"]:focus {
  outline: none;
  border-color: var(--accent);
}

.field input[type="text"]:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.message-input {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.message-input textarea {
  padding: 0.65rem 0.75rem;
  border-radius: 10px;
  border: 1px solid var(--border);
  background: var(--bg);
  color: var(--text);
  line-height: 1.5;
  resize: vertical;
  font-family: inherit;
  font-size: 0.9rem;
}

.message-input textarea:focus {
  outline: none;
  border-color: var(--accent);
}

.message-input textarea:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.input-actions {
  display: flex;
  gap: 0.5rem;
  justify-content: flex-end;
}

.input-actions button {
  padding: 0.6rem 1.1rem;
  border: none;
  border-radius: 10px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s;
}

.send-btn {
  background: var(--accent);
  color: #0f1419;
}

.send-btn:hover:not(:disabled) {
  opacity: 0.9;
}

.send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.stop-btn {
  background: transparent;
  color: var(--muted);
  border: 1px solid var(--border);
}

.stop-btn:hover:not(:disabled) {
  color: var(--error);
  border-color: var(--error);
}

.stop-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
