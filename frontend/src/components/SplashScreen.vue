<script setup lang="ts">
import { ref, onMounted } from 'vue'

const emit = defineEmits<{ done: [] }>()

const visible = ref(true)
const exiting = ref(false)
const phase = ref<'dawn' | 'rise' | 'shine' | 'text' | 'exit'>('dawn')

let timer: ReturnType<typeof setTimeout> | null = null

function skip() {
  if (exiting.value) return
  clearTimers()
  beginExit()
}

function clearTimers() {
  if (timer) { clearTimeout(timer); timer = null }
}

function beginExit() {
  exiting.value = true
  phase.value = 'exit'
  // 太阳继续上升淡出 + 文字淡出 + 场景整体升天
  timer = setTimeout(() => {
    visible.value = false
    emit('done')
  }, 800)
}

onMounted(() => {
  timer = setTimeout(() => { phase.value = 'rise'  }, 300)
  timer = setTimeout(() => { phase.value = 'shine' }, 1600)
  timer = setTimeout(() => { phase.value = 'text'  }, 2200)
  timer = setTimeout(() => { beginExit() }, 4000)
})
</script>

<template>
  <Transition name="splash">
    <div v-if="visible" class="splash-screen" :class="{ exiting }" @click="skip">
      <!-- ===== 宣纸底纹 ===== -->
      <div class="paper-bg"></div>

      <!-- ===== 天空光晕 ===== -->
      <div class="sky-glow" :class="{ bright: phase === 'shine' || phase === 'text' || phase === 'exit' }"></div>

      <!-- ===== SVG 主场景 ===== -->
      <svg
        class="scene"
        viewBox="0 0 1200 800"
        preserveAspectRatio="xMidYMid slice"
        xmlns="http://www.w3.org/2000/svg"
      >
        <defs>
          <radialGradient id="sunGrad" cx="50%" cy="50%" r="50%">
            <stop offset="0%" stop-color="#fff8c0" />
            <stop offset="15%" stop-color="#ffd700" />
            <stop offset="50%" stop-color="#f0a030" />
            <stop offset="85%" stop-color="#d45030" />
            <stop offset="100%" stop-color="#a02020" />
          </radialGradient>

          <radialGradient id="sunGlow" cx="50%" cy="50%" r="50%">
            <stop offset="0%" stop-color="#ffd700" stop-opacity="0.4" />
            <stop offset="30%" stop-color="#f0a030" stop-opacity="0.15" />
            <stop offset="60%" stop-color="#e86430" stop-opacity="0.04" />
            <stop offset="100%" stop-color="#e86430" stop-opacity="0" />
          </radialGradient>

          <linearGradient id="mtnFar" x1="0%" y1="0%" x2="0%" y2="100%">
            <stop offset="0%" stop-color="#444" stop-opacity="0.3" />
            <stop offset="100%" stop-color="#333" stop-opacity="0.5" />
          </linearGradient>
          <linearGradient id="mtnMid" x1="0%" y1="0%" x2="0%" y2="100%">
            <stop offset="0%" stop-color="#2a2a2a" stop-opacity="0.5" />
            <stop offset="100%" stop-color="#1a1a1a" stop-opacity="0.7" />
          </linearGradient>
          <linearGradient id="mtnNear" x1="0%" y1="0%" x2="0%" y2="100%">
            <stop offset="0%" stop-color="#1a1a1a" stop-opacity="0.7" />
            <stop offset="100%" stop-color="#0a0a0a" stop-opacity="0.9" />
          </linearGradient>

          <filter id="inkBlurSm"><feGaussianBlur stdDeviation="1.5" /></filter>
          <filter id="inkBlurMd"><feGaussianBlur stdDeviation="3" /></filter>
          <filter id="inkBlurLg"><feGaussianBlur stdDeviation="7" /></filter>
        </defs>

        <!-- ========== 天空填充 ========== -->
        <rect x="0" y="0" width="1200" height="800" fill="transparent" />

        <!-- ========== 太阳组（从山后升至中央上方） ========== -->
        <g class="sun-group" :class="'sun-' + phase">
          <circle cx="600" cy="520" r="220" fill="url(#sunGlow)" class="sun-halo-lg" />
          <circle cx="600" cy="520" r="130" fill="url(#sunGlow)" class="sun-halo-md" />
          <circle cx="600" cy="520" r="60" fill="url(#sunGrad)" class="sun-disc" />
          <circle cx="585" cy="505" r="18" fill="rgba(255,255,255,0.18)" class="sun-highlight" />
        </g>

        <!-- ========== 远山（模糊，淡墨） ========== -->
        <g filter="url(#inkBlurLg)" opacity="0.4">
          <polygon points="
            -50,560 30,470 80,490 140,430 200,460 250,420 310,450 370,410 430,455
            490,425 550,460 610,430 670,465 730,425 790,455 850,420 910,460
            970,435 1030,470 1090,440 1150,465 1200,450 1250,470
            1250,800 -50,800
          " fill="url(#mtnFar)" />
        </g>

        <!-- ========== 中山（轻度模糊） ========== -->
        <g filter="url(#inkBlurSm)" opacity="0.55">
          <polygon points="
            -50,600 50,520 100,545 160,490 220,530 280,480 340,520 400,500
            460,540 520,505 580,545 640,510 700,540 760,500 820,535 880,495
            940,530 1000,500 1060,540 1120,510 1180,535 1250,520
            1250,800 -50,800
          " fill="url(#mtnMid)" />
        </g>

        <!-- ========== 近山（清晰棱角） ========== -->
        <g>
          <polygon points="
            -50,640 40,570 95,590 150,550 210,580 270,530 330,570 390,545
            450,585 510,555 570,590 630,560 690,595 750,555 810,585 870,550
            930,580 990,555 1050,590 1110,565 1170,585 1250,570
            1250,800 -50,800
          " fill="url(#mtnNear)" />
        </g>

        <!-- ========== 最前山（浓墨，最锐利） ========== -->
        <g opacity="0.92">
          <polygon points="
            -50,690 60,640 120,660 180,620 250,655 310,625 380,660 440,635
            510,670 570,640 640,675 700,645 770,680 830,650 900,670 960,645
            1030,675 1090,650 1150,670 1200,655 1250,665
            1250,800 -50,800
          " fill="#0a0a0a" />
        </g>

        <!-- ========== 前景树影剪影（极少数，增加层次） ========== -->
        <g opacity="0.75" fill="#050505">
          <!-- 左侧松树轮廓 -->
          <polygon points="120,720 128,680 132,690 140,660 144,672 152,640 156,655 164,710 160,720" />
          <polygon points="100,725 106,695 110,705 116,675 120,688 126,665 130,680 134,715 130,725" />
          <!-- 右侧枯枝 -->
          <polygon points="1050,715 1054,685 1058,698 1064,670 1068,684 1072,705 1068,718" />
        </g>

        <!-- ========== 祥云 ========== -->
        <g class="clouds-scene" opacity="0.07">
          <g transform="translate(100, 130) scale(0.6)">
            <ellipse cx="80" cy="40" rx="70" ry="24" fill="#c41e3a"/>
            <ellipse cx="110" cy="28" rx="50" ry="20" fill="#c41e3a"/>
            <ellipse cx="50" cy="32" rx="40" ry="18" fill="#c41e3a"/>
          </g>
          <g transform="translate(920, 170) scale(0.45)" opacity="0.6">
            <ellipse cx="80" cy="40" rx="70" ry="24" fill="#c41e3a"/>
            <ellipse cx="110" cy="28" rx="50" ry="20" fill="#c41e3a"/>
            <ellipse cx="50" cy="32" rx="40" ry="18" fill="#c41e3a"/>
          </g>
        </g>

        <!-- ========== 飞鸟 ========== -->
        <g class="birds" opacity="0.12">
          <path d="M300,280 L304,275 L308,280 L304,277 Z" fill="#222"/>
          <path d="M320,290 L323,286 L326,290 L323,287 Z" fill="#222"/>
          <path d="M285,270 L289,265 L293,270 L289,267 Z" fill="#222"/>
          <path d="M850,220 L854,215 L858,220 L854,217 Z" fill="#222"/>
          <path d="M865,235 L868,232 L871,235 L868,233 Z" fill="#222"/>
        </g>
      </svg>

      <!-- ===== 回纹装饰框 ===== -->
      <div class="ornament-frame">
        <div class="ornament-corner corner-tl"></div>
        <div class="ornament-corner corner-tr"></div>
        <div class="ornament-corner corner-bl"></div>
        <div class="ornament-corner corner-br"></div>
      </div>

      <!-- ===== 文字层 ===== -->
      <div class="text-layer" :class="{ visible: phase === 'text' || phase === 'exit' }">
        <div class="title-ornament">
          <span class="orn-line"></span>
          <span class="orn-diamond">◆</span>
          <span class="orn-line"></span>
        </div>

        <h1 class="main-title">码上非遗</h1>

        <div class="title-ornament bottom">
          <span class="orn-line"></span>
          <span class="orn-diamond small">◇</span>
          <span class="orn-line"></span>
        </div>

        <p class="sub-title">感受非遗魅力</p>
      </div>

      <!-- ===== 跳过提示 ===== -->
      <span class="skip-hint" :class="{ fading: phase === 'exit' }">点击任意位置跳过</span>
    </div>
  </Transition>
</template>

<style scoped>
/* ==================== 容器 ==================== */
.splash-screen {
  position: fixed; inset: 0; z-index: 9999;
  display: flex; align-items: center; justify-content: center;
  overflow: hidden; cursor: pointer;
  background: #faf3e8;
  font-family:
    'Noto Serif SC', 'Source Han Serif SC', 'STSong', 'SimSun',
    'KaiTi', '楷体', 'FangSong', '仿宋', serif;
  transition: opacity 0.6s ease, filter 0.6s ease;
}
.splash-screen.exiting {
  opacity: 0;
  filter: brightness(1.4) blur(2px);
}

/* ==================== 宣纸底纹 ==================== */
.paper-bg {
  position: absolute; inset: 0; z-index: 0;
  background:
    repeating-linear-gradient(
      0deg, transparent, transparent 2px,
      rgba(180, 130, 80, 0.01) 2px, rgba(180, 130, 80, 0.01) 4px
    ),
    repeating-linear-gradient(
      90deg, transparent, transparent 3px,
      rgba(180, 130, 80, 0.007) 3px, rgba(180, 130, 80, 0.007) 6px
    ),
    linear-gradient(175deg, #fef9f2 0%, #fcf4e8 40%, #faf0e0 100%);
}

/* ==================== 天空光晕 ==================== */
.sky-glow {
  position: absolute; inset: 0; z-index: 1; pointer-events: none;
  background: radial-gradient(
    ellipse 80% 60% at 50% 28%,
    rgba(255, 220, 80, 0.22) 0%,
    rgba(255, 180, 40, 0.08) 35%,
    transparent 60%
  );
  opacity: 0;
  transition: opacity 1.6s ease;
}
.sky-glow.bright { opacity: 1; }

/* ==================== SVG 场景 ==================== */
.scene {
  position: absolute; inset: 0;
  width: 100%; height: 100%;
  z-index: 2; pointer-events: none;
}

/* ==================== 太阳动画 ==================== */
/*
  太阳从 cy=520（山后）→ 最终位置 cy≈240（屏幕中央上方）
  通过 translateY 实现：初始 +280px → 最终 0
*/
.sun-group {
  transform-origin: 600px 520px;
}

.sun-dawn .sun-disc,
.sun-dawn .sun-halo-md,
.sun-dawn .sun-halo-lg,
.sun-dawn .sun-highlight {
  transform: translateY(240px) scale(0.6);
  opacity: 0;
}

/* 升起：从山后 → 屏幕中央正上方 */
.sun-rise .sun-disc {
  animation: sunRiseDisc 1.3s cubic-bezier(0.34, 1.56, 0.64, 1) forwards;
}
.sun-rise .sun-halo-lg {
  animation: sunRiseHalo 1.3s cubic-bezier(0.34, 1.56, 0.64, 1) forwards;
}
.sun-rise .sun-halo-md {
  animation: sunRiseHalo 1.3s cubic-bezier(0.34, 1.56, 0.64, 1) 0.12s forwards;
}
.sun-rise .sun-highlight {
  animation: sunRiseDisc 1.3s cubic-bezier(0.34, 1.56, 0.64, 1) 0.08s forwards;
}

@keyframes sunRiseDisc {
  0%   { transform: translateY(240px) scale(0.6); opacity: 0; }
  30%  { opacity: 0.8; }
  100% { transform: translateY(0) scale(1); opacity: 1; }
}
@keyframes sunRiseHalo {
  0%   { transform: translateY(240px) scale(0.3); opacity: 0; }
  100% { transform: translateY(0) scale(1); opacity: 1; }
}

/* 就位后微动 */
.sun-shine .sun-disc,
.sun-shine .sun-halo-md,
.sun-shine .sun-halo-lg,
.sun-shine .sun-highlight {
  transform: translateY(0); opacity: 1;
}
.sun-shine .sun-halo-lg { animation: sunPulse 3.5s ease-in-out infinite; }

.sun-text .sun-disc,
.sun-text .sun-halo-md,
.sun-text .sun-halo-lg,
.sun-text .sun-highlight {
  transform: translateY(0); opacity: 1;
}
.sun-text .sun-halo-lg { animation: sunPulse 3.5s ease-in-out infinite; }

/* 退场：太阳继续微微上飘 */
.sun-exit .sun-disc,
.sun-exit .sun-halo-md,
.sun-exit .sun-halo-lg,
.sun-exit .sun-highlight {
  animation: sunFadeUp 0.7s ease-in forwards;
}
@keyframes sunFadeUp {
  0%   { transform: translateY(0); opacity: 1; }
  100% { transform: translateY(-40px); opacity: 0.5; }
}

@keyframes sunPulse {
  0%, 100% { transform: translateY(0) scale(1); opacity: 1; }
  50%      { transform: translateY(0) scale(1.05); opacity: 0.88; }
}

/* ===== 飞鸟 ===== */
.birds {
  animation: birdsFloat 10s ease-in-out infinite;
}
@keyframes birdsFloat {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(-12px, -8px); }
}

/* ===== 场景祥云 ===== */
.clouds-scene {
  animation: sceneCloudDrift 14s ease-in-out infinite;
}
@keyframes sceneCloudDrift {
  0%, 100% { transform: translate(0, 0); opacity: 0.06; }
  50%      { transform: translate(14px, -10px); opacity: 0.1; }
}

/* ==================== 回纹装饰框 ==================== */
.ornament-frame {
  position: absolute; inset: 0; z-index: 3; pointer-events: none;
}
.ornament-corner {
  position: absolute;
  width: 80px; height: 80px;
  opacity: 0.1;
}
.corner-tl {
  top: 40px; left: 40px;
  border-top: 2px solid #c41e3a;
  border-left: 2px solid #c41e3a;
}
.corner-tr {
  top: 40px; right: 40px;
  border-top: 2px solid #c41e3a;
  border-right: 2px solid #c41e3a;
}
.corner-bl {
  bottom: 40px; left: 40px;
  border-bottom: 2px solid #c41e3a;
  border-left: 2px solid #c41e3a;
}
.corner-br {
  bottom: 40px; right: 40px;
  border-bottom: 2px solid #c41e3a;
  border-right: 2px solid #c41e3a;
}

/* ==================== 文字层 ==================== */
.text-layer {
  position: relative; z-index: 4;
  text-align: center;
  opacity: 0;
  transform: translateY(20px);
  transition: opacity 0.8s ease, transform 0.8s ease;
  user-select: none;
}
.text-layer.visible {
  opacity: 1;
  transform: translateY(0);
}

/* 标题装饰横线 */
.title-ornament {
  display: flex; align-items: center; justify-content: center;
  gap: 16px; margin-bottom: 10px;
  opacity: 0;
  animation: ornamentFadeIn 0.7s ease-out 0.15s forwards;
}
.title-ornament.bottom {
  margin-top: 10px; margin-bottom: 28px;
  animation-delay: 0.35s;
}
@keyframes ornamentFadeIn {
  0%   { opacity: 0; transform: scaleX(0.15); }
  100% { opacity: 0.55; transform: scaleX(1); }
}

.orn-line {
  display: block;
  width: 56px; height: 1px;
  background: linear-gradient(90deg, transparent, #b8860b, transparent);
}
.orn-diamond { color: #b8860b; font-size: 10px; opacity: 0.7; }
.orn-diamond.small { font-size: 7px; opacity: 0.5; }

/* 主标题 */
.main-title {
  font-size: clamp(42px, 9vw, 72px);
  font-weight: 900;
  letter-spacing: 12px;
  margin: 0;
  background: linear-gradient(
    180deg,
    #ffd700 0%,
    #e8b800 25%,
    #c47820 50%,
    #a06020 75%,
    #8b4513 100%
  );
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  filter: drop-shadow(0 2px 16px rgba(200, 150, 30, 0.5));
  animation: titleRise 0.9s cubic-bezier(0.22, 0.61, 0.36, 1) forwards;
}
@keyframes titleRise {
  0%   {
    transform: translateY(36px);
    opacity: 0;
    filter: drop-shadow(0 2px 16px rgba(200, 150, 30, 0.5)) blur(4px);
  }
  50%  {
    filter: drop-shadow(0 2px 16px rgba(200, 150, 30, 0.5)) blur(0);
  }
  100% {
    transform: translateY(0);
    opacity: 1;
    filter: drop-shadow(0 2px 16px rgba(200, 150, 30, 0.5)) blur(0);
  }
}

/* 副标题 */
.sub-title {
  font-size: clamp(16px, 3.5vw, 22px);
  font-weight: 400;
  color: #a07040;
  letter-spacing: 10px;
  margin: 0;
  opacity: 0;
  animation: subtitleRise 0.8s ease-out 0.5s forwards;
}
@keyframes subtitleRise {
  0%   { transform: translateY(14px); opacity: 0; }
  100% { transform: translateY(0); opacity: 0.8; }
}

/* ==================== 跳过提示 ==================== */
.skip-hint {
  position: absolute; bottom: 34px; left: 50%; transform: translateX(-50%);
  font-size: 12px; color: #c8b8a0; letter-spacing: 3px;
  transition: opacity 0.5s; z-index: 5; user-select: none;
}
.skip-hint.fading { opacity: 0; }

/* ==================== Vue Transition ==================== */
.splash-leave-active {
  transition: opacity 0.5s ease;
}
.splash-leave-to {
  opacity: 0;
}

/* ==================== 响应式 ==================== */
@media (max-width: 768px) {
  .main-title { letter-spacing: 6px; }
  .sub-title { letter-spacing: 5px; }
  .ornament-corner { width: 50px; height: 50px; }
  .corner-tl { top: 20px; left: 20px; }
  .corner-tr { top: 20px; right: 20px; }
  .corner-bl { bottom: 20px; left: 20px; }
  .corner-br { bottom: 20px; right: 20px; }
  .orn-line { width: 30px; }
}
</style>
