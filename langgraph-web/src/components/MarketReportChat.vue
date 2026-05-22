<script setup lang="ts">
import { nextTick, ref } from 'vue'
import { generateMarketReport } from '../api/marketReport'
import type { ChatMessage, MarketReportRequest } from '../types/marketReport'

const startDate = ref('2025-05-20')
const endDate = ref('2025-05-20')
const stockCodesInput = ref('300750.SZ')
const messages = ref<ChatMessage[]>([])
const isGenerating = ref(false)
const messagesContainer = ref<HTMLElement | null>(null)

const streamingMessageId = ref<string | null>(null)

let abortController: AbortController | null = null
let messageCounter = 0

function createMessageId(): string {
  messageCounter += 1
  return `msg-${Date.now()}-${messageCounter}`
}

function parseStockCodes(raw: string): string[] {
  return raw
    .split(/[\s,，;；]+/)
    .map((code) => code.trim())
    .filter(Boolean)
}

function buildRequestSummary(body: MarketReportRequest): string {
  return [
    `开始日期：${body.startDate}`,
    `结束日期：${body.endDate}`,
    `个股代码：${body.stockCodes.join(', ')}`,
  ].join('\n')
}

function appendError(text: string): void {
  messages.value.push({
    id: createMessageId(),
    role: 'error',
    content: text,
  })
}

function validateForm(): string | null {
  if (!startDate.value || !endDate.value) {
    return '请填写开始日期与结束日期。'
  }

  if (startDate.value > endDate.value) {
    return '开始日期不能晚于结束日期。'
  }

  if (parseStockCodes(stockCodesInput.value).length === 0) {
    return '请至少填写一个个股代码。'
  }

  return null
}

function isMessageStreaming(message: ChatMessage): boolean {
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

async function handleGenerate(): Promise<void> {
  const validationMessage = validateForm()
  if (validationMessage) {
    appendError(validationMessage)
    await scrollToBottom()
    return
  }

  const body: MarketReportRequest = {
    startDate: startDate.value,
    endDate: endDate.value,
    stockCodes: parseStockCodes(stockCodesInput.value),
  }

  messages.value.push({
    id: createMessageId(),
    role: 'user',
    content: buildRequestSummary(body),
  })

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
    await generateMarketReport(body, {
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
      if (!assistant?.content.trim()) {
        messages.value = messages.value.filter((message) => message.id !== assistantId)
      }
      return
    }

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
  <div class="chat-layout">
    <header class="chat-header">
      <h1>市场简报生成</h1>
      <p>
        填写日期区间与个股代码后，通过 POST
        <code>/api/market-report/generate</code>
        流式生成简报。开发模式下 Vite 会将 <code>/api</code> 代理到
        <code>http://localhost:8080</code>。
      </p>
    </header>

    <section ref="messagesContainer" class="messages" aria-live="polite">
      <article
        v-for="message in messages"
        :key="message.id"
        class="msg"
        :class="[message.role, { streaming: isMessageStreaming(message) }]"
      >
        {{ message.content }}
      </article>
    </section>

    <footer class="chat-footer">
      <form class="report-form" @submit.prevent="handleGenerate">
        <div class="form-row">
          <label class="field">
            <span>开始日期</span>
            <input v-model="startDate" type="date" :disabled="isGenerating" required />
          </label>
          <label class="field">
            <span>结束日期</span>
            <input v-model="endDate" type="date" :disabled="isGenerating" required />
          </label>
        </div>

        <label class="field full">
          <span>个股代码列表</span>
          <textarea
            v-model="stockCodesInput"
            rows="3"
            placeholder="多个代码用逗号、空格或换行分隔，例如：&#10;600519&#10;000001,600036"
            :disabled="isGenerating"
            required
          />
        </label>

        <div class="form-actions">
          <button type="button" class="secondary" :disabled="!isGenerating" @click="stopGeneration">
            停止
          </button>
          <button type="submit" :disabled="isGenerating">生成简报</button>
        </div>
      </form>
    </footer>
  </div>
</template>

<style scoped>
.chat-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.chat-header {
  padding: 0.75rem 1.25rem;
  border-bottom: 1px solid var(--border);
  background: var(--panel);
}

.chat-header h1 {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 600;
}

.chat-header p {
  margin: 0.35rem 0 0;
  font-size: 0.8rem;
  color: var(--muted);
}

.chat-header code {
  font-size: 0.85em;
  color: var(--accent);
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

.msg.error {
  align-self: stretch;
  max-width: none;
  background: rgba(248, 113, 113, 0.12);
  border: 1px solid var(--error);
  color: var(--error);
  font-size: 0.9rem;
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

.report-form {
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

.field.full {
  flex: 1 1 100%;
}

.field input[type="date"],
.field textarea {
  padding: 0.55rem 0.7rem;
  border-radius: 10px;
  border: 1px solid var(--border);
  background: var(--bg);
  color: var(--text);
  line-height: 1.4;
}

.field textarea {
  min-height: 4.5rem;
  resize: vertical;
}

.field input[type="date"]:focus,
.field textarea:focus {
  outline: none;
  border-color: var(--accent);
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
