<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { selfRegister, inheritorRegister } from '@/api/auth'
import { uploadFile } from '@/api/upload'
import AvatarCropper from '@/components/AvatarCropper.vue'
import DecoCard from '@/components/DecoCard.vue'
import type { RoleType } from '@/types/api'

onMounted(() => {
  document.title = "码上非遗——用户注册"
})

const router = useRouter()

const form = reactive({
  username: '',
  name: '',
  email: '',
  password: '',
  confirmPassword: '',
  phone: '',
  sex: '',
  idNumber: '',
  region: '',
  birthDate: '',
  avatarUrl: '',
  role: '' as RoleType | '',
})

const errorMsg = ref('')
const loading = ref(false)
const showPassword = ref(false)
const showConfirmPassword = ref(false)
const uploadingAvatar = ref(false)
const avatarPreview = ref('')
const cropperVisible = ref(false)
const cropperImageUrl = ref('')

function handleAvatarSelect(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  if (!['image/jpeg','image/png','image/webp'].includes(file.type)) {
    errorMsg.value = '仅支持 JPG/PNG/WebP 格式'; return
  }
  errorMsg.value = ''
  cropperImageUrl.value = URL.createObjectURL(file)
  cropperVisible.value = true
  // 重置 input 以便重新选择同一文件
  ;(input as HTMLInputElement).value = ''
}

async function onCropperCrop(blob: Blob) {
  // 先展示本地预览（即时反馈）
  avatarPreview.value = URL.createObjectURL(blob)
  uploadingAvatar.value = true
  try {
    const res = await uploadFile(new File([blob], 'avatar.jpg', { type: 'image/jpeg' }), 'image')
    form.avatarUrl = res.data
    // 上传成功后替换为服务器 URL
    URL.revokeObjectURL(avatarPreview.value)
    avatarPreview.value = res.data
  } catch (e: unknown) {
    errorMsg.value = e instanceof Error ? e.message : '头像上传失败'
    URL.revokeObjectURL(avatarPreview.value)
    avatarPreview.value = ''
  } finally {
    uploadingAvatar.value = false
    URL.revokeObjectURL(cropperImageUrl.value)
  }
}

// 字段是否已触碰过（失焦后才开始显示"有效"状态）
const touched = reactive<Record<string, boolean>>({
  username: false, name: false, email: false, password: false,
  confirmPassword: false, phone: false, idNumber: false, region: false, role: false,
})

const fieldErrors = reactive<Record<string, string>>({
  username: '', name: '', email: '', password: '',
  confirmPassword: '', phone: '', idNumber: '', region: '', role: '',
})

const fieldOk = reactive<Record<string, boolean>>({
  username: false, name: false, email: false, password: false,
  confirmPassword: false, phone: false, idNumber: false, region: false, role: false,
})

/** 实时字段校验 — 输入时触发，给出具体错误提示 */
function validateField(field: string) {
  const v = (form as any)[field]
  const t = (form as any)[field]?.trim?.() ?? ''

  switch (field) {
    case 'username':
      if (!v || !t) fieldErrors.username = '请输入用户名'
      else if (t.length < 2) fieldErrors.username = '用户名至少 2 个字符'
      else if (!/^[a-zA-Z0-9_一-鿿]+$/.test(t)) fieldErrors.username = '用户名包含非法字符'
      else fieldErrors.username = ''
      fieldOk.username = touched.username && !fieldErrors.username && !!t
      break
    case 'name':
      if (!v || !t) fieldErrors.name = '请输入姓名'
      else if (t.length < 2) fieldErrors.name = '姓名至少 2 个字符'
      else fieldErrors.name = ''
      fieldOk.name = touched.name && !fieldErrors.name && !!t
      break
    case 'email':
      if (!v || !t) fieldErrors.email = '请输入邮箱'
      else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(t)) fieldErrors.email = '邮箱格式不正确'
      else fieldErrors.email = ''
      fieldOk.email = touched.email && !fieldErrors.email && !!t
      break
    case 'password':
      if (!v) fieldErrors.password = '请输入密码'
      else if (v.length < 6) fieldErrors.password = '密码至少 6 位'
      else if (v.length > 20) fieldErrors.password = '密码不能超过 20 位'
      else fieldErrors.password = ''
      fieldOk.password = touched.password && !fieldErrors.password && !!v
      // 联动确认密码
      if (form.confirmPassword) {
        if (v !== form.confirmPassword) fieldErrors.confirmPassword = '两次密码不一致'
        else fieldErrors.confirmPassword = ''
        fieldOk.confirmPassword = touched.confirmPassword && !fieldErrors.confirmPassword && !!form.confirmPassword
      }
      break
    case 'confirmPassword':
      if (!v) fieldErrors.confirmPassword = '请确认密码'
      else if (v !== form.password) fieldErrors.confirmPassword = '两次密码不一致'
      else fieldErrors.confirmPassword = ''
      fieldOk.confirmPassword = touched.confirmPassword && !fieldErrors.confirmPassword && !!v
      break
    case 'phone':
      if (isInheritor.value && !t) fieldErrors.phone = '请输入手机号'
      else if (t && !/^1[3-9]\d{9}$/.test(t)) fieldErrors.phone = '请输入正确的11位手机号'
      else fieldErrors.phone = ''
      fieldOk.phone = touched.phone && !fieldErrors.phone && !!t
      break
    case 'idNumber':
      if (isInheritor.value && !t) fieldErrors.idNumber = '请输入身份证号'
      else if (t && !/^\d{17}[\dXx]$/.test(t)) fieldErrors.idNumber = '身份证号格式不正确（18位）'
      else fieldErrors.idNumber = ''
      fieldOk.idNumber = touched.idNumber && !fieldErrors.idNumber && !!t
      break
    case 'region':
      if (isInheritor.value && !t) fieldErrors.region = '请选择完整省市地区'
      else fieldErrors.region = ''
      fieldOk.region = touched.region && !fieldErrors.region && !!t
      break
    case 'role':
      fieldErrors.role = !v ? '请选择注册类型' : ''
      fieldOk.role = !fieldErrors.role && !!v
      break
  }
}

/** 失焦时标记字段已触碰，显示通过/错误状态 */
function onBlur(field: string) {
  touched[field] = true
  validateField(field)
}

const isInheritor = computed(() => form.role === 'INHERITOR')

// 省市数据（与GeoJSON地图行政区划一致）
const CHINA_REGIONS: Record<string, string[]> = {
  '北京市': ['东城区','西城区','朝阳区','海淀区','丰台区','石景山区','通州区','大兴区','顺义区','昌平区','房山区','门头沟区','平谷区','怀柔区','密云区','延庆区'],
  '天津市': ['和平区','河东区','河西区','南开区','河北区','红桥区','东丽区','西青区','津南区','北辰区','武清区','宝坻区','滨海新区','宁河区','静海区','蓟州区'],
  '河北省': ['石家庄市','唐山市','秦皇岛市','邯郸市','邢台市','保定市','张家口市','承德市','沧州市','廊坊市','衡水市'],
  '山西省': ['太原市','大同市','阳泉市','长治市','晋城市','朔州市','晋中市','运城市','忻州市','临汾市','吕梁市'],
  '内蒙古自治区': ['呼和浩特市','包头市','乌海市','赤峰市','通辽市','鄂尔多斯市','呼伦贝尔市','巴彦淖尔市','乌兰察布市'],
  '辽宁省': ['沈阳市','大连市','鞍山市','抚顺市','本溪市','丹东市','锦州市','营口市','阜新市','辽阳市','盘锦市','铁岭市','朝阳市','葫芦岛市'],
  '吉林省': ['长春市','吉林市','四平市','辽源市','通化市','白山市','松原市','白城市','延边朝鲜族自治州'],
  '黑龙江省': ['哈尔滨市','齐齐哈尔市','鸡西市','鹤岗市','双鸭山市','大庆市','伊春市','佳木斯市','七台河市','牡丹江市','黑河市','绥化市'],
  '上海市': ['黄浦区','徐汇区','长宁区','静安区','普陀区','虹口区','杨浦区','浦东新区','闵行区','宝山区','嘉定区','金山区','松江区','青浦区','奉贤区','崇明区'],
  '江苏省': ['南京市','无锡市','徐州市','常州市','苏州市','南通市','连云港市','淮安市','盐城市','扬州市','镇江市','泰州市','宿迁市'],
  '浙江省': ['杭州市','宁波市','温州市','嘉兴市','湖州市','绍兴市','金华市','衢州市','舟山市','台州市','丽水市'],
  '安徽省': ['合肥市','芜湖市','蚌埠市','淮南市','马鞍山市','淮北市','铜陵市','安庆市','黄山市','滁州市','阜阳市','宿州市','六安市','亳州市','池州市','宣城市'],
  '福建省': ['福州市','厦门市','莆田市','三明市','泉州市','漳州市','南平市','龙岩市','宁德市'],
  '江西省': ['南昌市','景德镇市','萍乡市','九江市','新余市','鹰潭市','赣州市','吉安市','宜春市','抚州市','上饶市'],
  '山东省': ['济南市','青岛市','淄博市','枣庄市','东营市','烟台市','潍坊市','济宁市','泰安市','威海市','日照市','临沂市','德州市','聊城市','滨州市','菏泽市'],
  '河南省': ['郑州市','开封市','洛阳市','平顶山市','安阳市','鹤壁市','新乡市','焦作市','濮阳市','许昌市','漯河市','三门峡市','南阳市','商丘市','信阳市','周口市','驻马店市'],
  '湖北省': ['武汉市','黄石市','十堰市','宜昌市','襄阳市','鄂州市','荆门市','孝感市','荆州市','黄冈市','咸宁市','随州市','恩施土家族苗族自治州'],
  '湖南省': ['长沙市','株洲市','湘潭市','衡阳市','邵阳市','岳阳市','常德市','张家界市','益阳市','郴州市','永州市','怀化市','娄底市','湘西土家族苗族自治州'],
  '广东省': ['广州市','韶关市','深圳市','珠海市','汕头市','佛山市','江门市','湛江市','茂名市','肇庆市','惠州市','梅州市','汕尾市','河源市','阳江市','清远市','东莞市','中山市','潮州市','揭阳市','云浮市'],
  '广西壮族自治区': ['南宁市','柳州市','桂林市','梧州市','北海市','防城港市','钦州市','贵港市','玉林市','百色市','贺州市','河池市','来宾市','崇左市'],
  '海南省': ['海口市','三亚市','三沙市','儋州市'],
  '重庆市': ['渝中区','江北区','沙坪坝区','九龙坡区','南岸区','渝北区','巴南区','万州区','涪陵区','大渡口区','北碚区','綦江区','大足区','黔江区','长寿区','江津区','合川区','永川区','南川区','璧山区','铜梁区','潼南区','荣昌区','开州区','梁平区','武隆区'],
  '四川省': ['成都市','自贡市','攀枝花市','泸州市','德阳市','绵阳市','广元市','遂宁市','内江市','乐山市','南充市','眉山市','宜宾市','广安市','达州市','雅安市','巴中市','资阳市'],
  '贵州省': ['贵阳市','六盘水市','遵义市','安顺市','毕节市','铜仁市','黔西南布依族苗族自治州','黔东南苗族侗族自治州','黔南布依族苗族自治州'],
  '云南省': ['昆明市','曲靖市','玉溪市','保山市','昭通市','丽江市','普洱市','临沧市','楚雄彝族自治州','红河哈尼族彝族自治州','文山壮族苗族自治州','西双版纳傣族自治州','大理白族自治州','德宏傣族景颇族自治州','怒江傈僳族自治州','迪庆藏族自治州'],
  '西藏自治区': ['拉萨市','日喀则市','昌都市','林芝市','山南市','那曲市','阿里地区'],
  '陕西省': ['西安市','铜川市','宝鸡市','咸阳市','渭南市','延安市','汉中市','榆林市','安康市','商洛市'],
  '甘肃省': ['兰州市','嘉峪关市','金昌市','白银市','天水市','武威市','张掖市','平凉市','酒泉市','庆阳市','定西市','陇南市','临夏回族自治州','甘南藏族自治州'],
  '青海省': ['西宁市','海东市','海北藏族自治州','黄南藏族自治州','海南藏族自治州','果洛藏族自治州','玉树藏族自治州','海西蒙古族藏族自治州'],
  '宁夏回族自治区': ['银川市','石嘴山市','吴忠市','固原市','中卫市'],
  '新疆维吾尔自治区': ['乌鲁木齐市','克拉玛依市','吐鲁番市','哈密市','昌吉回族自治州','博尔塔拉蒙古自治州','巴音郭楞蒙古自治州','阿克苏地区','克孜勒苏柯尔克孜自治州','喀什地区','和田地区','伊犁哈萨克自治州','塔城地区','阿勒泰地区'],
  '台湾省': ['台北市','高雄市','台中市','台南市'],
  '香港特别行政区': ['中西区','湾仔区','东区','南区','油尖旺区','深水埗区','九龙城区','黄大仙区','观塘区','荃湾区','屯门区','元朗区','北区','大埔区','西贡区','沙田区','葵青区','离岛区'],
  '澳门特别行政区': ['花地玛堂区','圣安多尼堂区','大堂区','望德堂区','风顺堂区','嘉模堂区','圣方济各堂区'],
}
const provinces = Object.keys(CHINA_REGIONS)
const selectedProvince = ref('')
const availableCities = computed(() => selectedProvince.value ? CHINA_REGIONS[selectedProvince.value] || [] : [])
function onProvinceChange() {
  form.region = '' // 切换省份时重置城市选择
}

function validate(): string | null {
  if (!form.username.trim()) return '请输入用户名'
  if (!form.name.trim()) return '请输入姓名'
  if (!form.email.trim()) return '请输入邮箱'
  if (!form.password) return '请输入密码'
  if (form.password.length < 6) return '密码长度至少为 6 位'
  if (form.password !== form.confirmPassword) return '两次输入的密码不一致'
  if (!form.role) return '请选择注册类型'

  // 手机号长度校验（数据库phone列VARCHAR(11)）
  if (form.phone.trim() && form.phone.trim().length > 11) {
    return '手机号不能超过11位，请输入正确的手机号码'
  }

  // 非遗传承人需要填写更多信息
  if (isInheritor.value) {
    if (!form.idNumber.trim()) return '请输入身份证号'
    if (!form.phone.trim()) return '请输入手机号'
    if (!form.region.trim()) return '请选择完整的省市地区'
  }

  return null
}

async function handleRegister() {
  const err = validate()
  if (err) {
    errorMsg.value = err
    return
  }

  loading.value = true
  errorMsg.value = ''

  try {
    if (isInheritor.value) {
      // 非遗传承人注册（详细）
      await inheritorRegister({
        username: form.username.trim(),
        name: form.name.trim(),
        password: form.password,
        phone: form.phone.trim(),
        sex: form.sex || undefined,
        idNumber: form.idNumber.trim(),
        birthDate: form.birthDate || undefined,
        region: form.region.trim(),
        avatarUrl: form.avatarUrl || undefined,
      })
      router.push('/register-success?type=inheritor')
    } else {
      // 普通用户注册（简化）
      await selfRegister({
        username: form.username.trim(),
        name: form.name.trim(),
        password: form.password,
        phone: form.phone || undefined,
        sex: form.sex || undefined,
        email: form.email.trim(),
        avatarUrl: form.avatarUrl || undefined,
      })
      router.push('/register-success?type=user')
    }
  } catch (e: unknown) {
    errorMsg.value = e instanceof Error ? e.message : '注册失败，请重试'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <header class="page-header">
    <div class="logo" @click="$router.push('/')">
      <img src="/logo.png" alt="码上非遗" class="logo-img" />
      <span class="logo-text">码上非遗</span>
    </div>
    <a @click="$router.push('/')" class="back-link">返回首页</a>
  </header>

  <div class="page-wrap">
    <!-- 左侧国风装饰卡片 -->
    <div class="float-left-box">
      <DecoCard />
    </div>

    <!-- 右侧注册表单 -->
    <div class="login-right-box">
      <div class="login-card">
        <h1 class="login-title">
          {{ isInheritor ? '非遗传承人入驻' : '用户注册' }}
        </h1>
        <p class="login-subtitle" v-if="isInheritor">填写详细信息，展示您的非遗技艺</p>
        <div class="title-line"></div>

        <form class="login-form" @submit.prevent="handleRegister">
          <div class="form-grid">
          <!-- 角色选择（放在表单最前面） -->
          <div class="form-item full-width">
            <label class="role-label">注册类型 *</label>
            <div class="role-selector">
              <div
                  class="role-card"
                  :class="{ active: form.role === 'USER' }"
                  @click="form.role = 'USER'; touched.role = true; validateField('role')"
              >
                <div class="role-icon user-icon">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                    <circle cx="12" cy="7" r="4"/>
                  </svg>
                </div>
                <span class="role-name">普通用户注册</span>
                <span class="role-desc">浏览非遗文化内容</span>
              </div>
              <div
                  class="role-card"
                  :class="{ active: form.role === 'INHERITOR' }"
                  @click="form.role = 'INHERITOR'; touched.role = true; validateField('role')"
              >
                <div class="role-icon inheritor-icon">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <polygon points="12 2 22 8.5 22 15.5 12 22 2 15.5 2 8.5 12 2"/>
                    <line x1="12" y1="22" x2="12" y2="15.5"/>
                    <polyline points="22 8.5 12 15.5 2 8.5"/>
                  </svg>
                </div>
                <span class="role-name">非遗传承人入驻</span>
                <span class="role-desc">展示与传承技艺</span>
              </div>
            </div>
            <span class="field-msg" :class="{ 'msg-error': fieldErrors.role, 'msg-ok': fieldOk.role }">{{ fieldErrors.role || (fieldOk.role ? '✓' : '') }}</span>
          </div>
          <!-- 头像上传 -->
          <div class="form-item">
            <label>头像（可选）</label>
            <div class="avatar-upload-row">
              <div class="avatar-preview-box">
                <img v-if="avatarPreview" :src="avatarPreview" class="avatar-preview-img" />
                <span v-else class="avatar-preview-empty">?</span>
              </div>
              <label class="avatar-upload-btn" :class="{ disabled: uploadingAvatar }">
                {{ uploadingAvatar ? '上传中...' : '选择图片' }}
                <input type="file" accept="image/jpeg,image/png,image/webp" hidden @change="handleAvatarSelect" :disabled="uploadingAvatar" />
              </label>
            </div>
            <span class="field-error" v-if="form.avatarUrl && !uploadingAvatar">✓ 头像已上传</span>
          </div>
          <!-- 用户名 | 姓名 -->
          <div class="form-item">
            <label for="reg-username">用户名 *</label>
            <input
                id="reg-username"
                v-model="form.username"
                type="text"
                placeholder="登录账号（2-20位字母数字下划线）"
                autocomplete="username"
                :class="{ 'input-error': touched.username && fieldErrors.username, 'input-ok': fieldOk.username }"
                @input="validateField('username')"
                @blur="onBlur('username')"
            />
            <span class="field-msg" :class="{ 'msg-error': touched.username && fieldErrors.username, 'msg-ok': fieldOk.username }">{{ (touched.username && fieldErrors.username) || (fieldOk.username ? '✓ 格式正确' : '') }}</span>
          </div>
          <div class="form-item">
            <label for="reg-name">姓名 *</label>
            <input
                id="reg-name"
                v-model="form.name"
                type="text"
                placeholder="真实姓名（至少2个字符）"
                :class="{ 'input-error': touched.name && fieldErrors.name, 'input-ok': fieldOk.name }"
                @input="validateField('name')"
                @blur="onBlur('name')"
            />
            <span class="field-msg" :class="{ 'msg-error': touched.name && fieldErrors.name, 'msg-ok': fieldOk.name }">{{ (touched.name && fieldErrors.name) || (fieldOk.name ? '✓' : '') }}</span>
          </div>

          <!-- 邮箱 | 手机号 -->
          <div class="form-item">
            <label for="reg-email">邮箱 *</label>
            <input
                id="reg-email"
                v-model="form.email"
                type="email"
                placeholder="用于找回密码"
                autocomplete="email"
                :class="{ 'input-error': touched.email && fieldErrors.email, 'input-ok': fieldOk.email }"
                @input="validateField('email')"
                @blur="onBlur('email')"
            />
            <span class="field-msg" :class="{ 'msg-error': touched.email && fieldErrors.email, 'msg-ok': fieldOk.email }">{{ (touched.email && fieldErrors.email) || (fieldOk.email ? '✓ 格式正确' : '') }}</span>
          </div>
          <div class="form-item">
            <label for="reg-phone">手机号{{ isInheritor ? ' *' : '' }}</label>
            <input
                id="reg-phone"
                v-model="form.phone"
                type="text"
                placeholder="11位手机号，如 13800138000"
                maxlength="11"
                :class="{ 'input-error': touched.phone && fieldErrors.phone, 'input-ok': fieldOk.phone }"
                @input="validateField('phone')"
                @blur="onBlur('phone')"
            />
            <span class="field-msg" :class="{ 'msg-error': touched.phone && fieldErrors.phone, 'msg-ok': fieldOk.phone }">{{ (touched.phone && fieldErrors.phone) || (fieldOk.phone ? '✓' : '') }}</span>
          </div>

          <!-- 密码 | 确认密码 -->
          <div class="form-item">
            <label for="reg-password">密码 *</label>
            <div class="pwd-wrap">
              <input
                  id="reg-password"
                  v-model="form.password"
                  :type="showPassword ? 'text' : 'password'"
                  placeholder="6-20 位密码"
                  autocomplete="new-password"
                  :class="{ 'input-error': touched.password && fieldErrors.password, 'input-ok': fieldOk.password }"
                  @input="validateField('password')"
                  @blur="onBlur('password')"
              />
              <button type="button" class="pwd-toggle" @click="showPassword = !showPassword" :title="showPassword ? '隐藏密码' : '显示密码'">
                {{ showPassword ? '🙈' : '👁' }}
              </button>
            </div>
            <span class="field-msg" :class="{ 'msg-error': touched.password && fieldErrors.password, 'msg-ok': fieldOk.password }">{{ (touched.password && fieldErrors.password) || (fieldOk.password ? '✓ 密码强度合格' : '') }}</span>
          </div>
          <div class="form-item">
            <label for="reg-confirm">确认密码 *</label>
            <div class="pwd-wrap">
              <input
                  id="reg-confirm"
                  v-model="form.confirmPassword"
                  :type="showConfirmPassword ? 'text' : 'password'"
                  placeholder="再次输入密码"
                  autocomplete="new-password"
                  :class="{ 'input-error': touched.confirmPassword && fieldErrors.confirmPassword, 'input-ok': fieldOk.confirmPassword }"
                  @input="validateField('confirmPassword')"
                  @blur="onBlur('confirmPassword')"
              />
              <button type="button" class="pwd-toggle" @click="showConfirmPassword = !showConfirmPassword" :title="showConfirmPassword ? '隐藏密码' : '显示密码'">
                {{ showConfirmPassword ? '🙈' : '👁' }}
              </button>
            </div>
            <span class="field-msg" :class="{ 'msg-error': touched.confirmPassword && fieldErrors.confirmPassword, 'msg-ok': fieldOk.confirmPassword }">{{ (touched.confirmPassword && fieldErrors.confirmPassword) || (fieldOk.confirmPassword ? '✓ 密码一致' : '') }}</span>
          </div>

          <!-- 非遗传承人专属字段 -->
          <template v-if="isInheritor">
            <div class="form-divider full-width">
              <span>传承人信息</span>
            </div>

              <div class="form-item">
                <label for="reg-idnumber">身份证号 *</label>
                <input
                    id="reg-idnumber"
                    v-model="form.idNumber"
                    type="text"
                    placeholder="18位身份证号"
                    maxlength="18"
                    :class="{ 'input-error': touched.idNumber && fieldErrors.idNumber, 'input-ok': fieldOk.idNumber }"
                    @input="validateField('idNumber')"
                    @blur="onBlur('idNumber')"
                />
                <span class="field-msg" :class="{ 'msg-error': touched.idNumber && fieldErrors.idNumber, 'msg-ok': fieldOk.idNumber }">{{ (touched.idNumber && fieldErrors.idNumber) || (fieldOk.idNumber ? '✓' : '') }}</span>
              </div>

            <div class="form-item">
              <label for="reg-birthdate">出生日期</label>
              <input
                  id="reg-birthdate"
                  v-model="form.birthDate"
                  type="date"
              />
            </div>
              <div class="form-item full-width">
                <label>所属地区 *</label>
                <div class="region-selects">
                  <select v-model="selectedProvince" @change="onProvinceChange(); validateField('region')" @blur="onBlur('region')" class="region-select">
                    <option value="">选择省份</option>
                    <option v-for="p in provinces" :key="p" :value="p">{{ p }}</option>
                  </select>
                  <select v-model="form.region" :disabled="!selectedProvince" class="region-select" @change="validateField('region')" @blur="onBlur('region')">
                    <option value="">选择城市</option>
                    <option v-for="c in availableCities" :key="c" :value="selectedProvince + c">{{ c }}</option>
                  </select>
                </div>
                <span class="field-msg" :class="{ 'msg-error': touched.region && fieldErrors.region, 'msg-ok': fieldOk.region }">{{ (touched.region && fieldErrors.region) || (fieldOk.region ? '✓' : '') }}</span>
              </div>

            <div class="form-item">
              <label for="reg-sex">性别</label>
              <select id="reg-sex" v-model="form.sex">
                <option value="">请选择</option>
                <option value="男">男</option>
                <option value="女">女</option>
              </select>
            </div>
          </template>

          <!-- 普通用户可选字段（折叠显示） -->
          <template v-if="!isInheritor && form.role === 'USER'">
            <div class="form-item">
              <label for="reg-sex">性别</label>
              <select id="reg-sex" v-model="form.sex">
                <option value="">请选择</option>
                <option value="男">男</option>
                <option value="女">女</option>
              </select>
            </div>
          </template>

          <p v-if="errorMsg" class="error-msg full-width">{{ errorMsg }}</p>

          <button type="submit" class="login-btn full-width" :disabled="loading">
            {{ loading ? '注册中...' : (isInheritor ? '提交入驻申请' : '注 册') }}
          </button>
          </div>
        </form>

        <div class="switch-tip">
          已有账号？
          <span @click="router.push('/login')" class="register-text">立即登录</span>
        </div>
      </div>
    </div>
  </div>

  <footer class="page-footer">
    <p>校园非遗与文创传承人平台 © 2026 码上非遗</p>
  </footer>
  <AvatarCropper :visible="cropperVisible" :image-url="cropperImageUrl" @update:visible="cropperVisible = $event" @crop="onCropperCrop" />
</template>


<style scoped>
/* ===== 复用的 LoginView 样式 ===== */
.page-header {
  background: linear-gradient(135deg, #8b1a2b 0%, #a01e35 40%, #c41e3a 100%);
  color: #fff;
  padding: 0 6%;
  height: 72px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: sticky;
  top: 0;
  z-index: 100;
  font-family: 'Noto Serif SC', 'STKaiti', 'KaiTi', '楷体', 'STSong', serif;
  box-shadow: 0 2px 16px rgba(139, 26, 43, 0.35);
}
.logo {
  display: flex; align-items: center; gap: 10px;
  cursor: pointer; white-space: nowrap;
}
.logo-img { height: 42px; width: auto; }
.logo-text { font-size: 22px; font-weight: 700; letter-spacing: 4px; color: #fff; }
.back-link {
  color: #fff;
  cursor: pointer;
}
.back-link:hover {
  text-decoration: underline;
}

.page-wrap {
  min-height: calc(100vh - 64px - 120px);
  display: grid;
  grid-template-columns: 1fr 1fr;
  background: #fcf4e8;
}

.float-left-box {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.float-card {
  width: 100%;
  max-width: 480px;
  height: 520px;
  border-radius: 18px;
  background: linear-gradient(145deg, #c41e3a, #e8647a);
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 30px rgba(196, 30, 58, 0.2);
  cursor: pointer;
  perspective: 1000px;
  transform-style: preserve-3d;
}

/* 鼠标位置变量 */
.float-card {
  --mouse-x: 50%;
  --mouse-y: 50%;
}

/* 流光效果 */
.flow-light {
  position: absolute;
  width: 120%;
  height: 120%;
  top: -10%;
  left: -10%;
  background: radial-gradient(
      circle at var(--mouse-x, 50%) var(--mouse-y, 50%),
      rgba(255, 255, 255, 0.24),
      transparent 40%
  );
  opacity: 0;
  transition: opacity 0.25s ease;
  pointer-events: none;
}

.float-card:hover .flow-light {
  opacity: 1;
}

/* 国风剪纸/窗花纹样 */
.pattern-layer {
  position: absolute;
  width: 180px;
  height: 180px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 12px;
  opacity: 0;
  transform: scale(0.8);
  transition: all 0.4s ease;
  pointer-events: none;
}

.pattern-1 {
  top: 30px;
  left: 30px;
  clip-path: polygon(50% 0%, 100% 50%, 50% 100%, 0% 50%);
}

.pattern-2 {
  bottom: 40px;
  right: 40px;
  width: 140px;
  height: 140px;
  clip-path: polygon(25% 0%, 75% 0%, 100% 50%, 75% 100%, 25% 100%, 0% 50%);
}

.pattern-3 {
  top: 50%;
  left: 50%;
  width: 100px;
  height: 100px;
  transform: translate(-50%, -50%) scale(0.8);
  clip-path: circle(50% at 50% 50%);
}

.float-card:hover .pattern-layer {
  opacity: 0.65;
  transform: scale(1);
}

.float-card:hover .pattern-1 {
  animation: floatMove 8s ease-in-out infinite;
}

.float-card:hover .pattern-2 {
  animation: floatMove 10s ease-in-out infinite reverse;
}

.float-card:hover .pattern-3 {
  animation: pulse 4s ease-in-out infinite;
}

/* 浮动装饰 */
.float-deco {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.12);
  animation: floatMove 6s ease-in-out infinite;
  pointer-events: none;
}
.deco1 {
  width: 180px;
  height: 180px;
  top: 40px;
  left: 40px;
  animation-delay: 0s;
}
.deco2 {
  width: 120px;
  height: 120px;
  bottom: 80px;
  right: 60px;
  animation-delay: 1.8s;
}
.deco3 {
  width: 70px;
  height: 70px;
  top: 160px;
  right: 120px;
  animation-delay: 3.5s;
}

/* 跟随粒子 */
.particle {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.35);
  animation: particleFloat 3s ease-in-out infinite;
  pointer-events: none;
}

@keyframes floatMove {
  0% {
    transform: translateY(0) scale(1);
  }
  50% {
    transform: translateY(-22px) scale(1.08);
  }
  100% {
    transform: translateY(0) scale(1);
  }
}

@keyframes pulse {
  0% {
    transform: translate(-50%, -50%) scale(0.8);
    opacity: 0.65;
  }
  50% {
    transform: translate(-50%, -50%) scale(1);
    opacity: 1;
  }
  100% {
    transform: translate(-50%, -50%) scale(0.8);
    opacity: 0.65;
  }
}

@keyframes particleFloat {
  0% {
    transform: translateY(0) translateX(0);
    opacity: 0.35;
  }
  50% {
    transform: translateY(-30px) translateX(8px);
    opacity: 0.7;
  }
  100% {
    transform: translateY(0) translateX(0);
    opacity: 0.35;
  }
}

.float-text-wrap {
  z-index: 2;
  text-align: center;
  color: #fff;
  padding: 0 30px;
}
.float-text-wrap h2 {
  font-size: 42px;
  margin-bottom: 10px;
  letter-spacing: 4px;
}
.slogan {
  font-size: 20px;
  margin-bottom: 24px;
  opacity: 0.9;
}
.desc {
  font-size: 15px;
  line-height: 2;
  opacity: 0.85;
}

.login-right-box {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
}
.login-card {
  width: 100%;
  max-width: 520px;
  padding: 32px 28px;
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 6px 22px rgba(196, 30, 58, 0.12);
}
.login-title {
  text-align: center;
  font-size: 22px;
  color: #c41e3a;
  margin-bottom: 4px;
}
.login-subtitle {
  text-align: center;
  font-size: 13px;
  color: #999;
  margin-bottom: 8px;
}
.title-line {
  width: 50px;
  height: 3px;
  background: #c41e3a;
  margin: 0 auto 24px;
}
.login-form {
  display: flex;
  flex-direction: column;
}

/* 双列表单网格 */
.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 14px;
}
.form-grid .full-width {
  grid-column: 1 / -1;
}
.form-item {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  margin-bottom: 10px;
}
.form-item label {
  width: 64px;
  flex-shrink: 0;
  font-size: 13px;
  color: #444;
  margin: 0 6px 0 0;
  text-align: right;
}
.form-item input,
.form-item select {
  flex: 1;
  min-width: 0;
  height: 36px;
  padding: 0 12px;
  border: 1px solid #e8647a;
  border-radius: 8px;
  font-size: 13px;
  color: #333;
  box-sizing: border-box;
  transition: 0.25s;
  outline: none;
  background: #fff;
}
.form-item input:focus,
.form-item select:focus {
  border-color: #c41e3a;
  box-shadow: 0 0 0 3px rgba(196, 30, 58, 0.1);
}

/* 密码可见切换 */
.pwd-wrap { position: relative; display: flex; align-items: center; flex: 1; min-width: 0; }
.pwd-wrap input { padding-right: 44px !important; }
.pwd-toggle {
  position: absolute; right: 4px; top: 50%; transform: translateY(-50%);
  width: 36px; height: 36px; border: none; background: transparent;
  font-size: 18px; cursor: pointer; display: flex; align-items: center;
  justify-content: center; border-radius: 6px; transition: background 0.2s;
  line-height: 1; padding: 0; min-height: auto;
}
.pwd-toggle:hover { background: rgba(196, 30, 58, 0.08); }
.form-item select {
  cursor: pointer;
  appearance: auto;
}

.region-selects {
  display: flex;
  gap: 12px;
}
.region-select {
  flex: 1;
}
.region-select:disabled {
  background: #f5f5f5;
  color: #bbb;
  cursor: not-allowed;
}

/* 双列布局 */
/* 分割线 */
.form-divider {
  display: flex;
  align-items: center;
  margin: 8px 0 16px;
  gap: 12px;
}
.form-divider::before,
.form-divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: #e8647a;
  opacity: 0.4;
}
.form-divider span {
  font-size: 13px;
  color: #c41e3a;
  font-weight: 600;
  white-space: nowrap;
}

.error-msg {
  color: #c41e3a;
  font-size: 13px;
  margin: 0 0 16px 0;
  text-align: center;
}
.login-btn {
  width: 100%;
  height: 42px;
  background: #c41e3a;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: bold;
  cursor: pointer;
  transition: 0.25s;
}
.login-btn:hover:not(:disabled) {
  background: #a8162b;
}
.login-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.switch-tip {
  text-align: center;
  margin-top: 18px;
  font-size: 14px;
  color: #666;
}
.register-text {
  color: #c41e3a;
  cursor: pointer;
}
.register-text:hover {
  text-decoration: underline;
}

.page-footer {
  background-color: #c41e3a;
  color: #fff;
  text-align: center;
  padding: 24px 5%;
}

/* ===== 角色选择器 ===== */
.role-label {
  display: block;
  width: 100%;
  text-align: left;
  font-size: 13px;
  color: #444;
  margin: 0 0 6px 0;
}
/* ===== 逐字段实时校验反馈 ===== */
.field-msg {
  width: 100%;
  margin-left: 70px;
  font-size: 11px;
  min-height: 16px;
  line-height: 1.4;
  transition: color 0.2s;
}
.full-width .field-msg { margin-left: 0; }
.msg-error { color: #dc3545; }
.msg-ok { color: #28a745; font-weight: 600; }

/* 输入框校验状态 */
.input-error {
  border-color: #dc3545 !important;
  box-shadow: 0 0 0 3px rgba(220, 53, 69, 0.1) !important;
  background: #fff5f5 !important;
}
.input-ok {
  border-color: #28a745 !important;
  box-shadow: 0 0 0 3px rgba(40, 167, 69, 0.08) !important;
  background: #f6fff6 !important;
}

/* 旧 field-error 兼容（未迁移字段回退） */
.field-error {
  width: 100%;
  margin-left: 70px;
  font-size: 11px;
  color: #c41e3a;
  min-height: 16px;
  line-height: 1.4;
}
.full-width .field-error { margin-left: 0; }
.field-error:empty { display: none; }
.role-selector {
  display: flex;
  gap: 12px;
}
.role-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 14px 10px;
  border: 2px solid #e8647a;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.25s;
  background: #fff;
}
.role-card:hover {
  border-color: #c41e3a;
  background: #fef5f7;
}
.role-card.active {
  border-color: #c41e3a;
  background: #fef0f2;
  box-shadow: 0 0 0 3px rgba(196, 30, 58, 0.15);
}
.role-icon {
  width: 36px;
  height: 36px;
  margin-bottom: 6px;
  color: #c41e3a;
}
.role-icon svg {
  width: 100%;
  height: 100%;
}
.role-name {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin-bottom: 4px;
}
.role-desc {
  font-size: 12px;
  color: #999;
}
.role-card.active .role-name {
  color: #c41e3a;
}
.role-card.active .role-desc {
  color: #e8647a;
}

/* 头像上传 */
.avatar-upload-row {
  display: flex; align-items: center; gap: 16px;
}
.avatar-preview-box {
  width: 64px; height: 64px; border-radius: 50%;
  border: 2px dashed #e8647a;
  display: flex; align-items: center; justify-content: center;
  overflow: hidden; flex-shrink: 0; background: #fef5f7;
}
.avatar-preview-img {
  width: 100%; height: 100%; object-fit: cover;
}
.avatar-preview-empty {
  font-size: 28px; color: #e8647a; font-weight: 600;
}
.avatar-upload-btn {
  padding: 8px 18px; font-size: 13px; color: #c41e3a;
  background: #fff; border: 1px solid #c41e3a; border-radius: 6px;
  cursor: pointer; transition: all 0.2s; font-weight: 600;
}
.avatar-upload-btn:hover { background: #fff5f7; }
.avatar-upload-btn.disabled { opacity: 0.5; pointer-events: none; }

/* ===== 响应式 ===== */
@media (max-width: 992px) {
  .page-wrap {
    grid-template-columns: 1fr;
  }
  .float-card {
    height: 360px;
  }
  .float-text-wrap h2 {
    font-size: 32px;
  }
}
@media (max-width: 576px) {
  .float-card {
    height: 300px;
  }
  .float-text-wrap h2 {
    font-size: 26px;
  }
  .form-grid {
    grid-template-columns: 1fr;
    gap: 0;
  }
  .form-grid .full-width {
    grid-column: 1;
  }
  .role-selector {
    flex-direction: column;
  }
  .region-selects {
    flex-direction: column;
    gap: 8px;
  }
}
</style>
