<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { listMyProducts, addProduct, updateProduct, offProduct, onProduct } from '@/api/product'
import type { ProductVO } from '@/types/api'

const products = ref<ProductVO[]>([])
const loading = ref(false)
const showEditor = ref(false)
const editingId = ref<number | null>(null)

const categories = ['剪纸', '刺绣', '陶艺', '竹编', '手工艺品', '文具礼品', '家居装饰', '其他']

const form = ref({
  name: '',
  category: '',
  description: '',
  price: 0,
  stock: 0,
  imageUrl: '',
})

const submitMsg = ref('')

/** 图片加载失败时回退到占位符 */
const failedImages = ref(new Set<number>())

function onImgError(productId: number) {
  failedImages.value.add(productId)
}

onMounted(() => { loadProducts() })

async function loadProducts() {
  loading.value = true
  try {
    const res = await listMyProducts()
    products.value = res.data
  } catch { /* ignore */ } finally { loading.value = false }
}

function openCreate() {
  editingId.value = null
  form.value = { name: '', category: '', description: '', price: 0, stock: 0, imageUrl: '' }
  showEditor.value = true
  submitMsg.value = ''
}

function openEdit(product: ProductVO) {
  editingId.value = product.id
  form.value = {
    name: product.name,
    category: product.category || '',
    description: product.description || '',
    price: product.price,
    stock: product.stock,
    imageUrl: product.imageUrl || '',
  }
  showEditor.value = true
  submitMsg.value = ''
}

async function handleSave() {
  if (!form.value.name) { submitMsg.value = '请填写商品名称'; return }
  try {
    if (editingId.value) {
      await updateProduct(editingId.value, form.value)
    } else {
      await addProduct(form.value)
    }
    showEditor.value = false
    await loadProducts()
  } catch { submitMsg.value = '保存失败' }
}

async function handleToggle(product: ProductVO) {
  try {
    if (product.status === 1) {
      await offProduct(product.id)
    } else {
      await onProduct(product.id)
    }
    await loadProducts()
  } catch { /* ignore */ }
}
</script>

<template>
  <section class="tab-content">
    <div class="section-header">
      <h2>商品设置</h2>
      <button class="btn-primary" @click="openCreate">+ 上架商品</button>
    </div>

    <!-- ==================== 编辑弹窗 ==================== -->
    <div v-if="showEditor" class="modal-overlay" @click.self="showEditor = false">
      <div class="modal-content">
        <div class="modal-header">
          <h3>{{ editingId ? '编辑商品' : '上架新商品' }}</h3>
          <button class="modal-close" @click="showEditor = false">✕</button>
        </div>
        <div class="modal-body">
          <div class="form-row">
            <div class="form-group flex-2">
              <label>商品名称 *</label>
              <input v-model="form.name" type="text" class="form-input" placeholder="商品名称" />
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
              <label>价格 (元)</label>
              <input v-model.number="form.price" type="number" min="0" step="0.01" class="form-input" />
            </div>
            <div class="form-group flex-1">
              <label>库存</label>
              <input v-model.number="form.stock" type="number" min="0" class="form-input" />
            </div>
          </div>
          <div class="form-group">
            <label>图片链接</label>
            <input v-model="form.imageUrl" type="text" class="form-input" placeholder="商品图片URL" />
          </div>
          <div class="form-group">
            <label>商品描述</label>
            <textarea v-model="form.description" class="form-textarea" rows="4" placeholder="商品详情..."></textarea>
          </div>
          <div v-if="submitMsg" class="submit-msg error">{{ submitMsg }}</div>
        </div>
        <div class="modal-footer">
          <button class="btn-outline" @click="showEditor = false">取消</button>
          <button class="btn-primary" @click="handleSave">保存</button>
        </div>
      </div>
    </div>

    <!-- ==================== 加载状态 ==================== -->
    <div v-if="loading" class="empty-state">加载中…</div>

    <!-- ==================== 空状态 ==================== -->
    <div v-else-if="products.length === 0" class="empty-state">
      <div class="empty-hint">
        <div class="empty-icon">📦</div>
        <p>暂无商品</p>
        <button class="btn-primary" @click="openCreate">+ 上架第一件商品</button>
      </div>
    </div>

    <!-- ==================== 商品卡片网格 ==================== -->
    <div v-else class="product-grid">
      <!-- 添加商品卡片 -->
      <div class="product-card add-card" @click="openCreate">
        <div class="add-card-content">
          <span class="add-icon">+</span>
          <span class="add-text">上架新商品</span>
        </div>
      </div>

      <!-- 商品卡片 -->
      <div v-for="product in products" :key="product.id" class="product-card">
        <div class="product-img">
          <img
            v-if="product.imageUrl && !failedImages.has(product.id)"
            :src="product.imageUrl"
            :alt="product.name"
            class="product-img-real"
            @error="onImgError(product.id)"
          />
          <div v-if="!product.imageUrl || failedImages.has(product.id)" class="img-placeholder">
            {{ product.category || '非遗' }}
          </div>
        </div>
        <div class="product-body">
          <div class="product-category-tag">{{ product.category || '-' }}</div>
          <h3 class="product-name">{{ product.name }}</h3>
          <p class="product-desc">{{ product.description || '暂无描述' }}</p>
          <div class="product-footer">
            <span class="product-price">¥{{ product.price.toFixed(2) }}</span>
            <span class="product-stock" v-if="product.stock > 0">库存 {{ product.stock }}</span>
            <span class="product-stock sold-out" v-else>已售罄</span>
          </div>
          <div class="product-meta">
            <span class="product-time">{{ product.createTime?.slice(0, 10) }}</span>
            <span class="product-status" :class="product.status === 1 ? 'published' : 'draft'">
              {{ product.status === 1 ? '已上架' : '已下架' }}
            </span>
          </div>
          <div class="product-actions">
            <button class="btn-edit" @click.stop="openEdit(product)">编辑</button>
            <button
              class="btn-toggle"
              :class="product.status === 1 ? 'btn-delist' : 'btn-relist'"
              @click.stop="handleToggle(product)"
            >
              {{ product.status === 1 ? '下架' : '上架' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
/* ==================== 区块头部 ==================== */
.tab-content { }
.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}
.section-header h2 {
  font-size: 22px;
  color: #c41e3a;
  padding-left: 14px;
  border-left: 4px solid #c41e3a;
}

/* ==================== 通用按钮 ==================== */
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

/* ==================== 编辑弹窗 ==================== */
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
  width: 560px;
  max-width: 90vw;
  max-height: 85vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 8px 30px rgba(0,0,0,0.2);
}
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
  padding: 4px 8px;
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

/* ==================== 表单 ==================== */
.form-row { display: flex; gap: 16px; }
.form-group { margin-bottom: 16px; }
.form-group label {
  display: block;
  font-size: 13px;
  color: #666;
  margin-bottom: 5px;
  font-weight: 600;
}
.flex-2 { flex: 2; }
.flex-1 { flex: 1; }
.form-input {
  width: 100%;
  height: 42px;
  padding: 0 14px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  box-sizing: border-box;
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
  padding: 14px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  resize: vertical;
  box-sizing: border-box;
  outline: none;
  font-family: inherit;
  line-height: 1.8;
  background: #fafafa;
}
.form-textarea:focus {
  border-color: #c41e3a;
  box-shadow: 0 0 0 3px rgba(196,30,58,0.08);
  background: #fff;
}
.submit-msg { font-size: 13px; margin-top: 4px; }
.submit-msg.error { color: #dc3545; }

/* ==================== 空状态 ==================== */
.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #999;
  font-size: 14px;
}
.empty-hint {
  background: #fff;
  border-radius: 16px;
  padding: 48px 40px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.06);
  display: inline-block;
}
.empty-icon { font-size: 48px; margin-bottom: 12px; }
.empty-hint p { margin-bottom: 20px; }

/* ==================== 商品卡片网格 ==================== */
.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(270px, 1fr));
  gap: 24px;
}

/* ==================== 商品卡片 ==================== */
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

/* ---- 商品图片 ---- */
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
.product-card:hover .product-img-real { transform: scale(1.05); }
.img-placeholder {
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

/* ---- 卡片内容 ---- */
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
  margin-bottom: 10px;
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

/* ---- 元信息行 ---- */
.product-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
  padding-top: 10px;
  border-top: 1px solid #f5f5f5;
}
.product-time {
  font-size: 12px;
  color: #aaa;
}
.product-status {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 600;
}
.product-status.published {
  background: #d4edda;
  color: #155724;
}
.product-status.draft {
  background: #e2e3e5;
  color: #383d41;
}

/* ---- 操作按钮 ---- */
.product-actions {
  display: flex;
  gap: 10px;
}
.btn-edit {
  flex: 1;
  padding: 8px 0;
  font-size: 13px;
  color: #c41e3a;
  background: #fff;
  border: 1px solid #c41e3a;
  border-radius: 6px;
  cursor: pointer;
  transition: 0.2s;
  font-weight: 600;
}
.btn-edit:hover { background: #fff5f7; }
.btn-toggle {
  flex: 1;
  padding: 8px 0;
  font-size: 13px;
  color: #fff;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: 0.2s;
  font-weight: 600;
}
.btn-delist {
  background: #dc3545;
}
.btn-delist:hover { background: #c82333; }
.btn-relist {
  background: #28a745;
}
.btn-relist:hover { background: #218838; }

/* ==================== 添加商品卡片 ==================== */
.add-card {
  cursor: pointer;
  border: 2px dashed #e0c0c7;
  background: #fffbfb;
  box-shadow: none;
  min-height: 380px;
}
.add-card:hover {
  border-color: #c41e3a;
  background: #fff5f7;
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(196,30,58,0.10);
}
.add-card-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  min-height: 380px;
  gap: 12px;
}
.add-icon {
  font-size: 48px;
  color: #c41e3a;
  font-weight: 300;
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: #ffe9ec;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: 0.2s;
}
.add-card:hover .add-icon {
  background: #c41e3a;
  color: #fff;
}
.add-text {
  font-size: 15px;
  color: #c41e3a;
  font-weight: 600;
}

/* ==================== 响应式 ==================== */
@media (max-width: 768px) {
  .product-grid {
    grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
    gap: 16px;
  }
  .form-row { flex-direction: column; gap: 0; }
  .product-img { height: 160px; }
}
</style>
