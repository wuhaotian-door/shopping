<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { aiSearch, type SearchResult } from '@/api/ai'

const query = ref('')
const activeType = ref('')
const loading = ref(false)
const results = ref<SearchResult['results']>([])
const total = ref(0)
const searched = ref(false)

const types = [
  { key: '', label: '全部' },
  { key: 'article', label: '文章' },
  { key: 'inheritor', label: '传承人' },
  { key: 'project', label: '项目' },
  { key: 'product', label: '商品' },
]

const typeIcons: Record<string, string> = {
  article: '📄',
  inheritor: '👤',
  project: '🎯',
  product: '🛍️',
}

async function doSearch() {
  const q = query.value.trim()
  if (!q) return
  loading.value = true
  searched.value = true
  try {
    const res = await aiSearch(q, activeType.value)
    results.value = res.results
    total.value = res.total
  } catch {
    results.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function filterType(key: string) {
  activeType.value = key
  if (query.value.trim()) doSearch()
}

onMounted(() => {
  document.title = '语义搜索 — 码上非遗'
})
</script>

<template>
  <div class="search-page">
    <!-- ===== 搜索区 ===== -->
    <div class="search-hero">
      <div class="search-icon">🔍</div>
      <h1>非遗语义搜索</h1>
      <p>用自然语言搜索非遗内容，支持口语化表达</p>

      <div class="search-box">
        <input
          v-model="query"
          type="text"
          placeholder="试试：学刺绣去哪找老师？适合送长辈的非遗礼物？"
          @keyup.enter="doSearch"
        />
        <button @click="doSearch" :disabled="loading">{{ loading ? '搜索中…' : '搜索' }}</button>
      </div>

      <!-- 类型筛选 -->
      <div class="filter-bar">
        <button
          v-for="t in types" :key="t.key"
          class="filter-btn"
          :class="{ active: activeType === t.key }"
          @click="filterType(t.key)"
        >{{ t.label }}</button>
      </div>
    </div>

    <!-- ===== 结果区 ===== -->
    <div class="search-results">
      <div v-if="loading" class="loading-text">搜索中…</div>

      <template v-else-if="searched">
        <div v-if="results.length > 0" class="results-header">
          找到 <strong>{{ total }}</strong> 条相关结果
        </div>

        <div v-if="results.length > 0" class="result-list">
          <div v-for="(r, i) in results" :key="i" class="result-card">
            <div class="card-header">
              <span class="card-type">{{ typeIcons[r.content_type] || '📄' }} {{ types.find(t => t.key === r.content_type)?.label || r.content_type }}</span>
              <span class="card-source">{{ r.source }}</span>
            </div>
            <div class="card-content">{{ r.content }}</div>
          </div>
        </div>

        <div v-else class="empty-result">
          <div class="empty-icon">📭</div>
          <h3>未找到相关内容</h3>
          <p>请尝试使用不同的关键词或更宽泛的表达方式</p>
          <div class="empty-tips">
            <span>💡 试试：</span>
            <button v-for="q in ['非遗手工体验', '传统技艺', '民间艺术']" :key="q"
              class="tip-btn" @click="query = q; doSearch()">{{ q }}</button>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.search-page {
  min-height: calc(100vh - 116px);
  background: #fcf4e8;
}

/* ===== 搜索 Hero ===== */
.search-hero {
  text-align: center;
  padding: 60px 20px 40px;
  background: linear-gradient(180deg, #fff 0%, #fcf4e8 100%);
}
.search-icon { font-size: 60px; margin-bottom: 12px; }
.search-hero h1 { font-size: 28px; color: #c41e3a; margin: 0 0 8px; }
.search-hero p { font-size: 15px; color: #999; margin: 0 0 28px; }

.search-box {
  display: flex;
  max-width: 600px;
  margin: 0 auto;
  gap: 0;
  box-shadow: 0 4px 20px rgba(196,30,58,0.1);
  border-radius: 28px;
  overflow: hidden;
}
.search-box input {
  flex: 1;
  height: 52px;
  padding: 0 22px;
  font-size: 15px;
  border: 2px solid #c41e3a;
  border-right: none;
  outline: none;
  border-radius: 28px 0 0 28px;
  font-family: inherit;
}
.search-box button {
  padding: 0 32px;
  font-size: 15px;
  font-weight: 600;
  background: linear-gradient(135deg, #c41e3a, #d43030);
  color: #fff;
  border: none;
  cursor: pointer;
  border-radius: 0 28px 28px 0;
  transition: opacity 0.2s;
}
.search-box button:hover:not(:disabled) { opacity: 0.9; }
.search-box button:disabled { opacity: 0.5; cursor: not-allowed; }

/* 类型筛选 */
.filter-bar {
  display: flex;
  gap: 8px;
  justify-content: center;
  margin-top: 20px;
  flex-wrap: wrap;
}
.filter-btn {
  padding: 6px 18px;
  font-size: 13px;
  border: 1px solid rgba(196,30,58,0.15);
  background: #fff;
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.2s;
  color: #666;
}
.filter-btn:hover { background: #fff5f7; }
.filter-btn.active {
  background: #c41e3a;
  color: #fff;
  border-color: #c41e3a;
}

/* ===== 结果 ===== */
.search-results {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}
.loading-text { text-align: center; padding: 40px; color: #999; }
.results-header { font-size: 14px; color: #888; margin-bottom: 16px; }

.result-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.result-card {
  background: #fff;
  border-radius: 12px;
  padding: 18px 22px;
  box-shadow: 0 2px 12px rgba(196,30,58,0.04);
  border: 1px solid rgba(196,30,58,0.06);
  transition: all 0.2s;
}
.result-card:hover { transform: translateY(-2px); box-shadow: 0 4px 16px rgba(196,30,58,0.08); }
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
.card-type {
  font-size: 12px;
  color: #c41e3a;
  background: #ffe9ec;
  padding: 2px 10px;
  border-radius: 8px;
  font-weight: 600;
}
.card-source { font-size: 12px; color: #ccc; }
.card-content {
  font-size: 14px;
  color: #555;
  line-height: 1.7;
  display: -webkit-box;
  -webkit-line-clamp: 4;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 空结果 */
.empty-result {
  text-align: center;
  padding: 60px 20px;
}
.empty-icon { font-size: 56px; margin-bottom: 16px; }
.empty-result h3 { font-size: 20px; color: #888; margin: 0 0 8px; }
.empty-result p { color: #bbb; font-size: 14px; margin: 0 0 20px; }
.empty-tips { display: flex; align-items: center; gap: 8px; justify-content: center; flex-wrap: wrap; }
.empty-tips span { font-size: 13px; color: #999; }
.tip-btn {
  padding: 6px 14px;
  font-size: 12px;
  background: #fff5f7;
  border: 1px solid #ffe9ec;
  border-radius: 14px;
  cursor: pointer;
  color: #c41e3a;
}
.tip-btn:hover { background: #ffe9ec; }

@media (max-width: 768px) {
  .search-hero { padding: 30px 16px 24px; }
  .search-box input { font-size: 14px; }
}
</style>
