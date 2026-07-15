<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { listMySlots, createSlot, updateSlot, deleteSlot } from '@/api/slot'
import type { AppointmentSlot } from '@/types/api'

const slots = ref<AppointmentSlot[]>([])
const loading = ref(false)
const showForm = ref(false)
const editingId = ref<number | null>(null)

const form = ref({
  slotDate: '',
  timeSlot: '',
  maxPeople: 5,
  status: 1,
})

const timeOptions = ['上午 9:00-11:00', '上午 10:00-12:00', '下午 14:00-16:00', '下午 15:00-17:00', '晚上 18:00-20:00']

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
  form.value = { slotDate: '', timeSlot: '', maxPeople: 5, status: 1 }
  showForm.value = true
}

function openEditForm(slot: AppointmentSlot) {
  editingId.value = slot.id
  form.value = {
    slotDate: slot.slotDate,
    timeSlot: slot.timeSlot,
    maxPeople: slot.maxPeople,
    status: slot.status,
  }
  showForm.value = true
}

async function handleSave() {
  if (!form.value.slotDate || !form.value.timeSlot) return
  try {
    if (editingId.value) {
      await updateSlot(editingId.value, form.value as AppointmentSlot)
    } else {
      await createSlot(form.value as AppointmentSlot)
    }
    showForm.value = false
    await loadSlots()
  } catch { /* ignore */ }
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
    await updateSlot(slot.id, { ...slot, status: slot.status === 1 ? 0 : 1 } as AppointmentSlot)
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
          <label>时段</label>
          <select v-model="form.timeSlot" class="form-input">
            <option value="" disabled>选择时段</option>
            <option v-for="t in timeOptions" :key="t" :value="t">{{ t }}</option>
          </select>
        </div>
        <div class="form-group">
          <label>最大人数</label>
          <input v-model.number="form.maxPeople" type="number" min="1" max="50" class="form-input" />
        </div>
      </div>
      <div class="form-actions">
        <button class="btn-primary" @click="handleSave">保存</button>
        <button class="btn-outline" @click="showForm = false">取消</button>
      </div>
    </div>

    <div class="table-wrap">
      <table class="order-table">
        <thead>
          <tr>
            <th>日期</th><th>时段</th><th>最大人数</th><th>已预约</th><th>状态</th><th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="slot in slots" :key="slot.id">
            <td>{{ slot.slotDate }}</td>
            <td>{{ slot.timeSlot }}</td>
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
.order-table th, .order-table td { padding: 14px 16px; text-align: left; font-size: 14px; }
.order-table thead { background: #fff5f7; }
.order-table th { color: #c41e3a; font-weight: 600; font-size: 13px; }
.order-table tbody tr { border-bottom: 1px solid #f5f5f5; }
.order-table tbody tr:hover { background: #fefafa; }
.action-cell { white-space: nowrap; display: flex; gap: 6px; }
.status-tag { display: inline-block; padding: 3px 10px; border-radius: 12px; font-size: 12px; font-weight: 600; }
.status-confirmed { background: #d4edda; color: #155724; }
.status-cancelled { background: #f8d7da; color: #721c24; }
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
.form-row { display: flex; gap: 16px; }
.form-group { flex: 1; margin-bottom: 12px; }
.form-group label { display: block; font-size: 13px; color: #666; margin-bottom: 4px; }
.form-input { width: 100%; height: 40px; padding: 0 12px; border: 1px solid #e0e0e0; border-radius: 8px; font-size: 14px; box-sizing: border-box; outline: none; }
.form-input:focus { border-color: #c41e3a; box-shadow: 0 0 0 3px rgba(196,30,58,0.08); }
.form-actions { display: flex; gap: 12px; margin-top: 16px; }
.empty-state { text-align: center; padding: 60px 20px; color: #999; font-size: 14px; }
</style>
