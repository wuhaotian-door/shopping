<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { listMyArticles, createArticle, updateArticle, deleteArticle } from '@/api/knowledge'
import type { KnowledgeArticleVO, KnowledgeArticleDTO } from '@/types/api'

const articles = ref<KnowledgeArticleVO[]>([])
const loading = ref(false)
const showEditor = ref(false)
const editingId = ref<number | null>(null)

const categories = ['剪纸', '刺绣', '陶艺', '竹编', '传承人故事', '技艺教学', '活动纪实', '非遗科普']

const form = ref<KnowledgeArticleDTO>({
  title: '',
  category: '',
  content: '',
  status: 0,
})

const submitMsg = ref('')

onMounted(() => { loadArticles() })

async function loadArticles() {
  loading.value = true
  try {
    const res = await listMyArticles()
    articles.value = res.data
  } catch { /* ignore */ } finally { loading.value = false }
}

function openCreate() {
  editingId.value = null
  form.value = { title: '', category: '', content: '', status: 0 }
  showEditor.value = true
  submitMsg.value = ''
}

function openEdit(article: KnowledgeArticleVO) {
  editingId.value = article.id
  form.value = {
    title: article.title,
    category: article.category,
    content: article.content,
    status: article.status,
  }
  showEditor.value = true
  submitMsg.value = ''
}

async function handleSave() {
  if (!form.value.title || !form.value.content) {
    submitMsg.value = '请填写标题和内容'
    return
  }
  try {
    if (editingId.value) {
      await updateArticle(editingId.value, form.value)
      submitMsg.value = '文章更新成功！'
    } else {
      await createArticle(form.value)
      submitMsg.value = '文章已保存为草稿！'
    }
    showEditor.value = false
    await loadArticles()
    setTimeout(() => { submitMsg.value = '' }, 3000)
  } catch { submitMsg.value = '保存失败，请重试' }
}

async function handleDelete(id: number) {
  if (!confirm('确定删除此文章吗？')) return
  try {
    await deleteArticle(id)
    await loadArticles()
  } catch { /* ignore */ }
}

async function togglePublish(article: KnowledgeArticleVO) {
  const newStatus = article.status === 1 ? 0 : 1
  try {
    await updateArticle(article.id, {
      title: article.title,
      content: article.content,
      category: article.category,
      status: newStatus,
    })
    await loadArticles()
  } catch { /* ignore */ }
}
</script>

<template>
  <section class="tab-content">
    <div class="section-header">
      <h2>非遗资讯管理</h2>
      <button class="btn-primary" @click="openCreate">+ 写文章</button>
    </div>

    <!-- 文章编辑面板 -->
    <div v-if="showEditor" class="editor-card">
      <h3>{{ editingId ? '编辑文章' : '写文章' }}</h3>
      <div class="form-row">
        <div class="form-group flex-2">
          <label>标题</label>
          <input v-model="form.title" type="text" class="form-input" placeholder="文章标题" />
        </div>
        <div class="form-group flex-1">
          <label>分类</label>
          <select v-model="form.category" class="form-input">
            <option value="" disabled>选择分类</option>
            <option v-for="c in categories" :key="c" :value="c">{{ c }}</option>
          </select>
        </div>
      </div>
      <div class="form-group">
        <label>内容</label>
        <textarea v-model="form.content" class="form-textarea" rows="10" placeholder="文章内容..."></textarea>
      </div>
      <div class="form-group">
        <label>状态</label>
        <select v-model="form.status" class="form-input" style="width:180px">
          <option :value="0">草稿</option>
          <option :value="1">发布</option>
        </select>
      </div>
      <div class="form-actions">
        <button class="btn-primary" @click="handleSave">保存</button>
        <button class="btn-outline" @click="showEditor = false">取消</button>
        <span v-if="submitMsg" class="submit-msg" :class="{ error: submitMsg.includes('失败') || submitMsg.includes('请填写') }">
          {{ submitMsg }}
        </span>
      </div>
    </div>

    <!-- 文章列表 -->
    <div class="table-wrap">
      <table class="order-table">
        <thead>
          <tr>
            <th>标题</th><th>分类</th><th>状态</th><th>浏览量</th><th>创建时间</th><th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="a in articles" :key="a.id">
            <td class="article-title-cell">{{ a.title }}</td>
            <td><span class="category-tag">{{ a.category || '-' }}</span></td>
            <td>
              <span class="article-status" :class="a.status === 1 ? 'published' : 'draft'">
                {{ a.status === 1 ? '已发布' : '草稿' }}
              </span>
            </td>
            <td>{{ a.viewCount }}</td>
            <td>{{ a.createTime?.slice(0, 10) }}</td>
            <td class="action-cell">
              <button class="btn-outline-sm" @click="openEdit(a)">编辑</button>
              <button class="btn-confirm" @click="togglePublish(a)">
                {{ a.status === 1 ? '撤回' : '发布' }}
              </button>
              <button class="btn-reject" @click="handleDelete(a.id)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-if="loading" class="empty-state">加载中…</div>
      <div v-else-if="articles.length === 0" class="empty-state">暂无文章，点击"写文章"开始创作</div>
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
.article-title-cell { max-width: 280px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
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
.submit-msg { font-size: 13px; color: #28a745; }
.submit-msg.error { color: #dc3545; }
.empty-state { text-align: center; padding: 60px 20px; color: #999; font-size: 14px; }
</style>
