<script setup lang="ts">
import { nextTick, onMounted, ref } from 'vue'
import { createChat, streamChat, listChats, listMessages } from '../api/streamChat'
import type { StreamChatRequest, ChatItem, MessageItem } from '../api/streamChat'

interface ChatMessage {
  id: string
  role: 'user' | 'assistant' | 'info' | 'error'
  content: string
  streaming?: boolean
}

// 会话状态
const chatId = ref<string | null>(null)
const chatName = ref('')
const isChatReady = ref(false)
const isCreatingChat = ref(false)

// 对话状态
const userMessage = ref('')
const messages = ref<ChatMessage[]>([])
const isGenerating = ref(false)
const messagesContainer = ref<HTMLElement | null>(null)
const streamingMessageId = ref<string | null>(null)

// 侧边栏状态
const sidebarOpen = ref(true)
const chatList = ref<ChatItem[]>([])
const isLoadingList = ref(false)
const isLoadingHistory = ref(false)

// 配置
const APP_ID = '1'
const DEFAULT_MODEL = 'default'

let abortController: AbortController | null = null
let messageCounter = 0

function createMessageId(): string {
  messageCounter += 1
  return `msg-${Date.now()}-${messageCounter}`
}

function appendInfo(text: string): void {
  messages.value.push({ id: createMessageId(), role: 'info', content: text })
}

function appendError(text: string): void {
  messages.value.push({ id: createMessageId(), role: 'error', content: text })
}

async function scrollToBottom(): Promise<void> {
  await nextTick()
  const container = messagesContainer.value
  if (container) {
    container.scrollTop = container.scrollHeight
  }
}

/** 加载会话列表 */
async function loadChatList(): Promise<void> {
  isLoadingList.value = true
  try {
    const res = await listChats(APP_ID, 1, 50)
    if (res.code === 200 && res.data) {
      chatList.value = res.data.records
    }
  } catch (err) {
    console.error('加载会话列表失败:', err)
  } finally {
    isLoadingList.value = false
  }
}

/** 创建会话 */
async function initChat(): Promise<void> {
  isCreatingChat.value = true
  try {
    const now = new Date()
    const name = `对话 ${now.getMonth() + 1}/${now.getDate()} ${now.getHours()}:${String(now.getMinutes()).padStart(2, '0')}`
    const res = await createChat(APP_ID, name, DEFAULT_MODEL)

    if (res.code === 200 && res.data) {
      chatId.value = res.data.id
      chatName.value = res.data.chatName || name
      isChatReady.value = true
      messages.value = []
      appendInfo(`✅ 会话已创建：${chatName.value}`)
      // 刷新列表
      await loadChatList()
    } else {
      appendError(`创建会话失败：${res.msg || '未知错误'}`)
    }
  } catch (err) {
    appendError(`创建会话异常：${err instanceof Error ? err.message : String(err)}`)
  } finally {
    isCreatingChat.value = false
    await scrollToBottom()
  }
}

/** 点击历史会话，加载消息 */
async function selectChat(item: ChatItem): Promise<void> {
  if (isGenerating.value) return
  if (item.id === chatId.value) return

  abortController?.abort()

  chatId.value = item.id
  chatName.value = item.chatName
  messages.value = []
  isChatReady.value = false
  isLoadingHistory.value = true

  try {
    const res = await listMessages(item.id)
    if (res.code === 200 && res.data) {
      const historyMessages = res.data
      if (historyMessages.length === 0) {
        appendInfo('该会话暂无消息记录')
      } else {
        for (const msg of historyMessages) {
          const role = msg.messageType === 'user' ? 'user' : 'assistant'
          messages.value.push({
            id: createMessageId(),
            role,
            content: msg.message || '',
          })
        }
      }
    }
    isChatReady.value = true
  } catch (err) {
    appendError(`加载消息历史失败：${err instanceof Error ? err.message : String(err)}`)
  } finally {
    isLoadingHistory.value = false
    await scrollToBottom()
  }
}

/** 发送消息 */
async function handleSend(): Promise<void> {
  const text = userMessage.value.trim()
  if (!text) return
  if (!isChatReady.value || chatId.value === null) {
    appendError('会话尚未就绪，请稍后重试')
    return
  }
  if (isGenerating.value) return

  userMessage.value = ''

  messages.value.push({ id: createMessageId(), role: 'user', content: text })

  const assistantId = createMessageId()
  streamingMessageId.value = assistantId
  messages.value.push({ id: assistantId, role: 'assistant', content: '', streaming: true })
  const assistantIndex = messages.value.length - 1
  await scrollToBottom()

  abortController = new AbortController()
  isGenerating.value = true

  try {
    const request: StreamChatRequest = {
      appId: APP_ID,
      question: text,
      chatId: chatId.value,
    }

    await streamChat(request, {
      signal: abortController.signal,
      onChunk: (fullText) => {
        const msg = messages.value[assistantIndex]
        if (msg) msg.content = fullText
        void scrollToBottom()
      },
      onDone: () => {},
    })
  } catch (err) {
    const msg = messages.value[assistantIndex]
    if (err instanceof DOMException && err.name === 'AbortError') {
      if (!msg?.content.trim()) {
        messages.value = messages.value.filter((m) => m.id !== assistantId)
      }
      appendInfo('⏸️ 已停止生成')
    } else {
      messages.value = messages.value.filter((m) => m.id !== assistantId)
      appendError(err instanceof Error ? err.message : String(err))
    }
  } finally {
    streamingMessageId.value = null
    const msg = messages.value[assistantIndex]
    if (msg) msg.streaming = false
    isGenerating.value = false
    abortController = null
    await scrollToBottom()
    // 发送完后刷新列表
    await loadChatList()
  }
}

function stopGeneration(): void {
  abortController?.abort()
}

function handleKeyPress(event: KeyboardEvent): void {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    handleSend()
  }
}

function isMessageStreaming(message: ChatMessage): boolean {
  return message.role === 'assistant' && message.id === streamingMessageId.value && isGenerating.value
}

function toggleSidebar(): void {
  sidebarOpen.value = !sidebarOpen.value
}

/** 开启新会话 */
async function startNewChat(): Promise<void> {
  abortController?.abort()
  messages.value = []
  userMessage.value = ''
  chatId.value = null
  chatName.value = ''
  isChatReady.value = false
  streamingMessageId.value = null
  isGenerating.value = false
  abortController = null
  await initChat()
}

onMounted(async () => {
  await loadChatList()
  await initChat()
})
</script>

<template>
  <div class="stream-chat-layout">
    <!-- 左侧会话列表 -->
    <aside class="sidebar" :class="{ collapsed: !sidebarOpen }">
      <div class="sidebar-header">
        <span v-if="sidebarOpen" class="sidebar-title">会话历史</span>
        <button class="toggle-btn" @click="toggleSidebar">
          {{ sidebarOpen ? '◀' : '▶' }}
        </button>
      </div>
      <div v-if="sidebarOpen" class="sidebar-body">
        <div v-if="isLoadingList" class="sidebar-loading">加载中...</div>
        <div v-else-if="chatList.length === 0" class="sidebar-empty">暂无会话</div>
        <ul v-else class="chat-list">
          <li
            v-for="item in chatList"
            :key="item.id"
            class="chat-item"
            :class="{ active: item.id === chatId }"
            @click="selectChat(item)"
          >
            <span class="chat-item-name">{{ item.chatName }}</span>
            <span class="chat-item-time">{{ item.createTime?.slice(5, 16) }}</span>
          </li>
        </ul>
      </div>
    </aside>

    <!-- 右侧主区域 -->
    <div class="main-area">
      <header class="chat-header">
        <div class="header-content">
          <h1>💬 AI 对话</h1>
          <div v-if="isChatReady" class="session-info">
            <span class="session-badge">会话中</span>
            <span class="chat-name">{{ chatName }}</span>
          </div>
          <div v-else-if="isCreatingChat" class="session-info">
            <span class="loading-badge">正在创建会话...</span>
          </div>
        </div>
        <p class="header-desc">与 AI 进行流式对话，回复实时呈现</p>
      </header>

      <section ref="messagesContainer" class="messages" aria-live="polite">
        <div v-if="isLoadingHistory" class="loading-history">加载消息中...</div>

        <div v-else-if="messages.length === 0 && isChatReady" class="welcome">
          <div class="welcome-icon">🤖</div>
          <h2>开始对话</h2>
          <p>会话已就绪，在下方输入问题即可开始</p>
        </div>

        <article
          v-for="message in messages"
          :key="message.id"
          class="msg"
          :class="[message.role, { streaming: isMessageStreaming(message) }]"
        >
          <div v-if="message.role === 'info'" class="info-content">{{ message.content }}</div>
          <div v-else-if="message.role === 'error'" class="error-content">❌ {{ message.content }}</div>
          <div v-else class="message-content">
            <div v-if="message.role === 'user'" class="user-label">👤 你</div>
            <div v-if="message.role === 'assistant'" class="assistant-label">🤖 AI</div>
            <pre class="message-text">{{ message.content }}</pre>
          </div>
        </article>
      </section>

      <footer class="chat-footer">
        <div class="message-input">
          <textarea
            v-model="userMessage"
            placeholder="输入你的问题...（Enter 发送，Shift+Enter 换行）"
            :disabled="isGenerating || !isChatReady"
            @keydown="handleKeyPress"
            rows="2"
          />
          <div class="input-actions">
            <button
              type="button"
              class="new-chat-btn"
              :disabled="isGenerating || isCreatingChat"
              @click="startNewChat"
            >
              + 新会话
            </button>
            <div class="right-actions">
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
                :disabled="isGenerating || !userMessage.trim() || !isChatReady"
                @click="handleSend"
              >
                {{ isGenerating ? '生成中...' : '发送' }}
              </button>
            </div>
          </div>
        </div>
      </footer>
    </div>
  </div>
</template>

<style scoped>
.stream-chat-layout {
  display: flex;
  height: 100%;
}

/* ===== 侧边栏 ===== */
.sidebar {
  width: 260px;
  min-width: 260px;
  background: var(--panel);
  border-right: 1px solid var(--border);
  display: flex;
  flex-direction: column;
  transition: width 0.2s, min-width 0.2s;
}

.sidebar.collapsed {
  width: 40px;
  min-width: 40px;
}

.sidebar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem;
  border-bottom: 1px solid var(--border);
}

.sidebar-title {
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--text);
}

.toggle-btn {
  background: transparent;
  border: none;
  color: var(--muted);
  font-size: 0.8rem;
  padding: 0.25rem 0.4rem;
  cursor: pointer;
  border-radius: 4px;
}

.toggle-btn:hover {
  color: var(--text);
  background: var(--border);
}

.sidebar-body {
  flex: 1;
  overflow-y: auto;
  padding: 0.5rem;
}

.sidebar-loading,
.sidebar-empty {
  text-align: center;
  color: var(--muted);
  font-size: 0.8rem;
  padding: 1.5rem 0;
}

.chat-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.chat-item {
  padding: 0.6rem 0.7rem;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s;
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.chat-item:hover {
  background: var(--border);
}

.chat-item.active {
  background: rgba(56, 189, 248, 0.15);
  border-left: 3px solid var(--accent);
}

.chat-item-name {
  font-size: 0.85rem;
  color: var(--text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-item-time {
  font-size: 0.7rem;
  color: var(--muted);
}

/* ===== 主区域 ===== */
.main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  height: 100%;
  overflow: hidden;
}

.chat-header {
  padding: 0.75rem 1.25rem;
  border-bottom: 1px solid var(--border);
  background: var(--panel);
  flex-shrink: 0;
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

.loading-badge {
  padding: 0.25rem 0.75rem;
  background: #f59e0b;
  color: #0f1419;
  border-radius: 12px;
  font-size: 0.75rem;
  font-weight: 600;
  animation: pulse 1.2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}

.chat-name {
  font-size: 0.8rem;
  color: var(--muted);
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
  min-height: 0;
}

.loading-history {
  text-align: center;
  color: var(--muted);
  padding: 2rem;
  font-size: 0.9rem;
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
  margin: 0;
  font-size: 0.9rem;
}

.msg {
  max-width: 85%;
  padding: 0.65rem 0.9rem;
  border-radius: 12px;
  line-height: 1.5;
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

.message-content { width: 100%; }

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
  0%, 100% { opacity: 1; }
  50% { opacity: 0.2; }
}

.chat-footer {
  padding: 0.75rem 1rem 1rem;
  border-top: 1px solid var(--border);
  background: var(--panel);
  flex-shrink: 0;
}

.message-input {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  max-width: 52rem;
  margin: 0 auto;
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
  justify-content: space-between;
  align-items: center;
}

.right-actions {
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

.send-btn:hover:not(:disabled) { opacity: 0.9; }
.send-btn:disabled { opacity: 0.5; cursor: not-allowed; }

.stop-btn {
  background: transparent;
  color: var(--muted);
  border: 1px solid var(--border);
}

.stop-btn:hover:not(:disabled) {
  color: var(--error);
  border-color: var(--error);
}

.stop-btn:disabled { opacity: 0.5; cursor: not-allowed; }

.new-chat-btn {
  background: transparent;
  color: var(--accent);
  border: 1px solid var(--accent);
}

.new-chat-btn:hover:not(:disabled) {
  background: rgba(56, 189, 248, 0.1);
}

.new-chat-btn:disabled { opacity: 0.5; cursor: not-allowed; }
</style>
