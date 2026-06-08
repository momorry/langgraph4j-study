<script setup lang="ts">
    import { nextTick, ref } from 'vue'
    import { generateMarketReportV3 } from '../api/marketReportV3'
    import type { MarketReportRequest, TimelineNode } from '../types/marketReport'

    /** 展开/折叠 data 详情 */
    const expandedNodes = ref<Set<string>>(new Set())

    function toggleExpand(name: string): void {
        if (expandedNodes.value.has(name)) {
            expandedNodes.value.delete(name)
        } else {
            expandedNodes.value.add(name)
        }
    }

    function formatData(raw: string): string {
        try {
            const parsed = JSON.parse(raw)
            return JSON.stringify(parsed, null, 2)
        } catch {
            return raw
        }
    }

    const startDate = ref('2025-05-20')
    const endDate = ref('2025-05-20')
    const stockCodesInput = ref('300750.SZ')
    const isGenerating = ref(false)
    const messagesContainer = ref<HTMLElement | null>(null)

    const timelineNodes = ref<TimelineNode[]>([])
    const streamingContent = ref('')

    let abortController: AbortController | null = null

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

    function handleNodeEvent(node: TimelineNode): void {
        const existing = timelineNodes.value.find((n) => n.name === node.name)
        if (existing) {
            Object.assign(existing, node)
        } else {
            timelineNodes.value.push({ ...node })
        }
        void scrollToBottom()
    }

    function handleContentEvent(snapshot: string): void {
        streamingContent.value = snapshot
        void scrollToBottom()
    }

    async function handleGenerate(): Promise<void> {
        const validationMessage = validateForm()
        if (validationMessage) {
            alert(validationMessage)
            return
        }

        const body: MarketReportRequest = {
            startDate: startDate.value,
            endDate: endDate.value,
            stockCodes: parseStockCodes(stockCodesInput.value),
        }

        // 重置状态
        timelineNodes.value = []
        streamingContent.value = ''
        expandedNodes.value.clear()
        isGenerating.value = true

        abortController = new AbortController()

        try {
            await generateMarketReportV3(body, {
                signal: abortController.signal,
                onNode: handleNodeEvent,
                onContent: handleContentEvent,
            })
        } catch (error) {
            if (error instanceof DOMException && error.name === 'AbortError') {
                return
            }
            alert(error instanceof Error ? error.message : String(error))
        } finally {
            isGenerating.value = false
            abortController = null
            await scrollToBottom()
        }
    }
</script>

<template>
    <div class="chat-layout">
        <header class="chat-header">
            <h1>市场简报生成（V3）</h1>
            <p>
                填写日期区间与个股代码后，通过 POST
                <code>/api/report-v3</code>
                流式生成简报（V3 版本）。开发模式下 Vite 会将 <code>/api</code> 代理到
                <code>http://localhost:8080</code>。
            </p>
        </header>

        <section ref="messagesContainer" class="main-content">
            <!-- 请求信息 -->
            <div v-if="timelineNodes.length > 0" class="request-summary">
                <div class="request-label">📋 请求参数</div>
                <div class="request-detail">{{ buildRequestSummary({ startDate, endDate, stockCodes: parseStockCodes(stockCodesInput) }) }}</div>
            </div>

            <!-- 垂直时间线 -->
            <div v-if="timelineNodes.length > 0" class="timeline">
                <div
                    v-for="(node, index) in timelineNodes"
                    :key="node.name"
                    class="timeline-item"
                    :class="node.status"
                >
                    <!-- 连接线 -->
                    <div class="timeline-connector">
                        <div class="timeline-dot" :class="node.status">
                            <svg v-if="node.status === 'done'" viewBox="0 0 16 16" fill="currentColor" width="12" height="12">
                                <path d="M13.78 4.22a.75.75 0 010 1.06l-7.25 7.25a.75.75 0 01-1.06 0L2.22 9.28a.75.75 0 011.06-1.06L6 10.94l6.72-6.72a.75.75 0 011.06 0z"/>
                            </svg>
                            <div v-else-if="node.status === 'running'" class="dot-pulse"></div>
                            <svg v-else-if="node.status === 'error'" viewBox="0 0 16 16" fill="currentColor" width="12" height="12">
                                <path d="M3.72 3.72a.75.75 0 011.06 0L8 6.94l3.22-3.22a.75.75 0 111.06 1.06L9.06 8l3.22 3.22a.75.75 0 11-1.06 1.06L8 9.06l-3.22 3.22a.75.75 0 01-1.06-1.06L6.94 8 3.72 4.78a.75.75 0 010-1.06z"/>
                            </svg>
                        </div>
                        <div v-if="index < timelineNodes.length - 1 || streamingContent" class="timeline-line" :class="node.status"></div>
                    </div>
                    <!-- 节点内容 -->
                    <div class="timeline-content">
                        <div class="node-header">
                            <span class="timeline-label">{{ node.name }}</span>
                            <span class="node-cost" v-if="node.costTime">⏱ {{ node.costTime }}</span>
                            <span class="node-status-tag" :class="node.status">
                                <template v-if="node.status === 'running'">执行中</template>
                                <template v-else-if="node.status === 'done'">完成</template>
                                <template v-else>失败</template>
                            </span>
                        </div>
                        <div v-if="node.message" class="node-message">{{ node.message }}</div>
                        <div v-if="node.data" class="node-data-section">
                            <button class="node-data-toggle" @click="toggleExpand(node.name)">
                                {{ expandedNodes.has(node.name) ? '收起数据 ▼' : '查看数据 ▶' }}
                            </button>
                            <pre v-if="expandedNodes.has(node.name)" class="node-data-content">{{ formatData(node.data) }}</pre>
                        </div>
                    </div>
                </div>
            </div>

            <!-- 流式输出内容 -->
            <div v-if="streamingContent" class="streaming-output">
                <div class="output-header">📝 生成报告</div>
                <div class="output-content" :class="{ active: isGenerating }">
                    {{ streamingContent }}<span v-if="isGenerating" class="cursor-blink"></span>
                </div>
            </div>

            <!-- 空状态 -->
            <div v-if="timelineNodes.length === 0 && !isGenerating" class="empty-state">
                <div class="empty-icon">📊</div>
                <p>填写参数后点击“生成简报”开始</p>
            </div>
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

    .main-content {
        flex: 1;
        overflow-y: auto;
        padding: 1.5rem;
        max-width: 52rem;
        width: 100%;
        margin: 0 auto;
    }

    /* 请求摘要 */
    .request-summary {
        background: var(--panel, #f8f9fa);
        border: 1px solid var(--border, #e2e8f0);
        border-radius: 12px;
        padding: 0.75rem 1rem;
        margin-bottom: 1.5rem;
    }

    .request-label {
        font-size: 0.85rem;
        font-weight: 600;
        color: var(--text, #1a202c);
        margin-bottom: 0.35rem;
    }

    .request-detail {
        font-size: 0.8rem;
        color: var(--muted, #718096);
        white-space: pre-wrap;
        line-height: 1.5;
    }

    /* 垂直时间线 */
    .timeline {
        position: relative;
        margin-bottom: 1.5rem;
    }

    .timeline-item {
        display: flex;
        gap: 0.75rem;
        position: relative;
    }

    .timeline-connector {
        display: flex;
        flex-direction: column;
        align-items: center;
        flex-shrink: 0;
        width: 28px;
    }

    .timeline-dot {
        width: 28px;
        height: 28px;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        flex-shrink: 0;
        position: relative;
        z-index: 1;
        transition: all 0.3s ease;
    }

    .timeline-dot.running {
        background: #3b82f6;
        color: #fff;
        box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.2);
    }

    .timeline-dot.done {
        background: #10b981;
        color: #fff;
    }

    .timeline-dot.error {
        background: #ef4444;
        color: #fff;
    }

    .dot-pulse {
        width: 8px;
        height: 8px;
        border-radius: 50%;
        background: #fff;
        animation: pulse 1.2s ease-in-out infinite;
    }

    @keyframes pulse {
        0%, 100% { opacity: 1; transform: scale(1); }
        50% { opacity: 0.5; transform: scale(0.7); }
    }

    .timeline-line {
        width: 2px;
        flex: 1;
        min-height: 16px;
        transition: background 0.3s ease;
    }

    .timeline-line.done {
        background: #10b981;
    }

    .timeline-line.running {
        background: linear-gradient(to bottom, #3b82f6, var(--border, #e2e8f0));
    }

    .timeline-line.error {
        background: #ef4444;
    }

    .timeline-content {
        padding-bottom: 1rem;
        min-height: 28px;
        display: flex;
        flex-direction: column;
        gap: 0.35rem;
        flex: 1;
        min-width: 0;
    }

    .node-header {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        flex-wrap: wrap;
    }

    .timeline-label {
        font-size: 0.9rem;
        font-weight: 600;
        color: var(--text, #1a202c);
    }

    .node-cost {
        font-size: 0.75rem;
        color: var(--muted, #718096);
    }

    .node-status-tag {
        font-size: 0.7rem;
        padding: 0.1rem 0.45rem;
        border-radius: 4px;
        font-weight: 500;
    }

    .node-status-tag.running {
        background: rgba(59, 130, 246, 0.12);
        color: #3b82f6;
    }

    .node-status-tag.done {
        background: rgba(16, 185, 129, 0.12);
        color: #10b981;
    }

    .node-status-tag.error {
        background: rgba(239, 68, 68, 0.12);
        color: #ef4444;
    }

    .node-message {
        font-size: 0.8rem;
        color: var(--muted, #718096);
        line-height: 1.4;
    }

    .node-data-section {
        margin-top: 0.15rem;
    }

    .node-data-toggle {
        font-size: 0.75rem;
        color: #3b82f6;
        background: none;
        border: none;
        cursor: pointer;
        padding: 0.15rem 0;
        font-weight: 500;
    }

    .node-data-toggle:hover {
        text-decoration: underline;
    }

    .node-data-content {
        margin: 0.35rem 0 0;
        padding: 0.6rem 0.75rem;
        background: var(--bg, #f1f5f9);
        border: 1px solid var(--border, #e2e8f0);
        border-radius: 8px;
        font-size: 0.75rem;
        line-height: 1.5;
        overflow-x: auto;
        max-height: 300px;
        overflow-y: auto;
        white-space: pre-wrap;
        word-break: break-all;
        color: var(--text, #1a202c);
    }

    .timeline-item.running .timeline-label {
        color: #3b82f6;
    }

    /* 流式输出区域 */
    .streaming-output {
        background: var(--panel, #f8f9fa);
        border: 1px solid var(--border, #e2e8f0);
        border-radius: 12px;
        padding: 1rem;
        margin-top: 0.5rem;
    }

    .output-header {
        font-size: 0.85rem;
        font-weight: 600;
        color: var(--text, #1a202c);
        margin-bottom: 0.75rem;
        padding-bottom: 0.5rem;
        border-bottom: 1px solid var(--border, #e2e8f0);
    }

    .output-content {
        font-size: 0.9rem;
        line-height: 1.7;
        color: var(--text, #1a202c);
        white-space: pre-wrap;
        word-break: break-word;
    }

    .cursor-blink {
        display: inline-block;
        width: 2px;
        height: 1em;
        margin-left: 1px;
        vertical-align: text-bottom;
        background: var(--accent, #3b82f6);
        animation: blink 0.9s ease-in-out infinite;
    }

    @keyframes blink {
        0%, 100% { opacity: 1; }
        50% { opacity: 0; }
    }

    /* 空状态 */
    .empty-state {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        padding: 4rem 1rem;
        color: var(--muted, #718096);
    }

    .empty-icon {
        font-size: 3rem;
        margin-bottom: 1rem;
        opacity: 0.5;
    }

    .empty-state p {
        font-size: 0.9rem;
        margin: 0;
    }

    /* 底部表单 */
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
