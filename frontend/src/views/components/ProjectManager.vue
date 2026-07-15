<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { listMyProjects, createProject, updateProject, deleteProject, toggleProjectStatus } from '@/api/project'
import type { HeritageProjectVO, HeritageProjectDTO } from '@/types/api'

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
})

const submitMsg = ref('')

onMounted(() => { loadProjects() })

async function loadProjects() {
  loading.value = true
  try {
    const res = await listMyProjects()
    projects.value = res.data
  } catch { /* ignore */ } finally { loading.value = false }
}

function openCreate() {
  editingId.value = null
  form.value = { title: '', category: '', description: '', location: '', startDate: '', endDate: '', maxPeople: 20, status: 0 }
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
  }
  showEditor.value = true
  submitMsg.value = ''
}

async function handleSave() {
  if (!form.value.title) { submitMsg.value = '请填写项目标题'; return }
  try {
    if (editingId.value) {
      await updateProject(editingId.value, form.value)
    } else {
      await createProject(form.value)
    }
    showEditor.value = false
    await loadProjects()
  } catch { submitMsg.value = '保存失败' }
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
  <section class="tab-content">
    <div class="section-header">
      <h2>非遗项目管理</h2>
      <button class="btn-primary" @click="openCreate">+ 发布项目</button>
    </div>

    <div v-if="showEditor" class="editor-card">
      <h3>{{ editingId ? '编辑项目' : '发布新项目' }}</h3>
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
      <div class="form-group">
        <label>项目描述</label>
        <textarea v-model="form.description" class="form-textarea" rows="5" placeholder="项目详情..."></textarea>
      </div>
      <div class="form-actions">
        <button class="btn-primary" @click="handleSave">保存</button>
        <button class="btn-outline" @click="showEditor = false">取消</button>
        <span v-if="submitMsg" class="submit-msg error">{{ submitMsg }}</span>
      </div>
    </div>

    <div class="table-wrap">
      <table class="order-table">
        <thead>
          <tr>
            <th>标题</th><th>分类</th><th>地点</th><th>日期</th>
            <th>人数</th><th>状态</th><th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="p in projects" :key="p.id">
            <td class="article-title-cell">{{ p.title }}</td>
            <td><span class="category-tag">{{ p.category || '-' }}</span></td>
            <td>{{ p.location || '-' }}</td>
            <td>{{ p.startDate || '?' }} ~ {{ p.endDate || '?' }}</td>
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
      <div v-else-if="projects.length === 0" class="empty-state">暂无项目，点击"发布项目"创建</div>
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
.article-title-cell { max-width: 220px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.action-cell { white-space: nowrap; display: flex; gap: 6px; }
.category-tag { display: inline-block; padding: 2px 10px; background: #ffe9ec; color: #c41e3a; border-radius: 10px; font-size: 12px; }
.article-status { display: inline-block; padding: 2px 10px; border-radius: 10px; font-size: 12px; font-weight: 600; }
.article-status.published { background: #d4edda; color: #155724; }
.article-status.draft { background: #e2e3e5; color: #383d41; }
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
