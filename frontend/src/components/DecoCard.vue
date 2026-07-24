<script setup lang="ts">
/**
 * 登录/注册页 — 国风装饰卡片
 * 剪纸纹样 + 宣纸肌理 + 祥云 + 回纹 + 鎏金光点 + 鼠标3D悬浮
 */
import { ref } from 'vue'
const props = defineProps<{ title?: string; subtitle?: string; descLines?: string[] }>()

const cardRef = ref<HTMLElement | null>(null)
const tiltX = ref(0)
const tiltY = ref(0)
const isHovering = ref(false)
const glowX = ref(50)
const glowY = ref(50)

function onMouseMove(e: MouseEvent) {
  if (!cardRef.value) return
  const rect = cardRef.value.getBoundingClientRect()
  const x = e.clientX - rect.left - rect.width / 2
  const y = e.clientY - rect.top - rect.height / 2
  tiltX.value = -(y / rect.height) * 18  // -9° ~ 9°
  tiltY.value = (x / rect.width) * 18
  glowX.value = ((e.clientX - rect.left) / rect.width) * 100
  glowY.value = ((e.clientY - rect.top) / rect.height) * 100
}

function onMouseEnter() { isHovering.value = true }
function onMouseLeave() {
  isHovering.value = false
  tiltX.value = 0
  tiltY.value = 0
  glowX.value = 50
  glowY.value = 50
}

// 光点数据
const sparkles = Array.from({ length: 10 }, (_, i) => ({
  left: 5 + Math.random() * 90,
  delay: Math.random() * 5,
  duration: 4 + Math.random() * 4,
  size: 2 + Math.random() * 3,
  opacity: 0.3 + Math.random() * 0.4,
}))
</script>

<template>
  <div class="float-card"
    ref="cardRef"
    :class="{ hovering: isHovering }"
    :style="{
      '--mouse-x': glowX + '%',
      '--mouse-y': glowY + '%',
      transform: isHovering
        ? `perspective(1000px) translateY(-6px) rotateX(${tiltX}deg) rotateY(${tiltY}deg)`
        : 'perspective(1000px) translateY(0) rotateX(0) rotateY(0)',
      transition: isHovering ? 'transform 0.15s ease-out, box-shadow 0.2s ease' : 'transform 0.4s ease, box-shadow 0.4s ease',
    }"
    @mousemove="onMouseMove"
    @mouseenter="onMouseEnter"
    @mouseleave="onMouseLeave"
  >
    <!-- 宣纸肌理 -->
    <div class="paper-texture"></div>

    <!-- 背景光晕层 -->
    <div class="bg-glow glow-1"></div>
    <div class="bg-glow glow-2"></div>

    <!-- 鼠标跟随流光 -->
    <div class="flow-light"></div>

    <!-- 剪纸纹样 SVG -->
    <svg class="pattern-svg" viewBox="0 0 480 520" preserveAspectRatio="xMidYMid slice">
      <!-- 左上窗花 -->
      <g transform="translate(50, 50)" opacity="0.14">
        <circle cx="0" cy="0" r="16" fill="none" stroke="#fff" stroke-width="1.2"/>
        <circle cx="0" cy="0" r="8" fill="none" stroke="#fff" stroke-width="0.8"/>
        <circle cx="18" cy="0" r="6" fill="none" stroke="#fff" stroke-width="0.8"/>
        <circle cx="-18" cy="0" r="6" fill="none" stroke="#fff" stroke-width="0.8"/>
        <circle cx="0" cy="18" r="6" fill="none" stroke="#fff" stroke-width="0.8"/>
        <circle cx="0" cy="-18" r="6" fill="none" stroke="#fff" stroke-width="0.8"/>
      </g>

      <!-- 左上祥云 -->
      <g transform="translate(120, 55)" opacity="0.1">
        <ellipse cx="0" cy="0" rx="24" ry="10" fill="none" stroke="#e6b85c" stroke-width="1"/>
        <ellipse cx="18" cy="-4" rx="16" ry="7" fill="none" stroke="#e6b85c" stroke-width="0.8"/>
        <ellipse cx="-16" cy="-3" rx="14" ry="6" fill="none" stroke="#e6b85c" stroke-width="0.8"/>
      </g>

      <!-- 右上回纹 -->
      <g transform="translate(420, 60)" opacity="0.12">
        <polyline points="-20,0 -8,-10 4,0 16,-10 28,0" fill="none" stroke="#fff" stroke-width="1.2"/>
        <polyline points="-20,10 -8,0 4,10 16,0 28,10" fill="none" stroke="#fff" stroke-width="0.9" opacity="0.6"/>
      </g>

      <!-- 右下窗花 -->
      <g transform="translate(410, 450)" opacity="0.12">
        <circle cx="0" cy="0" r="14" fill="none" stroke="#fff" stroke-width="1"/>
        <polygon points="0,-10 9,-3 6,7 -6,7 -9,-3" fill="none" stroke="#fff" stroke-width="0.8"/>
        <circle cx="0" cy="0" r="4" fill="none" stroke="#fff" stroke-width="0.6"/>
      </g>

      <!-- 右下祥云 -->
      <g transform="translate(350, 470)" opacity="0.08">
        <ellipse cx="0" cy="0" rx="20" ry="8" fill="none" stroke="#e6b85c" stroke-width="0.8"/>
        <ellipse cx="14" cy="-3" rx="13" ry="6" fill="none" stroke="#e6b85c" stroke-width="0.7"/>
        <ellipse cx="-12" cy="-2" rx="11" ry="5" fill="none" stroke="#e6b85c" stroke-width="0.7"/>
      </g>

      <!-- 左下回纹 -->
      <g transform="translate(60, 470)" opacity="0.1">
        <polyline points="-16,0 -4,-8 8,0 20,-8" fill="none" stroke="#fff" stroke-width="1"/>
      </g>

      <!-- 中上窗花 -->
      <g transform="translate(240, 40)" opacity="0.08">
        <circle cx="0" cy="0" r="10" fill="none" stroke="#e6b85c" stroke-width="0.8"/>
        <circle cx="12" cy="0" r="5" fill="none" stroke="#e6b85c" stroke-width="0.6"/>
        <circle cx="-12" cy="0" r="5" fill="none" stroke="#e6b85c" stroke-width="0.6"/>
        <circle cx="0" cy="12" r="5" fill="none" stroke="#e6b85c" stroke-width="0.6"/>
        <circle cx="0" cy="-12" r="5" fill="none" stroke="#e6b85c" stroke-width="0.6"/>
      </g>

      <!-- 菱形竹编纹 -->
      <g transform="translate(100, 360)" opacity="0.08">
        <polygon points="0,-12 8,0 0,12 -8,0" fill="none" stroke="#e6b85c" stroke-width="0.8"/>
        <polygon points="0,-5 3,0 0,5 -3,0" fill="none" stroke="#e6b85c" stroke-width="0.6"/>
      </g>
      <g transform="translate(380, 220)" opacity="0.07">
        <polygon points="0,-10 7,0 0,10 -7,0" fill="none" stroke="#e6b85c" stroke-width="0.7"/>
      </g>
    </svg>

    <!-- 浮动光点 -->
    <div class="float-deco deco1"></div>
    <div class="float-deco deco2"></div>
    <div class="float-deco deco3"></div>

    <!-- 鎏金飘散粒子 -->
    <div
      v-for="s in sparkles" :key="'sp'+s.left"
      class="gold-sparkle"
      :style="{
        left: s.left + '%',
        animationDelay: s.delay + 's',
        animationDuration: s.duration + 's',
        width: s.size + 'px',
        height: s.size + 'px',
        opacity: s.opacity,
      }"
    ></div>

    <!-- 文字内容 -->
    <div class="float-text-wrap">
      <h2>{{ title || '码上非遗' }}</h2>
      <p class="slogan">{{ subtitle || '感受非遗魅力' }}</p>
      <p v-for="line in (descLines || ['汇聚剪纸、刺绣、陶艺、竹编传统技艺', '连接校园与民间手工艺传承人'])" :key="line" class="desc">{{ line }}</p>
    </div>
  </div>
</template>

<style scoped>
.float-card {
  width: 100%; max-width: 480px; height: 520px;
  border-radius: 18px;
  background: linear-gradient(160deg, #8b1a2b 0%, #b82c38 30%, #c41e3a 60%, #d44550 100%);
  position: relative; overflow: hidden;
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 8px 30px rgba(196,30,58,0.2), 0 0 60px rgba(196,30,58,0.08);
  cursor: pointer;
  transform-style: preserve-3d;
  --mouse-x: 50%; --mouse-y: 50%;
}
.float-card.hovering { box-shadow: 0 14px 44px rgba(196,30,58,0.35), 0 0 90px rgba(196,30,58,0.15); }

/* 宣纸肌理 */
.paper-texture {
  position: absolute; inset: 0;
  background:
    repeating-linear-gradient(0deg, transparent, transparent 2px, rgba(255,255,255,0.015) 2px, rgba(255,255,255,0.015) 4px),
    repeating-linear-gradient(90deg, transparent, transparent 3px, rgba(255,255,255,0.01) 3px, rgba(255,255,255,0.01) 6px);
  pointer-events: none; z-index: 1;
}

/* 背景光晕 */
.bg-glow { position: absolute; border-radius: 50%; filter: blur(60px); pointer-events: none; z-index: 0; }
.glow-1 { width: 260px; height: 260px; top: -60px; right: -40px; background: rgba(232,100,122,0.25); }
.glow-2 { width: 200px; height: 200px; bottom: -40px; left: -30px; background: rgba(200,80,90,0.2); }

/* 鼠标跟随流光 */
.flow-light {
  position: absolute; width: 120%; height: 120%; top: -10%; left: -10%;
  background: radial-gradient(circle at var(--mouse-x,50%) var(--mouse-y,50%), rgba(255,255,255,0.2), transparent 40%);
  opacity: 0; transition: opacity 0.25s ease; pointer-events: none; z-index: 2;
}
.float-card.hovering .flow-light { opacity: 1; }

/* SVG 纹样 */
.pattern-svg { position: absolute; inset: 0; width: 100%; height: 100%; pointer-events: none; z-index: 1; }

/* 圆形光斑 */
.float-deco {
  position: absolute; border-radius: 50%;
  background: rgba(255,255,255,0.06);
  pointer-events: none; z-index: 1;
}
.deco1 { width: 180px; height: 180px; top: 40px; left: 40px; animation: decoFloat 6s ease-in-out infinite; }
.deco2 { width: 120px; height: 120px; bottom: 80px; right: 60px; animation: decoFloat 7s ease-in-out 2s infinite; }
.deco3 { width: 70px; height: 70px; top: 160px; right: 120px; animation: decoFloat 5s ease-in-out 3s infinite; }

@keyframes decoFloat {
  0%,100% { transform: translateY(0) scale(1); }
  50% { transform: translateY(-15px) scale(1.1); }
}

/* 鎏金粒子 */
.gold-sparkle {
  position: absolute; bottom: -10px;
  background: #e6b85c; border-radius: 50%;
  pointer-events: none; z-index: 1;
  animation: sparkleRise linear infinite;
}
@keyframes sparkleRise {
  0% { transform: translateY(0) scale(0); opacity: 0; }
  10% { opacity: 1; transform: scale(1); }
  90% { opacity: 0.6; }
  100% { transform: translateY(-550px) scale(0.3); opacity: 0; }
}

/* 文字 */
.float-text-wrap { z-index: 3; text-align: center; color: #fff; padding: 0 30px; }
.float-text-wrap h2 { font-size: 42px; margin-bottom: 10px; letter-spacing: 6px; font-weight: 700; text-shadow: 0 2px 8px rgba(0,0,0,0.15); }
.slogan { font-size: 20px; margin-bottom: 24px; opacity: 0.9; letter-spacing: 4px; }
.desc { font-size: 14px; line-height: 1.8; opacity: 0.75; }
</style>
