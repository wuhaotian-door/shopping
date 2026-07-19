<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { sendResetCode, resetPassword } from '@/api/auth'

onMounted(() => {
  document.title = '找回密码 — 码上非遗'
})

const router = useRouter()

const step = ref(1)
const errorMsg = ref('')
const successMsg = ref('')
const loading = ref(false)
const countdown = ref(0)

const form = ref({
  username: '',
  code: '',
  newPassword: '',
  confirmPassword: '',
})

let timer: ReturnType<typeof setInterval> | null = null

function startCountdown() {
  countdown.value = 60
  timer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0 && timer) {
      clearInterval(timer)
      timer = null
    }
  }, 1000)
}

async function handleSendCode() {
  if (!form.value.username) {
    errorMsg.value = '请输入用户名'
    return
  }
  loading.value = true
  errorMsg.value = ''
  successMsg.value = ''
  try {
    const res = await sendResetCode({ username: form.value.username })
    successMsg.value = res.data || '验证码已发送至注册邮箱'
    step.value = 2
    startCountdown()
  } catch (e: unknown) {
    errorMsg.value = e instanceof Error ? e.message : '验证码发送失败'
  } finally {
    loading.value = false
  }
}

async function handleResetPassword() {
  if (!form.value.code) {
    errorMsg.value = '请输入验证码'
    return
  }
  if (!form.value.newPassword) {
    errorMsg.value = '请输入新密码'
    return
  }
  if (form.value.newPassword.length < 6) {
    errorMsg.value = '密码长度不能少于6位'
    return
  }
  if (form.value.newPassword !== form.value.confirmPassword) {
    errorMsg.value = '两次输入的密码不一致'
    return
  }
  loading.value = true
  errorMsg.value = ''
  successMsg.value = ''
  try {
    const res = await resetPassword({
      username: form.value.username,
      code: form.value.code,
      newPassword: form.value.newPassword,
    })
    successMsg.value = res.data || '密码重置成功'
    setTimeout(() => {
      router.push('/login')
    }, 1500)
  } catch (e: unknown) {
    errorMsg.value = e instanceof Error ? e.message : '密码重置失败'
  } finally {
    loading.value = false
  }
}

async function resendCode() {
  if (countdown.value > 0) return
  await handleSendCode()
}
</script>

<template>
  <header class="page-header">
    <div class="logo">码上非遗</div>
    <a @click="router.push('/')" class="back-link">返回首页</a>
  </header>

  <div class="page-wrap">
    <div class="card-box">
      <div class="reset-card">
        <h1 class="reset-title">找回密码</h1>
        <div class="title-line"></div>

        <!-- 步骤指示器 -->
        <div class="step-indicator">
          <div class="step" :class="{ active: step >= 1, done: step > 1 }">
            <span class="step-num">1</span>
            <span class="step-label">验证身份</span>
          </div>
          <div class="step-line" :class="{ active: step > 1 }"></div>
          <div class="step" :class="{ active: step >= 2 }">
            <span class="step-num">2</span>
            <span class="step-label">重置密码</span>
          </div>
        </div>

        <!-- 第一步：输入用户名 -->
        <form v-if="step === 1" class="reset-form" @submit.prevent="handleSendCode">
          <div class="form-item">
            <label for="username">用户名</label>
            <input
              id="username"
              v-model="form.username"
              type="text"
              placeholder="请输入注册时的用户名"
              autocomplete="username"
            />
          </div>

          <div class="tip-box">
            验证码将发送至您注册时绑定的邮箱
          </div>

          <p v-if="errorMsg" class="error-msg">{{ errorMsg }}</p>

          <button type="submit" class="reset-btn" :disabled="loading">
            {{ loading ? '发送中...' : '发送验证码' }}
          </button>
        </form>

        <!-- 第二步：输入验证码和新密码 -->
        <form v-else class="reset-form" @submit.prevent="handleResetPassword">
          <div class="form-item">
            <label for="code">邮箱验证码</label>
            <div class="code-row">
              <input
                id="code"
                v-model="form.code"
                type="text"
                placeholder="请输入6位验证码"
                maxlength="6"
              />
              <button
                type="button"
                class="resend-btn"
                :disabled="countdown > 0"
                @click="resendCode"
              >
                {{ countdown > 0 ? `${countdown}s` : '重新发送' }}
              </button>
            </div>
          </div>

          <div class="form-item">
            <label for="newPassword">新密码</label>
            <input
              id="newPassword"
              v-model="form.newPassword"
              type="password"
              placeholder="请输入新密码（至少6位）"
              autocomplete="new-password"
            />
          </div>

          <div class="form-item">
            <label for="confirmPassword">确认密码</label>
            <input
              id="confirmPassword"
              v-model="form.confirmPassword"
              type="password"
              placeholder="请再次输入新密码"
              autocomplete="new-password"
            />
          </div>

          <p v-if="errorMsg" class="error-msg">{{ errorMsg }}</p>
          <p v-if="successMsg" class="success-msg">{{ successMsg }}</p>

          <button type="submit" class="reset-btn" :disabled="loading">
            {{ loading ? '重置中...' : '确认重置' }}
          </button>

          <button type="button" class="back-btn" @click="step = 1; errorMsg = ''">
            返回上一步
          </button>
        </form>

        <div class="switch-tip">
          想起密码了？
          <span @click="router.push('/login')" class="login-text">返回登录</span>
        </div>
      </div>
    </div>
  </div>

  <footer class="page-footer">
    <p>校园非遗与文创传承人平台 © 2026 码上非遗</p>
  </footer>
</template>

<style scoped>
.page-header {
  background-color: #c41e3a;
  color: #fff;
  padding: 0 5%;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.logo {
  font-size: 20px;
  font-weight: bold;
}
.back-link {
  color: #fff;
  cursor: pointer;
}
.back-link:hover {
  text-decoration: underline;
}

.page-wrap {
  min-height: calc(100vh - 64px - 120px);
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fcf4e8;
  padding: 40px 20px;
}

.card-box {
  width: 100%;
  max-width: 460px;
}

.reset-card {
  background: #fff;
  border-radius: 14px;
  padding: 40px 36px;
  box-shadow: 0 6px 22px rgba(196, 30, 58, 0.12);
}

.reset-title {
  text-align: center;
  font-size: 26px;
  color: #c41e3a;
  margin-bottom: 10px;
}
.title-line {
  width: 50px;
  height: 3px;
  background: #c41e3a;
  margin: 0 auto 28px;
}

/* 步骤指示器 */
.step-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 32px;
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

/* 表单 */
.reset-form {
  display: flex;
  flex-direction: column;
}
.form-item {
  margin-bottom: 20px;
}
.form-item label {
  display: block;
  font-size: 15px;
  color: #444;
  margin-bottom: 8px;
}
.form-item input {
  width: 100%;
  height: 44px;
  padding: 0 14px;
  border: 1px solid #e8647a;
  border-radius: 8px;
  font-size: 15px;
  color: #333;
  box-sizing: border-box;
  transition: 0.25s;
  outline: none;
}
.form-item input:focus {
  border-color: #c41e3a;
  box-shadow: 0 0 0 3px rgba(196, 30, 58, 0.1);
}

.code-row {
  display: flex;
  gap: 10px;
}
.code-row input {
  flex: 1;
}
.resend-btn {
  white-space: nowrap;
  padding: 0 16px;
  height: 44px;
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

.tip-box {
  background: #fff5f7;
  border-radius: 8px;
  padding: 12px 16px;
  font-size: 13px;
  color: #888;
  margin-bottom: 20px;
  text-align: center;
}

.error-msg {
  color: #c41e3a;
  font-size: 13px;
  margin: 0 0 16px 0;
  text-align: center;
}
.success-msg {
  color: #155724;
  font-size: 13px;
  margin: 0 0 16px 0;
  text-align: center;
}

.reset-btn {
  width: 100%;
  height: 46px;
  background: #c41e3a;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: bold;
  cursor: pointer;
  transition: 0.25s;
}
.reset-btn:hover:not(:disabled) {
  background: #a8162b;
}
.reset-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.back-btn {
  width: 100%;
  height: 42px;
  background: transparent;
  color: #999;
  border: none;
  font-size: 14px;
  cursor: pointer;
  margin-top: 10px;
}
.back-btn:hover {
  color: #c41e3a;
}

.switch-tip {
  text-align: center;
  margin-top: 24px;
  font-size: 14px;
  color: #666;
}
.login-text {
  color: #c41e3a;
  cursor: pointer;
}
.login-text:hover {
  text-decoration: underline;
}

.page-footer {
  background-color: #c41e3a;
  color: #fff;
  text-align: center;
  padding: 24px 5%;
}

@media (max-width: 576px) {
  .reset-card {
    padding: 30px 24px;
  }
}
</style>
