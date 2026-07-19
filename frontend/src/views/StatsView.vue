<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import StatsPanel from './components/StatsPanel.vue'

const router = useRouter()

onMounted(() => {
  document.title = '数据统计 — 码上非遗'
})
</script>

<template>
  <div class="stats-page-body">
    <StatsPanel />
  </div>
</template>

<style scoped>
.stats-page-header {
  background: #c41e3a;
  color: #fff;
  padding: 0 5%;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.header-left { display: flex; align-items: center; gap: 16px; }
.logo { font-size: 18px; font-weight: bold; cursor: pointer; }
.header-title { font-size: 14px; opacity: 0.85; }
.back-btn {
  padding: 6px 16px; font-size: 13px; color: #fff; background: rgba(255,255,255,0.15);
  border: 1px solid rgba(255,255,255,0.4); border-radius: 4px; cursor: pointer;
}
.back-btn:hover { background: rgba(255,255,255,0.3); }
.stats-page-body {
  padding: 32px 5%;
  min-height: calc(100vh - 56px);
  max-width: 1200px;
  margin: 0 auto;
  background: #fcf4e8;
}
</style>
