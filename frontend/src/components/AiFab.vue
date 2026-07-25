<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()

// 仅在非 AI 页面、非管理员页面显示
const visible = computed(() => {
  const name = route.name as string
  return name !== 'Rag' && name !== 'Admin'
})

// 拖拽
const fabX = ref(window.innerWidth - 80)
const fabY = ref(window.innerHeight - 200)
let dragging = false
let startX = 0; let startY = 0
let originX = 0; let originY = 0

function onMouseDown(e: MouseEvent) {
  if ((e.target as HTMLElement).tagName === 'SPAN') {
    // 点击文字 → 跳转
    return
  }
  dragging = true
  startX = e.clientX; startY = e.clientY
  originX = fabX.value; originY = fabY.value
  e.preventDefault()
}

function onMouseMove(e: MouseEvent) {
  if (!dragging) return
  fabX.value = Math.max(20, Math.min(window.innerWidth - 70, originX + e.clientX - startX))
  fabY.value = Math.max(60, Math.min(window.innerHeight - 70, originY + e.clientY - startY))
}

function onMouseUp(e: MouseEvent) {
  if (!dragging) { return }
  dragging = false
  // 如果移动距离 < 5px 视为点击
  const dx = Math.abs(e.clientX - startX)
  const dy = Math.abs(e.clientY - startY)
  if (dx < 5 && dy < 5) {
    router.push('/rag')
  }
}

onMounted(() => {
  document.addEventListener('mousemove', onMouseMove)
  document.addEventListener('mouseup', onMouseUp)
})
onUnmounted(() => {
  document.removeEventListener('mousemove', onMouseMove)
  document.removeEventListener('mouseup', onMouseUp)
})
</script>

<template>
  <div v-if="visible" class="ai-fab"
    :style="{ left: fabX + 'px', top: fabY + 'px' }"
    @mousedown="onMouseDown"
    @touchstart="() => {}"
  >
    <svg class="fab-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
      <rect x="4" y="4" width="16" height="14" rx="3" />
      <circle cx="9" cy="10" r="1.5" fill="currentColor" stroke="none" />
      <circle cx="15" cy="10" r="1.5" fill="currentColor" stroke="none" />
      <line x1="9" y1="14" x2="15" y2="14" />
      <line x1="8" y1="2" x2="10" y2="4" />
      <line x1="16" y1="2" x2="14" y2="4" />
    </svg>
  </div>
</template>

<style scoped>
.ai-fab {
  position: fixed; z-index: 900;
  width: 56px; height: 56px; border-radius: 50%;
  background: linear-gradient(135deg, #c41e3a, #d43030);
  color: #fff; display: flex; align-items: center; justify-content: center;
  cursor: grab; box-shadow: 0 4px 18px rgba(196,30,58,0.4);
  user-select: none; transition: box-shadow 0.2s;
}
.ai-fab:active { cursor: grabbing; box-shadow: 0 6px 24px rgba(196,30,58,0.55); }
.fab-icon {
  width: 28px; height: 28px; pointer-events: none;
  transition: transform 0.3s ease;
}
.ai-fab:hover .fab-icon { transform: scale(1.15); }
</style>
