<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'
import { adminCreate } from '@/api/auth'

const authStore = useAuthStore()
const router = useRouter()

// ---------- 注册表单 ----------
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
    // 清空表单
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

function handleLogout() {
  authStore.logout()
  router.push('/login')
}
</script>

<template>
  <div class="home-container">
    <header class="home-header">
      <h1>Code1st</h1>
      <div class="header-right">
        <span class="user-name">{{ authStore.userInfo?.name }}</span>
        <button class="logout-btn" @click="handleLogout">退出登录</button>
      </div>
    </header>

    <main class="home-main">
      <div class="welcome-card">
        <h2>欢迎回来，{{ authStore.userInfo?.name }}！</h2>
        <p>你已成功登录系统。</p>
        <div class="info-row">
          <span class="info-label">用户名：</span>
          <span>{{ authStore.userInfo?.userName }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">用户 ID：</span>
          <span>{{ authStore.userInfo?.id }}</span>
        </div>
      </div>

      <div class="register-card">
        <h2>注册新管理员</h2>
        <p>新管理员的默认密码为 <strong>123456</strong>，请提醒其尽快修改。</p>

        <form class="register-form" @submit.prevent="handleRegister">
          <div class="form-row">
            <div class="form-item">
              <label for="reg-username">用户名 *</label>
              <input
                id="reg-username"
                v-model="form.username"
                type="text"
                placeholder="登录用户名"
              />
            </div>
            <div class="form-item">
              <label for="reg-name">姓名 *</label>
              <input
                id="reg-name"
                v-model="form.name"
                type="text"
                placeholder="真实姓名"
              />
            </div>
          </div>

          <div class="form-row">
            <div class="form-item">
              <label for="reg-phone">手机号</label>
              <input
                id="reg-phone"
                v-model="form.phone"
                type="text"
                placeholder="手机号"
              />
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
            <input
              id="reg-idnumber"
              v-model="form.idNumber"
              type="text"
              placeholder="身份证号"
            />
          </div>

          <p v-if="successMsg" class="success-msg">{{ successMsg }}</p>
          <p v-if="errorMsg" class="error-msg">{{ errorMsg }}</p>

          <button type="submit" class="register-btn" :disabled="registering">
            {{ registering ? '创建中...' : '创建管理员' }}
          </button>
        </form>
      </div>
    </main>
  </div>
</template>

<style scoped>
.home-container {
  min-height: 100vh;
  background: #f5f6fa;
}

.home-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 32px;
  height: 60px;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.home-header h1 {
  font-size: 18px;
  color: #333;
  margin: 0;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-name {
  font-size: 14px;
  color: #555;
}

.logout-btn {
  padding: 6px 16px;
  font-size: 13px;
  color: #e74c3c;
  background: #fff;
  border: 1px solid #e74c3c;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
}

.logout-btn:hover {
  color: #fff;
  background: #e74c3c;
}

.home-main {
  padding: 32px;
  display: flex;
  gap: 32px;
  flex-wrap: wrap;
  align-items: flex-start;
}

/* ---- 欢迎卡片 ---- */
.welcome-card {
  flex: 1 1 360px;
  max-width: 480px;
  padding: 32px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.welcome-card h2 {
  margin: 0 0 8px 0;
  font-size: 20px;
  color: #333;
}

.welcome-card > p {
  margin: 0 0 24px 0;
  font-size: 14px;
  color: #999;
}

.info-row {
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;
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

/* ---- 注册卡片 ---- */
.register-card {
  flex: 1 1 400px;
  max-width: 560px;
  padding: 32px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.register-card h2 {
  margin: 0 0 8px 0;
  font-size: 20px;
  color: #333;
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
  height: 40px;
  padding: 0 12px;
  border: 1px solid #ddd;
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
  border-color: #667eea;
}

.success-msg {
  color: #27ae60;
  font-size: 13px;
  margin: 0 0 14px 0;
  text-align: center;
}

.error-msg {
  color: #e74c3c;
  font-size: 13px;
  margin: 0 0 14px 0;
  text-align: center;
}

.register-btn {
  height: 42px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 15px;
  cursor: pointer;
  transition: opacity 0.2s;
}

.register-btn:hover:not(:disabled) {
  opacity: 0.9;
}

.register-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
