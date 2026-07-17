<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { getProductById } from '@/api/product'
import { listReviews, submitReview } from '@/api/review'
import type { ProductVO, ProductReviewVO } from '@/types/api'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

onMounted(() => {
  const id = Number(route.params.id)
  if (id) {
    loadProduct(id)
    loadReviews(id)
  }
})

// ===== 商品信息 =====
const product = ref<ProductVO | null>(null)
const loading = ref(true)

async function loadProduct(id: number) {
  loading.value = true
  try {
    const res = await getProductById(id)
    product.value = res.data
    document.title = (res.data?.name || '商品详情') + ' — 码上非遗'
  } catch {
    // ignore
  } finally {
    loading.value = false
  }
}

// ===== 登录状态 =====
const isLoggedIn = computed(() => authStore.isLoggedIn)

// ===== 评价列表 =====
const reviews = ref<ProductReviewVO[]>([])
const reviewLoading = ref(false)

async function loadReviews(productId: number) {
  reviewLoading.value = true
  try {
    const res = await listReviews(productId)
    reviews.value = res.data || []
  } catch {
    // ignore
  } finally {
    reviewLoading.value = false
  }
}

// ===== 评分统计 =====
const avgRating = computed(() => product.value?.avgRating || 0)
const reviewCount = computed(() => product.value?.reviewCount || 0)

/** 渲染星级 */
function renderStars(rating: number): string {
  const full = Math.floor(rating)
  const half = rating - full >= 0.5 ? 1 : 0
  const empty = 5 - full - half
  return '★'.repeat(full) + (half ? '☆' : '') + '☆'.repeat(empty)
}

/** 评分分布（用于展示） */
const starLabels = ['', '很差', '较差', '一般', '较好', '很好']

// ===== 提交评价 =====
const showReviewForm = ref(false)
const reviewForm = ref({ rating: 5, content: '' })
const submitLoading = ref(false)
const submitMsg = ref('')

async function handleSubmitReview() {
  if (!reviewForm.value.content.trim()) {
    submitMsg.value = '请输入评价内容'
    return
  }
  if (!product.value) return
  submitLoading.value = true
  submitMsg.value = ''
  try {
    await submitReview(product.value.id, {
      rating: reviewForm.value.rating,
      content: reviewForm.value.content.trim(),
    })
    submitMsg.value = '评价提交成功！'
    reviewForm.value = { rating: 5, content: '' }
    showReviewForm.value = false
    // 重新加载评价列表和商品信息（更新评分）
    await loadReviews(product.value.id)
    await loadProduct(product.value.id)
  } catch (e: unknown) {
    submitMsg.value = e instanceof Error ? e.message : '提交失败'
  } finally {
    submitLoading.value = false
  }
}
</script>

<template>
  <div class="detail-page">
    <!-- ==================== 顶部导航 ==================== -->
    <header class="page-header">
      <div class="logo" @click="router.push('/')">码上非遗</div>
      <nav class="nav-links">
        <a class="nav-item" @click="router.push('/')">首页</a>
        <a class="nav-item" @click="router.push('/news')">非遗资讯</a>
        <a class="nav-item" @click="router.push('/inheritors')">传承人</a>
        <a class="nav-item active" @click="router.push('/store')">文创周边</a>
        <a class="nav-item" @click="router.push('/')">智能问答</a>
      </nav>
      <div class="header-right">
        <button class="nav-btn" @click="router.push('/store')">← 返回商城</button>
      </div>
    </header>

    <!-- ==================== 主体 ==================== -->
    <main class="detail-main">
      <div v-if="loading" class="loading-state">加载中...</div>

      <template v-else-if="product">
        <!-- 商品信息区 -->
        <div class="product-info-section">
          <div class="product-image-wrap">
            <img
              v-if="product.imageUrl"
              :src="product.imageUrl"
              :alt="product.name"
              class="product-image"
            />
            <div v-else class="img-placeholder">{{ product.category }}</div>
          </div>
          <div class="product-info-right">
            <span class="product-category-tag">{{ product.category }}</span>
            <h1 class="product-title">{{ product.name }}</h1>

            <!-- 综合评分 -->
            <div class="rating-summary">
              <span class="stars-display">{{ renderStars(avgRating) }}</span>
              <span class="avg-score">{{ avgRating.toFixed(1) }}</span>
              <span class="review-count">（{{ reviewCount }} 条评价）</span>
            </div>

            <p class="product-price-detail">¥{{ product.price.toFixed(2) }}</p>
            <p class="product-meta">
              传承人：{{ product.inheritorName }}
              <span v-if="product.stock > 0" class="in-stock">｜ 库存 {{ product.stock }} 件</span>
              <span v-else class="out-stock">｜ 已售罄</span>
            </p>
            <p class="product-desc">{{ product.description }}</p>
          </div>
        </div>

        <!-- 评价区域 -->
        <div class="review-section">
          <div class="review-header">
            <h2>商品评价（{{ reviewCount }}）</h2>
            <button
              v-if="isLoggedIn"
              class="btn-primary"
              @click="showReviewForm = !showReviewForm"
            >
              {{ showReviewForm ? '取消评价' : '写评价' }}
            </button>
            <button v-else class="btn-outline" @click="router.push('/login')">
              登录后评价
            </button>
          </div>

          <!-- 评价表单 -->
          <div v-if="showReviewForm" class="review-form-card">
            <div class="form-item">
              <label>评分</label>
              <div class="star-selector">
                <span
                  v-for="n in 5"
                  :key="n"
                  class="star-option"
                  :class="{ active: n <= reviewForm.rating }"
                  @click="reviewForm.rating = n"
                >★</span>
                <span class="star-label">{{ starLabels[reviewForm.rating] }}</span>
              </div>
            </div>
            <div class="form-item">
              <label>评价内容</label>
              <textarea
                v-model="reviewForm.content"
                class="form-textarea"
                rows="4"
                placeholder="分享你的使用体验..."
              ></textarea>
            </div>
            <div class="form-actions">
              <button class="btn-primary" :disabled="submitLoading" @click="handleSubmitReview">
                {{ submitLoading ? '提交中...' : '提交评价' }}
              </button>
              <span v-if="submitMsg" class="submit-msg" :class="{ error: submitMsg.includes('失败') }">{{ submitMsg }}</span>
            </div>
          </div>

          <!-- 评价列表 -->
          <div v-if="reviewLoading" class="loading-state">加载评价中...</div>
          <div v-else-if="reviews.length === 0" class="empty-reviews">暂无评价，快来写下第一条评价吧</div>
          <div v-else class="review-list">
            <div v-for="review in reviews" :key="review.id" class="review-card">
              <div class="review-top">
                <div class="review-user-avatar">{{ (review.userName || '用')[0] }}</div>
                <div class="review-user-info">
                  <span class="review-user-name">{{ review.userName || '匿名用户' }}</span>
                  <span class="review-time">{{ review.createTime?.slice(0, 10) }}</span>
                </div>
                <span class="review-stars">{{ renderStars(review.rating) }}</span>
              </div>
              <p class="review-content">{{ review.content }}</p>
            </div>
          </div>
        </div>
      </template>

      <div v-else class="empty-state">商品不存在</div>
    </main>

    <!-- ==================== 页脚 ==================== -->
    <footer class="page-footer">
      <p>校园非遗与文创传承人平台 © 2026 码上非遗</p>
    </footer>
  </div>
</template>

<style scoped>
.detail-page {
  min-height: 100vh;
  background: #f8f9fa;
  display: flex;
  flex-direction: column;
}

/* ---- 导航 ---- */
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
.detail-main {
  flex: 1;
  max-width: 1000px;
  margin: 0 auto;
  padding: 40px 20px;
}

/* ---- 商品信息 ---- */
.product-info-section {
  display: flex;
  gap: 40px;
  background: #fff;
  border-radius: 12px;
  padding: 32px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.06);
  margin-bottom: 40px;
}
.product-image-wrap {
  width: 400px;
  height: 400px;
  flex-shrink: 0;
  border-radius: 10px;
  overflow: hidden;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
}
.product-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.img-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48px;
  color: #c41e3a;
  background: linear-gradient(135deg, #ffe9ec, #fef0f2);
}
.product-info-right {
  flex: 1;
  display: flex;
  flex-direction: column;
}
.product-category-tag {
  display: inline-block;
  padding: 4px 12px;
  background: #fff5f7;
  color: #c41e3a;
  border-radius: 12px;
  font-size: 13px;
  font-weight: 600;
  width: fit-content;
  margin-bottom: 12px;
}
.product-title {
  font-size: 24px;
  color: #222;
  margin-bottom: 12px;
}
.rating-summary {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}
.stars-display {
  color: #f5a623;
  font-size: 20px;
  letter-spacing: 2px;
}
.avg-score {
  font-size: 20px;
  font-weight: bold;
  color: #c41e3a;
}
.review-count {
  font-size: 14px;
  color: #999;
}
.product-price-detail {
  font-size: 32px;
  font-weight: bold;
  color: #c41e3a;
  margin-bottom: 12px;
}
.product-meta {
  font-size: 14px;
  color: #666;
  margin-bottom: 16px;
}
.in-stock { color: #28a745; }
.out-stock { color: #dc3545; }
.product-desc {
  font-size: 15px;
  color: #555;
  line-height: 1.8;
  white-space: pre-wrap;
}

/* ---- 评价区域 ---- */
.review-section {
  background: #fff;
  border-radius: 12px;
  padding: 32px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.06);
}
.review-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 2px solid #f0f0f0;
}
.review-header h2 {
  font-size: 20px;
  color: #333;
}

/* ---- 评价表单 ---- */
.review-form-card {
  background: #fafafa;
  border-radius: 10px;
  padding: 24px;
  margin-bottom: 24px;
}
.form-item { margin-bottom: 16px; }
.form-item label { display: block; font-size: 14px; color: #444; margin-bottom: 8px; font-weight: 600; }
.star-selector { display: flex; align-items: center; gap: 4px; }
.star-option {
  font-size: 32px;
  color: #ddd;
  cursor: pointer;
  transition: 0.15s;
  user-select: none;
}
.star-option.active { color: #f5a623; }
.star-option:hover { color: #f5a623; transform: scale(1.1); }
.star-label { font-size: 14px; color: #888; margin-left: 12px; }
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

/* ---- 评价列表 ---- */
.review-list { display: flex; flex-direction: column; gap: 16px; }
.review-card {
  padding: 20px;
  border: 1px solid #f0f0f0;
  border-radius: 10px;
  transition: 0.2s;
}
.review-card:hover { border-color: #f0c0c8; background: #fffafa; }
.review-top {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.review-user-avatar {
  width: 40px; height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #e8647a, #c41e3a);
  color: #fff;
  display: flex; align-items: center; justify-content: center;
  font-size: 18px; font-weight: 700;
  flex-shrink: 0;
}
.review-user-info {
  flex: 1;
  display: flex; flex-direction: column;
}
.review-user-name { font-size: 15px; color: #333; font-weight: 600; }
.review-time { font-size: 12px; color: #999; margin-top: 2px; }
.review-stars {
  color: #f5a623;
  font-size: 16px;
  letter-spacing: 1px;
}
.review-content {
  font-size: 14px;
  color: #555;
  line-height: 1.7;
  padding-left: 52px;
}

.empty-reviews {
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
  .product-info-section { flex-direction: column; padding: 20px; }
  .product-image-wrap { width: 100%; height: 280px; }
  .page-header { padding: 0 16px; flex-wrap: wrap; }
  .nav-links { gap: 12px; }
  .nav-item { font-size: 12px; }
}
</style>
