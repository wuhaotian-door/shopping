<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useCartStore } from '@/stores/cart'
import { getProductOrderDetail, cancelProductOrder, completeProductOrder } from '@/api/product'
import { submitReview, listReviews } from '@/api/review'
import type { ProductOrderVO, ProductReviewVO } from '@/types/api'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const cartStore = useCartStore()

const orderId = Number(route.params.id)
const isLoggedIn = computed(() => authStore.isLoggedIn)
const isInheritor = computed(() => authStore.isInheritor)
const currentUserId = computed(() => authStore.userInfo?.id || 0)

const order = ref<ProductOrderVO | null>(null)
const loading = ref(true)
const actionLoading = ref(false)
const actionMsg = ref('')

// 取消确认弹窗
const showCancelConfirm = ref(false)

// 评价功能
const showReviewForm = ref(false)
const reviewRating = ref(5)
const reviewContent = ref('')
const reviewLoading = ref(false)
const reviewMsg = ref('')
const existingReviews = ref<ProductReviewVO[]>([])

const statusMap: Record<number, { label: string; cls: string }> = {
  0: { label: '待发货', cls: 'status-pending' },
  1: { label: '已发货', cls: 'status-shipped' },
  2: { label: '已完成', cls: 'status-completed' },
  3: { label: '已取消', cls: 'status-cancelled' },
}

onMounted(async () => {
  document.title = '订单详情 — 码上非遗'
  await loadOrder()
})

async function loadOrder() {
  loading.value = true
  try {
    const res = await getProductOrderDetail(orderId)
    order.value = res.data
    if (order.value) {
      await loadReviews()
    }
  } catch {
    actionMsg.value = '获取订单详情失败'
  } finally {
    loading.value = false
  }
}

async function loadReviews() {
  if (!order.value) return
  try {
    const res = await listReviews(order.value.productId)
    existingReviews.value = res.data || []
  } catch { /* */ }
}

// 当前用户是否已评价该商品
const hasReviewed = computed(() =>
  existingReviews.value.some(r => r.userId === currentUserId.value)
)

// 用户能否取消（待发货 + 是下单用户）
const canCancel = computed(() =>
  order.value?.status === 0 && order.value?.userId === currentUserId.value
)

// 用户能否确认收货（已发货 + 是下单用户 + 不是传承人）
const canConfirmReceive = computed(() =>
  !isInheritor.value &&
  order.value?.status === 1 &&
  order.value?.userId === currentUserId.value
)

async function handleCancel() {
  if (!order.value) return
  actionLoading.value = true
  try {
    await cancelProductOrder(order.value.id)
    actionMsg.value = '订单已取消'
    showCancelConfirm.value = false
    await loadOrder()
  } catch {
    actionMsg.value = '取消失败'
  } finally {
    actionLoading.value = false
  }
}

async function handleConfirmReceive() {
  if (!order.value) return
  if (!confirm('确认已收到商品？确认后订单将变为已完成。')) return
  actionLoading.value = true
  try {
    await completeProductOrder(order.value.id)
    actionMsg.value = '确认收货成功'
    await loadOrder()
  } catch {
    actionMsg.value = '确认收货失败'
  } finally {
    actionLoading.value = false
  }
}

async function handleSubmitReview() {
  if (!order.value) return
  if (!reviewContent.value.trim()) {
    reviewMsg.value = '请输入评价内容'
    return
  }
  reviewLoading.value = true
  reviewMsg.value = ''
  try {
    await submitReview(order.value.productId, {
      rating: reviewRating.value,
      content: reviewContent.value.trim(),
    })
    reviewMsg.value = '评价提交成功！'
    reviewContent.value = ''
    reviewRating.value = 5
    showReviewForm.value = false
    await loadReviews()
  } catch {
    reviewMsg.value = '评价提交失败'
  } finally {
    reviewLoading.value = false
  }
}

// 侧边栏导航
const sideItems = [
  { key: 'cart', label: '购物车', to: '/cart' },
  { key: 'orders', label: '我的订单', to: '/orders' },
  { key: 'profile', label: '个人中心', to: '/profile' },
]
function goTo(path: string) {
  if (!isLoggedIn.value) { router.push('/login'); return }
  router.push(path)
}

// 返回按钮逻辑：传承人返回订单管理 tab，普通用户返回上一页
function goBack() {
  if (isInheritor.value) {
    router.push('/inheritor?tab=productOrders')
  } else {
    router.back()
  }
}
</script>

<template>
  <!-- ==================== 返回按钮 ==================== -->
  <a @click="goBack" class="back-btn">← 返回</a>

  <!-- ==================== 主内容区 ==================== -->
  <main class="detail-page">
    <div v-if="loading" class="loading">加载中...</div>

    <template v-else-if="order">
      <h2 class="page-title">订单详情</h2>

      <!-- 操作消息提示 -->
      <p v-if="actionMsg" class="action-msg" :class="{ 'msg-ok': actionMsg.includes('成功') || actionMsg.includes('取消') }">
        {{ actionMsg }}
      </p>

      <!-- 订单基本信息卡片 -->
      <div class="info-card">
        <h3>订单信息</h3>
        <div class="info-grid">
          <div class="info-row"><span class="info-label">订单编号</span><span>#{{ order.id }}</span></div>
          <div class="info-row"><span class="info-label">下单用户</span><span>{{ order.userName }}</span></div>
          <div class="info-row"><span class="info-label">订单状态</span>
            <span class="status-tag" :class="statusMap[order.status]?.cls">{{ statusMap[order.status]?.label }}</span>
          </div>
          <div class="info-row"><span class="info-label">联系电话</span><span>{{ order.phone }}</span></div>
          <div class="info-row"><span class="info-label">收货地址</span><span>{{ order.address }}</span></div>
          <div class="info-row"><span class="info-label">下单时间</span><span>{{ order.createTime?.slice(0, 16).replace('T', ' ') }}</span></div>
          <div class="info-row" v-if="order.remark"><span class="info-label">备注</span><span>{{ order.remark }}</span></div>
        </div>
      </div>

      <!-- 商品信息卡片 -->
      <div class="info-card">
        <h3>商品信息</h3>
        <div class="product-info">
          <div class="product-img-wrap">
            <img
              v-if="order.productImage"
              :src="order.productImage"
              :alt="order.productName"
              class="product-img"
              @error="($event.target as HTMLImageElement).style.display='none'"
            />
            <div v-if="!order.productImage" class="img-placeholder">{{ order.productName?.charAt(0) }}</div>
          </div>
          <div class="product-meta">
            <h4>{{ order.productName }}</h4>
            <p class="product-price">¥{{ order.price?.toFixed(2) }} × {{ order.quantity }}</p>
            <p class="product-total">合计：<strong>¥{{ order.totalAmount?.toFixed(2) }}</strong></p>
          </div>
        </div>
      </div>

      <!-- 快递信息卡片（已发货后显示） -->
      <div v-if="order.courierCompany || order.trackingNumber" class="info-card">
        <h3>物流信息</h3>
        <div class="info-grid">
          <div class="info-row" v-if="order.courierCompany">
            <span class="info-label">快递公司</span><span>{{ order.courierCompany }}</span>
          </div>
          <div class="info-row" v-if="order.trackingNumber">
            <span class="info-label">快递单号</span><span class="tracking-num">{{ order.trackingNumber }}</span>
          </div>
        </div>
      </div>

      <!-- 操作按钮行 -->
      <div class="action-bar">
        <!-- 取消订单按钮 -->
        <button v-if="canCancel" class="btn-cancel" :disabled="actionLoading" @click="showCancelConfirm = true">
          取消订单
        </button>

        <!-- 确认收货按钮（已发货 + 是下单用户） -->
        <button v-if="canConfirmReceive" class="btn-confirm-receive" :disabled="actionLoading" @click="handleConfirmReceive">
          确认收货
        </button>

        <!-- 评价按钮（订单已完成且未评价时显示，传承人身份不显示） -->
        <button
          v-if="order.status === 2 && !hasReviewed && !isInheritor"
          class="btn-review"
          @click="showReviewForm = !showReviewForm"
        >
          {{ showReviewForm ? '收起评价' : '评价商品' }}
        </button>

        <!-- 已评价提示（传承人身份不显示） -->
        <span v-if="order.status === 2 && hasReviewed && !isInheritor" class="reviewed-tip">已评价</span>
      </div>

      <!-- 评价表单 -->
      <div v-if="showReviewForm" class="info-card review-card">
        <h3>评价商品</h3>
        <div class="rating-row">
          <span class="rating-label">评分</span>
          <div class="star-picker">
            <button
              v-for="n in 5" :key="n"
              class="star-btn" :class="{ active: n <= reviewRating }"
              @click="reviewRating = n"
            >★</button>
          </div>
        </div>
        <textarea
          v-model="reviewContent"
          class="review-textarea"
          placeholder="分享你的使用体验..."
          rows="4"
        ></textarea>
        <p v-if="reviewMsg" class="review-msg" :class="{ 'msg-ok': reviewMsg.includes('成功'), 'msg-err': !reviewMsg.includes('成功') }">
          {{ reviewMsg }}
        </p>
        <button class="btn-submit-review" :disabled="reviewLoading" @click="handleSubmitReview">
          {{ reviewLoading ? '提交中...' : '提交评价' }}
        </button>
      </div>

      <!-- 已有评价（传承人身份不显示） -->
      <div v-if="existingReviews.length > 0 && !isInheritor" class="info-card">
        <h3>我的评价</h3>
        <div v-for="r in existingReviews.filter(r => r.userId === currentUserId)" :key="r.id" class="review-item">
          <div class="review-header">
            <span class="review-stars">{{ '★'.repeat(r.rating || 5) }}{{ '☆'.repeat(5 - (r.rating || 5)) }}</span>
            <span class="review-time">{{ r.createTime?.slice(0, 10) }}</span>
          </div>
          <p class="review-content">{{ r.content }}</p>
        </div>
      </div>

    </template>

    <!-- 未找到 -->
    <div v-else class="empty">订单不存在</div>
  </main>

  <!-- ==================== 取消确认弹窗 ==================== -->
  <div v-if="showCancelConfirm" class="modal-overlay" @click.self="showCancelConfirm = false">
    <div class="modal-box">
      <h3>确认取消订单</h3>
      <p>取消后订单将无法恢复，确定要取消订单 #{{ order?.id }} 吗？</p>
      <div class="modal-btns">
        <button class="btn-modal-cancel" @click="showCancelConfirm = false">再想想</button>
        <button class="btn-modal-confirm" :disabled="actionLoading" @click="handleCancel">
          {{ actionLoading ? '取消中...' : '确认取消' }}
        </button>
      </div>
    </div>
  </div>

  <!-- ==================== 页脚 ==================== -->
  <footer class="page-footer">
    <p>校园非遗与文创传承人平台 © 2026 码上非遗</p>
  </footer>

  <!-- ==================== 仿淘宝侧边栏 ==================== -->
  <aside class="side-toolbar">
    <div v-for="item in sideItems" :key="item.key" class="side-btn" @click="goTo(item.to)">
      <!-- 购物车图标 -->
      <svg v-if="item.key === 'cart'" class="side-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
        <circle cx="9" cy="21" r="1"/><circle cx="20" cy="21" r="1"/>
        <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"/>
      </svg>
      <!-- 订单图标 -->
      <svg v-else-if="item.key === 'orders'" class="side-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
        <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
        <polyline points="14,2 14,8 20,8"/>
        <line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/>
      </svg>
      <!-- 个人中心图标 -->
      <svg v-else class="side-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
        <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
        <circle cx="12" cy="7" r="4"/>
      </svg>
      <span class="side-label">{{ item.label }}</span>
      <!-- 购物车角标 -->
      <span v-if="item.key === 'cart' && cartStore.count > 0" class="side-badge">{{ cartStore.count }}</span>
    </div>
  </aside>
</template>

<style scoped>
/* ==================== 返回按钮 ==================== */
.back-btn {
  display: inline-block;
  margin: 16px 0 0 6%;
  color: #c41e3a;
  cursor: pointer;
  font-size: 15px;
  letter-spacing: 2px;
  font-weight: 600;
}
.back-btn:hover { color: #8b1a2b; }

/* ==================== 主内容区 ==================== */
.detail-page { max-width: 800px; margin: 0 auto; padding: 32px 5%; min-height: 70vh; }
.page-title { color: #c41e3a; font-size: 24px; margin-bottom: 24px; }
.loading, .empty { text-align: center; padding: 60px; color: #999; font-size: 14px; }
.action-msg { text-align: center; padding: 8px 0; margin-bottom: 16px; border-radius: 6px; color: #dc3545; font-size: 14px; font-weight: 600; }
.action-msg.msg-ok { color: #28a745; }

/* ==================== 信息卡片 ==================== */
.info-card {
  background: #fff; border-radius: 12px; padding: 24px;
  margin-bottom: 20px; box-shadow: 0 2px 12px rgba(0,0,0,0.06);
}
.info-card h3 {
  font-size: 17px; color: #c41e3a; margin-bottom: 16px;
  padding-bottom: 10px; border-bottom: 1px solid #f5f0eb;
}
.info-grid { display: flex; flex-direction: column; gap: 10px; }
.info-row { display: flex; font-size: 14px; color: #333; }
.info-label { width: 80px; color: #999; flex-shrink: 0; }

.status-tag { display: inline-block; padding: 2px 10px; border-radius: 12px; font-size: 12px; font-weight: 600; }
.status-pending { background: #fff3cd; color: #856404; }
.status-shipped { background: #cce5ff; color: #004085; }
.status-completed { background: #d4edda; color: #155724; }
.status-cancelled { background: #f8d7da; color: #721c24; }

.tracking-num { font-family: monospace; color: #c41e3a; font-weight: 600; }

/* ==================== 商品信息 ==================== */
.product-info { display: flex; gap: 20px; align-items: center; }
.product-img-wrap { width: 120px; height: 120px; border-radius: 8px; overflow: hidden; flex-shrink: 0; background: #f5f5f5; }
.product-img { width: 100%; height: 100%; object-fit: cover; }
.img-placeholder { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; background: linear-gradient(135deg, #e8647a, #f0a0b0); color: #fff; font-size: 32px; font-weight: bold; }
.product-meta { flex: 1; }
.product-meta h4 { font-size: 16px; color: #333; margin-bottom: 8px; }
.product-price { font-size: 14px; color: #c41e3a; margin-bottom: 4px; }
.product-total { font-size: 16px; color: #333; }
.product-total strong { color: #c41e3a; font-size: 20px; }

/* ==================== 操作按钮 ==================== */
.action-bar { display: flex; gap: 12px; margin-bottom: 20px; align-items: center; }
.btn-cancel { padding: 10px 24px; background: #fff; color: #dc3545; border: 1px solid #dc3545; border-radius: 6px; cursor: pointer; font-size: 14px; transition: 0.2s; }
.btn-cancel:hover { background: #dc3545; color: #fff; }
.btn-cancel:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-confirm-receive { padding: 10px 24px; background: #28a745; color: #fff; border: none; border-radius: 6px; cursor: pointer; font-size: 14px; font-weight: 600; transition: 0.2s; }
.btn-confirm-receive:hover { background: #218838; }
.btn-confirm-receive:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-review { padding: 10px 24px; background: #c41e3a; color: #fff; border: none; border-radius: 6px; cursor: pointer; font-size: 14px; font-weight: 600; transition: 0.2s; }
.btn-review:hover { background: #a01830; }
.reviewed-tip { font-size: 13px; color: #999; }

/* ==================== 评价表单 ==================== */
.review-card { margin-top: 0; }
.rating-row { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
.rating-label { font-size: 14px; color: #666; }
.star-picker { display: flex; gap: 2px; }
.star-btn { background: none; border: none; font-size: 28px; color: #ddd; cursor: pointer; transition: 0.15s; padding: 0; line-height: 1; }
.star-btn.active { color: #ffb800; }
.review-textarea { width: 100%; padding: 12px; border: 1px solid #ddd; border-radius: 8px; font-size: 14px; box-sizing: border-box; resize: vertical; font-family: inherit; outline: none; }
.review-textarea:focus { border-color: #c41e3a; }
.review-msg { font-size: 13px; margin: 8px 0 0; }
.review-msg.msg-err { color: #dc3545; }
.review-msg.msg-ok { color: #28a745; }
.btn-submit-review { margin-top: 12px; padding: 10px 24px; background: #c41e3a; color: #fff; border: none; border-radius: 6px; cursor: pointer; font-size: 14px; font-weight: 600; transition: 0.2s; }
.btn-submit-review:hover { background: #a01830; }
.btn-submit-review:disabled { opacity: 0.5; cursor: not-allowed; }

/* ==================== 已有评价 ==================== */
.review-item { border-bottom: 1px solid #f5f0eb; padding: 12px 0; }
.review-item:last-child { border-bottom: none; }
.review-header { display: flex; justify-content: space-between; margin-bottom: 6px; }
.review-stars { color: #ffb800; font-size: 14px; }
.review-time { font-size: 12px; color: #999; }
.review-content { font-size: 14px; color: #333; line-height: 1.6; }

/* ==================== 弹窗 ==================== */
.modal-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.45); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.modal-box { background: #fff; border-radius: 12px; padding: 32px; max-width: 400px; width: 90%; text-align: center; }
.modal-box h3 { font-size: 18px; color: #dc3545; margin-bottom: 12px; }
.modal-box p { font-size: 14px; color: #666; margin-bottom: 24px; line-height: 1.6; }
.modal-btns { display: flex; gap: 12px; justify-content: center; }
.btn-modal-cancel { padding: 10px 24px; background: #f5f5f5; border: none; border-radius: 6px; cursor: pointer; font-size: 14px; }
.btn-modal-confirm { padding: 10px 24px; background: #dc3545; color: #fff; border: none; border-radius: 6px; cursor: pointer; font-size: 14px; }
.btn-modal-confirm:disabled { opacity: 0.5; cursor: not-allowed; }

/* ==================== 侧边栏 ==================== */
.side-toolbar {
  position: fixed; right: 16px; top: 50%; transform: translateY(-50%);
  display: flex; flex-direction: column; gap: 8px; z-index: 500;
}
.side-btn {
  position: relative; width: 56px; height: 56px;
  background: #fff; border-radius: 10px;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  cursor: pointer; box-shadow: 0 2px 8px rgba(0,0,0,0.10);
  transition: all 0.2s; user-select: none;
}
.side-btn:hover { box-shadow: 0 4px 16px rgba(196,30,58,0.3); color: #c41e3a; }
.side-icon { width: 22px; height: 22px; color: #666; transition: color 0.2s; }
.side-btn:hover .side-icon { color: #c41e3a; }
.side-label { font-size: 10px; color: #999; margin-top: 1px; transition: color 0.2s; }
.side-btn:hover .side-label { color: #c41e3a; }
.side-badge {
  position: absolute; top: -4px; right: -4px;
  min-width: 18px; height: 18px; border-radius: 9px;
  background: #ff6b35; color: #fff; font-size: 10px; font-weight: 700;
  display: flex; align-items: center; justify-content: center;
  border: 2px solid #fff;
}

/* ==================== 页脚 ==================== */
.page-footer { background-color: #c41e3a; color: #fff; text-align: center; padding: 24px 5%; margin-top: auto; }

/* ==================== 响应式 ==================== */
@media (max-width: 768px) {
  .product-info { flex-direction: column; }
  .product-img-wrap { width: 100%; height: 200px; }
  .detail-page { padding: 20px 16px; }
  .side-btn { width: 48px; height: 48px; border-radius: 8px; }
  .side-icon { width: 20px; height: 20px; }
  .side-label { font-size: 9px; }
}
</style>
