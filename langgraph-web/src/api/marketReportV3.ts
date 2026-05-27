import type { GenerateMarketReportOptions, MarketReportRequest } from '../types/marketReport'
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

async function readStreamBody(
    reader: ReadableStreamDefaultReader<Uint8Array>,
    isSse: boolean,
    onEvent: (data: string) => void,
): Promise<void> {
    const decoder = new TextDecoder()
    let sseRemainder = ''
    let snapshot = ''

    const emitEvent = (piece: string): void => {
        if (!piece) {
            return
        }
        snapshot += piece
        onEvent(snapshot)
    }

    const ingestSse = (raw: string): void => {
        sseRemainder += raw
        sseRemainder = processSseChunk(sseRemainder, emitEvent)
    }

    while (true) {
        const { done, value } = await reader.read()
        if (done) {
            break
        }

        const chunk = decoder.decode(value, { stream: true })
        if (isSse) {
            ingestSse(chunk)
        } else {
            emitEvent(chunk)
        }
    }

    const tail = decoder.decode()
    if (tail) {
        if (isSse) {
            ingestSse(tail)
        } else {
            emitEvent(tail)
        }
    }

    if (isSse && sseRemainder.trim()) {
        flushSseRemainder(sseRemainder, emitEvent)
    }
}

export async function generateMarketReportV3(
    request: MarketReportRequest,
    options: GenerateMarketReportOptions,
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
        const text = await response.text()
        if (text) {
            options.onEvent(text)
        }
        return
    }

    const contentType = (response.headers.get('content-type') ?? '').toLowerCase()
    const isSse = contentType.includes('text/event-stream')

    await readStreamBody(reader, isSse, options.onEvent)
}
