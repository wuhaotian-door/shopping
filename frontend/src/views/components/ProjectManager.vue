<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { listMyProjects, createProject, updateProject, deleteProject, toggleProjectStatus } from '@/api/project'
import { getSlotRange } from '@/api/slot'
import type { HeritageProjectVO, HeritageProjectDTO, SlotDateRange } from '@/types/api'

const props = defineProps<{
  projectType: number  // 0 = 公益（非盈利）, 1 = 非公益（盈利）
}>()

const projects = ref<HeritageProjectVO[]>([])
const loading = ref(false)
const showEditor = ref(false)
const editingId = ref<number | null>(null)

const categories = ['剪纸', '刺绣', '陶艺', '竹编', '木雕', '泥塑', '皮影', '其他']

const form = ref<HeritageProjectDTO>({
  title: '',
  category: '',
  description: '',
  location: '',
  startDate: '',
  endDate: '',
  maxPeople: 20,
  status: 0,
  projectType: props.projectType,
  price: undefined,
})

const submitMsg = ref('')
const slotRange = ref<SlotDateRange | null>(null)

// 面板标签
const panelLabel = computed(() => props.projectType === 0 ? '公益（非盈利）' : '非公益（盈利）')
const panelDesc = computed(() => props.projectType === 0 ? '公益（非盈利）非遗项目' : '非公益（盈利）非遗项目')
const panelTheme = computed(() => props.projectType === 0 ? 'welfare' : 'forprofit')

onMounted(() => { loadProjects(); loadSlotRange() })

async function loadProjects() {
  loading.value = true
  try {
    const res = await listMyProjects(props.projectType)
    projects.value = res.data
  } catch { /* ignore */ } finally { loading.value = false }
}

async function loadSlotRange() {
  try {
    const res = await getSlotRange()
    if (res.data && res.data.minDate) {
      slotRange.value = res.data
    } else {
      slotRange.value = null
    }
  } catch { slotRange.value = null }
}

function openCreate() {
  editingId.value = null
  form.value = { title: '', category: '', description: '', location: '', startDate: '', endDate: '', maxPeople: 20, status: 0, projectType: props.projectType, price: undefined }
  showEditor.value = true
  submitMsg.value = ''
}

function openEdit(project: HeritageProjectVO) {
  editingId.value = project.id
  form.value = {
    title: project.title,
    category: project.category,
    description: project.description,
    location: project.location,
    startDate: project.startDate,
    endDate: project.endDate,
    maxPeople: project.maxPeople,
    status: project.status,
    projectType: project.projectType,
    price: project.price,
  }
  showEditor.value = true
  submitMsg.value = ''
}

/**
 * 检查项目日期是否在时段范围内
 * @returns true if valid (no warning or user confirmed), false if user cancelled
 */
function checkDateRange(): boolean {
  if (!form.value.startDate || !form.value.endDate) return true

  if (!slotRange.value || !slotRange.value.minDate) {
    return confirm('尚未配置任何可预约时段。建议先设置可预约时段后再发布项目。\n\n是否仍要继续保存？')
  }

  const start = form.value.startDate
  const end = form.value.endDate
  const slotMin = slotRange.value.minDate!
  const slotMax = slotRange.value.maxDate!

  const warnings: string[] = []
  if (start < slotMin) {
    warnings.push(`项目开始日期（${start}）早于最早时段（${slotMin}）`)
  }
  if (end > slotMax) {
    warnings.push(`项目结束日期（${end}）晚于最晚时段（${slotMax}）`)
  }

  if (warnings.length > 0) {
    const msg = warnings.join('\n')
    return confirm(`${msg}\n\n项目日期超出了已设置的时段范围，建议先配置相应日期的时段。\n是否仍要继续保存？`)
  }

  return true
}

async function handleSave() {
  if (!form.value.title) { submitMsg.value = '请填写项目标题'; return }

  // 校验日期范围
  if (!checkDateRange()) return

  try {
    let res
    if (editingId.value) {
      res = await updateProject(editingId.value, form.value)
    } else {
      res = await createProject(form.value)
    }
    showEditor.value = false
    await loadProjects()

    // 检查后端返回的警告信息
    const data = res.data
    if (data && data !== '项目创建成功' && data !== '项目更新成功') {
      alert(data)
    }
  } catch (err: any) {
    const msg = err?.response?.data?.msg || err?.message || '保存失败'
    submitMsg.value = msg
  }
}

async function handleDelete(id: number) {
  if (!confirm('确定删除此项目吗？')) return
  try { await deleteProject(id); await loadProjects() } catch { /* ignore */ }
}

async function handleToggle(project: HeritageProjectVO) {
  const newStatus = project.status === 1 ? 0 : 1
  try { await toggleProjectStatus(project.id, newStatus); await loadProjects() } catch { /* ignore */ }
}
</script>

<template>
  <section class="tab-content" :class="`panel-${panelTheme}`">
    <div class="section-header">
      <h2 :class="`heading-${panelTheme}`">{{ panelDesc }}</h2>
      <div class="header-right">
        <span v-if="slotRange?.minDate" class="slot-range-info">
          时段覆盖：{{ slotRange.minDate }} ~ {{ slotRange.maxDate }}
        </span>
        <span v-else class="slot-range-warn">尚未配置时段</span>
        <button class="btn-primary" :class="`btn-${panelTheme}`" @click="openCreate">+ 发布{{ panelLabel }}项目</button>
      </div>
    </div>

    <div v-if="showEditor" class="editor-card">
      <h3>{{ editingId ? '编辑项目' : `发布${panelLabel}项目` }}</h3>
      <div class="form-row">
        <div class="form-group flex-2">
          <label>项目标题 *</label>
          <input v-model="form.title" type="text" class="form-input" placeholder="项目名称" />
        </div>
        <div class="form-group flex-1">
          <label>分类</label>
          <select v-model="form.category" class="form-input">
            <option value="" disabled>选择</option>
            <option v-for="c in categories" :key="c" :value="c">{{ c }}</option>
          </select>
        </div>
      </div>
      <div class="form-row">
        <div class="form-group flex-1">
          <label>活动地点</label>
          <input v-model="form.location" type="text" class="form-input" placeholder="地点" />
        </div>
        <div class="form-group flex-1">
          <label>最大人数</label>
          <input v-model.number="form.maxPeople" type="number" min="1" class="form-input" />
        </div>
        <div class="form-group flex-1">
          <label>状态</label>
          <select v-model="form.status" class="form-input">
            <option :value="0">下架</option>
            <option :value="1">上架</option>
          </select>
        </div>
      </div>
      <div class="form-row">
        <div class="form-group flex-1">
          <label>开始日期</label>
          <input v-model="form.startDate" type="date" class="form-input" />
        </div>
        <div class="form-group flex-1">
          <label>结束日期</label>
          <input v-model="form.endDate" type="date" class="form-input" />
        </div>
      </div>
      <div class="form-row">
        <div class="form-group flex-1">
          <label>项目类型</label>
          <select v-model="form.projectType" class="form-input">
            <option :value="0">公益（非盈利）</option>
            <option :value="1">非公益（盈利）</option>
          </select>
        </div>
        <div class="form-group flex-1" v-if="form.projectType === 1">
          <label>价格（元）</label>
          <input v-model.number="form.price" type="number" min="0" step="0.01" class="form-input" placeholder="0.00" />
        </div>
      </div>
      <div class="form-group">
        <label>项目描述</label>
        <textarea v-model="form.description" class="form-textarea" rows="5" placeholder="项目详情..."></textarea>
      </div>
      <div class="form-actions">
        <button class="btn-primary" :class="`btn-${panelTheme}`" @click="handleSave">保存</button>
        <button class="btn-outline" @click="showEditor = false">取消</button>
        <span v-if="submitMsg" class="submit-msg error">{{ submitMsg }}</span>
      </div>
    </div>

    <div class="table-wrap">
      <table class="order-table">
        <thead>
          <tr>
            <th>标题</th><th>分类</th><th>地点</th><th>日期</th>
            <th v-if="props.projectType === 1">价格</th>
            <th>人数</th><th>状态</th><th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="p in projects" :key="p.id">
            <td class="article-title-cell">{{ p.title }}</td>
            <td><span class="category-tag" :class="`cat-${panelTheme}`">{{ p.category || '-' }}</span></td>
            <td>{{ p.location || '-' }}</td>
            <td>{{ p.startDate || '?' }} ~ {{ p.endDate || '?' }}</td>
            <td v-if="props.projectType === 1">¥{{ (p.price ?? 0).toFixed(2) }}</td>
            <td>{{ p.currentPeople }}/{{ p.maxPeople }}</td>
            <td>
              <span class="article-status" :class="p.status === 1 ? 'published' : 'draft'">
                {{ p.status === 1 ? '已上架' : '已下架' }}
              </span>
            </td>
            <td class="action-cell">
              <button class="btn-outline-sm" @click="openEdit(p)">编辑</button>
              <button class="btn-confirm" @click="handleToggle(p)">
                {{ p.status === 1 ? '下架' : '上架' }}
              </button>
              <button class="btn-reject" @click="handleDelete(p.id)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-if="loading" class="empty-state">加载中…</div>
      <div v-else-if="projects.length === 0" class="empty-state">暂无{{ panelLabel }}项目，点击"发布{{ panelLabel }}项目"创建</div>
    </div>
  </section>
</template>

<style scoped>
.tab-content { }

/* ==================== 公益面板（红色系） ==================== */
.panel-welfare { }
.heading-welfare {
  font-size: 22px;
  color: #c41e3a;
  padding-left: 14px;
  border-left: 4px solid #c41e3a;
}
.btn-welfare {
  background: #c41e3a;
}
.btn-welfare:hover {
  background: #a8162b;
}
.cat-welfare {
  background: #ffe9ec;
  color: #c41e3a;
}

/* ==================== 非公益面板（蓝色系） ==================== */
.panel-forprofit { }
.heading-forprofit {
  font-size: 22px;
  color: #1a73e8;
  padding-left: 14px;
  border-left: 4px solid #1a73e8;
}
.btn-forprofit {
  background: #1a73e8;
}
.btn-forprofit:hover {
  background: #1557b0;
}
.cat-forprofit {
  background: #e8f0fe;
  color: #1a73e8;
}

.section-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 24px; flex-wrap: wrap; gap: 12px; }
.section-header h2 { }
.header-right { display: flex; align-items: center; gap: 16px; }
.slot-range-info { font-size: 13px; color: #28a745; background: #d4edda; padding: 4px 12px; border-radius: 12px; }
.slot-range-warn { font-size: 13px; color: #856404; background: #fff3cd; padding: 4px 12px; border-radius: 12px; }
.table-wrap { background: #fff; border-radius: 12px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.06); }
.order-table { width: 100%; border-collapse: collapse; }
.order-table th, .order-table td { padding: 14px 16px; text-align: left; font-size: 14px; }
.order-table thead { background: #fff5f7; }
.order-table th { color: #c41e3a; font-weight: 600; font-size: 13px; }
.order-table tbody tr { border-bottom: 1px solid #f5f5f5; }
.order-table tbody tr:hover { background: #fefafa; }
.article-title-cell { max-width: 220px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.action-cell { white-space: nowrap; display: flex; gap: 6px; }
.category-tag { display: inline-block; padding: 2px 10px; border-radius: 10px; font-size: 12px; }
.article-status { display: inline-block; padding: 2px 10px; border-radius: 10px; font-size: 12px; font-weight: 600; }
.article-status.published { background: #d4edda; color: #155724; }
.article-status.draft { background: #e2e3e5; color: #383d41; }
.btn-confirm { padding: 5px 10px; font-size: 12px; color: #fff; background: #28a745; border: none; border-radius: 4px; cursor: pointer; }
.btn-confirm:hover { background: #218838; }
.btn-outline-sm { padding: 5px 10px; font-size: 12px; color: #c41e3a; background: #fff; border: 1px solid #c41e3a; border-radius: 4px; cursor: pointer; }
.btn-outline-sm:hover { background: #fff5f7; }
.btn-reject { padding: 5px 10px; font-size: 12px; color: #fff; background: #dc3545; border: none; border-radius: 4px; cursor: pointer; }
.btn-reject:hover { background: #c82333; }
.btn-primary { padding: 10px 24px; font-size: 14px; color: #fff; border: none; border-radius: 6px; cursor: pointer; font-weight: bold; }
.btn-outline { padding: 10px 24px; font-size: 14px; color: #c41e3a; background: #fff; border: 1px solid #c41e3a; border-radius: 6px; cursor: pointer; }
.btn-outline:hover { background: #fff5f7; }
.editor-card { background: #fff; border-radius: 12px; padding: 28px; margin-bottom: 24px; box-shadow: 0 2px 10px rgba(0,0,0,0.06); }
.editor-card h3 { margin-bottom: 18px; color: #c41e3a; }
.form-row { display: flex; gap: 16px; }
.form-group { margin-bottom: 16px; }
.form-group label { display: block; font-size: 13px; color: #666; margin-bottom: 5px; font-weight: 600; }
.flex-2 { flex: 2; }
.flex-1 { flex: 1; }
.form-input { width: 100%; height: 42px; padding: 0 14px; border: 1px solid #e0e0e0; border-radius: 8px; font-size: 14px; box-sizing: border-box; outline: none; transition: 0.25s; background: #fafafa; }
.form-input:focus { border-color: #c41e3a; box-shadow: 0 0 0 3px rgba(196,30,58,0.08); background: #fff; }
.form-textarea { width: 100%; padding: 14px; border: 1px solid #e0e0e0; border-radius: 8px; font-size: 14px; resize: vertical; box-sizing: border-box; outline: none; font-family: inherit; line-height: 1.8; background: #fafafa; }
.form-textarea:focus { border-color: #c41e3a; box-shadow: 0 0 0 3px rgba(196,30,58,0.08); background: #fff; }
.form-actions { display: flex; align-items: center; gap: 12px; margin-top: 8px; }
.submit-msg { font-size: 13px; }
.submit-msg.error { color: #dc3545; }
.empty-state { text-align: center; padding: 60px 20px; color: #999; font-size: 14px; }
</style>
