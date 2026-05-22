<script setup lang="ts">
import { nextTick, ref } from 'vue'
import { generateEssayReview } from '../api/essayReview'
import type { EssayMessage, EssayReviewRequest } from '../types/essayReview'

const studentName = ref('张三')
const essayTopic = ref('生活中的美好')
const messages = ref<EssayMessage[]>([])
const isGenerating = ref(false)
const messagesContainer = ref<HTMLElement | null>(null)

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
  if (!studentName.value || !studentName.value.trim()) {
    return '请填写学生姓名。'
  }

  if (!essayTopic.value || !essayTopic.value.trim()) {
    return '请填写作文题目。'
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

async function handleSubmit(): Promise<void> {
  const validationMessage = validateForm()
  if (validationMessage) {
    appendError(validationMessage)
    await scrollToBottom()
    return
  }

  const request: EssayReviewRequest = {
    studentName: studentName.value.trim(),
    essayTopic: essayTopic.value.trim(),
  }

  // 添加用户消息
  messages.value.push({
    id: createMessageId(),
    role: 'user',
    content: `学生：${request.studentName}\n作文题目：《${request.essayTopic}》`,
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
    await generateEssayReview(request, {
      signal: abortController.signal,
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
        messages.value = messages.value.filter((message) => message.id !== assistantId)
      }
      appendInfo('⏸️ 已停止生成')
      return
    }

    // 其他错误
    messages.value = messages.value.filter((message) => message.id !== assistantId)
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
</script>

<template>
  <div class="essay-layout">
    <header class="essay-header">
      <h1>📝 作文批改系统</h1>
      <p>
        填写学生姓名和作文题目后，系统将模拟完整工作流：
        <code>布置作业</code> → <code>提交作文</code> → <code>流式输出评语</code>
      </p>
    </header>

    <section ref="messagesContainer" class="messages" aria-live="polite">
      <div v-if="messages.length === 0" class="welcome">
        <div class="welcome-icon">👨‍🏫</div>
        <h2>欢迎使用作文批改系统</h2>
        <p>请输入学生姓名和作文题目，开始批改流程</p>
        <div class="workflow-steps">
          <div class="step">
            <span class="step-number">1</span>
            <span class="step-text">布置作文作业</span>
          </div>
          <div class="step">
            <span class="step-number">2</span>
            <span class="step-text">学生提交作文</span>
          </div>
          <div class="step">
            <span class="step-number">3</span>
            <span class="step-text">教师流式输出评语</span>
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
          <div v-if="message.role === 'user'" class="user-label">👤 请求</div>
          <div v-if="message.role === 'assistant'" class="assistant-label">👨‍🏫 教师评语</div>
          <pre class="message-text">{{ message.content }}</pre>
        </div>
      </article>
    </section>

    <footer class="essay-footer">
      <form class="essay-form" @submit.prevent="handleSubmit">
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

        <div class="form-actions">
          <button type="button" class="secondary" :disabled="!isGenerating" @click="stopGeneration">
            停止生成
          </button>
          <button type="submit" :disabled="isGenerating">
            {{ isGenerating ? '批改中...' : '提交批改' }}
          </button>
        </div>
      </form>
    </footer>
  </div>
</template>

<style scoped>
.essay-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.essay-header {
  padding: 0.75rem 1.25rem;
  border-bottom: 1px solid var(--border);
  background: var(--panel);
}

.essay-header h1 {
  margin: 0;
  font-size: 1.2rem;
  font-weight: 600;
}

.essay-header p {
  margin: 0.35rem 0 0;
  font-size: 0.8rem;
  color: var(--muted);
}

.essay-header code {
  font-size: 0.85em;
  color: var(--accent);
  background: rgba(139, 92, 246, 0.1);
  padding: 0.1rem 0.3rem;
  border-radius: 4px;
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
  padding: 3rem 1rem;
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

.workflow-steps {
  display: flex;
  justify-content: center;
  gap: 1.5rem;
  flex-wrap: wrap;
}

.step {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  background: var(--panel);
  border: 1px solid var(--border);
  border-radius: 8px;
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

.essay-footer {
  padding: 0.75rem 1rem 1rem;
  border-top: 1px solid var(--border);
  background: var(--panel);
}

.essay-form {
  max-width: 52rem;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
}

.form-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.65rem;
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

.form-actions {
  display: flex;
  gap: 0.5rem;
  justify-content: flex-end;
  margin-top: 0.15rem;
}

.form-actions button {
  padding: 0.6rem 1.1rem;
  border: none;
  border-radius: 10px;
  font-weight: 600;
  background: var(--accent);
  color: #0f1419;
  cursor: pointer;
  transition: opacity 0.2s;
}

.form-actions button:hover:not(:disabled) {
  opacity: 0.9;
}

.form-actions button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.form-actions button.secondary {
  background: transparent;
  color: var(--muted);
  border: 1px solid var(--border);
}

.form-actions button.secondary:hover:not(:disabled) {
  color: var(--text);
  border-color: var(--muted);
}
</style>
