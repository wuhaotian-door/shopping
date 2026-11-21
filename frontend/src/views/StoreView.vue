<script setup lang="ts">
import { ref, shallowRef, computed, onMounted } from 'vue'
import { isAxiosError } from 'axios'
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'
import { useCartStore } from '@/stores/cart'
import { listProducts, listProductsByCategory } from '@/api/product'
import type { ProductVO } from '@/types/api'

const authStore = useAuthStore()
const router = useRouter()
const cartStore = useCartStore()

onMounted(() => { document.title = '文创周边 — 码上非遗'; loadProducts() })

const isLoggedIn = computed(() => authStore.isLoggedIn)

const productList = ref<ProductVO[]>([])
const loading = shallowRef(false)
const loadError = shallowRef('')
const activeCategory = shallowRef('')
const categories = ['全部', '剪纸', '刺绣', '陶艺', '竹编', '综合']
const failedImages = ref(new Set<number>())
let latestRequest = 0

function onImgError(id: number) { failedImages.value.add(id) }

async function loadProducts(category = activeCategory.value) {
  activeCategory.value = category
  const request = ++latestRequest
  loadError.value = ''
  loading.value = true
  try {
    const result = await (category ? listProductsByCategory(category) : listProducts())
    if (request === latestRequest) productList.value = result.data
  } catch (error) {
    if (request === latestRequest) {
      loadError.value = isAxiosError(error) && error.response?.status === 503
        ? '商城繁忙，请稍后重试'
        : '商品加载失败，请重试'
    }
  } finally {
    if (request === latestRequest) loading.value = false
  }
}

function addToCart(p: ProductVO) {
  if (!isLoggedIn.value) { router.push('/login'); return }
  cartStore.add(p, authStore.uid)
}

function goTo(path: string) {
  // 三个入口都需要登录
  if (!isLoggedIn.value) { router.push('/login'); return }
  router.push(path)
}
</script>

<template>
  <!-- ===== 主内容区 ===== -->
  <main class="store-main">
    <div class="store-container">
      <div class="store-header"><h1>文创周边</h1><p>精选非遗文创好物，把传统手工艺带回家</p></div>

      <div class="category-bar">
        <span v-for="cat in categories" :key="cat" class="category-chip"
          :class="{ active: activeCategory === cat || (cat === '全部' && !activeCategory) }"
          @click="loadProducts(cat === '全部' ? '' : cat)">{{ cat }}</span>
      </div>

      <div v-if="loading" class="loading-state">加载中...</div>
      <div v-else-if="!isLoggedIn" class="not-login-prompt">
        <div class="prompt-card"><div class="prompt-icon">🔐</div><h3>请先登录</h3><p>登录后即可浏览和选购心仪的文创商品</p><button class="btn-primary" @click="router.push('/login')">去登录</button></div>
      </div>
      <div v-else-if="loadError" class="empty-state">
        <p role="alert">{{ loadError }}</p>
        <button type="button" class="btn-primary retry-button" @click="loadProducts()">重试</button>
      </div>
      <div v-else-if="productList.length === 0" class="empty-state">暂无文创商品，敬请期待</div>
      <div v-else class="product-grid">
        <div v-for="p in productList" :key="p.id" class="product-card" @click="router.push(`/product/${p.id}`)">
          <div class="product-img">
            <img v-if="p.imageUrl && !failedImages.has(p.id)" :src="p.imageUrl" :alt="p.name" class="product-img-real" @error="onImgError(p.id)" />
            <div v-else class="img-placeholder-product">{{ p.category }}</div>
          </div>
          <div class="product-body">
            <div class="product-category-tag">{{ p.category }}</div>
            <h3 class="product-name">{{ p.name }}</h3>
            <p class="product-desc">{{ p.description }}</p>
            <div class="product-footer"><span class="product-price">¥{{ p.price.toFixed(2) }}</span><span class="product-stock" :class="{ 'sold-out': p.stock === 0 }">{{ p.stock > 0 ? `库存 ${p.stock}` : '已售罄' }}</span></div>
            <div class="product-inheritor">传承人：{{ p.inheritorName }}</div>
            <button class="btn-add-cart" :disabled="p.stock === 0" @click.stop="addToCart(p)">{{ p.stock === 0 ? '已售罄' : '加入购物车' }}</button>
          </div>
        </div>
      </div>
    </div>
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

  <footer class="page-footer"><p>校园非遗与文创传承人平台 © 2026 码上非遗</p></footer>
</template>

<style scoped>
* { margin: 0; padding: 0; box-sizing: border-box; }

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

/* ===== 主内容 ===== */
.store-main { min-height: calc(100vh - 60px); background: #fcf4e8; padding: 30px 5%; }
.store-container { max-width: 1200px; margin: 0 auto; }
.store-header { text-align: center; margin-bottom: 30px; }
.store-header h1 { font-size: 32px; color: #c41e3a; margin-bottom: 8px; }
.store-header p { font-size: 15px; color: #888; }

.category-bar { display: flex; justify-content: center; gap: 12px; margin-bottom: 36px; flex-wrap: wrap; }
.category-chip { padding: 8px 22px; border: 1px solid #e8647a; border-radius: 20px; font-size: 14px; cursor: pointer; color: #c41e3a; transition: all 0.2s; user-select: none; background: #fff; }
.category-chip:hover { background: #ffe9ec; }
.category-chip.active { background: #c41e3a; color: #fff; border-color: #c41e3a; }

.product-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(270px, 1fr)); gap: 24px; }
.product-card { background: #fff; border-radius: 12px; overflow: hidden; box-shadow: 0 2px 12px rgba(0,0,0,0.06); transition: 0.3s; display: flex; flex-direction: column; cursor: pointer; }
.product-card:hover { transform: translateY(-4px); box-shadow: 0 8px 24px rgba(196,30,58,0.12); }
.product-img { height: 200px; background: linear-gradient(135deg, #ffe9ec, #fff5f7); display: flex; align-items: center; justify-content: center; overflow: hidden; }
.product-img-real { width: 100%; height: 100%; object-fit: cover; transition: transform 0.3s; }
.product-card:hover .product-img-real { transform: scale(1.05); }
.img-placeholder-product { width: 80px; height: 80px; border-radius: 16px; background: linear-gradient(135deg, #e8647a, #c41e3a); display: flex; align-items: center; justify-content: center; color: #fff; font-size: 16px; font-weight: bold; }
.product-body { padding: 18px; display: flex; flex-direction: column; flex: 1; }
.product-category-tag { display: inline-block; padding: 2px 10px; background: #ffe9ec; color: #c41e3a; border-radius: 10px; font-size: 11px; margin-bottom: 8px; align-self: flex-start; }
.product-name { font-size: 17px; color: #222; margin-bottom: 8px; line-height: 1.4; }
.product-desc { font-size: 13px; color: #888; line-height: 1.6; flex: 1; margin-bottom: 12px; overflow: hidden; text-overflow: ellipsis; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; }
.product-footer { display: flex; align-items: baseline; gap: 10px; margin-bottom: 8px; }
.product-price { font-size: 22px; color: #c41e3a; font-weight: bold; }
.product-stock { font-size: 12px; color: #999; }
.product-stock.sold-out { color: #dc3545; }
.product-inheritor { font-size: 12px; color: #aaa; margin-bottom: 12px; }
.btn-add-cart { width: 100%; padding: 10px; font-size: 14px; color: #fff; background: #c41e3a; border: none; border-radius: 6px; cursor: pointer; font-weight: bold; transition: 0.2s; }
.btn-add-cart:hover { background: #a8162b; }
.btn-add-cart:disabled { background: #ccc; cursor: not-allowed; }

.not-login-prompt { display: flex; justify-content: center; padding: 60px 0; }
.prompt-card { text-align: center; background: #fff; border-radius: 16px; padding: 48px 40px; box-shadow: 0 4px 16px rgba(0,0,0,0.06); max-width: 400px; }
.prompt-icon { font-size: 48px; margin-bottom: 16px; }
.prompt-card h3 { font-size: 20px; color: #333; margin-bottom: 10px; }
.prompt-card p { font-size: 14px; color: #888; margin-bottom: 24px; }
.btn-primary { padding: 10px 24px; font-size: 14px; color: #fff; background: #c41e3a; border: none; border-radius: 6px; cursor: pointer; font-weight: bold; transition: 0.2s; }
.btn-primary:hover { background: #a8162b; }

.loading-state, .empty-state { text-align: center; padding: 60px; color: #999; font-size: 14px; }
.retry-button { margin-top: 12px; }
.page-footer { background-color: #c41e3a; color: #fff; text-align: center; padding: 20px 5%; font-size: 14px; }

@media (max-width: 768px) {
  .product-grid { grid-template-columns: repeat(auto-fill, minmax(160px, 1fr)); gap: 16px; }
  .side-toolbar { right: 6px; width: 50px; }
  .side-btn { padding: 10px 0; }
  .side-btn svg { width: 20px; height: 20px; }
  .side-label { font-size: 11px; }
}
</style>
