<script setup lang="ts">
import { ref, onMounted, watch, computed } from 'vue'
import VChart from 'vue-echarts'
import { use, registerMap } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { PieChart, BarChart, MapChart } from 'echarts/charts'
import {
  TitleComponent, TooltipComponent, LegendComponent,
  GridComponent, VisualMapComponent, GeoComponent,
} from 'echarts/components'

// 注册 ECharts 组件（按需引入）
use([
  CanvasRenderer, PieChart, BarChart, MapChart,
  TitleComponent, TooltipComponent, LegendComponent,
  GridComponent, VisualMapComponent, GeoComponent,
])

import { getAllStats, getCitiesByProvince } from '@/api/statistics'
import type { AllStats, StatItem, EndangeredItem } from '@/api/statistics'

const loading = ref(true)
const mapReady = ref(false)
const stats = ref<AllStats | null>(null)
const errorMsg = ref('')

// 下钻状态
const mapLevel = ref<'country' | 'province'>('country')
const currentProvince = ref('')
const provinceCityData = ref<StatItem[]>([])
const drilling = ref(false)

// 暖珊瑚陶土色阶梯（与朱砂红和谐不违和）
const CORAL_RAMP = ['#ffe8e0', '#f8c8b8', '#f09878', '#e07050', '#c05038', '#8b2a18']
const PIE_COLORS = ['#e07050', '#c05038', '#d07078', '#f8c8b8', '#ffe8e0']
const HEAT_RED = ['#ffe9ec', '#c41e3a']  // 热力图保留红色

// 中国全部34个省级行政区（与GeoJSON名称一致）
const ALL_PROVINCES = [
  '北京市','天津市','河北省','山西省','内蒙古自治区','辽宁省','吉林省','黑龙江省',
  '上海市','江苏省','浙江省','安徽省','福建省','江西省','山东省','河南省','湖北省',
  '湖南省','广东省','广西壮族自治区','海南省','重庆市','四川省','贵州省','云南省',
  '西藏自治区','陕西省','甘肃省','青海省','宁夏回族自治区','新疆维吾尔自治区',
  '台湾省','香港特别行政区','澳门特别行政区',
]

// 全部34个省级行政区→DataV编码（用于加载该省市级GeoJSON）
const PROVINCE_CODE: Record<string, string> = {
  '北京市': '110000', '天津市': '120000', '河北省': '130000', '山西省': '140000',
  '内蒙古自治区': '150000', '辽宁省': '210000', '吉林省': '220000', '黑龙江省': '230000',
  '上海市': '310000', '江苏省': '320000', '浙江省': '330000', '安徽省': '340000',
  '福建省': '350000', '江西省': '360000', '山东省': '370000', '河南省': '410000',
  '湖北省': '420000', '湖南省': '430000', '广东省': '440000', '广西壮族自治区': '450000',
  '海南省': '460000', '重庆市': '500000', '四川省': '510000', '贵州省': '520000',
  '云南省': '530000', '西藏自治区': '540000', '陕西省': '610000', '甘肃省': '620000',
  '青海省': '630000', '宁夏回族自治区': '640000', '新疆维吾尔自治区': '650000',
  '台湾省': '710000', '香港特别行政区': '810000', '澳门特别行政区': '820000',
}

// 城市坐标（含地级市，用于省份下钻时散点标注和热力填充）
const CITY_COORDS: Record<string, [number, number]> = {
  '苏州市': [120.59, 31.30], '无锡市': [120.31, 31.57], '湖州市': [120.09, 30.89],
  '宜兴市': [119.82, 31.36], '安吉县': [119.68, 30.64],
  '成都市': [104.07, 30.57], '景德镇市': [117.18, 29.27], '开封市': [114.31, 34.80],
  '泉州市': [118.59, 24.91], '大理市': [100.23, 25.61], '西安市': [108.94, 34.26],
  '广州市': [113.26, 23.13],
}

// 全国视图：省级热力图数据
const countryMapData = computed(() => {
  const provinceMap = new Map(ALL_PROVINCES.map(p => [p, 0]))
  if (stats.value?.region) {
    stats.value.region.forEach(r => { provinceMap.set(r.name, r.value) })
  }
  return Array.from(provinceMap, ([name, value]) => ({ name, value }))
})

// 省级视图：市级热力图数据
const provinceMapData = computed(() =>
  provinceCityData.value.map(d => ({ name: d.name, value: d.value }))
)

// 全国省级热力图配置
const countryMapOption = computed(() => ({
  title: { text: '已入驻中国非遗传承人全国热力图', subtext: '点击省份下钻查看市级明细 | 滚轮缩放', left: 'center', top: 6, textStyle: { color: '#c41e3a', fontSize: 16 }, subtextStyle: { color: '#bbb', fontSize: 12 } },
  tooltip: { trigger: 'item', formatter: (p: { name: string; value: number }) => `${p.name}: ${p.value || 0}人` },
  visualMap: { min: 0, max: Math.max(...countryMapData.value.map(d => d.value), 1), left: 20, bottom: 30, inRange: { color: HEAT_RED }, text: ['多', '少'], textStyle: { color: '#666', fontSize: 12 }, calculable: true },
  geo: { map: 'china', roam: true, scaleLimit: { min: 1, max: 10 }, label: { show: false }, emphasis: { label: { show: true, color: '#c41e3a', fontSize: 14, fontWeight: 'bold' }, itemStyle: { areaColor: '#ffe9ec' } }, itemStyle: { borderColor: '#e0e0e0', borderWidth: 0.5, areaColor: '#fafafa' } },
  series: [{ type: 'map', map: 'china', geoIndex: 0, data: countryMapData.value, selectedMode: false }]
}))

// 省份下钻市级热力图配置
const provinceMapOption = computed(() => ({
  title: { text: `${currentProvince.value}非遗传承人各市分布`, subtext: '双击地图或点击按钮返回全国视图', left: 'center', top: 6, textStyle: { color: '#c41e3a', fontSize: 16 }, subtextStyle: { color: '#bbb', fontSize: 12 } },
  tooltip: { trigger: 'item', formatter: (p: { name: string; value: number }) => `${p.name}: ${p.value || 0}人` },
  visualMap: { min: 0, max: Math.max(...provinceMapData.value.map(d => d.value), 1), left: 20, bottom: 30, inRange: { color: HEAT_RED }, text: ['多', '少'], textStyle: { color: '#666', fontSize: 12 }, calculable: true },
  geo: { map: currentProvince.value, roam: true, scaleLimit: { min: 1, max: 10 }, label: { show: true, color: '#888', fontSize: 11 }, emphasis: { label: { show: true, color: '#c41e3a', fontSize: 14, fontWeight: 'bold' }, itemStyle: { areaColor: '#ffe9ec' } }, itemStyle: { borderColor: '#e0e0e0', borderWidth: 0.5, areaColor: '#fafafa' } },
  series: [
    { type: 'map', map: currentProvince.value, geoIndex: 0, data: provinceMapData.value, selectedMode: false },
    { type: 'effectScatter', coordinateSystem: 'geo', rippleEffect: { brushType: 'stroke', scale: 3 }, symbolSize: 16, label: { show: true, formatter: (p: { name: string }) => p.name, position: 'right', fontSize: 12, color: '#c41e3a', fontWeight: 'bold' }, itemStyle: { color: '#c41e3a' }, data: provinceCityData.value.filter(d => CITY_COORDS[d.name]).map(d => ({ name: `${d.name} ${d.value}人`, value: [CITY_COORDS[d.name][0], CITY_COORDS[d.name][1]] })) }
  ]
}))

// 当前激活的地图配置
const mapOption = computed(() => mapLevel.value === 'country' ? countryMapOption.value : provinceMapOption.value)

// 点击省份 → 下钻到市级
async function drillDown(provinceName: string) {
  const code = PROVINCE_CODE[provinceName]
  if (!code) return
  drilling.value = true
  try {
    // 加载该省市级 GeoJSON
    const resp = await fetch(`https://geo.datav.aliyun.com/areas_v3/bound/${code}_full.json`)
    registerMap(provinceName, await resp.json() as any)
    // 请求该省市级数据
    const res = await getCitiesByProvince(provinceName)
    provinceCityData.value = res.data
    currentProvince.value = provinceName
    mapLevel.value = 'province'
  } catch { /* ignore */ }
  finally { drilling.value = false }
}

// 返回全国视图
function backToCountry() {
  mapLevel.value = 'country'
  currentProvince.value = ''
  provinceCityData.value = []
}

// 年龄饼图配置
const ageOption = computed(() => ({
  tooltip: { trigger: 'item', formatter: '{b}: {c}人 ({d}%)' },
  legend: { bottom: 0, textStyle: { color: '#666', fontSize: 12 } },
  series: [{
    type: 'pie', radius: ['50%', '78%'], center: ['50%', '46%'],
    itemStyle: { borderRadius: 4, borderColor: '#fff', borderWidth: 3 },
    label: { show: true, formatter: '{b}\n{d}%', fontSize: 12 },
    data: stats.value?.age?.map((d, i) => ({ ...d, itemStyle: { color: PIE_COLORS[i % PIE_COLORS.length] } })) || [],
  }]
}))

// 技艺柱状图配置
const craftOption = computed(() => {
  if (!stats.value?.craft?.length) return {}
  const data = [...stats.value.craft].sort((a, b) => a.value - b.value)
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 80, right: 50, top: 10, bottom: 24 },
    xAxis: { type: 'value', axisLine: { show: false }, axisTick: { show: false }, splitLine: { lineStyle: { color: '#f0f0f0' } } },
    yAxis: { type: 'category', data: data.map(d => d.name), axisLine: { show: false }, axisTick: { show: false }, axisLabel: { color: '#666', fontSize: 13 } },
    series: [{
      type: 'bar', data: data.map((d, i) => ({ value: d.value, itemStyle: { color: CORAL_RAMP[Math.min(i, CORAL_RAMP.length - 1)], borderRadius: [0, 4, 4, 0] } })),
      barWidth: 16, label: { show: true, position: 'right', color: '#999', fontSize: 12 }
    }]
  }
})

// 排行热门 TOP10 柱状图配置
const hotRankingOption = computed(() => {
  if (!stats.value?.hotRanking?.length) return {}
  const data = stats.value.hotRanking
  const TOP_COLORS = ['#e07050', '#d06848', '#c06040', '#b05838', '#a05030', '#904828', '#804020', '#703818', '#603010', '#502808']
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 100, right: 60, top: 10, bottom: 24 },
    xAxis: { type: 'value', axisLine: { show: false }, axisTick: { show: false }, splitLine: { lineStyle: { color: '#f0f0f0' } } },
    yAxis: { type: 'category', data: data.map(d => d.name), inverse: true, axisLine: { show: false }, axisTick: { show: false }, axisLabel: { color: '#666', fontSize: 13 } },
    series: [{
      type: 'bar', data: data.map((d, i) => ({ value: d.value, itemStyle: { color: TOP_COLORS[i] || '#c05038', borderRadius: [0, 4, 4, 0] } })),
      barWidth: 18, label: { show: true, position: 'right', color: '#999', fontSize: 12, formatter: '{c}人' }
    }]
  }
})

// 图表点击事件处理
function onChartClick(params: any) {
  if (mapLevel.value === 'country' && params.name && ALL_PROVINCES.includes(params.name)) {
    drillDown(params.name)
  }
}

function onChartDblClick() {
  if (mapLevel.value === 'province') {
    backToCountry()
  }
}

onMounted(async () => {
  // 1. 加载全国省份 GeoJSON
  try {
    const resp = await fetch('https://geo.datav.aliyun.com/areas_v3/bound/100000_full.json')
    registerMap('china', await resp.json() as any)
    mapReady.value = true
  } catch { /* 地图加载失败 */ }

  // 2. 加载统计数据
  try {
    const res = await getAllStats()
    stats.value = res.data
  } catch (e: unknown) {
    errorMsg.value = e instanceof Error ? e.message : '加载统计数据失败'
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <section class="stats-panel">
    <div class="section-header">
      <h2>数据统计</h2>
      <span class="header-sub">非遗传承人数据分析</span>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="loading-text">
      <span class="spinner"></span> 正在加载统计数据…
    </div>

    <!-- 错误 -->
    <div v-else-if="errorMsg" class="error-text">
      <p>{{ errorMsg }}</p>
      <p class="error-hint">请确认后端已重启，且已执行 seed_inheritor_profile.sql</p>
    </div>
    <!-- 空状态 -->
    <div v-else-if="!stats" class="empty-text">暂无统计数据</div>

    <!-- 数据就绪 -->
    <template v-else>
      <!-- 概览卡片 -->
      <div class="overview-grid">
        <div class="overview-card">
          <div class="ov-icon">🎨</div>
          <div class="ov-num">{{ stats.overview.totalInheritors }}</div>
          <div class="ov-label">传承人总数</div>
        </div>
        <div class="overview-card">
          <div class="ov-icon">📍</div>
          <div class="ov-num">{{ stats.overview.regionCount }}</div>
          <div class="ov-label">覆盖地区</div>
        </div>
        <div class="overview-card">
          <div class="ov-icon">📊</div>
          <div class="ov-num">{{ stats.overview.avgAge.toFixed(1) }}</div>
          <div class="ov-label">平均年龄</div>
        </div>
      </div>

      <!-- 中国地图热力图（省-市下钻） -->
      <div class="chart-card chart-full map-wrapper heatmap-card">
        <div class="map-top-row">
          <h3 class="heatmap-title">{{ mapLevel === 'country' ? '已入驻中国非遗传承人全国热力图' : `${currentProvince}非遗传承人各市分布` }}</h3>
          <span class="map-hint">{{ mapLevel === 'country' ? '点击省份下钻到市 | 滚轮缩放' : '双击地图或点击按钮返回全国' }}</span>
        </div>
        <button v-if="mapLevel === 'province'" class="back-btn" @click="backToCountry">← 返回全国视图</button>
        <div class="chart-box map-box">
          <VChart v-if="mapReady && !drilling" :option="mapOption" autoresize @click="onChartClick" @dblclick="onChartDblClick" />
          <div v-else class="map-fallback">{{ drilling ? '正在加载市级地图…' : '地图数据加载中…' }}</div>
        </div>
      </div>

      <!-- 图表网格：年龄 + 技艺 同行 -->
      <div class="charts-grid">
        <!-- 年龄饼图 -->
        <div class="chart-card">
          <h3>传承人年龄分布</h3>
          <div class="chart-box">
            <VChart :option="ageOption" autoresize />
          </div>
        </div>
        <!-- 技艺柱状图 -->
        <div class="chart-card">
          <h3>非遗技艺类型分布</h3>
          <div class="chart-box">
            <VChart v-if="stats.craft?.length" :option="craftOption" autoresize />
            <div v-else class="empty-inner">暂无技艺数据</div>
          </div>
        </div>
      </div>

      <!-- 濒危预警看板 -->
      <div class="chart-card chart-full endangered-section">
        <div class="endangered-header">
          <h3>⚠️ 濒危预警看板</h3>
          <span class="endangered-sub">传承人数 ≤ 3 人的非遗技艺（按濒危程度排序）</span>
        </div>
        <div v-if="stats.endangered?.length" class="endangered-grid">
          <div v-for="item in stats.endangered" :key="item.craft" class="endangered-card" :class="'level-' + (item.level === '严重濒危' ? 'critical' : item.level === '高度濒危' ? 'high' : 'warn')">
            <div class="endangered-level">{{ item.level }}</div>
            <div class="endangered-craft">{{ item.craft }}</div>
            <div class="endangered-stats">
              <span>传承人：<strong>{{ item.count }}</strong> 人</span>
              <span>平均年龄：<strong>{{ item.avgAge }}</strong> 岁</span>
            </div>
            <div class="endangered-inheritors">
              <span v-for="ih in item.inheritors" :key="ih.name" class="ih-tag">
                {{ ih.name }}（{{ ih.age }}岁）
              </span>
            </div>
          </div>
        </div>
        <div v-else class="empty-inner">🎉 暂无濒危项目，所有技艺传承状况良好</div>
      </div>

      <!-- 排行热门 -->
      <div class="chart-card chart-full">
        <h3>🏆 排行热门 · 非遗技艺传承人数量 TOP10</h3>
        <div class="chart-box craft-box">
          <VChart v-if="stats.hotRanking?.length" :option="hotRankingOption" autoresize />
          <div v-else class="empty-inner">暂无排行数据</div>
        </div>
      </div>
    </template>
  </section>
</template>

<style scoped>
.stats-panel { }

/* ===== 标题 ===== */
.section-header { display: flex; align-items: baseline; gap: 14px; margin-bottom: 28px; }
.section-header h2 { font-size: 22px; color: #c41e3a; padding-left: 14px; border-left: 4px solid #c41e3a; margin: 0; }

/* 热力图卡片保留红色标题 */
.heatmap-title { color: #c41e3a !important; border-left-color: #c41e3a !important; }
.header-sub { font-size: 13px; color: #bbb; }

/* ===== 加载动画 ===== */
.spinner {
  display: inline-block; width: 18px; height: 18px; border: 2px solid #f0f0f0;
  border-top-color: #c41e3a; border-radius: 50%; animation: spin 0.8s linear infinite; vertical-align: -4px; margin-right: 6px;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* ===== 概览卡片（3D立体） ===== */
.overview-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; margin-bottom: 28px; }
.overview-card {
  background: linear-gradient(145deg, #fff 0%, #fefaf5 100%);
  border-radius: 16px; padding: 28px 20px; text-align: center;
  border: 1px solid rgba(196, 30, 58, 0.1);
  box-shadow:
    0 4px 6px rgba(139, 26, 43, 0.06),
    0 10px 20px rgba(139, 26, 43, 0.04),
    inset 0 1px 0 rgba(255,255,255,0.6);
  transform: perspective(500px) rotateX(0deg);
  transition: transform 0.35s cubic-bezier(0.22,0.61,0.36,1),
              box-shadow 0.35s ease;
}
.overview-card:hover {
  transform: perspective(500px) rotateX(2deg) translateY(-6px);
  box-shadow:
    0 8px 16px rgba(139, 26, 43, 0.12),
    0 16px 32px rgba(139, 26, 43, 0.06),
    inset 0 1px 0 rgba(255,255,255,0.6);
}
.ov-icon { font-size: 30px; margin-bottom: 8px; filter: drop-shadow(0 2px 2px rgba(0,0,0,0.1)); }
.ov-num { font-size: 38px; font-weight: 800; color: #c41e3a; text-shadow: 0 1px 2px rgba(196,30,58,0.15); }
.ov-label { font-size: 13px; color: #999; margin-top: 4px; }

/* ===== 图表卡片（3D立体） ===== */
.charts-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin-bottom: 20px; }
.chart-card {
  background: linear-gradient(145deg, #fff 0%, #fefaf5 100%);
  border-radius: 16px; padding: 24px;
  border: 1px solid rgba(196, 30, 58, 0.08);
  box-shadow:
    0 4px 6px rgba(139, 26, 43, 0.05),
    0 8px 16px rgba(139, 26, 43, 0.03);
  transform: perspective(600px) rotateX(0deg);
  transition: transform 0.4s cubic-bezier(0.22,0.61,0.36,1),
              box-shadow 0.4s ease;
}
.chart-card:hover {
  transform: perspective(600px) rotateX(2deg) translateY(-4px);
  box-shadow:
    0 8px 20px rgba(139, 26, 43, 0.1),
    0 20px 40px rgba(139, 26, 43, 0.05);
}
.chart-card h3 {
  font-size: 15px; color: #8b6914; margin: 0 0 16px;
  padding-left: 10px; border-left: 3px solid #d4a030;
}
.chart-box { width: 100%; height: 340px; }
.map-wrapper { position: relative; }
.map-top-row { display: flex; align-items: baseline; gap: 14px; margin-bottom: 4px; }
.map-top-row h3 { margin-bottom: 0 !important; }
.map-hint { font-size: 12px; color: #bbb; }
.back-btn {
  position: absolute; top: 20px; left: 24px; z-index: 10;
  padding: 8px 18px; font-size: 13px; color: #fff; background: #c41e3a; border: none;
  border-radius: 8px; cursor: pointer; font-weight: 600;
  box-shadow: 0 2px 10px rgba(196,30,58,0.3); transition: all 0.2s;
}
.back-btn:hover { background: #a8162b; transform: translateY(-1px); box-shadow: 0 4px 16px rgba(196,30,58,0.4); }

.map-box { height: 720px; }
.craft-box { height: 300px; }
.chart-full { margin-bottom: 20px; }

/* ===== 状态 ===== */
.loading-text { text-align: center; padding: 80px 20px; color: #999; font-size: 14px; }
.error-text { text-align: center; padding: 80px 20px; color: #e74c3c; font-size: 14px; }
.error-hint { font-size: 12px; color: #bbb; margin-top: 10px; }
.empty-text { text-align: center; padding: 80px 20px; color: #ccc; font-size: 14px; }
.empty-inner { text-align: center; padding: 80px 20px; color: #ccc; font-size: 13px; }
.map-fallback { text-align: center; padding: 100px 20px; color: #ccc; font-size: 14px; }

/* ===== 濒危预警看板 ===== */
.endangered-section { border-left: 4px solid #dc3545; }
.endangered-header { display: flex; align-items: baseline; gap: 12px; margin-bottom: 20px; }
.endangered-header h3 { margin-bottom: 0 !important; color: #dc3545 !important; border-left-color: #dc3545 !important; }
.endangered-sub { font-size: 12px; color: #999; }
.endangered-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 16px; }
.endangered-card {
  background: linear-gradient(145deg, #fff 0%, #fff8f5 100%);
  border-radius: 12px; padding: 18px 20px;
  border: 1px solid rgba(220, 53, 69, 0.15);
  transition: transform 0.3s, box-shadow 0.3s;
}
.endangered-card:hover { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(220,53,69,0.1); }
.endangered-level {
  display: inline-block; padding: 3px 12px; border-radius: 10px;
  font-size: 12px; font-weight: 700; letter-spacing: 1px;
}
.level-critical .endangered-level { background: #ffe0e0; color: #dc3545; border: 1px solid #f5c6cb; }
.level-high .endangered-level { background: #fff3e0; color: #e65100; border: 1px solid #ffcc80; }
.level-warn .endangered-level { background: #fffde0; color: #f9a825; border: 1px solid #fff9c4; }
.endangered-craft { font-size: 17px; font-weight: 700; color: #333; margin: 10px 0 8px; }
.endangered-stats { display: flex; gap: 20px; font-size: 13px; color: #888; margin-bottom: 8px; }
.endangered-stats strong { color: #c41e3a; }
.endangered-inheritors { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 8px; }
.ih-tag {
  padding: 3px 10px; background: #f5f5f5; border-radius: 12px;
  font-size: 12px; color: #666; border: 1px solid #eee;
}

@media (max-width: 992px) {
  .charts-grid { grid-template-columns: 1fr; }
  .overview-grid { grid-template-columns: 1fr; }
  .map-box { height: 400px; }
}
</style>
