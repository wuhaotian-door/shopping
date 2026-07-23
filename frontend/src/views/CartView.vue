<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useCartStore } from '@/stores/cart'
import { submitProductOrder } from '@/api/product'

const router = useRouter()
const authStore = useAuthStore()
const cartStore = useCartStore()

onMounted(() => { document.title = '购物车 — 码上非遗' })

const isLoggedIn = computed(() => authStore.isLoggedIn)
const userName = computed(() => authStore.userInfo?.name)

// 下单
const showOrderForm = ref(false)
const orderForm = ref({ phone: '', address: '', remark: '' })
const orderMsg = ref('')
const submitting = ref(false)

function goCheckout() {
  if (!isLoggedIn.value) { router.push('/login'); return }
  showOrderForm.value = true
}

async function submit() {
  if (!orderForm.value.phone || !orderForm.value.address) {
    orderMsg.value = '请填写联系电话和收货地址'; return
  }
  submitting.value = true
  try {
    for (const item of cartStore.items) {
      await submitProductOrder({
        productId: item.product.id, quantity: item.quantity,
        phone: orderForm.value.phone, address: orderForm.value.address,
        remark: orderForm.value.remark,
      })
    }
    orderMsg.value = '下单成功！'
    cartStore.clear()
    setTimeout(() => { router.push('/store') }, 1500)
  } catch { orderMsg.value = '下单失败，请稍后重试' }
  finally { submitting.value = false }
}
</script>

<template>
  <header class="page-header">
    <div class="logo" @click="router.push('/')">
      <img src="/logo.png" alt="码上非遗" class="logo-img" />
      <span class="logo-text">码上非遗</span>
    </div>
    <a @click="router.push('/store')" class="back-link">← 返回文创周边</a>
  </header>

  <main class="cart-page">
    <h2>我的购物车</h2>

    <div v-if="cartStore.items.length === 0" class="empty">
      <p>购物车是空的</p>
      <button @click="router.push('/store')">去逛逛</button>
    </div>

    <template v-else>
      <div class="cart-list">
        <div v-for="(item, index) in cartStore.items" :key="item.product.id" class="cart-row">
          <div class="cart-info">
            <span class="cart-name">{{ item.product.name }}</span>
            <span class="cart-cat">{{ item.product.category }}</span>
          </div>
          <span class="cart-price">¥{{ item.product.price.toFixed(2) }}</span>
          <div class="cart-qty">
            <button @click="cartStore.updateQty(index, item.quantity - 1, authStore.uid)">−</button>
            <span>{{ item.quantity }}</span>
            <button @click="cartStore.updateQty(index, item.quantity + 1)">+</button>
          </div>
          <span class="cart-sub">¥{{ (item.product.price * item.quantity).toFixed(2) }}</span>
          <button class="cart-del" @click="cartStore.remove(index, authStore.uid)">删除</button>
        </div>
      </div>

      <div class="cart-footer">
        <span class="total">合计：<strong>¥{{ cartStore.total.toFixed(2) }}</strong></span>
        <button class="checkout-btn" @click="goCheckout">去结算</button>
      </div>
    </template>

    <!-- 下单表单 -->
    <div v-if="showOrderForm" class="modal-overlay" @click.self="showOrderForm = false">
      <div class="modal-box">
        <h3>填写收货信息</h3>
        <input v-model="orderForm.phone" placeholder="联系电话（必填）" class="field" />
        <input v-model="orderForm.address" placeholder="收货地址（必填）" class="field" />
        <input v-model="orderForm.remark" placeholder="备注（可选）" class="field" />
        <p v-if="orderMsg" :class="orderMsg.includes('成功') ? 'msg-ok' : 'msg-err'">{{ orderMsg }}</p>
        <div class="modal-btns">
          <button class="btn-cancel" @click="showOrderForm = false">取消</button>
          <button class="btn-submit" :disabled="submitting" @click="submit">{{ submitting ? '提交中…' : '确认下单' }}</button>
        </div>
      </div>
    </div>
  </main>
</template>

<style scoped>
.page-header {
  background: linear-gradient(135deg, #8b1a2b 0%, #a01e35 40%, #c41e3a 100%);
  color: #fff; padding: 0 6%; height: 72px;
  display: flex; align-items: center; justify-content: space-between;
  font-family: 'Noto Serif SC','STKaiti','KaiTi','楷体','STSong',serif;
  box-shadow: 0 2px 16px rgba(139,26,43,0.35);
}
.logo { display: flex; align-items: center; gap: 10px; cursor: pointer; }
.logo-img { height: 42px; }
.logo-text { font-size: 22px; font-weight: 700; letter-spacing: 4px; }
.back-link { color: rgba(255,255,255,0.85); cursor: pointer; font-size: 15px; letter-spacing: 2px; }
.back-link:hover { color: #fff; }

.cart-page { max-width: 800px; margin: 0 auto; padding: 40px 5%; min-height: 70vh; }
.cart-page h2 { color: #c41e3a; margin-bottom: 28px; font-size: 24px; }

.empty { text-align: center; padding: 80px 20px; color: #999; }
.empty button { margin-top: 16px; padding: 10px 28px; background: #c41e3a; color: #fff; border: none; border-radius: 6px; cursor: pointer; font-size: 14px; }

.cart-list { background: #fff; border-radius: 12px; overflow: hidden; box-shadow: 0 2px 12px rgba(0,0,0,0.06); }
.cart-row { display: flex; align-items: center; gap: 16px; padding: 16px 20px; border-bottom: 1px solid #f0f0f0; }
.cart-info { flex: 1; }
.cart-name { display: block; font-weight: 600; color: #333; }
.cart-cat { font-size: 12px; color: #999; }
.cart-price { width: 80px; color: #c41e3a; font-weight: 600; }
.cart-qty { display: flex; align-items: center; gap: 8px; }
.cart-qty button { width: 28px; height: 28px; border: 1px solid #ddd; border-radius: 4px; background: #fff; cursor: pointer; font-size: 16px; }
.cart-qty span { min-width: 24px; text-align: center; font-weight: 600; }
.cart-sub { width: 80px; font-weight: 600; color: #333; }
.cart-del { padding: 4px 10px; border: none; background: none; color: #c41e3a; cursor: pointer; font-size: 13px; }
.cart-del:hover { text-decoration: underline; }

.cart-footer { display: flex; align-items: center; justify-content: flex-end; gap: 20px; padding: 20px; background: #fff; margin-top: 16px; border-radius: 12px; box-shadow: 0 2px 12px rgba(0,0,0,0.06); }
.total strong { font-size: 22px; color: #c41e3a; }
.checkout-btn { padding: 12px 36px; background: #c41e3a; color: #fff; border: none; border-radius: 6px; font-size: 16px; cursor: pointer; font-weight: 600; }

.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.35); display: flex; align-items: center; justify-content: center; z-index: 300; }
.modal-box { background: #fff; border-radius: 12px; padding: 32px; width: 420px; max-width: 90vw; }
.modal-box h3 { margin-bottom: 20px; color: #c41e3a; }
.field { width: 100%; padding: 10px 14px; border: 1px solid #ddd; border-radius: 6px; margin-bottom: 12px; font-size: 14px; outline: none; box-sizing: border-box; }
.field:focus { border-color: #c41e3a; }
.msg-ok { color: #28a745; font-size: 13px; }
.msg-err { color: #c41e3a; font-size: 13px; }
.modal-btns { display: flex; gap: 12px; justify-content: flex-end; margin-top: 16px; }
.btn-cancel { padding: 10px 20px; border: 1px solid #ddd; border-radius: 6px; background: #fff; cursor: pointer; }
.btn-submit { padding: 10px 24px; background: #c41e3a; color: #fff; border: none; border-radius: 6px; cursor: pointer; font-weight: 600; }
.btn-submit:disabled { opacity: 0.5; }
</style>
