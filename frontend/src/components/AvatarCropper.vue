<script setup lang="ts">
import { ref, watch, onUnmounted, nextTick } from 'vue'

const props = defineProps<{ visible: boolean; imageUrl: string }>()
const emit = defineEmits<{ (e: 'update:visible', v: boolean): void; (e: 'crop', blob: Blob): void }>()

const canvasRef = ref<HTMLCanvasElement | null>(null)
const previewCanvasRef = ref<HTMLCanvasElement | null>(null)
let img: HTMLImageElement | null = null

// 裁剪框固定为正方形，边长取画布短边
const cropSize = ref(0)
const cropX = ref(0)
const cropY = ref(0)

// 图片变换状态
let imgX = 0        // 图片左上角在画布上的 x
let imgY = 0        // 图片左上角在画布上的 y
let scale = 1       // 缩放倍数 0.3~3
const MIN_SCALE = 0.3
const MAX_SCALE = 3

// 鼠标拖拽
let dragging = false
let dragStartX = 0
let dragStartY = 0
let dragImgX = 0
let dragImgY = 0

// canvas 尺寸相关
let canvasW = 0
let canvasH = 0

function loadImage() {
  return new Promise<void>((resolve) => {
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

  // 裁剪框居中，正方形
  cropSize.value = Math.min(canvasW, canvasH) * 0.82
  cropX.value = (canvasW - cropSize.value) / 2
  cropY.value = (canvasH - cropSize.value) / 2

  // 初始缩放：让图片宽或高刚好填满裁剪框
  if (img) {
    const s = Math.max(cropSize.value / img.width, cropSize.value / img.height)
    scale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, s))
    // 图片居中于裁剪框
    const sw = img.width * scale
    const sh = img.height * scale
    imgX = cropX.value + (cropSize.value - sw) / 2
    imgY = cropY.value + (cropSize.value - sh) / 2
  }
  draw()
}

function draw() {
  const canvas = canvasRef.value
  if (!canvas || !img) return
  const ctx = canvas.getContext('2d')!
  ctx.clearRect(0, 0, canvasW, canvasH)

  // 1. 绘制图片
  const sw = img.width * scale
  const sh = img.height * scale
  ctx.drawImage(img, imgX, imgY, sw, sh)

  // 2. 绘制半透明遮罩（裁剪框外）
  const cx = cropX.value, cy = cropY.value, cs = cropSize.value
  ctx.fillStyle = 'rgba(0,0,0,0.55)'
  ctx.fillRect(0, 0, canvasW, cy)                    // 上方
  ctx.fillRect(0, cy + cs, canvasW, canvasH - cy - cs) // 下方
  ctx.fillRect(0, cy, cx, cs)                         // 左方
  ctx.fillRect(cx + cs, cy, canvasW - cx - cs, cs)     // 右方

  // 3. 裁剪框边框
  ctx.strokeStyle = '#fff'
  ctx.lineWidth = 2
  ctx.strokeRect(cx, cy, cs, cs)

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

  // 裁剪预览区域（裁剪框内的图片部分）
  const cx = cropX.value, cy = cropY.value, cs = cropSize.value
  pctx.save()
  pctx.beginPath()
  pctx.arc(pw / 2, pw / 2, pw / 2, 0, Math.PI * 2)
  pctx.clip()
  pctx.drawImage(img,
    cx - imgX, cy - imgY, cs, cs,   // 源区域（裁剪框内）
    0, 0, pw, pw                     // 目标（预览画布）
  )
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
  const dx = e.offsetX - dragStartX
  const dy = e.offsetY - dragStartY
  imgX = clampX(dragImgX + dx)
  imgY = clampY(dragImgY + dy)
  draw()
}

function onMouseUp() {
  dragging = false
}

function onMouseLeave() {
  dragging = false
}

function onWheel(e: WheelEvent) {
  e.preventDefault()
  const oldScale = scale
  let newScale = scale * (e.deltaY < 0 ? 1.08 : 0.92)
  newScale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, newScale))

  // 缩放锚点为鼠标在画布上的坐标
  const mx = e.offsetX
  const my = e.offsetY

  // 图片上对应点：imgPoint = (mouse - imgPos) / oldScale
  // 缩放后该点仍在鼠标位置：imgPoint * newScale + newImgPos = mouse
  // => newImgPos = mouse - imgPoint * newScale
  const ratio = newScale / oldScale
  imgX = clampX(mx - (mx - imgX) * ratio)
  imgY = clampY(my - (my - imgY) * ratio)
  scale = newScale
  draw()
}

// 边界限制：图片至少覆盖裁剪框的一部分
function clampX(x: number): number {
  const sw = img!.width * scale
  const cx = cropX.value, cs = cropSize.value
  const minX = cx + cs - sw  // 图片右边缘至少到裁剪框左边缘
  const maxX = cx            // 图片左边缘最多到裁剪框左边缘
  return Math.max(minX, Math.min(maxX, x))
}

function clampY(y: number): number {
  const sh = img!.height * scale
  const cy = cropY.value, cs = cropSize.value
  const minY = cy + cs - sh
  const maxY = cy
  return Math.max(minY, Math.min(maxY, y))
}

// ===== 确认 =====
function confirm() {
  if (!img) return
  const cx = cropX.value, cy = cropY.value, cs = cropSize.value
  const outCanvas = document.createElement('canvas')
  outCanvas.width = cs
  outCanvas.height = cs
  const octx = outCanvas.getContext('2d')!
  octx.drawImage(img,
    cx - imgX, cy - imgY, cs, cs,
    0, 0, cs, cs
  )
  outCanvas.toBlob((blob) => {
    if (blob) emit('crop', blob)
    emit('update:visible', false)
  }, 'image/jpeg', 0.92)
}

function cancel() {
  img = null
  emit('update:visible', false)
}

watch(
  () => props.visible,
  async (v) => {
    if (v && props.imageUrl) {
      await loadImage()
      await initCanvas()
    }
  }
)

onUnmounted(() => { img = null })
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
          <canvas ref="previewCanvasRef" width="72" height="72" class="preview-canvas" />
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