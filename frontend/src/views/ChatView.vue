<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import ChatPanel from './components/ChatPanel.vue'

const router = useRouter()

onMounted(() => {
  document.title = '消息 — 码上非遗'
})
</script>

<template>
  <div class="chat-page-body">
    <ChatPanel />
  </div>
</template>

<style scoped>
.chat-page-header {
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
.chat-page-body {
  padding: 20px 5%;
  min-height: calc(100vh - 56px);
  background: #fcf4e8;
}
</style>
