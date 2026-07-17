<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'
import { listProducts, listProductsByCategory, submitProductOrder, listMyProductOrders } from '@/api/product'
import type { ProductVO, ProductOrderVO, ProductOrderStatus } from '@/types/api'

const authStore = useAuthStore()
const router = useRouter()

onMounted(() => {
  document.title = '文创周边 — 码上非遗'
  loadProducts()
})

// ===== 登录状态 =====
const isLoggedIn = computed(() => authStore.isLoggedIn)
const userName = computed(() => authStore.userInfo?.name)
const userRole = computed(() => authStore.userInfo?.role)
const isInheritor = computed(() => userRole.value === 'INHERITOR')

function handleLogout() {
  authStore.logout()
  router.push('/')
}

// ===== 商品列表 =====
const productList = ref<ProductVO[]>([])
const loading = ref(false)
const activeCategory = ref('')

const categories = ['全部', '剪纸', '刺绣', '陶艺', '竹编', '综合']

async function loadProducts() {
  loading.value = true
  try {
    const res = await listProducts()
    productList.value = res.data
  } catch {
    // 未登录时不显示错误
  } finally {
    loading.value = false
  }
}

/** 图片加载失败时回退到占位符 */
const failedImages = ref(new Set<number>())

function onImgError(productId: number) {
  failedImages.value.add(productId)
}

async function filterByCategory(cat: string) {
  activeCategory.value = cat
  if (cat === '全部' || cat === '') {
    await loadProducts()
    return
  }
  loading.value = true
  try {
    const res = await listProductsByCategory(cat)
    productList.value = res.data
  } catch {
    // ignore
  } finally {
    loading.value = false
  }
}

// ===== 购物车 =====
interface CartItem {
  product: ProductVO
  quantity: number
}

const cart = ref<CartItem[]>([])
const showCart = ref(false)

function addToCart(product: ProductVO) {
  if (!isLoggedIn.value) {
    router.push('/login')
    return
  }
  const existing = cart.value.find(item => item.product.id === product.id)
  if (existing) {
    if (existing.quantity < product.stock) {
      existing.quantity++
    }
  } else {
    cart.value.push({ product, quantity: 1 })
  }
}

function removeFromCart(index: number) {
  cart.value.splice(index, 1)
}

function updateCartQty(index: number, qty: number) {
  if (qty < 1) {
    removeFromCart(index)
    return
  }
  if (qty > cart.value[index].product.stock) {
    qty = cart.value[index].product.stock
  }
  cart.value[index].quantity = qty
}

const cartTotal = computed(() => {
  return cart.value.reduce((sum, item) => sum + item.product.price * item.quantity, 0)
})

// ===== 下单弹窗 =====
const showOrderModal = ref(false)
const orderForm = ref({
  phone: '',
  address: '',
  remark: '',
})
const orderSubmitting = ref(false)
const orderMsg = ref('')

function openOrderModal() {
  if (cart.value.length === 0) {
    orderMsg.value = '购物车为空，请先添加商品'
    setTimeout(() => { orderMsg.value = '' }, 2000)
    return
  }
  orderForm.value = { phone: '', address: '', remark: '' }
  orderMsg.value = ''
  showCart.value = false
  showOrderModal.value = true
}

function closeOrderModal() {
  showOrderModal.value = false
}

async function submitOrders() {
  if (!orderForm.value.phone || !orderForm.value.address) {
    orderMsg.value = '请填写联系电话和收货地址'
    return
  }
  orderSubmitting.value = true
  orderMsg.value = ''
  try {
    for (const item of cart.value) {
      await submitProductOrder({
        productId: item.product.id,
        quantity: item.quantity,
        phone: orderForm.value.phone,
        address: orderForm.value.address,
        remark: orderForm.value.remark,
      })
    }
    orderMsg.value = '下单成功！传承人将尽快为您发货。'
    cart.value = []
    await loadProducts()
    setTimeout(() => {
      closeOrderModal()
      orderMsg.value = ''
    }, 2000)
  } catch {
    orderMsg.value = '下单失败，请稍后重试'
  } finally {
    orderSubmitting.value = false
  }
}

// ===== 我的订单 =====
const showMyOrders = ref(false)
const myOrders = ref<ProductOrderVO[]>([])
const myOrdersLoading = ref(false)

const orderStatusMap: Record<ProductOrderStatus, { text: string; cls: string }> = {
  0: { text: '待发货', cls: 'status-pending' },
  1: { text: '已发货', cls: 'status-shipped' },
  2: { text: '已完成', cls: 'status-completed' },
  3: { text: '已取消', cls: 'status-cancelled' },
}

async function loadMyOrders() {
  if (!isLoggedIn.value) {
    router.push('/login')
    return
  }
  myOrdersLoading.value = true
  showMyOrders.value = true
  try {
    const res = await listMyProductOrders()
    myOrders.value = res.data
  } catch {
    // ignore
  } finally {
    myOrdersLoading.value = false
  }
}
</script>

<template>
  <!-- ==================== 顶部导航 ==================== -->
  <header class="page-header">
    <div class="logo" @click="router.push('/')">码上非遗</div>

    <nav class="nav-links">
      <a class="nav-item" @click="router.push('/')">首页</a>
      <a class="nav-item" @click="router.push('/')">非遗资讯</a>
      <a class="nav-item" @click="router.push('/')">传承人</a>
      <a class="nav-item active" @click="router.push('/store')">文创周边</a>
      <a class="nav-item" @click="router.push('/')">智能问答</a>
      <template v-if="isInheritor">
        <span class="nav-separator"></span>
        <a class="nav-item nav-inheritor" @click="router.push('/inheritor')">我的工坊</a>
      </template>
    </nav>

    <div class="header-right">
      <template v-if="isLoggedIn">
        <span class="user-role-tag" :class="isInheritor ? 'inheritor-tag' : 'user-tag'">
          {{ isInheritor ? '非遗传承人' : '用户' }}
        </span>
        <span class="user-name">{{ userName }}</span>
        <!-- 购物车按钮 -->
        <button class="cart-btn" @click="showCart = !showCart; showMyOrders = false">
          🛒购物车
          <span v-if="cart.length > 0" class="cart-badge">{{ cart.length }}</span>
        </button>
        <button class="my-orders-btn" @click="loadMyOrders">我的订单</button>
        <button class="chat-btn" @click="router.push('/chat')">💬 消息</button>
        <button class="logout-btn" @click="handleLogout">退出</button>
      </template>
      <template v-else>
        <button class="login-top-btn" @click="router.push('/login')">登录</button>
      </template>
    </div>
  </header>

  <!-- ==================== 购物车侧边栏 ==================== -->
  <div v-if="showCart" class="cart-overlay" @click.self="showCart = false">
    <div class="cart-panel">
      <div class="cart-header">
        <h3>购物车</h3>
        <button class="cart-close" @click="showCart = false">✕</button>
      </div>
      <div class="cart-body">
        <div v-if="cart.length === 0" class="cart-empty">购物车是空的</div>
        <div v-for="(item, index) in cart" :key="item.product.id" class="cart-item">
          <div class="cart-item-img">
            <img
              v-if="item.product.imageUrl && !failedImages.has(item.product.id)"
              :src="item.product.imageUrl"
              :alt="item.product.name"
              class="cart-img-real"
              @error="onImgError(item.product.id)"
            />
            <div v-if="!item.product.imageUrl || failedImages.has(item.product.id)" class="img-placeholder-sm">{{ item.product.category }}</div>
          </div>
          <div class="cart-item-info">
            <div class="cart-item-name">{{ item.product.name }}</div>
            <div class="cart-item-price">¥{{ item.product.price.toFixed(2) }}</div>
          </div>
          <div class="cart-item-qty">
            <button @click="updateCartQty(index, item.quantity - 1)">−</button>
            <span>{{ item.quantity }}</span>
            <button @click="updateCartQty(index, item.quantity + 1)">+</button>
          </div>
          <button class="cart-item-remove" @click="removeFromCart(index)">删除</button>
        </div>
      </div>
      <div v-if="cart.length > 0" class="cart-footer">
        <div class="cart-total">合计：<strong>¥{{ cartTotal.toFixed(2) }}</strong></div>
        <button class="btn-checkout" @click="openOrderModal">去结算</button>
      </div>
    </div>
  </div>

  <!-- ==================== 下单弹窗 ==================== -->
  <div v-if="showOrderModal" class="modal-overlay" @click.self="closeOrderModal">
    <div class="modal-content">
      <div class="modal-header">
        <h3>确认下单</h3>
        <button class="modal-close" @click="closeOrderModal">✕</button>
      </div>
      <div class="modal-body">
        <div class="order-summary">
          <div v-for="item in cart" :key="item.product.id" class="order-summary-item">
            <span>{{ item.product.name }} x {{ item.quantity }}</span>
            <span>¥{{ (item.product.price * item.quantity).toFixed(2) }}</span>
          </div>
          <div class="order-summary-total">
            <strong>合计：¥{{ cartTotal.toFixed(2) }}</strong>
          </div>
        </div>
        <div class="form-group">
          <label>联系电话 *</label>
          <input
            v-model="orderForm.phone"
            type="text"
            class="form-input"
            placeholder="请输入联系电话"
          />
        </div>
        <div class="form-group">
          <label>收货地址 *</label>
          <textarea
            v-model="orderForm.address"
            class="form-textarea"
            rows="3"
            placeholder="请输入收货地址"
          ></textarea>
        </div>
        <div class="form-group">
          <label>备注</label>
          <textarea
            v-model="orderForm.remark"
            class="form-textarea"
            rows="2"
            placeholder="如有特殊要求请注明"
          ></textarea>
        </div>
        <div v-if="orderMsg" class="order-msg" :class="{ success: orderMsg.includes('成功') }">
          {{ orderMsg }}
        </div>
      </div>
      <div class="modal-footer">
        <button class="btn-outline" @click="closeOrderModal">取消</button>
        <button class="btn-primary" @click="submitOrders" :disabled="orderSubmitting">
          {{ orderSubmitting ? '提交中...' : '确认下单' }}
        </button>
      </div>
    </div>
  </div>

  <!-- ==================== 我的订单弹窗 ==================== -->
  <div v-if="showMyOrders" class="modal-overlay" @click.self="showMyOrders = false">
    <div class="modal-content modal-wide">
      <div class="modal-header">
        <h3>我的商品订单</h3>
        <button class="modal-close" @click="showMyOrders = false">✕</button>
      </div>
      <div class="modal-body">
        <div v-if="myOrdersLoading" class="empty-state">加载中...</div>
        <div v-else-if="myOrders.length === 0" class="empty-state">暂无商品订单</div>
        <table v-else class="order-table">
          <thead>
            <tr>
              <th>商品</th>
              <th>单价</th>
              <th>数量</th>
              <th>金额</th>
              <th>电话</th>
              <th>地址</th>
              <th>状态</th>
              <th>时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="order in myOrders" :key="order.id">
              <td>{{ order.productName }}</td>
              <td>¥{{ order.price.toFixed(2) }}</td>
              <td>{{ order.quantity }}</td>
              <td>¥{{ order.totalAmount.toFixed(2) }}</td>
              <td>{{ order.phone }}</td>
              <td class="address-cell">{{ order.address }}</td>
              <td>
                <span class="status-tag" :class="orderStatusMap[order.status]?.cls">
                  {{ orderStatusMap[order.status]?.text }}
                </span>
              </td>
              <td>{{ order.createTime?.slice(0, 10) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>

  <!-- ==================== 主内容区 ==================== -->
  <main class="store-main">
    <div class="store-container">
      <!-- 页面标题 -->
      <div class="store-header">
        <h1>文创周边</h1>
        <p>精选非遗文创好物，把传统手工艺带回家</p>
      </div>

      <!-- 分类筛选栏 -->
      <div class="category-bar">
        <span
          v-for="cat in categories"
          :key="cat"
          class="category-chip"
          :class="{ active: activeCategory === cat || (cat === '全部' && !activeCategory) }"
          @click="filterByCategory(cat === '全部' ? '' : cat)"
        >
          {{ cat }}
        </span>
      </div>

      <!-- 商品网格 -->
      <div v-if="loading" class="loading-state">加载中...</div>
      <div v-else-if="!isLoggedIn" class="not-login-prompt">
        <div class="prompt-card">
          <div class="prompt-icon">🔐</div>
          <h3>请先登录</h3>
          <p>登录后即可浏览和选购心仪的文创商品</p>
          <button class="btn-primary" @click="router.push('/login')">去登录</button>
        </div>
      </div>
      <div v-else-if="productList.length === 0" class="empty-state">
        暂无文创商品，敬请期待
      </div>
      <div v-else class="product-grid">
        <div v-for="product in productList" :key="product.id" class="product-card">
          <div class="product-img">
            <img
              v-if="product.imageUrl && !failedImages.has(product.id)"
              :src="product.imageUrl"
              :alt="product.name"
              class="product-img-real"
              @error="onImgError(product.id)"
            />
            <div v-if="!product.imageUrl || failedImages.has(product.id)" class="img-placeholder-product">{{ product.category }}</div>
          </div>
          <div class="product-body">
            <div class="product-category-tag">{{ product.category }}</div>
            <h3 class="product-name">{{ product.name }}</h3>
            <p class="product-desc">{{ product.description }}</p>
            <div class="product-footer">
              <span class="product-price">¥{{ product.price.toFixed(2) }}</span>
              <span class="product-stock" v-if="product.stock > 0">库存 {{ product.stock }}</span>
              <span class="product-stock sold-out" v-else>已售罄</span>
            </div>
            <div class="product-inheritor">传承人：{{ product.inheritorName }}</div>
            <button
              class="btn-add-cart"
              :disabled="product.stock === 0"
              @click="addToCart(product)"
            >
              {{ product.stock === 0 ? '已售罄' : '加入购物车' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </main>

  <!-- ==================== 页脚 ==================== -->
  <footer class="page-footer">
    <p>校园非遗与文创传承人平台 © 2026 码上非遗</p>
  </footer>
</template>

<style scoped>
/* ==================== 全局重置 ==================== */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
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
  background: rgba(255,255,255,0.4);
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
.user-tag { background: #fff; color: #c41e3a; }
.inheritor-tag { background: #ffd700; color: #8b4513; }
.user-name { font-size: 14px; }

.cart-btn {
  position: relative;
  padding: 6px 14px;
  font-size: 12px;
  color: #c41e3a;
  background: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-weight: bold;
  transition: 0.2s;
}
.cart-btn:hover { background: #ffe9ec; }
.cart-badge {
  position: absolute;
  top: -6px;
  right: -6px;
  background: #ffd700;
  color: #8b4513;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  font-size: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
}
.my-orders-btn {
  padding: 6px 14px;
  font-size: 12px;
  color: #fff;
  background: rgba(255,255,255,0.15);
  border: 1px solid rgba(255,255,255,0.4);
  border-radius: 4px;
  cursor: pointer;
  transition: 0.2s;
}
.my-orders-btn:hover { background: rgba(255,255,255,0.3); }
.chat-btn {
  padding: 6px 14px;
  font-size: 12px;
  color: #fff;
  background: rgba(255,255,255,0.2);
  border: 1px solid rgba(255,255,255,0.5);
  border-radius: 4px;
  cursor: pointer;
  transition: 0.2s;
}
.chat-btn:hover { background: rgba(255,255,255,0.35); }
.logout-btn {
  padding: 6px 14px;
  font-size: 12px;
  color: #fff;
  background: rgba(255,255,255,0.15);
  border: 1px solid rgba(255,255,255,0.4);
  border-radius: 4px;
  cursor: pointer;
  transition: 0.2s;
}
.logout-btn:hover { background: rgba(255,255,255,0.3); }
.login-top-btn {
  padding: 8px 22px;
  font-size: 14px;
  color: #c41e3a;
  background: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-weight: bold;
  transition: 0.2s;
}
.login-top-btn:hover { background: #ffe9ec; }

/* ==================== 购物车侧边栏 ==================== */
.cart-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.35);
  z-index: 200;
  display: flex;
  justify-content: flex-end;
}
.cart-panel {
  width: 420px;
  max-width: 90vw;
  height: 100%;
  background: #fff;
  display: flex;
  flex-direction: column;
  box-shadow: -4px 0 20px rgba(0,0,0,0.15);
}
.cart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #eee;
}
.cart-header h3 { font-size: 18px; color: #c41e3a; }
.cart-close {
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  color: #999;
  padding: 4px 8px;
}
.cart-close:hover { color: #333; }
.cart-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px 24px;
}
.cart-empty {
  text-align: center;
  padding: 60px 0;
  color: #999;
  font-size: 14px;
}
.cart-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid #f5f5f5;
}
.cart-item-img {
  width: 56px;
  height: 56px;
  flex-shrink: 0;
  border-radius: 8px;
  overflow: hidden;
}
.img-placeholder-sm {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #e8647a, #f0a0b0);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 11px;
}
.cart-img-real {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.cart-item-info { flex: 1; min-width: 0; }
.cart-item-name {
  font-size: 14px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.cart-item-price {
  font-size: 14px;
  color: #c41e3a;
  font-weight: bold;
  margin-top: 4px;
}
.cart-item-qty {
  display: flex;
  align-items: center;
  gap: 6px;
}
.cart-item-qty button {
  width: 26px;
  height: 26px;
  border: 1px solid #ddd;
  border-radius: 4px;
  background: #fff;
  cursor: pointer;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.cart-item-qty button:hover { background: #f5f5f5; }
.cart-item-qty span {
  min-width: 20px;
  text-align: center;
  font-size: 14px;
}
.cart-item-remove {
  background: none;
  border: none;
  color: #999;
  cursor: pointer;
  font-size: 12px;
  padding: 4px;
}
.cart-item-remove:hover { color: #dc3545; }
.cart-footer {
  padding: 20px 24px;
  border-top: 1px solid #eee;
}
.cart-total {
  margin-bottom: 14px;
  font-size: 16px;
  color: #333;
}
.cart-total strong {
  color: #c41e3a;
  font-size: 20px;
}
.btn-checkout {
  width: 100%;
  padding: 12px;
  font-size: 16px;
  color: #fff;
  background: #c41e3a;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-weight: bold;
  transition: 0.2s;
}
.btn-checkout:hover { background: #a8162b; }

/* ==================== 弹窗通用 ==================== */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.45);
  z-index: 300;
  display: flex;
  align-items: center;
  justify-content: center;
}
.modal-content {
  background: #fff;
  border-radius: 12px;
  width: 480px;
  max-width: 90vw;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 8px 30px rgba(0,0,0,0.2);
}
.modal-wide { width: 800px; }
.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #eee;
}
.modal-header h3 { font-size: 18px; color: #c41e3a; }
.modal-close {
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  color: #999;
}
.modal-close:hover { color: #333; }
.modal-body {
  padding: 24px;
  overflow-y: auto;
  flex: 1;
}
.modal-footer {
  padding: 16px 24px;
  border-top: 1px solid #eee;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

/* 下单摘要 */
.order-summary {
  background: #fff5f7;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 20px;
}
.order-summary-item {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
  color: #555;
  padding: 6px 0;
}
.order-summary-total {
  padding-top: 10px;
  margin-top: 10px;
  border-top: 1px solid #ffe9ec;
  text-align: right;
  font-size: 16px;
  color: #c41e3a;
}
.order-msg {
  margin-top: 12px;
  padding: 10px;
  border-radius: 6px;
  background: #fff3cd;
  color: #856404;
  font-size: 13px;
  text-align: center;
}
.order-msg.success {
  background: #d4edda;
  color: #155724;
}

/* 表单 */
.form-group { margin-bottom: 16px; }
.form-group label {
  display: block;
  font-size: 13px;
  color: #666;
  margin-bottom: 6px;
  font-weight: 600;
}
.form-input {
  width: 100%;
  height: 42px;
  padding: 0 14px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  color: #333;
  outline: none;
  transition: 0.25s;
  background: #fafafa;
}
.form-input:focus {
  border-color: #c41e3a;
  box-shadow: 0 0 0 3px rgba(196,30,58,0.08);
  background: #fff;
}
.form-textarea {
  width: 100%;
  padding: 12px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  color: #333;
  outline: none;
  resize: vertical;
  transition: 0.25s;
  background: #fafafa;
  font-family: inherit;
  line-height: 1.6;
}
.form-textarea:focus {
  border-color: #c41e3a;
  box-shadow: 0 0 0 3px rgba(196,30,58,0.08);
  background: #fff;
}

/* 通用按钮 */
.btn-primary {
  padding: 10px 24px;
  font-size: 14px;
  color: #fff;
  background: #c41e3a;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-weight: bold;
  transition: 0.2s;
}
.btn-primary:hover { background: #a8162b; }
.btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-outline {
  padding: 10px 24px;
  font-size: 14px;
  color: #c41e3a;
  background: #fff;
  border: 1px solid #c41e3a;
  border-radius: 6px;
  cursor: pointer;
  transition: 0.2s;
}
.btn-outline:hover { background: #fff5f7; }

/* ==================== 我的订单表格 ==================== */
.order-table {
  width: 100%;
  border-collapse: collapse;
}
.order-table th,
.order-table td {
  padding: 12px 14px;
  text-align: left;
  font-size: 13px;
}
.order-table thead { background: #fff5f7; }
.order-table th {
  color: #c41e3a;
  font-weight: 600;
  font-size: 12px;
}
.order-table tbody tr {
  border-bottom: 1px solid #f5f5f5;
  transition: 0.2s;
}
.order-table tbody tr:hover { background: #fefafa; }
.address-cell {
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.status-tag {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}
.status-pending { background: #fff3cd; color: #856404; }
.status-shipped { background: #cce5ff; color: #004085; }
.status-completed { background: #e2e3e5; color: #383d41; }
.status-cancelled { background: #f8d7da; color: #721c24; }

/* ==================== 主内容区 ==================== */
.store-main {
  min-height: calc(100vh - 64px - 60px);
  background: #fcf4e8;
  padding: 30px 5%;
}
.store-container {
  max-width: 1200px;
  margin: 0 auto;
}
.store-header {
  text-align: center;
  margin-bottom: 30px;
}
.store-header h1 {
  font-size: 32px;
  color: #c41e3a;
  margin-bottom: 8px;
}
.store-header p {
  font-size: 15px;
  color: #888;
}

/* ===== 分类筛选 ===== */
.category-bar {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-bottom: 36px;
  flex-wrap: wrap;
}
.category-chip {
  padding: 8px 22px;
  border: 1px solid #e8647a;
  border-radius: 20px;
  font-size: 14px;
  cursor: pointer;
  color: #c41e3a;
  transition: all 0.2s;
  user-select: none;
  background: #fff;
}
.category-chip:hover { background: #ffe9ec; }
.category-chip.active {
  background: #c41e3a;
  color: #fff;
  border-color: #c41e3a;
}

/* ===== 商品网格 ===== */
.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(270px, 1fr));
  gap: 24px;
}
.product-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
  transition: 0.3s;
  display: flex;
  flex-direction: column;
}
.product-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(196,30,58,0.12);
}
.product-img {
  height: 200px;
  background: linear-gradient(135deg, #ffe9ec, #fff5f7);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}
.product-img-real {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}
.product-card:hover .product-img-real {
  transform: scale(1.05);
}
.img-placeholder-product {
  width: 80px;
  height: 80px;
  border-radius: 16px;
  background: linear-gradient(135deg, #e8647a, #c41e3a);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 16px;
  font-weight: bold;
}
.product-body {
  padding: 18px;
  display: flex;
  flex-direction: column;
  flex: 1;
}
.product-category-tag {
  display: inline-block;
  padding: 2px 10px;
  background: #ffe9ec;
  color: #c41e3a;
  border-radius: 10px;
  font-size: 11px;
  margin-bottom: 8px;
  align-self: flex-start;
}
.product-name {
  font-size: 17px;
  color: #222;
  margin-bottom: 8px;
  line-height: 1.4;
}
.product-desc {
  font-size: 13px;
  color: #888;
  line-height: 1.6;
  flex: 1;
  margin-bottom: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
.product-footer {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-bottom: 8px;
}
.product-price {
  font-size: 22px;
  color: #c41e3a;
  font-weight: bold;
}
.product-stock {
  font-size: 12px;
  color: #999;
}
.product-stock.sold-out { color: #dc3545; }
.product-inheritor {
  font-size: 12px;
  color: #aaa;
  margin-bottom: 12px;
}
.btn-add-cart {
  width: 100%;
  padding: 10px;
  font-size: 14px;
  color: #fff;
  background: #c41e3a;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-weight: bold;
  transition: 0.2s;
}
.btn-add-cart:hover { background: #a8162b; }
.btn-add-cart:disabled {
  background: #ccc;
  cursor: not-allowed;
}

/* ===== 未登录提示 ===== */
.not-login-prompt {
  display: flex;
  justify-content: center;
  padding: 60px 0;
}
.prompt-card {
  text-align: center;
  background: #fff;
  border-radius: 16px;
  padding: 48px 40px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.06);
  max-width: 400px;
}
.prompt-icon { font-size: 48px; margin-bottom: 16px; }
.prompt-card h3 {
  font-size: 20px;
  color: #333;
  margin-bottom: 10px;
}
.prompt-card p {
  font-size: 14px;
  color: #888;
  margin-bottom: 24px;
}

/* ===== 加载/空状态 ===== */
.loading-state {
  text-align: center;
  padding: 60px;
  color: #999;
  font-size: 14px;
}
.empty-state {
  text-align: center;
  padding: 60px 0;
  color: #999;
  font-size: 14px;
}

/* ==================== 页脚 ==================== */
.page-footer {
  background-color: #c41e3a;
  color: #fff;
  text-align: center;
  padding: 20px 5%;
  font-size: 14px;
}

/* ==================== 响应式 ==================== */
@media (max-width: 992px) {
  .nav-links { gap: 12px; }
  .product-grid { grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); }
  .cart-panel { width: 100vw; }
}
@media (max-width: 768px) {
  .page-header {
    flex-wrap: wrap;
    height: auto;
    padding: 10px 16px;
    gap: 10px;
  }
  .nav-links {
    order: 3;
    width: 100%;
    justify-content: center;
    flex-wrap: wrap;
    gap: 8px;
  }
  .header-right { flex-wrap: wrap; }
  .product-grid { grid-template-columns: repeat(auto-fill, minmax(160px, 1fr)); gap: 16px; }
}
</style>
