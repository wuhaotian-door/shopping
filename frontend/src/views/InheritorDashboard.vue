<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useRouter, useRoute } from 'vue-router'
import OrderManager from './components/OrderManager.vue'
import SlotManager from './components/SlotManager.vue'
import KnowledgeManager from './components/KnowledgeManager.vue'
import ProjectManager from './components/ProjectManager.vue'
import ProductManager from './components/ProductManager.vue'
import InheritorProfile from './components/InheritorProfile.vue'
import ChatPanel from './components/ChatPanel.vue'
import StatsPanel from './components/StatsPanel.vue'
import { listInheritorProductOrders, shipProductOrder, cancelProductOrder } from '@/api/product'
import type { ProductOrderVO } from '@/types/api'

const authStore = useAuthStore()
const router = useRouter()
const route = useRoute()

onMounted(() => {
  document.title = '非遗传承人工坊 — 码上非遗'
  if (activeTab.value === 'productOrders') loadProductOrders()
})

const userName = computed(() => authStore.userInfo?.name || '')

// 多账户切换
const showAccountMenu = ref(false)
const switcherRef = ref<HTMLElement | null>(null)
function toggleAccountMenu() {
  showAccountMenu.value = !showAccountMenu.value
}
const accounts = computed(() => authStore.accounts)
const activeIndex = computed(() => authStore.activeIndex)
const avatarUrl = computed(() => authStore.avatarUrl)
const avatarFailed = ref(false)
function onAvatarError() { avatarFailed.value = true }
watch(avatarUrl, () => { avatarFailed.value = false })
function roleLabel(r: string) {
  return r === 'INHERITOR' ? '传承人' : r === 'ADMIN' ? '管理员' : '用户'
}
function doSwitchAccount(index: number) {
  authStore.switchAccount(index)
  showAccountMenu.value = false
}
function addAccount() {
  showAccountMenu.value = false
  router.push('/login?add=1')
}

// 当前激活的 tab — 支持通过 URL query ?tab=xxx 指定
type TabKey = 'orders' | 'slots' | 'knowledge' | 'projects' | 'products' | 'productOrders' | 'stats' | 'consultation' | 'profile'
const activeTab = ref<TabKey>((route.query.tab as TabKey) || 'orders')

// 下拉菜单分组
interface MenuGroup {
  label: string
  children: { key: TabKey; label: string }[]
}
const menuGroups: (MenuGroup | { key: TabKey; label: string })[] = [
  { label: '工坊运营', children: [
    { key: 'orders', label: '预约订单' },
    { key: 'slots', label: '时段管理' },
  ]},
  { label: '内容管理', children: [
    { key: 'knowledge', label: '非遗资讯' },
    { key: 'projects', label: '非遗项目' },
    { key: 'products', label: '商品设置' },
  ]},
  { label: '交易管理', children: [
    { key: 'productOrders', label: '订单管理' },
  ]},
  { key: 'stats', label: '数据统计' },
  { key: 'consultation', label: '咨询消息' },
  { key: 'profile', label: '个人中心' },
]

const openMenu = ref<string>('')
const navRef = ref<HTMLElement | null>(null)

function toggleMenu(label: string) {
  openMenu.value = openMenu.value === label ? '' : label
}
function closeMenu() {
  openMenu.value = ''
}
function onDocClick(e: MouseEvent) {
  if (navRef.value && !navRef.value.contains(e.target as Node)) {
    closeMenu()
  }
  if (switcherRef.value && !switcherRef.value.contains(e.target as Node)) {
    showAccountMenu.value = false
  }
}
onMounted(() => document.addEventListener('click', onDocClick))
onUnmounted(() => document.removeEventListener('click', onDocClick))

function selectTab(key: TabKey) {
  activeTab.value = key
  openMenu.value = ''
  if (key === 'productOrders') loadProductOrders()
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

function viewOrderDetail(orderId: number) {
  router.push(`/order/${orderId}`)
}

async function handleCancelPO(order: ProductOrderVO) {
  if (!confirm('确定要取消该订单吗？')) return
  try {
    await cancelProductOrder(order.id)
    await loadProductOrders()
  } catch {
    // ignore
  }
}

// ==================== 发货弹窗 ====================
const showShipModal = ref(false)
const shipOrderId = ref<number | null>(null)
const shipCourier = ref('')
const shipTracking = ref('')
const shipLoading = ref(false)

function openShipModal(order: ProductOrderVO) {
  shipOrderId.value = order.id
  shipCourier.value = ''
  shipTracking.value = ''
  showShipModal.value = true
}

async function submitShip() {
  if (!shipOrderId.value) return
  if (!shipCourier.value.trim()) {
    alert('请输入快递公司')
    return
  }
  if (!shipTracking.value.trim()) {
    alert('请输入快递单号')
    return
  }
  shipLoading.value = true
  try {
    await shipProductOrder(shipOrderId.value, shipCourier.value.trim(), shipTracking.value.trim())
    showShipModal.value = false
    await loadProductOrders()
  } catch {
    alert('发货失败')
  } finally {
    shipLoading.value = false
  }
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
      <div class="logo" @click="router.push('/')">
        <img src="/logo.png" alt="码上非遗" class="logo-img" />
        <span class="logo-text">码上非遗</span>
      </div>
      <span class="workspace-tag">传承人工坊</span>
    </div>

    <nav ref="navRef" class="nav-tabs">
      <template v-for="item in menuGroups" :key="'label' in item ? item.label : (item as {key:TabKey;label:string}).label">
        <!-- 下拉菜单项 -->
        <div v-if="'children' in item" class="nav-dropdown" :class="{ open: openMenu === item.label }">
          <button class="nav-tab nav-has-sub" :class="{ active: (item as MenuGroup).children.some(c => activeTab === c.key) }" @click="toggleMenu(item.label)">
            {{ item.label }}
            <span class="arrow">▾</span>
          </button>
          <div class="dropdown-menu">
            <a
              v-for="child in (item as MenuGroup).children"
              :key="child.key"
              class="dropdown-item"
              :class="{ active: activeTab === child.key }"
              @click="selectTab(child.key)"
            >{{ child.label }}</a>
          </div>
        </div>
        <!-- 直接跳转项 -->
        <a
          v-else
          class="nav-tab"
          :class="{ active: activeTab === (item as {key:TabKey;label:string}).key }"
          @click="selectTab((item as {key:TabKey;label:string}).key)"
        >{{ (item as {key:TabKey;label:string}).label }}</a>
      </template>
    </nav>

    <div class="header-right">
      <span class="inheritor-badge">非遗传承人</span>
      <!-- 多账户切换 -->
      <div class="account-switcher" ref="switcherRef" v-if="accounts.length > 0">
        <button class="account-btn" @click.stop="toggleAccountMenu" :class="{ open: showAccountMenu }">
          <img v-if="avatarUrl && !avatarFailed" :src="avatarUrl" class="header-avatar" @error="onAvatarError" />
          <span v-else class="header-avatar-placeholder">{{ userName.charAt(0) }}</span>
          <span class="user-name">{{ userName }}</span>
          <span class="account-arrow" :class="{ flipped: showAccountMenu }">&#9662;</span>
        </button>
        <Transition name="menu-fade">
        <div class="account-menu" v-if="showAccountMenu">
          <div
            v-for="(acc, idx) in accounts"
            :key="idx"
            class="account-item"
            :class="{ active: idx === activeIndex }"
            @click.stop="doSwitchAccount(idx)"
          >
            <span class="account-avatar">{{ acc.name.charAt(0) }}</span>
            <span class="account-info">
              <span class="account-name">{{ acc.name }}</span>
              <span class="account-detail">{{ acc.username }} · {{ roleLabel(acc.role) }}</span>
            </span>
            <span v-if="idx === activeIndex" class="account-check-icon">&#10003;</span>
          </div>
          <div class="account-divider"></div>
          <div class="account-item add-account" @click.stop="addAccount">
            <span class="account-add-icon">+</span>
            <span class="account-name">添加其他账号</span>
          </div>
        </div>
        </Transition>
      </div>      <button class="home-btn" @click="router.push('/')">返回首页</button>
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

      <!-- 非遗资讯 — 文章管理 -->
      <KnowledgeManager v-if="activeTab === 'knowledge'" />

      <!-- 非遗项目 — 公益/非公益分栏 -->
      <template v-if="activeTab === 'projects'">
        <div class="split-panels">
          <div class="split-panel split-panel-left">
            <ProjectManager :projectType="0" />
          </div>
          <div class="split-divider"></div>
          <div class="split-panel split-panel-right">
            <ProjectManager :projectType="1" />
          </div>
        </div>
      </template>

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
                  <button class="btn-view" @click="viewOrderDetail(order.id)">查看</button>
                  <template v-if="order.status === 0">
                    <button class="btn-confirm" style="background: #17a2b8" @click="openShipModal(order)">发货</button>
                    <button class="btn-reject" @click="handleCancelPO(order)">取消</button>
                  </template>
                  <span v-else class="no-action">—</span>
                </td>
              </tr>
            </tbody>
          </table>
          <div v-if="productOrderLoading" class="empty-state">加载中…</div>
          <div v-else-if="productOrderList.length === 0" class="empty-state">暂无商品订单</div>
        </div>

        <!-- 发货弹窗 -->
        <div v-if="showShipModal" class="modal-overlay" @click.self="showShipModal = false">
          <div class="modal-box">
            <h3>填写快递信息</h3>
            <div class="field">
              <label>快递公司</label>
              <input v-model="shipCourier" placeholder="如：顺丰速运" class="modal-input" />
            </div>
            <div class="field">
              <label>快递单号</label>
              <input v-model="shipTracking" placeholder="请输入快递单号" class="modal-input" />
            </div>
            <div class="modal-btns">
              <button class="btn-cancel" @click="showShipModal = false">取消</button>
              <button class="btn-submit" :disabled="shipLoading" @click="submitShip">
                {{ shipLoading ? '发货中...' : '确认发货' }}
              </button>
            </div>
          </div>
        </div>
      </section>

      <!-- 数据统计 — ECharts 可视化 -->
      <StatsPanel v-if="activeTab === 'stats'" />

      <!-- 咨询消息 — 实时会话聊天 -->
      <ChatPanel v-if="activeTab === 'consultation'" />

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
  background: linear-gradient(135deg, #8b1a2b 0%, #a01e35 40%, #c41e3a 100%);
  color: #fff;
  padding: 0 6%;
  height: 72px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: sticky;
  top: 0;
  z-index: 100;
  font-family: 'Noto Serif SC', 'STKaiti', 'KaiTi', '楷体', 'STSong', serif;
  letter-spacing: 1px;
  box-shadow: 0 2px 16px rgba(139, 26, 43, 0.35);
}
.header-left {
  display: flex;
  align-items: center;
  gap: 14px;
}
.logo {
  display: flex; align-items: center; gap: 10px;
  cursor: pointer; white-space: nowrap;
}
.logo-img { height: 42px; width: auto; }
.logo-text { font-size: 22px; font-weight: 700; letter-spacing: 4px; color: #fff; }
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
  display: flex; align-items: center; gap: 4px;
}

/* 下拉菜单容器 */
.nav-dropdown { position: relative; }
.nav-has-sub {
  display: flex; align-items: center; gap: 4px;
  background: none; border: none; font-family: inherit;
}
.arrow { font-size: 10px; transition: transform 0.2s; }
.nav-dropdown.open .arrow { transform: rotate(180deg); }

/* 下拉菜单面板（朱砂红主题） */
.dropdown-menu {
  position: absolute; top: 100%; left: 0;
  margin-top: 4px;
  min-width: 140px;
  background: linear-gradient(180deg, #fffaf8 0%, #fff5f2 100%);
  border: 1px solid rgba(196, 30, 58, 0.15);
  border-radius: 6px;
  box-shadow: 0 6px 24px rgba(139, 26, 43, 0.18);
  padding: 4px 0;
  opacity: 0; visibility: hidden;
  transform: translateY(-4px);
  transition: all 0.2s ease;
  z-index: 200;
}
.nav-dropdown.open .dropdown-menu {
  opacity: 1; visibility: visible;
  transform: translateY(0);
}
.dropdown-item {
  display: block;
  padding: 10px 20px;
  font-size: 14px;
  color: #5c3030;
  cursor: pointer; white-space: nowrap;
  transition: all 0.15s;
  border-left: 3px solid transparent;
}
.dropdown-item:hover {
  background: rgba(196, 30, 58, 0.06);
  color: #c41e3a;
  border-left-color: #c41e3a;
}
.dropdown-item.active {
  color: #c41e3a;
  font-weight: 700;
  background: rgba(196, 30, 58, 0.08);
  border-left-color: #c41e3a;
}

/* 基础 tab 样式 */
.nav-tab {
  color: rgba(255,255,255,0.85);
  text-decoration: none; font-size: 14px;
  cursor: pointer; padding: 8px 16px; border-radius: 6px;
  transition: all 0.2s; white-space: nowrap;
  letter-spacing: 1px;
}
.nav-tab:hover { color: #fff; background: rgba(255,255,255,0.12); }
.nav-tab.active { color: #c41e3a; background: #fff; font-weight: 700; }

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
  position: relative;
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

.header-avatar {
  width: 28px !important;
  height: 28px !important;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
  border: 2px solid rgba(255,255,255,0.4);
}
.header-avatar-placeholder {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: rgba(255,255,255,0.2);
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
  flex-shrink: 0;
}
/* 多账户切换器 */
.account-switcher { position: relative; z-index: 150; }
.account-btn {
  display: flex; align-items: center; gap: 6px;
  padding: 5px 12px; font-size: 14px;
  color: #fff; background: rgba(255,255,255,0.1);
  border: 1px solid rgba(255,255,255,0.3); border-radius: 6px;
  cursor: pointer; font-family: inherit; white-space: nowrap;
  transition: all 0.2s;
}
.account-btn:hover, .account-btn.open {
  background: rgba(255,255,255,0.22);
  border-color: rgba(255,255,255,0.5);
}
.account-arrow { font-size: 10px; transition: transform 0.25s; }
.account-arrow.flipped { transform: rotate(180deg); }
.account-menu {
  position: absolute; top: calc(100% + 8px); right: 0;
  min-width: 240px;
  background: linear-gradient(160deg, #3d0a12 0%, #5c1522 40%, #7a1e2d 100%);
  border: 1px solid rgba(255,255,255,0.12);
  border-radius: 10px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.35), 0 0 0 1px rgba(255,215,0,0.08);
  overflow: hidden;
  padding: 6px 0;
}
.account-item {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 14px; cursor: pointer; transition: background 0.15s;
  color: rgba(255,255,255,0.9); font-size: 14px;
  position: relative;
}
.account-item:hover { background: rgba(255,255,255,0.1); }
.account-item.active { background: rgba(255,215,0,0.12); color: #ffd700; }
.account-avatar {
  width: 32px; height: 32px; border-radius: 50%;
  background: rgba(255,255,255,0.15); color: #fff;
  display: flex; align-items: center; justify-content: center;
  font-size: 14px; font-weight: 700; flex-shrink: 0;
}
.account-item.active .account-avatar { background: #ffd700; color: #5c1522; }
.account-info { display: flex; flex-direction: column; }
.account-name { font-weight: 600; }
.account-detail { font-size: 11px; color: rgba(255,255,255,0.5); }
.account-item.active .account-detail { color: rgba(255,215,0,0.7); }
.account-check-icon { margin-left: auto; color: #ffd700; font-size: 16px; font-weight: 700; }
.account-divider { height: 1px; background: rgba(255,255,255,0.1); margin: 4px 0; }
.add-account { color: rgba(255,255,255,0.7); }
.add-account:hover { color: #fff; background: rgba(255,255,255,0.1); }
.account-add-icon {
  width: 32px; height: 32px; border-radius: 50%;
  border: 1.5px dashed rgba(255,255,255,0.4); color: rgba(255,255,255,0.6);
  display: flex; align-items: center; justify-content: center;
  font-size: 18px; flex-shrink: 0;
}
.add-account:hover .account-add-icon { border-color: #ffd700; color: #ffd700; }
.menu-fade-enter-active { transition: all 0.2s ease-out; }
.menu-fade-leave-active { transition: all 0.15s ease-in; }
.menu-fade-enter-from { opacity: 0; transform: translateY(-6px); }
.menu-fade-leave-to { opacity: 0; transform: translateY(-4px); }
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

/* "查看" 按钮 */
.btn-view {
  padding: 5px 14px; font-size: 12px; color: #c41e3a; background: #fff;
  border: 1px solid #c41e3a; border-radius: 4px; cursor: pointer; transition: 0.2s; margin-right: 6px;
}
.btn-view:hover { background: #c41e3a; color: #fff; }

/* ==================== 发货弹窗 ==================== */
.modal-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.45); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.modal-box { background: #fff; border-radius: 12px; padding: 32px; max-width: 420px; width: 90%; }
.modal-box h3 { font-size: 18px; color: #333; margin-bottom: 20px; text-align: center; }
.field { margin-bottom: 16px; }
.field label { display: block; font-size: 13px; color: #666; margin-bottom: 6px; }
.modal-input { width: 100%; padding: 10px 14px; border: 1px solid #ddd; border-radius: 6px; font-size: 14px; outline: none; box-sizing: border-box; }
.modal-input:focus { border-color: #c41e3a; }
.modal-btns { display: flex; gap: 12px; justify-content: flex-end; margin-top: 20px; }
.btn-cancel { padding: 10px 24px; border: 1px solid #ddd; border-radius: 6px; background: #fff; cursor: pointer; font-size: 14px; }
.btn-submit { padding: 10px 24px; background: #c41e3a; color: #fff; border: none; border-radius: 6px; cursor: pointer; font-size: 14px; font-weight: 600; }
.btn-submit:disabled { opacity: 0.5; cursor: not-allowed; }

/* ==================== 页脚 ==================== */
.page-footer {
  background-color: #c41e3a;
  color: #fff;
  text-align: center;
  padding: 20px 5%;
  font-size: 14px;
}

/* ==================== 分栏布局（非遗项目） ==================== */
.split-panels {
  display: flex;
  gap: 0;
  min-height: 600px;
}

.split-panel {
  flex: 1;
  min-width: 0;
  overflow-y: auto;
}

.split-divider {
  width: 2px;
  background: #e0e0e0;
  margin: 0 16px;
  flex-shrink: 0;
}

/* ==================== 响应式 ==================== */
@media (max-width: 992px) {
  .nav-tabs { gap: 2px; }
  .nav-tab { padding: 8px 10px; font-size: 13px; }
  .split-panels {
    flex-direction: column;
  }
  .split-divider {
    width: 100%;
    height: 2px;
    margin: 16px 0;
  }
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
