<script setup lang="ts">import { ref } from 'vue'
import MarketReportChat from './components/MarketReportChat.vue'
import MarketReportChatV3 from './components/MarketReportChatV3.vue'
import EssayReviewChat from './components/EssayReviewChat.vue'
import EssayMultiTurnChat from './components/EssayMultiTurnChat.vue'
import ShoppingTimeline from './components/ShoppingTimeline.vue'
import SseTestPage from './components/SseTestPage.vue'

type TabKey = 'market-report' | 'market-report-v3' | 'essay-review' | 'essay-multi-turn' | 'shopping' | 'sse-test'

const activeTab = ref<TabKey>('shopping')
</script>

<template>
  <div class="app-container">
    <nav class="tab-nav">
      <button
              :class="['tab-button', { active: activeTab === 'market-report' }]"
              @click="activeTab = 'market-report'"
      >
        📊 市场简报
      </button>
      <button
              :class="['tab-button', { active: activeTab === 'market-report-v3' }]"
              @click="activeTab = 'market-report-v3'"
      >
        📊 市场简报V3
      </button>
      <button
              :class="['tab-button', { active: activeTab === 'essay-review' }]"
              @click="activeTab = 'essay-review'"
      >
        📝 作文批改（单次）
      </button>
      <button
              :class="['tab-button', { active: activeTab === 'essay-multi-turn' }]"
              @click="activeTab = 'essay-multi-turn'"
      >
        💬 作文批改（多轮对话）
      </button>
      <button
              :class="['tab-button', { active: activeTab === 'shopping' }]"
              @click="activeTab = 'shopping'"
      >
        🛒 购物流程
      </button>
      <button
              :class="['tab-button', { active: activeTab === 'sse-test' }]"
              @click="activeTab = 'sse-test'"
      >
        🧪 SSE测试
      </button>
    </nav>

    <div class="tab-content">
      <MarketReportChat v-if="activeTab === 'market-report'" />
      <MarketReportChatV3 v-else-if="activeTab === 'market-report-v3'" />
      <EssayReviewChat v-else-if="activeTab === 'essay-review'" />
      <EssayMultiTurnChat v-else-if="activeTab === 'essay-multi-turn'" />
      <ShoppingTimeline v-else-if="activeTab === 'shopping'" />
      <SseTestPage v-else-if="activeTab === 'sse-test'" />
    </div>
  </div>
</template>

<style>
.app-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.tab-nav {
  display: flex;
  gap: 0.5rem;
  padding: 0.75rem 1.25rem;
  background: var(--panel);
  border-bottom: 1px solid var(--border);
}

.tab-button {
  padding: 0.5rem 1rem;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: transparent;
  color: var(--muted);
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.tab-button:hover {
  color: var(--text);
  border-color: var(--muted);
}

.tab-button.active {
  background: var(--accent);
  color: #0f1419;
  border-color: var(--accent);
}

.tab-content {
  flex: 1;
}
</style>
