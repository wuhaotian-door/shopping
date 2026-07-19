<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'
import { listPublicArticles } from '@/api/knowledge'
import type { KnowledgeArticleVO } from '@/types/api'

const authStore = useAuthStore()
const router = useRouter()

onMounted(() => {
  document.title = '非遗资讯 — 码上非遗'
  loadArticles()
})

// ===== 登录状态 =====
const isLoggedIn = computed(() => authStore.isLoggedIn)
const userName = computed(() => authStore.userInfo?.name)
const userRole = computed(() => authStore.userInfo?.role)
const isInheritor = computed(() => userRole.value === 'INHERITOR')

function handleLogout() {
  authStore.logout()
  router.push('/')
}

// ===== 文章列表 =====
const newsList = ref<KnowledgeArticleVO[]>([])
const loading = ref(true)

async function loadArticles() {
  loading.value = true
  try {
    const res = await listPublicArticles()
    newsList.value = res.data || []
  } catch {
    // ignore
  } finally {
    loading.value = false
  }
}

// ===== 筛选 & 排序 =====
const activeCategory = ref('全部')
const categories = ['全部', '剪纸', '刺绣', '陶艺', '竹编', '校园活动', '传承人专访']

const activeTimeFilter = ref('')
const timeFilters = [
  { label: '全部时间', value: '' },
  { label: '近一周', value: 'week' },
  { label: '近一月', value: 'month' },
  { label: '今年', value: 'year' },
]

const activeSort = ref('latest')
const sortOptions = [
  { label: '最新发布', value: 'latest' },
  { label: '浏览量最高', value: 'views' },
]

const filteredNews = computed(() => {
  let list = newsList.value

  // 分类筛选
  if (activeCategory.value !== '全部') {
    list = list.filter(a => a.category === activeCategory.value)
  }

  // 时间筛选
  const now = Date.now()
  if (activeTimeFilter.value === 'week') {
    const weekAgo = now - 7 * 24 * 60 * 60 * 1000
    list = list.filter(a => new Date(a.createTime).getTime() >= weekAgo)
  } else if (activeTimeFilter.value === 'month') {
    const monthAgo = now - 30 * 24 * 60 * 60 * 1000
    list = list.filter(a => new Date(a.createTime).getTime() >= monthAgo)
  } else if (activeTimeFilter.value === 'year') {
    const yearStart = new Date(new Date().getFullYear(), 0, 1).getTime()
    list = list.filter(a => new Date(a.createTime).getTime() >= yearStart)
  }

  // 排序
  if (activeSort.value === 'views') {
    list = [...list].sort((a, b) => b.viewCount - a.viewCount)
  } else {
    list = [...list].sort((a, b) => new Date(b.createTime).getTime() - new Date(a.createTime).getTime())
  }

  return list
})
</script>

<template>
  <div class="news-page">
    <!-- ==================== 顶部导航 ==================== -->
    <header class="page-header">
      <div class="logo" @click="router.push('/')">码上非遗</div>

      <nav class="nav-links">
        <a class="nav-item" @click="router.push('/')">首页</a>
        <a class="nav-item active" @click="router.push('/news')">非遗资讯</a>
        <a class="nav-item" @click="router.push('/inheritors')">传承人</a>
        <a class="nav-item" @click="router.push('/store')">文创周边</a>
        <a class="nav-item" @click="router.push('/')">智能问答</a>
        <template v-if="isInheritor">
          <span class="nav-separator"></span>
          <a class="nav-item nav-inheritor" @click="router.push('/inheritor')">我的工坊</a>
        </template>
      </nav>

      <div class="header-right">
        <template v-if="isLoggedIn">
          <span class="user-role-tag" :class="isInheritor ? 'inheritor-tag' : 'user-tag'">
            {{ isInheritor ? '非遗传承人' : '用户' }}
          </span>
          <span class="user-name">{{ userName }}</span>
          <button v-if="!isInheritor" class="header-btn" @click="router.push('/profile')">👤 个人中心</button>
          <button class="header-btn" @click="router.push('/chat')">💬 消息</button>
          <button class="logout-btn" @click="handleLogout">退出</button>
        </template>
        <template v-else>
          <button class="login-top-btn" @click="router.push('/login')">登录</button>
        </template>
      </div>
    </header>

    <!-- ==================== 主体 ==================== -->
    <main class="news-main">
      <div class="section-header">
        <h2>非遗资讯中心</h2>
        <p class="section-subtitle">共 {{ newsList.length }} 篇资讯</p>
      </div>

      <!-- 筛选 & 排序栏 -->
      <div class="filter-section">
        <!-- 分类筛选 -->
        <div class="filter-row">
          <span class="filter-label">分类：</span>
          <div class="filter-tag-list">
            <span
              v-for="cat in categories"
              :key="cat"
              class="filter-tag"
              :class="{ active: activeCategory === cat }"
              @click="activeCategory = cat"
            >{{ cat }}</span>
          </div>
        </div>

        <!-- 时间筛选 + 排序 -->
        <div class="filter-row">
          <span class="filter-label">时间：</span>
          <div class="filter-tag-list">
            <span
              v-for="t in timeFilters"
              :key="t.value"
              class="filter-tag"
              :class="{ active: activeTimeFilter === t.value }"
              @click="activeTimeFilter = t.value"
            >{{ t.label }}</span>
          </div>
          <span class="filter-label" style="margin-left: 24px">排序：</span>
          <div class="filter-tag-list">
            <span
              v-for="s in sortOptions"
              :key="s.value"
              class="filter-tag"
              :class="{ active: activeSort === s.value }"
              @click="activeSort = s.value"
            >{{ s.label }}</span>
          </div>
        </div>
      </div>

      <!-- 加载中 -->
      <div v-if="loading" class="loading-state">加载中...</div>

      <!-- 文章列表 -->
      <div v-else class="news-grid">
        <div v-for="article in filteredNews" :key="article.id" class="news-card" @click="router.push(`/news/${article.id}`)">
          <div class="news-card-img">
            <div class="img-placeholder">{{ article.category || '非遗' }}</div>
          </div>
          <div class="news-card-body">
            <span class="news-category-tag">{{ article.category }}</span>
            <h3>{{ article.title }}</h3>
            <p class="news-desc">{{ article.content?.slice(0, 120) || '' }}</p>
            <div class="news-meta">
              <span>📅 {{ article.createTime?.slice(0, 10) }}</span>
              <span>👁 浏览 {{ article.viewCount }}</span>
              <span>✍️ {{ article.authorName }}</span>
            </div>
          </div>
        </div>
        <div v-if="filteredNews.length === 0" class="empty-state">暂无符合条件的资讯</div>
      </div>
    </main>

    <!-- ==================== 页脚 ==================== -->
    <footer class="page-footer">
      <p>校园非遗与文创传承人平台 © 2026 码上非遗</p>
    </footer>
  </div>
</template>

<style scoped>
.news-page {
  min-height: 100vh;
  background: #fcf4e8;
  display: flex;
  flex-direction: column;
}

/* ---- 导航 ---- */
.page-header {
  background-color: #c41e3a;
  color: #fff;
  padding: 0 5%;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: sticky;
  top: 0;
  z-index: 100;
}
.logo { font-size: 20px; font-weight: bold; cursor: pointer; }
.nav-links { display: flex; align-items: center; gap: 20px; }
.nav-item {
  color: #fff; text-decoration: none; font-size: 14px; cursor: pointer;
  padding: 4px 0; border-bottom: 2px solid transparent; transition: border-color 0.2s;
}
.nav-item:hover, .nav-item.active { border-bottom-color: #fff; }
.nav-separator { width: 1px; height: 18px; background: rgba(255,255,255,0.4); margin: 0 4px; }
.nav-inheritor { color: #ffd700; }
.nav-inheritor:hover { border-bottom-color: #ffd700; }
.header-right { display: flex; align-items: center; gap: 10px; }
.user-role-tag { font-size: 11px; padding: 2px 8px; border-radius: 10px; font-weight: bold; }
.user-tag { background: rgba(255,255,255,0.2); color: #fff; }
.inheritor-tag { background: #ffd700; color: #c41e3a; }
.user-name { font-size: 14px; color: #fff; margin-right: 4px; }
.header-btn {
  padding: 6px 12px; font-size: 12px; color: #fff;
  background: rgba(255,255,255,0.2); border: 1px solid rgba(255,255,255,0.5);
  border-radius: 4px; cursor: pointer; transition: 0.2s;
}
.header-btn:hover { background: rgba(255,255,255,0.35); }
.logout-btn {
  padding: 6px 12px; font-size: 12px; color: #c41e3a;
  background: #fff; border: none; border-radius: 4px; cursor: pointer; transition: 0.2s;
}
.logout-btn:hover { background: #f5f5f5; }
.login-top-btn {
  padding: 8px 22px; font-size: 14px; color: #c41e3a;
  background: #fff; border: none; border-radius: 4px; cursor: pointer; font-weight: bold;
}

/* ---- 主体 ---- */
.news-main { flex: 1; max-width: 1000px; margin: 0 auto; padding: 40px 20px; }
.section-header { text-align: center; margin-bottom: 24px; }
.section-header h2 { font-size: 28px; color: #c41e3a; margin-bottom: 8px; }
.section-subtitle { font-size: 15px; color: #999; }

/* ---- 分类筛选 ---- */
/* ---- 筛选栏 ---- */
.filter-section {
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  margin-bottom: 28px;
}
.filter-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.filter-row + .filter-row { margin-top: 14px; padding-top: 14px; border-top: 1px solid #f5f5f5; }
.filter-label {
  font-size: 14px;
  color: #888;
  font-weight: 600;
  white-space: nowrap;
}
.filter-tag-list { display: flex; gap: 8px; flex-wrap: wrap; }
.filter-tag {
  padding: 6px 16px; font-size: 13px; color: #666;
  background: #f8f8f8; border: 1px solid #e8e8e8; border-radius: 18px;
  cursor: pointer; transition: 0.2s; user-select: none;
}
.filter-tag:hover { border-color: #c41e3a; color: #c41e3a; background: #fff5f7; }
.filter-tag.active { background: #c41e3a; color: #fff; border-color: #c41e3a; }

/* ---- 文章列表 ---- */
.news-grid { display: flex; flex-direction: column; gap: 20px; }
.news-card {
  display: flex; background: #fff; border-radius: 12px;
  overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.06);
  transition: 0.3s; cursor: pointer;
}
.news-card:hover { transform: translateY(-3px); box-shadow: 0 6px 20px rgba(196,30,58,0.1); }
.news-card-img {
  width: 240px; min-height: 180px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
}
.img-placeholder {
  width: 100%; height: 100%;
  background: linear-gradient(135deg, #ffe9ec, #fef0f2);
  display: flex; align-items: center; justify-content: center;
  font-size: 28px; font-weight: bold; color: #c41e3a;
}
.news-card-body { padding: 20px 24px; flex: 1; display: flex; flex-direction: column; }
.news-category-tag {
  display: inline-block; padding: 3px 10px; background: #fff5f7;
  color: #c41e3a; border-radius: 10px; font-size: 12px; font-weight: 600;
  width: fit-content; margin-bottom: 8px;
}
.news-card-body h3 { font-size: 18px; color: #222; margin-bottom: 8px; line-height: 1.4; }
.news-desc { font-size: 14px; color: #666; line-height: 1.7; flex: 1; }
.news-meta { display: flex; gap: 18px; margin-top: 12px; font-size: 13px; color: #999; }

/* ---- 通用 ---- */
.loading-state { text-align: center; padding: 80px 0; color: #999; }
.empty-state { text-align: center; padding: 60px 0; color: #999; font-size: 15px; }

/* ---- 页脚 ---- */
.page-footer { background-color: #c41e3a; color: #fff; text-align: center; padding: 24px 5%; margin-top: auto; }

@media (max-width: 768px) {
  .page-header { padding: 0 16px; flex-wrap: wrap; }
  .nav-links { gap: 12px; }
  .nav-item { font-size: 12px; }
  .header-right { flex-wrap: wrap; }
  .news-card { flex-direction: column; }
  .news-card-img { width: 100%; min-height: 160px; }
}
</style>
