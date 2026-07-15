<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getMyProfile, saveProfile } from '@/api/profile'
import type { InheritorProfileVO, InheritorProfileDTO } from '@/types/api'

const profile = ref<InheritorProfileVO>({
  id: 0,
  employeeId: 0,
  name: '',
  phone: '',
  sex: '',
  honorTitle: '',
  selfIntro: '',
  craftTypes: '',
  address: '',
  avatarUrl: '',
})

const editMode = ref(false)
const form = ref<InheritorProfileDTO>({
  honorTitle: '',
  selfIntro: '',
  craftTypes: '',
  address: '',
  avatarUrl: '',
})
const saveMsg = ref('')

onMounted(() => { loadProfile() })

async function loadProfile() {
  try {
    const res = await getMyProfile()
    if (res.data) {
      profile.value = res.data
    }
  } catch { /* ignore - will use defaults */ }
}

function enterEdit() {
  form.value = {
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
  try {
    await saveProfile(form.value)
    await loadProfile()
    editMode.value = false
    saveMsg.value = '保存成功！'
    setTimeout(() => { saveMsg.value = '' }, 3000)
  } catch {
    saveMsg.value = '保存失败'
  }
}

function cancelEdit() {
  editMode.value = false
  saveMsg.value = ''
}

const craftArray = () => profile.value.craftTypes ? profile.value.craftTypes.split(/[,，]/).filter(Boolean) : []
</script>

<template>
  <section class="tab-content">
    <div class="section-header">
      <h2>入驻管理</h2>
      <button v-if="!editMode" class="btn-outline" @click="enterEdit">编辑信息</button>
    </div>

    <div class="profile-card">
      <div class="profile-avatar-section">
        <div class="profile-avatar-lg"></div>
        <div class="profile-name-role">
          <h3 v-if="!editMode">{{ profile.name || '未设置姓名' }}</h3>
          <p v-if="!editMode" class="profile-honor">{{ profile.honorTitle || '未设置荣誉称号' }}</p>
          <p v-if="!editMode" class="profile-phone">{{ profile.phone || '' }}</p>
        </div>
      </div>

      <div class="profile-detail-grid">
        <div class="detail-item">
          <label>荣誉称号</label>
          <span v-if="!editMode">{{ profile.honorTitle || '-' }}</span>
          <input v-else v-model="form.honorTitle" type="text" class="form-input" placeholder="如：国家级非遗传承人 · 苏绣" />
        </div>
        <div class="detail-item">
          <label>性别</label>
          <span>{{ profile.sex || '-' }}</span>
        </div>
        <div class="detail-item">
          <label>所在地</label>
          <span v-if="!editMode">{{ profile.address || '-' }}</span>
          <input v-else v-model="form.address" type="text" class="form-input" placeholder="如：江苏省苏州市" />
        </div>
        <div class="detail-item">
          <label>擅长技艺</label>
          <div v-if="!editMode" class="craft-tags">
            <span v-for="c in craftArray()" :key="c" class="craft-tag">{{ c }}</span>
            <span v-if="craftArray().length === 0">-</span>
          </div>
          <input v-else v-model="form.craftTypes" type="text" class="form-input" placeholder="多个技艺用逗号分隔" />
        </div>
      </div>

      <div class="detail-item" style="margin-top:16px">
        <label>个人简介</label>
        <p v-if="!editMode" class="profile-intro">{{ profile.selfIntro || '暂无简介' }}</p>
        <textarea v-else v-model="form.selfIntro" class="form-textarea" rows="4" placeholder="请输入个人简介"></textarea>
      </div>

      <div v-if="editMode" class="profile-edit-actions">
        <button class="btn-primary" @click="handleSave">保存修改</button>
        <button class="btn-outline" @click="cancelEdit">取消</button>
        <span v-if="saveMsg" class="save-msg" :class="{ error: saveMsg.includes('失败') }">{{ saveMsg }}</span>
      </div>
    </div>
  </section>
</template>

<style scoped>
.tab-content { }
.section-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 24px; }
.section-header h2 { font-size: 22px; color: #c41e3a; padding-left: 14px; border-left: 4px solid #c41e3a; }
.profile-card { background: #fff; border-radius: 12px; padding: 32px; box-shadow: 0 2px 10px rgba(0,0,0,0.06); }
.profile-avatar-section { display: flex; align-items: center; gap: 24px; margin-bottom: 28px; padding-bottom: 24px; border-bottom: 1px solid #f0f0f0; }
.profile-avatar-lg { width: 100px; height: 100px; border-radius: 50%; background: linear-gradient(135deg, #e8647a, #c41e3a); border: 4px solid #fff5f7; flex-shrink: 0; }
.profile-name-role h3 { font-size: 24px; color: #222; margin-bottom: 6px; }
.profile-honor { font-size: 15px; color: #c41e3a; font-weight: 600; }
.profile-phone { font-size: 13px; color: #999; margin-top: 4px; }
.profile-detail-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
.detail-item label { display: block; font-size: 13px; color: #999; margin-bottom: 6px; }
.detail-item span, .detail-item p { font-size: 15px; color: #333; }
.craft-tags { display: flex; gap: 8px; flex-wrap: wrap; }
.craft-tag { display: inline-block; padding: 4px 14px; background: #ffe9ec; color: #c41e3a; border-radius: 16px; font-size: 13px; font-weight: 600; }
.profile-intro { font-size: 14px; color: #555; line-height: 1.8; }
.profile-edit-actions { margin-top: 24px; padding-top: 20px; border-top: 1px solid #f0f0f0; display: flex; align-items: center; gap: 12px; }
.save-msg { font-size: 13px; color: #28a745; }
.save-msg.error { color: #dc3545; }
.btn-primary { padding: 10px 24px; font-size: 14px; color: #fff; background: #c41e3a; border: none; border-radius: 6px; cursor: pointer; font-weight: bold; }
.btn-primary:hover { background: #a8162b; }
.btn-outline { padding: 8px 20px; font-size: 13px; color: #c41e3a; background: #fff; border: 1px solid #c41e3a; border-radius: 6px; cursor: pointer; }
.btn-outline:hover { background: #fff5f7; }
.form-input { width: 100%; height: 42px; padding: 0 14px; border: 1px solid #e0e0e0; border-radius: 8px; font-size: 14px; box-sizing: border-box; outline: none; background: #fafafa; }
.form-input:focus { border-color: #c41e3a; box-shadow: 0 0 0 3px rgba(196,30,58,0.08); background: #fff; }
.form-textarea { width: 100%; padding: 14px; border: 1px solid #e0e0e0; border-radius: 8px; font-size: 14px; resize: vertical; box-sizing: border-box; outline: none; font-family: inherit; line-height: 1.8; background: #fafafa; }
.form-textarea:focus { border-color: #c41e3a; box-shadow: 0 0 0 3px rgba(196,30,58,0.08); background: #fff; }
@media (max-width: 992px) {
  .profile-detail-grid { grid-template-columns: 1fr; }
}
@media (max-width: 768px) {
  .profile-avatar-section { flex-direction: column; text-align: center; }
}
</style>
