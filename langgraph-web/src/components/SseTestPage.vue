<script setup lang="ts">
    import { ref } from 'vue'
    import type { TimelineNode } from '../types/marketReport'
    import { flushSseRemainder, processSseChunk } from '../utils/sseStream'

    const isRunning = ref(false)
    const events = ref<Array<{ time: string; raw: string }>>([])
    const timelineNodes = ref<TimelineNode[]>([])

    let abortController: AbortController | null = null

    function formatTime(): string {
        const d = new Date()
        return `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}:${d.getSeconds().toString().padStart(2, '0')}.${d.getMilliseconds().toString().padStart(3, '0')}`
    }

    function handleNodeEvent(node: TimelineNode): void {
        const existing = timelineNodes.value.find((n) => n.name === node.name)
        if (existing) {
            Object.assign(existing, node)
        } else {
            timelineNodes.value.push({ ...node })
        }
    }

    async function handleStart(): Promise<void> {
        events.value = []
        timelineNodes.value = []
        isRunning.value = true
        abortController = new AbortController()

        try {
            const response = await fetch('/api/sse-test', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    Accept: 'text/event-stream',
                },
                signal: abortController.signal,
            })

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}`)
            }

            const reader = response.body?.getReader()
            if (!reader) return

            const decoder = new TextDecoder()
            let sseRemainder = ''

            const handleRawLine = (raw: string): void => {
                events.value.push({ time: formatTime(), raw })

                let obj: Record<string, unknown>
                try {
                    obj = JSON.parse(raw) as Record<string, unknown>
                } catch {
                    return
                }

                if (obj.type === 'done') return

                if (!obj.type && typeof obj.name === 'string' && typeof obj.status === 'string') {
                    handleNodeEvent({
                        name: obj.name as string,
                        message: (obj.message as string) ?? '',
                        data: (obj.data as string) ?? '',
                        status: obj.status as 'running' | 'done' | 'error',
                        costTime: (obj.costTime as string) ?? '',
                    })
                }
            }

            while (true) {
                const { done, value } = await reader.read()
                if (done) break
                const chunk = decoder.decode(value, { stream: true })
                sseRemainder += chunk
                sseRemainder = processSseChunk(sseRemainder, handleRawLine)
            }

            const tail = decoder.decode()
            if (tail) {
                sseRemainder += tail
                sseRemainder = processSseChunk(sseRemainder, handleRawLine)
            }
            if (sseRemainder.trim()) {
                flushSseRemainder(sseRemainder, handleRawLine)
            }
        } catch (e) {
            if (e instanceof DOMException && e.name === 'AbortError') return
            events.value.push({ time: formatTime(), raw: `ERROR: ${e}` })
        } finally {
            isRunning.value = false
        }
    }

    function handleStop(): void {
        abortController?.abort()
    }
</script>

<template>
    <div class="test-layout">
        <header class="test-header">
            <h1>🧪 SSE 纯 For 循环测试</h1>
            <p>不依赖 langgraph4j，纯 for 循环 + sleep(2s)，验证 SSE 是否能逐帧推送到前端。</p>
        </header>

        <section class="test-main">
            <div class="panel-row">
                <!-- 左：时间线 -->
                <div class="panel">
                    <h2>时间线</h2>
                    <div v-if="timelineNodes.length === 0 && !isRunning" class="empty">点击"开始测试"</div>
                    <div v-for="node in timelineNodes" :key="node.name" class="tl-item" :class="node.status">
                        <span class="dot" :class="node.status"></span>
                        <span class="name">{{ node.name }}</span>
                        <span class="status-tag" :class="node.status">{{ node.status }}</span>
                    </div>
                </div>

                <!-- 右：原始事件日志 -->
                <div class="panel">
                    <h2>原始事件日志</h2>
                    <div v-if="events.length === 0" class="empty">等待事件...</div>
                    <div v-for="(evt, i) in events" :key="i" class="log-line">
                        <span class="log-time">{{ evt.time }}</span>
                        <span class="log-raw">{{ evt.raw }}</span>
                    </div>
                </div>
            </div>
        </section>

        <footer class="test-footer">
            <button class="primary" :disabled="isRunning" @click="handleStart">
                {{ isRunning ? '测试中...' : '开始测试' }}
            </button>
            <button class="secondary" :disabled="!isRunning" @click="handleStop">停止</button>
        </footer>
    </div>
</template>

<style scoped>
    .test-layout {
        min-height: 100vh;
        display: flex;
        flex-direction: column;
    }
    .test-header {
        padding: 0.75rem 1.25rem;
        border-bottom: 1px solid var(--border);
        background: var(--panel);
    }
    .test-header h1 { margin: 0; font-size: 1.1rem; font-weight: 600; }
    .test-header p { margin: 0.35rem 0 0; font-size: 0.8rem; color: var(--muted); }

    .test-main { flex: 1; padding: 1.5rem; overflow-y: auto; }
    .panel-row { display: flex; gap: 1.5rem; }
    .panel {
        flex: 1;
        background: var(--panel, #f8f9fa);
        border: 1px solid var(--border, #e2e8f0);
        border-radius: 12px;
        padding: 1rem;
        max-height: 65vh;
        overflow-y: auto;
    }
    .panel h2 { margin: 0 0 0.75rem; font-size: 0.95rem; }
    .empty { color: var(--muted); font-size: 0.85rem; padding: 2rem 0; text-align: center; }

    /* 时间线 */
    .tl-item {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        padding: 0.4rem 0;
    }
    .dot {
        width: 10px; height: 10px; border-radius: 50%; flex-shrink: 0;
    }
    .dot.running { background: #3b82f6; animation: pulse 1s infinite; }
    .dot.done { background: #10b981; }
    @keyframes pulse { 0%,100% { opacity:1 } 50% { opacity:0.4 } }
    .name { font-weight: 500; font-size: 0.85rem; }
    .status-tag {
        font-size: 0.7rem;
        padding: 0.1rem 0.4rem;
        border-radius: 4px;
    }
    .status-tag.running { background: rgba(59,130,246,0.12); color: #3b82f6; }
    .status-tag.done { background: rgba(16,185,129,0.12); color: #10b981; }

    /* 日志 */
    .log-line {
        display: flex;
        gap: 0.5rem;
        padding: 0.25rem 0;
        border-bottom: 1px solid var(--border, #e2e8f0);
        font-size: 0.75rem;
    }
    .log-time { color: var(--muted); white-space: nowrap; font-family: monospace; }
    .log-raw {
        word-break: break-all;
        font-family: monospace;
        color: var(--text, #1a202c);
    }

    /* 底部 */
    .test-footer {
        padding: 0.75rem 1.25rem;
        border-top: 1px solid var(--border);
        background: var(--panel);
        display: flex;
        gap: 0.5rem;
        justify-content: center;
    }
    .test-footer button {
        padding: 0.6rem 1.5rem;
        border: none;
        border-radius: 10px;
        font-weight: 600;
        font-size: 0.9rem;
        cursor: pointer;
    }
    .test-footer .primary { background: var(--accent, #3b82f6); color: #fff; }
    .test-footer .secondary { background: transparent; color: var(--muted); border: 1px solid var(--border); }
    .test-footer button:disabled { opacity: 0.5; cursor: not-allowed; }
</style>
