<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'
import OrderManager from './components/OrderManager.vue'
import SlotManager from './components/SlotManager.vue'
import KnowledgeManager from './components/KnowledgeManager.vue'
import ProjectManager from './components/ProjectManager.vue'
import ProductManager from './components/ProductManager.vue'
import ConsultationPanel from './components/ConsultationPanel.vue'
import InheritorProfile from './components/InheritorProfile.vue'

const authStore = useAuthStore()
const router = useRouter()

onMounted(() => {
  document.title = '非遗传承人工坊 — 码上非遗'
})

const userName = computed(() => authStore.userInfo?.name)

type TabKey = 'orders' | 'slots' | 'knowledge' | 'projects' | 'products' | 'consultation' | 'profile'
const activeTab = ref<TabKey>('orders')

const tabs: { key: TabKey; label: string }[] = [
  { key: 'orders', label: '预约订单' },
  { key: 'slots', label: '时段管理' },
  { key: 'knowledge', label: '知识库' },
  { key: 'projects', label: '非遗项目' },
  { key: 'products', label: '文创产品' },
  { key: 'consultation', label: '咨询消息' },
  { key: 'profile', label: '入驻管理' },
]

function switchTab(key: TabKey) {
  activeTab.value = key
}

function handleLogout() {
  authStore.logout()
  router.push('/')
}
</script>

<template>
  <!-- ==================== 顶部导航 ==================== -->
  <header class="page-header">
    <div class="header-left">
      <div class="logo" @click="router.push('/')">码上非遗</div>
      <span class="workspace-tag">传承人工坊</span>
    </div>

    <nav class="nav-tabs">
      <a
        v-for="tab in tabs"
        :key="tab.key"
        class="nav-tab"
        :class="{ active: activeTab === tab.key }"
        @click="switchTab(tab.key)"
      >
        {{ tab.label }}
      </a>
    </nav>

    <div class="header-right">
      <span class="inheritor-badge">非遗传承人</span>
      <span class="user-name">{{ userName }}</span>
      <button class="home-btn" @click="router.push('/')">返回首页</button>
      <button class="logout-btn" @click="handleLogout">退出</button>
    </div>
  </header>

  <!-- ==================== 主内容区 ==================== -->
  <main class="dashboard-main">
    <div class="dashboard-container">
      <OrderManager v-if="activeTab === 'orders'" />
      <SlotManager v-if="activeTab === 'slots'" />
      <KnowledgeManager v-if="activeTab === 'knowledge'" />
      <ProjectManager v-if="activeTab === 'projects'" />
      <ProductManager v-if="activeTab === 'products'" />
      <ConsultationPanel v-if="activeTab === 'consultation'" />
      <InheritorProfile v-if="activeTab === 'profile'" />
    </div>
  </main>

  <!-- ==================== 页脚 ==================== -->
  <footer class="page-footer">
    <p>校园非遗与文创传承人平台 © 2026 码上非遗 — 传承人工坊</p>
  </footer>
</template>

<style scoped>
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
.header-left {
  display: flex;
  align-items: center;
  gap: 14px;
}
.logo {
  font-size: 20px;
  font-weight: bold;
  cursor: pointer;
  white-space: nowrap;
}
.workspace-tag {
  font-size: 12px;
  background: #ffd700;
  color: #8b4513;
  padding: 2px 10px;
  border-radius: 10px;
  font-weight: bold;
  white-space: nowrap;
}

.nav-tabs {
  display: flex;
  align-items: center;
  gap: 6px;
  overflow-x: auto;
}
.nav-tab {
  color: rgba(255,255,255,0.8);
  text-decoration: none;
  font-size: 14px;
  cursor: pointer;
  padding: 8px 16px;
  border-radius: 6px;
  transition: all 0.2s;
  white-space: nowrap;
}
.nav-tab:hover {
  color: #fff;
  background: rgba(255,255,255,0.12);
}
.nav-tab.active {
  color: #c41e3a;
  background: #fff;
  font-weight: bold;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}
.inheritor-badge {
  font-size: 11px;
  background: #ffd700;
  color: #8b4513;
  padding: 2px 10px;
  border-radius: 10px;
  font-weight: bold;
  white-space: nowrap;
}
.user-name {
  font-size: 14px;
  white-space: nowrap;
}
.home-btn {
  padding: 6px 12px;
  font-size: 12px;
  color: #fff;
  background: rgba(255,255,255,0.15);
  border: 1px solid rgba(255,255,255,0.4);
  border-radius: 4px;
  cursor: pointer;
  transition: 0.2s;
  white-space: nowrap;
}
.home-btn:hover {
  background: rgba(255,255,255,0.3);
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
  white-space: nowrap;
}
.logout-btn:hover {
  background: rgba(255,255,255,0.3);
}

/* ==================== 主内容区 ==================== */
.dashboard-main {
  min-height: calc(100vh - 64px - 60px);
  background: #fcf4e8;
  padding: 30px 5%;
}
.dashboard-container {
  max-width: 1200px;
  margin: 0 auto;
}

/* ==================== 页脚 ==================== */
.page-footer {
  background-color: #c41e3a;
  color: #fff;
  text-align: center;
  padding: 20px 5%;
  font-size: 14px;
}

/* ==================== 响应式 ==================== */
@media (max-width: 992px) {
  .nav-tabs { gap: 2px; }
  .nav-tab { padding: 8px 10px; font-size: 13px; }
}
@media (max-width: 768px) {
  .page-header {
    flex-wrap: wrap;
    height: auto;
    padding: 10px 16px;
    gap: 10px;
  }
  .nav-tabs {
    order: 3;
    width: 100%;
    justify-content: center;
  }
  .inheritor-badge { display: none; }
}
</style>
