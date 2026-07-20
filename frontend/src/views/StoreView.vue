<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
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
const loading = ref(false)
const activeCategory = ref('')
const categories = ['全部', '剪纸', '刺绣', '陶艺', '竹编', '综合']
const failedImages = ref(new Set<number>())

function onImgError(id: number) { failedImages.value.add(id) }

async function loadProducts() {
  loading.value = true
  try { productList.value = (await listProducts()).data }
  catch { /* */ } finally { loading.value = false }
}

async function filterByCategory(cat: string) {
  activeCategory.value = cat
  if (!cat || cat === '全部') { await loadProducts(); return }
  loading.value = true
  try { productList.value = (await listProductsByCategory(cat)).data }
  catch { /* */ } finally { loading.value = false }
}

function addToCart(p: ProductVO) {
  if (!isLoggedIn.value) { router.push('/login'); return }
  cartStore.add(p, authStore.uid)
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
          @click="filterByCategory(cat === '全部' ? '' : cat)">{{ cat }}</span>
      </div>

      <div v-if="loading" class="loading-state">加载中...</div>
      <div v-else-if="!isLoggedIn" class="not-login-prompt">
        <div class="prompt-card"><div class="prompt-icon">🔐</div><h3>请先登录</h3><p>登录后即可浏览和选购心仪的文创商品</p><button class="btn-primary" @click="router.push('/login')">去登录</button></div>
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

  <!-- ===== 右下角悬浮加号按钮 ===== -->
  <div class="fab" @click="router.push('/cart')">
    <span class="fab-icon">+</span>
    <span v-if="cartStore.count > 0" class="fab-badge">{{ cartStore.count }}</span>
  </div>

  <footer class="page-footer"><p>校园非遗与文创传承人平台 © 2026 码上非遗</p></footer>
</template>

<style scoped>
* { margin: 0; padding: 0; box-sizing: border-box; }

/* ===== 悬浮加号按钮 ===== */
.fab {
  position: fixed; bottom: 32px; right: 32px; z-index: 500;
  width: 60px; height: 60px; border-radius: 50%;
  background: linear-gradient(135deg, #c41e3a, #d43030);
  color: #fff; font-size: 32px; font-weight: 300;
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; box-shadow: 0 4px 16px rgba(196,30,58,0.4);
  transition: transform 0.2s, box-shadow 0.2s;
  user-select: none;
}
.fab:hover { transform: scale(1.1); box-shadow: 0 6px 24px rgba(196,30,58,0.55); }
.fab-icon { line-height: 1; }
.fab-badge {
  position: absolute; top: -4px; right: -4px;
  min-width: 22px; height: 22px; border-radius: 11px;
  background: #ffd700; color: #c41e3a;
  font-size: 12px; font-weight: 700;
  display: flex; align-items: center; justify-content: center;
  border: 2px solid #fff;
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
.page-footer { background-color: #c41e3a; color: #fff; text-align: center; padding: 20px 5%; font-size: 14px; }

@media (max-width: 768px) {
  .product-grid { grid-template-columns: repeat(auto-fill, minmax(160px, 1fr)); gap: 16px; }
  .fab { width: 50px; height: 50px; bottom: 20px; right: 20px; font-size: 28px; }
}
</style>
