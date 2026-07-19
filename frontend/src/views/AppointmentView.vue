<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { listPublicProfiles } from '@/api/profile'
import { listPublicProjectsByInheritor } from '@/api/project'
import { getAvailableSlots } from '@/api/slot'
import { submitOrder } from '@/api/order'
import type { InheritorProfileVO, HeritageProjectVO, AppointmentSlot } from '@/types/api'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const inheritorId = Number(route.params.inheritorId)

// ===== 登录状态 =====
const isLoggedIn = computed(() => authStore.isLoggedIn)
const userName = computed(() => authStore.userInfo?.name)
const userRole = computed(() => authStore.userInfo?.role)
const isInheritor = computed(() => userRole.value === 'INHERITOR')

function handleLogout() {
  authStore.logout()
  router.push('/')
}

// ===== 传承人信息 =====
const inheritor = ref<InheritorProfileVO | null>(null)
const loadingProfile = ref(true)

async function loadInheritor() {
  loadingProfile.value = true
  try {
    const res = await listPublicProfiles()
    const list = res.data || []
    inheritor.value = list.find(p => p.employeeId === inheritorId) || null
    if (inheritor.value) {
      document.title = `${inheritor.value.name} — 工坊预约 — 码上非遗`
    }
  } catch {
    // ignore
  } finally {
    loadingProfile.value = false
  }
}

// ===== 非遗项目 =====
const projects = ref<HeritageProjectVO[]>([])
const loadingProjects = ref(true)

async function loadProjects() {
  loadingProjects.value = true
  try {
    const res = await listPublicProjectsByInheritor(inheritorId)
    projects.value = res.data || []
  } catch {
    // ignore
  } finally {
    loadingProjects.value = false
  }
}

// ===== 可用时段 =====
const selectedDate = ref('')
const availableSlots = ref<AppointmentSlot[]>([])
const loadingSlots = ref(false)

async function loadSlots() {
  if (!selectedDate.value) {
    availableSlots.value = []
    return
  }
  loadingSlots.value = true
  try {
    const res = await getAvailableSlots(inheritorId, selectedDate.value)
    availableSlots.value = res.data || []
  } catch {
    availableSlots.value = []
  } finally {
    loadingSlots.value = false
  }
}

// ===== 预约表单 =====
const selectedProjectId = ref<number | undefined>(undefined)
const bookForm = ref({
  userName: '',
  phone: '',
  timeSlot: '',
  peopleCount: 1,
  remark: '',
})

const submitting = ref(false)
const submitSuccess = ref(false)
const submitError = ref('')

async function handleSubmit() {
  submitError.value = ''
  if (!bookForm.value.userName.trim()) {
    submitError.value = '请输入姓名'
    return
  }
  if (!bookForm.value.phone.trim()) {
    submitError.value = '请输入联系电话'
    return
  }
  if (!selectedDate.value) {
    submitError.value = '请选择预约日期'
    return
  }
  if (!bookForm.value.timeSlot) {
    submitError.value = '请选择预约时段'
    return
  }

  submitting.value = true
  try {
    await submitOrder({
      userName: bookForm.value.userName.trim(),
      phone: bookForm.value.phone.trim(),
      inheritorId,
      projectId: selectedProjectId.value,
      appointmentDate: selectedDate.value,
      timeSlot: bookForm.value.timeSlot,
      peopleCount: bookForm.value.peopleCount,
      remark: bookForm.value.remark.trim(),
    })
    submitSuccess.value = true
  } catch (e: any) {
    submitError.value = e?.response?.data?.msg || e?.msg || '预约提交失败，请稍后再试'
  } finally {
    submitting.value = false
  }
}

// ===== 工具函数 =====
function splitCrafts(craftTypes: string): string[] {
  return craftTypes ? craftTypes.split(/[,，]/).filter(Boolean) : []
}

/** 获取今天日期字符串 yyyy-MM-dd */
function todayStr(): string {
  const d = new Date()
  const yyyy = d.getFullYear()
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  return `${yyyy}-${mm}-${dd}`
}

onMounted(() => {
  loadInheritor()
  loadProjects()
})
</script>

<template>
  <div class="appointment-page">

    <!-- ==================== 主体内容 ==================== -->
    <main class="appointment-main">
      <!-- 返回链接 -->
      <div class="back-row">
        <a class="back-link" @click="router.push('/inheritors')">← 返回传承人列表</a>
      </div>

      <!-- 加载中 -->
      <div v-if="loadingProfile" class="loading-state">加载中...</div>

      <!-- 传承人不存在 -->
      <div v-else-if="!inheritor" class="empty-state">
        <p>😕 未找到该传承人信息</p>
        <button class="back-btn" @click="router.push('/inheritors')">返回传承人列表</button>
      </div>

      <template v-else>
        <!-- ===== 传承人信息区 ===== -->
        <section class="inheritor-section">
          <div class="inheritor-card">
            <div class="inheritor-avatar">
              <span class="avatar-initial">{{ (inheritor.name || '传')[0] }}</span>
            </div>
            <div class="inheritor-detail">
              <h2>{{ inheritor.name }}</h2>
              <p class="inheritor-honor">{{ inheritor.honorTitle || '非遗传承人' }}</p>
              <div v-if="splitCrafts(inheritor.craftTypes).length > 0" class="craft-tags">
                <span v-for="craft in splitCrafts(inheritor.craftTypes)" :key="craft" class="craft-tag">{{ craft }}</span>
              </div>
              <p class="inheritor-region" v-if="inheritor.region || inheritor.address">
                📍 {{ inheritor.region || inheritor.address }}
              </p>
              <p class="inheritor-intro">{{ inheritor.selfIntro || '暂无简介' }}</p>
            </div>
          </div>
        </section>

        <!-- ===== 非遗项目展示区 ===== -->
        <section class="projects-section">
          <h3 class="section-title">非遗项目</h3>
          <div v-if="loadingProjects" class="loading-state">加载中...</div>
          <div v-else-if="projects.length === 0" class="empty-tip">该传承人暂无已上架的非遗项目</div>
          <div v-else class="project-grid">
            <div v-for="project in projects" :key="project.id" class="project-card">
              <div class="project-type-badge" :class="project.projectType === 0 ? 'welfare-badge' : 'commercial-badge'">
                {{ project.projectType === 0 ? '公益' : '付费' }}
              </div>
              <h4 class="project-title">{{ project.title }}</h4>
              <p class="project-category" v-if="project.category">{{ project.category }}</p>
              <p class="project-desc">{{ project.description?.slice(0, 120) || '暂无描述' }}</p>
              <div class="project-meta">
                <span v-if="project.location">📍 {{ project.location }}</span>
                <span v-if="project.startDate">📅 {{ project.startDate }} ~ {{ project.endDate }}</span>
                <span>👥 {{ project.currentPeople }}/{{ project.maxPeople || '不限' }}人</span>
              </div>
              <div class="project-price" v-if="project.projectType === 1 && project.price">
                💰 ¥{{ project.price }}
              </div>
            </div>
          </div>
        </section>

        <!-- ===== 预约表单区 ===== -->
        <section class="booking-section">
          <h3 class="section-title">工坊预约</h3>

          <!-- 预约成功 -->
          <div v-if="submitSuccess" class="success-card">
            <div class="success-icon">✅</div>
            <h4>预约已提交！</h4>
            <p>您的预约申请已成功提交，请等待传承人确认。</p>
            <div class="success-actions">
              <button class="btn-primary" @click="router.push('/inheritors')">返回传承人列表</button>
              <button class="btn-secondary" @click="submitSuccess = false; router.push('/profile')">查看我的订单</button>
            </div>
          </div>

          <!-- 预约表单 -->
          <div v-else class="booking-form-card">
            <!-- 错误提示 -->
            <div v-if="submitError" class="form-error">{{ submitError }}</div>

            <div class="form-row">
              <div class="form-group">
                <label>预约项目</label>
                <select v-model="selectedProjectId" class="form-input">
                  <option :value="undefined">不限项目</option>
                  <option v-for="p in projects" :key="p.id" :value="p.id">
                    {{ p.title }}（{{ p.projectType === 0 ? '公益' : '付费' }}）
                  </option>
                </select>
              </div>
              <div class="form-group">
                <label>预约日期 <span class="required">*</span></label>
                <input
                  type="date"
                  v-model="selectedDate"
                  :min="todayStr()"
                  @change="loadSlots"
                  class="form-input"
                />
              </div>
              <div class="form-group">
                <label>预约时段 <span class="required">*</span></label>
                <select v-model="bookForm.timeSlot" class="form-input" :disabled="!selectedDate">
                  <option value="">请选择时段</option>
                  <option
                    v-for="slot in availableSlots"
                    :key="slot.id"
                    :value="slot.startTime + ' - ' + slot.endTime"
                    :disabled="slot.bookedCount >= slot.maxPeople || slot.status === 0"
                  >
                    {{ slot.startTime }} - {{ slot.endTime }}
                    （{{ slot.bookedCount }}/{{ slot.maxPeople }}人）
                  </option>
                </select>
                <span v-if="loadingSlots" class="form-hint">加载时段中...</span>
                <span v-else-if="selectedDate && availableSlots.length === 0" class="form-hint warn">该日期暂无可用时段</span>
              </div>
            </div>

            <div class="form-row">
              <div class="form-group">
                <label>姓名 <span class="required">*</span></label>
                <input
                  type="text"
                  v-model="bookForm.userName"
                  placeholder="请输入您的姓名"
                  class="form-input"
                  maxlength="32"
                />
              </div>
              <div class="form-group">
                <label>联系电话 <span class="required">*</span></label>
                <input
                  type="text"
                  v-model="bookForm.phone"
                  placeholder="请输入联系电话"
                  class="form-input"
                  maxlength="20"
                />
              </div>
            </div>

            <div class="form-row">
              <div class="form-group">
                <label>预约人数</label>
                <input
                  type="number"
                  v-model.number="bookForm.peopleCount"
                  :min="1"
                  :max="50"
                  class="form-input"
                />
              </div>
              <div class="form-group">
                <label>备注说明</label>
                <input
                  type="text"
                  v-model="bookForm.remark"
                  placeholder="如有特殊需求请注明（选填）"
                  class="form-input"
                  maxlength="500"
                />
              </div>
            </div>

            <button
              class="submit-btn"
              :disabled="submitting"
              @click="handleSubmit"
            >
              {{ submitting ? '提交中...' : '提交预约' }}
            </button>
          </div>
        </section>
      </template>
    </main>

    <!-- ==================== 页脚 ==================== -->
    <footer class="page-footer">
      <p>校园非遗与文创传承人平台 © 2026 码上非遗</p>
    </footer>
  </div>
</template>

<style scoped>
.appointment-page {
  min-height: 100vh;
  background: #fcf4e8;
  display: flex;
  flex-direction: column;
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
  position: sticky;
  top: 0;
  z-index: 100;
}
.logo {
  font-size: 20px;
  font-weight: bold;
  cursor: pointer;
}
.nav-links {
  display: flex;
  align-items: center;
  gap: 20px;
}
.nav-item {
  color: #fff;
  text-decoration: none;
  font-size: 14px;
  cursor: pointer;
  padding: 4px 0;
  border-bottom: 2px solid transparent;
  transition: border-color 0.2s;
}
.nav-item:hover,
.nav-item.active {
  border-bottom-color: #fff;
}
.nav-separator {
  width: 1px;
  height: 18px;
  background: rgba(255, 255, 255, 0.4);
  margin: 0 4px;
}
.nav-inheritor {
  color: #ffd700;
}
.nav-inheritor:hover {
  border-bottom-color: #ffd700;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}
.user-role-tag {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: bold;
}
.user-tag {
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
}
.inheritor-tag {
  background: #ffd700;
  color: #c41e3a;
}
.user-name {
  font-size: 14px;
  color: #fff;
  margin-right: 4px;
}
.header-btn {
  padding: 6px 12px;
  font-size: 12px;
  color: #fff;
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 4px;
  cursor: pointer;
  transition: 0.2s;
}
.header-btn:hover {
  background: rgba(255, 255, 255, 0.35);
}
.logout-btn {
  padding: 6px 12px;
  font-size: 12px;
  color: #c41e3a;
  background: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: 0.2s;
}
.logout-btn:hover {
  background: #f5f5f5;
}
.login-top-btn {
  padding: 8px 22px;
  font-size: 14px;
  color: #c41e3a;
  background: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-weight: bold;
}
.login-top-btn:hover {
  background: #f0f0f0;
}

/* ---- 主体 ---- */
.appointment-main {
  flex: 1;
  max-width: 960px;
  width: 100%;
  margin: 0 auto;
  padding: 32px 20px;
}

.back-row {
  margin-bottom: 24px;
}
.back-link {
  color: #c41e3a;
  cursor: pointer;
  font-size: 14px;
  text-decoration: none;
  transition: 0.2s;
}
.back-link:hover {
  color: #a8162b;
  text-decoration: underline;
}

/* ---- 加载/空状态 ---- */
.loading-state {
  text-align: center;
  padding: 80px 0;
  font-size: 16px;
  color: #999;
}
.empty-state {
  text-align: center;
  padding: 80px 0;
}
.empty-state p {
  font-size: 18px;
  color: #999;
  margin-bottom: 20px;
}

/* ===== 传承人信息区 ===== */
.inheritor-section {
  margin-bottom: 36px;
}
.inheritor-card {
  background: #fff;
  border-radius: 12px;
  padding: 32px;
  display: flex;
  gap: 28px;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.07);
  align-items: flex-start;
}
.inheritor-avatar {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  background: linear-gradient(135deg, #e8647a, #c41e3a);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.avatar-initial {
  font-size: 42px;
  color: #fff;
  font-weight: 700;
  user-select: none;
}
.inheritor-detail {
  flex: 1;
  min-width: 0;
}
.inheritor-detail h2 {
  font-size: 24px;
  color: #222;
  margin: 0 0 6px;
}
.inheritor-honor {
  color: #c41e3a;
  font-size: 15px;
  font-weight: 500;
  margin-bottom: 10px;
}
.craft-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}
.craft-tag {
  display: inline-block;
  padding: 3px 10px;
  background: #fff5f7;
  color: #c41e3a;
  border: 1px solid #f0c0c8;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}
.inheritor-region {
  font-size: 14px;
  color: #666;
  margin-bottom: 8px;
}
.inheritor-intro {
  font-size: 14px;
  color: #555;
  line-height: 1.8;
}

/* ===== 非遗项目区 ===== */
.projects-section {
  margin-bottom: 36px;
}
.section-title {
  font-size: 20px;
  color: #c41e3a;
  margin-bottom: 16px;
  padding-left: 12px;
  border-left: 4px solid #c41e3a;
}
.empty-tip {
  text-align: center;
  padding: 40px 0;
  font-size: 15px;
  color: #999;
  background: #fff;
  border-radius: 12px;
}
.project-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}
.project-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.07);
  position: relative;
  transition: 0.3s;
}
.project-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 22px rgba(196, 30, 58, 0.12);
}
.project-type-badge {
  position: absolute;
  top: 16px;
  right: 16px;
  font-size: 11px;
  padding: 3px 10px;
  border-radius: 10px;
  font-weight: 600;
}
.welfare-badge {
  background: #e8f5e9;
  color: #2e7d32;
}
.commercial-badge {
  background: #fff3e0;
  color: #e65100;
}
.project-title {
  font-size: 18px;
  color: #222;
  margin: 0 70px 8px 0;
}
.project-category {
  font-size: 13px;
  color: #999;
  margin-bottom: 10px;
}
.project-desc {
  font-size: 13px;
  color: #666;
  line-height: 1.7;
  margin-bottom: 14px;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.project-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 12px;
  color: #888;
}
.project-price {
  margin-top: 10px;
  font-size: 16px;
  color: #e65100;
  font-weight: 600;
}

/* ===== 预约表单区 ===== */
.booking-section {
  margin-bottom: 36px;
}
.success-card {
  background: #fff;
  border-radius: 12px;
  padding: 48px 32px;
  text-align: center;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.07);
}
.success-icon {
  font-size: 56px;
  margin-bottom: 16px;
}
.success-card h4 {
  font-size: 20px;
  color: #2e7d32;
  margin-bottom: 8px;
}
.success-card p {
  font-size: 15px;
  color: #666;
  margin-bottom: 24px;
}
.success-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}
.btn-primary {
  padding: 10px 24px;
  background: #c41e3a;
  color: #fff;
  border: none;
  border-radius: 22px;
  font-size: 14px;
  cursor: pointer;
  transition: 0.2s;
}
.btn-primary:hover {
  background: #a8162b;
}
.btn-secondary {
  padding: 10px 24px;
  background: #fff;
  color: #c41e3a;
  border: 1px solid #c41e3a;
  border-radius: 22px;
  font-size: 14px;
  cursor: pointer;
  transition: 0.2s;
}
.btn-secondary:hover {
  background: #fff5f7;
}

.booking-form-card {
  background: #fff;
  border-radius: 12px;
  padding: 32px;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.07);
}
.form-error {
  background: #fff0f0;
  color: #c41e3a;
  padding: 10px 16px;
  border-radius: 8px;
  font-size: 14px;
  margin-bottom: 20px;
  border: 1px solid #f0c0c8;
}
.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 20px;
}
.form-group {
  display: flex;
  flex-direction: column;
}
.form-group label {
  font-size: 14px;
  color: #333;
  margin-bottom: 6px;
  font-weight: 500;
}
.required {
  color: #c41e3a;
}
.form-input {
  padding: 10px 14px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  color: #333;
  background: #fafafa;
  transition: 0.2s;
  outline: none;
  font-family: inherit;
}
.form-input:focus {
  border-color: #c41e3a;
  background: #fff;
  box-shadow: 0 0 0 3px rgba(196, 30, 58, 0.08);
}
.form-input:disabled {
  background: #eee;
  color: #999;
  cursor: not-allowed;
}
.form-hint {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}
.form-hint.warn {
  color: #e65100;
}
.submit-btn {
  width: 100%;
  padding: 14px;
  background: #c41e3a;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: 0.2s;
  margin-top: 8px;
}
.submit-btn:hover:not(:disabled) {
  background: #a8162b;
}
.submit-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

/* ---- 页脚 ---- */
.page-footer {
  background-color: #c41e3a;
  color: #fff;
  text-align: center;
  padding: 24px 5%;
  margin-top: auto;
}

/* ---- 响应式 ---- */
@media (max-width: 768px) {
  .page-header {
    padding: 0 16px;
    flex-wrap: wrap;
  }
  .nav-links {
    gap: 12px;
  }
  .nav-item {
    font-size: 12px;
  }
  .header-right {
    flex-wrap: wrap;
  }
  .appointment-main {
    padding: 24px 16px;
  }
  .inheritor-card {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }
  .craft-tags {
    justify-content: center;
  }
  .form-row {
    grid-template-columns: 1fr;
  }
  .project-grid {
    grid-template-columns: 1fr;
  }
}
</style>