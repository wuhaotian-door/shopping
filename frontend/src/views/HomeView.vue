<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'
import { listPublicArticles } from '@/api/knowledge'
import { listPublicProfiles } from '@/api/profile'
import { listPublicProducts } from '@/api/culturalProduct'
import type { KnowledgeArticleVO, InheritorProfileVO, CulturalProductVO } from '@/types/api'

const authStore = useAuthStore()
const router = useRouter()

onMounted(() => {
  document.title = '码上非遗——感受非遗魅力'
  loadArticles()
  loadMasters()
  loadProducts()
})

// 登录状态和角色
const isLoggedIn = computed(() => authStore.isLoggedIn)
const userRole = computed(() => authStore.userInfo?.role)
const userName = computed(() => authStore.userInfo?.name)
const isInheritor = computed(() => userRole.value === 'INHERITOR')

// ------- 视频轮播 -------
const currentSlide = ref(0)
const slides = [
  { caption: '汇聚剪纸、刺绣、陶艺、竹编等校园非遗技艺，沉浸式感受传统手工艺魅力' },
  { caption: '连接校园与民间手工艺传承人，让非遗文化在年轻一代中薪火相传' },
  { caption: '探索非遗文创产品，体验 AI 智能问答，开启你的非遗文化之旅' },
]

function prevSlide() {
  currentSlide.value = (currentSlide.value - 1 + slides.length) % slides.length
}
function nextSlide() {
  currentSlide.value = (currentSlide.value + 1) % slides.length
}

// ------- 资讯筛选 -------
const activeFilter = ref('')
const filters = ['剪纸', '刺绣', '陶艺', '竹编', '校园活动', '传承人专访']
const currentPage = ref(1)

function filterNews(type: string) {
  activeFilter.value = type
  currentPage.value = 1
}
function goPage(page: number) {
  if (page < 1) return
  currentPage.value = page
}

// ------- 知识库文章（从API加载） -------
const newsList = ref<KnowledgeArticleVO[]>([])
const filteredNews = computed(() => {
  if (!activeFilter.value) return newsList.value
  return newsList.value.filter(a => a.category === activeFilter.value)
})
async function loadArticles() {
  try {
    const res = await listPublicArticles()
    newsList.value = res.data || []
  } catch { /* keep empty */ }
}

// ------- 传承人列表（从API加载） -------
const masterList = ref<InheritorProfileVO[]>([])
async function loadMasters() {
  try {
    const res = await listPublicProfiles()
    masterList.value = res.data || []
  } catch { /* keep empty */ }
}

// ------- 文创产品（从API加载） -------
const productList = ref<CulturalProductVO[]>([])
async function loadProducts() {
  try {
    const res = await listPublicProducts()
    productList.value = res.data || []
  } catch { /* keep empty */ }
}

function handleLogout() {
  authStore.logout()
  router.push('/')
}
</script>

<template>
  <!-- ==================== 顶部导航 ==================== -->
  <header class="page-header">
    <div class="logo" @click="router.push('/')">码上非遗</div>

    <nav class="nav-links">
      <a class="nav-item active" href="#">首页</a>
      <a class="nav-item" href="#news">非遗资讯</a>
      <a class="nav-item" href="#masters">传承人</a>
      <a class="nav-item" @click="router.push('/store')">文创周边</a>
      <a class="nav-item" href="#ai">智能问答</a>

      <!-- 非遗传承人专属导航 -->
      <template v-if="isInheritor">
        <span class="nav-separator"></span>
        <a class="nav-item nav-inheritor" @click="router.push('/inheritor')">我的工坊</a>
        <a class="nav-item nav-inheritor" @click="router.push('/inheritor')">工坊管理</a>
      </template>
    </nav>

    <!-- 右侧：游客 → 登录按钮 | 已登录 → 用户菜单 -->
    <div class="header-right">
      <template v-if="isLoggedIn">
        <span class="user-role-tag" :class="userRole === 'INHERITOR' ? 'inheritor-tag' : 'user-tag'">
          {{ isInheritor ? '非遗传承人' : '用户' }}
        </span>
        <span class="user-name">{{ userName }}</span>
        <button class="logout-btn" @click="handleLogout">退出</button>
      </template>
      <template v-else>
        <button class="login-top-btn" @click="router.push('/login')">登录</button>
      </template>
    </div>
  </header>

  <!-- ==================== 视频轮播横幅 ==================== -->
  <section class="banner">
    <div class="banner-video-wrap">
      <div class="banner-placeholder">
        <div class="banner-pattern-overlay"></div>
        <div class="banner-center-text">
          <h2>码上非遗</h2>
          <p>感受非遗魅力</p>
          <button v-if="!isLoggedIn" class="banner-join-btn" @click="router.push('/register')">立即加入</button>
          <button v-if="isInheritor" class="banner-join-btn inheritor-btn" @click="router.push('/inheritor')">进入我的工坊</button>
        </div>
      </div>
    </div>
    <div class="banner-caption">
      <p>{{ slides[currentSlide].caption }}</p>
    </div>
    <button class="banner-btn banner-prev" @click="prevSlide">‹</button>
    <button class="banner-btn banner-next" @click="nextSlide">›</button>
    <div class="banner-dots">
      <span
          v-for="(slide, i) in slides"
          :key="i"
          class="dot"
          :class="{ active: i === currentSlide }"
          @click="currentSlide = i"
      ></span>
    </div>
  </section>

  <!-- ==================== 非遗资讯区域 ==================== -->
  <section class="news-section" id="news">
    <h2 class="section-main-title">非遗资讯中心</h2>
    <div class="news-wrap">
      <!-- 左侧筛选栏 -->
      <aside class="news-filter-sidebar">
        <h3>分类筛选</h3>
        <div class="filter-tag-list">
          <div class="filter-tag" :class="{ active: activeFilter === '' }" @click="filterNews('')">全部资讯</div>
          <div v-for="f in filters" :key="f" class="filter-tag" :class="{ active: activeFilter === f }" @click="filterNews(f)">{{ f }}</div>
        </div>
        <h3 style="margin-top: 24px">发布时间</h3>
        <div class="filter-tag-list">
          <div class="filter-tag">近一周</div>
          <div class="filter-tag">近一月</div>
          <div class="filter-tag">今年</div>
        </div>
        <h3 style="margin-top: 24px">排序方式</h3>
        <div class="filter-tag-list">
          <div class="filter-tag active">最新发布</div>
          <div class="filter-tag">浏览量最高</div>
        </div>
      </aside>

      <!-- 右侧资讯列表 -->
      <div class="news-card-list">
        <div v-for="news in filteredNews" :key="news.id" class="news-card">
          <div class="news-card-img">
            <div class="img-placeholder">{{ news.category || '非遗' }}</div>
          </div>
          <div class="news-card-text">
            <h4>{{ news.title }}</h4>
            <p class="desc">{{ news.content?.slice(0, 100) || '' }}</p>
            <div class="news-card-bottom">
              <span>{{ news.createTime?.slice(0, 10) }}</span>
              <span>浏览 {{ news.viewCount }}</span>
            </div>
          </div>
        </div>
        <div v-if="filteredNews.length === 0" class="empty-state">{{ activeFilter ? '该分类暂无资讯' : '暂无资讯内容' }}</div>
        <div class="news-page-bar">
          <span @click="goPage(currentPage - 1)">上一页</span>
          <span class="active">{{ currentPage }}</span>
          <span @click="goPage(currentPage + 1)">下一页</span>
        </div>
      </div>
    </div>
  </section>

  <!-- ==================== 传承人区域 ==================== -->
  <section class="master-full-section" id="masters">
    <h2 class="section-title">入驻非遗传承人</h2>
    <div class="master-card-grid">
      <div v-for="master in masterList" :key="master.employeeId" class="master-card">
        <div class="master-card-top">
          <div class="master-avatar-big"></div>
        </div>
        <div class="master-card-body">
          <h3>{{ master.name }}</h3>
          <p class="master-tech">{{ master.honorTitle || '非遗传承人' }}</p>
          <p class="master-intro">{{ master.selfIntro?.slice(0, 80) || '' }}</p>
          <a href="#" class="master-book-btn">预约工坊</a>
        </div>
      </div>
      <div v-if="masterList.length === 0" class="empty-state" style="grid-column:1/-1">暂无入驻传承人</div>
    </div>
  </section>

  <!-- ==================== 底部文创 + AI ==================== -->
  <section class="bottom-section" id="products">
    <div>
      <h2 class="section-title">文创商品推荐</h2>
      <div v-for="product in productList" :key="product.id" class="product-card">
        <div class="product-image">
          <div class="img-placeholder-sm">文创</div>
        </div>
        <div class="product-info">
          <h3>{{ product.productName }}</h3>
          <p>{{ product.detailBrief || '' }}</p>
          <div class="product-price" v-if="product.price">¥{{ product.price }}</div>
        </div>
      </div>
    </div>

    <div id="ai">
      <h2 class="section-title">AI 非遗小百科</h2>
      <div class="ai-chat-box">
        <div class="chat-bubble">剪纸怎么入门？</div>
        <div class="chat-bubble">了解更多非遗知识</div>
        <p class="ai-desc">智能问答入口，依托 RAG 知识库帮助用户快速了解非遗技艺、传承历史和文创产品信息。</p>
        <a href="#" class="ai-ask-btn">立即提问</a>
      </div>
    </div>
  </section>

  <!-- ==================== 页脚 ==================== -->
  <footer class="page-footer">
    <p>校园非遗与文创传承人平台 © 2026 码上非遗</p>
  </footer>
</template>

<style scoped>
/* ==================== 全局重置 ==================== */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

/* ==================== 顶部导航 ==================== */
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
.logo {
  font-size: 20px;
  font-weight: bold;
  cursor: pointer;
}
.nav-links {
  display: flex;
  align-items: center;
  gap: 20px;
}
.nav-item {
  color: #fff;
  text-decoration: none;
  font-size: 14px;
  cursor: pointer;
  padding: 4px 0;
  border-bottom: 2px solid transparent;
  transition: border-color 0.2s;
}
.nav-item:hover,
.nav-item.active {
  border-bottom-color: #fff;
}
.nav-separator {
  width: 1px;
  height: 18px;
  background: rgba(255,255,255,0.4);
  margin: 0 4px;
}
.nav-inheritor {
  color: #ffd700;
}
.nav-inheritor:hover {
  border-bottom-color: #ffd700;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.user-role-tag {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: bold;
}
.user-tag {
  background: #fff;
  color: #c41e3a;
}
.inheritor-tag {
  background: #ffd700;
  color: #8b4513;
}
.user-name {
  font-size: 14px;
}
.logout-btn {
  padding: 6px 14px;
  font-size: 12px;
  color: #fff;
  background: rgba(255,255,255,0.15);
  border: 1px solid rgba(255,255,255,0.4);
  border-radius: 4px;
  cursor: pointer;
  transition: 0.2s;
}
.logout-btn:hover {
  background: rgba(255,255,255,0.3);
}
.login-top-btn {
  padding: 8px 22px;
  font-size: 14px;
  color: #c41e3a;
  background: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-weight: bold;
  transition: 0.2s;
}
.login-top-btn:hover {
  background: #ffe9ec;
}

/* ==================== 视频轮播横幅 ==================== */
.banner {
  width: 100%;
  height: 460px;
  position: relative;
  overflow: hidden;
}
.banner-video-wrap {
  width: 100%;
  height: 100%;
}
.banner-placeholder {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #c41e3a 0%, #e8647a 50%, #f0a0b0 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}
.banner-pattern-overlay {
  position: absolute;
  inset: 0;
  background:
      radial-gradient(circle at 20% 50%, rgba(255,255,255,0.08) 0%, transparent 50%),
      radial-gradient(circle at 80% 50%, rgba(255,255,255,0.06) 0%, transparent 50%);
}
.banner-center-text {
  text-align: center;
  color: #fff;
  z-index: 2;
}
.banner-center-text h2 {
  font-size: 56px;
  letter-spacing: 6px;
  margin-bottom: 12px;
  text-shadow: 0 2px 12px rgba(0,0,0,0.2);
}
.banner-center-text p {
  font-size: 22px;
  opacity: 0.9;
  letter-spacing: 4px;
}
.banner-join-btn {
  margin-top: 24px;
  padding: 12px 36px;
  font-size: 16px;
  color: #c41e3a;
  background: #fff;
  border: none;
  border-radius: 24px;
  font-weight: bold;
  cursor: pointer;
  transition: 0.2s;
}
.banner-join-btn:hover {
  background: #ffe9ec;
  transform: scale(1.05);
}
.banner-join-btn.inheritor-btn {
  color: #8b4513;
  background: #ffd700;
}
.banner-join-btn.inheritor-btn:hover {
  background: #ffe44d;
}
.banner-caption {
  position: absolute;
  left: 0;
  bottom: 0;
  width: 100%;
  padding: 30px 5%;
  background: linear-gradient(to top, rgba(196, 30, 58, 0.75), transparent 75%);
  color: #fff;
}
.banner-caption p {
  font-size: 17px;
  line-height: 1.6;
  max-width: 700px;
}
.banner-btn {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: rgba(255,255,255,0.22);
  border: none;
  color: #fff;
  font-size: 24px;
  cursor: pointer;
  transition: 0.25s;
  line-height: 44px;
  text-align: center;
}
.banner-btn:hover {
  background: rgba(255,255,255,0.45);
}
.banner-prev { left: 3%; }
.banner-next { right: 3%; }
.banner-dots {
  position: absolute;
  bottom: 14px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 10px;
}
.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: rgba(255,255,255,0.35);
  cursor: pointer;
  transition: 0.2s;
}
.dot.active { background: #fff; }

/* ==================== 非遗资讯区域 ==================== */
.news-section {
  padding: 50px 5%;
  background-color: #fff;
}
.section-main-title {
  text-align: center;
  color: #c41e3a;
  margin-bottom: 40px;
  font-size: 26px;
  position: relative;
}
.section-main-title::after {
  content: "";
  width: 60px;
  height: 3px;
  background: #c41e3a;
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  bottom: -10px;
}
.news-wrap {
  display: grid;
  grid-template-columns: 240px 1fr;
  gap: 30px;
}
.news-filter-sidebar {
  background: #fff5f7;
  border-radius: 10px;
  padding: 24px;
}
.news-filter-sidebar h3 {
  color: #c41e3a;
  margin-bottom: 16px;
  font-size: 16px;
  border-left: 4px solid #c41e3a;
  padding-left: 10px;
}
.filter-tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.filter-tag {
  padding: 6px 14px;
  border: 1px solid #e8647a;
  border-radius: 20px;
  font-size: 13px;
  cursor: pointer;
  transition: 0.2s;
  user-select: none;
}
.filter-tag.active {
  background: #c41e3a;
  color: #fff;
  border-color: #c41e3a;
}
.filter-tag:hover:not(.active) {
  background: #ffe9ec;
}
.news-card-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.news-card {
  display: flex;
  gap: 16px;
  border: 1px solid #eee;
  border-radius: 10px;
  overflow: hidden;
  transition: 0.3s;
  cursor: pointer;
}
.news-card:hover {
  box-shadow: 0 6px 18px rgba(196,30,58,0.12);
  transform: translateY(-3px);
}
.news-card-img {
  width: 160px;
  height: 120px;
  flex-shrink: 0;
}
.img-placeholder {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #e8647a, #c41e3a);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 14px;
  font-weight: bold;
}
.news-card-text {
  padding: 12px 12px 12px 0;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  flex: 1;
}
.news-card-text h4 {
  font-size: 17px;
  color: #222;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.news-card-text .desc {
  font-size: 13px;
  color: #666;
  line-height: 1.6;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
.news-card-bottom {
  margin-top: 8px;
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #999;
}
.empty-state {
  text-align: center;
  padding: 40px;
  color: #999;
  font-size: 14px;
}
.news-page-bar {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-top: 10px;
}
.news-page-bar span {
  padding: 6px 14px;
  border: 1px solid #ddd;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
  transition: 0.2s;
}
.news-page-bar span.active {
  background: #c41e3a;
  color: #fff;
  border-color: #c41e3a;
}
.news-page-bar span:hover:not(.active) {
  background: #fef0f2;
}

/* ==================== 传承人区域 ==================== */
.master-full-section {
  padding: 50px 5%;
  background: #fcf4e8;
}
.section-title {
  color: #c41e3a;
  margin-bottom: 35px;
  font-size: 26px;
  text-align: center;
  position: relative;
}
.section-title::after {
  content: "";
  width: 60px;
  height: 3px;
  background: #c41e3a;
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  bottom: -10px;
}
.master-card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 28px;
}
.master-card {
  background-color: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 14px rgba(0,0,0,0.07);
  transition: 0.3s;
}
.master-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 10px 26px rgba(196,30,58,0.14);
}
.master-card-top {
  height: 180px;
  background: linear-gradient(135deg, #ffe9ec, #fef0f2);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}
.master-avatar-big {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  background: linear-gradient(135deg, #e8647a, #c41e3a);
  border: 4px solid #fff;
}
.master-card-body {
  padding: 20px;
  text-align: center;
}
.master-card-body h3 {
  font-size: 20px;
  color: #222;
  margin-bottom: 6px;
}
.master-tech {
  color: #c41e3a;
  font-size: 14px;
  margin-bottom: 12px;
}
.master-intro {
  font-size: 13px;
  color: #666;
  line-height: 1.7;
  margin-bottom: 18px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
}
.master-book-btn {
  display: inline-block;
  padding: 9px 24px;
  background: #c41e3a;
  color: #fff;
  text-decoration: none;
  border-radius: 22px;
  font-size: 14px;
  transition: 0.2s;
}
.master-book-btn:hover {
  background: #a8162b;
}

/* ==================== 底部文创 + AI ==================== */
.bottom-section {
  padding: 40px 5%;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 32px;
  background-color: #fff;
}
.product-card {
  display: flex;
  gap: 16px;
  background-color: #fff5f7;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;
  transition: 0.2s;
}
.product-card:hover {
  background-color: #ffe9ec;
}
.product-image {
  width: 80px;
  height: 80px;
  flex-shrink: 0;
  border-radius: 8px;
  overflow: hidden;
}
.img-placeholder-sm {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #e8647a, #f0a0b0);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 12px;
}
.product-info h3 {
  font-size: 16px;
  color: #222;
  margin-bottom: 6px;
}
.product-info p {
  font-size: 13px;
  color: #666;
  line-height: 1.6;
}
.product-price {
  margin-top: 6px;
  font-size: 16px;
  color: #c41e3a;
  font-weight: bold;
}
.ai-chat-box {
  background-color: #fff5f7;
  border: 1px solid #e8647a;
  border-radius: 12px;
  padding: 24px;
}
.chat-bubble {
  background-color: #ffe9ec;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 12px;
  font-size: 14px;
  color: #555;
}
.ai-desc {
  margin-top: 16px;
  color: #666;
  font-size: 14px;
  line-height: 1.7;
}
.ai-ask-btn {
  display: inline-block;
  margin-top: 16px;
  padding: 10px 20px;
  background: #c41e3a;
  color: #fff;
  border-radius: 6px;
  text-decoration: none;
  font-size: 14px;
  transition: 0.2s;
}
.ai-ask-btn:hover {
  background: #a8162b;
}

/* ==================== 页脚 ==================== */
.page-footer {
  background-color: #c41e3a;
  color: #fff;
  text-align: center;
  padding: 24px 5%;
}

/* ==================== 响应式 ==================== */
@media (max-width: 992px) {
  .banner { height: 360px; }
  .banner-center-text h2 { font-size: 40px; }
  .news-wrap { grid-template-columns: 1fr; }
  .bottom-section { grid-template-columns: 1fr; }
  .nav-links { gap: 12px; }
}
@media (max-width: 576px) {
  .page-header {
    flex-direction: column;
    height: auto;
    padding: 12px 16px;
    gap: 10px;
  }
  .nav-links { flex-wrap: wrap; justify-content: center; gap: 10px; }
  .banner { height: 280px; }
  .banner-center-text h2 { font-size: 32px; }
  .banner-center-text p { font-size: 16px; }
  .banner-caption p { font-size: 14px; }
  .news-card { flex-direction: column; }
  .news-card-img { width: 100%; height: 160px; }
  .master-card-grid { grid-template-columns: 1fr; }
}
</style>
