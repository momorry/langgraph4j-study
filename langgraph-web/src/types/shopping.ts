import type { TimelineNode } from './marketReport'

export interface ShoppingRequest {
  product: string
}

export interface ShoppingOptions {
  signal?: AbortSignal
  /** 节点进度回调 */
  onNode: (node: TimelineNode) => void
}
