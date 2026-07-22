<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { listMyOrders } from '@/api/order'
import { listMyProductOrders, cancelProductOrder, completeProductOrder } from '@/api/product'
import type { OrderVO } from '@/types/api'
import type { ProductOrderVO } from '@/types/api'

const router = useRouter()
const authStore = useAuthStore()
const isInheritor = authStore.isInheritor

onMounted(() => {
  document.title = '我的订单 — 码上非遗'
  loadAppointments()
  if (isInheritor) loadProductOrders()
})

type TabKey = 'appointment' | 'product'
const activeTab = ref<TabKey>('appointment')

// ===== 工坊预约 =====
const appointments = ref<OrderVO[]>([])
const apptLoading = ref(false)
const apptStatusMap: Record<number, { label: string; cls: string }> = {
  0: { label: '待确认', cls: 's-pending' },
  1: { label: '已确认', cls: 's-confirmed' },
  2: { label: '已完成', cls: 's-done' },
  3: { label: '已取消', cls: 's-cancel' },
}

async function loadAppointments() {
  apptLoading.value = true
  try { appointments.value = (await listMyOrders()).data || [] }
  catch { /* */ } finally { apptLoading.value = false }
}

// ===== 文创订单 =====
const productOrders = ref<ProductOrderVO[]>([])
const poLoading = ref(false)
const poStatusMap: Record<number, { label: string; cls: string }> = {
  0: { label: '待发货', cls: 's-pending' },
  1: { label: '已发货', cls: 's-shipped' },
  2: { label: '已完成', cls: 's-done' },
  3: { label: '已取消', cls: 's-cancel' },
}

async function loadProductOrders() {
  poLoading.value = true
  try { productOrders.value = (await listMyProductOrders()).data || [] }
  catch { /* */ } finally { poLoading.value = false }
}

function viewOrderDetail(orderId: number) {
  router.push(`/order/${orderId}`)
}

async function cancelPOOrder(orderId: number) {
  if (!confirm('确定要取消该订单吗？')) return
  try { await cancelProductOrder(orderId); await loadProductOrders() }
  catch { /* */ }
}

async function handleConfirmReceive(orderId: number) {
  if (!confirm('确认已收到商品？')) return
  try { await completeProductOrder(orderId); await loadProductOrders() }
  catch { /* */ }
}
</script>

<template>
  <header class="page-header">
    <div class="logo" @click="router.push('/')">
      <img src="/logo.png" alt="码上非遗" class="logo-img" />
      <span class="logo-text">码上非遗</span>
    </div>
    <a @click="router.back()" class="back-link">← 返回</a>
  </header>

  <main class="orders-page">
    <h2>我的订单</h2>

    <!-- Tab 切换 -->
    <div class="tab-bar">
      <button class="tab-btn" :class="{ active: activeTab === 'appointment' }" @click="activeTab = 'appointment'">工坊预约</button>
      <button class="tab-btn" :class="{ active: activeTab === 'product' }" @click="activeTab = 'product'; loadProductOrders()">文创订单</button>
    </div>

    <!-- 工坊预约 -->
    <div v-if="activeTab === 'appointment'" class="tab-content">
      <div v-if="apptLoading" class="loading">加载中...</div>
      <div v-else-if="appointments.length === 0" class="empty">暂无工坊预约</div>
      <table v-else class="order-table">
        <thead><tr><th>传承人</th><th>预约日期</th><th>时段</th><th>人数</th><th>备注</th><th>状态</th></tr></thead>
        <tbody>
          <tr v-for="o in appointments" :key="o.id">
            <td>{{ o.inheritorName }}</td>
            <td>{{ o.date }}</td>
            <td>{{ o.timeSlot }}</td>
            <td>{{ o.peopleCount }}人</td>
            <td class="rmk">{{ o.remark }}</td>
            <td><span class="status" :class="apptStatusMap[o.status]?.cls">{{ apptStatusMap[o.status]?.label }}</span></td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 文创订单 -->
    <div v-if="activeTab === 'product'" class="tab-content">
      <div v-if="poLoading" class="loading">加载中...</div>
      <div v-else-if="productOrders.length === 0" class="empty">暂无文创订单</div>
      <table v-else class="order-table">
        <thead><tr><th>商品</th><th>单价</th><th>数量</th><th>金额</th><th>电话</th><th>地址</th><th>状态</th><th>时间</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="o in productOrders" :key="o.id">
            <td>{{ o.productName }}</td>
            <td>¥{{ o.price?.toFixed(2) }}</td>
            <td>{{ o.quantity }}</td>
            <td>¥{{ o.totalAmount?.toFixed(2) }}</td>
            <td>{{ o.phone }}</td>
            <td class="rmk">{{ o.address }}</td>
            <td><span class="status" :class="poStatusMap[o.status]?.cls">{{ poStatusMap[o.status]?.label }}</span></td>
            <td>{{ o.createTime?.slice(0, 10) }}</td>
            <td class="action-col">
              <button class="btn-view" @click="viewOrderDetail(o.id)">查看</button>
              <button v-if="o.status === 0" class="btn-cancel" @click="cancelPOOrder(o.id)">取消</button>
              <button v-if="o.status === 1" class="btn-confirm-receive" @click="handleConfirmReceive(o.id)">确认收货</button>
            </td>
          </tr>
        </tbody>
      </table>
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

.orders-page { max-width: 1000px; margin: 0 auto; padding: 32px 5%; min-height: 70vh; }
.orders-page h2 { color: #c41e3a; margin-bottom: 24px; font-size: 24px; }

.tab-bar { display: flex; gap: 0; margin-bottom: 24px; border-bottom: 2px solid #e8e0d8; }
.tab-btn {
  padding: 10px 28px; font-size: 15px; font-weight: 600; cursor: pointer;
  background: none; border: none; color: #999; font-family: inherit;
  border-bottom: 2px solid transparent; margin-bottom: -2px; transition: all 0.2s;
}
.tab-btn:hover { color: #c41e3a; }
.tab-btn.active { color: #c41e3a; border-bottom-color: #c41e3a; }

.tab-content { background: #fff; border-radius: 12px; overflow: hidden; box-shadow: 0 2px 12px rgba(0,0,0,0.06); }

.order-table { width: 100%; border-collapse: collapse; }
.order-table th, .order-table td { padding: 14px 16px; text-align: left; font-size: 14px; }
.order-table thead { background: #fff5f7; }
.order-table th { color: #c41e3a; font-weight: 600; font-size: 13px; }
.order-table tbody tr { border-bottom: 1px solid #f5f5f5; transition: 0.2s; }
.order-table tbody tr:hover { background: #fefafa; }
.rmk { max-width: 140px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.status { display: inline-block; padding: 3px 10px; border-radius: 12px; font-size: 12px; font-weight: 600; }
.s-pending { background: #fff3cd; color: #856404; }
.s-confirmed { background: #d4edda; color: #155724; }
.s-shipped { background: #cce5ff; color: #004085; }
.s-done { background: #e2e3e5; color: #383d41; }
.s-cancel { background: #f8d7da; color: #721c24; }

.loading, .empty { text-align: center; padding: 60px; color: #999; font-size: 14px; }
.action-col { white-space: nowrap; }
.btn-view {
  padding: 4px 12px; font-size: 12px; color: #c41e3a; background: #fff;
  border: 1px solid #c41e3a; border-radius: 4px; cursor: pointer; transition: 0.2s; margin-right: 6px;
}
.btn-view:hover { background: #c41e3a; color: #fff; }
.btn-cancel {
  padding: 4px 12px; font-size: 12px; color: #dc3545; background: #fff;
  border: 1px solid #dc3545; border-radius: 4px; cursor: pointer; transition: 0.2s;
}
.btn-cancel:hover { background: #dc3545; color: #fff; }
.btn-confirm-receive {
  padding: 4px 12px; font-size: 12px; color: #fff; background: #28a745;
  border: none; border-radius: 4px; cursor: pointer; transition: 0.2s;
}
.btn-confirm-receive:hover { background: #218838; }
</style>
