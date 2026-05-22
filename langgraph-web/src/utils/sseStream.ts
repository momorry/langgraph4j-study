/** 解析 SSE 帧，对每个完整 event 调用 onData；返回未读完的尾部。 */
export function processSseChunk(raw: string, onData: (data: string) => void): string {
  const parts = raw.split('\n\n')
  const remainder = parts.pop() ?? ''

  for (const part of parts) {
    const lines = part.split('\n')
    const dataLines: string[] = []

    for (const line of lines) {
      if (line.startsWith('data:')) {
        let payload = line.slice(5)
        if (payload.startsWith(' ')) {
          payload = payload.slice(1)
        }
        dataLines.push(payload)
      } else if (line.startsWith('event:') || line.startsWith('id:') || line.startsWith(':')) {
        continue
      } else if (line === '') {
        continue
      } else {
        dataLines.push(line)
      }
    }

    if (dataLines.length > 0) {
      onData(dataLines.join('\n'))
    }
  }

  return remainder
}

export function flushSseRemainder(remainder: string, onData: (data: string) => void): void {
  const trimmed = remainder.trim()
  if (!trimmed) {
    return
  }

  processSseChunk(`${trimmed}\n\n`, onData)

  if (trimmed.includes('data:')) {
    return
  }

  onData(trimmed)
}
