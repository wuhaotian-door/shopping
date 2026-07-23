<script setup lang="ts">
import { ref, watch, onUnmounted, nextTick } from 'vue'

const props = defineProps<{ visible: boolean; imageUrl: string }>()
const emit = defineEmits<{ (e: 'update:visible', v: boolean): void; (e: 'crop', blob: Blob): void }>()

const canvasRef = ref<HTMLCanvasElement | null>(null)
const previewCanvasRef = ref<HTMLCanvasElement | null>(null)
let img: HTMLImageElement | null = null

// 裁剪区域（圆形，cropSize = 直径）
const cropSize = ref(0)
const cropCX = ref(0)  // 圆心 X
const cropCY = ref(0)  // 圆心 Y

let imgX = 0
let imgY = 0
let scale = 1
const MIN_SCALE = 0.3
const MAX_SCALE = 3

let dragging = false
let dragStartX = 0
let dragStartY = 0
let dragImgX = 0
let dragImgY = 0

let canvasW = 0
let canvasH = 0

function loadImage() {
  return new Promise<void>((resolve) => {
    if (img) { img.src = ''; URL.revokeObjectURL(img.src) }
    img = new Image()
    img.onload = () => resolve()
    img.src = props.imageUrl
  })
}

async function initCanvas() {
  await nextTick()
  const canvas = canvasRef.value
  if (!canvas) return
  canvasW = canvas.width = canvas.offsetWidth
  canvasH = canvas.height = canvas.offsetHeight

  // 圆形裁剪区：直径取画布短边的 82%
  cropSize.value = Math.min(canvasW, canvasH) * 0.82
  cropCX.value = canvasW / 2
  cropCY.value = canvasH / 2

  if (img) {
    const r = cropSize.value / 2
    const s = Math.max((r * 2) / img.width, (r * 2) / img.height)
    scale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, s))
    const sw = img.width * scale
    const sh = img.height * scale
    imgX = cropCX.value - sw / 2
    imgY = cropCY.value - sh / 2
  }
  draw()
}

function draw() {
  const canvas = canvasRef.value
  if (!canvas || !img) return
  const ctx = canvas.getContext('2d')!

  // 1. 先清空，画图片
  ctx.clearRect(0, 0, canvasW, canvasH)
  const sw = img.width * scale
  const sh = img.height * scale
  ctx.drawImage(img, imgX, imgY, sw, sh)

  // 2. 半透明遮罩（圆形外部）
  const cx = cropCX.value, cy = cropCY.value, r = cropSize.value / 2
  ctx.save()
  // 用 evenodd fill：外矩形 + 内圆 → 只填充圆外部
  ctx.fillStyle = 'rgba(0,0,0,0.55)'
  ctx.beginPath()
  ctx.rect(0, 0, canvasW, canvasH)
  ctx.arc(cx, cy, r, 0, Math.PI * 2, true) // 逆时针 = 挖空
  ctx.fill('evenodd')
  ctx.restore()

  // 3. 圆形裁剪框边框
  ctx.strokeStyle = '#fff'
  ctx.lineWidth = 2
  ctx.beginPath()
  ctx.arc(cx, cy, r, 0, Math.PI * 2)
  ctx.stroke()

  // 4. 更新预览
  updatePreview()
}

function updatePreview() {
  const pCanvas = previewCanvasRef.value
  if (!pCanvas || !img) return
  const pw = pCanvas.width
  pCanvas.height = pw
  const pctx = pCanvas.getContext('2d')!
  pctx.clearRect(0, 0, pw, pw)

  // 预览圆形裁剪：从原始图片中取对应区域
  // imgX/imgY 是缩放后图片在画布上的坐标，除以 scale 映射回原始图片坐标
  const cx = cropCX.value, cy = cropCY.value, r = cropSize.value / 2
  const sx = (cx - r - imgX) / scale   // 图片原始像素坐标
  const sy = (cy - r - imgY) / scale
  const sw = (r * 2) / scale            // 图片原始像素宽高

  pctx.save()
  pctx.beginPath()
  pctx.arc(pw / 2, pw / 2, pw / 2, 0, Math.PI * 2)
  pctx.clip()
  pctx.drawImage(img, sx, sy, sw, sw, 0, 0, pw, pw)
  pctx.restore()
}

// ===== 鼠标事件 =====
function onMouseDown(e: MouseEvent) {
  dragging = true
  dragStartX = e.offsetX
  dragStartY = e.offsetY
  dragImgX = imgX
  dragImgY = imgY
}

function onMouseMove(e: MouseEvent) {
  if (!dragging) return
  imgX = clampX(dragImgX + e.offsetX - dragStartX)
  imgY = clampY(dragImgY + e.offsetY - dragStartY)
  draw()
}

function onMouseUp() { dragging = false }
function onMouseLeave() { dragging = false }

function onWheel(e: WheelEvent) {
  e.preventDefault()
  const oldScale = scale
  let newScale = scale * (e.deltaY < 0 ? 1.08 : 0.92)
  newScale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, newScale))

  const mx = e.offsetX
  const my = e.offsetY
  const ratio = newScale / oldScale
  imgX = clampX(mx - (mx - imgX) * ratio)
  imgY = clampY(my - (my - imgY) * ratio)
  scale = newScale
  draw()
}

// 边界限制：确保图片覆盖整个圆形裁剪区
function clampX(x: number): number {
  const sw = img!.width * scale
  const cx = cropCX.value, r = cropSize.value / 2
  const minX = cx + r - sw
  const maxX = cx - r
  return Math.max(minX, Math.min(maxX, x))
}

function clampY(y: number): number {
  const sh = img!.height * scale
  const cy = cropCY.value, r = cropSize.value / 2
  const minY = cy + r - sh
  const maxY = cy - r
  return Math.max(minY, Math.min(maxY, y))
}

// ===== 确认（输出圆形裁剪） =====
function confirm() {
  if (!img) return
  const r = cropSize.value / 2
  const d = r * 2
  const outCanvas = document.createElement('canvas')
  outCanvas.width = d
  outCanvas.height = d
  const octx = outCanvas.getContext('2d')!

  // 裁剪圆形（映射回原始图片坐标）
  octx.beginPath()
  octx.arc(r, r, r, 0, Math.PI * 2)
  octx.clip()
  octx.drawImage(img,
    (cropCX.value - r - imgX) / scale, (cropCY.value - r - imgY) / scale, d / scale, d / scale,
    0, 0, d, d
  )

  outCanvas.toBlob((blob) => {
    if (blob) emit('crop', blob)
    emit('update:visible', false)
  }, 'image/jpeg', 0.92)
}

function cancel() {
  if (img) { img.src = ''; img = null }
  emit('update:visible', false)
}

watch(() => props.visible, async (v) => {
  if (v && props.imageUrl) { await loadImage(); await initCanvas() }
})

onUnmounted(() => { if (img) { img.src = ''; img = null } })
</script>

<template>
  <div v-if="visible" class="cropper-overlay" @click.self="cancel">
    <div class="cropper-dialog">
      <div class="cropper-header">
        <span>裁剪头像</span>
        <button class="cropper-close" @click="cancel">&#10005;</button>
      </div>
      <div class="cropper-body">
        <canvas
          ref="canvasRef"
          class="cropper-canvas"
          @mousedown="onMouseDown"
          @mousemove="onMouseMove"
          @mouseup="onMouseUp"
          @mouseleave="onMouseLeave"
          @wheel.prevent="onWheel"
        />
      </div>
      <div class="cropper-bottom">
        <div class="cropper-preview-area">
          <canvas ref="previewCanvasRef" width="80" height="80" class="preview-canvas" />
          <span class="preview-label">预览</span>
        </div>
        <div class="cropper-actions">
          <el-button @click="cancel">取消</el-button>
          <el-button type="primary" @click="confirm">确认裁剪</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.cropper-overlay {
  position: fixed; inset: 0; z-index: 1000;
  background: rgba(0,0,0,0.7);
  display: flex; align-items: center; justify-content: center;
}
.cropper-dialog {
  background: #fff; border-radius: 14px;
  width: 640px; max-width: 96vw;
  overflow: hidden; box-shadow: 0 16px 48px rgba(0,0,0,0.3);
}
.cropper-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 16px 20px; border-bottom: 1px solid #eee;
  font-size: 17px; font-weight: 600; color: #333;
}
.cropper-close {
  background: none; border: none; font-size: 20px; cursor: pointer; color: #999;
}
.cropper-close:hover { color: #333; }
.cropper-body {
  background: #333;
  display: flex; align-items: center; justify-content: center;
}
.cropper-canvas {
  display: block; width: 100%; height: 500px;
  cursor: grab;
}
.cropper-canvas:active { cursor: grabbing; }
.cropper-bottom {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 20px; border-top: 1px solid #eee;
}
.cropper-preview-area {
  display: flex; align-items: center; gap: 10px;
}
.preview-canvas {
  border-radius: 50%;
  border: 2px solid #e0e0e0;
  display: block;
}
.preview-label { font-size: 12px; color: #999; }
.cropper-actions {
  display: flex; gap: 10px;
}
</style>
