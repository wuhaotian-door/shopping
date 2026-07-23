<script setup lang="ts">
import { ref, onMounted } from 'vue'

const emit = defineEmits<{ done: [] }>()
const visible = ref(true)
const exiting = ref(false)
const phase = ref<'hidden' | 'reveal' | 'text' | 'exit'>('hidden')

let timer: ReturnType<typeof setTimeout> | null = null

function skip() {
  if (exiting.value) return
  clearTimers()
  beginExit()
}
function clearTimers() { if (timer) { clearTimeout(timer); timer = null } }
function beginExit() {
  exiting.value = true
  phase.value = 'exit'
  timer = setTimeout(() => { visible.value = false; emit('done') }, 800)
}

onMounted(() => {
  timer = setTimeout(() => { phase.value = 'reveal' }, 600)
  timer = setTimeout(() => { phase.value = 'text'   }, 2100)
  timer = setTimeout(() => { beginExit() }, 4200)
})
</script>

<template>
  <Transition name="splash">
    <div v-if="visible" class="splash-screen" :class="[phase, { exiting }]" @click="skip">
      <div class="paper-bg"></div>
      <div class="sky-glow" :class="{ bright: phase === 'text' || phase === 'exit' }"></div>

      <!-- 棱彩折射光晕 -->
      <div class="prism-layer" :class="{ active: phase === 'reveal' || phase === 'text' || phase === 'exit' }">
        <div class="prism-spot ps-1"></div>
        <div class="prism-spot ps-2"></div>
        <div class="prism-spot ps-3"></div>
        <div class="prism-spot ps-4"></div>
        <div class="prism-spot ps-5"></div>
      </div>

      <svg class="scene" viewBox="0 0 1200 800" preserveAspectRatio="xMidYMid slice" xmlns="http://www.w3.org/2000/svg">
        <defs>
          <radialGradient id="sunGrad" cx="50%" cy="50%" r="50%">
            <stop offset="0%" stop-color="#fff8e0" />
            <stop offset="12%" stop-color="#ffe8b0" />
            <stop offset="35%" stop-color="#ffb040" />
            <stop offset="60%" stop-color="#f06030" />
            <stop offset="85%" stop-color="#d43030" />
            <stop offset="100%" stop-color="#a02020" />
          </radialGradient>
          <radialGradient id="sunGlow" cx="50%" cy="50%" r="50%">
            <stop offset="0%" stop-color="#ffb040" stop-opacity="0.5" />
            <stop offset="35%" stop-color="#f08040" stop-opacity="0.2" />
            <stop offset="65%" stop-color="#e06030" stop-opacity="0.05" />
            <stop offset="100%" stop-color="#e06030" stop-opacity="0" />
          </radialGradient>
          <radialGradient id="goldHalo" cx="50%" cy="50%" r="50%">
            <stop offset="60%" stop-color="transparent" />
            <stop offset="80%" stop-color="#ffd700" stop-opacity="0.55" />
            <stop offset="90%" stop-color="#ffb040" stop-opacity="0.28" />
            <stop offset="100%" stop-color="#ffb040" stop-opacity="0" />
          </radialGradient>

          <radialGradient id="cloudGradL" cx="50%" cy="50%" r="50%">
            <stop offset="0%" stop-color="#fff" stop-opacity="0.96" />
            <stop offset="45%" stop-color="#fafafa" stop-opacity="0.82" />
            <stop offset="80%" stop-color="#f0ece4" stop-opacity="0.45" />
            <stop offset="100%" stop-color="#e8e0d4" stop-opacity="0" />
          </radialGradient>
          <radialGradient id="cloudGradR" cx="50%" cy="50%" r="50%">
            <stop offset="0%" stop-color="#fff" stop-opacity="0.94" />
            <stop offset="42%" stop-color="#faf8f4" stop-opacity="0.78" />
            <stop offset="78%" stop-color="#f0ece4" stop-opacity="0.4" />
            <stop offset="100%" stop-color="#e8e0d4" stop-opacity="0" />
          </radialGradient>

          <filter id="blurSm"><feGaussianBlur stdDeviation="2" /></filter>
          <filter id="blurMd"><feGaussianBlur stdDeviation="4" /></filter>
          <filter id="blurLg"><feGaussianBlur stdDeviation="8" /></filter>
          <filter id="cloudBlur"><feGaussianBlur stdDeviation="8" /></filter>
        </defs>

        <rect x="0" y="0" width="1200" height="800" fill="transparent" />

        <!-- ========== 朝阳 ========== -->
        <g class="sun-fixed">
          <circle cx="600" cy="165" r="280" fill="url(#sunGlow)" class="sun-halo-lg" />
          <circle cx="600" cy="165" r="160" fill="url(#sunGlow)" class="sun-halo-md" />
          <circle cx="600" cy="165" r="72" fill="url(#sunGrad)" class="sun-disc" />
          <circle cx="600" cy="165" r="78" fill="url(#goldHalo)" class="sun-gold-ring" />
        </g>

        <!-- ========== 左云（全部在太阳后方） ========== -->
        <g class="cloud-left" filter="url(#cloudBlur)">
          <ellipse cx="500" cy="168" rx="180" ry="65" fill="url(#cloudGradL)" />
          <ellipse cx="420" cy="182" rx="130" ry="52" fill="url(#cloudGradL)" />
          <ellipse cx="560" cy="152" rx="115" ry="46" fill="url(#cloudGradL)" />
          <ellipse cx="470" cy="140" rx="100" ry="40" fill="url(#cloudGradL)" />
          <ellipse cx="520" cy="192" rx="90" ry="36" fill="url(#cloudGradL)" />
          <ellipse cx="380" cy="165" rx="80" ry="32" fill="url(#cloudGradL)" />
          <g opacity="0.1" fill="none" stroke="#3a3a3a" stroke-width="1.5">
            <path d="M360,170 Q380,155 400,165 Q420,150 440,162 Q460,148 480,158 Q500,145 520,155" />
            <path d="M390,185 Q410,175 430,185 Q450,172 470,180 Q490,168 510,178" />
          </g>
        </g>

        <!-- ========== 右云 ========== -->
        <g class="cloud-right" filter="url(#cloudBlur)">
          <ellipse cx="700" cy="172" rx="200" ry="70" fill="url(#cloudGradR)" />
          <ellipse cx="800" cy="185" rx="140" ry="58" fill="url(#cloudGradR)" />
          <ellipse cx="620" cy="155" rx="130" ry="52" fill="url(#cloudGradR)" />
          <ellipse cx="740" cy="142" rx="110" ry="45" fill="url(#cloudGradR)" />
          <ellipse cx="670" cy="195" rx="100" ry="40" fill="url(#cloudGradR)" />
          <ellipse cx="850" cy="168" rx="90" ry="36" fill="url(#cloudGradR)" />
          <ellipse cx="750" cy="205" rx="80" ry="32" fill="url(#cloudGradR)" />
          <g opacity="0.1" fill="none" stroke="#3a3a3a" stroke-width="1.5">
            <path d="M840,170 Q820,155 800,165 Q780,150 760,162 Q740,148 720,158 Q700,145 680,155" />
            <path d="M810,185 Q790,175 770,185 Q750,172 730,180 Q710,168 690,178" />
          </g>
        </g>

        <!-- ================================================ -->
        <!--  水墨远山 — 淡朱砂色系分层 + 墨线勾勒                 -->
        <!-- ================================================ -->
        <g filter="url(#blurMd)">
          <!-- 远山（最淡朱砂） -->
          <path d="
            M -60,580
            C 30,510 70,520 140,500
            C 200,482 240,495 310,475
            C 380,455 420,470 490,448
            C 560,426 600,440 660,425
            C 720,410 760,422 820,415
            C 890,406 930,418 1000,420
            C 1070,422 1110,430 1180,440
            C 1240,448 1280,442 1300,438
            L 1300,800 L -60,800 Z
          " fill="#ecccc4" opacity="0.4" />

          <!-- 中山（淡朱砂中层） -->
          <path d="
            M -60,610
            C 50,560 90,535 160,560
            C 220,580 260,548 330,570
            C 400,590 440,555 510,575
            C 580,595 620,560 690,578
            C 760,596 800,562 870,580
            C 940,598 980,565 1050,582
            C 1120,598 1160,570 1220,585
            C 1270,596 1300,580 1300,580
            L 1300,800 L -60,800 Z
          " fill="#e0b0a4" opacity="0.5" />

          <!-- 近山（淡朱砂偏深） -->
          <path d="
            M -60,640
            C 40,610 100,595 180,615
            C 250,632 300,598 380,620
            C 450,638 500,605 570,625
            C 640,645 690,610 760,630
            C 830,650 880,618 950,635
            C 1020,652 1070,625 1130,640
            C 1190,652 1240,635 1300,630
            L 1300,800 L -60,800 Z
          " fill="#d49080" opacity="0.6" />
        </g>

        <!-- ===== 墨线勾勒山脊（仅顶部轮廓，不描底边） ===== -->
        <g fill="none" stroke="#1a1a1a" stroke-width="1.6" stroke-linecap="round" opacity="0.25" filter="url(#blurSm)">
          <!-- 远山脊线 -->
          <path d="
            M -60,580
            C 30,510 70,520 140,500
            C 200,482 240,495 310,475
            C 380,455 420,470 490,448
            C 560,426 600,440 660,425
            C 720,410 760,422 820,415
            C 890,406 930,418 1000,420
            C 1070,422 1110,430 1180,440
            C 1240,448 1280,442 1300,438
          " />
          <!-- 中山脊线 -->
          <path d="
            M -60,610
            C 50,560 90,535 160,560
            C 220,580 260,548 330,570
            C 400,590 440,555 510,575
            C 580,595 620,560 690,578
            C 760,596 800,562 870,580
            C 940,598 980,565 1050,582
            C 1120,598 1160,570 1220,585
            C 1270,596 1300,580 1300,580
          " />
          <!-- 近山脊线（稍粗） -->
          <path d="
            M -60,640
            C 40,610 100,595 180,615
            C 250,632 300,598 380,620
            C 450,638 500,605 570,625
            C 640,645 690,610 760,630
            C 830,650 880,618 950,635
            C 1020,652 1070,625 1130,640
            C 1190,652 1240,635 1300,630
          " stroke-width="2" opacity="0.35" />
        </g>

        <!-- 山脚薄雾 -->
        <g>
          <rect x="-60" y="580" width="1360" height="100" fill="#fcf4e8" opacity="0.45" filter="url(#blurLg)" />
          <rect x="-60" y="620" width="1360" height="120" fill="#fcf4e8" opacity="0.55" filter="url(#blurLg)" />
          <rect x="-60" y="660" width="1360" height="140" fill="#fcf4e8" opacity="0.7"  filter="url(#blurLg)" />
        </g>

        <!-- 飞鸟 -->
        <g class="birds" opacity="0.1">
          <path d="M300,280 L304,275 L308,280 L304,277 Z" fill="#c41e3a"/>
          <path d="M870,240 L874,235 L878,240 L874,237 Z" fill="#c41e3a"/>
        </g>
      </svg>

      <div class="ornament-frame">
        <div class="ornament-corner corner-tl"></div>
        <div class="ornament-corner corner-tr"></div>
        <div class="ornament-corner corner-bl"></div>
        <div class="ornament-corner corner-br"></div>
      </div>

      <div class="text-layer" :class="{ visible: phase === 'text' || phase === 'exit' }">
        <div class="title-ornament"><span class="orn-line"></span><span class="orn-diamond">◆</span><span class="orn-line"></span></div>
        <h1 class="main-title">码上非遗</h1>
        <div class="title-ornament bottom"><span class="orn-line"></span><span class="orn-diamond small">◇</span><span class="orn-line"></span></div>
        <p class="sub-title">感受非遗魅力</p>
      </div>

      <span class="skip-hint" :class="{ fading: phase === 'exit' }">点击任意位置跳过</span>
    </div>
  </Transition>
</template>

<style scoped>
.splash-screen {
  position: fixed; inset: 0; z-index: 9999;
  display: flex; align-items: center; justify-content: center;
  overflow: hidden; cursor: pointer;
  background: #fef9f4;
  font-family: 'Noto Serif SC','STKaiti','KaiTi','楷体','STSong','SimSun',serif;
  transition: opacity 0.6s ease, filter 0.6s ease;
}
.splash-screen.exiting { opacity: 0; filter: brightness(1.5) blur(3px); }

.paper-bg {
  position: absolute; inset: 0; z-index: 0;
  background:
    repeating-linear-gradient(0deg,transparent,transparent 2px,rgba(180,120,100,0.012) 2px,rgba(180,120,100,0.012) 4px),
    repeating-linear-gradient(90deg,transparent,transparent 3px,rgba(180,120,100,0.008) 3px,rgba(180,120,100,0.008) 6px),
    linear-gradient(175deg,#fef9f4 0%,#fdf5ec 40%,#faf0e0 100%);
}

.sky-glow {
  position: absolute; inset: 0; z-index: 1; pointer-events: none;
  background: radial-gradient(ellipse 80% 55% at 50% 12%, rgba(255,176,64,0.18) 0%, rgba(240,128,64,0.06) 35%, transparent 55%);
  opacity: 0; transition: opacity 1.6s ease 0.4s;
}
.sky-glow.bright { opacity: 1; }

.scene { position: absolute; inset: 0; width: 100%; height: 100%; z-index: 2; pointer-events: none; }

/* 棱彩折射光晕 */
.prism-layer { position: absolute; inset: 0; z-index: 3; pointer-events: none; }
.prism-spot { position: absolute; width: 120px; height: 120px; border-radius: 50%; filter: blur(30px); opacity: 0; transition: opacity 0.8s ease; }
.prism-layer.active .prism-spot { opacity: 0.45; animation: prismShift 4s ease-in-out infinite; }
.ps-1 { top:8%; left:44%; background: radial-gradient(circle,rgba(255,120,80,0.5) 0%,transparent 70%); animation-delay: 0s; }
.ps-2 { top:6%; left:52%; background: radial-gradient(circle,rgba(255,200,60,0.45) 0%,transparent 70%); animation-delay: 0.6s; }
.ps-3 { top:11%; left:40%; background: radial-gradient(circle,rgba(255,150,100,0.4) 0%,transparent 70%); animation-delay: 1.2s; }
.ps-4 { top:5%; left:48%; background: radial-gradient(circle,rgba(255,180,40,0.35) 0%,transparent 70%); animation-delay: 1.8s; }
.ps-5 { top:10%; left:56%; background: radial-gradient(circle,rgba(240,100,60,0.35) 0%,transparent 70%); animation-delay: 2.4s; }
@keyframes prismShift {
  0%,100% { transform: translate(0,0) scale(1); }
  33% { transform: translate(15px,-10px) scale(1.2); }
  66% { transform: translate(-10px,8px) scale(0.85); }
}

/* 太阳 */
.sun-fixed { transform-origin: 600px 165px; }
.sun-gold-ring { filter: blur(2px); }
.sun-fixed .sun-disc      { opacity: 0; transition: opacity 0.5s ease 0.8s; }
.sun-fixed .sun-halo-lg   { opacity: 0; transition: opacity 0.5s ease 0.7s; }
.sun-fixed .sun-halo-md   { opacity: 0; transition: opacity 0.5s ease 0.9s; }
.sun-fixed .sun-gold-ring { opacity: 0; transition: opacity 0.5s ease 0.6s; }
.reveal .sun-fixed .sun-disc,
.text .sun-fixed .sun-disc,
.exit .sun-fixed .sun-disc { opacity: 1; }
.reveal .sun-fixed .sun-halo-lg,
.text .sun-fixed .sun-halo-lg,
.exit .sun-fixed .sun-halo-lg { opacity: 1; animation: sunWarmPulse 4s ease-in-out infinite; }
.reveal .sun-fixed .sun-halo-md,
.text .sun-fixed .sun-halo-md,
.exit .sun-fixed .sun-halo-md { opacity: 1; }
.reveal .sun-fixed .sun-gold-ring,
.text .sun-fixed .sun-gold-ring,
.exit .sun-fixed .sun-gold-ring { opacity: 1; animation: goldRingPulse 3s ease-in-out infinite; }
@keyframes sunWarmPulse {
  0%,100% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.04); opacity: 0.85; }
}
@keyframes goldRingPulse {
  0%,100% { transform: scale(1); opacity: 0.8; }
  50% { transform: scale(1.06); opacity: 1; }
}

/* 云朵动画 */
/* 左云 — 初始完全遮阳，拨开后完全移开不遮挡 */
.cloud-left  { transform-origin: 500px 168px; }
.cloud-right { transform-origin: 700px 172px; }

.hidden .cloud-left  { transform: translateX(160px); }
.hidden .cloud-right { transform: translateX(-160px); }

.reveal .cloud-left  { animation: cloudPartL 1.5s cubic-bezier(0.25,0.46,0.45,0.94) forwards; }
.reveal .cloud-right { animation: cloudPartR 1.5s cubic-bezier(0.25,0.46,0.45,0.94) forwards; }
@keyframes cloudPartL {
  0%   { transform: translateX(160px); }
  100% { transform: translateX(-240px); }
}
@keyframes cloudPartR {
  0%   { transform: translateX(-160px); }
  100% { transform: translateX(240px); }
}
.text .cloud-left,
.exit .cloud-left { transform: translateX(-240px); }
.text .cloud-right,
.exit .cloud-right { transform: translateX(240px); }

.birds { animation: birdsFloat 10s ease-in-out infinite; }
@keyframes birdsFloat {
  0%,100% { transform: translate(0,0); }
  50% { transform: translate(-12px,-8px); }
}

/* 回纹装饰框 */
.ornament-frame { position: absolute; inset: 0; z-index: 4; pointer-events: none; }
.ornament-corner { position: absolute; width: 80px; height: 80px; opacity: 0.08; }
.corner-tl { top:40px; left:40px; border-top:2px solid #c41e3a; border-left:2px solid #c41e3a; }
.corner-tr { top:40px; right:40px; border-top:2px solid #c41e3a; border-right:2px solid #c41e3a; }
.corner-bl { bottom:40px; left:40px; border-bottom:2px solid #c41e3a; border-left:2px solid #c41e3a; }
.corner-br { bottom:40px; right:40px; border-bottom:2px solid #c41e3a; border-right:2px solid #c41e3a; }

/* 文字层 */
.text-layer {
  position: relative; z-index: 5; text-align: center;
  opacity: 0; transform: translateY(20px);
  transition: opacity 0.8s ease, transform 0.8s ease; user-select: none;
}
.text-layer.visible { opacity: 1; transform: translateY(0); }
.title-ornament {
  display: flex; align-items: center; justify-content: center;
  gap: 16px; margin-bottom: 10px;
  opacity: 0;
  animation: ornamentFadeIn 0.7s ease-out 0.15s forwards;
}
.title-ornament.bottom { margin-top: 10px; margin-bottom: 28px; animation-delay: 0.35s; }
@keyframes ornamentFadeIn {
  0% { opacity: 0; transform: scaleX(0.15); }
  100% { opacity: 0.5; transform: scaleX(1); }
}
.orn-line { display: block; width: 56px; height: 1px; background: linear-gradient(90deg,transparent,#d4a030,transparent); }
.orn-diamond { color: #d4a030; font-size: 10px; opacity: 0.6; }
.orn-diamond.small { font-size: 7px; opacity: 0.45; }

.main-title {
  font-size: clamp(42px,9vw,72px); font-weight: 700; letter-spacing: 12px; margin: 0;
  background: linear-gradient(180deg,#ffe680 0%,#ffd700 20%,#e8b800 50%,#c47820 75%,#8b6914 100%);
  -webkit-background-clip: text; background-clip: text; -webkit-text-fill-color: transparent;
  filter: drop-shadow(0 2px 14px rgba(200,150,30,0.45));
  animation: titleRise 0.9s cubic-bezier(0.22,0.61,0.36,1) forwards;
}
@keyframes titleRise {
  0% { transform: translateY(36px); opacity: 0; filter: drop-shadow(0 2px 14px rgba(200,150,30,0.45)) blur(4px); }
  50% { filter: drop-shadow(0 2px 14px rgba(200,150,30,0.45)) blur(0); }
  100% { transform: translateY(0); opacity: 1; filter: drop-shadow(0 2px 14px rgba(200,150,30,0.45)) blur(0); }
}
.sub-title {
  font-size: clamp(16px,3.5vw,22px); font-weight: 500; color: #b8860b;
  letter-spacing: 10px; margin: 0;
  opacity: 0; animation: subtitleRise 0.8s ease-out 0.5s forwards;
}
@keyframes subtitleRise {
  0% { transform: translateY(14px); opacity: 0; }
  100% { transform: translateY(0); opacity: 0.8; }
}

.skip-hint {
  position: absolute; bottom: 34px; left: 50%; transform: translateX(-50%);
  font-size: 12px; color: #c8b0a8; letter-spacing: 3px;
  transition: opacity 0.5s; z-index: 6; user-select: none;
}
.skip-hint.fading { opacity: 0; }

.splash-leave-active { transition: opacity 0.5s ease; }
.splash-leave-to { opacity: 0; }

@media (max-width: 768px) {
  .main-title { letter-spacing: 6px; }
  .sub-title { letter-spacing: 5px; }
  .ornament-corner { width: 50px; height: 50px; }
  .corner-tl { top:20px; left:20px; } .corner-tr { top:20px; right:20px; }
  .corner-bl { bottom:20px; left:20px; } .corner-br { bottom:20px; right:20px; }
  .orn-line { width: 30px; }
}
</style>
