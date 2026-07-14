<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'
import { adminCreate, getPendingList, approveInheritor, rejectInheritor } from '@/api/auth'
import type { PendingUser } from '@/types/api'

const authStore = useAuthStore()
const router = useRouter()

onMounted(() => {
  document.title = '管理后台——码上非遗'
  loadPendingList()
})

// ---------- 注册管理员表单 ----------
const form = reactive({
  username: '',
  name: '',
  phone: '',
  sex: '',
  idNumber: '',
})

const successMsg = ref('')
const errorMsg = ref('')
const registering = ref(false)

async function handleRegister() {
  if (!form.username || !form.name) {
    errorMsg.value = '用户名和姓名为必填项'
    successMsg.value = ''
    return
  }

  registering.value = true
  errorMsg.value = ''
  successMsg.value = ''

  try {
    await adminCreate({
      username: form.username,
      name: form.name,
      phone: form.phone || undefined,
      sex: form.sex || undefined,
      idNumber: form.idNumber || undefined,
    })
    successMsg.value = `管理员「${form.name}」创建成功，默认密码为 123456`
    form.username = ''
    form.name = ''
    form.phone = ''
    form.sex = ''
    form.idNumber = ''
  } catch (e: unknown) {
    errorMsg.value = e instanceof Error ? e.message : '创建失败，请重试'
  } finally {
    registering.value = false
  }
}

// ---------- 待审核传承人列表 ----------
const pendingList = ref<PendingUser[]>([])
const pendingLoading = ref(false)
const pendingError = ref('')
const actionLoading = ref<Record<number, string>>({})

async function loadPendingList() {
  pendingLoading.value = true
  pendingError.value = ''
  try {
    const res = await getPendingList()
    pendingList.value = res.data
  } catch (e: unknown) {
    pendingError.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    pendingLoading.value = false
  }
}

async function handleApprove(id: number) {
  actionLoading.value = { ...actionLoading.value, [id]: 'approve' }
  try {
    await approveInheritor(id)
    pendingList.value = pendingList.value.filter(u => u.id !== id)
  } catch (e: unknown) {
    alert(e instanceof Error ? e.message : '操作失败')
  } finally {
    const next = { ...actionLoading.value }
    delete next[id]
    actionLoading.value = next
  }
}

async function handleReject(id: number) {
  actionLoading.value = { ...actionLoading.value, [id]: 'reject' }
  try {
    await rejectInheritor(id)
    pendingList.value = pendingList.value.filter(u => u.id !== id)
  } catch (e: unknown) {
    alert(e instanceof Error ? e.message : '操作失败')
  } finally {
    const next = { ...actionLoading.value }
    delete next[id]
    actionLoading.value = next
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
      <span class="logo" @click="router.push('/')">码上非遗</span>
      <span class="admin-tag">管理后台</span>
    </div>
    <div class="header-right">
      <span class="user-name">{{ authStore.userInfo?.name }}</span>
      <span class="role-badge">管理员</span>
      <button class="logout-btn" @click="handleLogout">退出</button>
      <a class="back-home-link" @click="router.push('/')">返回首页</a>
    </div>
  </header>

  <!-- ==================== 主要内容 ==================== -->
  <main class="admin-main">
    <!-- 欢迎卡片 -->
    <div class="welcome-card">
      <h2>欢迎回来，{{ authStore.userInfo?.name }}！</h2>
      <p>管理员后台 · 管理系统用户与审核传承人入驻申请</p>
      <div class="info-row">
        <span class="info-label">用户名：</span>
        <span>{{ authStore.userInfo?.userName }}</span>
      </div>
      <div class="info-row">
        <span class="info-label">用户 ID：</span>
        <span>{{ authStore.userInfo?.id }}</span>
      </div>
    </div>

    <!-- 待审核传承人列表 -->
    <div class="pending-card">
      <h2>待审核传承人</h2>
      <p>审核非遗传承人的入驻申请，通过后可登录系统。</p>

      <div v-if="pendingLoading" class="pending-status">加载中...</div>
      <div v-else-if="pendingError" class="pending-status error">
        {{ pendingError }}
        <button class="retry-btn" @click="loadPendingList">重试</button>
      </div>
      <div v-else-if="pendingList.length === 0" class="pending-empty">
        <div class="empty-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M9 12l2 2 4-4"/>
            <circle cx="12" cy="12" r="10"/>
          </svg>
        </div>
        暂无待审核的传承人申请
      </div>
      <div v-else class="pending-table-wrap">
        <table class="pending-table">
          <thead>
            <tr>
              <th>姓名</th>
              <th>用户名</th>
              <th>手机号</th>
              <th>性别</th>
              <th>身份证号</th>
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
              <td>{{ user.createTime?.slice(0, 10) || '-' }}</td>
              <td class="action-cell">
                <button
                  class="approve-btn"
                  :disabled="actionLoading[user.id] === 'approve'"
                  @click="handleApprove(user.id)"
                >
                  {{ actionLoading[user.id] === 'approve' ? '处理中...' : '通过' }}
                </button>
                <button
                  class="reject-btn"
                  :disabled="actionLoading[user.id] === 'reject'"
                  @click="handleReject(user.id)"
                >
                  {{ actionLoading[user.id] === 'reject' ? '处理中...' : '驳回' }}
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 注册新管理员 -->
    <div class="register-card">
      <h2>注册新管理员</h2>
      <p>新管理员的默认密码为 <strong>123456</strong>，请提醒其尽快修改。</p>

      <form class="register-form" @submit.prevent="handleRegister">
        <div class="form-row">
          <div class="form-item">
            <label for="reg-username">用户名 *</label>
            <input id="reg-username" v-model="form.username" type="text" placeholder="登录用户名" />
          </div>
          <div class="form-item">
            <label for="reg-name">姓名 *</label>
            <input id="reg-name" v-model="form.name" type="text" placeholder="真实姓名" />
          </div>
        </div>

        <div class="form-row">
          <div class="form-item">
            <label for="reg-phone">手机号</label>
            <input id="reg-phone" v-model="form.phone" type="text" placeholder="手机号" />
          </div>
          <div class="form-item">
            <label for="reg-sex">性别</label>
            <select id="reg-sex" v-model="form.sex">
              <option value="">请选择</option>
              <option value="男">男</option>
              <option value="女">女</option>
            </select>
          </div>
        </div>

        <div class="form-item">
          <label for="reg-idnumber">身份证号</label>
          <input id="reg-idnumber" v-model="form.idNumber" type="text" placeholder="身份证号" />
        </div>

        <p v-if="successMsg" class="success-msg">{{ successMsg }}</p>
        <p v-if="errorMsg" class="error-msg">{{ errorMsg }}</p>

        <button type="submit" class="register-btn" :disabled="registering">
          {{ registering ? '创建中...' : '创建管理员' }}
        </button>
      </form>
    </div>
  </main>

  <!-- ==================== 页脚 ==================== -->
  <footer class="page-footer">
    <p>校园非遗与文创传承人平台 © 2026 码上非遗</p>
  </footer>
</template>

<style scoped>
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
}
.admin-tag {
  font-size: 12px;
  background: rgba(255,255,255,0.2);
  padding: 3px 10px;
  border-radius: 10px;
  letter-spacing: 1px;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.user-name {
  font-size: 14px;
}
.role-badge {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  background: #ffd700;
  color: #8b4513;
  font-weight: bold;
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
.back-home-link {
  color: #fff;
  cursor: pointer;
  font-size: 13px;
  text-decoration: underline;
}

/* ==================== 主区域 ==================== */
.admin-main {
  min-height: calc(100vh - 64px - 72px);
  background: #fcf4e8;
  padding: 32px 5%;
  display: flex;
  gap: 28px;
  flex-wrap: wrap;
  align-items: flex-start;
}

/* ---- 欢迎卡片 ---- */
.welcome-card {
  flex: 1 1 320px;
  max-width: 420px;
  padding: 32px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 14px rgba(196, 30, 58, 0.08);
}
.welcome-card h2 {
  margin: 0 0 6px 0;
  font-size: 22px;
  color: #c41e3a;
}
.welcome-card > p {
  margin: 0 0 24px 0;
  font-size: 13px;
  color: #999;
}
.info-row {
  padding: 12px 0;
  border-bottom: 1px solid #fef0f2;
  font-size: 14px;
  color: #555;
}
.info-row:last-child {
  border-bottom: none;
}
.info-label {
  color: #999;
  margin-right: 8px;
}

/* ---- 待审核卡片 ---- */
.pending-card {
  flex: 1 1 600px;
  max-width: 900px;
  padding: 32px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 14px rgba(196, 30, 58, 0.08);
}
.pending-card h2 {
  margin: 0 0 6px 0;
  font-size: 22px;
  color: #c41e3a;
}
.pending-card > p {
  margin: 0 0 20px 0;
  font-size: 13px;
  color: #999;
}
.pending-status {
  text-align: center;
  padding: 24px;
  color: #888;
  font-size: 14px;
}
.pending-status.error {
  color: #e74c3c;
}
.retry-btn {
  margin-left: 10px;
  padding: 4px 12px;
  font-size: 12px;
  color: #c41e3a;
  background: #fff;
  border: 1px solid #c41e3a;
  border-radius: 4px;
  cursor: pointer;
}
.retry-btn:hover {
  background: #fef0f2;
}
.pending-empty {
  text-align: center;
  padding: 40px;
  color: #bbb;
  font-size: 14px;
}
.empty-icon {
  width: 48px;
  height: 48px;
  margin: 0 auto 12px;
  color: #ddd;
}
.empty-icon svg {
  width: 100%;
  height: 100%;
}
.pending-table-wrap {
  overflow-x: auto;
}
.pending-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}
.pending-table th,
.pending-table td {
  padding: 12px 10px;
  text-align: left;
  border-bottom: 1px solid #fef0f2;
  white-space: nowrap;
}
.pending-table th {
  color: #999;
  font-weight: 600;
  font-size: 12px;
}
.pending-table td {
  color: #444;
}
.pending-table tbody tr:hover {
  background: #fffbfb;
}
.action-cell {
  display: flex;
  gap: 8px;
}
.approve-btn,
.reject-btn {
  padding: 5px 16px;
  font-size: 12px;
  border-radius: 4px;
  cursor: pointer;
  border: none;
  transition: all 0.2s;
  font-weight: 500;
}
.approve-btn {
  background: #27ae60;
  color: #fff;
}
.approve-btn:hover:not(:disabled) {
  background: #219a52;
}
.reject-btn {
  background: #e74c3c;
  color: #fff;
}
.reject-btn:hover:not(:disabled) {
  background: #c0392b;
}
.approve-btn:disabled,
.reject-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* ---- 注册卡片 ---- */
.register-card {
  flex: 1 1 400px;
  max-width: 500px;
  padding: 32px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 14px rgba(196, 30, 58, 0.08);
}
.register-card h2 {
  margin: 0 0 6px 0;
  font-size: 22px;
  color: #c41e3a;
}
.register-card > p {
  margin: 0 0 24px 0;
  font-size: 13px;
  color: #999;
}
.register-form {
  display: flex;
  flex-direction: column;
}
.form-row {
  display: flex;
  gap: 16px;
}
.form-row .form-item {
  flex: 1;
}
.form-item {
  margin-bottom: 18px;
}
.form-item label {
  display: block;
  font-size: 13px;
  color: #555;
  margin-bottom: 4px;
}
.form-item input,
.form-item select {
  width: 100%;
  height: 42px;
  padding: 0 12px;
  border: 1px solid #e8647a;
  border-radius: 6px;
  font-size: 14px;
  color: #333;
  box-sizing: border-box;
  transition: border-color 0.2s;
  background: #fff;
}
.form-item input:focus,
.form-item select:focus {
  outline: none;
  border-color: #c41e3a;
  box-shadow: 0 0 0 3px rgba(196, 30, 58, 0.1);
}
.form-item select {
  cursor: pointer;
  appearance: auto;
}
.success-msg {
  color: #27ae60;
  font-size: 13px;
  margin: 0 0 14px 0;
  text-align: center;
}
.error-msg {
  color: #c41e3a;
  font-size: 13px;
  margin: 0 0 14px 0;
  text-align: center;
}
.register-btn {
  height: 44px;
  background: #c41e3a;
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 15px;
  font-weight: bold;
  cursor: pointer;
  transition: 0.2s;
}
.register-btn:hover:not(:disabled) {
  background: #a8162b;
}
.register-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* ==================== 页脚 ==================== */
.page-footer {
  background-color: #c41e3a;
  color: #fff;
  text-align: center;
  padding: 24px 5%;
}

/* ==================== 响应式 ==================== */
@media (max-width: 768px) {
  .admin-main {
    padding: 20px;
  }
  .form-row {
    flex-direction: column;
    gap: 0;
  }
  .page-header {
    padding: 0 16px;
  }
  .header-left {
    gap: 8px;
  }
  .logo {
    font-size: 16px;
  }
  .back-home-link {
    display: none;
  }
}
</style>
