<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { listMySlots, createSlot, updateSlot, deleteSlot } from '@/api/slot'
import type { AppointmentSlot, AppointmentSlotDTO } from '@/types/api'

const slots = ref<AppointmentSlot[]>([])
const loading = ref(false)
const showForm = ref(false)
const editingId = ref<number | null>(null)
const submitError = ref('')

const form = ref<AppointmentSlotDTO>({
  slotDate: '',
  startTime: '',
  endTime: '',
  recurrenceType: 'NONE',
  recurrenceEndDate: '',
  maxPeople: 5,
  status: 1,
})

const recurrenceOptions = [
  { value: 'NONE', label: '不重复' },
  { value: 'WEEKLY', label: '每周重复' },
  { value: 'MONTHLY', label: '每月重复' },
]

const showRecurrenceEnd = computed(() => form.value.recurrenceType !== 'NONE')

function getRecurrenceLabel(slot: AppointmentSlot): string {
  const map: Record<string, string> = { NONE: '不重复', WEEKLY: '每周', MONTHLY: '每月' }
  const label = map[slot.recurrenceType] || '不重复'
  if (slot.recurrenceType !== 'NONE' && slot.recurrenceEndDate) {
    return `${label}（至${slot.recurrenceEndDate}）`
  }
  return label
}

function getDayOfWeek(dateStr: string): string {
  if (!dateStr) return ''
  const days = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  const d = new Date(dateStr)
  return days[d.getDay()]
}

onMounted(() => { loadSlots() })

async function loadSlots() {
  loading.value = true
  try {
    const res = await listMySlots()
    slots.value = res.data
  } catch { /* ignore */ } finally { loading.value = false }
}

function openAddForm() {
  editingId.value = null
  form.value = { slotDate: '', startTime: '', endTime: '', recurrenceType: 'NONE', recurrenceEndDate: '', maxPeople: 5, status: 1 }
  showForm.value = true
  submitError.value = ''
}

function openEditForm(slot: AppointmentSlot) {
  editingId.value = slot.id
  form.value = {
    slotDate: slot.slotDate,
    startTime: slot.startTime,
    endTime: slot.endTime,
    recurrenceType: slot.recurrenceType || 'NONE',
    recurrenceEndDate: slot.recurrenceEndDate || '',
    maxPeople: slot.maxPeople,
    status: slot.status,
  }
  showForm.value = true
  submitError.value = ''
}

async function handleSave() {
  if (!form.value.slotDate) { submitError.value = '请选择日期'; return }
  if (!form.value.startTime) { submitError.value = '请选择开始时间'; return }
  if (!form.value.endTime) { submitError.value = '请选择结束时间'; return }
  if (form.value.startTime >= form.value.endTime) { submitError.value = '开始时间必须早于结束时间'; return }
  if (form.value.recurrenceType !== 'NONE' && !form.value.recurrenceEndDate) {
    submitError.value = '请选择重复截止日期'; return
  }

  try {
    // 构建请求数据，清除空的 recurrenceEndDate 避免 400 错误
    const payload: AppointmentSlotDTO = {
      ...form.value,
      recurrenceEndDate: form.value.recurrenceType !== 'NONE' && form.value.recurrenceEndDate ? form.value.recurrenceEndDate : undefined,
    }
    if (editingId.value) {
      await updateSlot(editingId.value, payload)
    } else {
      await createSlot(payload)
    }
    showForm.value = false
    submitError.value = ''
    await loadSlots()
  } catch (err: any) {
    const msg = err?.response?.data?.msg || err?.message || '保存失败'
    submitError.value = msg
  }
}

async function handleDelete(id: number) {
  if (!confirm('确定删除此时段吗？')) return
  try {
    await deleteSlot(id)
    await loadSlots()
  } catch { /* ignore */ }
}

async function toggleStatus(slot: AppointmentSlot) {
  try {
    const newStatus = slot.status === 1 ? 0 : 1
    await updateSlot(slot.id, {
      slotDate: slot.slotDate,
      startTime: slot.startTime,
      endTime: slot.endTime,
      recurrenceType: slot.recurrenceType || 'NONE',
      recurrenceEndDate: slot.recurrenceType !== 'NONE' ? slot.recurrenceEndDate : undefined,
      maxPeople: slot.maxPeople,
      status: newStatus,
    })
    await loadSlots()
  } catch { /* ignore */ }
}
</script>

<template>
  <section class="tab-content">
    <div class="section-header">
      <h2>时段管理</h2>
      <button class="btn-primary" @click="openAddForm">+ 添加时段</button>
    </div>

    <!-- 添加/编辑表单 -->
    <div v-if="showForm" class="form-card">
      <h3>{{ editingId ? '编辑时段' : '添加时段' }}</h3>
      <div class="form-row">
        <div class="form-group">
          <label>日期</label>
          <input v-model="form.slotDate" type="date" class="form-input" />
        </div>
        <div class="form-group">
          <label>开始时间</label>
          <input v-model="form.startTime" type="time" class="form-input" />
        </div>
        <div class="form-group">
          <label>结束时间</label>
          <input v-model="form.endTime" type="time" class="form-input" />
        </div>
      </div>
      <div class="form-row">
        <div class="form-group">
          <label>重复类型</label>
          <select v-model="form.recurrenceType" class="form-input">
            <option v-for="r in recurrenceOptions" :key="r.value" :value="r.value">{{ r.label }}</option>
          </select>
        </div>
        <div class="form-group" v-if="showRecurrenceEnd">
          <label>重复截止日期</label>
          <input v-model="form.recurrenceEndDate" type="date" class="form-input" :min="form.slotDate" />
          <span class="form-hint">从开始日期重复到截止日期（含）</span>
        </div>
        <div class="form-group">
          <label>最大人数</label>
          <input v-model.number="form.maxPeople" type="number" min="1" max="50" class="form-input" />
        </div>
      </div>
      <div class="form-actions">
        <button class="btn-primary" @click="handleSave">保存</button>
        <button class="btn-outline" @click="showForm = false">取消</button>
        <span v-if="submitError" class="submit-msg error">{{ submitError }}</span>
      </div>
    </div>

    <div class="table-wrap">
      <table class="order-table">
        <thead>
          <tr>
            <th>日期</th><th>开始</th><th>结束</th><th>重复规则</th>
            <th>最大人数</th><th>已预约</th><th>状态</th><th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="slot in slots" :key="slot.id">
            <td>{{ slot.slotDate }} {{ getDayOfWeek(slot.slotDate) }}</td>
            <td>{{ slot.startTime?.substring(0, 5) }}</td>
            <td>{{ slot.endTime?.substring(0, 5) }}</td>
            <td><span class="recurrence-tag">{{ getRecurrenceLabel(slot) }}</span></td>
            <td>{{ slot.maxPeople }}人</td>
            <td>{{ slot.bookedCount }}人</td>
            <td>
              <span class="status-tag" :class="slot.status === 1 ? 'status-confirmed' : 'status-cancelled'">
                {{ slot.status === 1 ? '可用' : '关闭' }}
              </span>
            </td>
            <td class="action-cell">
              <button class="btn-confirm" @click="toggleStatus(slot)">
                {{ slot.status === 1 ? '关闭' : '开启' }}
              </button>
              <button class="btn-outline-sm" @click="openEditForm(slot)">编辑</button>
              <button class="btn-reject" @click="handleDelete(slot.id)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-if="loading" class="empty-state">加载中…</div>
      <div v-else-if="slots.length === 0" class="empty-state">
        暂无可用时段，请点击"添加时段"设置可预约时间
      </div>
    </div>
  </section>
</template>

<style scoped>
.tab-content { }
.section-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 24px; }
.section-header h2 { font-size: 22px; color: #c41e3a; padding-left: 14px; border-left: 4px solid #c41e3a; }
.table-wrap { background: #fff; border-radius: 12px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.06); }
.order-table { width: 100%; border-collapse: collapse; }
.order-table th, .order-table td { padding: 14px 12px; text-align: left; font-size: 14px; }
.order-table thead { background: #fff5f7; }
.order-table th { color: #c41e3a; font-weight: 600; font-size: 13px; }
.order-table tbody tr { border-bottom: 1px solid #f5f5f5; }
.order-table tbody tr:hover { background: #fefafa; }
.action-cell { white-space: nowrap; display: flex; gap: 6px; }
.status-tag { display: inline-block; padding: 3px 10px; border-radius: 12px; font-size: 12px; font-weight: 600; }
.status-confirmed { background: #d4edda; color: #155724; }
.status-cancelled { background: #f8d7da; color: #721c24; }
.recurrence-tag { display: inline-block; padding: 2px 8px; background: #e8e0f0; color: #5a3d7a; border-radius: 10px; font-size: 12px; }
.btn-confirm { padding: 5px 10px; font-size: 12px; color: #fff; background: #28a745; border: none; border-radius: 4px; cursor: pointer; }
.btn-confirm:hover { background: #218838; }
.btn-outline-sm { padding: 5px 10px; font-size: 12px; color: #c41e3a; background: #fff; border: 1px solid #c41e3a; border-radius: 4px; cursor: pointer; }
.btn-outline-sm:hover { background: #fff5f7; }
.btn-reject { padding: 5px 10px; font-size: 12px; color: #fff; background: #dc3545; border: none; border-radius: 4px; cursor: pointer; }
.btn-reject:hover { background: #c82333; }
.btn-primary { padding: 10px 24px; font-size: 14px; color: #fff; background: #c41e3a; border: none; border-radius: 6px; cursor: pointer; font-weight: bold; }
.btn-primary:hover { background: #a8162b; }
.btn-outline { padding: 10px 24px; font-size: 14px; color: #c41e3a; background: #fff; border: 1px solid #c41e3a; border-radius: 6px; cursor: pointer; }
.btn-outline:hover { background: #fff5f7; }
.form-card { background: #fff; border-radius: 12px; padding: 24px; margin-bottom: 20px; box-shadow: 0 2px 10px rgba(0,0,0,0.06); }
.form-card h3 { margin-bottom: 16px; color: #c41e3a; }
.form-row { display: flex; gap: 16px; flex-wrap: wrap; }
.form-group { flex: 1; min-width: 120px; margin-bottom: 12px; }
.form-group label { display: block; font-size: 13px; color: #666; margin-bottom: 4px; }
.form-input { width: 100%; height: 40px; padding: 0 12px; border: 1px solid #e0e0e0; border-radius: 8px; font-size: 14px; box-sizing: border-box; outline: none; }
.form-input:focus { border-color: #c41e3a; box-shadow: 0 0 0 3px rgba(196,30,58,0.08); }
.form-hint { display: block; font-size: 11px; color: #999; margin-top: 2px; }
.form-actions { display: flex; align-items: center; gap: 12px; margin-top: 8px; flex-wrap: wrap; }
.submit-msg { font-size: 13px; }
.submit-msg.error { color: #dc3545; }
.empty-state { text-align: center; padding: 60px 20px; color: #999; font-size: 14px; }

@media (max-width: 768px) {
  .section-header { flex-direction: column; align-items: flex-start; gap: 10px; margin-bottom: 16px; }
  .section-header h2 { font-size: 18px; padding-left: 10px; border-left-width: 3px; }
  .btn-primary { padding: 8px 16px; font-size: 13px; }
  .btn-outline { padding: 8px 16px; font-size: 13px; }
  .form-card { padding: 16px; border-radius: 8px; margin-bottom: 14px; }
  .form-card h3 { font-size: 16px; margin-bottom: 12px; }
  .form-row { flex-direction: column; gap: 8px; }
  .form-group { min-width: 100%; margin-bottom: 8px; }
  .form-group label { font-size: 12px; }
  .form-input { height: 38px; font-size: 13px; padding: 0 10px; }
  .form-hint { font-size: 10px; }
  .form-actions { flex-direction: column; gap: 8px; align-items: stretch; }
  .form-actions .btn-primary, .form-actions .btn-outline { width: 100%; text-align: center; }
  .table-wrap { border-radius: 8px; overflow-x: auto; }
  .order-table { min-width: 650px; }
  .order-table th, .order-table td { padding: 10px 8px; font-size: 13px; }
  .order-table th { font-size: 12px; }
  .action-cell { flex-wrap: wrap; gap: 4px; }
  .btn-confirm, .btn-outline-sm, .btn-reject { padding: 4px 8px; font-size: 11px; }
  .status-tag { font-size: 11px; padding: 2px 8px; }
  .recurrence-tag { font-size: 11px; padding: 2px 6px; }
  .empty-state { padding: 40px 16px; font-size: 13px; }
}
</style>
