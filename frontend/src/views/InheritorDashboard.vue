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
import { listInheritorProductOrders, shipProductOrder, completeProductOrder, cancelProductOrder } from '@/api/product'
import type { ProductOrderVO } from '@/types/api'

const authStore = useAuthStore()
const router = useRouter()

onMounted(() => {
  document.title = '非遗传承人工坊 — 码上非遗'
})

const userName = computed(() => authStore.userInfo?.name)

// 当前激活的 tab
type TabKey = 'orders' | 'slots' | 'knowledge' | 'projects' | 'products' | 'productOrders' | 'consultation' | 'profile'
const activeTab = ref<TabKey>('orders')

const tabs: { key: TabKey; label: string }[] = [
  { key: 'orders', label: '预约订单' },
  { key: 'slots', label: '时段管理' },
  { key: 'knowledge', label: '知识库' },
  { key: 'projects', label: '非遗项目' },
  { key: 'products', label: '商品设置' },
  { key: 'productOrders', label: '订单管理' },
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

// ==================== 商品订单（内联） ====================
const productOrderList = ref<ProductOrderVO[]>([])
const productOrderLoading = ref(false)

const poStatusMap: Record<number, { text: string; cls: string }> = {
  0: { text: '待发货', cls: 'status-pending' },
  1: { text: '已发货', cls: 'status-shipped' },
  2: { text: '已完成', cls: 'status-completed' },
  3: { text: '已取消', cls: 'status-cancelled' },
}

async function loadProductOrders() {
  productOrderLoading.value = true
  try {
    const res = await listInheritorProductOrders()
    productOrderList.value = res.data
  } catch {
    // ignore
  } finally {
    productOrderLoading.value = false
  }
}

async function handlePOAction(order: ProductOrderVO, action: 'ship' | 'complete' | 'cancel') {
  try {
    if (action === 'ship') await shipProductOrder(order.id)
    else if (action === 'complete') await completeProductOrder(order.id)
    else if (action === 'cancel') await cancelProductOrder(order.id)
    await loadProductOrders()
  } catch {
    // ignore
  }
}

// 切换到商品订单 tab 时加载数据
function onTabSwitch(key: TabKey) {
  activeTab.value = key
  if (key === 'productOrders') loadProductOrders()
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
        @click="onTabSwitch(tab.key)"
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

      <!-- 预约订单 — 从数据库读取工坊预约 -->
      <OrderManager v-if="activeTab === 'orders'" />

      <!-- 时段管理 — 设置工坊可参观时段 -->
      <SlotManager v-if="activeTab === 'slots'" />

      <!-- 知识库 — 文章管理 -->
      <KnowledgeManager v-if="activeTab === 'knowledge'" />

      <!-- 非遗项目 — 项目管理 -->
      <ProjectManager v-if="activeTab === 'projects'" />

      <!-- 商品设置 — 从数据库读取传承人产品目录 -->
      <ProductManager v-if="activeTab === 'products'" />

      <!-- 订单管理 — 从数据库读取用户产品预约订单 -->
      <section v-if="activeTab === 'productOrders'" class="tab-content">
        <div class="section-header">
          <h2>订单管理</h2>
          <div class="order-stats">
            <span class="stat-item">待发货 <strong>{{ productOrderList.filter(o => o.status === 0).length }}</strong></span>
            <span class="stat-item">已发货 <strong>{{ productOrderList.filter(o => o.status === 1).length }}</strong></span>
            <span class="stat-item">已完成 <strong>{{ productOrderList.filter(o => o.status === 2).length }}</strong></span>
          </div>
        </div>

        <div class="table-wrap">
          <table class="order-table">
            <thead>
              <tr>
                <th>用户</th>
                <th>商品</th>
                <th>单价</th>
                <th>数量</th>
                <th>金额</th>
                <th>联系电话</th>
                <th>地址</th>
                <th>状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="order in productOrderList" :key="order.id">
                <td>{{ order.userName }}</td>
                <td>{{ order.productName }}</td>
                <td>¥{{ order.price.toFixed(2) }}</td>
                <td>{{ order.quantity }}</td>
                <td>¥{{ order.totalAmount.toFixed(2) }}</td>
                <td>{{ order.phone }}</td>
                <td class="remark-cell">{{ order.address }}</td>
                <td>
                  <span class="status-tag" :class="poStatusMap[order.status]?.cls">
                    {{ poStatusMap[order.status]?.text }}
                  </span>
                </td>
                <td class="action-cell">
                  <template v-if="order.status === 0">
                    <button class="btn-confirm" style="background: #17a2b8" @click="handlePOAction(order, 'ship')">发货</button>
                    <button class="btn-reject" @click="handlePOAction(order, 'cancel')">取消</button>
                  </template>
                  <template v-else-if="order.status === 1">
                    <button class="btn-confirm" @click="handlePOAction(order, 'complete')">完成</button>
                  </template>
                  <span v-else class="no-action">—</span>
                </td>
              </tr>
            </tbody>
          </table>
          <div v-if="productOrderLoading" class="empty-state">加载中…</div>
          <div v-else-if="productOrderList.length === 0" class="empty-state">暂无商品订单</div>
        </div>
      </section>

      <!-- 咨询消息 -->
      <ConsultationPanel v-if="activeTab === 'consultation'" />

      <!-- 入驻管理 -->
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

/* ==================== 商品订单内联样式 ==================== */
.tab-content { }
.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}
.section-header h2 {
  font-size: 22px;
  color: #c41e3a;
  padding-left: 14px;
  border-left: 4px solid #c41e3a;
}
.order-stats {
  display: flex;
  gap: 20px;
}
.stat-item {
  font-size: 14px;
  color: #666;
}
.stat-item strong {
  color: #c41e3a;
  font-size: 18px;
  margin-left: 4px;
}
.table-wrap {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 10px rgba(0,0,0,0.06);
}
.order-table {
  width: 100%;
  border-collapse: collapse;
}
.order-table th,
.order-table td {
  padding: 14px 16px;
  text-align: left;
  font-size: 14px;
}
.order-table thead {
  background: #fff5f7;
}
.order-table th {
  color: #c41e3a;
  font-weight: 600;
  font-size: 13px;
}
.order-table tbody tr {
  border-bottom: 1px solid #f5f5f5;
  transition: 0.2s;
}
.order-table tbody tr:hover {
  background: #fefafa;
}
.remark-cell {
  max-width: 160px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.action-cell {
  white-space: nowrap;
}
.no-action {
  color: #ccc;
}

.status-tag {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}
.status-pending {
  background: #fff3cd;
  color: #856404;
}
.status-confirmed {
  background: #d4edda;
  color: #155724;
}
.status-completed {
  background: #e2e3e5;
  color: #383d41;
}
.status-cancelled {
  background: #f8d7da;
  color: #721c24;
}
.status-shipped {
  background: #cce5ff;
  color: #004085;
}

.btn-confirm {
  padding: 5px 14px;
  font-size: 12px;
  color: #fff;
  background: #28a745;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  margin-right: 6px;
  transition: 0.2s;
}
.btn-confirm:hover {
  background: #218838;
}
.btn-reject {
  padding: 5px 14px;
  font-size: 12px;
  color: #fff;
  background: #dc3545;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: 0.2s;
}
.btn-reject:hover {
  background: #c82333;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #999;
  font-size: 14px;
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
