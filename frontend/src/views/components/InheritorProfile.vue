<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMyProfile, saveProfile } from '@/api/profile'
import { uploadFile } from '@/api/upload'
import { deleteMyAccount } from '@/api/auth'
import { ElMessage } from 'element-plus'
import AvatarCropper from '@/components/AvatarCropper.vue'
import { sendResetCode, resetPassword } from '@/api/auth'
import { listMyOrders } from '@/api/order'
import { listMyProductOrders } from '@/api/product'
import type { InheritorProfileVO, InheritorProfileDTO, OrderVO, ProductOrderVO } from '@/types/api'
import { useAuthStore } from '@/stores/auth'
import { useCartStore } from '@/stores/cart'
import { useHistory } from '@/composables/useHistory'

const router = useRouter()
const authStore = useAuthStore()
const cartStore = useCartStore()
const { getUserHistory } = useHistory()

const profile = ref<InheritorProfileVO>({
  id: 0, employeeId: 0, name: '', phone: '', sex: '', region: '',
  honorTitle: '', selfIntro: '', craftTypes: '', address: '', avatarUrl: '',
  birthDate: null, age: null,
})

const editMode = ref(false)
const form = ref({
  name: '', phone: '', sex: '', birthDate: '',
  honorTitle: '', selfIntro: '', craftTypes: '', address: '', avatarUrl: '',
})
const saveMsg = ref('')
const uploadingAvatar = ref(false)
const cropVisible = ref(false)
const cropImage = ref('')
const fileInput = ref<HTMLInputElement | null>(null)
let cropResolve: ((blob: Blob) => void) | null = null
const showHistory = ref(false)

onMounted(() => { loadProfile(); loadOrders(); loadProductOrders() })

// ===== 工坊预约订单 =====
const orders = ref<OrderVO[]>([])
const loadingOrders = ref(false)
const orderStatusMap: Record<number, { label: string; cls: string }> = {
  0: { label: '待确认', cls: 's-pending' },
  1: { label: '已确认', cls: 's-confirmed' },
  2: { label: '已完成', cls: 's-done' },
  3: { label: '已取消', cls: 's-cancel' },
}
async function loadOrders() {
  loadingOrders.value = true
  try { orders.value = (await listMyOrders()).data || [] }
  catch { /* */ } finally { loadingOrders.value = false }
}

// ===== 文创商品订单 =====
const productOrders = ref<ProductOrderVO[]>([])
const loadingPO = ref(false)
const poStatusMap: Record<number, { label: string; cls: string }> = {
  0: { label: '待发货', cls: 's-pending' },
  1: { label: '已发货', cls: 's-shipped' },
  2: { label: '已完成', cls: 's-done' },
  3: { label: '已取消', cls: 's-cancel' },
}
async function loadProductOrders() {
  loadingPO.value = true
  try { productOrders.value = (await listMyProductOrders()).data || [] }
  catch { /* */ } finally { loadingPO.value = false }
}

async function loadProfile() {
  try {
    const res = await getMyProfile()
    if (res.data) profile.value = res.data
  } catch { /* ignore */ }
}

function enterEdit() {
  form.value = {
    name: profile.value.name || '',
    phone: profile.value.phone || '',
    sex: profile.value.sex || '',
    birthDate: profile.value.birthDate || '',
    honorTitle: profile.value.honorTitle || '',
    selfIntro: profile.value.selfIntro || '',
    craftTypes: profile.value.craftTypes || '',
    address: profile.value.address || '',
    avatarUrl: profile.value.avatarUrl || '',
  }
  editMode.value = true
  saveMsg.value = ''
}

async function handleSave() {
  saveMsg.value = ''
  try {
    const dto: InheritorProfileDTO = {
      honorTitle: form.value.honorTitle || undefined,
      selfIntro: form.value.selfIntro || undefined,
      craftTypes: form.value.craftTypes || undefined,
      address: form.value.address || undefined,
      avatarUrl: form.value.avatarUrl || undefined,
      birthDate: form.value.birthDate || undefined,
    }
    await saveProfile(dto)
    await loadProfile()
    editMode.value = false
    saveMsg.value = '保存成功！'
    setTimeout(() => { saveMsg.value = '' }, 3000)
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : '保存失败'
    saveMsg.value = msg.includes('一年') || msg.includes('365') ? msg : '保存失败：' + msg
  }
}

function cancelEdit() {
  editMode.value = false
  saveMsg.value = ''
}

function startAvatarCrop(file: File) {
  const ext = file.name.split('.').pop()?.toLowerCase()
  if (!ext || !['jpg','jpeg','png','webp'].includes(ext)) { ElMessage.error('仅支持 jpg/png/webp 格式'); return }
  cropImage.value = URL.createObjectURL(file)
  cropVisible.value = true
  new Promise<Blob>((resolve) => { cropResolve = resolve }).then(doUploadCropped)
}
async function doUploadCropped(blob: Blob) {
  uploadingAvatar.value = true
  try {
    const res = await uploadFile(new File([blob], 'avatar.jpg', { type: 'image/jpeg' }), 'image')
    profile.value.avatarUrl = res.data
    form.value.avatarUrl = res.data
    await saveProfile({ avatarUrl: res.data })
    authStore.updateAvatar(res.data)
    ElMessage.success('头像更新成功')
  } catch (e: unknown) { ElMessage.error(e instanceof Error ? e.message : '头像上传失败') }
  finally { uploadingAvatar.value = false }
}
function onCropConfirm(blob: Blob) { cropResolve?.(blob); cropResolve = null }

const craftArray = () => profile.value.craftTypes ? profile.value.craftTypes.split(/[,，]/).filter(Boolean) : []

// 修改密码（邮箱验证码）
const pwdCode = ref('')
const newPassword = ref('')
const pwdLoading = ref(false)
const pwdMsg = ref('')
const codeSent = ref(false)

async function handleSendCode() {
  pwdLoading.value = true
  pwdMsg.value = ''
  try {
    await sendResetCode({ username: authStore.userInfo?.userName || '' })
    codeSent.value = true
    pwdMsg.value = '验证码已发送至您的邮箱'
  } catch (e: unknown) {
    pwdMsg.value = e instanceof Error ? e.message : '发送失败'
  } finally { pwdLoading.value = false }
}

const deletingAccount = ref(false)
async function handleDeleteAccount() {
  if (!confirm('确定要注销账号吗？此操作不可恢复，所有个人数据（好友关系、聊天记录、订单等）将被永久删除。')) return
  if (!confirm('再次确认：注销后将无法找回任何数据。是否继续？')) return
  deletingAccount.value = true
  try {
    await deleteMyAccount()
    alert('账号已注销')
    authStore.logout()
    router.push('/')
  } catch (e: unknown) {
    alert(e instanceof Error ? e.message : '注销失败')
  } finally { deletingAccount.value = false }
}

async function handleChangePwd() {
  if (!pwdCode.value) { pwdMsg.value = '请输入验证码'; return }
  if (newPassword.value.length < 6) { pwdMsg.value = '新密码至少6位'; return }
  pwdLoading.value = true
  pwdMsg.value = ''
  try {
    await resetPassword({ username: authStore.userInfo?.userName || '', code: pwdCode.value, newPassword: newPassword.value })
    pwdMsg.value = '密码修改成功！'
    newPassword.value = ''
    pwdCode.value = ''
    codeSent.value = false
  } catch (e: unknown) {
    pwdMsg.value = e instanceof Error ? e.message : '修改失败'
  } finally { pwdLoading.value = false }
}
</script>

<template>
  <section class="tab-content">
    <div class="section-header">
      <h2>个人中心</h2>
      <button v-if="!editMode" class="btn-outline" @click="enterEdit">编辑信息</button>
    </div>

    <div class="profile-card">
      <!-- 头像+姓名 -->
      <div class="profile-avatar-section">
        <div class="profile-avatar-lg" :style="profile.avatarUrl ? { backgroundImage: 'url(' + profile.avatarUrl + ')', backgroundSize: 'cover', backgroundPosition: 'center' } : {}"></div>
        <div class="profile-name-role">
          <h3 v-if="!editMode">{{ profile.name || '未设置姓名' }}</h3>
          <input v-else v-model="form.name" type="text" class="form-input name-input" placeholder="姓名" />
          <p v-if="!editMode" class="profile-honor">{{ profile.honorTitle || '未设置荣誉称号' }}</p>
          <p v-if="!editMode" class="profile-region">{{ profile.region || '' }}</p>
          <div v-if="editMode" class="avatar-upload-btn" style="margin-top:8px">
            <input ref="fileInput" type="file" accept=".jpg,.jpeg,.png,.webp" style="display:none" @change="(e) => { const t = e.target as HTMLInputElement; const f = t.files?.[0]; if (f) startAvatarCrop(f); t.value = '' }" />
            <el-button size="small" :loading="uploadingAvatar" @click="fileInput?.click()">{{ uploadingAvatar ? '上传中...' : '更换头像' }}</el-button>
          </div>
        </div>
      </div>

      <!-- 基本信息 -->
      <div class="profile-detail-grid">
        <div class="detail-item">
          <label>手机号</label>
          <span v-if="!editMode">{{ profile.phone || '-' }}</span>
          <input v-else v-model="form.phone" type="text" maxlength="11" class="form-input" placeholder="手机号" />
        </div>
        <div class="detail-item">
          <label>性别</label>
          <span v-if="!editMode">{{ profile.sex || '-' }}</span>
          <select v-else v-model="form.sex" class="form-input">
            <option value="">请选择</option>
            <option value="男">男</option>
            <option value="女">女</option>
          </select>
        </div>
        <div class="detail-item">
          <label>年龄</label>
          <span v-if="!editMode">{{ profile.age != null ? profile.age + ' 岁' : '-' }}</span>
          <div v-else>
            <input v-model="form.birthDate" type="date" class="form-input" />
            <span class="birth-hint">一年仅可修改一次</span>
          </div>
          <span v-if="!editMode && profile.birthDate" class="birth-hint">（{{ profile.birthDate }}）</span>
        </div>
        <div class="detail-item">
          <label>荣誉称号</label>
          <span v-if="!editMode">{{ profile.honorTitle || '-' }}</span>
          <input v-else v-model="form.honorTitle" type="text" class="form-input" placeholder="如：国家级非遗传承人 · 苏绣" />
        </div>
        <div class="detail-item">
          <label>所在地</label>
          <span v-if="!editMode">{{ profile.address || '-' }}</span>
          <input v-else v-model="form.address" type="text" class="form-input" placeholder="如：江苏省苏州市" />
        </div>
      </div>

      <!-- 擅长技艺 -->
      <div class="detail-item" style="margin-top:16px">
        <label>擅长技艺</label>
        <div v-if="!editMode" class="craft-tags">
          <span v-for="c in craftArray()" :key="c" class="craft-tag">{{ c }}</span>
          <span v-if="craftArray().length === 0">-</span>
        </div>
        <input v-else v-model="form.craftTypes" type="text" class="form-input" placeholder="多个技艺用逗号分隔，如：苏绣,双面绣,剪纸" />
      </div>

      <!-- 个人简介 -->
      <div class="detail-item" style="margin-top:16px">
        <label>个人简介</label>
        <p v-if="!editMode" class="profile-intro">{{ profile.selfIntro || '暂无简介' }}</p>
        <textarea v-else v-model="form.selfIntro" class="form-textarea" rows="5" placeholder="介绍一下您的非遗传承经历和擅长领域..."></textarea>
      </div>

      <!-- 编辑按钮 -->
      <!-- ===== 快捷入口卡片 ===== -->
      <div class="quick-cards">
        <div class="quick-card" @click="router.push('/cart')">
          <span class="qc-icon">🛒</span>
          <span class="qc-label">购物车</span>
          <span v-if="cartStore.count > 0" class="qc-badge">{{ cartStore.count }}</span>
        </div>
        <div class="quick-card" @click="showHistory = !showHistory">
          <span class="qc-icon">👣</span>
          <span class="qc-label">个人足迹</span>
        </div>
      </div>

      <!-- 足迹面板 -->
      <div v-if="showHistory" class="history-panel">
        <h3>浏览足迹</h3>
        <div v-if="getUserHistory().length === 0" class="empty-tip">暂无浏览记录</div>
        <div v-for="h in getUserHistory().slice(0, 10)" :key="h.path" class="history-item" @click="router.push(h.path)">
          <span class="hi-title">{{ h.title }}</span>
          <span class="hi-time">{{ h.time }}</span>
        </div>
      </div>

      <div v-if="editMode" class="profile-edit-actions">
        <button class="btn-primary" @click="handleSave">保存修改</button>
        <button class="btn-outline" @click="cancelEdit">取消</button>
        <span v-if="saveMsg" class="save-msg" :class="{ error: saveMsg.includes('失败') }">{{ saveMsg }}</span>
      </div>
    </div>

    <!-- 修改密码 -->
    <div class="password-card">
      <h3>修改密码（邮箱验证）</h3>
      <div class="pwd-form">
        <div v-if="!codeSent" class="pwd-row">
          <button class="btn-primary" :disabled="pwdLoading" @click="handleSendCode">
            {{ pwdLoading ? '发送中...' : '发送验证码到邮箱' }}
          </button>
        </div>
        <div v-else class="pwd-row">
          <input v-model="pwdCode" type="text" placeholder="6位验证码" maxlength="6" class="form-input code-input" />
          <input v-model="newPassword" type="password" placeholder="新密码（至少6位）" class="form-input" />
          <button class="btn-primary" :disabled="!pwdCode || newPassword.length < 6 || pwdLoading" @click="handleChangePwd">
            {{ pwdLoading ? '修改中...' : '确认修改' }}
          </button>
          <button class="btn-outline" @click="codeSent = false; pwdCode = ''">重新发送</button>
        </div>
        <p v-if="pwdMsg" class="save-msg" :class="{ error: pwdMsg.includes('失败') || pwdMsg.includes('错误') }">{{ pwdMsg }}</p>
      </div>
    </div>

    <!-- ===== 注销账号 ===== -->
    <div class="section-header" style="margin-top:36px"><h2>账号管理</h2></div>
    <div class="delete-card">
      <div class="delete-row">
        <div><span class="delete-label">注销账号</span><span class="delete-desc">注销后所有个人数据将被永久删除，不可恢复</span></div>
        <button class="btn-delete" @click="handleDeleteAccount" :disabled="deletingAccount">{{ deletingAccount ? '注销中...' : '注销账号' }}</button>
      </div>
    </div>

    <!-- ===== 工坊预约订单 ===== -->
    <div class="section-header" style="margin-top:36px"><h2>工坊预约</h2><button class="btn-outline" @click="router.push('/orders')">查看全部订单 →</button></div>
    <div class="order-card">
      <div v-if="loadingOrders" class="empty-tip">加载中...</div>
      <div v-else-if="orders.length === 0" class="empty-tip">暂无工坊预约</div>
      <table v-else class="order-table">
        <thead><tr><th>传承人</th><th>日期</th><th>时段</th><th>人数</th><th>备注</th><th>状态</th></tr></thead>
        <tbody>
          <tr v-for="o in orders" :key="o.id">
            <td>{{ o.inheritorName || '-' }}</td>
            <td>{{ o.date }}</td>
            <td>{{ o.timeSlot }}</td>
            <td>{{ o.peopleCount }}人</td>
            <td class="rmk">{{ o.remark || '-' }}</td>
            <td><span class="s-tag" :class="orderStatusMap[o.status]?.cls">{{ orderStatusMap[o.status]?.label }}</span></td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- ===== 文创商品订单 ===== -->
    <div class="section-header" style="margin-top:36px"><h2>文创订单</h2><button class="btn-outline" @click="router.push('/orders')">查看全部订单 →</button></div>
    <div class="order-card">
      <div v-if="loadingPO" class="empty-tip">加载中...</div>
      <div v-else-if="productOrders.length === 0" class="empty-tip">暂无文创订单</div>
      <table v-else class="order-table">
        <thead><tr><th>商品</th><th>单价</th><th>数量</th><th>金额</th><th>电话</th><th>状态</th><th>时间</th></tr></thead>
        <tbody>
          <tr v-for="o in productOrders" :key="o.id">
            <td>{{ o.productName }}</td>
            <td>¥{{ o.price?.toFixed(2) }}</td>
            <td>{{ o.quantity }}</td>
            <td>¥{{ o.totalAmount?.toFixed(2) }}</td>
            <td>{{ o.phone || '-' }}</td>
            <td><span class="s-tag" :class="poStatusMap[o.status]?.cls">{{ poStatusMap[o.status]?.label }}</span></td>
            <td>{{ o.createTime?.slice(0,10) }}</td>
          </tr>
        </tbody>
      </table>
    </div>
    <AvatarCropper v-model:visible="cropVisible" :image-url="cropImage" @crop="onCropConfirm" />
</section>
</template>

<style scoped>
.tab-content { }
.section-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 24px; }
.section-header h2 { font-size: 22px; color: #c41e3a; padding-left: 14px; border-left: 4px solid #c41e3a; }
.profile-card { background: #fff; border-radius: 14px; padding: 36px; box-shadow: 0 2px 12px rgba(0,0,0,0.06); }
.profile-avatar-section { display: flex; align-items: center; gap: 24px; margin-bottom: 28px; padding-bottom: 24px; border-bottom: 1px solid #f0f0f0; }
.profile-avatar-lg { width: 100px; height: 100px; border-radius: 50%; background: linear-gradient(135deg, #e8647a, #c41e3a); border: 4px solid #fff5f7; flex-shrink: 0; }
.profile-name-role h3 { font-size: 24px; color: #222; margin: 0 0 6px; }
.name-input { font-size: 22px !important; font-weight: bold; width: 200px; }
.profile-honor { font-size: 15px; color: #c41e3a; font-weight: 600; margin: 0 0 2px; }
.profile-region { font-size: 13px; color: #999; margin: 0; }
.profile-detail-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
.detail-item { }
.detail-item label { display: block; font-size: 13px; color: #999; margin-bottom: 6px; }
.detail-item span, .detail-item p { font-size: 15px; color: #333; }
.craft-tags { display: flex; gap: 8px; flex-wrap: wrap; }
.craft-tag { display: inline-block; padding: 4px 14px; background: #ffe9ec; color: #c41e3a; border-radius: 16px; font-size: 13px; font-weight: 600; }
.profile-intro { font-size: 14px; color: #555; line-height: 1.8; }
.profile-edit-actions { margin-top: 28px; padding-top: 20px; border-top: 1px solid #f0f0f0; display: flex; align-items: center; gap: 12px; }
.save-msg { font-size: 13px; color: #28a745; }
.save-msg.error { color: #dc3545; }
.btn-primary { padding: 10px 24px; font-size: 14px; color: #fff; background: #c41e3a; border: none; border-radius: 8px; cursor: pointer; font-weight: bold; transition: 0.2s; }
.btn-primary:hover { background: #a8162b; }
.btn-outline { padding: 8px 20px; font-size: 13px; color: #c41e3a; background: #fff; border: 1px solid #c41e3a; border-radius: 8px; cursor: pointer; transition: 0.2s; }
.btn-outline:hover { background: #fff5f7; }
.birth-hint { font-size: 12px; color: #bbb; margin-left: 6px; }
.password-card {
  background: #fff; border-radius: 14px; padding: 28px 36px; margin-top: 20px; box-shadow: 0 2px 12px rgba(0,0,0,0.06);
}
.password-card h3 { font-size: 16px; color: #c41e3a; margin: 0 0 16px; padding-left: 10px; border-left: 3px solid #c41e3a; }
.pwd-row { display: flex; gap: 12px; }
.pwd-row .form-input { flex: 1; }
.pwd-row .btn-primary { width: auto; padding: 0 24px; white-space: nowrap; }
.form-input { width: 100%; height: 42px; padding: 0 14px; border: 1px solid #e0e0e0; border-radius: 8px; font-size: 14px; box-sizing: border-box; outline: none; background: #fafafa; }
.form-input:focus { border-color: #c41e3a; box-shadow: 0 0 0 3px rgba(196,30,58,0.08); background: #fff; }
.form-textarea { width: 100%; padding: 14px; border: 1px solid #e0e0e0; border-radius: 8px; font-size: 14px; resize: vertical; box-sizing: border-box; outline: none; font-family: inherit; line-height: 1.8; background: #fafafa; }
.form-textarea:focus { border-color: #c41e3a; box-shadow: 0 0 0 3px rgba(196,30,58,0.08); background: #fff; }
/* 订单卡片 */
.order-card { background: #fff; border-radius: 14px; padding: 20px 24px; box-shadow: 0 2px 12px rgba(0,0,0,0.06); overflow-x: auto; }
.order-table { width: 100%; border-collapse: collapse; min-width: 600px; }
.order-table th, .order-table td { padding: 12px 14px; text-align: left; font-size: 13px; }
.order-table thead { background: #fff5f7; }
.order-table th { color: #c41e3a; font-weight: 600; font-size: 12px; }
.order-table tbody tr { border-bottom: 1px solid #f5f5f5; }
.order-table tbody tr:hover { background: #fefafa; }
.rmk { max-width: 120px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.s-tag { display: inline-block; padding: 2px 10px; border-radius: 10px; font-size: 11px; font-weight: 600; }
.s-pending { background: #fff3cd; color: #856404; }
.s-confirmed { background: #d4edda; color: #155724; }
.s-shipped { background: #cce5ff; color: #004085; }
.s-done { background: #e2e3e5; color: #383d41; }
.s-cancel { background: #f8d7da; color: #721c24; }
.empty-tip { text-align: center; padding: 40px; color: #999; font-size: 14px; }

/* 快捷入口卡片 */
.quick-cards { display: flex; gap: 16px; margin-top: 20px; }
.quick-card {
  position: relative;
  flex: 1; background: #fff; border-radius: 14px;
  padding: 24px 16px; text-align: center; cursor: pointer;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
  transition: transform 0.2s, box-shadow 0.2s;
  border: 1px solid rgba(196,30,58,0.08);
}
.quick-card:hover { transform: translateY(-3px); box-shadow: 0 6px 20px rgba(196,30,58,0.12); }
.qc-icon { font-size: 32px; display: block; margin-bottom: 8px; }
.qc-label { font-size: 14px; color: #666; font-weight: 600; }
.qc-badge {
  position: absolute; top: 12px; right: 16px;
  min-width: 22px; height: 22px; border-radius: 11px;
  background: #c41e3a; color: #fff; font-size: 12px; font-weight: 700;
  display: flex; align-items: center; justify-content: center;
}

/* 足迹面板 */
.history-panel {
  background: #fff; border-radius: 14px; padding: 20px 24px;
  margin-top: 16px; box-shadow: 0 2px 12px rgba(0,0,0,0.06);
}
.history-panel h3 { font-size: 16px; color: #c41e3a; margin-bottom: 12px; }
.history-item {
  display: flex; justify-content: space-between; align-items: center;
  padding: 10px 0; border-bottom: 1px solid #f5f5f5; cursor: pointer;
  transition: background 0.15s;
}
.history-item:hover { background: #fefafa; margin: 0 -12px; padding: 10px 12px; border-radius: 6px; }
.hi-title { font-size: 14px; color: #333; }
.hi-time { font-size: 12px; color: #aaa; }

@media (max-width: 992px) { .profile-detail-grid { grid-template-columns: 1fr; } }
.delete-card { background: #fff; border-radius: 14px; padding: 20px 24px; box-shadow: 0 2px 12px rgba(0,0,0,0.06); }
.delete-row { display: flex; align-items: center; justify-content: space-between; gap: 20px; }
.delete-label { font-size: 15px; font-weight: 600; color: #dc3545; display: block; }
.delete-desc { font-size: 12px; color: #999; display: block; margin-top: 4px; }
.btn-delete { padding: 10px 24px; font-size: 14px; color: #fff; background: #dc3545; border: none; border-radius: 6px; cursor: pointer; font-weight: 600; transition: 0.2s; white-space: nowrap; }
.btn-delete:hover { background: #c82333; }

@media (max-width: 768px) { .profile-avatar-section { flex-direction: column; text-align: center; } }
</style>
