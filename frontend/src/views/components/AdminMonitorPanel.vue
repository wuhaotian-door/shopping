<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'

use([CanvasRenderer, BarChart, LineChart, PieChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent])

import { getMonitorData } from '@/api/statistics'
import type { MonitorData } from '@/api/statistics'

const loading = ref(true)
const data = ref<MonitorData | null>(null)
const errorMsg = ref('')

// 主题色系
const THEME = '#c41e3a'
const GRADIENT = ['#c41e3a', '#e8647a', '#f0a0af', '#f5c5cd', '#fadde2']

onMounted(async () => {
  try {
    const res = await getMonitorData()
    data.value = res.data
  } catch (e: unknown) {
    errorMsg.value = e instanceof Error ? e.message : '加载失败'
  } finally { loading.value = false }
})

// 注册趋势面积图
const regOption = computed(() => {
  if (!data.value?.registrationTrend?.length) return {}
  const months = data.value.registrationTrend.map(d => d.month)
  return {
    tooltip: { trigger: 'axis' },
    legend: { data: ['新增用户', '新增传承人', '合计'], bottom: 0, textStyle: { fontSize: 12 } },
    grid: { left: 50, right: 20, top: 20, bottom: 40 },
    xAxis: { type: 'category', data: months, axisLabel: { rotate: 30, fontSize: 11 }, boundaryGap: false },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#f0f0f0' } } },
    series: [
      {
        name: '新增用户', type: 'line', smooth: true, symbol: 'circle', symbolSize: 8,
        data: data.value.registrationTrend.map(d => d.users),
        lineStyle: { width: 3, color: '#c41e3a' },
        itemStyle: { color: '#c41e3a' },
        areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(196,30,58,0.3)' }, { offset: 1, color: 'rgba(196,30,58,0.02)' }] } },
      },
      {
        name: '新增传承人', type: 'line', smooth: true, symbol: 'diamond', symbolSize: 8,
        data: data.value.registrationTrend.map(d => d.inheritors),
        lineStyle: { width: 3, color: '#e8647a' },
        itemStyle: { color: '#e8647a' },
        areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(232,100,122,0.3)' }, { offset: 1, color: 'rgba(232,100,122,0.02)' }] } },
      },
      {
        name: '合计', type: 'line', smooth: true, symbol: 'triangle', symbolSize: 8,
        data: data.value.registrationTrend.map(d => d.total),
        lineStyle: { width: 2, type: 'dashed', color: '#999' },
        itemStyle: { color: '#999' },
        areaStyle: { opacity: 0 },
      }
    ]
  }
})

// 订单状态 3D 立体饼图
const orderPieOption = computed(() => {
  if (!data.value?.orderStats?.statusDistribution?.length) return {}
  return {
    tooltip: { trigger: 'item', formatter: '{b}: {c}单 ({d}%)' },
    legend: { bottom: 0, textStyle: { fontSize: 12 } },
    series: [{
      type: 'pie', radius: ['55%', '80%'], center: ['50%', '45%'],
      emphasis: { scaleSize: 12, label: { fontSize: 16, fontWeight: 'bold' } },
      itemStyle: {
        borderRadius: 6, borderColor: '#fff', borderWidth: 4,
        shadowBlur: 16, shadowOffsetX: 0, shadowOffsetY: 4, shadowColor: 'rgba(0,0,0,0.1)',
      },
      label: { show: true, formatter: '{b}\n{d}%', fontSize: 12 },
      data: data.value.orderStats.statusDistribution.map((d, i) => ({
        ...d,
        itemStyle: { color: ['#c41e3a', '#e8647a', '#f0a0af', '#ddd'][i] || '#c41e3a' }
      }))
    }]
  }
})

// 月度柱状图（带3D阴影）
const totalOrders = computed(() => data.value?.orderStats?.totalOrders || 0)
</script>

<template>
  <div class="monitor-panel">
    <div v-if="loading" class="loading-text">加载监控数据…</div>
    <div v-else-if="errorMsg" class="error-text">{{ errorMsg }}</div>
    <template v-else-if="data">
      <!-- 顶部 KPI -->
      <div class="kpi-row">
        <div class="kpi-card">
          <div class="kpi-icon">👥</div>
          <div class="kpi-num">{{ data.overview.totalInheritors }}</div>
          <div class="kpi-label">活跃传承人</div>
        </div>
        <div class="kpi-card accent-green">
          <div class="kpi-icon">📦</div>
          <div class="kpi-num">{{ totalOrders }}</div>
          <div class="kpi-label">商品订单总数</div>
        </div>
        <div class="kpi-card accent-purple">
          <div class="kpi-icon">📍</div>
          <div class="kpi-num">{{ data.overview.regionCount }}</div>
          <div class="kpi-label">覆盖地区</div>
        </div>
        <div class="kpi-card accent-orange">
          <div class="kpi-icon">📊</div>
          <div class="kpi-num">{{ data.overview.avgAge.toFixed(1) }}</div>
          <div class="kpi-label">平均年龄</div>
        </div>
      </div>

      <!-- 图表区 -->
      <div class="charts-row">
        <!-- 注册趋势面积图 -->
        <div class="chart-card chart-wide">
          <h3>📈 网站注册趋势</h3>
          <div class="chart-body">
            <VChart v-if="data.registrationTrend.length" :option="regOption" autoresize />
            <div v-else class="empty-inner">暂无注册数据</div>
          </div>
        </div>

        <!-- 订单状态3D饼图 -->
        <div class="chart-card">
          <h3>🛒 商品订单状态分布</h3>
          <div class="chart-body">
            <VChart v-if="data.orderStats?.statusDistribution?.length" :option="orderPieOption" autoresize />
            <div v-else class="empty-inner">暂无订单数据</div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.monitor-panel { }

/* KPI 卡片 */
.kpi-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; margin-bottom: 24px; }
.kpi-card {
  background: #fff; border-radius: 14px; padding: 22px 20px; text-align: center;
  box-shadow: 0 3px 14px rgba(0,0,0,0.06); border-top: 4px solid #c41e3a;
  transition: transform 0.25s, box-shadow 0.25s;
}
.kpi-card:hover { transform: translateY(-4px); box-shadow: 0 8px 24px rgba(196,30,58,0.12); }
.kpi-card.accent-green  { border-top-color: #27ae60; }
.kpi-card.accent-purple { border-top-color: #8e44ad; }
.kpi-card.accent-orange { border-top-color: #e67e22; }
.kpi-icon { font-size: 26px; margin-bottom: 6px; }
.kpi-num { font-size: 34px; font-weight: bold; color: #c41e3a; }
.kpi-card.accent-green .kpi-num  { color: #27ae60; }
.kpi-card.accent-purple .kpi-num { color: #8e44ad; }
.kpi-card.accent-orange .kpi-num { color: #e67e22; }
.kpi-label { font-size: 12px; color: #999; margin-top: 4px; }

/* 图表 */
.charts-row { display: grid; grid-template-columns: 1.5fr 1fr; gap: 20px; }
.chart-card {
  background: #fff; border-radius: 14px; padding: 22px;
  box-shadow: 0 3px 14px rgba(0,0,0,0.06);
}
.chart-card h3 { font-size: 15px; color: #c41e3a; margin: 0 0 12px; padding-left: 10px; border-left: 3px solid #c41e3a; }
.chart-body { width: 100%; height: 340px; }
.chart-wide .chart-body { height: 360px; }

.loading-text, .error-text { text-align: center; padding: 40px; color: #999; }
.error-text { color: #e74c3c; }
.empty-inner { text-align: center; padding: 60px; color: #ccc; font-size: 13px; }

@media (max-width: 992px) {
  .charts-row { grid-template-columns: 1fr; }
  .kpi-row { grid-template-columns: repeat(2, 1fr); }
}
</style>
