<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { listInheritorOrders, confirmOrder, rejectOrder, completeOrder } from '@/api/order'
import type { OrderVO } from '@/types/api'

const orderList = ref<OrderVO[]>([])
const orderLoading = ref(false)

onMounted(() => { loadOrders() })

const statusMap: Record<number, { text: string; cls: string }> = {
  0: { text: '待确认', cls: 'status-pending' },
  1: { text: '已确认', cls: 'status-confirmed' },
  2: { text: '已完成', cls: 'status-completed' },
  3: { text: '已取消', cls: 'status-cancelled' },
}

async function loadOrders() {
  orderLoading.value = true
  try {
    const res = await listInheritorOrders()
    orderList.value = res.data
  } catch { /* ignore */ } finally { orderLoading.value = false }
}

async function handleOrderAction(order: OrderVO, action: 'confirm' | 'reject' | 'complete') {
  try {
    if (action === 'confirm') await confirmOrder(order.id)
    else if (action === 'reject') await rejectOrder(order.id)
    else if (action === 'complete') await completeOrder(order.id)
    await loadOrders()
  } catch { /* ignore */ }
}

defineExpose({ loadOrders })
</script>

<template>
  <section class="tab-content">
    <div class="section-header">
      <h2>预约订单</h2>
      <div class="order-stats">
        <span class="stat-item">待确认 <strong>{{ orderList.filter(o => o.status === 0).length }}</strong></span>
        <span class="stat-item">已确认 <strong>{{ orderList.filter(o => o.status === 1).length }}</strong></span>
        <span class="stat-item">已完成 <strong>{{ orderList.filter(o => o.status === 2).length }}</strong></span>
      </div>
    </div>

    <div class="table-wrap">
      <table class="order-table">
        <thead>
          <tr>
            <th>用户</th><th>联系电话</th><th>预约日期</th><th>时段</th>
            <th>人数</th><th>备注</th><th>状态</th><th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="order in orderList" :key="order.id">
            <td>{{ order.userName }}</td>
            <td>{{ order.phone }}</td>
            <td>{{ order.appointmentDate }}</td>
            <td>{{ order.timeSlot }}</td>
            <td>{{ order.peopleCount }}人</td>
            <td class="remark-cell">{{ order.remark }}</td>
            <td><span class="status-tag" :class="statusMap[order.status]?.cls">{{ statusMap[order.status]?.text }}</span></td>
            <td class="action-cell">
              <template v-if="order.status === 0">
                <button class="btn-confirm" @click="handleOrderAction(order, 'confirm')">确认</button>
                <button class="btn-reject" @click="handleOrderAction(order, 'reject')">拒绝</button>
              </template>
              <template v-else-if="order.status === 1">
                <button class="btn-confirm" @click="handleOrderAction(order, 'complete')">完成</button>
              </template>
              <span v-else class="no-action">—</span>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-if="orderLoading" class="empty-state">加载中…</div>
      <div v-else-if="orderList.length === 0" class="empty-state">暂无预约订单</div>
    </div>
  </section>
</template>

<style scoped>
.tab-content { }
.section-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 24px; }
.section-header h2 { font-size: 22px; color: #c41e3a; padding-left: 14px; border-left: 4px solid #c41e3a; }
.order-stats { display: flex; gap: 20px; }
.stat-item { font-size: 14px; color: #666; }
.stat-item strong { color: #c41e3a; font-size: 18px; margin-left: 4px; }
.table-wrap { background: #fff; border-radius: 12px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.06); }
.order-table { width: 100%; border-collapse: collapse; }
.order-table th, .order-table td { padding: 14px 16px; text-align: left; font-size: 14px; }
.order-table thead { background: #fff5f7; }
.order-table th { color: #c41e3a; font-weight: 600; font-size: 13px; }
.order-table tbody tr { border-bottom: 1px solid #f5f5f5; }
.order-table tbody tr:hover { background: #fefafa; }
.remark-cell { max-width: 160px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.action-cell { white-space: nowrap; }
.no-action { color: #ccc; }
.status-tag { display: inline-block; padding: 3px 10px; border-radius: 12px; font-size: 12px; font-weight: 600; }
.status-pending { background: #fff3cd; color: #856404; }
.status-confirmed { background: #d4edda; color: #155724; }
.status-completed { background: #e2e3e5; color: #383d41; }
.status-cancelled { background: #f8d7da; color: #721c24; }
.btn-confirm { padding: 5px 14px; font-size: 12px; color: #fff; background: #28a745; border: none; border-radius: 4px; cursor: pointer; margin-right: 6px; }
.btn-confirm:hover { background: #218838; }
.btn-reject { padding: 5px 14px; font-size: 12px; color: #fff; background: #dc3545; border: none; border-radius: 4px; cursor: pointer; }
.btn-reject:hover { background: #c82333; }
.empty-state { text-align: center; padding: 60px 20px; color: #999; font-size: 14px; }
</style>
