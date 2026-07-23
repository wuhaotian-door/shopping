<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'
import { listPublicArticles } from '@/api/knowledge'
import { listPublicProfiles } from '@/api/profile'
import type { KnowledgeArticleVO, InheritorProfileVO } from '@/types/api'
import HeroBanner from '@/components/HeroBanner.vue'

const authStore = useAuthStore()
const router = useRouter()

onMounted(() => {
  document.title = '码上非遗——感受非遗魅力'
  loadArticles()
  loadMasters()
})

// 登录状态和角色
const isLoggedIn = computed(() => authStore.isLoggedIn)
const userRole = computed(() => authStore.userInfo?.role)
const userName = computed(() => authStore.userInfo?.name)
const isInheritor = computed(() => userRole.value === 'INHERITOR')

// ------- 知识库文章（从API加载） -------
const newsList = ref<KnowledgeArticleVO[]>([])
async function loadArticles() {
  try {
    const res = await listPublicArticles()
    newsList.value = res.data || []
  } catch { /* keep empty */ }
}

/** 首页仅展示前5篇资讯 */
const topNews = computed(() => newsList.value.slice(0, 5))

/** 封面图加载失败时回退到占位符 */
const failedImages = ref(new Set<number>())
function onImgError(articleId: number) {
  failedImages.value.add(articleId)
}

// ------- 传承人列表（从API加载） -------
const masterList = ref<InheritorProfileVO[]>([])
async function loadMasters() {
  try {
    const res = await listPublicProfiles()
    masterList.value = res.data || []
  } catch { /* keep empty */ }
}

/** 首页仅展示前5位传承人 */
const topMasters = computed(() => masterList.value.slice(0, 5))

/** 拆分擅长技艺字符串为数组 */
function splitCrafts(craftTypes: string): string[] {
  return craftTypes ? craftTypes.split(/[,，]/).filter(Boolean) : []
}

function handleLogout() {
  authStore.logout()
  router.push('/')
}
</script>

<template>
  <!-- ==================== 金乌绕日主视觉大图 ==================== -->
  <HeroBanner />

  <!-- ==================== 非遗资讯区域 ==================== -->
  <section class="news-section" id="news">
    <h2 class="section-main-title">非遗资讯中心</h2>
    <div class="news-card-list">
        <div v-for="news in topNews" :key="news.id" class="news-card" @click="router.push(`/news/${news.id}`)">
          <div class="news-card-img">
            <img
              v-if="news.coverUrl && !failedImages.has(news.id)"
              :src="news.coverUrl"
              :alt="news.title"
              class="news-card-img-real"
              @error="onImgError(news.id)"
            />
            <div v-else class="img-placeholder">{{ news.category || '非遗' }}</div>
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
        <div v-if="newsList.length === 0" class="empty-state">暂无资讯内容</div>
        <div v-if="newsList.length > 5" class="view-all-wrap" style="text-align:center;margin-top:20px">
          <button class="view-all-btn" @click="router.push('/news')">查看更多资讯 →</button>
        </div>
      </div>
  </section>

  <!-- ==================== 传承人区域 ==================== -->
  <section class="master-full-section" id="masters">
    <h2 class="section-title">入驻非遗传承人</h2>
    <div class="master-card-grid">
      <div v-for="master in topMasters" :key="master.employeeId" class="master-card">
        <div class="master-card-top">
          <div class="master-avatar-big">
            <span class="master-avatar-initial">{{ (master.name || '传')[0] }}</span>
          </div>
          <div class="master-region-tag" v-if="master.region || master.address">
            📍 {{ master.region || master.address }}
          </div>
        </div>
        <div class="master-card-body">
          <h3>{{ master.name }}</h3>
          <p class="master-tech">{{ master.honorTitle || '非遗传承人' }}</p>
          <!-- 擅长技艺标签 -->
          <div class="master-craft-tags" v-if="splitCrafts(master.craftTypes).length > 0">
            <span v-for="craft in splitCrafts(master.craftTypes).slice(0, 3)" :key="craft" class="master-craft-tag">{{ craft }}</span>
            <span v-if="splitCrafts(master.craftTypes).length > 3" class="master-craft-more">+{{ splitCrafts(master.craftTypes).length - 3 }}</span>
          </div>
          <p class="master-intro">{{ master.selfIntro?.slice(0, 80) || '暂无简介' }}</p>
          <a class="master-book-btn" @click="router.push('/appointment/' + master.employeeId)">预约工坊</a>
        </div>
      </div>
      <div v-if="masterList.length === 0" class="empty-state" style="grid-column:1/-1">暂无入驻传承人</div>
    </div>
    <div v-if="masterList.length > 5" class="view-all-wrap">
      <button class="view-all-btn" @click="router.push('/inheritors')">
        查看全部 {{ masterList.length }} 位传承人 →
      </button>
    </div>
  </section>

  <!-- ==================== 底部 AI ==================== -->
  <section class="bottom-section" id="products">
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
.header-chat-btn {
  padding: 6px 14px;
  font-size: 12px;
  color: #fff;
  background: rgba(255,255,255,0.2);
  border: 1px solid rgba(255,255,255,0.5);
  border-radius: 4px;
  cursor: pointer;
  transition: 0.2s;
}
.header-chat-btn:hover {
  background: rgba(255,255,255,0.35);
}
.header-profile-btn {
  padding: 6px 14px;
  font-size: 12px;
  color: #fff;
  background: rgba(255,255,255,0.2);
  border: 1px solid rgba(255,255,255,0.5);
  border-radius: 4px;
  cursor: pointer;
  transition: 0.2s;
}
.header-profile-btn:hover {
  background: rgba(255,255,255,0.35);
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
  overflow: hidden;
}
.news-card-img-real {
  width: 100%;
  height: 100%;
  object-fit: cover;
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
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  position: relative;
}
.master-avatar-big {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  background: linear-gradient(135deg, #e8647a, #c41e3a);
  border: 4px solid #fff;
  display: flex;
  align-items: center;
  justify-content: center;
}
.master-avatar-initial {
  font-size: 42px;
  color: #fff;
  font-weight: 700;
  user-select: none;
}
.master-region-tag {
  font-size: 12px;
  color: #c41e3a;
  background: rgba(255, 255, 255, 0.85);
  padding: 2px 12px;
  border-radius: 10px;
  font-weight: 500;
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
  margin-bottom: 10px;
}
.master-craft-tags {
  display: flex;
  justify-content: center;
  gap: 6px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}
.master-craft-tag {
  display: inline-block;
  padding: 3px 10px;
  background: #fff5f7;
  color: #c41e3a;
  border: 1px solid #f0c0c8;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}
.master-craft-more {
  display: inline-block;
  padding: 3px 10px;
  background: #f5f5f5;
  color: #999;
  border-radius: 12px;
  font-size: 12px;
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

.view-all-wrap {
  text-align: center;
  margin-top: 32px;
}
.view-all-btn {
  display: inline-block;
  padding: 12px 36px;
  background: transparent;
  color: #c41e3a;
  border: 2px solid #c41e3a;
  border-radius: 24px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: 0.25s;
}
.view-all-btn:hover {
  background: #c41e3a;
  color: #fff;
}

/* ==================== 底部 AI ==================== */
.bottom-section {
  padding: 40px 5%;
  display: grid;
  grid-template-columns: 1fr;
  gap: 32px;
  background-color: #fff;
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
