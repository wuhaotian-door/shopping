<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMyProfile, updateProfile, sendChangePasswordCode, changePassword } from '@/api/user'
import type { UserProfileVO, UserProfileDTO } from '@/types/api'

const router = useRouter()

onMounted(() => {
  document.title = '个人中心 — 码上非遗'
  loadProfile()
})

const profile = ref<UserProfileVO>({
  id: 0,
  name: '',
  username: '',
  phone: '',
  email: '',
  sex: '',
})

// ==================== 编辑模式 ====================
const editMode = ref(false)
const form = ref<UserProfileDTO>({
  username: '',
  name: '',
  phone: '',
  email: '',
  sex: '',
})
const saveMsg = ref('')

async function loadProfile() {
  try {
    const res = await getMyProfile()
    if (res.data) {
      profile.value = res.data
    }
  } catch { /* ignore */ }
}

function enterEdit() {
  form.value = {
    username: profile.value.username || '',
    name: profile.value.name || '',
    phone: profile.value.phone || '',
    email: profile.value.email || '',
    sex: profile.value.sex || '',
  }
  editMode.value = true
  saveMsg.value = ''
}

async function handleSave() {
  try {
    await updateProfile(form.value)
    await loadProfile()
    editMode.value = false
    saveMsg.value = '保存成功！'
    setTimeout(() => { saveMsg.value = '' }, 3000)
  } catch (e: unknown) {
    saveMsg.value = e instanceof Error ? e.message : '保存失败'
  }
}

function cancelEdit() {
  editMode.value = false
  saveMsg.value = ''
}

// ==================== 修改密码 ====================
const showPasswordSection = ref(false)
const pwStep = ref(1) // 1=发送验证码, 2=输入验证码和新密码
const pwLoading = ref(false)
const pwErrorMsg = ref('')
const pwSuccessMsg = ref('')
const pwCountdown = ref(0)
const pwForm = ref({
  code: '',
  newPassword: '',
  confirmPassword: '',
})

let pwTimer: ReturnType<typeof setInterval> | null = null

function openPasswordChange() {
  showPasswordSection.value = true
  pwStep.value = 1
  pwErrorMsg.value = ''
  pwSuccessMsg.value = ''
  pwForm.value = { code: '', newPassword: '', confirmPassword: '' }
}

function closePasswordChange() {
  showPasswordSection.value = false
  if (pwTimer) { clearInterval(pwTimer); pwTimer = null }
  pwCountdown.value = 0
}

function startPwCountdown() {
  pwCountdown.value = 60
  pwTimer = setInterval(() => {
    pwCountdown.value--
    if (pwCountdown.value <= 0 && pwTimer) {
      clearInterval(pwTimer)
      pwTimer = null
    }
  }, 1000)
}

async function handleSendCode() {
  pwLoading.value = true
  pwErrorMsg.value = ''
  pwSuccessMsg.value = ''
  try {
    const res = await sendChangePasswordCode()
    pwSuccessMsg.value = res.data || '验证码已发送至注册邮箱'
    pwStep.value = 2
    startPwCountdown()
  } catch (e: unknown) {
    pwErrorMsg.value = e instanceof Error ? e.message : '验证码发送失败'
  } finally {
    pwLoading.value = false
  }
}

async function handleChangePassword() {
  if (!pwForm.value.code) {
    pwErrorMsg.value = '请输入验证码'
    return
  }
  if (!pwForm.value.newPassword) {
    pwErrorMsg.value = '请输入新密码'
    return
  }
  if (pwForm.value.newPassword.length < 6) {
    pwErrorMsg.value = '密码长度不能少于6位'
    return
  }
  if (pwForm.value.newPassword !== pwForm.value.confirmPassword) {
    pwErrorMsg.value = '两次输入的密码不一致'
    return
  }
  pwLoading.value = true
  pwErrorMsg.value = ''
  pwSuccessMsg.value = ''
  try {
    const res = await changePassword({
      code: pwForm.value.code,
      newPassword: pwForm.value.newPassword,
    })
    pwSuccessMsg.value = res.data || '密码修改成功'
    setTimeout(() => {
      closePasswordChange()
    }, 1500)
  } catch (e: unknown) {
    pwErrorMsg.value = e instanceof Error ? e.message : '密码修改失败'
  } finally {
    pwLoading.value = false
  }
}

async function resendCode() {
  if (pwCountdown.value > 0) return
  await handleSendCode()
}
</script>

<template>
  <div class="profile-page">
    <!-- ==================== 顶部导航 ==================== -->
    <header class="page-header">
      <div class="header-left">
        <div class="logo" @click="router.push('/')">码上非遗</div>
        <span class="workspace-tag">个人中心</span>
      </div>
      <nav class="header-right">
        <button class="nav-btn" @click="router.push('/')">返回首页</button>
      </nav>
    </header>

    <!-- ==================== 主体内容 ==================== -->
    <main class="profile-main">
      <div class="profile-container">
        <!-- ========== 个人信息卡片 ========== -->
        <div class="section-header">
          <h2>个人信息</h2>
          <button v-if="!editMode" class="btn-outline" @click="enterEdit">编辑信息</button>
        </div>

        <div class="profile-card">
          <!-- 头像区域 -->
          <div class="profile-avatar-section">
            <div class="profile-avatar-lg">
              <span class="avatar-initial">{{ (profile.name || '用')[0] }}</span>
            </div>
            <div class="profile-name-role">
              <h3 v-if="!editMode">{{ profile.name || '未设置姓名' }}</h3>
              <p class="profile-username">@{{ profile.username || '' }}</p>
              <span class="role-badge user-badge">普通用户</span>
            </div>
          </div>

          <!-- 信息网格 -->
          <div class="profile-detail-grid">
            <div class="detail-item">
              <label>用户名</label>
              <span v-if="!editMode">{{ profile.username || '-' }}</span>
              <input v-else v-model="form.username" type="text" class="form-input" placeholder="请输入用户名" />
            </div>
            <div class="detail-item">
              <label>姓名</label>
              <span v-if="!editMode">{{ profile.name || '-' }}</span>
              <input v-else v-model="form.name" type="text" class="form-input" placeholder="请输入姓名" />
            </div>
            <div class="detail-item">
              <label>手机号</label>
              <span v-if="!editMode">{{ profile.phone || '-' }}</span>
              <input v-else v-model="form.phone" type="text" class="form-input" placeholder="请输入手机号" />
            </div>
            <div class="detail-item">
              <label>邮箱</label>
              <span v-if="!editMode">{{ profile.email || '-' }}</span>
              <input v-else v-model="form.email" type="text" class="form-input" placeholder="请输入邮箱" />
            </div>
            <div class="detail-item">
              <label>性别</label>
              <span v-if="!editMode">{{ profile.sex || '-' }}</span>
              <select v-else v-model="form.sex" class="form-input">
                <option value="">请选择</option>
                <option value="男">男</option>
                <option value="女">女</option>
              </select>
            </div>
          </div>

          <!-- 编辑操作按钮 -->
          <div v-if="editMode" class="profile-edit-actions">
            <button class="btn-primary" @click="handleSave">保存修改</button>
            <button class="btn-outline" @click="cancelEdit">取消</button>
            <span v-if="saveMsg" class="save-msg" :class="{ error: saveMsg.includes('失败') || saveMsg.includes('已存在') }">{{ saveMsg }}</span>
          </div>
        </div>

        <!-- ========== 账号安全卡片 ========== -->
        <div class="section-header" style="margin-top: 40px">
          <h2>账号安全</h2>
        </div>

        <div class="profile-card">
          <div class="security-item">
            <div class="security-info">
              <span class="security-label">登录密码</span>
              <span class="security-desc">定期更换密码可以保护账号安全</span>
            </div>
            <button class="btn-outline" @click="openPasswordChange">
              {{ showPasswordSection ? '取消修改' : '修改密码' }}
            </button>
          </div>

          <!-- 修改密码面板 -->
          <div v-if="showPasswordSection" class="password-panel">
            <div class="divider"></div>

            <!-- 步骤指示器 -->
            <div class="step-indicator">
              <div class="step" :class="{ active: pwStep >= 1, done: pwStep > 1 }">
                <span class="step-num">1</span>
                <span class="step-label">验证身份</span>
              </div>
              <div class="step-line" :class="{ active: pwStep > 1 }"></div>
              <div class="step" :class="{ active: pwStep >= 2 }">
                <span class="step-num">2</span>
                <span class="step-label">设置新密码</span>
              </div>
            </div>

            <!-- 第一步：获取验证码 -->
            <div v-if="pwStep === 1" class="pw-form">
              <div class="tip-box">
                <p>验证码将发送至您绑定的邮箱 <strong>{{ profile.email || '3326353413@qq.com' }}</strong></p>
              </div>
              <p v-if="pwErrorMsg" class="error-msg">{{ pwErrorMsg }}</p>
              <button class="btn-primary pw-full-btn" :disabled="pwLoading" @click="handleSendCode">
                {{ pwLoading ? '发送中...' : '发送验证码' }}
              </button>
            </div>

            <!-- 第二步：输入验证码和新密码 -->
            <div v-else class="pw-form">
              <div class="form-item">
                <label>邮箱验证码</label>
                <div class="code-row">
                  <input
                    v-model="pwForm.code"
                    type="text"
                    placeholder="请输入6位验证码"
                    maxlength="6"
                    class="form-input"
                  />
                  <button
                    type="button"
                    class="resend-btn"
                    :disabled="pwCountdown > 0"
                    @click="resendCode"
                  >
                    {{ pwCountdown > 0 ? `${pwCountdown}s` : '重新发送' }}
                  </button>
                </div>
              </div>

              <div class="form-item">
                <label>新密码</label>
                <input
                  v-model="pwForm.newPassword"
                  type="password"
                  placeholder="请输入新密码（至少6位）"
                  class="form-input"
                  autocomplete="new-password"
                />
              </div>

              <div class="form-item">
                <label>确认密码</label>
                <input
                  v-model="pwForm.confirmPassword"
                  type="password"
                  placeholder="请再次输入新密码"
                  class="form-input"
                  autocomplete="new-password"
                />
              </div>

              <p v-if="pwErrorMsg" class="error-msg">{{ pwErrorMsg }}</p>
              <p v-if="pwSuccessMsg" class="success-msg">{{ pwSuccessMsg }}</p>

              <button class="btn-primary pw-full-btn" :disabled="pwLoading" @click="handleChangePassword">
                {{ pwLoading ? '修改中...' : '确认修改' }}
              </button>

              <button class="back-link-btn" @click="pwStep = 1; pwErrorMsg = ''; pwSuccessMsg = ''">
                返回上一步
              </button>
            </div>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<style scoped>
.profile-page {
  min-height: 100vh;
  background: #f8f9fa;
}

/* ---- 顶部导航 ---- */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 40px;
  height: 64px;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  position: sticky;
  top: 0;
  z-index: 100;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.logo {
  font-size: 20px;
  font-weight: 800;
  color: #c41e3a;
  cursor: pointer;
  user-select: none;
}
.workspace-tag {
  font-size: 13px;
  color: #888;
  padding: 2px 12px;
  background: #f5f5f5;
  border-radius: 12px;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.nav-btn {
  padding: 8px 20px;
  font-size: 13px;
  color: #666;
  background: #f5f5f5;
  border: none;
  border-radius: 6px;
  cursor: pointer;
}
.nav-btn:hover {
  background: #e9e9e9;
}

/* ---- 主体 ---- */
.profile-main {
  max-width: 800px;
  margin: 0 auto;
  padding: 40px 20px;
}
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

/* ---- 卡片 ---- */
.profile-card {
  background: #fff;
  border-radius: 12px;
  padding: 32px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.06);
}

/* ---- 头像区域 ---- */
.profile-avatar-section {
  display: flex;
  align-items: center;
  gap: 24px;
  margin-bottom: 28px;
  padding-bottom: 24px;
  border-bottom: 1px solid #f0f0f0;
}
.profile-avatar-lg {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  background: linear-gradient(135deg, #e8647a, #c41e3a);
  border: 4px solid #fff5f7;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}
.avatar-initial {
  font-size: 40px;
  color: #fff;
  font-weight: 700;
  user-select: none;
}
.profile-name-role h3 {
  font-size: 24px;
  color: #222;
  margin-bottom: 4px;
}
.profile-username {
  font-size: 14px;
  color: #999;
  margin-bottom: 8px;
}
.role-badge {
  display: inline-block;
  padding: 3px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}
.user-badge {
  background: #e8f4fd;
  color: #1890ff;
}

/* ---- 信息网格 ---- */
.profile-detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}
.detail-item label {
  display: block;
  font-size: 13px;
  color: #999;
  margin-bottom: 6px;
}
.detail-item span {
  font-size: 15px;
  color: #333;
}

/* ---- 编辑操作 ---- */
.profile-edit-actions {
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  gap: 12px;
}
.save-msg {
  font-size: 13px;
  color: #28a745;
}
.save-msg.error {
  color: #dc3545;
}

/* ---- 账号安全 ---- */
.security-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.security-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.security-label {
  font-size: 15px;
  color: #333;
  font-weight: 600;
}
.security-desc {
  font-size: 13px;
  color: #999;
}

/* ---- 修改密码面板 ---- */
.password-panel {
  margin-top: 0;
}
.divider {
  border-top: 1px solid #f0f0f0;
  margin: 20px 0;
}

/* 步骤指示器 */
.step-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24px;
}
.step {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}
.step-num {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #e0e0e0;
  color: #999;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: bold;
  transition: 0.3s;
}
.step.active .step-num {
  background: #c41e3a;
  color: #fff;
}
.step.done .step-num {
  background: #d4edda;
  color: #155724;
}
.step-label {
  font-size: 12px;
  color: #999;
}
.step.active .step-label {
  color: #c41e3a;
  font-weight: 600;
}
.step-line {
  width: 60px;
  height: 2px;
  background: #e0e0e0;
  margin: 0 12px;
  margin-bottom: 22px;
  transition: 0.3s;
}
.step-line.active {
  background: #d4edda;
}

/* 密码表单 */
.pw-form {
  max-width: 420px;
  margin: 0 auto;
}
.pw-full-btn {
  width: 100%;
  margin-top: 8px;
}

.tip-box {
  background: #fff5f7;
  border-radius: 8px;
  padding: 14px 16px;
  font-size: 14px;
  color: #666;
  margin-bottom: 20px;
  text-align: center;
}
.tip-box strong {
  color: #c41e3a;
}

.form-item {
  margin-bottom: 18px;
}
.form-item label {
  display: block;
  font-size: 14px;
  color: #444;
  margin-bottom: 6px;
}
.form-item .form-input {
  width: 100%;
}

.code-row {
  display: flex;
  gap: 10px;
}
.code-row .form-input {
  flex: 1;
}

.resend-btn {
  white-space: nowrap;
  padding: 0 16px;
  height: 42px;
  border: 1px solid #c41e3a;
  border-radius: 8px;
  background: #fff;
  color: #c41e3a;
  font-size: 13px;
  cursor: pointer;
  transition: 0.2s;
}
.resend-btn:hover:not(:disabled) {
  background: #fff5f7;
}
.resend-btn:disabled {
  border-color: #ccc;
  color: #999;
  cursor: not-allowed;
}

.back-link-btn {
  width: 100%;
  height: 40px;
  background: transparent;
  color: #999;
  border: none;
  font-size: 14px;
  cursor: pointer;
  margin-top: 8px;
}
.back-link-btn:hover {
  color: #c41e3a;
}

.error-msg {
  color: #c41e3a;
  font-size: 13px;
  margin: 0 0 14px 0;
  text-align: center;
}
.success-msg {
  color: #155724;
  font-size: 13px;
  margin: 0 0 14px 0;
  text-align: center;
}

/* ---- 按钮 ---- */
.btn-primary {
  padding: 10px 24px;
  font-size: 14px;
  color: #fff;
  background: #c41e3a;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-weight: bold;
}
.btn-primary:hover {
  background: #a8162b;
}
.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.btn-outline {
  padding: 8px 20px;
  font-size: 13px;
  color: #c41e3a;
  background: #fff;
  border: 1px solid #c41e3a;
  border-radius: 6px;
  cursor: pointer;
}
.btn-outline:hover {
  background: #fff5f7;
}

/* ---- 表单 ---- */
.form-input {
  width: 100%;
  height: 42px;
  padding: 0 14px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  box-sizing: border-box;
  outline: none;
  background: #fafafa;
}
.form-input:focus {
  border-color: #c41e3a;
  box-shadow: 0 0 0 3px rgba(196, 30, 58, 0.08);
  background: #fff;
}
select.form-input {
  cursor: pointer;
}

/* ---- 响应式 ---- */
@media (max-width: 768px) {
  .page-header {
    padding: 0 16px;
  }
  .profile-main {
    padding: 24px 16px;
  }
  .profile-card {
    padding: 20px;
  }
  .profile-detail-grid {
    grid-template-columns: 1fr;
  }
  .profile-avatar-section {
    flex-direction: column;
    text-align: center;
  }
  .security-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
}
</style>
