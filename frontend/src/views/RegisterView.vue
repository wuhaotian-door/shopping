<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { selfRegister, inheritorRegister } from '@/api/auth'
import type { RoleType } from '@/types/api'

onMounted(() => {
  document.title = "码上非遗——用户注册"
})

const router = useRouter()

const form = reactive({
  username: '',
  name: '',
  password: '',
  confirmPassword: '',
  phone: '',
  sex: '',
  idNumber: '',
  region: '',
  role: '' as RoleType | '',
})

const errorMsg = ref('')
const loading = ref(false)

const isInheritor = computed(() => form.role === 'INHERITOR')

function validate(): string | null {
  if (!form.username.trim()) return '请输入用户名'
  if (!form.name.trim()) return '请输入姓名'
  if (!form.password) return '请输入密码'
  if (form.password.length < 6) return '密码长度至少为 6 位'
  if (form.password !== form.confirmPassword) return '两次输入的密码不一致'
  if (!form.role) return '请选择注册类型'

  // 非遗传承人需要填写更多信息
  if (isInheritor.value) {
    if (!form.idNumber.trim()) return '请输入身份证号'
    if (!form.phone.trim()) return '请输入手机号'
    if (!form.region.trim()) return '请输入所属地区'
  }

  return null
}

async function handleRegister() {
  const err = validate()
  if (err) {
    errorMsg.value = err
    return
  }

  loading.value = true
  errorMsg.value = ''

  try {
    if (isInheritor.value) {
      // 非遗传承人注册（详细）
      await inheritorRegister({
        username: form.username.trim(),
        name: form.name.trim(),
        password: form.password,
        phone: form.phone.trim(),
        sex: form.sex || undefined,
        idNumber: form.idNumber.trim(),
        region: form.region.trim(),
      })
      router.push('/register-success?type=inheritor')
    } else {
      // 普通用户注册（简化）
      await selfRegister({
        username: form.username.trim(),
        name: form.name.trim(),
        password: form.password,
        phone: form.phone || undefined,
        sex: form.sex || undefined,
      })
      router.push('/register-success?type=user')
    }
  } catch (e: unknown) {
    errorMsg.value = e instanceof Error ? e.message : '注册失败，请重试'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <header class="page-header">
    <div class="logo">码上非遗</div>
    <a @click="$router.push('/')" class="back-link">返回首页</a>
  </header>

  <div class="page-wrap">
    <!-- 左侧国风灵动互动窗口 -->
    <div class="float-left-box">
      <div
          class="float-card"
          @mousemove="onMouseMove"
          @mouseenter="onMouseEnter"
          @mouseleave="onMouseLeave"
      >
        <!-- 背景流光 -->
        <div class="flow-light"></div>

        <!-- 国风纹样层 -->
        <div class="pattern-layer pattern-1"></div>
        <div class="pattern-layer pattern-2"></div>
        <div class="pattern-layer pattern-3"></div>

        <!-- 浮动装饰 -->
        <div class="float-deco deco1"></div>
        <div class="float-deco deco2"></div>
        <div class="float-deco deco3"></div>

        <!-- 鼠标跟随粒子 -->
        <div class="particle" v-for="i in 6" :key="i" :style="particleStyle(i)"></div>

        <div class="float-text-wrap">
          <h2>码上非遗</h2>
          <p class="slogan">感受非遗魅力</p>
          <p class="desc">汇聚剪纸、刺绣、陶艺、竹编传统技艺<br>连接校园与民间手工艺传承人</p>
        </div>
      </div>
    </div>

    <!-- 右侧注册表单 -->
    <div class="login-right-box">
      <div class="login-card" :style="{ maxWidth: isInheritor ? '520px' : '460px' }">
        <h1 class="login-title">
          {{ isInheritor ? '非遗传承人入驻' : '用户注册' }}
        </h1>
        <p class="login-subtitle" v-if="isInheritor">填写详细信息，展示您的非遗技艺</p>
        <div class="title-line"></div>

        <form class="login-form" @submit.prevent="handleRegister">
          <!-- 角色选择（放在表单最前面） -->
          <div class="form-item">
            <label class="role-label">注册类型 *</label>
            <div class="role-selector">
              <div
                  class="role-card"
                  :class="{ active: form.role === 'USER' }"
                  @click="form.role = 'USER'"
              >
                <div class="role-icon user-icon">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                    <circle cx="12" cy="7" r="4"/>
                  </svg>
                </div>
                <span class="role-name">普通用户注册</span>
                <span class="role-desc">浏览非遗文化内容</span>
              </div>
              <div
                  class="role-card"
                  :class="{ active: form.role === 'INHERITOR' }"
                  @click="form.role = 'INHERITOR'"
              >
                <div class="role-icon inheritor-icon">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <polygon points="12 2 22 8.5 22 15.5 12 22 2 15.5 2 8.5 12 2"/>
                    <line x1="12" y1="22" x2="12" y2="15.5"/>
                    <polyline points="22 8.5 12 15.5 2 8.5"/>
                  </svg>
                </div>
                <span class="role-name">非遗传承人入驻</span>
                <span class="role-desc">展示与传承技艺</span>
              </div>
            </div>
          </div>

          <!-- 基础信息（两种角色都显示） -->
          <div class="form-item">
            <label for="reg-username">用户名 *</label>
            <input
                id="reg-username"
                v-model="form.username"
                type="text"
                placeholder="登录账号"
                autocomplete="username"
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

          <div class="form-row">
            <div class="form-item form-half">
              <label for="reg-password">密码 *</label>
              <input
                  id="reg-password"
                  v-model="form.password"
                  type="password"
                  placeholder="至少 6 位密码"
                  autocomplete="new-password"
              />
            </div>

            <div class="form-item form-half">
              <label for="reg-confirm">确认密码 *</label>
              <input
                  id="reg-confirm"
                  v-model="form.confirmPassword"
                  type="password"
                  placeholder="再次输入密码"
                  autocomplete="new-password"
              />
            </div>
          </div>

          <!-- 非遗传承人专属字段 -->
          <template v-if="isInheritor">
            <div class="form-divider">
              <span>传承人信息</span>
            </div>

            <div class="form-item">
              <label for="reg-phone-inheritor">手机号 *</label>
              <input
                  id="reg-phone-inheritor"
                  v-model="form.phone"
                  type="text"
                  placeholder="请输入手机号"
              />
            </div>

            <div class="form-item">
              <label for="reg-idnumber">身份证号 *</label>
              <input
                  id="reg-idnumber"
                  v-model="form.idNumber"
                  type="text"
                  placeholder="请输入身份证号"
                  maxlength="18"
              />
            </div>

            <div class="form-item">
              <label for="reg-region">所属地区 *</label>
              <input
                  id="reg-region"
                  v-model="form.region"
                  type="text"
                  placeholder="如：江苏省苏州市"
              />
            </div>

            <div class="form-item">
              <label for="reg-sex-inheritor">性别</label>
              <select id="reg-sex-inheritor" v-model="form.sex">
                <option value="">请选择</option>
                <option value="男">男</option>
                <option value="女">女</option>
              </select>
            </div>
          </template>

          <!-- 普通用户可选字段（折叠显示） -->
          <template v-if="!isInheritor && form.role === 'USER'">
            <div class="form-item">
              <label for="reg-phone">手机号</label>
              <input
                  id="reg-phone"
                  v-model="form.phone"
                  type="text"
                  placeholder="选填"
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
          </template>

          <p v-if="errorMsg" class="error-msg">{{ errorMsg }}</p>

          <button type="submit" class="login-btn" :disabled="loading">
            {{ loading ? '注册中...' : (isInheritor ? '提交入驻申请' : '注 册') }}
          </button>
        </form>

        <div class="switch-tip">
          已有账号？
          <span @click="router.push('/login')" class="register-text">立即登录</span>
        </div>
      </div>
    </div>
  </div>

  <footer class="page-footer">
    <p>校园非遗与文创传承人平台 © 2026 码上非遗</p>
  </footer>
</template>

<script lang="ts">
export default {
  methods: {
    onMouseMove(e: MouseEvent) {
      const card = e.currentTarget as HTMLElement
      const rect = card.getBoundingClientRect()
      const x = e.clientX - rect.left - rect.width / 2
      const y = e.clientY - rect.top - rect.height / 2

      card.style.transform = `
        translateY(-6px)
        perspective(1000px)
        rotateY(${x * 0.008}deg)
        rotateX(${-y * 0.008}deg)
      `

      card.style.setProperty('--mouse-x', `${e.clientX - rect.left}px`)
      card.style.setProperty('--mouse-y', `${e.clientY - rect.top}px`)
    },
    onMouseEnter(e: MouseEvent) {
      const card = e.currentTarget as HTMLElement
      card.style.transition = 'transform 0.15s ease-out, box-shadow 0.2s ease'
      card.style.boxShadow = '0 12px 36px rgba(196, 30, 58, 0.35)'
    },
    onMouseLeave(e: MouseEvent) {
      const card = e.currentTarget as HTMLElement
      card.style.transition = 'transform 0.3s ease, box-shadow 0.3s ease'
      card.style.transform = 'translateY(0) perspective(1000px) rotateY(0) rotateX(0)'
      card.style.boxShadow = '0 8px 30px rgba(196, 30, 58, 0.2)'
    },
    particleStyle(index: number) {
      const size = 8 + index * 2
      return {
        width: `${size}px`,
        height: `${size}px`,
        left: `${15 + index * 15}%`,
        animationDelay: `${index * 0.4}s`,
      }
    }
  }
}
</script>

<style scoped>
/* ===== 复用的 LoginView 样式 ===== */
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
  display: grid;
  grid-template-columns: 1fr 1fr;
  background: #fcf4e8;
}

.float-left-box {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.float-card {
  width: 100%;
  max-width: 480px;
  height: 520px;
  border-radius: 18px;
  background: linear-gradient(145deg, #c41e3a, #e8647a);
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 30px rgba(196, 30, 58, 0.2);
  cursor: pointer;
  perspective: 1000px;
  transform-style: preserve-3d;
}

/* 鼠标位置变量 */
.float-card {
  --mouse-x: 50%;
  --mouse-y: 50%;
}

/* 流光效果 */
.flow-light {
  position: absolute;
  width: 120%;
  height: 120%;
  top: -10%;
  left: -10%;
  background: radial-gradient(
      circle at var(--mouse-x, 50%) var(--mouse-y, 50%),
      rgba(255, 255, 255, 0.24),
      transparent 40%
  );
  opacity: 0;
  transition: opacity 0.25s ease;
  pointer-events: none;
}

.float-card:hover .flow-light {
  opacity: 1;
}

/* 国风剪纸/窗花纹样 */
.pattern-layer {
  position: absolute;
  width: 180px;
  height: 180px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 12px;
  opacity: 0;
  transform: scale(0.8);
  transition: all 0.4s ease;
  pointer-events: none;
}

.pattern-1 {
  top: 30px;
  left: 30px;
  clip-path: polygon(50% 0%, 100% 50%, 50% 100%, 0% 50%);
}

.pattern-2 {
  bottom: 40px;
  right: 40px;
  width: 140px;
  height: 140px;
  clip-path: polygon(25% 0%, 75% 0%, 100% 50%, 75% 100%, 25% 100%, 0% 50%);
}

.pattern-3 {
  top: 50%;
  left: 50%;
  width: 100px;
  height: 100px;
  transform: translate(-50%, -50%) scale(0.8);
  clip-path: circle(50% at 50% 50%);
}

.float-card:hover .pattern-layer {
  opacity: 0.65;
  transform: scale(1);
}

.float-card:hover .pattern-1 {
  animation: floatMove 8s ease-in-out infinite;
}

.float-card:hover .pattern-2 {
  animation: floatMove 10s ease-in-out infinite reverse;
}

.float-card:hover .pattern-3 {
  animation: pulse 4s ease-in-out infinite;
}

/* 浮动装饰 */
.float-deco {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.12);
  animation: floatMove 6s ease-in-out infinite;
  pointer-events: none;
}
.deco1 {
  width: 180px;
  height: 180px;
  top: 40px;
  left: 40px;
  animation-delay: 0s;
}
.deco2 {
  width: 120px;
  height: 120px;
  bottom: 80px;
  right: 60px;
  animation-delay: 1.8s;
}
.deco3 {
  width: 70px;
  height: 70px;
  top: 160px;
  right: 120px;
  animation-delay: 3.5s;
}

/* 跟随粒子 */
.particle {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.35);
  animation: particleFloat 3s ease-in-out infinite;
  pointer-events: none;
}

@keyframes floatMove {
  0% {
    transform: translateY(0) scale(1);
  }
  50% {
    transform: translateY(-22px) scale(1.08);
  }
  100% {
    transform: translateY(0) scale(1);
  }
}

@keyframes pulse {
  0% {
    transform: translate(-50%, -50%) scale(0.8);
    opacity: 0.65;
  }
  50% {
    transform: translate(-50%, -50%) scale(1);
    opacity: 1;
  }
  100% {
    transform: translate(-50%, -50%) scale(0.8);
    opacity: 0.65;
  }
}

@keyframes particleFloat {
  0% {
    transform: translateY(0) translateX(0);
    opacity: 0.35;
  }
  50% {
    transform: translateY(-30px) translateX(8px);
    opacity: 0.7;
  }
  100% {
    transform: translateY(0) translateX(0);
    opacity: 0.35;
  }
}

.float-text-wrap {
  z-index: 2;
  text-align: center;
  color: #fff;
  padding: 0 30px;
}
.float-text-wrap h2 {
  font-size: 42px;
  margin-bottom: 10px;
  letter-spacing: 4px;
}
.slogan {
  font-size: 20px;
  margin-bottom: 24px;
  opacity: 0.9;
}
.desc {
  font-size: 15px;
  line-height: 2;
  opacity: 0.85;
}

.login-right-box {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
}
.login-card {
  width: 100%;
  max-width: 460px;
  padding: 40px 32px;
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 6px 22px rgba(196, 30, 58, 0.12);
}
.login-title {
  text-align: center;
  font-size: 26px;
  color: #c41e3a;
  margin-bottom: 4px;
}
.login-subtitle {
  text-align: center;
  font-size: 13px;
  color: #999;
  margin-bottom: 8px;
}
.title-line {
  width: 50px;
  height: 3px;
  background: #c41e3a;
  margin: 0 auto 32px;
}
.login-form {
  display: flex;
  flex-direction: column;
}
.form-item {
  margin-bottom: 22px;
}
.form-item label {
  display: block;
  font-size: 15px;
  color: #444;
  margin-bottom: 8px;
}
.form-item input,
.form-item select {
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
  background: #fff;
}
.form-item input:focus,
.form-item select:focus {
  border-color: #c41e3a;
  box-shadow: 0 0 0 3px rgba(196, 30, 58, 0.1);
}
.form-item select {
  cursor: pointer;
  appearance: auto;
}

/* 双列布局 */
.form-row {
  display: flex;
  gap: 16px;
}
.form-half {
  flex: 1;
}

/* 分割线 */
.form-divider {
  display: flex;
  align-items: center;
  margin: 8px 0 22px;
  gap: 12px;
}
.form-divider::before,
.form-divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: #e8647a;
  opacity: 0.4;
}
.form-divider span {
  font-size: 13px;
  color: #c41e3a;
  font-weight: 600;
  white-space: nowrap;
}

.error-msg {
  color: #c41e3a;
  font-size: 13px;
  margin: 0 0 16px 0;
  text-align: center;
}
.login-btn {
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
.login-btn:hover:not(:disabled) {
  background: #a8162b;
}
.login-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.switch-tip {
  text-align: center;
  margin-top: 24px;
  font-size: 14px;
  color: #666;
}
.register-text {
  color: #c41e3a;
  cursor: pointer;
}
.register-text:hover {
  text-decoration: underline;
}

.page-footer {
  background-color: #c41e3a;
  color: #fff;
  text-align: center;
  padding: 24px 5%;
}

/* ===== 角色选择器 ===== */
.role-label {
  display: block;
  font-size: 15px;
  color: #444;
  margin-bottom: 8px;
}
.role-selector {
  display: flex;
  gap: 12px;
}
.role-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px 10px;
  border: 2px solid #e8647a;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.25s;
  background: #fff;
}
.role-card:hover {
  border-color: #c41e3a;
  background: #fef5f7;
}
.role-card.active {
  border-color: #c41e3a;
  background: #fef0f2;
  box-shadow: 0 0 0 3px rgba(196, 30, 58, 0.15);
}
.role-icon {
  width: 40px;
  height: 40px;
  margin-bottom: 8px;
  color: #c41e3a;
}
.role-icon svg {
  width: 100%;
  height: 100%;
}
.role-name {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin-bottom: 4px;
}
.role-desc {
  font-size: 12px;
  color: #999;
}
.role-card.active .role-name {
  color: #c41e3a;
}
.role-card.active .role-desc {
  color: #e8647a;
}

/* ===== 响应式 ===== */
@media (max-width: 992px) {
  .page-wrap {
    grid-template-columns: 1fr;
  }
  .float-card {
    height: 360px;
  }
  .float-text-wrap h2 {
    font-size: 32px;
  }
}
@media (max-width: 576px) {
  .float-card {
    height: 300px;
  }
  .float-text-wrap h2 {
    font-size: 26px;
  }
  .role-selector {
    flex-direction: column;
  }
  .form-row {
    flex-direction: column;
    gap: 0;
  }
}
</style>
