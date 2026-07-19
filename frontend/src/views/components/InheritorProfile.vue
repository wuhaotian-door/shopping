<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getMyProfile, saveProfile } from '@/api/profile'
import { sendResetCode, resetPassword } from '@/api/auth'
import type { InheritorProfileVO, InheritorProfileDTO } from '@/types/api'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()

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

onMounted(() => { loadProfile() })

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
        <div class="profile-avatar-lg"></div>
        <div class="profile-name-role">
          <h3 v-if="!editMode">{{ profile.name || '未设置姓名' }}</h3>
          <input v-else v-model="form.name" type="text" class="form-input name-input" placeholder="姓名" />
          <p v-if="!editMode" class="profile-honor">{{ profile.honorTitle || '未设置荣誉称号' }}</p>
          <p v-if="!editMode" class="profile-region">{{ profile.region || '' }}</p>
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
@media (max-width: 992px) { .profile-detail-grid { grid-template-columns: 1fr; } }
@media (max-width: 768px) { .profile-avatar-section { flex-direction: column; text-align: center; } }
</style>
