<script setup lang="ts">
import { ref, onMounted, nextTick, watch, computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { listConversations, getMessages, sendMessage, searchUser } from '@/api/chat'
import { listFriends, listPendingRequests, applyFriend, acceptFriend, rejectFriend } from '@/api/friend'
import type { ConversationVO, ChatMessageVO, FriendVO } from '@/types/api'

const authStore = useAuthStore()
const currentRole = computed(() => authStore.userInfo?.role || 'USER')
const isUser = computed(() => currentRole.value === 'USER')

// ==================== 状态 ====================
const conversations = ref<ConversationVO[]>([])
const messages = ref<ChatMessageVO[]>([])
const activeConv = ref<ConversationVO | null>(null)
const inputText = ref('')
const loading = ref(false)
const msgLoading = ref(false)
const pollTimer = ref<ReturnType<typeof setInterval> | null>(null)

// 新建对话
const showNewChat = ref(false)
const searchUsername = ref('')
const searchResult = ref<{ id: number; name: string; username: string; role: string; isFriend: number } | null>(null)
const searchError = ref('')

// 好友面板
const showFriendPanel = ref(false)
const friends = ref<FriendVO[]>([])
const pendingRequests = ref<FriendVO[]>([])
const friendLoading = ref(false)

// 消息容器引用
const msgContainer = ref<HTMLElement | null>(null)

// ==================== 加载会话列表 ====================
async function loadConversations() {
  loading.value = true
  try {
    const res = await listConversations()
    conversations.value = res.data
  } catch { conversations.value = [] }
  finally { loading.value = false }
}

// ==================== 选择会话 ====================
async function selectConversation(conv: ConversationVO) {
  activeConv.value = conv
  await loadMessages(conv.conversationId)
  startPolling()
}

async function loadMessages(conversationId: number) {
  msgLoading.value = true
  try {
    const res = await getMessages(conversationId)
    messages.value = res.data
    await nextTick()
    scrollToBottom()
  } catch { messages.value = [] }
  finally { msgLoading.value = false }
}

function scrollToBottom() {
  if (msgContainer.value) {
    msgContainer.value.scrollTop = msgContainer.value.scrollHeight
  }
}

// ==================== 轮询新消息 ====================
function startPolling() {
  stopPolling()
  pollTimer.value = setInterval(async () => {
    if (!activeConv.value) return
    try {
      const res = await getMessages(activeConv.value.conversationId)
      messages.value = res.data
    } catch { /* ignore */ }
  }, 3000)
}

function stopPolling() {
  if (pollTimer.value) {
    clearInterval(pollTimer.value)
    pollTimer.value = null
  }
}

// ==================== 发送消息 ====================
async function handleSend() {
  const text = inputText.value.trim()
  if (!text) return

  inputText.value = ''
  try {
    const dto: { conversationId?: number; receiverId?: number; receiverRole?: string; content: string } = { content: text }
    if (activeConv.value) {
      dto.conversationId = activeConv.value.conversationId
    }
    const res = await sendMessage(dto)
    // 如果是新会话，刷新会话列表
    if (!activeConv.value) {
      await loadConversations()
      const newConv = conversations.value.find(c => c.conversationId === res.data.conversationId)
      if (newConv) {
        activeConv.value = newConv
      }
    }
    messages.value.push(res.data)
    await nextTick()
    scrollToBottom()
  } catch (e: unknown) {
    alert(e instanceof Error ? e.message : '发送失败')
  }
}

// ==================== 新建对话 ====================
async function handleSearchUser() {
  const username = searchUsername.value.trim()
  if (!username) {
    searchError.value = '请输入用户名'
    searchResult.value = null
    return
  }
  searchError.value = ''
  try {
    const res = await searchUser(username)
    searchResult.value = res.data
  } catch {
    searchError.value = '未找到该用户'
    searchResult.value = null
  }
}

async function handleStartChat(receiverId: number, receiverRole: string) {
  try {
    const dto = { receiverId, receiverRole, content: '你好！' }
    const res = await sendMessage(dto)
    showNewChat.value = false
    searchUsername.value = ''
    searchResult.value = null
    await loadConversations()
    const newConv = conversations.value.find(c => c.conversationId === res.data.conversationId)
    if (newConv) {
      activeConv.value = newConv
      await loadMessages(newConv.conversationId)
      startPolling()
    }
  } catch (e: unknown) {
    alert(e instanceof Error ? e.message : '发起对话失败')
  }
}

/** 快捷联系管理员 */
async function handleChatWithAdmin() {
  try {
    const dto = { receiverId: 1, receiverRole: 'ADMIN', content: '你好，管理员！' }
    const res = await sendMessage(dto)
    showNewChat.value = false
    await loadConversations()
    const newConv = conversations.value.find(c => c.conversationId === res.data.conversationId)
    if (newConv) {
      activeConv.value = newConv
      await loadMessages(newConv.conversationId)
      startPolling()
    }
  } catch (e: unknown) {
    alert(e instanceof Error ? e.message : '发起对话失败')
  }
}

// ==================== 好友管理 ====================
async function loadFriendData() {
  friendLoading.value = true
  try {
    const [fRes, pRes] = await Promise.all([listFriends(), listPendingRequests()])
    friends.value = fRes.data
    pendingRequests.value = pRes.data
  } catch { /* ignore */ }
  finally { friendLoading.value = false }
}

async function handleApplyFriend() {
  if (!searchResult.value || searchResult.value.role !== 'USER') { alert('请先搜索用户'); return }
  try {
    await applyFriend(searchResult.value.id)
    alert('好友申请已发送！')
    searchUsername.value = ''
    searchResult.value = null
  } catch (e: unknown) {
    alert(e instanceof Error ? e.message : '申请失败')
  }
}

async function handleAcceptFriend(rel: FriendVO) {
  try {
    await acceptFriend(rel.relationId)
    await loadFriendData()
    alert('已接受好友申请')
  } catch (e: unknown) {
    alert(e instanceof Error ? e.message : '操作失败')
  }
}

async function handleRejectFriend(rel: FriendVO) {
  try {
    await rejectFriend(rel.relationId)
    await loadFriendData()
    alert('已拒绝好友申请')
  } catch (e: unknown) {
    alert(e instanceof Error ? e.message : '操作失败')
  }
}

// ==================== 生命周期 ====================
onMounted(() => {
  loadConversations()
})

// 监听activeConv变化（切换会话时停止旧轮询）
watch(activeConv, (newVal, oldVal) => {
  if (oldVal && newVal && oldVal.conversationId !== newVal.conversationId) {
    // 切换会话会自动在新函数里启动轮询
  }
})
</script>

<template>
  <section class="chat-layout">
    <!-- ===== 左侧：会话列表 ===== -->
    <aside class="chat-sidebar">
      <div class="sidebar-header">
        <h3>消息</h3>
        <button class="icon-btn" @click="showNewChat = !showNewChat" title="新建对话">+</button>
        <button v-if="isUser" class="icon-btn" @click="showFriendPanel = !showFriendPanel; showFriendPanel && loadFriendData()" title="好友管理">
          👥
        </button>
      </div>

      <!-- 新建对话面板 -->
      <div v-if="showNewChat" class="new-chat-box">
        <!-- 快捷联系管理员 -->
        <button v-if="currentRole !== 'ADMIN'" class="admin-quick-btn" @click="handleChatWithAdmin">
          📞 联系管理员
        </button>
        <div class="search-divider"><span>或搜索用户</span></div>
        <div class="search-row">
          <input v-model="searchUsername" type="text" placeholder="输入对方用户名" @keyup.enter="handleSearchUser" />
          <button class="btn-sm-primary" @click="handleSearchUser">搜索</button>
        </div>
        <p v-if="searchError" class="err-text">{{ searchError }}</p>
        <div v-if="searchResult" class="search-result">
          <div class="result-row">
            <span class="result-name">{{ searchResult.name }}</span>
            <span class="result-role">{{ searchResult.role === 'INHERITOR' ? '传承人' : searchResult.role === 'ADMIN' ? '管理员' : '用户' }}</span>
            <span class="result-id">@{{ searchResult.username }}</span>
          </div>
          <div class="result-actions">
            <button class="btn-sm-primary" @click="handleStartChat(searchResult.id, searchResult.role)">发起对话</button>
            <button
              v-if="isUser && searchResult.role === 'USER' && !searchResult.isFriend"
              class="btn-sm-outline"
              @click="handleApplyFriend"
            >加好友</button>
            <span v-if="isUser && searchResult.role === 'USER' && searchResult.isFriend" class="friend-badge">已是好友</span>
          </div>
        </div>
      </div>

      <!-- 会话列表 -->
      <div class="conv-list">
        <div
          v-for="conv in conversations"
          :key="conv.conversationId"
          class="conv-item"
          :class="{ active: activeConv?.conversationId === conv.conversationId }"
          @click="selectConversation(conv)"
        >
          <div class="conv-avatar">{{ conv.otherUserName?.charAt(0) || '?' }}</div>
          <div class="conv-info">
            <div class="conv-top">
              <span class="conv-name">{{ conv.otherUserName || '未知' }}</span>
              <span class="conv-time">{{ conv.lastMessageTime?.slice(0, 16)?.replace('T', ' ') || '' }}</span>
            </div>
            <div class="conv-bottom">
              <span class="conv-preview">{{ conv.lastMessage || '暂无消息' }}</span>
              <span v-if="conv.unreadCount > 0" class="conv-badge">{{ conv.unreadCount }}</span>
            </div>
          </div>
        </div>
        <div v-if="!loading && conversations.length === 0" class="empty-text">暂无会话</div>
      </div>
    </aside>

    <!-- ===== 右侧：消息区 ===== -->
    <main class="chat-main">
      <template v-if="activeConv">
        <div class="chat-header">
          <span class="chat-partner">{{ activeConv.otherUserName || '未知用户' }}</span>
          <span class="chat-role-tag">{{ activeConv.otherUserRole === 'INHERITOR' ? '传承人' : activeConv.otherUserRole === 'ADMIN' ? '管理员' : '用户' }}</span>
        </div>
        <div ref="msgContainer" class="chat-messages">
          <div v-if="msgLoading" class="empty-text">加载中…</div>
          <template v-else>
            <div
              v-for="msg in messages"
              :key="msg.id"
              class="msg-row"
              :class="msg.mine ? 'msg-mine' : 'msg-other'"
            >
              <div class="msg-bubble">
                <div class="msg-text">{{ msg.content }}</div>
                <div class="msg-time">{{ msg.createdAt?.slice(0, 16)?.replace('T', ' ') || '' }}</div>
              </div>
            </div>
            <div v-if="messages.length === 0" class="empty-text">暂无消息，发送第一条消息吧</div>
          </template>
        </div>
        <div class="chat-input-area">
          <input
            v-model="inputText"
            type="text"
            placeholder="输入消息..."
            @keyup.enter="handleSend"
          />
          <button class="btn-send" @click="handleSend" :disabled="!inputText.trim()">发送</button>
        </div>
      </template>
      <template v-else>
        <div class="no-chat">
          <div class="no-chat-icon">💬</div>
          <p>选择一个会话开始聊天</p>
          <p class="sub-text">点击左侧 + 号搜索用户发起新对话</p>
        </div>
      </template>
    </main>

    <!-- ===== 好友面板（仅用户） ===== -->
    <aside v-if="showFriendPanel && isUser" class="friend-panel">
      <div class="panel-header">
        <h3>好友</h3>
        <button class="icon-btn" @click="showFriendPanel = false">✕</button>
      </div>

      <!-- 好友列表 -->
      <div class="friend-section">
        <h4>我的好友 ({{ friends.length }})</h4>
        <div v-if="friendLoading" class="empty-text">加载中…</div>
        <div v-else>
          <div v-for="f in friends" :key="f.userId" class="friend-row">
            <span class="friend-name">{{ f.name }}</span>
            <span class="friend-id">ID: {{ f.userId }}</span>
            <button class="btn-sm-primary" @click="handleStartChat(f.userId, 'USER')">发消息</button>
          </div>
          <div v-if="friends.length === 0" class="empty-text">暂无好友</div>
        </div>
      </div>

      <!-- 待处理申请 -->
      <div class="friend-section">
        <h4>好友申请 ({{ pendingRequests.length }})</h4>
        <div v-if="friendLoading" class="empty-text">加载中…</div>
        <div v-else>
          <div v-for="r in pendingRequests" :key="r.userId" class="friend-row">
            <span class="friend-name">{{ r.name }}</span>
            <div class="request-actions">
              <button class="btn-sm-accept" @click="handleAcceptFriend(r)">接受</button>
              <button class="btn-sm-reject" @click="handleRejectFriend(r)">拒绝</button>
            </div>
          </div>
          <div v-if="pendingRequests.length === 0" class="empty-text">暂无待处理申请</div>
        </div>
      </div>
    </aside>
  </section>
</template>

<style scoped>
/* ==================== 布局 ==================== */
.chat-layout {
  display: flex;
  height: calc(100vh - 180px);
  min-height: 480px;
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 10px rgba(0,0,0,0.06);
}

/* ==================== 左侧边栏 ==================== */
.chat-sidebar {
  width: 300px;
  border-right: 1px solid #eee;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  background: #fafafa;
}
.sidebar-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px;
  border-bottom: 1px solid #eee;
}
.sidebar-header h3 {
  flex: 1;
  font-size: 16px;
  color: #c41e3a;
  margin: 0;
}
.icon-btn {
  width: 32px; height: 32px;
  border: 1px solid #ddd;
  background: #fff;
  border-radius: 6px;
  cursor: pointer;
  font-size: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: 0.2s;
}
.icon-btn:hover { background: #fff5f7; border-color: #c41e3a; }

/* 新建对话搜索 */
.new-chat-box {
  padding: 12px 16px;
  border-bottom: 1px solid #eee;
  background: #fff;
}
.search-row {
  display: flex; gap: 8px;
}
.search-row input {
  flex: 1; height: 36px; padding: 0 10px; border: 1px solid #ddd; border-radius: 6px; font-size: 13px; outline: none;
}
.search-row input:focus { border-color: #c41e3a; }
.search-result {
  margin-top: 10px; padding: 10px; background: #fff5f7; border-radius: 8px;
}
.result-row { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.result-name { font-weight: 600; font-size: 14px; }
.result-role { font-size: 12px; background: #ffe9ec; color: #c41e3a; padding: 1px 8px; border-radius: 8px; }
.result-id { font-size: 12px; color: #999; }
.result-actions { display: flex; gap: 8px; align-items: center; }
.friend-badge { font-size: 12px; color: #27ae60; }
.err-text { color: #dc3545; font-size: 12px; margin-top: 6px; }
.admin-quick-btn {
  width: 100%; padding: 10px; font-size: 14px; color: #fff; background: #c41e3a;
  border: none; border-radius: 8px; cursor: pointer; transition: 0.2s; font-weight: 600;
}
.admin-quick-btn:hover { background: #a8162b; }
.search-divider {
  display: flex; align-items: center; margin: 12px 0; gap: 10px;
}
.search-divider::before, .search-divider::after { content: ''; flex: 1; height: 1px; background: #e0e0e0; }
.search-divider span { font-size: 12px; color: #bbb; }

/* 会话列表 */
.conv-list { flex: 1; overflow-y: auto; }
.conv-item {
  display: flex; align-items: center; gap: 12px; padding: 14px 16px; cursor: pointer; transition: 0.15s;
  border-bottom: 1px solid #f5f5f5;
}
.conv-item:hover { background: #fef5f7; }
.conv-item.active { background: #fff5f7; border-left: 3px solid #c41e3a; }
.conv-avatar {
  width: 44px; height: 44px; border-radius: 50%;
  background: linear-gradient(135deg, #e8647a, #c41e3a); color: #fff;
  display: flex; align-items: center; justify-content: center;
  font-size: 18px; font-weight: bold; flex-shrink: 0;
}
.conv-info { flex: 1; min-width: 0; }
.conv-top { display: flex; justify-content: space-between; margin-bottom: 4px; }
.conv-name { font-size: 14px; font-weight: 600; color: #333; }
.conv-time { font-size: 11px; color: #bbb; }
.conv-bottom { display: flex; justify-content: space-between; align-items: center; }
.conv-preview { font-size: 12px; color: #999; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 160px; }
.conv-badge {
  background: #ff4d4f; color: #fff; font-size: 11px; padding: 1px 7px; border-radius: 10px; min-width: 18px; text-align: center;
}

/* ==================== 右侧消息区 ==================== */
.chat-main { flex: 1; display: flex; flex-direction: column; min-width: 0; }
.chat-header { display: flex; align-items: center; gap: 8px; padding: 14px 20px; border-bottom: 1px solid #eee; }
.chat-partner { font-size: 16px; font-weight: 600; color: #333; }
.chat-role-tag { font-size: 11px; background: #ffe9ec; color: #c41e3a; padding: 2px 8px; border-radius: 8px; }
.chat-messages { flex: 1; overflow-y: auto; padding: 20px; display: flex; flex-direction: column; gap: 14px; background: #f9f9f9; }

.msg-row { display: flex; max-width: 75%; }
.msg-mine { align-self: flex-end; }
.msg-other { align-self: flex-start; }
.msg-bubble { padding: 10px 14px; border-radius: 12px; position: relative; }
.msg-mine .msg-bubble { background: #c41e3a; color: #fff; border-bottom-right-radius: 4px; }
.msg-other .msg-bubble { background: #fff; color: #333; border-bottom-left-radius: 4px; box-shadow: 0 1px 3px rgba(0,0,0,0.08); }
.msg-text { font-size: 14px; line-height: 1.6; word-break: break-word; }
.msg-time { font-size: 10px; opacity: 0.7; margin-top: 4px; text-align: right; }

.chat-input-area { display: flex; gap: 10px; padding: 14px 20px; border-top: 1px solid #eee; background: #fff; }
.chat-input-area input { flex: 1; height: 42px; padding: 0 14px; border: 1px solid #e0e0e0; border-radius: 21px; font-size: 14px; outline: none; transition: 0.2s; }
.chat-input-area input:focus { border-color: #c41e3a; box-shadow: 0 0 0 3px rgba(196,30,58,0.08); }
.btn-send { padding: 0 24px; height: 42px; background: #c41e3a; color: #fff; border: none; border-radius: 21px; font-size: 14px; font-weight: bold; cursor: pointer; transition: 0.2s; }
.btn-send:hover:not(:disabled) { background: #a8162b; }
.btn-send:disabled { opacity: 0.5; cursor: not-allowed; }

/* 无会话状态 */
.no-chat { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; color: #bbb; }
.no-chat-icon { font-size: 64px; margin-bottom: 16px; }
.no-chat p { font-size: 16px; margin: 4px 0; }
.sub-text { font-size: 13px !important; color: #ddd !important; }

/* ==================== 好友面板 ==================== */
.friend-panel {
  width: 260px; border-left: 1px solid #eee; background: #fafafa; display: flex; flex-direction: column; flex-shrink: 0;
}
.panel-header {
  display: flex; align-items: center; padding: 16px; border-bottom: 1px solid #eee;
}
.panel-header h3 { flex: 1; font-size: 16px; color: #c41e3a; margin: 0; }
.friend-section { padding: 12px 16px; border-bottom: 1px solid #eee; overflow-y: auto; }
.friend-section h4 { font-size: 13px; color: #666; margin: 0 0 8px; }
.friend-row { display: flex; align-items: center; gap: 6px; padding: 8px 0; border-bottom: 1px solid #f5f5f5; flex-wrap: wrap; }
.friend-name { font-size: 13px; font-weight: 600; color: #333; }
.friend-id { font-size: 11px; color: #bbb; }
.request-actions { display: flex; gap: 4px; margin-left: auto; }

/* ==================== 通用按钮 ==================== */
.btn-sm-primary {
  padding: 4px 12px; font-size: 12px; color: #fff; background: #c41e3a; border: none; border-radius: 4px; cursor: pointer; transition: 0.2s;
}
.btn-sm-primary:hover { background: #a8162b; }
.btn-sm-outline {
  padding: 4px 12px; font-size: 12px; color: #c41e3a; background: #fff; border: 1px solid #c41e3a; border-radius: 4px; cursor: pointer; transition: 0.2s;
}
.btn-sm-outline:hover { background: #fff5f7; }
.btn-sm-accept {
  padding: 3px 10px; font-size: 11px; color: #fff; background: #27ae60; border: none; border-radius: 4px; cursor: pointer;
}
.btn-sm-accept:hover { background: #219a52; }
.btn-sm-reject {
  padding: 3px 10px; font-size: 11px; color: #fff; background: #dc3545; border: none; border-radius: 4px; cursor: pointer;
}
.btn-sm-reject:hover { background: #c82333; }

.empty-text { text-align: center; padding: 30px 16px; color: #ccc; font-size: 13px; }

/* ==================== 响应式 ==================== */
@media (max-width: 768px) {
  .chat-layout { flex-direction: column; height: auto; }
  .chat-sidebar { width: 100%; max-height: 250px; }
  .friend-panel { width: 100%; }
}
</style>
