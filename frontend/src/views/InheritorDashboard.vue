<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'
import OrderManager from './components/OrderManager.vue'
import SlotManager from './components/SlotManager.vue'
import KnowledgeManager from './components/KnowledgeManager.vue'
import ProjectManager from './components/ProjectManager.vue'
import ProductManager from './components/ProductManager.vue'
import ConsultationPanel from './components/ConsultationPanel.vue'
import InheritorProfile from './components/InheritorProfile.vue'

const authStore = useAuthStore()
const router = useRouter()

onMounted(() => {
  document.title = '非遗传承人工坊 — 码上非遗'
  loadOrders()
  loadMyProducts()
  loadProductOrders()
})

const userName = computed(() => authStore.userInfo?.name)

// 当前激活的 tab
type TabKey = 'orders' | 'products' | 'productOrders' | 'articles' | 'consultation' | 'profile'
const activeTab = ref<TabKey>('orders')

const tabs: { key: TabKey; label: string }[] = [
  { key: 'orders', label: '预约订单' },
  { key: 'slots', label: '时段管理' },
  { key: 'knowledge', label: '知识库' },
  { key: 'projects', label: '非遗项目' },
  { key: 'products', label: '文创产品' },
  { key: 'consultation', label: '咨询消息' },
  { key: 'profile', label: '入驻管理' },
]

function switchTab(key: TabKey) {
  activeTab.value = key
}

function handleLogout() {
  authStore.logout()
  router.push('/')
}

// ==================== 预约订单（从数据库读取） ====================
const orderList = ref<OrderVO[]>([])
const orderLoading = ref(false)

const statusMap: Record<number, { text: string; cls: string }> = {
  0: { text: '待确认', cls: 'status-pending' },
  1: { text: '已确认', cls: 'status-confirmed' },
  2: { text: '已完成', cls: 'status-completed' },
  3: { text: '已取消', cls: 'status-cancelled' },
}

async function loadOrders() {
  orderLoading.value = true
  try {
    const res = await listInheritorOrders()
    orderList.value = res.data
  } catch {
    // 接口不可用时保持空列表
  } finally {
    orderLoading.value = false
  }
}

async function handleOrderAction(order: OrderVO, action: 'confirm' | 'reject' | 'complete') {
  try {
    if (action === 'confirm') {
      await confirmOrder(order.id)
    } else if (action === 'reject') {
      await rejectOrder(order.id)
    } else if (action === 'complete') {
      await completeOrder(order.id)
    }
    await loadOrders()
  } catch {
    // 操作失败时不做状态变更
  }
}

// ==================== 商品管理 ====================
const myProductList = ref<ProductVO[]>([])
const productLoading = ref(false)
const showProductForm = ref(false)
const editingProduct = ref<ProductVO | null>(null)
const productForm = ref({
  name: '',
  description: '',
  price: 0,
  stock: 0,
  imageUrl: '',
  category: '',
})
const productFormMsg = ref('')
const productCategories = ['剪纸', '刺绣', '陶艺', '竹编', '综合']

async function loadMyProducts() {
  productLoading.value = true
  try {
    const res = await listMyProducts()
    myProductList.value = res.data
  } catch {
    // ignore
  } finally {
    productLoading.value = false
  }
}

function openAddProduct() {
  editingProduct.value = null
  productForm.value = { name: '', description: '', price: 0, stock: 0, imageUrl: '', category: '' }
  productFormMsg.value = ''
  showProductForm.value = true
}

function openEditProduct(product: ProductVO) {
  editingProduct.value = product
  productForm.value = {
    name: product.name,
    description: product.description || '',
    price: product.price,
    stock: product.stock,
    imageUrl: product.imageUrl || '',
    category: product.category || '',
  }
  productFormMsg.value = ''
  showProductForm.value = true
}

function closeProductForm() {
  showProductForm.value = false
  editingProduct.value = null
}

async function submitProductForm() {
  if (!productForm.value.name || !productForm.value.price) {
    productFormMsg.value = '商品名称和价格不能为空'
    return
  }
  productFormMsg.value = ''
  try {
    if (editingProduct.value) {
      await updateProduct(editingProduct.value.id, productForm.value)
    } else {
      await addProduct(productForm.value)
    }
    showProductForm.value = false
    await loadMyProducts()
  } catch {
    productFormMsg.value = '操作失败，请稍后重试'
  }
}

async function toggleProductStatus(product: ProductVO) {
  try {
    if (product.status === 1) {
      await offProduct(product.id)
    } else {
      await onProduct(product.id)
    }
    await loadMyProducts()
  } catch {
    // ignore
  }
}

// ==================== 商品订单 ====================
const productOrderList = ref<ProductOrderVO[]>([])
const productOrderLoading = ref(false)

const poStatusMap: Record<ProductOrderStatus, { text: string; cls: string }> = {
  0: { text: '待发货', cls: 'status-pending' },
  1: { text: '已发货', cls: 'status-shipped' },
  2: { text: '已完成', cls: 'status-completed' },
  3: { text: '已取消', cls: 'status-cancelled' },
}

async function loadProductOrders() {
  productOrderLoading.value = true
  try {
    const res = await listInheritorProductOrders()
    productOrderList.value = res.data
  } catch {
    // ignore
  } finally {
    productOrderLoading.value = false
  }
}

async function handlePOAction(order: ProductOrderVO, action: 'ship' | 'complete' | 'cancel') {
  try {
    if (action === 'ship') {
      await shipProductOrder(order.id)
    } else if (action === 'complete') {
      await completeProductOrder(order.id)
    } else if (action === 'cancel') {
      await cancelProductOrder(order.id)
    }
    await loadProductOrders()
    await loadMyProducts()
  } catch {
    // ignore
  }
}

// ==================== 发表文章 ====================
interface Article {
  id: number
  title: string
  category: string
  createTime: string
  status: 'published' | 'draft'
  viewCount: number
}

const articleForm = ref({
  title: '',
  category: '',
  content: '',
})

const articleCategories = ['剪纸', '刺绣', '陶艺', '竹编', '传承人故事', '技艺教学', '活动纪实']

const myArticles = ref<Article[]>([
  { id: 1, title: '苏绣入门：平针绣的基本技法', category: '技艺教学', createTime: '2026-07-12', status: 'published', viewCount: 2350 },
  { id: 2, title: '我的刺绣传承之路', category: '传承人故事', createTime: '2026-07-08', status: 'published', viewCount: 1820 },
  { id: 3, title: '校园刺绣工作坊活动回顾', category: '活动纪实', createTime: '2026-07-05', status: 'draft', viewCount: 0 },
])

const articleSubmitMsg = ref('')

function submitArticle() {
  if (!articleForm.value.title || !articleForm.value.category || !articleForm.value.content) {
    articleSubmitMsg.value = '请填写完整的文章信息'
    return
  }
  myArticles.value.unshift({
    id: Date.now(),
    title: articleForm.value.title,
    category: articleForm.value.category,
    createTime: new Date().toISOString().slice(0, 10),
    status: 'draft',
    viewCount: 0,
  })
  articleSubmitMsg.value = '文章已保存为草稿，可联系管理员发布'
  articleForm.value = { title: '', category: '', content: '' }
  setTimeout(() => { articleSubmitMsg.value = '' }, 3000)
}

// ==================== 咨询 mock 数据 ====================
interface Message {
  id: number
  from: 'user' | 'inheritor'
  userName: string
  content: string
  time: string
}

const messages = ref<Message[]>([
  { id: 1, from: 'user', userName: '张同学', content: '李老师您好，我想了解一下苏绣的入门工具需要准备哪些？', time: '2026-07-14 10:30' },
  { id: 2, from: 'inheritor', userName: '我', content: '你好！苏绣入门需要准备绣绷、绣布、绣线和绣针。建议先从简单的平针绣开始练习。', time: '2026-07-14 10:45' },
  { id: 3, from: 'user', userName: '张同学', content: '好的，谢谢老师！那绣线有什么推荐的吗？', time: '2026-07-14 11:00' },
  { id: 4, from: 'inheritor', userName: '我', content: '推荐使用蚕丝线，光泽度好，绣出来效果最佳。新手也可以用棉线先练习手感。', time: '2026-07-14 11:15' },
  { id: 5, from: 'user', userName: '刘同学', content: '老师，请问工坊这周末还有名额吗？我们社团想来体验。', time: '2026-07-15 09:20' },
])

const replyText = ref('')

function sendReply() {
  if (!replyText.value.trim()) return
  messages.value.push({
    id: Date.now(),
    from: 'inheritor',
    userName: '我',
    content: replyText.value.trim(),
    time: new Date().toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-'),
  })
  replyText.value = ''
}

// ==================== 入驻管理 ====================
const profile = ref({
  realName: '李秀英',
  honorTitle: '国家级非遗传承人 · 苏绣',
  phone: '138****6789',
  idNumber: '320***************',
  sex: '女',
  selfIntro: '从事苏绣创作四十余年，作品多次获国家级大奖，致力于将刺绣技艺带入校园，让更多年轻人了解和喜爱传统手工艺。',
  craftTypes: ['苏绣', '双面绣', '乱针绣'],
  address: '江苏省苏州市',
})

const profileEditMode = ref(false)
const profileForm = ref({ ...profile.value })
const profileSaveMsg = ref('')

function enterEditMode() {
  profileForm.value = { ...profile.value }
  profileEditMode.value = true
}

function cancelEdit() {
  profileEditMode.value = false
  profileSaveMsg.value = ''
}

function saveProfile() {
  profile.value = { ...profileForm.value }
  profileEditMode.value = false
  profileSaveMsg.value = '保存成功！'
  setTimeout(() => { profileSaveMsg.value = '' }, 3000)
}
</script>

<template>
  <!-- ==================== 顶部导航 ==================== -->
  <header class="page-header">
    <div class="header-left">
      <div class="logo" @click="router.push('/')">码上非遗</div>
      <span class="workspace-tag">传承人工坊</span>
    </div>

    <nav class="nav-tabs">
      <a
        v-for="tab in tabs"
        :key="tab.key"
        class="nav-tab"
        :class="{ active: activeTab === tab.key }"
        @click="switchTab(tab.key)"
      >
        {{ tab.label }}
      </a>
    </nav>

    <div class="header-right">
      <span class="inheritor-badge">非遗传承人</span>
      <span class="user-name">{{ userName }}</span>
      <button class="home-btn" @click="router.push('/')">返回首页</button>
      <button class="logout-btn" @click="handleLogout">退出</button>
    </div>
  </header>

  <!-- ==================== 主内容区 ==================== -->
  <main class="dashboard-main">
    <div class="dashboard-container">

      <!-- ========== 预约订单 ========== -->
      <section v-if="activeTab === 'orders'" class="tab-content">
        <div class="section-header">
          <h2>预约订单</h2>
          <div class="order-stats">
            <span class="stat-item">待确认 <strong>{{ orderList.filter(o => o.status === 0).length }}</strong></span>
            <span class="stat-item">已确认 <strong>{{ orderList.filter(o => o.status === 1).length }}</strong></span>
            <span class="stat-item">已完成 <strong>{{ orderList.filter(o => o.status === 2).length }}</strong></span>
          </div>
        </div>

        <div class="table-wrap">
          <table class="order-table">
            <thead>
              <tr>
                <th>用户</th>
                <th>联系电话</th>
                <th>预约日期</th>
                <th>时段</th>
                <th>人数</th>
                <th>备注</th>
                <th>状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="order in orderList" :key="order.id">
                <td>{{ order.userName }}</td>
                <td>{{ order.phone }}</td>
                <td>{{ order.appointmentDate }}</td>
                <td>{{ order.timeSlot }}</td>
                <td>{{ order.peopleCount }}人</td>
                <td class="remark-cell">{{ order.remark }}</td>
                <td>
                  <span class="status-tag" :class="statusMap[order.status]?.cls">
                    {{ statusMap[order.status]?.text }}
                  </span>
                </td>
                <td class="action-cell">
                  <template v-if="order.status === 0">
                    <button class="btn-confirm" @click="handleOrderAction(order, 'confirm')">确认</button>
                    <button class="btn-reject" @click="handleOrderAction(order, 'reject')">拒绝</button>
                  </template>
                  <template v-else-if="order.status === 1">
                    <button class="btn-confirm" @click="handleOrderAction(order, 'complete')">完成</button>
                  </template>
                  <span v-else class="no-action">—</span>
                </td>
              </tr>
            </tbody>
          </table>
          <div v-if="orderLoading" class="empty-state">加载中…</div>
          <div v-else-if="orderList.length === 0" class="empty-state">暂无预约订单</div>
        </div>
      </section>

      <!-- ========== 商品管理 ========== -->
      <section v-if="activeTab === 'products'" class="tab-content">
        <div class="section-header">
          <h2>商品管理</h2>
          <button class="btn-primary" @click="openAddProduct">上架新商品</button>
        </div>

        <!-- 商品表单弹窗 -->
        <div v-if="showProductForm" class="modal-overlay" @click.self="closeProductForm">
          <div class="modal-content">
            <div class="modal-header">
              <h3>{{ editingProduct ? '编辑商品' : '上架新商品' }}</h3>
              <button class="modal-close" @click="closeProductForm">✕</button>
            </div>
            <div class="modal-body">
              <div class="form-group">
                <label>商品名称 *</label>
                <input v-model="productForm.name" type="text" class="form-input" placeholder="请输入商品名称" />
              </div>
              <div class="form-group">
                <label>分类</label>
                <select v-model="productForm.category" class="form-input">
                  <option value="" disabled>请选择分类</option>
                  <option v-for="cat in productCategories" :key="cat" :value="cat">{{ cat }}</option>
                </select>
              </div>
              <div class="form-row">
                <div class="form-group flex-1">
                  <label>价格 (¥) *</label>
                  <input v-model.number="productForm.price" type="number" step="0.01" min="0" class="form-input" placeholder="0.00" />
                </div>
                <div class="form-group flex-1">
                  <label>库存</label>
                  <input v-model.number="productForm.stock" type="number" min="0" class="form-input" placeholder="0" />
                </div>
              </div>
              <div class="form-group">
                <label>商品描述</label>
                <textarea v-model="productForm.description" class="form-textarea" rows="3" placeholder="请输入商品描述"></textarea>
              </div>
              <div v-if="productFormMsg" class="order-msg">{{ productFormMsg }}</div>
            </div>
            <div class="modal-footer">
              <button class="btn-outline" @click="closeProductForm">取消</button>
              <button class="btn-primary" @click="submitProductForm">
                {{ editingProduct ? '保存修改' : '立即上架' }}
              </button>
            </div>
          </div>
        </div>

        <div class="table-wrap">
          <table class="order-table">
            <thead>
              <tr>
                <th>商品名称</th>
                <th>分类</th>
                <th>价格</th>
                <th>库存</th>
                <th>状态</th>
                <th>创建时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="product in myProductList" :key="product.id">
                <td>{{ product.name }}</td>
                <td><span class="category-tag">{{ product.category }}</span></td>
                <td>¥{{ product.price.toFixed(2) }}</td>
                <td>{{ product.stock }}</td>
                <td>
                  <span class="status-tag" :class="product.status === 1 ? 'status-confirmed' : 'status-cancelled'">
                    {{ product.status === 1 ? '上架中' : '已下架' }}
                  </span>
                </td>
                <td>{{ product.createTime?.slice(0, 10) }}</td>
                <td class="action-cell">
                  <button class="btn-confirm" @click="openEditProduct(product)">编辑</button>
                  <button
                    v-if="product.status === 1"
                    class="btn-reject"
                    @click="toggleProductStatus(product)"
                  >下架</button>
                  <button
                    v-else
                    class="btn-confirm"
                    style="background: #17a2b8"
                    @click="toggleProductStatus(product)"
                  >上架</button>
                </td>
              </tr>
            </tbody>
          </table>
          <div v-if="productLoading" class="empty-state">加载中…</div>
          <div v-else-if="myProductList.length === 0" class="empty-state">暂无商品，点击"上架新商品"开始</div>
        </div>
      </section>

      <!-- ========== 商品订单 ========== -->
      <section v-if="activeTab === 'productOrders'" class="tab-content">
        <div class="section-header">
          <h2>商品订单</h2>
          <div class="order-stats">
            <span class="stat-item">待发货 <strong>{{ productOrderList.filter(o => o.status === 0).length }}</strong></span>
            <span class="stat-item">已发货 <strong>{{ productOrderList.filter(o => o.status === 1).length }}</strong></span>
            <span class="stat-item">已完成 <strong>{{ productOrderList.filter(o => o.status === 2).length }}</strong></span>
          </div>
        </div>

        <div class="table-wrap">
          <table class="order-table">
            <thead>
              <tr>
                <th>用户</th>
                <th>商品</th>
                <th>单价</th>
                <th>数量</th>
                <th>金额</th>
                <th>联系电话</th>
                <th>地址</th>
                <th>状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="order in productOrderList" :key="order.id">
                <td>{{ order.userName }}</td>
                <td>{{ order.productName }}</td>
                <td>¥{{ order.price.toFixed(2) }}</td>
                <td>{{ order.quantity }}</td>
                <td>¥{{ order.totalAmount.toFixed(2) }}</td>
                <td>{{ order.phone }}</td>
                <td class="remark-cell">{{ order.address }}</td>
                <td>
                  <span class="status-tag" :class="poStatusMap[order.status]?.cls">
                    {{ poStatusMap[order.status]?.text }}
                  </span>
                </td>
                <td class="action-cell">
                  <template v-if="order.status === 0">
                    <button class="btn-confirm" style="background: #17a2b8" @click="handlePOAction(order, 'ship')">发货</button>
                    <button class="btn-reject" @click="handlePOAction(order, 'cancel')">取消</button>
                  </template>
                  <template v-else-if="order.status === 1">
                    <button class="btn-confirm" @click="handlePOAction(order, 'complete')">完成</button>
                  </template>
                  <span v-else class="no-action">—</span>
                </td>
              </tr>
            </tbody>
          </table>
          <div v-if="productOrderLoading" class="empty-state">加载中…</div>
          <div v-else-if="productOrderList.length === 0" class="empty-state">暂无商品订单</div>
        </div>
      </section>

      <!-- ========== 发表文章 ========== -->
      <section v-if="activeTab === 'articles'" class="tab-content">
        <div class="section-header">
          <h2>发表文章</h2>
        </div>

        <div class="article-editor">
          <div class="form-row">
            <div class="form-group flex-1">
              <label>文章标题</label>
              <input
                v-model="articleForm.title"
                type="text"
                placeholder="请输入文章标题"
                class="form-input"
              />
            </div>
            <div class="form-group" style="width: 200px">
              <label>分类</label>
              <select v-model="articleForm.category" class="form-input">
                <option value="" disabled>请选择分类</option>
                <option v-for="cat in articleCategories" :key="cat" :value="cat">{{ cat }}</option>
              </select>
            </div>
          </div>
          <div class="form-group">
            <label>文章内容</label>
            <textarea
              v-model="articleForm.content"
              class="form-textarea"
              rows="8"
              placeholder="请输入文章内容，支持换行..."
            ></textarea>
          </div>
          <div class="editor-actions">
            <button class="btn-primary" @click="submitArticle">保存草稿</button>
            <span v-if="articleSubmitMsg" class="submit-msg" :class="{ error: articleSubmitMsg.includes('请') }">
              {{ articleSubmitMsg }}
            </span>
          </div>
        </div>

        <div class="section-header" style="margin-top: 40px">
          <h2>我的文章</h2>
        </div>
        <div class="table-wrap">
          <table class="order-table">
            <thead>
              <tr>
                <th>标题</th>
                <th>分类</th>
                <th>发布时间</th>
                <th>状态</th>
                <th>浏览量</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="article in myArticles" :key="article.id">
                <td class="article-title-cell">{{ article.title }}</td>
                <td><span class="category-tag">{{ article.category }}</span></td>
                <td>{{ article.createTime }}</td>
                <td>
                  <span class="article-status" :class="article.status === 'published' ? 'published' : 'draft'">
                    {{ article.status === 'published' ? '已发布' : '草稿' }}
                  </span>
                </td>
                <td>{{ article.viewCount }}</td>
              </tr>
            </tbody>
          </table>
          <div v-if="myArticles.length === 0" class="empty-state">暂无文章</div>
        </div>
      </section>

      <!-- ========== 咨询 ========== -->
      <section v-if="activeTab === 'consultation'" class="tab-content">
        <div class="section-header">
          <h2>用户咨询</h2>
        </div>

        <div class="chat-panel">
          <div class="chat-messages">
            <div
              v-for="msg in messages"
              :key="msg.id"
              class="chat-msg"
              :class="msg.from === 'inheritor' ? 'msg-self' : 'msg-other'"
            >
              <div class="msg-avatar">
                {{ msg.from === 'inheritor' ? '我' : msg.userName.charAt(0) }}
              </div>
              <div class="msg-body">
                <div class="msg-header">
                  <span class="msg-sender">{{ msg.userName }}</span>
                  <span class="msg-time">{{ msg.time }}</span>
                </div>
                <div class="msg-content">{{ msg.content }}</div>
              </div>
            </div>
            <div v-if="messages.length === 0" class="empty-state">暂无咨询消息</div>
          </div>

          <div class="chat-reply">
            <textarea
              v-model="replyText"
              class="reply-input"
              rows="3"
              placeholder="输入回复内容..."
            ></textarea>
            <button class="btn-primary" @click="sendReply" :disabled="!replyText.trim()">发送回复</button>
          </div>
        </div>
      </section>

      <!-- ========== 入驻管理 ========== -->
      <section v-if="activeTab === 'profile'" class="tab-content">
        <div class="section-header">
          <h2>入驻管理</h2>
          <button v-if="!profileEditMode" class="btn-outline" @click="enterEditMode">编辑信息</button>
        </div>

        <div class="profile-card">
          <!-- 头像区域 -->
          <div class="profile-avatar-section">
            <div class="profile-avatar-lg"></div>
            <div class="profile-name-role">
              <h3 v-if="!profileEditMode">{{ profile.realName }}</h3>
              <input v-else v-model="profileForm.realName" type="text" class="form-input" style="font-size: 22px; font-weight: bold" />
              <p v-if="!profileEditMode" class="profile-honor">{{ profile.honorTitle }}</p>
              <input v-else v-model="profileForm.honorTitle" type="text" class="form-input" style="margin-top: 6px" placeholder="荣誉称号" />
            </div>
          </div>

          <!-- 信息网格 -->
          <div class="profile-detail-grid">
            <div class="detail-item">
              <label>联系电话</label>
              <span v-if="!profileEditMode">{{ profile.phone }}</span>
              <input v-else v-model="profileForm.phone" type="text" class="form-input" />
            </div>
            <div class="detail-item">
              <label>性别</label>
              <span v-if="!profileEditMode">{{ profile.sex }}</span>
              <select v-else v-model="profileForm.sex" class="form-input">
                <option value="男">男</option>
                <option value="女">女</option>
              </select>
            </div>
            <div class="detail-item">
              <label>身份证号</label>
              <span v-if="!profileEditMode">{{ profile.idNumber }}</span>
              <input v-else v-model="profileForm.idNumber" type="text" class="form-input" />
            </div>
            <div class="detail-item">
              <label>所在地</label>
              <span v-if="!profileEditMode">{{ profile.address }}</span>
              <input v-else v-model="profileForm.address" type="text" class="form-input" />
            </div>
          </div>

          <!-- 技艺类型 -->
          <div class="detail-item" style="margin-top: 16px">
            <label>擅长技艺</label>
            <div v-if="!profileEditMode" class="craft-tags">
              <span v-for="craft in profile.craftTypes" :key="craft" class="craft-tag">{{ craft }}</span>
            </div>
            <input
              v-else
              type="text"
              class="form-input"
              placeholder="多个技艺用逗号分隔"
              :value="profileForm.craftTypes.join('，')"
              @input="(e: Event) => { const t = e.target as HTMLInputElement; profileForm.craftTypes = t.value.split(/[,，]/).map((s: string) => s.trim()).filter(Boolean) }"
            />
          </div>

          <!-- 个人简介 -->
          <div class="detail-item" style="margin-top: 16px">
            <label>个人简介</label>
            <p v-if="!profileEditMode" class="profile-intro">{{ profile.selfIntro }}</p>
            <textarea v-else v-model="profileForm.selfIntro" class="form-textarea" rows="4" placeholder="请输入个人简介"></textarea>
          </div>

          <!-- 编辑模式按钮 -->
          <div v-if="profileEditMode" class="profile-edit-actions">
            <button class="btn-primary" @click="saveProfile">保存修改</button>
            <button class="btn-outline" @click="cancelEdit">取消</button>
            <span v-if="profileSaveMsg" class="save-msg">{{ profileSaveMsg }}</span>
          </div>
        </div>
      </section>

    </div>
  </main>

  <!-- ==================== 页脚 ==================== -->
  <footer class="page-footer">
    <p>校园非遗与文创传承人平台 © 2026 码上非遗 — 传承人工坊</p>
  </footer>
</template>

<style scoped>
/* ==================== 顶部导航 ==================== */
.page-header {
  background-color: #c41e3a;
  color: #fff;
  padding: 0 5%;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: sticky;
  top: 0;
  z-index: 100;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 14px;
}
.logo {
  font-size: 20px;
  font-weight: bold;
  cursor: pointer;
  white-space: nowrap;
}
.workspace-tag {
  font-size: 12px;
  background: #ffd700;
  color: #8b4513;
  padding: 2px 10px;
  border-radius: 10px;
  font-weight: bold;
  white-space: nowrap;
}

.nav-tabs {
  display: flex;
  align-items: center;
  gap: 6px;
  overflow-x: auto;
}
.nav-tab {
  color: rgba(255,255,255,0.8);
  text-decoration: none;
  font-size: 14px;
  cursor: pointer;
  padding: 8px 16px;
  border-radius: 6px;
  transition: all 0.2s;
  white-space: nowrap;
}
.nav-tab:hover {
  color: #fff;
  background: rgba(255,255,255,0.12);
}
.nav-tab.active {
  color: #c41e3a;
  background: #fff;
  font-weight: bold;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}
.inheritor-badge {
  font-size: 11px;
  background: #ffd700;
  color: #8b4513;
  padding: 2px 10px;
  border-radius: 10px;
  font-weight: bold;
  white-space: nowrap;
}
.user-name {
  font-size: 14px;
  white-space: nowrap;
}
.home-btn {
  padding: 6px 12px;
  font-size: 12px;
  color: #fff;
  background: rgba(255,255,255,0.15);
  border: 1px solid rgba(255,255,255,0.4);
  border-radius: 4px;
  cursor: pointer;
  transition: 0.2s;
  white-space: nowrap;
}
.home-btn:hover {
  background: rgba(255,255,255,0.3);
}
.logout-btn {
  padding: 6px 14px;
  font-size: 12px;
  color: #fff;
  background: rgba(255,255,255,0.15);
  border: 1px solid rgba(255,255,255,0.4);
  border-radius: 4px;
  cursor: pointer;
  transition: 0.2s;
  white-space: nowrap;
}
.logout-btn:hover {
  background: rgba(255,255,255,0.3);
}

/* ==================== 主内容区 ==================== */
.dashboard-main {
  min-height: calc(100vh - 64px - 60px);
  background: #fcf4e8;
  padding: 30px 5%;
}
.dashboard-container {
  max-width: 1200px;
  margin: 0 auto;
}

/* ==================== Section 通用 ==================== */
.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}
.section-header h2 {
  font-size: 22px;
  color: #c41e3a;
  padding-left: 14px;
  border-left: 4px solid #c41e3a;
}

/* ==================== 订单统计 ==================== */
.order-stats {
  display: flex;
  gap: 20px;
}
.stat-item {
  font-size: 14px;
  color: #666;
}
.stat-item strong {
  color: #c41e3a;
  font-size: 18px;
  margin-left: 4px;
}

/* ==================== 表格 ==================== */
.table-wrap {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 10px rgba(0,0,0,0.06);
}
.order-table {
  width: 100%;
  border-collapse: collapse;
}
.order-table th,
.order-table td {
  padding: 14px 16px;
  text-align: left;
  font-size: 14px;
}
.order-table thead {
  background: #fff5f7;
}
.order-table th {
  color: #c41e3a;
  font-weight: 600;
  font-size: 13px;
}
.order-table tbody tr {
  border-bottom: 1px solid #f5f5f5;
  transition: 0.2s;
}
.order-table tbody tr:hover {
  background: #fefafa;
}
.remark-cell {
  max-width: 160px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.action-cell {
  white-space: nowrap;
}
.no-action {
  color: #ccc;
}

/* 状态标签 */
.status-tag {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}
.status-pending {
  background: #fff3cd;
  color: #856404;
}
.status-confirmed {
  background: #d4edda;
  color: #155724;
}
.status-completed {
  background: #e2e3e5;
  color: #383d41;
}
.status-cancelled {
  background: #f8d7da;
  color: #721c24;
}
.status-shipped {
  background: #cce5ff;
  color: #004085;
}

/* 操作按钮 */
.btn-confirm {
  padding: 5px 14px;
  font-size: 12px;
  color: #fff;
  background: #28a745;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  margin-right: 6px;
  transition: 0.2s;
}
.btn-confirm:hover {
  background: #218838;
}
.btn-reject {
  padding: 5px 14px;
  font-size: 12px;
  color: #fff;
  background: #dc3545;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: 0.2s;
}
.btn-reject:hover {
  background: #c82333;
}

/* ==================== 文章编辑器 ==================== */
.article-editor {
  background: #fff;
  border-radius: 12px;
  padding: 28px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.06);
}
.form-row {
  display: flex;
  gap: 20px;
  margin-bottom: 18px;
}
.form-group {
  margin-bottom: 18px;
}
.form-group label {
  display: block;
  font-size: 14px;
  color: #444;
  margin-bottom: 6px;
  font-weight: 600;
}
.flex-1 {
  flex: 1;
}
.form-input {
  width: 100%;
  height: 42px;
  padding: 0 14px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  color: #333;
  outline: none;
  transition: 0.25s;
  background: #fafafa;
  box-sizing: border-box;
}
.form-input:focus {
  border-color: #c41e3a;
  box-shadow: 0 0 0 3px rgba(196,30,58,0.08);
  background: #fff;
}
.form-textarea {
  width: 100%;
  padding: 14px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  color: #333;
  outline: none;
  resize: vertical;
  transition: 0.25s;
  background: #fafafa;
  box-sizing: border-box;
  font-family: inherit;
  line-height: 1.8;
}
.form-textarea:focus {
  border-color: #c41e3a;
  box-shadow: 0 0 0 3px rgba(196,30,58,0.08);
  background: #fff;
}
.editor-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}
.submit-msg {
  font-size: 13px;
  color: #28a745;
}
.submit-msg.error {
  color: #dc3545;
}

.category-tag {
  display: inline-block;
  padding: 2px 10px;
  background: #ffe9ec;
  color: #c41e3a;
  border-radius: 10px;
  font-size: 12px;
}
.article-title-cell {
  max-width: 280px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.article-status {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 600;
}
.article-status.published {
  background: #d4edda;
  color: #155724;
}
.article-status.draft {
  background: #e2e3e5;
  color: #383d41;
}

/* 通用按钮 */
.btn-primary {
  padding: 10px 24px;
  font-size: 14px;
  color: #fff;
  background: #c41e3a;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-weight: bold;
  transition: 0.2s;
}
.btn-primary:hover {
  background: #a8162b;
}
.btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.btn-outline {
  padding: 8px 20px;
  font-size: 13px;
  color: #c41e3a;
  background: #fff;
  border: 1px solid #c41e3a;
  border-radius: 6px;
  cursor: pointer;
  transition: 0.2s;
}
.btn-outline:hover {
  background: #fff5f7;
}

/* ==================== 咨询面板 ==================== */
.chat-panel {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 10px rgba(0,0,0,0.06);
  display: flex;
  flex-direction: column;
  min-height: 500px;
}
.chat-messages {
  flex: 1;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  max-height: 480px;
  overflow-y: auto;
}
.chat-msg {
  display: flex;
  gap: 12px;
  max-width: 80%;
}
.msg-self {
  align-self: flex-end;
  flex-direction: row-reverse;
}
.msg-other {
  align-self: flex-start;
}
.msg-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: bold;
  color: #fff;
  flex-shrink: 0;
}
.msg-other .msg-avatar {
  background: linear-gradient(135deg, #e8647a, #c41e3a);
}
.msg-self .msg-avatar {
  background: linear-gradient(135deg, #ffd700, #e8b800);
  color: #8b4513;
}
.msg-body {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.msg-self .msg-body {
  align-items: flex-end;
}
.msg-header {
  display: flex;
  gap: 10px;
  align-items: center;
}
.msg-sender {
  font-size: 13px;
  font-weight: 600;
  color: #444;
}
.msg-time {
  font-size: 11px;
  color: #999;
}
.msg-content {
  padding: 12px 16px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.7;
}
.msg-other .msg-content {
  background: #fff5f7;
  color: #333;
  border-bottom-left-radius: 4px;
}
.msg-self .msg-content {
  background: #c41e3a;
  color: #fff;
  border-bottom-right-radius: 4px;
}

.chat-reply {
  display: flex;
  gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid #eee;
  background: #fafafa;
  align-items: flex-end;
}
.reply-input {
  flex: 1;
  padding: 12px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
  resize: none;
  font-family: inherit;
  line-height: 1.6;
  transition: 0.25s;
}
.reply-input:focus {
  border-color: #c41e3a;
  box-shadow: 0 0 0 3px rgba(196,30,58,0.08);
}

/* ==================== 入驻管理 ==================== */
.profile-card {
  background: #fff;
  border-radius: 12px;
  padding: 32px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.06);
}
.profile-avatar-section {
  display: flex;
  align-items: center;
  gap: 24px;
  margin-bottom: 28px;
  padding-bottom: 24px;
  border-bottom: 1px solid #f0f0f0;
}
.profile-avatar-lg {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  background: linear-gradient(135deg, #e8647a, #c41e3a);
  border: 4px solid #fff5f7;
  flex-shrink: 0;
}
.profile-name-role h3 {
  font-size: 24px;
  color: #222;
  margin-bottom: 6px;
}
.profile-honor {
  font-size: 15px;
  color: #c41e3a;
  font-weight: 600;
}
.profile-detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}
.detail-item label {
  display: block;
  font-size: 13px;
  color: #999;
  margin-bottom: 6px;
}
.detail-item span,
.detail-item p {
  font-size: 15px;
  color: #333;
}
.craft-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.craft-tag {
  display: inline-block;
  padding: 4px 14px;
  background: #ffe9ec;
  color: #c41e3a;
  border-radius: 16px;
  font-size: 13px;
  font-weight: 600;
}
.profile-intro {
  font-size: 14px;
  color: #555;
  line-height: 1.8;
}
.profile-edit-actions {
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  gap: 12px;
}
.save-msg {
  font-size: 13px;
  color: #28a745;
}

/* ==================== 空状态 ==================== */
.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #999;
  font-size: 14px;
}

/* ==================== 页脚 ==================== */
.page-footer {
  background-color: #c41e3a;
  color: #fff;
  text-align: center;
  padding: 20px 5%;
  font-size: 14px;
}

/* ==================== 商品管理弹窗 ==================== */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.45);
  z-index: 300;
  display: flex;
  align-items: center;
  justify-content: center;
}
.modal-content {
  background: #fff;
  border-radius: 12px;
  width: 480px;
  max-width: 90vw;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 8px 30px rgba(0,0,0,0.2);
}
.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #eee;
}
.modal-header h3 { font-size: 18px; color: #c41e3a; }
.modal-close {
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  color: #999;
}
.modal-close:hover { color: #333; }
.modal-body {
  padding: 24px;
  overflow-y: auto;
  flex: 1;
}
.modal-footer {
  padding: 16px 24px;
  border-top: 1px solid #eee;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
.order-msg {
  margin-top: 12px;
  padding: 10px;
  border-radius: 6px;
  background: #fff3cd;
  color: #856404;
  font-size: 13px;
  text-align: center;
}

/* ==================== 响应式 ==================== */
@media (max-width: 992px) {
  .nav-tabs { gap: 2px; }
  .nav-tab { padding: 8px 10px; font-size: 13px; }
}
@media (max-width: 768px) {
  .page-header {
    flex-wrap: wrap;
    height: auto;
    padding: 10px 16px;
    gap: 10px;
  }
  .nav-tabs {
    order: 3;
    width: 100%;
    justify-content: center;
  }
  .inheritor-badge { display: none; }
}
</style>
