<script setup lang="ts">
/**
 * 码上非遗 — 首页 Banner
 * 统一物理粒子系统：装饰文字 + 导航文字全部参与物理碰撞
 * 导航文字悬浮时冻结+发光，点击跳转
 * 边界内收紧，反弹系数调高
 *
 * z-index: z1背景 < z2水波 < z3粒子文字 < z4主标题
 */

import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

// ==================== 常量 ====================
const H = 520
const GOLD    = '#e6b85c'
const CINNABAR = '#e8647a'
const VIEW_W = 1200; const VIEW_H = 520
const CX = VIEW_W / 2; const CY = VIEW_H / 2

// ==================== 统一粒子 ====================
interface Particle {
  id: number
  text: string
  x: number; y: number
  vx: number; vy: number
  r: number; size: number
  color: string      // 颜色
  nav: boolean       // 是否可点击导航
  route: string      // 导航路由
  frozen: boolean    // 鼠标悬浮时冻结
}

const NAV_ITEMS = [
  { text: '非遗资讯', route: '/news' },
  { text: '传承人',   route: '/inheritors' },
  { text: '文创周边', route: '/store' },
  { text: '数据统计', route: '/stats' },
  { text: '智能问答', route: '/rag' },
]

const DECO_WORDS = [
  '剪纸','刺绣','陶艺','竹编','扎染','木雕','活字','年画',
  '京剧','皮影','昆曲','茶道','丝绸','瓷器','非遗传承','码上非遗',
]

const particles = reactive<Particle[]>([])
let pid = 0
const REPEL = 0.5
const FRICTION = 0.998
const MAX_SPD = 0.4
const MIN_DIST = 50
const BOUND_TOP    = 55   // 上边界：避开顶部导航栏
const BOUND_BOTTOM = 60   // 下边界：避开下方资讯区域
const BOUND_SIDE   = 35   // 左右边界
const BOUNCE = 0.75

// 主标题排斥区
const TZ = { cx: CX, cy: CY, rx: 195, ry: 58 }

function rand(a: number, b: number) { return a + Math.random() * (b - a) }
function clamp(v: number, lo: number, hi: number) { return v < lo ? lo : v > hi ? hi : v }

// 初始化 — 全部均匀分布在标题周围
const allWords = [...NAV_ITEMS.map(n => ({ ...n, nav: true, color: CINNABAR })), ...DECO_WORDS.map(w => ({ text: w, nav: false, route: '', color: GOLD }))]

allWords.forEach((w, i) => {
  const angle = (i / allWords.length) * Math.PI * 2
  const r = w.nav ? 240 + rand(-20, 20) : 130 + (i % 4) * 50 + rand(-10, 15)
  particles.push({
    id: ++pid, text: w.text, nav: w.nav, route: w.route,
    color: w.color, frozen: false,
    x: CX + r * Math.cos(angle),
    y: clamp(CY + r * Math.sin(angle), BOUND_TOP + r, VIEW_H - BOUND_BOTTOM - r),
    vx: rand(-0.2, 0.2), vy: rand(-0.2, 0.2),
    r: 20 + w.text.length * 5,
    size: w.nav ? 17 : (13 + rand(0, w.text.length > 4 ? 7 : 13)),
  })
})

function physicsTick() {
  for (let i = 0; i < particles.length; i++) {
    const a = particles[i]
    if (a.frozen) continue  // 悬浮时冻结

    // 主标题椭圆排斥
    const dtx = a.x - TZ.cx; const dty = a.y - TZ.cy
    const e = (dtx*dtx)/(TZ.rx*TZ.rx) + (dty*dty)/(TZ.ry*TZ.ry)
    if (e < 1) {
      const nd = Math.sqrt(dtx*dtx + dty*dty) || 1
      a.vx += (dtx/nd) * 0.12; a.vy += (dty/nd) * 0.12
      a.x += (dtx/nd) * 3; a.y += (dty/nd) * 3
    }

    // 粒子间排斥
    for (let j = i + 1; j < particles.length; j++) {
      const b = particles[j]
      const dx = a.x - b.x; const dy = a.y - b.y
      const d = Math.sqrt(dx*dx + dy*dy)
      if (d < MIN_DIST && d > 0.1) {
        const f = REPEL * (1 - d / MIN_DIST)
        a.vx += (dx/d) * f; a.vy += (dy/d) * f
        b.vx -= (dx/d) * f; b.vy -= (dy/d) * f
      }
    }

    // 边界反弹（上/下/左/右独立控制）
    if (a.x <= BOUND_SIDE)          { a.x = BOUND_SIDE;          a.vx =  Math.abs(a.vx) * BOUNCE }
    if (a.x >= VIEW_W - BOUND_SIDE) { a.x = VIEW_W - BOUND_SIDE; a.vx = -Math.abs(a.vx) * BOUNCE }
    if (a.y <= BOUND_TOP)           { a.y = BOUND_TOP;           a.vy =  Math.abs(a.vy) * BOUNCE }
    if (a.y >= VIEW_H - BOUND_BOTTOM) { a.y = VIEW_H - BOUND_BOTTOM; a.vy = -Math.abs(a.vy) * BOUNCE }

    // 限速 + 摩擦
    const spd = Math.sqrt(a.vx**2 + a.vy**2)
    const limit = MAX_SPD * (hovering.value ? 1.8 : 1)
    if (spd > limit) { a.vx *= limit/spd; a.vy *= limit/spd }
    a.vx *= FRICTION; a.vy *= FRICTION
    a.x += a.vx; a.y += a.vy
  }
}

// ==================== 动画 ====================
let af = 0
function loop() { physicsTick(); af = requestAnimationFrame(loop) }

// ==================== 交互 ====================
const hovering  = ref(false)
const hoveredId = ref(0)
const ripples   = ref<{ x: number; y: number; id: number }[]>([])
let rid = 0

function onEnter() { hovering.value = true }
function onLeave() {
  hovering.value = false
  // 解冻所有
  particles.forEach(p => p.frozen = false)
  hoveredId.value = 0
}

function onClick(e: MouseEvent) {
  const rect = (e.currentTarget as HTMLElement).getBoundingClientRect()
  const cx = e.clientX - rect.left; const cy = e.clientY - rect.top
  for (let i = 0; i < 3; i++) {
    const id = ++rid
    ripples.value.push({ x: cx, y: cy, id })
    setTimeout(() => { ripples.value = ripples.value.filter(r => r.id !== id) }, 1600 + i * 300)
  }
  // 水波推开
  const vx = (cx / rect.width) * VIEW_W; const vy = (cy / rect.height) * VIEW_H
  particles.forEach(p => {
    const dx = p.x - vx; const dy = p.y - vy
    const d = Math.sqrt(dx*dx + dy*dy)
    if (d < 200 && d > 0.5) { const f = 3.5 * (1 - d/200); p.vx += (dx/d)*f; p.vy += (dy/d)*f }
  })
}

// 导航悬浮：冻结+发光，离开解冻
function onWordEnter(p: Particle) { p.frozen = true; hoveredId.value = p.id }
function onWordLeave(p: Particle) { p.frozen = false; hoveredId.value = 0 }
function onWordClick(p: Particle, e: MouseEvent) {
  e.stopPropagation()
  if (p.nav && p.route) router.push(p.route)
}

onMounted(() => { af = requestAnimationFrame(loop) })
onUnmounted(() => { cancelAnimationFrame(af) })
</script>

<template>
  <section
    class="banner" :style="{ height: H + 'px' }"
    :class="{ hovering }"
    @mouseenter="onEnter"
    @mouseleave="onLeave"
    @click="onClick"
  >
    <!-- z1: 背景 -->
    <div class="layer layer-bg">
      <div class="paper-base"></div>
      <svg class="bg-svg" viewBox="0 0 1200 520" preserveAspectRatio="xMidYMid slice">
        <defs>
          <linearGradient id="mtA" x1="0%" y1="0%" x2="0%" y2="100%"><stop offset="0%" stop-color="#e0d0c8" stop-opacity=".25"/><stop offset="100%" stop-color="#d8c4b8" stop-opacity=".05"/></linearGradient>
          <linearGradient id="mtB" x1="0%" y1="0%" x2="0%" y2="100%"><stop offset="0%" stop-color="#dcc8c0" stop-opacity=".32"/><stop offset="100%" stop-color="#c8b0a0" stop-opacity=".08"/></linearGradient>
          <filter id="blurLg"><feGaussianBlur stdDeviation="7"/></filter>
          <filter id="blurMd"><feGaussianBlur stdDeviation="3.5"/></filter>
        </defs>
        <g filter="url(#blurMd)" opacity="0.45">
          <path class="mtn mtn1" d="M-60,400 C30,320 80,335 160,310 C240,288 300,305 380,285 C460,265 520,280 600,272 C680,264 740,278 820,270 C900,262 960,275 1060,272 C1140,270 1200,275 1260,278 L1260,520 L-60,520 Z" fill="url(#mtA)"/>
          <path class="mtn mtn2" d="M-60,440 C60,370 130,360 220,380 C310,400 370,375 460,392 C550,408 620,380 710,395 C800,410 870,388 960,400 C1050,412 1120,395 1260,390 L1260,520 L-60,520 Z" fill="url(#mtB)"/>
        </g>
        <rect x="-60" y="400" width="1320" height="120" fill="#fcf4e8" opacity="0.5" filter="url(#blurLg)"/>
      </svg>
    </div>

    <!-- z2: 水波 -->
    <div class="layer layer-ripple" aria-hidden="true">
      <div v-for="(r,i) in ripples" :key="r.id" class="ripple-wrap" :style="{left:r.x+'px',top:r.y+'px'}">
        <div class="ripple-ring ring1" :style="{animationDelay:(i%3)*.15+'s'}"></div>
        <div class="ripple-ring ring2" :style="{animationDelay:(i%3)*.15+.2+'s'}"></div>
        <div class="ripple-ring ring3" :style="{animationDelay:(i%3)*.15+.4+'s'}"></div>
      </div>
    </div>

    <!-- z3: 统一粒子(装饰+导航) -->
    <div class="layer layer-particles">
      <svg class="particle-svg" viewBox="0 0 1200 520" preserveAspectRatio="xMidYMid slice">
        <g v-for="p in particles" :key="p.id"
          :transform="`translate(${p.x}, ${p.y})`"
          :opacity="p.nav ? 0.75 : 0.3"
          class="particle-g"
          :class="{
            'nav-frozen': p.nav && p.frozen,
            'nav-active': p.nav && hoveredId === p.id,
          }"
        >
          <!-- 导航文字悬浮发光底圈 -->
          <circle v-if="p.nav && p.frozen" cx="0" cy="0" :r="p.r+6" fill="none" :stroke="CINNABAR" stroke-width="1" opacity="0.5" class="frozen-ring" />
          <!-- 文字 -->
          <text
            :fill="p.nav && p.frozen ? '#ffc0c8' : p.color"
            :font-size="p.nav && p.frozen ? p.size + 2 : p.size"
            font-family="'Noto Serif SC','STKaiti','KaiTi','楷体',serif"
            :font-weight="p.nav ? 600 : 400"
            text-anchor="middle" dominant-baseline="central"
            :class="{ 'clickable-text': p.nav }"
            @mouseenter="p.nav && onWordEnter(p)"
            @mouseleave="p.nav && onWordLeave(p)"
            @click="p.nav && onWordClick(p, $event)"
          >{{ p.text }}</text>
        </g>
      </svg>
    </div>

    <!-- z4: 居中主标题 -->
    <div class="layer layer-title">
      <h1 class="hero-title">码上非遗</h1>
    </div>
  </section>
</template>

<style scoped>
/* 容器 */
.banner {
  position: relative; width: 100%; overflow: hidden;
  background: #fcf4e8; cursor: pointer;
  transition: box-shadow 0.5s, transform 0.5s;
}
.banner.hovering { box-shadow: 0 6px 36px rgba(139,26,43,0.15); transform: translateY(-2px); }
.layer { position: absolute; inset: 0; }

/* z1 */
.layer-bg { z-index: 1; pointer-events: none; }
.paper-base {
  position: absolute; inset: 0;
  background: repeating-linear-gradient(0deg,transparent,transparent 2px,rgba(196,30,58,0.004) 2px,rgba(196,30,58,0.004) 4px),
              radial-gradient(ellipse at 50% 65%,rgba(196,30,58,0.015) 0%,transparent 50%);
}
.bg-svg { width: 100%; height: 100%; }
.mtn1 { animation: mtnFloat 12s ease-in-out infinite; }
.mtn2 { animation: mtnFloat 14s ease-in-out 2s infinite; }
@keyframes mtnFloat { 0%,100%{transform:translateY(0)} 50%{transform:translateY(-8px)} }
.banner.hovering .mtn1 { animation-duration:6s; }
.banner.hovering .mtn2 { animation-duration:7s; }

/* z2 */
.layer-ripple { z-index: 2; pointer-events: none; }
.ripple-wrap { position: absolute; width: 0; height: 0; transform: translate(-50%,-50%); }
.ripple-ring {
  position: absolute; width: 0; height: 0; border-radius: 50%;
  border: 1.5px solid rgba(196,30,58,0.5);
  transform: translate(-50%,-50%);
  animation: rippleLayer 1.6s ease-out forwards;
}
.ring2 { border-color: rgba(196,30,58,0.35); }
.ring3 { border-color: rgba(196,30,58,0.2); }
@keyframes rippleLayer { 0%{width:0;height:0;opacity:1} 100%{width:500px;height:500px;opacity:0} }

/* z3 */
.layer-particles { z-index: 3; }
.particle-svg { width: 100%; height: 100%; }
.particle-g { transition: opacity 0.6s; }
.banner.hovering .particle-g { opacity: 0.4 !important; }

/* 可点击导航 */
.clickable-text { cursor: pointer; pointer-events: auto; transition: font-size 0.2s; }
.nav-active text { filter: drop-shadow(0 0 6px rgba(232,100,122,0.6)); }
.frozen-ring { animation: ringPulse 0.8s ease-in-out infinite; }
@keyframes ringPulse { 0%,100%{opacity:.4;r:28} 50%{opacity:.7;r:32} }

/* z4 */
.layer-title { z-index: 4; pointer-events: none; display: flex; align-items: center; justify-content: center; }
.hero-title {
  font-size: clamp(44px,10vw,78px); font-weight: 800; letter-spacing: 16px;
  font-family: 'Noto Serif SC','STKaiti','KaiTi','楷体','STSong','SimSun',serif;
  background: linear-gradient(180deg,#ffe680 0%,#ffd700 25%,#e6b85c 50%,#c49830 75%,#a07018 100%);
  -webkit-background-clip: text; background-clip: text;
  -webkit-text-fill-color: transparent;
  filter: drop-shadow(0 2px 10px rgba(200,150,30,0.4));
  user-select: none; transition: filter 0.5s;
}
.banner.hovering .hero-title { filter: drop-shadow(0 3px 18px rgba(230,184,92,0.55)); }
</style>
