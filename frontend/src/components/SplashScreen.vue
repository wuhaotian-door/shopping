<script setup lang="ts">
import { ref, onMounted } from 'vue'

const emit = defineEmits<{ done: [] }>()

const visible = ref(true)
const exiting = ref(false)
// 阶段：hidden(云遮日) → reveal(拨云见日) → text(文字浮现) → exit
const phase = ref<'hidden' | 'reveal' | 'text' | 'exit'>('hidden')

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
  timer = setTimeout(() => {
    visible.value = false
    emit('done')
  }, 800)
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
      <!-- ===== 宣纸底纹 ===== -->
      <div class="paper-bg"></div>

      <!-- ===== 天空晨光（云开后亮起） ===== -->
      <div class="sky-glow" :class="{ bright: phase === 'text' || phase === 'exit' }"></div>

      <!-- ===== SVG 主场景 ===== -->
      <svg
        class="scene"
        viewBox="0 0 1200 800"
        preserveAspectRatio="xMidYMid slice"
        xmlns="http://www.w3.org/2000/svg"
      >
        <defs>
          <!-- 朝阳渐变：橙金→绯红（日出温度感） -->
          <radialGradient id="sunGrad" cx="50%" cy="50%" r="50%">
            <stop offset="0%" stop-color="#fff8e0" />
            <stop offset="10%" stop-color="#ffe8b0" />
            <stop offset="30%" stop-color="#ffb040" />
            <stop offset="55%" stop-color="#f06030" />
            <stop offset="80%" stop-color="#d43030" />
            <stop offset="100%" stop-color="#a02020" />
          </radialGradient>

          <!-- 朝阳光晕 -->
          <radialGradient id="sunGlow" cx="50%" cy="50%" r="50%">
            <stop offset="0%" stop-color="#ffb040" stop-opacity="0.5" />
            <stop offset="30%" stop-color="#f08040" stop-opacity="0.2" />
            <stop offset="60%" stop-color="#e06030" stop-opacity="0.05" />
            <stop offset="100%" stop-color="#e06030" stop-opacity="0" />
          </radialGradient>

          <!-- 白云渐变（水墨笔触感） -->
          <radialGradient id="cloudGradL" cx="50%" cy="50%" r="50%">
            <stop offset="0%" stop-color="#fff" stop-opacity="0.95" />
            <stop offset="50%" stop-color="#fafafa" stop-opacity="0.8" />
            <stop offset="85%" stop-color="#f0ece4" stop-opacity="0.4" />
            <stop offset="100%" stop-color="#e8e0d4" stop-opacity="0" />
          </radialGradient>
          <radialGradient id="cloudGradR" cx="50%" cy="50%" r="50%">
            <stop offset="0%" stop-color="#fff" stop-opacity="0.92" />
            <stop offset="45%" stop-color="#faf8f4" stop-opacity="0.75" />
            <stop offset="80%" stop-color="#f0ece4" stop-opacity="0.35" />
            <stop offset="100%" stop-color="#e8e0d4" stop-opacity="0" />
          </radialGradient>

          <!-- 淡朱砂山峦渐变 -->
          <linearGradient id="mtnFar" x1="0%" y1="0%" x2="0%" y2="100%">
            <stop offset="0%" stop-color="#c47a7a" stop-opacity="0.3" />
            <stop offset="100%" stop-color="#a85a5a" stop-opacity="0.45" />
          </linearGradient>
          <linearGradient id="mtnMid" x1="0%" y1="0%" x2="0%" y2="100%">
            <stop offset="0%" stop-color="#b06060" stop-opacity="0.45" />
            <stop offset="100%" stop-color="#8b4040" stop-opacity="0.6" />
          </linearGradient>
          <linearGradient id="mtnNear" x1="0%" y1="0%" x2="0%" y2="100%">
            <stop offset="0%" stop-color="#904040" stop-opacity="0.6" />
            <stop offset="100%" stop-color="#6b2020" stop-opacity="0.8" />
          </linearGradient>
          <linearGradient id="mtnFront" x1="0%" y1="0%" x2="0%" y2="100%">
            <stop offset="0%" stop-color="#6b2020" stop-opacity="0.75" />
            <stop offset="100%" stop-color="#4a1010" stop-opacity="0.92" />
          </linearGradient>

          <!-- 金色光晕渐变 -->
          <radialGradient id="goldHalo" cx="50%" cy="50%" r="50%">
            <stop offset="65%" stop-color="transparent" />
            <stop offset="82%" stop-color="#ffd700" stop-opacity="0.5" />
            <stop offset="92%" stop-color="#ffb040" stop-opacity="0.25" />
            <stop offset="100%" stop-color="#ffb040" stop-opacity="0" />
          </radialGradient>

          <filter id="inkBlurSm"><feGaussianBlur stdDeviation="1.5" /></filter>
          <filter id="inkBlurMd"><feGaussianBlur stdDeviation="3" /></filter>
          <filter id="inkBlurLg"><feGaussianBlur stdDeviation="7" /></filter>

          <!-- 云朵模糊（水墨晕染感） -->
          <filter id="cloudBlur">
            <feGaussianBlur stdDeviation="8" />
          </filter>
        </defs>

        <!-- ========== 天空基底 ========== -->
        <rect x="0" y="0" width="1200" height="800" fill="transparent" />

        <!-- ========== 朝阳（固定在页面正上方） ========== -->
        <g class="sun-fixed">
          <!-- 大光晕 -->
          <circle cx="600" cy="165" r="280" fill="url(#sunGlow)" class="sun-halo-lg" />
          <!-- 中光晕 -->
          <circle cx="600" cy="165" r="160" fill="url(#sunGlow)" class="sun-halo-md" />
          <!-- 太阳本体（正圆，纯朝阳色，无高光） -->
          <circle cx="600" cy="165" r="72" fill="url(#sunGrad)" class="sun-disc" />
          <!-- 金色光圈 -->
          <circle cx="600" cy="165" r="78" fill="url(#goldHalo)" class="sun-gold-ring" />
        </g>

        <!-- ========== 左云（放大+传统纹饰） ========== -->
        <g class="cloud-left" filter="url(#cloudBlur)">
          <!-- 云朵主体 -->
          <ellipse cx="500" cy="172" rx="200" ry="70" fill="url(#cloudGradL)" />
          <ellipse cx="400" cy="185" rx="140" ry="58" fill="url(#cloudGradL)" />
          <ellipse cx="580" cy="155" rx="130" ry="52" fill="url(#cloudGradL)" />
          <ellipse cx="460" cy="142" rx="110" ry="45" fill="url(#cloudGradL)" />
          <ellipse cx="530" cy="195" rx="100" ry="40" fill="url(#cloudGradL)" />
          <ellipse cx="350" cy="168" rx="90" ry="36" fill="url(#cloudGradL)" />
          <ellipse cx="450" cy="205" rx="80" ry="32" fill="url(#cloudGradL)" />
          <!-- 淡墨传统纹饰线条（回纹/云气纹） -->
          <g opacity="0.12" fill="none" stroke="#3a3a3a" stroke-width="1.5">
            <path d="M340,170 Q360,155 380,165 Q400,150 420,162 Q440,148 460,158 Q480,145 500,155" />
            <path d="M370,185 Q390,175 410,185 Q430,172 450,180 Q470,168 490,178" />
            <path d="M360,150 Q380,138 400,148 Q420,135 440,145" />
          </g>
        </g>

        <!-- ========== 右云（放大+传统纹饰） ========== -->
        <g class="cloud-right" filter="url(#cloudBlur)">
          <!-- 云朵主体 -->
          <ellipse cx="700" cy="172" rx="200" ry="70" fill="url(#cloudGradR)" />
          <ellipse cx="800" cy="185" rx="140" ry="58" fill="url(#cloudGradR)" />
          <ellipse cx="620" cy="155" rx="130" ry="52" fill="url(#cloudGradR)" />
          <ellipse cx="740" cy="142" rx="110" ry="45" fill="url(#cloudGradR)" />
          <ellipse cx="670" cy="195" rx="100" ry="40" fill="url(#cloudGradR)" />
          <ellipse cx="850" cy="168" rx="90" ry="36" fill="url(#cloudGradR)" />
          <ellipse cx="750" cy="205" rx="80" ry="32" fill="url(#cloudGradR)" />
          <!-- 淡墨传统纹饰线条（回纹/云气纹） -->
          <g opacity="0.12" fill="none" stroke="#3a3a3a" stroke-width="1.5">
            <path d="M860,170 Q840,155 820,165 Q800,150 780,162 Q760,148 740,158 Q720,145 700,155" />
            <path d="M830,185 Q810,175 790,185 Q770,172 750,180 Q730,168 710,178" />
            <path d="M840,150 Q820,138 800,148 Q780,135 760,145" />
          </g>
        </g>

        <!-- ========== 远山 ========== -->
        <g filter="url(#inkBlurLg)" opacity="0.5">
          <polygon points="
            -50,560 15,480 35,490 60,465 90,478 115,455 140,470 160,450
            185,462 210,445 235,458 260,440 285,452 310,435 335,448
            360,432 385,445 410,428 435,442 460,425 485,440 510,425
            535,440 560,422 585,438 610,420 635,435 660,418 685,432
            710,415 735,430 760,415 785,428 810,410 835,425 860,408
            885,422 910,408 935,420 960,405 985,420 1010,402 1035,418
            1060,400 1085,415 1110,398 1135,412 1160,395 1185,410 1200,400 1250,415
            1250,800 -50,800
          " fill="url(#mtnFar)" />
        </g>

        <!-- ========== 中山 ========== -->
        <g filter="url(#inkBlurSm)" opacity="0.6">
          <polygon points="
            -50,590 25,520 45,530 70,505 95,518 120,492 145,508 170,484
            195,500 220,478 245,495 270,472 295,488 320,465 345,482
            370,458 395,475 420,452 445,468 470,448 495,462 520,442
            545,458 570,438 595,452 620,432 645,446 670,426 695,440
            720,420 745,435 770,418 795,430 820,412 845,425 870,408
            895,420 920,405 945,418 970,400 995,415 1020,398 1045,412
            1070,395 1095,410 1120,392 1145,408 1170,390 1195,405 1200,398 1250,408
            1250,800 -50,800
          " fill="url(#mtnMid)" />
        </g>

        <!-- ========== 近山 ========== -->
        <g>
          <polygon points="
            -50,630 20,565 42,575 65,555 88,568 112,548 135,560 158,540
            182,555 205,538 228,552 252,532 275,548 298,528 322,542
            345,525 368,540 392,520 415,535 438,518 462,532 485,515
            508,530 532,512 555,526 578,510 602,522 625,505 648,518
            672,500 695,515 718,498 742,510 765,495 788,508 812,490
            835,505 858,488 882,500 905,485 928,498 952,480 975,495
            998,478 1022,492 1045,475 1068,490 1092,472 1115,488 1138,470
            1162,485 1185,468 1200,478 1250,470
            1250,800 -50,800
          " fill="url(#mtnNear)" />
        </g>

        <!-- ========== 最前山 ========== -->
        <g opacity="0.92">
          <polygon points="
            -50,680 12,630 28,640 45,622 62,635 78,615 95,628 112,612
            128,625 145,608 162,620 178,602 195,615 212,600 228,612
            245,595 262,608 278,592 295,605 312,590 328,602 345,586
            362,598 378,582 395,595 412,580 428,592 445,576 462,588
            478,572 495,585 512,570 528,582 545,566 562,578 578,562
            595,575 612,560 628,572 645,555 662,568 678,552 695,565
            712,550 728,562 745,546 762,558 778,542 795,555 812,540
            828,552 845,536 862,548 878,532 895,545 912,530 928,542
            945,526 962,538 978,522 995,535 1012,520 1028,532 1045,515
            1062,528 1078,512 1095,525 1112,510 1128,522 1145,506 1162,518
            1178,502 1195,515 1200,510 1250,518
            1250,800 -50,800
          " fill="url(#mtnFront)" />
        </g>

        <!-- ========== 前景树影 ========== -->
        <g opacity="0.7" fill="url(#mtnFront)">
          <polygon points="80,708 86,670 90,678 96,648 100,660 106,630 110,644 114,622 118,636 120,698 116,710" />
          <polygon points="65,712 70,685 74,692 78,662 82,672 86,648 90,660 92,702 88,714" />
          <polygon points="1080,705 1084,675 1088,686 1092,658 1096,670 1100,698 1096,710" />
          <polygon points="1120,700 1123,672 1127,680 1130,655 1134,665 1136,692 1132,704" />
        </g>

        <!-- ========== 飞鸟 ========== -->
        <g class="birds" opacity="0.12">
          <path d="M280,260 L284,255 L288,260 L284,257 Z" fill="#6b2020"/>
          <path d="M300,270 L303,266 L306,270 L303,267 Z" fill="#6b2020"/>
          <path d="M260,250 L264,245 L268,250 L264,247 Z" fill="#6b2020"/>
          <path d="M850,200 L854,195 L858,200 L854,197 Z" fill="#6b2020"/>
          <path d="M865,215 L868,212 L871,215 L868,213 Z" fill="#6b2020"/>
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
  background: #fef9f4;
  font-family: 'Noto Serif SC', 'STKaiti', 'KaiTi', '楷体', 'STSong', 'SimSun', serif;
  transition: opacity 0.6s ease, filter 0.6s ease;
}
.splash-screen.exiting {
  opacity: 0;
  filter: brightness(1.5) blur(3px);
}

/* ==================== 宣纸底纹 ==================== */
.paper-bg {
  position: absolute; inset: 0; z-index: 0;
  background:
    repeating-linear-gradient(0deg, transparent, transparent 2px,
      rgba(180, 120, 100, 0.012) 2px, rgba(180, 120, 100, 0.012) 4px),
    repeating-linear-gradient(90deg, transparent, transparent 3px,
      rgba(180, 120, 100, 0.008) 3px, rgba(180, 120, 100, 0.008) 6px),
    linear-gradient(175deg, #fef9f4 0%, #fdf5ec 40%, #faf0e0 100%);
}

/* ==================== 天空晨光 ==================== */
.sky-glow {
  position: absolute; inset: 0; z-index: 1; pointer-events: none;
  background: radial-gradient(
    ellipse 80% 55% at 50% 12%,
    rgba(255, 176, 64, 0.18) 0%,
    rgba(240, 128, 64, 0.06) 35%,
    transparent 55%
  );
  opacity: 0;
  transition: opacity 1.6s ease 0.4s;
}
.sky-glow.bright { opacity: 1; }

/* ==================== SVG ==================== */
.scene {
  position: absolute; inset: 0;
  width: 100%; height: 100%;
  z-index: 2; pointer-events: none;
}

/* ==================== 朝阳 ==================== */
.sun-fixed {
  transform-origin: 600px 165px;
}
.sun-gold-ring {
  filter: blur(2px);
}

/* --- 左云 --- */
.cloud-left  { transform-origin: 500px 172px; }
.cloud-right { transform-origin: 700px 172px; }

/* 遮住太阳：云向中间靠拢，完全覆盖 */
.hidden .cloud-left  { transform: translateX(150px); }
.hidden .cloud-right { transform: translateX(-150px); }

/* 拨云见日：云向两边移开，但保留部分遮住太阳边缘 */
.reveal .cloud-left {
  animation: cloudPartLeft 1.5s cubic-bezier(0.25, 0.46, 0.45, 0.94) forwards;
}
.reveal .cloud-right {
  animation: cloudPartRight 1.5s cubic-bezier(0.25, 0.46, 0.45, 0.94) forwards;
}
@keyframes cloudPartLeft {
  0%   { transform: translateX(150px); }
  100% { transform: translateX(-100px); }
}
@keyframes cloudPartRight {
  0%   { transform: translateX(-150px); }
  100% { transform: translateX(100px); }
}

.text .cloud-left,
.exit .cloud-left {
  transform: translateX(-100px);
}
.text .cloud-right,
.exit .cloud-right {
  transform: translateX(100px);
}

/* ==================== 太阳显隐 ==================== */
.sun-fixed .sun-disc      { opacity: 0; transition: opacity 0.5s ease 0.8s; }
.sun-fixed .sun-halo-lg   { opacity: 0; transition: opacity 0.5s ease 0.7s; }
.sun-fixed .sun-halo-md   { opacity: 0; transition: opacity 0.5s ease 0.9s; }
.sun-fixed .sun-gold-ring { opacity: 0; transition: opacity 0.5s ease 0.6s; }

.reveal .sun-fixed .sun-disc,
.text .sun-fixed .sun-disc,
.exit .sun-fixed .sun-disc {
  opacity: 1;
}
.reveal .sun-fixed .sun-halo-lg,
.text .sun-fixed .sun-halo-lg,
.exit .sun-fixed .sun-halo-lg {
  opacity: 1;
  animation: sunWarmPulse 4s ease-in-out infinite;
}
.reveal .sun-fixed .sun-halo-md,
.text .sun-fixed .sun-halo-md,
.exit .sun-fixed .sun-halo-md {
  opacity: 1;
}
/* 金色光圈：拨云后显现 */
.reveal .sun-fixed .sun-gold-ring,
.text .sun-fixed .sun-gold-ring,
.exit .sun-fixed .sun-gold-ring {
  opacity: 1;
  animation: goldRingPulse 3s ease-in-out infinite;
}

@keyframes sunWarmPulse {
  0%, 100% { transform: scale(1); opacity: 1; }
  50%      { transform: scale(1.04); opacity: 0.85; }
}
@keyframes goldRingPulse {
  0%, 100% { transform: scale(1); opacity: 0.8; }
  50%      { transform: scale(1.06); opacity: 1; }
}

/* ===== 飞鸟 ===== */
.birds { animation: birdsFloat 10s ease-in-out infinite; }
@keyframes birdsFloat {
  0%, 100% { transform: translate(0, 0); }
  50%      { transform: translate(-12px, -8px); }
}

/* ==================== 回纹装饰框 ==================== */
.ornament-frame {
  position: absolute; inset: 0; z-index: 3; pointer-events: none;
}
.ornament-corner {
  position: absolute;
  width: 80px; height: 80px;
  opacity: 0.08;
}
.corner-tl {
  top: 40px; left: 40px;
  border-top: 2px solid #c41e3a; border-left: 2px solid #c41e3a;
}
.corner-tr {
  top: 40px; right: 40px;
  border-top: 2px solid #c41e3a; border-right: 2px solid #c41e3a;
}
.corner-bl {
  bottom: 40px; left: 40px;
  border-bottom: 2px solid #c41e3a; border-left: 2px solid #c41e3a;
}
.corner-br {
  bottom: 40px; right: 40px;
  border-bottom: 2px solid #c41e3a; border-right: 2px solid #c41e3a;
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
.text-layer.visible { opacity: 1; transform: translateY(0); }

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
  100% { opacity: 0.5; transform: scaleX(1); }
}

.orn-line {
  display: block; width: 56px; height: 1px;
  background: linear-gradient(90deg, transparent, #d4a030, transparent);
}
.orn-diamond { color: #d4a030; font-size: 10px; opacity: 0.6; }
.orn-diamond.small { font-size: 7px; opacity: 0.45; }

.main-title {
  font-size: clamp(42px, 9vw, 72px);
  font-weight: 700;
  letter-spacing: 12px;
  margin: 0;
  background: linear-gradient(180deg,
    #ffe680 0%, #ffd700 20%, #e8b800 50%, #c47820 75%, #8b6914 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  filter: drop-shadow(0 2px 14px rgba(200, 150, 30, 0.45));
  animation: titleRise 0.9s cubic-bezier(0.22, 0.61, 0.36, 1) forwards;
}
@keyframes titleRise {
  0%   {
    transform: translateY(36px); opacity: 0;
    filter: drop-shadow(0 2px 14px rgba(200, 150, 30, 0.45)) blur(4px);
  }
  50%  { filter: drop-shadow(0 2px 14px rgba(200, 150, 30, 0.45)) blur(0); }
  100% {
    transform: translateY(0); opacity: 1;
    filter: drop-shadow(0 2px 14px rgba(200, 150, 30, 0.45)) blur(0);
  }
}

.sub-title {
  font-size: clamp(16px, 3.5vw, 22px);
  font-weight: 500;
  color: #b8860b;
  letter-spacing: 10px;
  margin: 0;
  opacity: 0;
  animation: subtitleRise 0.8s ease-out 0.5s forwards;
}
@keyframes subtitleRise {
  0%   { transform: translateY(14px); opacity: 0; }
  100% { transform: translateY(0); opacity: 0.8; }
}

.skip-hint {
  position: absolute; bottom: 34px; left: 50%; transform: translateX(-50%);
  font-size: 12px; color: #c8b0a8; letter-spacing: 3px;
  transition: opacity 0.5s; z-index: 5; user-select: none;
}
.skip-hint.fading { opacity: 0; }

.splash-leave-active { transition: opacity 0.5s ease; }
.splash-leave-to { opacity: 0; }

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
