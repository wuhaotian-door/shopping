<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'
import {
  adminCreate, getPendingList, approveInheritor, rejectInheritor,
  listAllUsers, searchUsers, updateUserStatus, getDashboardStats,
  changePassword,
} from '@/api/auth'
import { listAllOrders, cancelOrder as cancelOrderApi, adminUpdateOrderStatus, adminUpdateOrderFields } from '@/api/order'
import { listAllArticles, adminDeleteArticle } from '@/api/knowledge'
import { listProducts as listAllProducts, adminDeleteProduct, adminToggleProduct, listAllProductOrders, adminUpdateProductOrderStatus, adminUpdateProductOrderAddress } from '@/api/product'
import { listAllProjects, adminDeleteProject } from '@/api/project'
import ChatPanel from './components/ChatPanel.vue'
import type { PendingUser, UserInfo, DashboardStats, OrderVO, KnowledgeArticleVO, ProductVO, ProductOrderVO, HeritageProjectVO } from '@/types/api'

const authStore = useAuthStore()
const router = useRouter()

onMounted(() => {
  document.title = '管理后台——码上非遗'
  loadDashboard()
})

// ==================== 侧边栏菜单 ====================
type MenuKey = 'dashboard' | 'users' | 'pending' | 'news' | 'products' | 'projects' | 'orders' | 'messages' | 'settings'
const activeMenu = ref<MenuKey>('dashboard')

const menuItems: { key: MenuKey; label: string; icon: string }[] = [
  { key: 'dashboard', label: '仪表盘', icon: '📊' },
  { key: 'users', label: '用户管理', icon: '👥' },
  { key: 'pending', label: '审核管理', icon: '✅' },
  { key: 'news', label: '资讯管理', icon: '📰' },
  { key: 'products', label: '商品管理', icon: '🛍️' },
  { key: 'projects', label: '项目管理', icon: '📋' },
  { key: 'orders', label: '订单管理', icon: '🛒' },
  { key: 'messages', label: '消息管理', icon: '💬' },
  { key: 'settings', label: '系统设置', icon: '⚙️' },
]

function switchMenu(key: MenuKey) {
  activeMenu.value = key
  if (key === 'dashboard') loadDashboard()
  else if (key === 'users') loadUsers()
  else if (key === 'pending') loadPendingList()
  else if (key === 'news') loadArticles()
  else if (key === 'products') loadProducts()
  else if (key === 'projects') loadProjects()
  else if (key === 'orders') loadOrders()
}

// ==================== 1. 仪表盘 ====================
const stats = ref<DashboardStats | null>(null)
const statsLoading = ref(false)
const statsError = ref('')

async function loadDashboard() {
  statsLoading.value = true
  statsError.value = ''
  try {
    const res = await getDashboardStats()
    stats.value = res.data
  } catch (e: unknown) {
    statsError.value = e instanceof Error ? e.message : '加载统计数据失败'
    console.error('Dashboard stats error:', e)
  } finally { statsLoading.value = false }
}

// ==================== 2. 用户管理 ====================
const userList = ref<UserInfo[]>([])
const userLoading = ref(false)
const userSearchKeyword = ref('')
const userActionLoading = ref<Record<number, boolean>>({})

async function loadUsers() {
  userLoading.value = true
  try {
    const res = await listAllUsers()
    userList.value = res.data
  } catch { userList.value = [] }
  finally { userLoading.value = false }
}

async function handleUserSearch() {
  if (!userSearchKeyword.value.trim()) {
    loadUsers()
    return
  }
  userLoading.value = true
  try {
    const res = await searchUsers(userSearchKeyword.value.trim())
    userList.value = res.data
  } catch { userList.value = [] }
  finally { userLoading.value = false }
}

async function handleToggleStatus(user: UserInfo) {
  const newStatus = user.status === 1 ? 0 : 1
  userActionLoading.value = { ...userActionLoading.value, [user.id]: true }
  try {
    await updateUserStatus(user.id, newStatus)
    user.status = newStatus
  } catch { alert('操作失败') }
  finally {
    const next = { ...userActionLoading.value }
    delete next[user.id]
    userActionLoading.value = next
  }
}

const statusLabel = (s: number) => s === 1 ? '启用' : s === 2 ? '待审核' : '禁用'
const statusClass = (s: number) => s === 1 ? 's-enabled' : s === 2 ? 's-pending' : 's-disabled'
const roleLabel = (r: string) => r === 'ADMIN' ? '管理员' : r === 'INHERITOR' ? '传承人' : '用户'

// ==================== 3. 审核管理 ====================
const pendingList = ref<PendingUser[]>([])
const pendingLoading = ref(false)
const pendingError = ref('')
const pendingActionLoading = ref<Record<number, string>>({})

async function loadPendingList() {
  pendingLoading.value = true
  pendingError.value = ''
  try {
    const res = await getPendingList()
    pendingList.value = res.data
  } catch (e: unknown) {
    pendingError.value = e instanceof Error ? e.message : '加载失败'
  } finally { pendingLoading.value = false }
}

async function handleApprove(id: number) {
  pendingActionLoading.value = { [id]: 'approve' }
  try {
    await approveInheritor(id)
    pendingList.value = pendingList.value.filter(u => u.id !== id)
  } catch (e: unknown) { alert(e instanceof Error ? e.message : '操作失败') }
  finally { pendingActionLoading.value = {} }
}

async function handleReject(id: number) {
  pendingActionLoading.value = { [id]: 'reject' }
  try {
    await rejectInheritor(id)
    pendingList.value = pendingList.value.filter(u => u.id !== id)
  } catch (e: unknown) { alert(e instanceof Error ? e.message : '操作失败') }
  finally { pendingActionLoading.value = {} }
}

// ==================== 4. 资讯管理 ====================
const articles = ref<KnowledgeArticleVO[]>([])
const articleLoading = ref(false)

async function loadArticles() {
  articleLoading.value = true
  try { const res = await listAllArticles(); articles.value = res.data } catch { articles.value = [] }
  finally { articleLoading.value = false }
}

async function handleDeleteArticle(id: number) {
  if (!confirm('确定删除该文章吗？')) return
  try { await adminDeleteArticle(id); articles.value = articles.value.filter(a => a.id !== id) }
  catch { alert('删除失败') }
}

const articleStatusLabel = (s: number) => s === 1 ? '已发布' : '草稿'
const articleStatusClass = (s: number) => s === 1 ? 's-enabled' : 's-disabled'

// ==================== 5. 商品管理 ====================
const products = ref<ProductVO[]>([])
const productLoading = ref(false)

async function loadProducts() {
  productLoading.value = true
  try { const res = await listAllProducts(); products.value = res.data } catch { products.value = [] }
  finally { productLoading.value = false }
}

async function handleDeleteProduct(id: number) {
  if (!confirm('确定删除该商品吗？')) return
  try { await adminDeleteProduct(id); products.value = products.value.filter(p => p.id !== id) }
  catch (e: unknown) { alert(e instanceof Error ? e.message : '删除失败') }
}

async function handleToggleProduct(product: ProductVO) {
  const newStatus = product.status === 1 ? 0 : 1
  try {
    await adminToggleProduct(product.id, newStatus)
    product.status = newStatus
  } catch { alert('操作失败') }
}

// ---------- 搜索 ----------
const newsKeyword = ref('')
const productKeyword = ref('')
const projectKeyword = ref('')
const orderKeyword = ref('')
const prodOrderKeyword = ref('')

const filteredArticles = computed(() =>
  !newsKeyword.value ? articles.value : articles.value.filter(a =>
    a.title.includes(newsKeyword.value) || (a.authorName || '').includes(newsKeyword.value) || (a.category || '').includes(newsKeyword.value)))
const filteredProducts = computed(() =>
  !productKeyword.value ? products.value : products.value.filter(p =>
    p.name.includes(productKeyword.value) || (p.category || '').includes(productKeyword.value) || (p.inheritorName || '').includes(productKeyword.value)))
const filteredProjects = computed(() =>
  !projectKeyword.value ? projects.value : projects.value.filter(p =>
    p.title.includes(projectKeyword.value) || (p.category || '').includes(projectKeyword.value) || (p.inheritorName || '').includes(projectKeyword.value) || projectTypeLabel(p.projectType ?? 0).includes(projectKeyword.value)))
const filteredOrders = computed(() =>
  !orderKeyword.value ? orderList.value : orderList.value.filter(o =>
    o.userName.includes(orderKeyword.value) || o.phone.includes(orderKeyword.value) || String(o.id).includes(orderKeyword.value)))
const filteredProdOrders = computed(() =>
  !prodOrderKeyword.value ? productOrderList.value : productOrderList.value.filter(o =>
    o.userName.includes(prodOrderKeyword.value) || o.productName.includes(prodOrderKeyword.value) || String(o.id).includes(prodOrderKeyword.value)))

// ==================== 6. 项目管理 ====================
const projects = ref<HeritageProjectVO[]>([])
const projectLoading = ref(false)

async function loadProjects() {
  projectLoading.value = true
  try { const res = await listAllProjects(); projects.value = res.data } catch { projects.value = [] }
  finally { projectLoading.value = false }
}

async function handleDeleteProject(id: number) {
  if (!confirm('确定删除该项目吗？')) return
  try { await adminDeleteProject(id); projects.value = projects.value.filter(p => p.id !== id) }
  catch { alert('删除失败') }
}

const projectStatusLabel = (s: number) => s === 1 ? '上架' : '下架'
const projectTypeLabel = (t: number) => t === 1 ? '非公益（盈利）' : '公益（非盈利）'

// ==================== 7. 订单管理 ====================
const orderList = ref<OrderVO[]>([])
const productOrderList = ref<ProductOrderVO[]>([])
const orderLoading = ref(false)
const orderTab = ref<'appointment' | 'product'>('appointment')

const orderStatusMap: Record<number, { text: string; cls: string }> = {
  0: { text: '待确认', cls: 'os-pending' },
  1: { text: '已确认', cls: 'os-confirmed' },
  2: { text: '已完成', cls: 'os-completed' },
  3: { text: '已取消', cls: 'os-cancelled' },
}

const prodOrderStatusMap: Record<number, { text: string; cls: string }> = {
  0: { text: '待发货', cls: 'os-pending' },
  1: { text: '已发货', cls: 'os-confirmed' },
  2: { text: '已完成', cls: 'os-completed' },
  3: { text: '已取消', cls: 'os-cancelled' },
}

async function loadOrders() {
  orderLoading.value = true
  try {
    const [appRes, prodRes] = await Promise.all([listAllOrders(), listAllProductOrders()])
    orderList.value = appRes.data
    productOrderList.value = prodRes.data
  } catch { orderList.value = []; productOrderList.value = [] }
  finally { orderLoading.value = false }
}

async function handleCancelOrder(id: number) {
  if (!confirm('确定取消该订单吗？')) return
  try {
    await cancelOrderApi(id)
    loadOrders()
  } catch { alert('操作失败') }
}

async function handleUpdateOrderStatus(order: OrderVO, newStatus: number) {
  try {
    await adminUpdateOrderStatus(order.id, newStatus)
    order.status = newStatus as OrderVO['status']
  } catch { alert('操作失败') }
}

async function handleUpdateProdOrderStatus(order: ProductOrderVO, newStatus: number) {
  try {
    await adminUpdateProductOrderStatus(order.id, newStatus)
    order.status = newStatus
  } catch { alert('操作失败') }
}

// ---------- 预约订单字段编辑 ----------
const editingAppFields = ref<Record<number, string>>({}) // 'date' | 'timeSlot'

function startEditAppField(orderId: number, field: string) {
  editingAppFields.value = { [orderId]: field }
}
function cancelEditAppField() {
  editingAppFields.value = {}
}
async function saveAppField(order: OrderVO, field: string, value: string) {
  editingAppFields.value = {}
  if (field === 'appointmentDate') order.appointmentDate = value
  else if (field === 'timeSlot') order.timeSlot = value
  try {
    await adminUpdateOrderFields(order.id,
      field === 'appointmentDate' ? value : undefined,
      field === 'timeSlot' ? value : undefined)
  } catch { alert('保存失败'); loadOrders() }
}

// ---------- 商品订单地址编辑 ----------
const editingProdAddr = ref<number>(0) // order id being edited

function startEditProdAddr(orderId: number) { editingProdAddr.value = orderId }
function cancelEditProdAddr() { editingProdAddr.value = 0 }
async function saveProdAddr(order: ProductOrderVO, value: string) {
  editingProdAddr.value = 0
  order.address = value
  try { await adminUpdateProductOrderAddress(order.id, value) }
  catch { alert('保存失败'); loadOrders() }
}

// ==================== 8. 系统设置 ====================
const adminForm = reactive({ username: '', name: '', phone: '', sex: '', idNumber: '' })
const adminMsg = ref('')
const adminMsgType = ref<'success' | 'error'>('success')
const adminSubmitting = ref(false)

async function handleCreateAdmin() {
  if (!adminForm.username || !adminForm.name) {
    adminMsg.value = '用户名和姓名为必填项'
    adminMsgType.value = 'error'
    return
  }
  adminSubmitting.value = true
  adminMsg.value = ''
  try {
    await adminCreate({
      username: adminForm.username,
      name: adminForm.name,
      phone: adminForm.phone || undefined,
      sex: adminForm.sex || undefined,
      idNumber: adminForm.idNumber || undefined,
    })
    adminMsg.value = `管理员「${adminForm.name}」创建成功，默认密码 123456`
    adminMsgType.value = 'success'
    adminForm.username = ''; adminForm.name = ''; adminForm.phone = ''
    adminForm.sex = ''; adminForm.idNumber = ''
  } catch (e: unknown) {
    adminMsg.value = e instanceof Error ? e.message : '创建失败'
    adminMsgType.value = 'error'
  } finally { adminSubmitting.value = false }
}

// ---------- 修改密码 ----------
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const pwdMsg = ref('')
const pwdMsgType = ref<'success' | 'error'>('success')
const pwdSubmitting = ref(false)

async function handleChangePassword() {
  pwdMsg.value = ''
  if (!pwdForm.oldPassword) { pwdMsg.value = '请输入旧密码'; pwdMsgType.value = 'error'; return }
  if (!pwdForm.newPassword || pwdForm.newPassword.length < 6) { pwdMsg.value = '新密码至少6位'; pwdMsgType.value = 'error'; return }
  if (pwdForm.newPassword !== pwdForm.confirmPassword) { pwdMsg.value = '两次密码不一致'; pwdMsgType.value = 'error'; return }
  pwdSubmitting.value = true
  try {
    await changePassword(pwdForm.oldPassword, pwdForm.newPassword)
    pwdMsg.value = '密码修改成功'
    pwdMsgType.value = 'success'
    pwdForm.oldPassword = ''; pwdForm.newPassword = ''; pwdForm.confirmPassword = ''
  } catch (e: unknown) {
    pwdMsg.value = e instanceof Error ? e.message : '修改失败'
    pwdMsgType.value = 'error'
  } finally { pwdSubmitting.value = false }
}

function handleLogout() {
  authStore.logout()
  router.push('/')
}

// ==================== 仪表盘鼠标互动效果 ====================
function onStatCardMouseMove(e: MouseEvent) {
  const card = e.currentTarget as HTMLElement
  const rect = card.getBoundingClientRect()
  const x = e.clientX - rect.left - rect.width / 2
  const y = e.clientY - rect.top - rect.height / 2
  card.style.transform = `perspective(600px) rotateY(${x * 0.02}deg) rotateX(${-y * 0.02}deg) translateY(-6px)`
  card.style.setProperty('--mx', `${e.clientX - rect.left}px`)
  card.style.setProperty('--my', `${e.clientY - rect.top}px`)
}

function onStatCardMouseEnter(e: MouseEvent) {
  const card = e.currentTarget as HTMLElement
  card.style.transition = 'transform 0.15s ease-out, box-shadow 0.25s ease'
}

function onStatCardMouseLeave(e: MouseEvent) {
  const card = e.currentTarget as HTMLElement
  card.style.transition = 'transform 0.5s ease, box-shadow 0.5s ease'
  card.style.transform = 'perspective(600px) rotateY(0deg) rotateX(0deg) translateY(0)'
}

function onQuickCardMouseMove(e: MouseEvent) {
  const card = e.currentTarget as HTMLElement
  const rect = card.getBoundingClientRect()
  const x = e.clientX - rect.left
  card.style.setProperty('--qx', `${x}px`)
}

function onQuickCardMouseEnter(e: MouseEvent) {
  const card = e.currentTarget as HTMLElement
  card.style.transition = 'all 0.15s ease-out'
}

function onQuickCardMouseLeave(e: MouseEvent) {
  const card = e.currentTarget as HTMLElement
  card.style.transition = 'all 0.3s ease'
}
</script>

<template>
  <div class="admin-layout">
    <!-- ==================== 侧边栏 ==================== -->
    <aside class="sidebar">
      <div class="sidebar-brand" @click="switchMenu('dashboard')">
        <span class="brand-logo">码上非遗</span>
        <span class="brand-sub">管理后台</span>
      </div>

      <nav class="sidebar-nav">
        <a
          v-for="item in menuItems"
          :key="item.key"
          class="nav-item"
          :class="{ active: activeMenu === item.key }"
          @click="switchMenu(item.key)"
        >
          <span class="nav-icon">{{ item.icon }}</span>
          <span class="nav-label">{{ item.label }}</span>
          <span v-if="item.key === 'pending' && pendingList.length > 0" class="nav-badge">
            {{ pendingList.length }}
          </span>
        </a>
      </nav>

      <div class="sidebar-footer">
        <span class="sidebar-user">{{ authStore.userInfo?.name }}</span>
        <span class="sidebar-role">管理员</span>
      </div>
    </aside>

    <!-- ==================== 主区域 ==================== -->
    <div class="main-area">
      <header class="top-bar">
        <h2 class="page-title">
          {{ menuItems.find(m => m.key === activeMenu)?.label }}
        </h2>
        <div class="top-actions">
          <button class="top-btn" @click="switchMenu('dashboard')">仪表盘</button>
          <button class="top-btn logout" @click="handleLogout">退出登录</button>
        </div>
      </header>

      <main class="content">

        <!-- ===== 1. 仪表盘 ===== -->
        <section v-if="activeMenu === 'dashboard'" class="dashboard">
          <div v-if="statsLoading" class="loading-text">加载中...</div>
          <div v-else-if="statsError" class="error-text">
            {{ statsError }}
            <button class="btn-sm btn-primary" style="margin-left: 12px" @click="loadDashboard">重试</button>
          </div>
          <div v-else-if="!stats" class="empty-text">暂无统计数据</div>
          <template v-else>
            <div class="stat-grid">
              <div class="stat-card" @mousemove="onStatCardMouseMove" @mouseenter="onStatCardMouseEnter" @mouseleave="onStatCardMouseLeave">
                <div class="stat-glow"></div>
                <div class="stat-icon-wrap">👥</div>
                <div class="stat-num">{{ stats.totalUsers }}</div>
                <div class="stat-label">用户总数</div>
              </div>
              <div class="stat-card accent-green" @mousemove="onStatCardMouseMove" @mouseenter="onStatCardMouseEnter" @mouseleave="onStatCardMouseLeave">
                <div class="stat-glow"></div>
                <div class="stat-icon-wrap">🙋</div>
                <div class="stat-num">{{ stats.userCount }}</div>
                <div class="stat-label">普通用户</div>
              </div>
              <div class="stat-card accent-orange" @mousemove="onStatCardMouseMove" @mouseenter="onStatCardMouseEnter" @mouseleave="onStatCardMouseLeave">
                <div class="stat-glow"></div>
                <div class="stat-icon-wrap">🎨</div>
                <div class="stat-num">{{ stats.inheritorCount }}</div>
                <div class="stat-label">传承人</div>
              </div>
              <div class="stat-card accent-red" @mousemove="onStatCardMouseMove" @mouseenter="onStatCardMouseEnter" @mouseleave="onStatCardMouseLeave">
                <div class="stat-glow"></div>
                <div class="stat-icon-wrap">🛡️</div>
                <div class="stat-num">{{ stats.adminCount }}</div>
                <div class="stat-label">管理员</div>
              </div>
              <div class="stat-card accent-purple" @mousemove="onStatCardMouseMove" @mouseenter="onStatCardMouseEnter" @mouseleave="onStatCardMouseLeave">
                <div class="stat-glow"></div>
                <div class="stat-icon-wrap">✅</div>
                <div class="stat-num">{{ stats.enabledCount }}</div>
                <div class="stat-label">已启用</div>
              </div>
              <div class="stat-card accent-yellow" @mousemove="onStatCardMouseMove" @mouseenter="onStatCardMouseEnter" @mouseleave="onStatCardMouseLeave">
                <div class="stat-glow"></div>
                <div class="stat-icon-wrap">⏳</div>
                <div class="stat-num">{{ stats.pendingCount }}</div>
                <div class="stat-label">待审核</div>
              </div>
            </div>

            <!-- 快捷入口 -->
            <div class="quick-actions">
              <h3>快捷操作</h3>
              <div class="quick-grid">
                <div class="quick-card" @click="switchMenu('pending')" @mousemove="onQuickCardMouseMove" @mouseenter="onQuickCardMouseEnter" @mouseleave="onQuickCardMouseLeave">
                  <span class="quick-icon">✅</span>
                  <span>审核传承人申请</span>
                  <span v-if="stats.pendingCount > 0" class="quick-badge">{{ stats.pendingCount }}</span>
                </div>
                <div class="quick-card" @click="switchMenu('users')" @mousemove="onQuickCardMouseMove" @mouseenter="onQuickCardMouseEnter" @mouseleave="onQuickCardMouseLeave">
                  <span class="quick-icon">👥</span>
                  <span>管理用户</span>
                </div>
                <div class="quick-card" @click="switchMenu('orders')" @mousemove="onQuickCardMouseMove" @mouseenter="onQuickCardMouseEnter" @mouseleave="onQuickCardMouseLeave">
                  <span class="quick-icon">🛒</span>
                  <span>查看订单</span>
                </div>
                <div class="quick-card" @click="switchMenu('settings')" @mousemove="onQuickCardMouseMove" @mouseenter="onQuickCardMouseEnter" @mouseleave="onQuickCardMouseLeave">
                  <span class="quick-icon">⚙️</span>
                  <span>添加管理员</span>
                </div>
              </div>
            </div>
          </template>
        </section>

        <!-- ===== 2. 用户管理 ===== -->
        <section v-if="activeMenu === 'users'" class="panel">
          <div class="panel-toolbar">
            <div class="search-box">
              <input
                v-model="userSearchKeyword"
                type="text"
                placeholder="搜索姓名或用户名..."
                @keyup.enter="handleUserSearch"
              />
              <button @click="handleUserSearch">搜索</button>
              <button v-if="userSearchKeyword" class="btn-clear" @click="userSearchKeyword = ''; loadUsers()">清空</button>
            </div>
          </div>

          <div v-if="userLoading" class="loading-text">加载中...</div>
          <div v-else class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>姓名</th>
                  <th>用户名</th>
                  <th>手机号</th>
                  <th>角色</th>
                  <th>状态</th>
                  <th>注册时间</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="u in userList" :key="u.id">
                  <td>{{ u.id }}</td>
                  <td>{{ u.name }}</td>
                  <td>{{ u.username }}</td>
                  <td>{{ u.phone || '-' }}</td>
                  <td><span class="role-tag" :class="'role-' + u.role.toLowerCase()">{{ roleLabel(u.role) }}</span></td>
                  <td><span class="status-dot" :class="statusClass(u.status)">{{ statusLabel(u.status) }}</span></td>
                  <td>{{ u.createTime?.slice(0, 10) }}</td>
                  <td>
                    <button
                      v-if="u.role !== 'ADMIN'"
                      class="btn-sm"
                      :class="u.status === 1 ? 'btn-danger' : 'btn-success'"
                      :disabled="userActionLoading[u.id]"
                      @click="handleToggleStatus(u)"
                    >
                      {{ u.status === 1 ? '禁用' : '启用' }}
                    </button>
                    <span v-else class="no-op">—</span>
                  </td>
                </tr>
              </tbody>
            </table>
            <div v-if="userList.length === 0" class="empty-text">暂无用户数据</div>
          </div>
        </section>

        <!-- ===== 3. 审核管理 ===== -->
        <section v-if="activeMenu === 'pending'" class="panel">
          <div v-if="pendingLoading" class="loading-text">加载中...</div>
          <div v-else-if="pendingError" class="error-text">
            {{ pendingError }}
            <button class="btn-sm btn-primary" @click="loadPendingList">重试</button>
          </div>
          <div v-else class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>姓名</th>
                  <th>用户名</th>
                  <th>手机号</th>
                  <th>性别</th>
                  <th>身份证号</th>
                  <th>所属地区</th>
                  <th>申请时间</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="user in pendingList" :key="user.id">
                  <td>{{ user.name }}</td>
                  <td>{{ user.username }}</td>
                  <td>{{ user.phone || '-' }}</td>
                  <td>{{ user.sex || '-' }}</td>
                  <td>{{ user.idNumber || '-' }}</td>
                  <td>{{ user.region || '-' }}</td>
                  <td>{{ user.createTime?.slice(0, 10) || '-' }}</td>
                  <td class="action-col">
                    <button class="btn-sm btn-success" :disabled="!!pendingActionLoading[user.id]" @click="handleApprove(user.id)">
                      {{ pendingActionLoading[user.id] === 'approve' ? '...' : '通过' }}
                    </button>
                    <button class="btn-sm btn-danger" :disabled="!!pendingActionLoading[user.id]" @click="handleReject(user.id)">
                      {{ pendingActionLoading[user.id] === 'reject' ? '...' : '驳回' }}
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
            <div v-if="pendingList.length === 0" class="empty-text">暂无待审核的传承人申请</div>
          </div>
        </section>

        <!-- ===== 4. 资讯管理 ===== -->
        <section v-if="activeMenu === 'news'" class="panel">
          <div class="panel-toolbar">
            <div class="search-box">
              <input v-model="newsKeyword" type="text" placeholder="搜索标题/作者/分类..." />
              <button v-if="newsKeyword" class="btn-clear" @click="newsKeyword = ''">清空</button>
            </div>
          </div>
          <div v-if="articleLoading" class="loading-text">加载中...</div>
          <div v-else class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>标题</th>
                  <th>分类</th>
                  <th>作者</th>
                  <th>状态</th>
                  <th>浏览量</th>
                  <th>发布时间</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="a in filteredArticles" :key="a.id">
                  <td>{{ a.id }}</td>
                  <td class="ellipsis-cell">{{ a.title }}</td>
                  <td>{{ a.category || '-' }}</td>
                  <td>{{ a.authorName || '-' }}</td>
                  <td><span class="status-dot" :class="articleStatusClass(a.status)">{{ articleStatusLabel(a.status) }}</span></td>
                  <td>{{ a.viewCount }}</td>
                  <td>{{ a.createTime?.slice(0, 10) }}</td>
                  <td>
                    <button class="btn-sm btn-danger" @click="handleDeleteArticle(a.id)">删除</button>
                  </td>
                </tr>
              </tbody>
            </table>
            <div v-if="filteredArticles.length === 0 && !articleLoading" class="empty-text">{{ newsKeyword ? '无匹配结果' : '暂无资讯' }}</div>
          </div>
        </section>

        <!-- ===== 5. 商品管理 ===== -->
        <section v-if="activeMenu === 'products'" class="panel">
          <div class="panel-toolbar">
            <div class="search-box">
              <input v-model="productKeyword" type="text" placeholder="搜索名称/分类/传承人..." />
              <button v-if="productKeyword" class="btn-clear" @click="productKeyword = ''">清空</button>
            </div>
          </div>
          <div v-if="productLoading" class="loading-text">加载中...</div>
          <div v-else class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>商品名称</th>
                  <th>分类</th>
                  <th>价格</th>
                  <th>库存</th>
                  <th>评分</th>
                  <th>传承人</th>
                  <th>状态</th>
                  <th>上架时间</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="p in filteredProducts" :key="p.id">
                  <td>{{ p.id }}</td>
                  <td class="ellipsis-cell">{{ p.name }}</td>
                  <td>{{ p.category || '-' }}</td>
                  <td>¥{{ p.price }}</td>
                  <td>{{ p.stock }}</td>
                  <td>{{ p.avgRating ? p.avgRating + ' (' + (p.reviewCount || 0) + ')' : '-' }}</td>
                  <td>{{ p.inheritorName || '-' }}</td>
                  <td><span class="status-dot" :class="p.status === 1 ? 's-enabled' : 's-disabled'">{{ p.status === 1 ? '上架' : '下架' }}</span></td>
                  <td>{{ p.createTime?.slice(0, 10) }}</td>
                  <td class="action-col">
                    <button class="btn-sm" :class="p.status === 1 ? 'btn-danger' : 'btn-success'" @click="handleToggleProduct(p)">{{ p.status === 1 ? '下架' : '上架' }}</button>
                    <button class="btn-sm btn-danger" @click="handleDeleteProduct(p.id)">删除</button>
                  </td>
                </tr>
              </tbody>
            </table>
            <div v-if="filteredProducts.length === 0 && !productLoading" class="empty-text">{{ productKeyword ? '无匹配结果' : '暂无商品' }}</div>
          </div>
        </section>

        <!-- ===== 6. 项目管理 ===== -->
        <section v-if="activeMenu === 'projects'" class="panel">
          <div class="panel-toolbar">
            <div class="search-box">
              <input v-model="projectKeyword" type="text" placeholder="搜索名称/分类/传承人/类型..." />
              <button v-if="projectKeyword" class="btn-clear" @click="projectKeyword = ''">清空</button>
            </div>
          </div>
          <div v-if="projectLoading" class="loading-text">加载中...</div>
          <div v-else class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>项目名称</th>
                  <th>分类</th>
                  <th>类型</th>
                  <th>传承人</th>
                  <th>地点</th>
                  <th>人数(当前/上限)</th>
                  <th>价格</th>
                  <th>状态</th>
                  <th>创建时间</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="p in filteredProjects" :key="p.id">
                  <td>{{ p.id }}</td>
                  <td class="ellipsis-cell">{{ p.title }}</td>
                  <td>{{ p.category || '-' }}</td>
                  <td>
                    <span class="status-dot" :class="p.projectType === 1 ? 's-pending' : 's-enabled'">{{ projectTypeLabel(p.projectType ?? 0) }}</span>
                  </td>
                  <td>{{ p.inheritorName || '-' }}</td>
                  <td>{{ p.location || '-' }}</td>
                  <td>{{ p.currentPeople }}/{{ p.maxPeople }}</td>
                  <td>{{ (p.projectType === 1 && p.price != null) ? '¥' + p.price.toFixed(2) : '-' }}</td>
                  <td><span class="status-dot" :class="p.status === 1 ? 's-enabled' : 's-disabled'">{{ projectStatusLabel(p.status) }}</span></td>
                  <td>{{ p.createTime?.slice(0, 10) }}</td>
                  <td>
                    <button class="btn-sm btn-danger" @click="handleDeleteProject(p.id)">删除</button>
                  </td>
                </tr>
              </tbody>
            </table>
            <div v-if="filteredProjects.length === 0 && !projectLoading" class="empty-text">{{ projectKeyword ? '无匹配结果' : '暂无项目' }}</div>
          </div>
        </section>

        <!-- ===== 7. 订单管理 ===== -->
        <section v-if="activeMenu === 'orders'" class="panel">
          <div class="panel-toolbar">
            <div class="tab-switch">
              <button :class="{ active: orderTab === 'appointment' }" @click="orderTab = 'appointment'">预约订单</button>
              <button :class="{ active: orderTab === 'product' }" @click="orderTab = 'product'">商城订单</button>
            </div>
            <div class="search-box">
              <input v-if="orderTab === 'appointment'" v-model="orderKeyword" type="text" placeholder="搜索ID/用户/电话..." />
              <input v-else v-model="prodOrderKeyword" type="text" placeholder="搜索ID/用户/商品..." />
              <button v-if="(orderTab === 'appointment' && orderKeyword) || (orderTab === 'product' && prodOrderKeyword)" class="btn-clear" @click="orderKeyword = ''; prodOrderKeyword = ''">清空</button>
            </div>
          </div>
          <div v-if="orderLoading" class="loading-text">加载中...</div>
          <!-- 预约订单表 -->
          <div v-else-if="orderTab === 'appointment'" class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>ID</th><th>用户</th><th>电话</th><th>预约日期</th><th>时段</th>
                  <th>人数</th><th>传承人ID</th><th>状态</th><th>提交时间</th><th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="o in filteredOrders" :key="o.id">
                  <td>{{ o.id }}</td><td>{{ o.userName }}</td><td>{{ o.phone }}</td>
                  <td class="editable-cell" @click="startEditAppField(o.id, 'appointmentDate')">
                    <template v-if="editingAppFields[o.id] === 'appointmentDate'">
                      <input type="date" :value="o.appointmentDate"
                        @blur="saveAppField(o, 'appointmentDate', ($event.target as HTMLInputElement).value)"
                        @keyup.enter="saveAppField(o, 'appointmentDate', ($event.target as HTMLInputElement).value)"
                        @keyup.escape="cancelEditAppField()" autofocus />
                    </template>
                    <template v-else>{{ o.appointmentDate }}</template>
                  </td>
                  <td class="editable-cell" @click="startEditAppField(o.id, 'timeSlot')">
                    <template v-if="editingAppFields[o.id] === 'timeSlot'">
                      <input type="text" :value="o.timeSlot"
                        @blur="saveAppField(o, 'timeSlot', ($event.target as HTMLInputElement).value)"
                        @keyup.enter="saveAppField(o, 'timeSlot', ($event.target as HTMLInputElement).value)"
                        @keyup.escape="cancelEditAppField()" autofocus />
                    </template>
                    <template v-else>{{ o.timeSlot }}</template>
                  </td>
                  <td>{{ o.peopleCount }}人</td><td>{{ o.inheritorId || '-' }}</td>
                  <td>
                    <select class="status-select" :class="orderStatusMap[o.status]?.cls" :value="o.status" @change="handleUpdateOrderStatus(o, Number(($event.target as HTMLSelectElement).value))">
                      <option v-for="(v, k) in orderStatusMap" :key="k" :value="Number(k)">{{ v.text }}</option>
                    </select>
                  </td>
                  <td>{{ o.createTime?.slice(0, 10) }}</td>
                  <td><button v-if="o.status !== 3 && o.status !== 2" class="btn-sm btn-danger" @click="handleCancelOrder(o.id)">取消</button><span v-else class="no-op">—</span></td>
                </tr>
              </tbody>
            </table>
            <div v-if="filteredOrders.length === 0 && !orderLoading" class="empty-text">{{ orderKeyword ? '无匹配结果' : '暂无预约订单' }}</div>
          </div>
          <!-- 商城订单表 -->
          <div v-else class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>ID</th><th>用户</th><th>电话</th><th>地址</th><th>商品</th>
                  <th>单价</th><th>数量</th><th>总价</th><th>状态</th><th>提交时间</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="o in filteredProdOrders" :key="o.id">
                  <td>{{ o.id }}</td><td>{{ o.userName }}</td><td>{{ o.phone }}</td>
                  <td class="editable-cell ellipsis-cell" @click="startEditProdAddr(o.id)">
                    <template v-if="editingProdAddr === o.id">
                      <input type="text" :value="o.address || ''" placeholder="输入地址"
                        @blur="saveProdAddr(o, ($event.target as HTMLInputElement).value)"
                        @keyup.enter="saveProdAddr(o, ($event.target as HTMLInputElement).value)"
                        @keyup.escape="cancelEditProdAddr()" autofocus />
                    </template>
                    <template v-else>{{ o.address || '点击编辑' }}</template>
                  </td>
                  <td class="ellipsis-cell">{{ o.productName }}</td>
                  <td>¥{{ o.price }}</td><td>{{ o.quantity }}</td><td>¥{{ o.totalAmount }}</td>
                  <td>
                    <select class="status-select" :class="prodOrderStatusMap[o.status]?.cls" :value="o.status" @change="handleUpdateProdOrderStatus(o, Number(($event.target as HTMLSelectElement).value))">
                      <option v-for="(v, k) in prodOrderStatusMap" :key="k" :value="Number(k)">{{ v.text }}</option>
                    </select>
                  </td>
                  <td>{{ o.createTime?.slice(0, 10) }}</td>
                </tr>
              </tbody>
            </table>
            <div v-if="filteredProdOrders.length === 0 && !orderLoading" class="empty-text">{{ prodOrderKeyword ? '无匹配结果' : '暂无商城订单' }}</div>
          </div>
        </section>

        <!-- ===== 8. 消息管理 ===== -->
        <section v-if="activeMenu === 'messages'" class="panel" style="padding: 0; background: transparent; box-shadow: none;">
          <ChatPanel />
        </section>

        <!-- ===== 9. 系统设置 ===== -->
        <section v-if="activeMenu === 'settings'" class="panel">
          <div class="settings-card">
            <h3>创建新管理员</h3>
            <p class="settings-desc">新管理员默认密码为 <strong>123456</strong></p>

            <form class="settings-form" @submit.prevent="handleCreateAdmin">
              <div class="form-row">
                <div class="form-g">
                  <label>用户名 *</label>
                  <input v-model="adminForm.username" type="text" placeholder="登录用户名" />
                </div>
                <div class="form-g">
                  <label>姓名 *</label>
                  <input v-model="adminForm.name" type="text" placeholder="真实姓名" />
                </div>
              </div>
              <div class="form-row">
                <div class="form-g">
                  <label>手机号</label>
                  <input v-model="adminForm.phone" type="text" placeholder="手机号" />
                </div>
                <div class="form-g">
                  <label>性别</label>
                  <select v-model="adminForm.sex">
                    <option value="">请选择</option>
                    <option value="男">男</option>
                    <option value="女">女</option>
                  </select>
                </div>
              </div>
              <div class="form-g">
                <label>身份证号</label>
                <input v-model="adminForm.idNumber" type="text" placeholder="身份证号" maxlength="18" />
              </div>

              <p v-if="adminMsg" class="form-msg" :class="adminMsgType">{{ adminMsg }}</p>

              <button type="submit" class="btn-primary" :disabled="adminSubmitting">
                {{ adminSubmitting ? '创建中...' : '创建管理员' }}
              </button>
            </form>
          </div>

          <div class="settings-card" style="margin-top: 20px">
            <h3>修改密码</h3>
            <p class="settings-desc">修改当前管理员 <strong>{{ authStore.userInfo?.name }}</strong> 的登录密码</p>
            <form class="settings-form" @submit.prevent="handleChangePassword">
              <div class="form-g">
                <label>旧密码</label>
                <input v-model="pwdForm.oldPassword" type="password" placeholder="输入旧密码" autocomplete="current-password" />
              </div>
              <div class="form-row">
                <div class="form-g">
                  <label>新密码</label>
                  <input v-model="pwdForm.newPassword" type="password" placeholder="至少 6 位" autocomplete="new-password" />
                </div>
                <div class="form-g">
                  <label>确认新密码</label>
                  <input v-model="pwdForm.confirmPassword" type="password" placeholder="再次输入" autocomplete="new-password" />
                </div>
              </div>
              <p v-if="pwdMsg" class="form-msg" :class="pwdMsgType">{{ pwdMsg }}</p>
              <button type="submit" class="btn-primary" :disabled="pwdSubmitting">
                {{ pwdSubmitting ? '修改中...' : '修改密码' }}
              </button>
            </form>
          </div>
        </section>

      </main>
    </div>
  </div>
</template>

<style scoped>
/* ==================== 布局 ==================== */
.admin-layout {
  display: flex;
  min-height: 100vh;
  background: #fcf4e8;
}

/* ==================== 侧边栏 ==================== */
.sidebar {
  width: 240px;
  background: linear-gradient(180deg, #8b0e1e 0%, #c41e3a 40%, #a8162b 100%);
  color: #fff;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  box-shadow: 2px 0 12px rgba(196,30,58,0.15);
}
.sidebar-brand {
  padding: 28px 20px 20px;
  cursor: pointer;
  border-bottom: 1px solid rgba(255,255,255,0.12);
  transition: 0.2s;
}
.sidebar-brand:hover { background: rgba(255,255,255,0.06); }
.brand-logo {
  display: block;
  font-size: 22px;
  font-weight: bold;
  color: #fff;
  letter-spacing: 2px;
}
.brand-sub {
  font-size: 11px;
  color: rgba(255,255,255,0.55);
  margin-top: 4px;
  display: block;
  letter-spacing: 4px;
}
.sidebar-nav {
  flex: 1;
  padding: 10px 0;
  overflow-y: auto;
}
.nav-item {
  display: flex;
  align-items: center;
  padding: 13px 24px;
  margin: 2px 10px;
  color: rgba(255,255,255,0.7);
  cursor: pointer;
  transition: all 0.25s;
  font-size: 14px;
  border-radius: 8px;
  text-decoration: none;
  position: relative;
}
.nav-item:hover {
  color: #fff;
  background: rgba(255,255,255,0.12);
}
.nav-item.active {
  color: #c41e3a;
  background: #fff;
  font-weight: 600;
  box-shadow: 0 2px 8px rgba(0,0,0,0.15);
}
.nav-icon { margin-right: 12px; font-size: 16px; }
.nav-label { flex: 1; }
.nav-badge {
  background: #ffd700;
  color: #8b4513;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  min-width: 18px;
  text-align: center;
  font-weight: bold;
}
.sidebar-footer {
  padding: 16px 24px;
  border-top: 1px solid rgba(255,255,255,0.12);
  display: flex;
  align-items: center;
  gap: 10px;
}
.sidebar-user { font-size: 14px; color: #fff; }
.sidebar-role {
  font-size: 11px;
  background: #ffd700;
  color: #8b4513;
  padding: 1px 10px;
  border-radius: 10px;
  font-weight: bold;
}

/* ==================== 顶部栏 ==================== */
.main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.top-bar {
  height: 60px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 32px;
  box-shadow: 0 2px 12px rgba(196,30,58,0.08);
  flex-shrink: 0;
  border-bottom: 2px solid #ffe9ec;
}
.page-title {
  font-size: 18px;
  color: #c41e3a;
  margin: 0;
  padding-left: 14px;
  border-left: 4px solid #c41e3a;
}
.top-actions { display: flex; gap: 10px; }
.top-btn {
  padding: 7px 18px;
  font-size: 13px;
  border: 1px solid #e8647a;
  background: #fff;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.25s;
  color: #c41e3a;
}
.top-btn:hover {
  background: #c41e3a;
  color: #fff;
  border-color: #c41e3a;
}
.top-btn.logout {
  color: #999;
  border-color: #ddd;
}
.top-btn.logout:hover {
  background: #e74c3c;
  color: #fff;
  border-color: #e74c3c;
}

/* ==================== 内容区 ==================== */
.content {
  flex: 1;
  padding: 28px 32px;
  overflow-y: auto;
}

/* ==================== 仪表盘 — 灵动卡片 ==================== */
.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 24px;
  margin-bottom: 32px;
}
.stat-card {
  background: #fff;
  border-radius: 16px;
  padding: 28px 24px;
  text-align: center;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: box-shadow 0.3s ease;
  box-shadow: 0 4px 16px rgba(196,30,58,0.06);
  border-top: 4px solid #c41e3a;
  --mx: 50%;
  --my: 50%;
}
.stat-card::before {
  content: '';
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at var(--mx, 50%) var(--my, 50%), rgba(196,30,58,0.06), transparent 50%);
  opacity: 0;
  transition: opacity 0.3s;
  pointer-events: none;
  z-index: 0;
}
.stat-card:hover::before { opacity: 1; }
.stat-card.accent-green  { border-top-color: #27ae60; }
.stat-card.accent-orange { border-top-color: #e67e22; }
.stat-card.accent-red    { border-top-color: #e74c3c; }
.stat-card.accent-purple { border-top-color: #8e44ad; }
.stat-card.accent-yellow { border-top-color: #f39c12; }

.stat-glow {
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle at var(--mx, 50%) var(--my, 50%), rgba(255,255,255,0.3), transparent 30%);
  opacity: 0;
  transition: opacity 0.3s;
  pointer-events: none;
  z-index: 1;
}
.stat-card:hover .stat-glow { opacity: 0.5; }

.stat-icon-wrap {
  font-size: 32px;
  margin-bottom: 10px;
  position: relative;
  z-index: 2;
  transition: transform 0.3s;
}
.stat-card:hover .stat-icon-wrap { transform: scale(1.2); }
.stat-num {
  font-size: 40px;
  font-weight: bold;
  color: #333;
  position: relative;
  z-index: 2;
  transition: color 0.3s;
}
.stat-card:hover .stat-num { color: #c41e3a; }
.stat-card.accent-green:hover .stat-num  { color: #27ae60; }
.stat-card.accent-orange:hover .stat-num { color: #e67e22; }
.stat-card.accent-red:hover .stat-num    { color: #e74c3c; }
.stat-card.accent-purple:hover .stat-num { color: #8e44ad; }
.stat-card.accent-yellow:hover .stat-num { color: #f39c12; }
.stat-label {
  font-size: 13px;
  color: #999;
  margin-top: 8px;
  position: relative;
  z-index: 2;
  transition: color 0.3s;
}
.stat-card:hover .stat-label { color: #666; }

/* ==================== 快捷入口 ==================== */
.quick-actions {
  background: #fff;
  border-radius: 16px;
  padding: 28px;
  box-shadow: 0 4px 16px rgba(196,30,58,0.06);
}
.quick-actions h3 {
  margin: 0 0 20px;
  font-size: 17px;
  color: #c41e3a;
  padding-left: 14px;
  border-left: 4px solid #c41e3a;
}
.quick-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 16px;
}
.quick-card {
  --qx: 0px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 18px 22px;
  border: 1px solid #ffe9ec;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.25s;
  font-size: 14px;
  color: #555;
  background: #fffafb;
  position: relative;
  overflow: hidden;
}
.quick-card::after {
  content: '';
  position: absolute;
  left: var(--qx, 0);
  top: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(196,30,58,0.04), transparent);
  transition: left 0.1s;
  pointer-events: none;
}
.quick-card:hover {
  border-color: #c41e3a;
  background: #fff;
  transform: translateY(-3px);
  box-shadow: 0 6px 20px rgba(196,30,58,0.12);
}
.quick-icon { font-size: 22px; transition: transform 0.3s; }
.quick-card:hover .quick-icon { transform: scale(1.15); }
.quick-badge {
  margin-left: auto;
  background: #ff4d4f;
  color: #fff;
  font-size: 12px;
  padding: 3px 12px;
  border-radius: 12px;
  font-weight: bold;
  animation: badgePulse 2s ease-in-out infinite;
}
@keyframes badgePulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.1); }
}

/* ==================== 面板通用 ==================== */
.panel {
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 2px 12px rgba(196,30,58,0.05);
  overflow: hidden;
}
.panel-toolbar {
  padding: 18px 24px;
  border-bottom: 1px solid #ffe9ec;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.search-box { display: flex; gap: 8px; }
.search-box input {
  width: 260px; height: 38px; padding: 0 14px; border: 1px solid #e8647a; border-radius: 8px;
  font-size: 13px; outline: none; transition: 0.25s; background: #fafafa;
}
.search-box input:focus {
  border-color: #c41e3a;
  box-shadow: 0 0 0 3px rgba(196,30,58,0.08);
  background: #fff;
}
.search-box button, .btn-clear {
  padding: 0 18px; height: 38px; border: 1px solid #e8647a; background: #fff;
  border-radius: 8px; font-size: 13px; cursor: pointer; transition: all 0.25s; color: #c41e3a;
}
.search-box button:hover {
  background: #c41e3a; color: #fff; border-color: #c41e3a;
}
.btn-clear { color: #999; border-color: #ddd; }
.btn-clear:hover { background: #f5f5f5; color: #666; }

/* ==================== 表格 ==================== */
.table-wrap table { width: 100%; border-collapse: collapse; }
.table-wrap th, .table-wrap td {
  padding: 12px 16px; text-align: left; font-size: 13px; border-bottom: 1px solid #f5f0f0; white-space: nowrap;
}
.table-wrap thead { background: #fff5f7; }
.table-wrap th { font-weight: 600; color: #c41e3a; font-size: 12px; }
.table-wrap tbody tr { transition: 0.15s; }
.table-wrap tbody tr:hover { background: #fefafa; }
.action-col { display: flex; gap: 8px; }

/* 状态标签 */
.status-dot { display: inline-block; padding: 3px 12px; border-radius: 12px; font-size: 12px; font-weight: 600; }
.s-enabled { background: #d4edda; color: #155724; }
.s-disabled { background: #f8d7da; color: #721c24; }
.s-pending { background: #fff3cd; color: #856404; }
.os-pending { background: #fff3cd; color: #856404; }
.os-confirmed { background: #d4edda; color: #155724; }
.os-completed { background: #e2e3e5; color: #383d41; }
.os-cancelled { background: #f8d7da; color: #721c24; }

/* 角色标签 */
.role-tag { display: inline-block; padding: 3px 12px; border-radius: 12px; font-size: 12px; font-weight: 600; }
.role-admin { background: #c41e3a; color: #fff; }
.role-inheritor { background: #ffe9ec; color: #c41e3a; }
.role-user { background: #eee; color: #666; }

/* ==================== 按钮 ==================== */
.btn-primary {
  padding: 9px 22px; font-size: 14px; color: #fff; background: #c41e3a;
  border: none; border-radius: 8px; cursor: pointer; font-weight: 600; transition: all 0.25s;
}
.btn-primary:hover { background: #a8162b; transform: translateY(-1px); box-shadow: 0 4px 12px rgba(196,30,58,0.25); }
.btn-primary:disabled { opacity: 0.5; cursor: not-allowed; transform: none; }
.btn-sm {
  padding: 5px 16px; font-size: 12px; border: none; border-radius: 6px; cursor: pointer; font-weight: 500; transition: all 0.2s;
}
.btn-sm:hover:not(:disabled) { transform: translateY(-1px); }
.btn-success { background: #27ae60; color: #fff; }
.btn-success:hover:not(:disabled) { background: #219a52; }
.btn-danger { background: #e74c3c; color: #fff; }
.btn-danger:hover:not(:disabled) { background: #c0392b; }
.btn-sm:disabled { opacity: 0.5; cursor: not-allowed; }
.no-op { color: #ccc; }
.ellipsis-cell { max-width: 150px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

/* 订单类型切换 */
.tab-switch { display: flex; gap: 0; }
.tab-switch button {
  padding: 8px 20px; font-size: 13px; border: 1px solid #ddd; background: #fff;
  cursor: pointer; transition: 0.2s; color: #666;
}
.tab-switch button:first-child { border-radius: 6px 0 0 6px; }
.tab-switch button:last-child { border-radius: 0 6px 6px 0; }
.tab-switch button.active { background: #c41e3a; color: #fff; border-color: #c41e3a; }

/* 订单状态选择器 */
.status-select {
  padding: 3px 8px; border-radius: 4px; font-size: 12px; font-weight: 600; border: 1px solid #ddd;
  cursor: pointer; outline: none;
}
.status-select.os-pending { background: #fff3cd; color: #856404; border-color: #ffc107; }
.status-select.os-confirmed { background: #d4edda; color: #155724; border-color: #28a745; }
.status-select.os-completed { background: #e2e3e5; color: #383d41; border-color: #6c757d; }
.status-select.os-cancelled { background: #f8d7da; color: #721c24; border-color: #dc3545; }

/* 可编辑单元格 */
.editable-cell { cursor: pointer; transition: background 0.15s; border-radius: 3px; }
.editable-cell:hover { background: #fff3cd; }
.editable-cell input {
  width: 100%; padding: 4px 6px; border: 1px solid #c41e3a; border-radius: 4px;
  font-size: 13px; outline: none; box-sizing: border-box;
}

/* ==================== 状态文本 ==================== */
.loading-text { text-align: center; padding: 48px; color: #999; font-size: 14px; }
.error-text { text-align: center; padding: 48px; color: #e74c3c; font-size: 14px; }
.empty-text { text-align: center; padding: 48px; color: #ccc; font-size: 14px; }

/* ==================== 系统设置表单 ==================== */
.settings-card { padding: 36px; max-width: 640px; }
.settings-card h3 { margin: 0 0 4px; font-size: 20px; color: #c41e3a; }
.settings-desc { margin: 0 0 28px; font-size: 13px; color: #999; }
.settings-desc strong { color: #c41e3a; }
.settings-form { display: flex; flex-direction: column; }
.form-row { display: flex; gap: 20px; }
.form-row .form-g { flex: 1; }
.form-g { margin-bottom: 18px; }
.form-g label { display: block; font-size: 13px; color: #555; margin-bottom: 6px; font-weight: 600; }
.form-g input, .form-g select {
  width: 100%; height: 42px; padding: 0 14px; border: 1px solid #e0e0e0; border-radius: 8px;
  font-size: 14px; color: #333; outline: none; transition: 0.25s; box-sizing: border-box; background: #fafafa;
}
.form-g input:focus, .form-g select:focus {
  border-color: #c41e3a; box-shadow: 0 0 0 3px rgba(196,30,58,0.08); background: #fff;
}
.form-msg { font-size: 13px; margin: 0 0 16px; }
.form-msg.success { color: #27ae60; }
.form-msg.error { color: #e74c3c; }

/* ==================== 响应式 ==================== */
@media (max-width: 768px) {
  .sidebar { width: 200px; }
  .nav-item { padding: 10px 16px; font-size: 13px; margin: 1px 6px; }
  .content { padding: 16px; }
  .top-bar { padding: 0 16px; }
  .stat-grid { grid-template-columns: repeat(2, 1fr); gap: 14px; }
  .form-row { flex-direction: column; gap: 0; }
}
</style>
