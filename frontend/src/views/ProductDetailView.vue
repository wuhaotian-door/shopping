<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useCartStore } from '@/stores/cart'
import { getProductById, submitProductOrder } from '@/api/product'
import { listReviews, submitReview, deleteReview } from '@/api/review'
import type { ProductVO, ProductReviewVO } from '@/types/api'
import CommentItem from '@/components/CommentItem.vue'
import Product3dViewer from '@/components/Product3dViewer.vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const cartStore = useCartStore()

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

// ===== 回复处理 =====
async function handleReplyClick(parentId: number, content: string) {
  if (!product.value) return
  try {
    await submitReview(product.value.id, {
      content,
      parentId,
    })
    await loadReviews(product.value.id)
    await loadProduct(product.value.id)
  } catch (e: unknown) {
    alert(e instanceof Error ? e.message : '回复失败')
  }
}

async function handleDeleteReview(reviewId: number) {
  try {
    await deleteReview(reviewId)
    await loadReviews(product.value!.id)
    await loadProduct(product.value!.id)
  } catch (e: unknown) {
    alert(e instanceof Error ? e.message : '删除失败')
  }
}

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

// ===== 加入购物车 =====
function addToCart() {
  if (!isLoggedIn.value) { router.push('/login'); return }
  if (!product.value || product.value.stock === 0) return
  cartStore.add(product.value, authStore.uid)
  cartToast.value = '已加入购物车'
  showToast()
}

// ===== 立即购买 =====
const showBuyForm = ref(false)
const buyForm = ref({ phone: '', address: '', remark: '' })
const buyQuantity = ref(1)
const buySubmitting = ref(false)
const buyMsg = ref('')

function buyNow() {
  if (!isLoggedIn.value) { router.push('/login'); return }
  if (!product.value || product.value.stock === 0) return
  buyQuantity.value = 1
  showBuyForm.value = true
  buyMsg.value = ''
}

function incQty() {
  if (product.value && buyQuantity.value < product.value.stock) buyQuantity.value++
}
function decQty() {
  if (buyQuantity.value > 1) buyQuantity.value--
}

async function submitBuy() {
  if (!product.value) return
  if (!buyForm.value.phone || !buyForm.value.address) {
    buyMsg.value = '请填写联系电话和收货地址'
    return
  }
  buySubmitting.value = true
  buyMsg.value = ''
  try {
    await submitProductOrder({
      productId: product.value.id,
      quantity: buyQuantity.value,
      phone: buyForm.value.phone,
      address: buyForm.value.address,
      remark: buyForm.value.remark,
    })
    buyMsg.value = '下单成功！'
    setTimeout(() => {
      showBuyForm.value = false
      router.push('/orders')
    }, 1200)
  } catch {
    buyMsg.value = '下单失败，请稍后重试'
  } finally {
    buySubmitting.value = false
  }
}

// ===== 轻提示 =====
const cartToast = ref('')
const show3d = ref(false)
const showVideo = ref(false)
let toastTimer: ReturnType<typeof setTimeout> | null = null
const modelUrl = computed(() => product.value?.modelUrl || '/models/a_chinese_tongguan_kiln_teapot-1.glb')
function showToast() {
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => { cartToast.value = '' }, 2000)
}

// ===== 侧边栏导航 =====
function goTo(path: string) {
  if (!isLoggedIn.value) { router.push('/login'); return }
  router.push(path)
}
</script>

<template>
  <div class="detail-page">
    <div class="back-bar">
      <button class="nav-btn" @click="router.push('/store')">← 返回商城</button>
    </div>

    <!-- ==================== 主体 ==================== -->
    <main class="detail-main">
      <div v-if="loading" class="loading-state">加载中...</div>

      <template v-else-if="product">
        <!-- 商品信息区 -->
        <div class="product-info-section">
          <div class="product-image-wrap">
            <!-- 3D 视图 -->
            <Product3dViewer v-if="show3d && modelUrl" :model-url="modelUrl" :poster-image="product.imageUrl || ''" :product-name="product.name" :auto-rotate="true" credit="CC BY 4.0 · 中国潼关窑茶壶" />
            <!-- 视频播放 -->
            <template v-else-if="showVideo && product.videoUrl">
              <video :src="product.videoUrl" controls playsinline class="product-video"></video>
            </template>
            <!-- 默认图片 -->
            <template v-else>
              <img v-if="product.imageUrl" :src="product.imageUrl" :alt="product.name" class="product-image" />
              <div v-else class="img-placeholder">{{ product.category }}</div>
            </template>
            <!-- 悬浮按钮 -->
            <div class="float-btns">
              <template v-if="!show3d && !showVideo">
                <button class="float-btn float-btn-3d" @click="show3d = true">🧧 查看文物3D模型</button>
                <button v-if="product.videoUrl" class="float-btn float-btn-video" @click="showVideo = true">▶ 查看文物视频</button>
                <span v-else class="float-no-video">目前暂无视频喔</span>
              </template>
              <button v-else class="float-btn float-btn-back" @click="show3d = false; showVideo = false">✕ 返回图片</button>
            </div>
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

            <!-- 操作按钮 -->
            <div class="action-bar">
              <button
                class="btn-add-cart"
                :disabled="product.stock === 0"
                @click="addToCart"
              >{{ product.stock === 0 ? '已售罄' : '加入购物车' }}</button>
              <button
                class="btn-buy-now"
                :disabled="product.stock === 0"
                @click="buyNow"
              >立即购买</button>
            </div>
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
            <CommentItem
              v-for="review in reviews"
              :key="review.id"
              :comment="review"
              :current-user-id="authStore.userInfo?.id ?? null"
              :depth="0"
              @reply="handleReplyClick"
              @delete="handleDeleteReview"
            >
              <template #extra="{ comment }">
                <span v-if="(comment as ProductReviewVO).rating != null" class="review-stars">{{ renderStars((comment as ProductReviewVO).rating!) }}</span>
              </template>
            </CommentItem>
          </div>
        </div>
      </template>

      <div v-else class="empty-state">商品不存在</div>
    </main>

    <!-- ===== 右侧悬浮侧边栏（仿淘宝） ===== -->
    <aside class="side-toolbar">
      <button class="side-btn" @click="goTo('/cart')" title="购物车">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="9" cy="21" r="1.2"/>
          <circle cx="18" cy="21" r="1.2"/>
          <path d="M2.5 3h2.2l2.4 12.2a2 2 0 0 0 2 1.6h8.7a2 2 0 0 0 2-1.5L21.5 7H6"/>
        </svg>
        <span class="side-label">购物车</span>
        <span v-if="cartStore.count > 0" class="side-badge">{{ cartStore.count }}</span>
      </button>

      <button class="side-btn" @click="goTo('/orders')" title="我的订单">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <path d="M14 3H6a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z"/>
          <polyline points="14 3 14 9 20 9"/>
          <line x1="8" y1="13" x2="16" y2="13"/>
          <line x1="8" y1="17" x2="13" y2="17"/>
        </svg>
        <span class="side-label">我的订单</span>
      </button>

      <button class="side-btn" @click="goTo('/profile')" title="个人中心">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="8" r="4"/>
          <path d="M4 21c0-4.4 3.6-8 8-8s8 3.6 8 8"/>
        </svg>
        <span class="side-label">个人中心</span>
      </button>
    </aside>

    <!-- ===== 加入购物车轻提示 ===== -->
    <transition name="toast">
      <div v-if="cartToast" class="cart-toast">{{ cartToast }}</div>
    </transition>

    <!-- ===== 立即购买弹窗 ===== -->
    <div v-if="showBuyForm" class="modal-overlay" @click.self="showBuyForm = false">
      <div class="modal-box">
        <h3>确认订单</h3>
        <div v-if="product" class="modal-product">
          <span class="modal-product-name">{{ product.name }}</span>
          <span class="modal-product-price">¥{{ product.price.toFixed(2) }} × {{ buyQuantity }}</span>
        </div>
        <!-- 数量选择器 -->
        <div class="qty-row">
          <span class="qty-label">数量</span>
          <div class="qty-selector">
            <button class="qty-btn" :disabled="buyQuantity <= 1" @click="decQty">−</button>
            <span class="qty-val">{{ buyQuantity }}</span>
            <button class="qty-btn" :disabled="!product || buyQuantity >= product.stock" @click="incQty">+</button>
          </div>
          <span class="qty-stock">库存 {{ product?.stock ?? 0 }} 件</span>
        </div>
        <input v-model="buyForm.phone" placeholder="联系电话（必填）" class="field" />
        <input v-model="buyForm.address" placeholder="收货地址（必填）" class="field" />
        <input v-model="buyForm.remark" placeholder="备注（可选）" class="field" />
        <p v-if="buyMsg" :class="buyMsg.includes('成功') ? 'msg-ok' : 'msg-err'">{{ buyMsg }}</p>
        <div class="modal-btns">
          <button class="btn-cancel" @click="showBuyForm = false">取消</button>
          <button class="btn-submit" :disabled="buySubmitting" @click="submitBuy">{{ buySubmitting ? '提交中…' : '确认下单' }}</button>
        </div>
      </div>
    </div>

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

/* ---- 返回栏 ---- */
.back-bar { padding: 14px 5%; background: #f8f9fa; }
.back-bar .nav-btn {
  padding: 8px 18px; font-size: 14px; color: #fff;
  background: #c41e3a; border: none;
  border-radius: 6px; cursor: pointer; transition: 0.2s;
  font-weight: 600; box-shadow: 0 2px 6px rgba(196,30,58,0.25);
}
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
  position: relative;
  position: relative;
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
.btn-3d-toggle { position: absolute; top: 10px; right: 10px; z-index: 20; padding: 6px 14px; font-size: 12px; color: #fff; background: rgba(196, 30, 58, 0.85); border: none; border-radius: 6px; cursor: pointer; font-weight: 600; backdrop-filter: blur(4px); transition: all 0.2s; }
.btn-3d-toggle:hover { background: #c41e3a; box-shadow: 0 2px 8px rgba(196, 30, 58, 0.35); }

.btn-3d-toggle {
  position: absolute;
  top: 10px;
  right: 10px;
  z-index: 20;
  padding: 6px 14px;
  font-size: 12px;
  color: #fff;
  background: rgba(196, 30, 58, 0.85);
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 600;
  backdrop-filter: blur(4px);
  transition: all 0.2s;
}
.btn-3d-toggle:hover {
  background: #c41e3a;
  box-shadow: 0 2px 8px rgba(196, 30, 58, 0.35);
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
.review-list { display: flex; flex-direction: column; gap: 4px; }

.review-stars {
  color: #f5a623;
  font-size: 16px;
  letter-spacing: 1px;
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

/* ---- 操作按钮 ---- */
.action-bar {
  display: flex;
  gap: 16px;
  margin-top: 28px;
}
.btn-add-cart {
  flex: 1;
  padding: 14px 0;
  font-size: 16px;
  color: #c41e3a;
  background: #fff;
  border: 2px solid #c41e3a;
  border-radius: 8px;
  cursor: pointer;
  font-weight: bold;
  transition: 0.2s;
}
.btn-add-cart:hover:not(:disabled) {
  background: #fff5f7;
}
.btn-add-cart:disabled {
  border-color: #ccc;
  color: #ccc;
  cursor: not-allowed;
}
.btn-buy-now {
  flex: 1;
  padding: 14px 0;
  font-size: 16px;
  color: #fff;
  background: linear-gradient(135deg, #c41e3a, #d43030);
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-weight: bold;
  transition: 0.2s;
  box-shadow: 0 2px 8px rgba(196,30,58,0.3);
}
.btn-buy-now:hover:not(:disabled) {
  background: linear-gradient(135deg, #a8162b, #c41e3a);
  box-shadow: 0 4px 12px rgba(196,30,58,0.45);
}
.btn-buy-now:disabled {
  background: #ccc;
  box-shadow: none;
  cursor: not-allowed;
}

/* ===== 右侧悬浮侧边栏（仿淘宝） ===== */
.side-toolbar {
  position: fixed; right: 16px; top: 50%;
  transform: translateY(-50%); z-index: 500;
  display: flex; flex-direction: column; gap: 2px;
  background: #fff; border-radius: 10px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.10);
  padding: 6px 0; width: 60px;
}
.side-btn {
  position: relative;
  display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  padding: 12px 0; gap: 4px;
  background: transparent; border: none; cursor: pointer;
  color: #666; transition: color 0.2s, background 0.2s;
}
.side-btn:hover { color: #c41e3a; background: #fff5f7; }
.side-btn svg { width: 24px; height: 24px; }
.side-label { font-size: 12px; line-height: 1; user-select: none; }
.side-badge {
  position: absolute; top: 6px; right: 10px;
  min-width: 18px; height: 18px; padding: 0 5px;
  border-radius: 9px; background: #ff4400; color: #fff;
  font-size: 11px; font-weight: 700;
  display: flex; align-items: center; justify-content: center;
  box-sizing: border-box; line-height: 1;
}

/* ---- 轻提示 ---- */
.cart-toast {
  position: fixed; bottom: 80px; left: 50%;
  transform: translateX(-50%);
  background: rgba(0,0,0,0.78);
  color: #fff; padding: 12px 28px;
  border-radius: 24px; font-size: 14px;
  z-index: 600; pointer-events: none;
  box-shadow: 0 4px 16px rgba(0,0,0,0.2);
}
.toast-enter-active, .toast-leave-active { transition: opacity 0.3s, transform 0.3s; }
.toast-enter-from, .toast-leave-to { opacity: 0; transform: translateX(-50%) translateY(10px); }

/* ---- 立即购买弹窗 ---- */
.modal-overlay {
  position: fixed; inset: 0;
  background: rgba(0,0,0,0.35);
  display: flex; align-items: center; justify-content: center;
  z-index: 700;
}
.modal-box {
  background: #fff; border-radius: 12px;
  padding: 32px; width: 420px; max-width: 90vw;
}
.modal-box h3 { margin-bottom: 16px; color: #c41e3a; font-size: 20px; }
.modal-product {
  display: flex; justify-content: space-between; align-items: center;
  padding: 12px 14px; background: #fafafa; border-radius: 8px;
  margin-bottom: 16px;
}
.modal-product-name { font-size: 14px; color: #333; font-weight: 600; }
.modal-product-price { font-size: 14px; color: #c41e3a; font-weight: 600; }

/* ---- 数量选择器 ---- */
.qty-row {
  display: flex; align-items: center; gap: 14px;
  margin-bottom: 16px;
}
.qty-label { font-size: 14px; color: #444; font-weight: 600; }
.qty-selector {
  display: flex; align-items: center;
  border: 1px solid #e0e0e0; border-radius: 6px; overflow: hidden;
}
.qty-btn {
  width: 36px; height: 36px;
  border: none; background: #f5f5f5;
  font-size: 18px; cursor: pointer; color: #333;
  transition: background 0.15s;
}
.qty-btn:hover:not(:disabled) { background: #e8e8e8; }
.qty-btn:disabled { color: #ccc; cursor: not-allowed; }
.qty-val {
  width: 48px; text-align: center;
  font-size: 16px; font-weight: 600; color: #333;
}
.qty-stock { font-size: 13px; color: #999; }

/* ---- 表单 ---- */
.field {
  width: 100%; padding: 10px 14px;
  border: 1px solid #ddd; border-radius: 6px;
  margin-bottom: 12px; font-size: 14px; outline: none;
  box-sizing: border-box;
}
.field:focus { border-color: #c41e3a; }
.msg-ok { color: #28a745; font-size: 13px; }
.msg-err { color: #c41e3a; font-size: 13px; }
.modal-btns { display: flex; gap: 12px; justify-content: flex-end; margin-top: 16px; }
.btn-cancel { padding: 10px 20px; border: 1px solid #ddd; border-radius: 6px; background: #fff; cursor: pointer; font-size: 14px; }
.btn-submit { padding: 10px 24px; background: #c41e3a; color: #fff; border: none; border-radius: 6px; cursor: pointer; font-weight: 600; font-size: 14px; }
.btn-submit:disabled { opacity: 0.5; cursor: not-allowed; }

/* ---- 响应式 ---- */
@media (max-width: 768px) {
  .product-info-section { flex-direction: column; padding: 20px; }
  .product-image-wrap { width: 100%; height: 280px; }
  .page-header { padding: 0 16px; flex-wrap: wrap; }
  .nav-links { gap: 12px; }
  .nav-item { font-size: 12px; }
  .action-bar { flex-direction: column; gap: 10px; }
  .side-toolbar { right: 6px; width: 50px; }
  .side-btn { padding: 10px 0; }
  .side-btn svg { width: 20px; height: 20px; }
  .side-label { font-size: 11px; }
  .modal-box { padding: 24px; }
}
.media-carousel { width: 100%; min-height: 300px; position: relative; }
.carousel-slide { width: 100%; animation: carouselFade 0.35s ease-out; }
@keyframes carouselFade { 0%{opacity:0} 100%{opacity:1} }
.product-video { width: 100%; max-height: 420px; object-fit: contain; border-radius: 8px; background: #000; }
.carousel-dots { display: flex; gap: 8px; margin-top: 10px; justify-content: center; }
.carousel-dot { width: 32px; height: 24px; border-radius: 4px; background: #f0f0f0; color: #999; display: flex; align-items: center; justify-content: center; font-size: 13px; cursor: pointer; transition: all 0.2s; border: 1px solid #e0e0e0; }
.carousel-dot:hover { border-color: #c41e3a; color: #c41e3a; }
.carousel-dot.active { background: #c41e3a; color: #fff; border-color: #c41e3a; }
.float-btns { position: absolute; bottom: 12px; left: 12px; right: 12px; z-index: 20; display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.float-btn { padding: 10px 20px; font-size: 14px; color: #fff; border: none; border-radius: 8px; cursor: pointer; font-weight: 600; backdrop-filter: blur(6px); transition: all 0.2s; letter-spacing: 1px; font-family: inherit; white-space: nowrap; }
.float-btn-3d { background: rgba(196,30,58,0.88); }
.float-btn-3d:hover { background: #c41e3a; box-shadow: 0 3px 12px rgba(196,30,58,0.4); transform: translateY(-1px); }
.float-btn-video { background: rgba(180,120,50,0.88); }
.float-btn-video:hover { background: #8b6914; box-shadow: 0 3px 12px rgba(180,120,50,0.4); transform: translateY(-1px); }
.float-btn-back { background: rgba(100,100,100,0.85); }
.float-btn-back:hover { background: #555; box-shadow: 0 3px 12px rgba(0,0,0,0.3); transform: translateY(-1px); }
.float-no-video { padding: 10px 16px; font-size: 13px; color: rgba(255,255,255,0.7); background: rgba(0,0,0,0.35); border-radius: 8px; backdrop-filter: blur(4px); letter-spacing: 1px; white-space: nowrap; }
</style>
