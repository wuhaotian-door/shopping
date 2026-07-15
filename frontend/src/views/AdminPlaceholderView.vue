<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'
import {
  adminCreate, getPendingList, approveInheritor, rejectInheritor,
  listAllUsers, searchUsers, updateUserStatus, getDashboardStats,
} from '@/api/auth'
import { listAllOrders, cancelOrder as cancelOrderApi } from '@/api/order'
import type { PendingUser, UserInfo, DashboardStats, OrderVO } from '@/types/api'

const authStore = useAuthStore()
const router = useRouter()

onMounted(() => {
  document.title = '管理后台——码上非遗'
  loadDashboard()
})

// ==================== 侧边栏菜单 ====================
type MenuKey = 'dashboard' | 'users' | 'pending' | 'news' | 'orders' | 'settings'
const activeMenu = ref<MenuKey>('dashboard')

const menuItems: { key: MenuKey; label: string; icon: string }[] = [
  { key: 'dashboard', label: '仪表盘', icon: '📊' },
  { key: 'users', label: '用户管理', icon: '👥' },
  { key: 'pending', label: '审核管理', icon: '✅' },
  { key: 'news', label: '资讯管理', icon: '📰' },
  { key: 'orders', label: '订单管理', icon: '🛒' },
  { key: 'settings', label: '系统设置', icon: '⚙️' },
]

function switchMenu(key: MenuKey) {
  activeMenu.value = key
  if (key === 'dashboard') loadDashboard()
  else if (key === 'users') loadUsers()
  else if (key === 'pending') loadPendingList()
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

// ==================== 4. 资讯管理（占位） ====================
// 当前资讯为 mock 数据，后续接入后端 API

// ==================== 5. 订单管理 ====================
const orderList = ref<OrderVO[]>([])
const orderLoading = ref(false)

const orderStatusMap: Record<number, { text: string; cls: string }> = {
  0: { text: '待确认', cls: 'os-pending' },
  1: { text: '已确认', cls: 'os-confirmed' },
  2: { text: '已完成', cls: 'os-completed' },
  3: { text: '已取消', cls: 'os-cancelled' },
}

async function loadOrders() {
  orderLoading.value = true
  try {
    const res = await listAllOrders()
    orderList.value = res.data
  } catch { orderList.value = [] }
  finally { orderLoading.value = false }
}

async function handleCancelOrder(id: number) {
  if (!confirm('确定取消该订单吗？')) return
  try {
    await cancelOrderApi(id)
    loadOrders()
  } catch { alert('操作失败') }
}

// ==================== 6. 系统设置 ====================
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

function handleLogout() {
  authStore.logout()
  router.push('/')
}
</script>

<template>
  <div class="admin-layout">
    <!-- ==================== 侧边栏 ==================== -->
    <aside class="sidebar">
      <div class="sidebar-brand" @click="router.push('/')">
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
          <button class="top-btn" @click="router.push('/')">返回首页</button>
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
              <div class="stat-card">
                <div class="stat-num">{{ stats.totalUsers }}</div>
                <div class="stat-label">用户总数</div>
              </div>
              <div class="stat-card accent-green">
                <div class="stat-num">{{ stats.userCount }}</div>
                <div class="stat-label">普通用户</div>
              </div>
              <div class="stat-card accent-orange">
                <div class="stat-num">{{ stats.inheritorCount }}</div>
                <div class="stat-label">传承人</div>
              </div>
              <div class="stat-card accent-red">
                <div class="stat-num">{{ stats.adminCount }}</div>
                <div class="stat-label">管理员</div>
              </div>
              <div class="stat-card accent-purple">
                <div class="stat-num">{{ stats.enabledCount }}</div>
                <div class="stat-label">已启用</div>
              </div>
              <div class="stat-card accent-yellow">
                <div class="stat-num">{{ stats.pendingCount }}</div>
                <div class="stat-label">待审核</div>
              </div>
            </div>

            <!-- 快捷入口 -->
            <div class="quick-actions">
              <h3>快捷操作</h3>
              <div class="quick-grid">
                <div class="quick-card" @click="switchMenu('pending')">
                  <span class="quick-icon">✅</span>
                  <span>审核传承人申请</span>
                  <span v-if="stats.pendingCount > 0" class="quick-badge">{{ stats.pendingCount }}</span>
                </div>
                <div class="quick-card" @click="switchMenu('users')">
                  <span class="quick-icon">👥</span>
                  <span>管理用户</span>
                </div>
                <div class="quick-card" @click="switchMenu('orders')">
                  <span class="quick-icon">🛒</span>
                  <span>查看订单</span>
                </div>
                <div class="quick-card" @click="switchMenu('settings')">
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
            <button class="btn-primary">+ 新建资讯</button>
          </div>
          <div class="empty-text" style="padding: 60px">
            <p style="font-size: 48px; margin-bottom: 12px">📰</p>
            <p>资讯管理功能即将上线</p>
            <p style="font-size: 13px; color: #bbb; margin-top: 8px">当前首页资讯为静态展示数据，后续将接入后台 CMS 系统</p>
          </div>
        </section>

        <!-- ===== 5. 订单管理 ===== -->
        <section v-if="activeMenu === 'orders'" class="panel">
          <div v-if="orderLoading" class="loading-text">加载中...</div>
          <div v-else class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>用户</th>
                  <th>电话</th>
                  <th>预约日期</th>
                  <th>时段</th>
                  <th>人数</th>
                  <th>传承人ID</th>
                  <th>状态</th>
                  <th>提交时间</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="o in orderList" :key="o.id">
                  <td>{{ o.id }}</td>
                  <td>{{ o.userName }}</td>
                  <td>{{ o.phone }}</td>
                  <td>{{ o.appointmentDate }}</td>
                  <td>{{ o.timeSlot }}</td>
                  <td>{{ o.peopleCount }}人</td>
                  <td>{{ o.inheritorId || '-' }}</td>
                  <td><span class="status-dot" :class="orderStatusMap[o.status]?.cls">{{ orderStatusMap[o.status]?.text }}</span></td>
                  <td>{{ o.createTime?.slice(0, 10) }}</td>
                  <td>
                    <button
                      v-if="o.status !== 3 && o.status !== 2"
                      class="btn-sm btn-danger"
                      @click="handleCancelOrder(o.id)"
                    >取消</button>
                    <span v-else class="no-op">—</span>
                  </td>
                </tr>
              </tbody>
            </table>
            <div v-if="orderList.length === 0" class="empty-text">暂无订单数据</div>
          </div>
        </section>

        <!-- ===== 6. 系统设置 ===== -->
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
  background: #f0f2f5;
}

/* ==================== 侧边栏 ==================== */
.sidebar {
  width: 240px;
  background: #001529;
  color: #fff;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.sidebar-brand {
  padding: 24px 20px 20px;
  cursor: pointer;
  border-bottom: 1px solid rgba(255,255,255,0.06);
}
.brand-logo {
  display: block;
  font-size: 20px;
  font-weight: bold;
  color: #fff;
}
.brand-sub {
  font-size: 12px;
  color: rgba(255,255,255,0.45);
  margin-top: 4px;
  display: block;
}

.sidebar-nav {
  flex: 1;
  padding: 12px 0;
}

.nav-item {
  display: flex;
  align-items: center;
  padding: 12px 24px;
  color: rgba(255,255,255,0.65);
  cursor: pointer;
  transition: all 0.2s;
  font-size: 14px;
  position: relative;
  text-decoration: none;
}
.nav-item:hover {
  color: #fff;
  background: rgba(255,255,255,0.08);
}
.nav-item.active {
  color: #fff;
  background: #c41e3a;
}
.nav-icon {
  margin-right: 12px;
  font-size: 16px;
}
.nav-label {
  flex: 1;
}
.nav-badge {
  background: #ff4d4f;
  color: #fff;
  font-size: 11px;
  padding: 1px 7px;
  border-radius: 10px;
  min-width: 18px;
  text-align: center;
}

.sidebar-footer {
  padding: 16px 24px;
  border-top: 1px solid rgba(255,255,255,0.06);
  display: flex;
  align-items: center;
  gap: 10px;
}
.sidebar-user {
  font-size: 14px;
  color: #fff;
}
.sidebar-role {
  font-size: 11px;
  background: #c41e3a;
  padding: 1px 8px;
  border-radius: 8px;
}

/* ==================== 主区域 ==================== */
.main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.top-bar {
  height: 56px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 28px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
  flex-shrink: 0;
}
.page-title {
  font-size: 18px;
  color: #333;
  margin: 0;
}
.top-actions {
  display: flex;
  gap: 10px;
}
.top-btn {
  padding: 6px 16px;
  font-size: 13px;
  border: 1px solid #ddd;
  background: #fff;
  border-radius: 4px;
  cursor: pointer;
  transition: 0.2s;
}
.top-btn:hover {
  border-color: #c41e3a;
  color: #c41e3a;
}
.top-btn.logout {
  color: #e74c3c;
  border-color: #e74c3c;
}
.top-btn.logout:hover {
  background: #e74c3c;
  color: #fff;
}

.content {
  flex: 1;
  padding: 24px 28px;
  overflow-y: auto;
}

/* ==================== 仪表盘 ==================== */
.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 20px;
  margin-bottom: 28px;
}
.stat-card {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  text-align: center;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
  border-top: 3px solid #c41e3a;
}
.stat-card.accent-green  { border-top-color: #27ae60; }
.stat-card.accent-orange { border-top-color: #e67e22; }
.stat-card.accent-red    { border-top-color: #e74c3c; }
.stat-card.accent-purple { border-top-color: #8e44ad; }
.stat-card.accent-yellow { border-top-color: #f39c12; }

.stat-num {
  font-size: 36px;
  font-weight: bold;
  color: #333;
}
.stat-label {
  font-size: 13px;
  color: #999;
  margin-top: 6px;
}

.quick-actions {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}
.quick-actions h3 {
  margin: 0 0 16px;
  font-size: 16px;
  color: #333;
}
.quick-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
}
.quick-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 20px;
  border: 1px solid #eee;
  border-radius: 8px;
  cursor: pointer;
  transition: 0.2s;
  font-size: 14px;
  color: #444;
}
.quick-card:hover {
  border-color: #c41e3a;
  background: #fff5f7;
}
.quick-icon { font-size: 20px; }
.quick-badge {
  margin-left: auto;
  background: #ff4d4f;
  color: #fff;
  font-size: 12px;
  padding: 2px 10px;
  border-radius: 10px;
}

/* ==================== 面板通用 ==================== */
.panel {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}
.panel-toolbar {
  padding: 16px 24px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.search-box {
  display: flex;
  gap: 8px;
}
.search-box input {
  width: 260px;
  height: 36px;
  padding: 0 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 13px;
  outline: none;
  transition: 0.2s;
}
.search-box input:focus {
  border-color: #c41e3a;
}
.search-box button,
.btn-clear {
  padding: 0 16px;
  height: 36px;
  border: 1px solid #ddd;
  background: #fff;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  transition: 0.2s;
}
.search-box button:hover {
  background: #c41e3a;
  color: #fff;
  border-color: #c41e3a;
}

/* ==================== 表格 ==================== */
.table-wrap table {
  width: 100%;
  border-collapse: collapse;
}
.table-wrap th,
.table-wrap td {
  padding: 12px 16px;
  text-align: left;
  font-size: 13px;
  border-bottom: 1px solid #f0f0f0;
  white-space: nowrap;
}
.table-wrap thead {
  background: #fafafa;
}
.table-wrap th {
  font-weight: 600;
  color: #555;
  font-size: 12px;
}
.table-wrap tbody tr:hover {
  background: #fefafa;
}
.action-col {
  display: flex;
  gap: 8px;
}

/* 状态标签 */
.status-dot {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}
.s-enabled { background: #d4edda; color: #155724; }
.s-disabled { background: #f8d7da; color: #721c24; }
.s-pending { background: #fff3cd; color: #856404; }
.os-pending { background: #fff3cd; color: #856404; }
.os-confirmed { background: #d4edda; color: #155724; }
.os-completed { background: #e2e3e5; color: #383d41; }
.os-cancelled { background: #f8d7da; color: #721c24; }

/* 角色标签 */
.role-tag {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}
.role-admin { background: #c41e3a; color: #fff; }
.role-inheritor { background: #ffe9ec; color: #c41e3a; }
.role-user { background: #e8e8e8; color: #555; }

/* ==================== 按钮 ==================== */
.btn-primary {
  padding: 8px 20px;
  font-size: 14px;
  color: #fff;
  background: #c41e3a;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 600;
  transition: 0.2s;
}
.btn-primary:hover { background: #a8162b; }
.btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }

.btn-sm {
  padding: 4px 14px;
  font-size: 12px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-weight: 500;
  transition: 0.2s;
}
.btn-success { background: #27ae60; color: #fff; }
.btn-success:hover:not(:disabled) { background: #219a52; }
.btn-danger { background: #e74c3c; color: #fff; }
.btn-danger:hover:not(:disabled) { background: #c0392b; }
.btn-sm:disabled { opacity: 0.5; cursor: not-allowed; }

.no-op { color: #ccc; }

/* ==================== 状态文本 ==================== */
.loading-text { text-align: center; padding: 40px; color: #999; font-size: 14px; }
.error-text { text-align: center; padding: 40px; color: #e74c3c; font-size: 14px; }
.empty-text { text-align: center; padding: 40px; color: #bbb; font-size: 14px; }

/* ==================== 系统设置表单 ==================== */
.settings-card {
  padding: 32px;
  max-width: 640px;
}
.settings-card h3 {
  margin: 0 0 4px;
  font-size: 20px;
  color: #333;
}
.settings-desc {
  margin: 0 0 24px;
  font-size: 13px;
  color: #999;
}
.settings-desc strong {
  color: #c41e3a;
}
.settings-form {
  display: flex;
  flex-direction: column;
  gap: 0;
}
.form-row {
  display: flex;
  gap: 20px;
}
.form-row .form-g { flex: 1; }
.form-g {
  margin-bottom: 18px;
}
.form-g label {
  display: block;
  font-size: 13px;
  color: #555;
  margin-bottom: 5px;
  font-weight: 600;
}
.form-g input,
.form-g select {
  width: 100%;
  height: 40px;
  padding: 0 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
  color: #333;
  outline: none;
  transition: 0.2s;
  box-sizing: border-box;
  background: #fff;
}
.form-g input:focus,
.form-g select:focus {
  border-color: #c41e3a;
  box-shadow: 0 0 0 3px rgba(196,30,58,0.08);
}
.form-msg {
  font-size: 13px;
  margin: 0 0 14px;
}
.form-msg.success { color: #27ae60; }
.form-msg.error { color: #e74c3c; }

/* ==================== 响应式 ==================== */
@media (max-width: 768px) {
  .sidebar { width: 200px; }
  .nav-item { padding: 10px 16px; font-size: 13px; }
  .content { padding: 16px; }
  .top-bar { padding: 0 16px; }
  .stat-grid { grid-template-columns: repeat(2, 1fr); }
  .form-row { flex-direction: column; gap: 0; }
}
</style>
