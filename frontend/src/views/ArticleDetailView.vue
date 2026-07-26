<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { getArticleById } from '@/api/knowledge'
import { listComments, submitComment, deleteComment } from '@/api/comment'
import type { KnowledgeArticleVO, ArticleCommentVO } from '@/types/api'
import CommentItem from '@/components/CommentItem.vue'
import { useHistory } from '@/composables/useHistory'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const { record } = useHistory()

onMounted(() => {
  const id = Number(route.params.id)
  if (id) {
    loadArticle(id)
    loadComments(id)
  }
})

// ===== 文章信息 =====
const article = ref<KnowledgeArticleVO | null>(null)
const loading = ref(true)

async function loadArticle(id: number) {
  loading.value = true
  try {
    const res = await getArticleById(id)
    article.value = res.data
    document.title = (res.data?.title || '文章详情') + ' — 码上非遗'
    // 自动记录浏览历史
    if (res.data) {
      record({
        targetType: 'news',
        targetId: res.data.id!,
        targetTitle: res.data.title || '文章',
        targetCover: res.data.coverUrl || '',
        targetPath: `/news/${res.data.id}`,
      })
    }
  } catch {
    // ignore
  } finally {
    loading.value = false
  }
}

// ===== 登录状态 =====
const isLoggedIn = computed(() => authStore.isLoggedIn)

// ===== 评论列表 =====
const comments = ref<ArticleCommentVO[]>([])
const commentLoading = ref(false)

async function loadComments(articleId: number) {
  commentLoading.value = true
  try {
    const res = await listComments(articleId)
    comments.value = res.data || []
  } catch {
    // ignore
  } finally {
    commentLoading.value = false
  }
}

/** 格式化文章内容（将 \n 转为 <br> 以便渲染） */
const contentHtml = computed(() => {
  if (!article.value?.content) return ''
  return article.value.content.replace(/\n/g, '<br>')
})

// ===== 提交评论 =====
const showCommentForm = ref(false)
const commentForm = ref({ content: '' })
const submitLoading = ref(false)
const submitMsg = ref('')

// ===== 回复处理 =====
async function handleReplyClick(parentId: number, content: string) {
  if (!article.value) return
  try {
    await submitComment(article.value.id, {
      content,
      parentId,
    })
    await loadComments(article.value.id)
  } catch (e: unknown) {
    alert(e instanceof Error ? e.message : '回复失败')
  }
}

async function handleSubmitComment() {
  if (!commentForm.value.content.trim()) {
    submitMsg.value = '请输入评论内容'
    return
  }
  if (!article.value) return
  submitLoading.value = true
  submitMsg.value = ''
  try {
    await submitComment(article.value.id, {
      content: commentForm.value.content.trim(),
    })
    submitMsg.value = '评论提交成功！'
    commentForm.value = { content: '' }
    showCommentForm.value = false
    await loadComments(article.value.id)
  } catch (e: unknown) {
    submitMsg.value = e instanceof Error ? e.message : '提交失败'
  } finally {
    submitLoading.value = false
  }
}

async function handleDeleteComment(commentId: number) {
  try {
    await deleteComment(commentId)
    await loadComments(article.value!.id)
  } catch (e: unknown) {
    alert(e instanceof Error ? e.message : '删除失败')
  }
}
</script>

<template>
  <div class="article-page">
    <div class="back-bar">
      <button class="nav-btn" @click="router.push('/news')">← 返回资讯列表</button>
    </div>

    <!-- ==================== 主体 ==================== -->
    <main class="article-main">
      <div v-if="loading" class="loading-state">加载中...</div>

      <template v-else-if="article">
        <!-- 文章内容区 -->
        <div class="article-section">
          <!-- 封面图 -->
          <div v-if="article.coverUrl" class="article-cover">
            <img :src="article.coverUrl" :alt="article.title" />
          </div>
          <div class="article-header">
            <span class="article-category-tag">{{ article.category }}</span>
            <h1 class="article-title">{{ article.title }}</h1>
            <div class="article-meta">
              <span>✍️ {{ article.authorName }}</span>
              <span>📅 {{ article.createTime?.slice(0, 10) }}</span>
              <span>👁 {{ article.viewCount }} 浏览</span>
              <span>💬 {{ comments.length }} 评论</span>
            </div>
          </div>
          <div class="article-body" v-html="contentHtml"></div>
        </div>

        <!-- 评论区域 -->
        <div class="comment-section">
          <div class="comment-header">
            <h2>评论（{{ comments.length }}）</h2>
            <button
              v-if="isLoggedIn"
              class="btn-primary"
              @click="showCommentForm = !showCommentForm"
            >
              {{ showCommentForm ? '取消评论' : '写评论' }}
            </button>
            <button v-else class="btn-outline" @click="router.push('/login')">
              登录后评论
            </button>
          </div>

          <!-- 评论表单 -->
          <div v-if="showCommentForm" class="comment-form-card">
            <div class="form-item">
              <label>评论内容</label>
              <textarea
                v-model="commentForm.content"
                class="form-textarea"
                rows="4"
                placeholder="写下你的想法..."
              ></textarea>
            </div>
            <div class="form-actions">
              <button class="btn-primary" :disabled="submitLoading" @click="handleSubmitComment">
                {{ submitLoading ? '提交中...' : '提交评论' }}
              </button>
              <span v-if="submitMsg" class="submit-msg" :class="{ error: submitMsg.includes('失败') }">{{ submitMsg }}</span>
            </div>
          </div>

          <!-- 评论列表 -->
          <div v-if="commentLoading" class="loading-state">加载评论中...</div>
          <div v-else-if="comments.length === 0" class="empty-comments">暂无评论，快来发表第一条评论吧</div>
          <div v-else class="comment-list">
            <CommentItem
              v-for="comment in comments"
              :key="comment.id"
              :comment="comment"
              :current-user-id="authStore.userInfo?.id ?? null"
              :depth="0"
              @reply="handleReplyClick"
              @delete="handleDeleteComment"
            />
          </div>
        </div>
      </template>

      <div v-else class="empty-state">文章不存在</div>
    </main>

    <!-- ==================== 页脚 ==================== -->
    <footer class="page-footer">
      <p>校园非遗与文创传承人平台 © 2026 码上非遗</p>
    </footer>
  </div>
</template>

<style scoped>
.article-page {
  min-height: 100vh;
  background: #f8f9fa;
  display: flex;
  flex-direction: column;
}

/* ---- 返回栏 ---- */
.back-bar { padding: 14px 5%; background: #f8f9fa; }
.back-bar .nav-btn { padding: 8px 18px; font-size: 13px; color: #fff; background: #c41e3a; border: none; border-radius: 6px; cursor: pointer; transition: 0.2s; box-shadow: 0 2px 6px rgba(196,30,58,0.25); }
.back-bar .nav-btn:hover { background: #a8162b; }

/* ---- 旧导航（保留备用） ---- */
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
.logo { font-size: 20px; font-weight: bold; cursor: pointer; }
.nav-links { display: flex; align-items: center; gap: 20px; }
.nav-item {
  color: #fff; text-decoration: none; font-size: 14px; cursor: pointer;
  padding: 4px 0; border-bottom: 2px solid transparent; transition: border-color 0.2s;
}
.nav-item:hover, .nav-item.active { border-bottom-color: #fff; }
.header-right { display: flex; align-items: center; gap: 12px; }
.nav-btn {
  padding: 8px 18px; font-size: 13px; color: #fff;
  background: rgba(255,255,255,0.15); border: 1px solid rgba(255,255,255,0.3);
  border-radius: 6px; cursor: pointer; transition: 0.2s;
}
.nav-btn:hover { background: rgba(255,255,255,0.25); }

/* ---- 主体 ---- */
.article-main {
  flex: 1;
  max-width: 860px;
  margin: 0 auto;
  padding: 40px 20px;
}

/* ---- 文章内容 ---- */
.article-section {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 10px rgba(0,0,0,0.06);
  margin-bottom: 40px;
}
.article-cover {
  width: 100%;
  max-height: 400px;
  overflow: hidden;
}
.article-cover img {
  width: 100%;
  height: 100%;
  max-height: 400px;
  object-fit: cover;
  display: block;
}
.article-header {
  padding: 40px 40px 0 40px;
  border-bottom: 1px solid #f0f0f0;
  padding-bottom: 24px;
  margin-bottom: 28px;
}
.article-category-tag {
  display: inline-block;
  padding: 4px 14px;
  background: #fff5f7;
  color: #c41e3a;
  border-radius: 12px;
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 12px;
}
.article-title {
  font-size: 28px;
  color: #222;
  line-height: 1.4;
  margin-bottom: 16px;
}
.article-meta {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
  font-size: 14px;
  color: #999;
}
.article-body {
  font-size: 16px;
  color: #444;
  line-height: 2;
  word-break: break-word;
  padding: 0 40px 40px 40px;
}

/* ---- 评论区域 ---- */
.comment-section {
  background: #fff;
  border-radius: 12px;
  padding: 32px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.06);
}
.comment-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 2px solid #f0f0f0;
}
.comment-header h2 {
  font-size: 20px;
  color: #333;
}

/* ---- 评论表单 ---- */
.comment-form-card {
  background: #fafafa;
  border-radius: 10px;
  padding: 24px;
  margin-bottom: 24px;
}
.form-item { margin-bottom: 16px; }
.form-item label { display: block; font-size: 14px; color: #444; margin-bottom: 8px; font-weight: 600; }
.form-textarea {
  width: 100%;
  padding: 14px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  resize: vertical;
  box-sizing: border-box;
  outline: none;
  font-family: inherit;
  line-height: 1.7;
}
.form-textarea:focus { border-color: #c41e3a; box-shadow: 0 0 0 3px rgba(196,30,58,0.08); }
.form-actions { display: flex; align-items: center; gap: 12px; }
.submit-msg { font-size: 13px; color: #28a745; }
.submit-msg.error { color: #dc3545; }

/* ---- 评论列表 ---- */
.comment-list { display: flex; flex-direction: column; gap: 4px; }

.empty-comments {
  text-align: center;
  padding: 40px 0;
  color: #999;
  font-size: 15px;
}

/* ---- 通用 ---- */
.loading-state { text-align: center; padding: 80px 0; color: #999; }
.empty-state { text-align: center; padding: 80px 0; color: #999; font-size: 16px; }

.btn-primary {
  padding: 10px 24px; font-size: 14px; color: #fff;
  background: #c41e3a; border: none; border-radius: 6px;
  cursor: pointer; font-weight: bold;
}
.btn-primary:hover { background: #a8162b; }
.btn-primary:disabled { opacity: 0.6; cursor: not-allowed; }
.btn-outline {
  padding: 8px 20px; font-size: 13px; color: #c41e3a;
  background: #fff; border: 1px solid #c41e3a;
  border-radius: 6px; cursor: pointer;
}
.btn-outline:hover { background: #fff5f7; }

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
  .article-section { padding: 24px; }
  .article-title { font-size: 22px; }
  .article-meta { gap: 12px; font-size: 13px; }
  .page-header { padding: 0 16px; flex-wrap: wrap; }
  .nav-links { gap: 12px; }
  .nav-item { font-size: 12px; }
}
</style>