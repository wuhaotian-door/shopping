<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import '@google/model-viewer'

defineProps<{
  modelUrl?: string
  posterImage: string
  productName: string
  autoRotate?: boolean
  credit?: string
}>()

const containerRef = ref<HTMLElement | null>(null)
const isInViewport = ref(false)
const modelError = ref(false)
const modelLoaded = ref(false)

let observer: IntersectionObserver | null = null

onMounted(() => {
  if (!containerRef.value) return
  observer = new IntersectionObserver(
    ([entry]) => {
      if (entry.isIntersecting) {
        isInViewport.value = true
        observer?.disconnect()
        observer = null
      }
    },
    { rootMargin: '200px' }
  )
  observer.observe(containerRef.value)
})

onUnmounted(() => {
  observer?.disconnect()
  observer = null
})

function onModelError() {
  modelError.value = true
}

function onModelLoad() {
  modelLoaded.value = true
}
</script>

<template>
  <div ref="containerRef" class="p3d-container">
    <!-- 骨架屏占位 -->
    <div v-if="!modelLoaded && !modelError && modelUrl && isInViewport" class="p3d-skeleton">
      <div class="skeleton-shimmer"></div>
      <img :src="posterImage" :alt="productName" class="skeleton-poster" />
    </div>

    <!-- 3D 预览 -->
    <div v-if="modelUrl && !modelError && isInViewport" class="p3d-viewer-wrap">
      <model-viewer
        :src="modelUrl"
        :alt="productName"
        :auto-rotate="autoRotate !== false"
        :camera-controls="true"
        :shadow-intensity="0.6"
        :exposure="1"
        environment-image="neutral"
        @error="onModelError"
        @load="onModelLoad"
        class="p3d-viewer"
      />
      <div class="p3d-hint">拖拽旋转 查看3D细节</div>
      <div v-if="credit" class="p3d-credit">{{ credit }}</div>
    </div>

    <!-- 降级：静态图片 -->
    <div v-if="!modelUrl || modelError || !isInViewport" class="p3d-fallback">
      <img
        v-if="posterImage"
        :src="posterImage"
        :alt="productName"
        class="p3d-poster"
      />
      <div v-else class="p3d-placeholder">{{ productName.charAt(0) }}</div>
    </div>
  </div>
</template>

<style scoped>
.p3d-container {
  width: 100%;
  height: 450px;
  position: relative;
  overflow: hidden;
  border-radius: 10px;
  background: #fcf4e8;
}

/* 骨架屏 */
.p3d-skeleton {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fcf4e8;
}
.skeleton-shimmer {
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, transparent 0%, rgba(0,0,0,0.04) 50%, transparent 100%);
  animation: shimmer 1.5s infinite;
}
@keyframes shimmer {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(100%); }
}
.skeleton-poster {
  max-width: 60%;
  max-height: 60%;
  object-fit: contain;
  opacity: 0.3;
  filter: blur(8px);
}

/* 3D 查看器 */
.p3d-viewer-wrap {
  width: 100%;
  height: 100%;
  position: relative;
}
.p3d-viewer {
  width: 100%;
  height: 100%;
  --poster-color: transparent;
}
.p3d-hint {
  position: absolute;
  bottom: 10px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 11px;
  color: rgba(0, 0, 0, 0.3);
  user-select: none;
  pointer-events: none;
}
.p3d-credit {
  position: absolute;
  bottom: 10px;
  right: 12px;
  font-size: 10px;
  color: rgba(0, 0, 0, 0.25);
  user-select: none;
}

/* 降级静态图 */
.p3d-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fcf4e8;
}
.p3d-poster {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.p3d-placeholder {
  width: 120px;
  height: 120px;
  border-radius: 16px;
  background: linear-gradient(135deg, #e8647a, #c41e3a);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 48px;
  font-weight: bold;
}
</style>