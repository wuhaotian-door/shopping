<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'
import { listPublicProfiles } from '@/api/profile'
import type { InheritorProfileVO } from '@/types/api'

const authStore = useAuthStore()
const router = useRouter()

onMounted(() => {
  document.title = '非遗传承人 — 码上非遗'
  loadMasters()
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

// ===== 传承人列表 =====
const masterList = ref<InheritorProfileVO[]>([])
const loading = ref(true)

async function loadMasters() {
  loading.value = true
  try {
    const res = await listPublicProfiles()
    masterList.value = res.data || []
  } catch {
    // ignore
  } finally {
    loading.value = false
  }
}

/** 拆分擅长技艺字符串为数组 */
function splitCrafts(craftTypes: string): string[] {
  return craftTypes ? craftTypes.split(/[,，]/).filter(Boolean) : []
}
</script>

<template>
  <div class="inheritor-page">
    <!-- ==================== 顶部导航（与首页/文创周边一致） ==================== -->
    <header class="page-header">
      <div class="logo" @click="router.push('/')">码上非遗</div>

      <nav class="nav-links">
        <a class="nav-item" @click="router.push('/')">首页</a>
        <a class="nav-item" @click="router.push('/news')">非遗资讯</a>
        <a class="nav-item active" @click="router.push('/inheritors')">传承人</a>
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

    <!-- ==================== 主体内容 ==================== -->
    <main class="inheritor-main">
      <div class="section-header">
        <h2>全部非遗传承人</h2>
        <p class="section-subtitle">共 {{ masterList.length }} 位传承人入驻</p>
      </div>

      <!-- 加载中 -->
      <div v-if="loading" class="loading-state">加载中...</div>

      <!-- 传承人卡片网格 -->
      <div v-else class="master-card-grid">
        <div v-for="master in masterList" :key="master.employeeId" class="master-card">
          <div class="master-card-top">
            <div class="master-avatar-big">
              <span class="master-avatar-initial">{{ (master.name || '传')[0] }}</span>
            </div>
            <div v-if="master.region || master.address" class="master-region-tag">
              📍 {{ master.region || master.address }}
            </div>
          </div>
          <div class="master-card-body">
            <h3>{{ master.name }}</h3>
            <p class="master-tech">{{ master.honorTitle || '非遗传承人' }}</p>
            <!-- 擅长技艺标签 -->
            <div v-if="splitCrafts(master.craftTypes).length > 0" class="master-craft-tags">
              <span v-for="craft in splitCrafts(master.craftTypes).slice(0, 3)" :key="craft" class="master-craft-tag">{{ craft }}</span>
              <span v-if="splitCrafts(master.craftTypes).length > 3" class="master-craft-more">+{{ splitCrafts(master.craftTypes).length - 3 }}</span>
            </div>
            <p class="master-intro">{{ master.selfIntro?.slice(0, 80) || '暂无简介' }}</p>
            <a href="#" class="master-book-btn">预约工坊</a>
          </div>
        </div>
        <div v-if="masterList.length === 0" class="empty-state">暂无入驻传承人</div>
      </div>
    </main>

    <!-- ==================== 页脚 ==================== -->
    <footer class="page-footer">
      <p>校园非遗与文创传承人平台 © 2026 码上非遗</p>
    </footer>
  </div>
</template>

<style scoped>
.inheritor-page {
  min-height: 100vh;
  background: #fcf4e8;
  display: flex;
  flex-direction: column;
}

/* ==================== 顶部导航（与首页/文创周边一致） ==================== */
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
  background: rgba(255, 255, 255, 0.4);
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
  gap: 10px;
}
.user-role-tag {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: bold;
}
.user-tag {
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
}
.inheritor-tag {
  background: #ffd700;
  color: #c41e3a;
}
.user-name {
  font-size: 14px;
  color: #fff;
  margin-right: 4px;
}
.header-btn {
  padding: 6px 12px;
  font-size: 12px;
  color: #fff;
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 4px;
  cursor: pointer;
  transition: 0.2s;
}
.header-btn:hover {
  background: rgba(255, 255, 255, 0.35);
}
.logout-btn {
  padding: 6px 12px;
  font-size: 12px;
  color: #c41e3a;
  background: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: 0.2s;
}
.logout-btn:hover {
  background: #f5f5f5;
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
}
.login-top-btn:hover {
  background: #f0f0f0;
}

/* ---- 主体 ---- */
.inheritor-main {
  flex: 1;
  max-width: 1200px;
  width: 100%;
  margin: 0 auto;
  padding: 40px 20px;
}
.section-header {
  text-align: center;
  margin-bottom: 36px;
}
.section-header h2 {
  font-size: 28px;
  color: #c41e3a;
  margin-bottom: 8px;
}
.section-subtitle {
  font-size: 15px;
  color: #999;
}

/* ---- 加载 ---- */
.loading-state {
  text-align: center;
  padding: 80px 0;
  font-size: 16px;
  color: #999;
}

/* ---- 卡片网格 ---- */
.master-card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 28px;
}
.master-card {
  background-color: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.07);
  transition: 0.3s;
}
.master-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 10px 26px rgba(196, 30, 58, 0.14);
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
  cursor: pointer;
}
.master-book-btn:hover {
  background: #a8162b;
}
.empty-state {
  grid-column: 1 / -1;
  text-align: center;
  padding: 80px 0;
  font-size: 16px;
  color: #999;
}

/* ---- 页脚 ---- */
.page-footer {
  background-color: #c41e3a;
  color: #fff;
  text-align: center;
  padding: 24px 5%;
  margin-top: auto;
}

/* ---- 响应式 ---- */
@media (max-width: 768px) {
  .page-header {
    padding: 0 16px;
    flex-wrap: wrap;
  }
  .nav-links {
    gap: 12px;
  }
  .nav-item {
    font-size: 12px;
  }
  .header-right {
    flex-wrap: wrap;
  }
  .inheritor-main {
    padding: 24px 16px;
  }
  .master-card-grid {
    grid-template-columns: 1fr;
  }
}
</style>
