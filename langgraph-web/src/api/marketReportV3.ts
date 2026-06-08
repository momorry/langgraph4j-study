import type { GenerateMarketReportV3Options, MarketReportRequest } from '../types/marketReport'
import { flushSseRemainder, processSseChunk } from '../utils/sseStream'

const GENERATE_URL = '/api/report-v3'

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

async function readStreamBodyV3(
    reader: ReadableStreamDefaultReader<Uint8Array>,
    isSse: boolean,
    options: GenerateMarketReportV3Options,
): Promise<void> {
    const decoder = new TextDecoder()
    let sseRemainder = ''
    let contentSnapshot = ''

    const handleRawLine = (raw: string): void => {
        let obj: Record<string, unknown>
        try {
            obj = JSON.parse(raw) as Record<string, unknown>
        } catch {
            contentSnapshot += raw
            options.onContent(contentSnapshot)
            return
        }

        // NodeDetail 格式（无 type 字段，有 name + status）
        if (!obj.type && typeof obj.name === 'string' && typeof obj.status === 'string') {
            options.onNode({
                name: obj.name as string,
                message: (obj.message as string) ?? '',
                data: (obj.data as string) ?? '',
                status: obj.status as 'running' | 'done' | 'error',
                costTime: (obj.costTime as string) ?? '',
            })
            return
        }

        // 旧格式兼容
        if (obj.type === 'content' && obj.data) {
            contentSnapshot += obj.data as string
            options.onContent(contentSnapshot)
        } else if (obj.type === 'node' && obj.name && obj.status) {
            options.onNode({
                name: obj.name as string,
                message: '',
                data: '',
                status: obj.status as 'running' | 'done' | 'error',
                costTime: '',
            })
        }
        // 'done' 类型无需处理
    }

    const ingestSse = (raw: string): void => {
        sseRemainder += raw
        sseRemainder = processSseChunk(sseRemainder, handleRawLine)
    }

    while (true) {
        const { done, value } = await reader.read()
        if (done) break

        const chunk = decoder.decode(value, { stream: true })
        if (isSse) {
            ingestSse(chunk)
        } else {
            handleRawLine(chunk)
        }
    }

    const tail = decoder.decode()
    if (tail) {
        if (isSse) {
            ingestSse(tail)
        } else {
            handleRawLine(tail)
        }
    }

    if (isSse && sseRemainder.trim()) {
        flushSseRemainder(sseRemainder, handleRawLine)
    }
}

export async function generateMarketReportV3(
    request: MarketReportRequest,
    options: GenerateMarketReportV3Options,
): Promise<void> {
    const response = await fetch(GENERATE_URL, {
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
        return
    }

    const contentType = (response.headers.get('content-type') ?? '').toLowerCase()
    const isSse = contentType.includes('text/event-stream')

    await readStreamBodyV3(reader, isSse, options)
}
