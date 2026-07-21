<script setup lang="ts">
/**
 * 码上非遗 — 首页 Banner（3D视差 + 剪纸纹样 + 水波特效 + 分层z-index）
 *
 * z-index 分层（从底到顶）:
 *   z1: 宣纸底色 + 水墨远山 3D 视差层
 *   z2: 漂浮剪纸纹样层 + 金色粒子
 *   z3: 左上红日（呼吸柔光）
 *   z4: 水波点击特效（pointer-events:none）
 *   z5: 顶层文字标题「码上非遗」（最高层级，永不遮挡）
 */

import { ref } from 'vue'

// ===== 可修改变量 =====
const H      = 520
const SUN_CX = 130  // 红日圆心
const SUN_CY = 110
const PARALLAX_MAX = 12  // 3D 偏移最大像素

// ===== 主题色 =====
const GOLD  = '#e6b85c'
const RED   = '#c41e3a'
const PAPER = '#fcf4e8'

// ===== 鼠标位置（3D 视差驱动） =====
const mx = ref(0) // -0.5 ~ 0.5
const my = ref(0)

// ===== 悬浮 / 波纹 =====
const hovering = ref(false)
const ripples  = ref<{ x: number; y: number; id: number }[]>([])
let rid = 0

function onMove(e: MouseEvent) {
  const rect = (e.currentTarget as HTMLElement).getBoundingClientRect()
  mx.value = (e.clientX - rect.left) / rect.width  - 0.5
  my.value = (e.clientY - rect.top)  / rect.height - 0.5
}
function onEnter() { hovering.value = true }
function onLeave() { hovering.value = false; mx.value = 0; my.value = 0 }

function onClick(e: MouseEvent) {
  const rect = (e.currentTarget as HTMLElement).getBoundingClientRect()
  const id = ++rid
  ripples.value.push({ x: e.clientX - rect.left, y: e.clientY - rect.top, id })
  setTimeout(() => { ripples.value = ripples.value.filter(r => r.id !== id) }, 1200)
}

/** 预留跳转 */
function jumpToHeritageList() {
  // TODO: router.push('/inheritors')
}
</script>

<template>
  <section
    class="banner" :style="{ height: H + 'px' }"
    :class="{ hovering }"
    @mousemove="onMove"
    @mouseenter="onEnter"
    @mouseleave="onLeave"
    @click="onClick"
  >
    <!-- ========================= z1: 背景层 ========================= -->
    <div class="layer layer-bg">
      <div class="paper-base"></div>
      <!-- 远山3D视差 -->
      <div class="mtn-wrap" :style="{
        transform: `translate3d(${-mx * PARALLAX_MAX}px, ${-my * PARALLAX_MAX * 0.6}px, 0)`,
      }">
        <svg class="mtn-svg" viewBox="0 0 1200 520" preserveAspectRatio="xMidYMid slice">
          <defs>
            <linearGradient id="mtnA" x1="0%" y1="0%" x2="0%" y2="100%">
              <stop offset="0%" stop-color="#e0d0c8" stop-opacity="0.3"/>
              <stop offset="100%" stop-color="#d8c4b8" stop-opacity="0.08"/>
            </linearGradient>
            <linearGradient id="mtnB" x1="0%" y1="0%" x2="0%" y2="100%">
              <stop offset="0%" stop-color="#dcc8c0" stop-opacity="0.38"/>
              <stop offset="100%" stop-color="#c8b0a0" stop-opacity="0.12"/>
            </linearGradient>
            <filter id="blurLg"><feGaussianBlur stdDeviation="7"/></filter>
            <filter id="blurMd"><feGaussianBlur stdDeviation="3.5"/></filter>
          </defs>
          <g filter="url(#blurMd)" opacity="0.5">
            <path class="mtn mtn1" d="
              M-60,400 C30,320 80,335 160,310 C240,288 300,305 380,285
              C460,265 520,280 600,272 C680,264 740,278 820,270
              C900,262 960,275 1060,272 C1140,270 1200,275 1260,278
              L1260,520 L-60,520 Z" fill="url(#mtnA)"/>
            <path class="mtn mtn2" d="
              M-60,440 C60,370 130,360 220,380 C310,400 370,375 460,392
              C550,408 620,380 710,395 C800,410 870,388 960,400
              C1050,412 1120,395 1260,390 L1260,520 L-60,520 Z" fill="url(#mtnB)"/>
          </g>
          <rect x="-60" y="400" width="1320" height="120" fill="#fcf4e8" opacity="0.5" filter="url(#blurLg)"/>
        </svg>
      </div>
    </div>

    <!-- ========================= z2: 剪纸纹样层 ========================= -->
    <div class="layer layer-motifs" :style="{
      transform: `translate3d(${mx * PARALLAX_MAX * 0.5}px, ${my * PARALLAX_MAX * 0.5}px, 0)`,
    }">
      <svg class="motif-svg" viewBox="0 0 1200 520" preserveAspectRatio="xMidYMid slice">
        <g :fill="GOLD" opacity="0.2">
          <!-- 窗花1 -->
          <g class="mf m-a" transform="translate(160,130)">
            <circle cx="0" cy="0" r="9"/><circle cx="15" cy="0" r="5"/>
            <circle cx="-15" cy="0" r="5"/><circle cx="0" cy="15" r="5"/><circle cx="0" cy="-15" r="5"/>
          </g>
          <!-- 竹编菱形 -->
          <g class="mf m-b" transform="translate(360,110)">
            <polygon points="0,-16 12,0 0,16 -12,0"/>
            <polygon points="0,-7 5,0 0,7 -5,0" opacity="0.5"/>
          </g>
          <!-- 牡丹花形 -->
          <g class="mf m-c" transform="translate(580,140)">
            <circle cx="9" cy="-9" r="7"/><circle cx="-9" cy="-9" r="7"/>
            <circle cx="9" cy="9" r="7"/><circle cx="-9" cy="9" r="7"/><circle cx="0" cy="0" r="4" opacity="0.6"/>
          </g>
          <!-- 回纹 -->
          <g class="mf m-d" transform="translate(790,150)">
            <polyline points="-22,0 -10,-10 2,0 14,-10 26,0" fill="none" :stroke="GOLD" stroke-width="2.2"/>
            <polyline points="-22,11 -10,1 2,11 14,1 26,11" fill="none" :stroke="GOLD" stroke-width="2.2" opacity="0.5"/>
          </g>
          <!-- 扎染同心菱形 -->
          <g class="mf m-e" transform="translate(1020,160)">
            <polygon points="0,-20 14,0 0,20 -14,0" opacity="0.45"/>
            <polygon points="0,-10 8,0 0,10 -8,0" opacity="0.65"/><circle cx="0" cy="0" r="3.5"/>
          </g>
          <!-- 祥云 -->
          <g class="mf m-f" transform="translate(270,210)">
            <circle cx="0" cy="0" r="12"/><circle cx="16" cy="-4" r="8"/><circle cx="-14" cy="-3" r="8"/>
          </g>
          <!-- 五瓣花 -->
          <g class="mf m-g" transform="translate(920,230)">
            <polygon points="0,-14 13,-4 8,9 -8,9 -13,-4" opacity="0.55"/><circle cx="0" cy="0" r="3.5"/>
          </g>
          <!-- 小菱形 -->
          <g class="mf m-h" transform="translate(140,290)">
            <polygon points="0,-12 9,0 0,12 -9,0"/><polygon points="0,-5 4,0 0,5 -4,0" opacity="0.4"/>
          </g>
          <!-- 三角花 -->
          <g class="mf m-i" transform="translate(550,260)">
            <circle cx="0" cy="-10" r="4.5"/><circle cx="10" cy="4" r="4.5"/>
            <circle cx="-10" cy="4" r="4.5"/><circle cx="0" cy="0" r="2.5" opacity="0.5"/>
          </g>
          <!-- 窗花2 -->
          <g class="mf m-j" transform="translate(1080,280)">
            <circle cx="0" cy="0" r="7"/><circle cx="11" cy="0" r="4"/><circle cx="-11" cy="0" r="4"/>
            <circle cx="0" cy="11" r="4"/><circle cx="0" cy="-11" r="4"/>
          </g>
          <!-- 回纹2 -->
          <g class="mf m-k" transform="translate(680,290)">
            <polyline points="-16,0 -4,-8 8,0 20,-8" fill="none" :stroke="GOLD" stroke-width="2"/>
          </g>
          <!-- 小云朵 -->
          <g class="mf m-l" transform="translate(430,300)">
            <circle cx="0" cy="0" r="8"/><circle cx="11" cy="-3" r="5"/><circle cx="-9" cy="-2" r="5"/>
          </g>
        </g>
        <!-- 金色粒子 -->
        <g opacity="0.25">
          <circle v-for="n in 16" :key="'p'+n"
            :cx="60 + n * 72" cy="500" r="1.8"
            :fill="n % 3 === 0 ? RED : GOLD"
            class="particle"
            :style="{ animationDelay: n * 0.4 + 's', animationDuration: (3 + n * 0.28) + 's' }"
          />
        </g>
      </svg>
    </div>

    <!-- ========================= z3: 左上红日 ========================= -->
    <div class="layer layer-sun">
      <svg class="sun-svg" viewBox="0 0 1200 520" preserveAspectRatio="xMidYMid slice">
        <defs>
          <radialGradient id="sG" cx="50%" cy="50%" r="50%">
            <stop offset="0%" stop-color="#fff8e0"/><stop offset="22%" stop-color="#ffe0a0"/>
            <stop offset="50%" stop-color="#e86850"/><stop offset="82%" stop-color="#c41e3a"/>
            <stop offset="100%" stop-color="#8b1a2b"/>
          </radialGradient>
          <radialGradient id="sGl" cx="50%" cy="50%" r="50%">
            <stop offset="0%" stop-color="#e86850" stop-opacity="0.4"/>
            <stop offset="50%" stop-color="#c41e3a" stop-opacity="0.08"/>
            <stop offset="100%" stop-color="#c41e3a" stop-opacity="0"/>
          </radialGradient>
        </defs>
        <circle cx="130" cy="110" r="95" fill="url(#sGl)" class="sun-glow"/>
        <circle cx="130" cy="110" r="44" fill="url(#sG)" class="sun-disc"/>
      </svg>
    </div>

    <!-- ========================= z4: 水波特效层 ========================= -->
    <div class="layer layer-ripple" aria-hidden="true">
      <div
        v-for="r in ripples" :key="r.id"
        class="ripple-ring"
        :style="{ left: r.x + 'px', top: r.y + 'px' }"
      ></div>
    </div>

    <!-- ========================= z5: 文字顶层 ========================= -->
    <div class="layer layer-title">
      <h1 class="hero-title">码上非遗</h1>
    </div>
  </section>
</template>

<style scoped>
/* ========================= 容器 ========================= */
.banner {
  position: relative; width: 100%; overflow: hidden;
  background: #fcf4e8;
  transition: box-shadow 0.5s, transform 0.5s;
  cursor: pointer;
}
.banner.hovering {
  box-shadow: 0 6px 36px rgba(139,26,43,0.15);
  transform: translateY(-2px);
}

/* ========================= 分层基类 ========================= */
.layer { position: absolute; inset: 0; pointer-events: none; }

/* ========================= z1: 背景层 ========================= */
.layer-bg    { z-index: 1; }
.paper-base {
  position: absolute; inset: 0;
  background:
    repeating-linear-gradient(0deg, transparent, transparent 2px, rgba(196,30,58,0.004) 2px, rgba(196,30,58,0.004) 4px),
    radial-gradient(ellipse at 50% 65%, rgba(196,30,58,0.018) 0%, transparent 50%);
}
.mtn-wrap { transition: transform 0.2s ease-out; }
.mtn-svg  { width: 100%; height: 100%; }
.mtn1 { animation: mtnFloat 12s ease-in-out infinite; }
.mtn2 { animation: mtnFloat 14s ease-in-out 2s infinite; }
@keyframes mtnFloat {
  0%,100% { transform: translateY(0); }
  50%     { transform: translateY(-8px); }
}
.banner.hovering .mtn1 { animation-duration: 6s; }
.banner.hovering .mtn2 { animation-duration: 7s; }

/* ========================= z2: 剪纸纹样 ========================= */
.layer-motifs { z-index: 2; transition: transform 0.15s ease-out; }
.motif-svg    { width: 100%; height: 100%; }

/* 12 个纹样 独立浮动节奏 */
.mf { }
.m-a  { animation: fUp1 5s ease-in-out infinite; }
.m-b  { animation: fUp2 5.5s ease-in-out 0.4s infinite; }
.m-c  { animation: fUp3 6s ease-in-out 1.2s infinite; }
.m-d  { animation: fUp1 5.2s ease-in-out 0.8s infinite; }
.m-e  { animation: fUp2 5.8s ease-in-out 2s infinite; }
.m-f  { animation: fUp3 6.2s ease-in-out 0.2s infinite; }
.m-g  { animation: fUp1 5.4s ease-in-out 1.6s infinite; }
.m-h  { animation: fUp2 5.6s ease-in-out 3s infinite; }
.m-i  { animation: fUp3 6s ease-in-out 2.8s infinite; }
.m-j  { animation: fUp1 5.3s ease-in-out 1s infinite; }
.m-k  { animation: fUp2 5.7s ease-in-out 2.4s infinite; }
.m-l  { animation: fUp3 5.9s ease-in-out 0.6s infinite; }

@keyframes fUp1 { 0%,100%{transform:translateY(0)} 50%{transform:translateY(-15px)} }
@keyframes fUp2 { 0%,100%{transform:translateY(0)} 50%{transform:translateY(-11px)} }
@keyframes fUp3 { 0%,100%{transform:translateY(0)} 50%{transform:translateY(-17px)} }

.banner.hovering .m-a,.banner.hovering .m-b,.banner.hovering .m-c,
.banner.hovering .m-d,.banner.hovering .m-e,.banner.hovering .m-f,
.banner.hovering .m-g,.banner.hovering .m-h,.banner.hovering .m-i,
.banner.hovering .m-j,.banner.hovering .m-k,.banner.hovering .m-l {
  animation-duration: 2.8s;
}

/* 粒子 */
.particle { animation: sparkRise 4s ease-out infinite; }
@keyframes sparkRise {
  0%{transform:translateY(0);opacity:0} 15%{opacity:.7} 100%{transform:translateY(-300px);opacity:0}
}
.banner.hovering .particle { animation-duration: 2.2s; }

/* ========================= z3: 红日 ========================= */
.layer-sun  { z-index: 3; }
.sun-svg    { width: 100%; height: 100%; }
.sun-disc   { animation: sunBreath 4s ease-in-out infinite; transform-origin: 130px 110px; }
.sun-glow   { animation: glowPulse 4s ease-in-out infinite; transform-origin: 130px 110px; }
@keyframes sunBreath { 0%,100%{transform:scale(1)} 50%{transform:scale(1.05)} }
@keyframes glowPulse { 0%,100%{transform:scale(1);opacity:.7} 50%{transform:scale(1.18);opacity:1} }
.banner.hovering .sun-glow { animation: glowExpand 3s ease-in-out infinite; }
@keyframes glowExpand { 0%,100%{transform:scale(1);opacity:.7} 50%{transform:scale(1.35);opacity:1} }

/* ========================= z4: 水波特效（pointer-events:none，低于标题） ========================= */
.layer-ripple { z-index: 4; }
.ripple-ring {
  position: absolute; width: 0; height: 0; border-radius: 50%;
  border: 2px solid rgba(196,30,58,0.45);
  transform: translate(-50%, -50%);
  animation: rippleOut 1.2s ease-out forwards;
}
@keyframes rippleOut {
  0%{width:0;height:0;opacity:1} 100%{width:500px;height:500px;opacity:0}
}

/* ========================= z5: 文字顶层（最高层级） ========================= */
.layer-title {
  z-index: 5;
  display: flex; align-items: center; justify-content: center;
  /* 瘦金体字体栈 */
}
.hero-title {
  position: absolute; bottom: 10%;
  font-size: clamp(40px, 9vw, 72px);
  font-weight: 700;
  letter-spacing: 12px;
  font-family: 'Noto Serif SC', 'STKaiti', 'KaiTi', '楷体', 'STSong', 'SimSun', serif;
  background: linear-gradient(180deg, #ffe680 0%, #ffd700 25%, #e6b85c 50%, #c49830 75%, #a07018 100%);
  -webkit-background-clip: text; background-clip: text;
  -webkit-text-fill-color: transparent;
  filter: drop-shadow(0 2px 10px rgba(200,150,30,0.4));
  user-select: none;
}
</style>
