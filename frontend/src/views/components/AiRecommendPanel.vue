<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { aiRecommend, type RecommendResult } from '@/api/ai'

const props = withDefaults(defineProps<{
  userId?: string
  limit?: number
  title?: string
}>(), {
  userId: 'default',
  limit: 5,
  title: '为你推荐',
})

const loading = ref(false)
const data = ref<RecommendResult | null>(null)
const error = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    data.value = await aiRecommend(props.userId, props.limit)
  } catch (e) {
    error.value = '推荐加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(load)
watch(() => props.userId, load)
</script>

<template>
  <div class="recommend-panel">
    <div class="panel-title">
      {{ title }}
      <span v-if="data?.personalized" class="badge-personalized">个性化</span>
    </div>

    <div v-if="loading" class="loading-text">加载推荐中…</div>
    <div v-else-if="error" class="error-text">{{ error }}</div>
    <div v-else-if="data" class="recommend-content">
      <pre class="recommend-text">{{ data.recommendations }}</pre>
    </div>
    <div v-else class="empty-text">暂无推荐内容</div>
  </div>
</template>

<style scoped>
.recommend-panel {
  background: #fff;
  border-radius: 14px;
  padding: 20px 24px;
  box-shadow: 0 2px 12px rgba(196,30,58,0.04);
  border: 1px solid rgba(196,30,58,0.06);
  margin-bottom: 16px;
}
.panel-title {
  font-size: 17px;
  font-weight: 600;
  color: #333;
  margin-bottom: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.badge-personalized {
  font-size: 11px;
  background: linear-gradient(135deg, #c41e3a, #d43030);
  color: #fff;
  padding: 2px 10px;
  border-radius: 8px;
  font-weight: 500;
}
.recommend-text {
  font-size: 14px;
  line-height: 1.8;
  color: #555;
  white-space: pre-wrap;
  font-family: inherit;
  margin: 0;
}
.loading-text { color: #bbb; font-size: 13px; text-align: center; padding: 20px; }
.error-text { color: #c41e3a; font-size: 13px; text-align: center; padding: 20px; }
.empty-text { color: #ccc; font-size: 13px; text-align: center; padding: 20px; }

@media (max-width: 768px) {
  .recommend-panel { padding: 14px 16px; border-radius: 10px; margin-bottom: 12px; }
  .panel-title { font-size: 15px; margin-bottom: 10px; gap: 6px; }
  .badge-personalized { font-size: 10px; padding: 2px 8px; }
  .recommend-text { font-size: 13px; line-height: 1.7; }
  .loading-text { font-size: 12px; padding: 16px; }
  .error-text { font-size: 12px; padding: 16px; }
  .empty-text { font-size: 12px; padding: 16px; }
}
</style>
