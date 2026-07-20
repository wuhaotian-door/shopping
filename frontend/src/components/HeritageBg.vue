<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'

const mouseX = ref(0)
const mouseY = ref(0)

function onMouseMove(e: MouseEvent) {
  mouseX.value = (e.clientX / window.innerWidth - 0.5) * 2  // -1 ~ 1
  mouseY.value = (e.clientY / window.innerHeight - 0.5) * 2
}

onMounted(() => window.addEventListener('mousemove', onMouseMove))
onUnmounted(() => window.removeEventListener('mousemove', onMouseMove))
</script>

<template>
  <div class="heritage-bg">
    <!-- 宣纸底 -->
    <div class="bg-paper"></div>

    <!-- 墨晕（鼠标跟随微移） -->
    <div class="ink-blot ink-1" :style="{ transform: `translate(${mouseX * 18}px, ${mouseY * 14}px)` }"></div>
    <div class="ink-blot ink-2" :style="{ transform: `translate(${mouseX * -12}px, ${mouseY * -10}px)` }"></div>

    <!-- 装饰粒子 -->
    <div
      v-for="n in 6" :key="n"
      class="float-particle"
      :style="{
        left: (10 + n * 15) + '%',
        top: (15 + (n % 3) * 28) + '%',
        animationDelay: (n * 0.7) + 's',
        transform: `translate(${mouseX * (4 + n)}px, ${mouseY * (3 + n % 2)}px)`,
      }"
    ></div>

    <!-- 淡纹饰边框 -->
    <div class="corner-deco corner-tl"></div>
    <div class="corner-deco corner-tr"></div>
    <div class="corner-deco corner-bl"></div>
    <div class="corner-deco corner-br"></div>
  </div>
</template>

<style scoped>
.heritage-bg {
  position: fixed; inset: 0; z-index: 0;
  pointer-events: none; overflow: hidden;
}

.bg-paper {
  position: absolute; inset: 0;
  background:
    repeating-linear-gradient(0deg, transparent, transparent 2px, rgba(196,30,58,0.006) 2px, rgba(196,30,58,0.006) 4px),
    repeating-linear-gradient(90deg, transparent, transparent 3px, rgba(196,30,58,0.004) 3px, rgba(196,30,58,0.004) 6px),
    radial-gradient(ellipse at 30% 20%, rgba(196,30,58,0.03) 0%, transparent 60%),
    radial-gradient(ellipse at 70% 50%, rgba(196,30,58,0.02) 0%, transparent 50%),
    radial-gradient(ellipse at 50% 85%, rgba(180,120,80,0.03) 0%, transparent 50%);
}

/* 淡墨晕 */
.ink-blot {
  position: absolute;
  border-radius: 50%;
  filter: blur(90px);
  opacity: 0.45;
  transition: transform 0.6s ease-out;
}
.ink-1 {
  width: 420px; height: 420px;
  top: -8%; left: -5%;
  background: radial-gradient(circle, rgba(210,160,150,0.25) 0%, transparent 70%);
}
.ink-2 {
  width: 380px; height: 380px;
  bottom: -6%; right: -4%;
  background: radial-gradient(circle, rgba(200,140,130,0.2) 0%, transparent 70%);
}

/* 浮动粒子 */
.float-particle {
  position: absolute;
  width: 6px; height: 6px;
  border-radius: 50%;
  background: rgba(196, 30, 58, 0.12);
  animation: particleDrift 8s ease-in-out infinite;
  transition: transform 0.5s ease-out;
}
@keyframes particleDrift {
  0%, 100% { transform: translate(0, 0); opacity: 0.5; }
  25%  { transform: translate(16px, -20px); opacity: 0.8; }
  50%  { transform: translate(-10px, -8px); opacity: 0.4; }
  75%  { transform: translate(12px, 14px); opacity: 0.6; }
}

/* 角落纹饰 */
.corner-deco {
  position: absolute;
  width: 100px; height: 100px;
  opacity: 0.06;
  border-color: #c41e3a;
  border-style: solid;
}
.corner-tl { top: 0; left: 0; border-width: 1px 0 0 1px; }
.corner-tr { top: 0; right: 0; border-width: 1px 1px 0 0; }
.corner-bl { bottom: 0; left: 0; border-width: 0 0 1px 1px; }
.corner-br { bottom: 0; right: 0; border-width: 0 1px 1px 0; }
</style>
