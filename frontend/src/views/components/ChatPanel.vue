<script setup lang="ts">
import { ref, onMounted, nextTick, watch, computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { listConversations, getMessages, sendMessage, searchUser } from '@/api/chat'
import { listFriends, listPendingRequests, listMyRequests, applyFriend, acceptFriend, rejectFriend } from '@/api/friend'
import { listPublicProfiles } from '@/api/profile'
import { listAllUsers } from '@/api/auth'
import type { ConversationVO, ChatMessageVO, FriendVO } from '@/types/api'
import { unreadCount } from '@/composables/useUnread'
import { notifyRecipient } from '@/api/notify'
import { tryNotify } from '@/composables/useNotifyCooldown'

const authStore = useAuthStore()
const currentRole = computed(() => authStore.userInfo?.role || 'USER')
/** 可加好友的角色：普通用户 + 非遗传承人，管理员不可加好友 */
const canFriend = computed(() => currentRole.value === 'USER' || currentRole.value === 'INHERITOR')
/** 角色中文映射 */
function roleLabel(r: string) {
  return r === 'INHERITOR' ? '传承人' : r === 'ADMIN' ? '管理员' : '用户'
}

const conversations = ref<ConversationVO[]>([])
const messages = ref<ChatMessageVO[]>([])
const activeConv = ref<ConversationVO | null>(null)
const inputText = ref('')
const loading = ref(false)
const msgLoading = ref(false)
const pollTimer = ref<ReturnType<typeof setInterval> | null>(null)
const msgContainer = ref<HTMLElement | null>(null)

// 新建对话
const showNewChat = ref(false)
const searchUsername = ref('')
const searchResults = ref<{ id: number; name: string; username: string; role: string; isFriend: number }[]>([])
const searchError = ref('')
const inheritorCache = ref<{ employeeId: number; name: string }[]>([])

// 预加载传承人列表（用于角色校验）
async function loadInheritorCache() {
  try { inheritorCache.value = (await listPublicProfiles()).data || [] } catch { /* */ }
}
onMounted(() => { loadConversations(); loadInheritorCache() })

// 好友
const showFriendPanel = ref(false)
const friends = ref<FriendVO[]>([])
const pendingRequests = ref<FriendVO[]>([])
const myRequests = ref<FriendVO[]>([])
const friendLoading = ref(false)

// ==================== 会话 ====================
async function loadConversations() {
  loading.value = true
  try { conversations.value = (await listConversations()).data }
  catch { conversations.value = [] }
  finally { loading.value = false }
}

async function selectConversation(conv: ConversationVO) {
  activeConv.value = conv
  await loadMessages(conv.conversationId)
  // 清除当前会话未读：服务端 loadMessages 已标记已读，刷新列表获取最新计数
  await loadConversations()
  if (conv.unreadCount > 0) {
    const updated = conversations.value.find(c => c.conversationId === conv.conversationId)
    if (updated) {
      unreadCount.value = Math.max(0, unreadCount.value - conv.unreadCount + (updated.unreadCount || 0))
      conv.unreadCount = updated.unreadCount || 0
    } else {
      unreadCount.value = Math.max(0, unreadCount.value - conv.unreadCount)
      conv.unreadCount = 0
    }
  }
  startPolling()
}

async function loadMessages(cid: number) {
  msgLoading.value = true
  try { messages.value = (await getMessages(cid)).data; await nextTick(); scrollToBottom() }
  catch { messages.value = [] }
  finally { msgLoading.value = false }
}

function scrollToBottom() {
  if (msgContainer.value) msgContainer.value.scrollTop = msgContainer.value.scrollHeight
}

function startPolling() {
  stopPolling()
  pollTimer.value = setInterval(async () => {
    if (!activeConv.value) return
    try { messages.value = (await getMessages(activeConv.value.conversationId)).data }
    catch { /* */ }
  }, 3000)
}
function stopPolling() { if (pollTimer.value) { clearInterval(pollTimer.value); pollTimer.value = null } }

// ==================== 发送消息 ====================
async function handleSend() {
  const text = inputText.value.trim()
  if (!text) return; inputText.value = ''
  try {
    const dto: any = { content: text }
    if (activeConv.value) dto.conversationId = activeConv.value.conversationId
    const res = await sendMessage(dto)
    if (!activeConv.value) {
      await loadConversations()
      const nc = conversations.value.find(c => c.conversationId === res.data.conversationId)
      if (nc) activeConv.value = nc
    }
    messages.value.push(res.data)
    // 邮件通知（冷却60秒，避免频繁发送）
    const cid = activeConv.value?.conversationId
    if (cid && tryNotify(cid)) {
      notifyRecipient(cid).catch(() => {})
    }
    await nextTick(); scrollToBottom()
  } catch (e: unknown) { alert(e instanceof Error ? e.message : '发送失败') }
}

// ==================== 新建对话 ====================
async function handleSearchUser() {
  const u = searchUsername.value.trim()
  if (!u) { searchError.value = '请输入关键词搜索'; searchResults.value = []; return }
  searchError.value = ''
  // 先加载好友列表（用于交叉校验 isFriend）
  if (canFriend.value) await loadFriendData()
  try {
    const res = (await searchUser(u)).data
    const raw = Array.isArray(res) ? res : (res ? [res] : [])
    // 去重 + 修正角色
    const out: typeof searchResults.value = []
    const seenNames = new Set<string>()
    for (const r of raw) {
      // 用公开传承人列表 + 启发式规则校验真实角色
      let role = r.role
      const isInh = inheritorCache.value.some(ih =>
        // 不使用 employeeId 比对（跨角色 id 可能碰撞）

        (r.name && ih.name === r.name) ||
        (r.username && ih.name === r.username)
      ) || (r.username || '').startsWith('inheritor_')
      if (isInh) role = 'INHERITOR'
      if (!role) role = 'USER'

      // 去重：按 name 去重（USER 和 INHERITOR 表可能返回同一人不同 id）
      const dedupKey = r.name || r.username || ''
      if (dedupKey && seenNames.has(dedupKey)) continue
      if (dedupKey) seenNames.add(dedupKey)

      out.push({
        ...r, role,
        isFriend: canFriend.value && role === currentRole.value
          ? (friends.value.some(f => f.userId === r.id && f.role === role) ? 1 : 0)
          : 0,
      })
    }
    // 二次合并：同一 name 只保留传承人版本
    const merged: typeof out = []
    for (const r of out) {
      const exist = merged.find(m => m.name && r.name && m.name === r.name)
      if (exist) {
        if (r.role === 'INHERITOR') { Object.assign(exist, r) }
      } else {
        merged.push(r)
      }
    }
    searchResults.value = merged
    if (merged.length === 0) searchError.value = '未找到匹配用户'
  } catch {
    // API 失败 → 加载全量数据本地模糊搜索
    await loadConversations()
    if (canFriend.value) await loadFriendData()
    const kw = u.toLowerCase()
    const myRole = currentRole.value
    const local: any[] = []
    const seen = new Set<string>()

    function addResult(r: any) {
      // 校正角色
      if (!r.role || r.role === 'USER') {
        if (inheritorCache.value.some(ih => // 不使用 employeeId 比对（跨角色 id 可能碰撞）
 (r.name && ih.name === r.name) || (r.username && ih.name === r.username))
          || (r.username || '').startsWith('inheritor_')) r.role = 'INHERITOR'
      }
      if (!r.role) r.role = 'USER'
      // 按 name 去重（同一人不同来源只保留第一条）
      const dedupKey = r.name || r.username || ''
      if (dedupKey && seen.has(dedupKey)) return
      if (dedupKey) seen.add(dedupKey)
      r.isFriend = canFriend.value && r.role === myRole
        ? (friends.value.some(f => f.userId === r.id && f.role === r.role) ? 1 : 0)
        : 0
      local.push(r)
    }

    // 来源1: 好友列表
    for (const f of friends.value) {
      if (f.name?.toLowerCase().includes(kw) || f.userName?.toLowerCase().includes(kw)) {
        addResult({ id: f.userId, name: f.name, username: f.userName, role: f.role })
      }
    }
    // 来源2: 会话列表
    for (const c of conversations.value) {
      if (c.otherUserName?.toLowerCase().includes(kw)) {
        addResult({ id: c.otherUserId, name: c.otherUserName, username: c.otherUserName, role: c.otherUserRole || 'USER' })
      }
    }
    // 来源3: 全量用户表
    try {
      const users = (await listAllUsers()).data || []
      for (const uu of users) {
        if (uu.name?.toLowerCase().includes(kw) || uu.username?.toLowerCase().includes(kw)) {
          addResult({ id: uu.id, name: uu.name, username: uu.username, role: 'USER' })
        }
      }
    } catch { /* */ }
    // 来源4: 全量传承人
    try {
      const inheritors = (await listPublicProfiles()).data || []
      for (const ih of inheritors) {
        if (ih.name?.toLowerCase().includes(kw)) {
          addResult({ id: ih.employeeId, name: ih.name, username: ih.name, role: 'INHERITOR' })
        }
      }
    } catch { /* */ }

    searchResults.value = local.slice(0, 10)
    if (local.length === 0) searchError.value = '未找到匹配用户'
  }
}

async function handleStartChat(rid: number, rrole: string) {
  try {
    const res = await sendMessage({ receiverId: rid, receiverRole: rrole, content: '你好！' })
    showNewChat.value = false; searchUsername.value = ''; searchResults.value = []
    await loadConversations()
    const nc = conversations.value.find(c => c.conversationId === res.data.conversationId)
    if (nc) { activeConv.value = nc; await loadMessages(nc.conversationId); startPolling() }
  } catch (e: unknown) { alert(e instanceof Error ? e.message : '发起对话失败') }
}

async function handleChatWithAdmin() {
  try {
    const res = await sendMessage({ receiverId: 1, receiverRole: 'ADMIN', content: '你好，管理员！' })
    showNewChat.value = false
    await loadConversations()
    const nc = conversations.value.find(c => c.conversationId === res.data.conversationId)
    if (nc) { activeConv.value = nc; await loadMessages(nc.conversationId); startPolling() }
  } catch (e: unknown) { alert(e instanceof Error ? e.message : '发起对话失败') }
}

// ==================== 好友 ====================
async function loadFriendData() {
  friendLoading.value = true
  try {
    const [f, p, mResult] = await Promise.all([
      listFriends(), listPendingRequests(),
      listMyRequests().then(r => r.data).catch(e => { console.error('listMyRequests 失败:', e); return [] }),
    ])
    const myRole = currentRole.value
    friends.value = f.data.filter(x => x.role === myRole)
    pendingRequests.value = p.data.filter(x => x.role === myRole)
    myRequests.value = Array.isArray(mResult) ? mResult : []
    console.log('好友数据: 好友', friends.value.length, '待处理', pendingRequests.value.length, '我的申请', myRequests.value.length, 'myRole', myRole, '申请原始', mResult)
  }
  catch { /* */ } finally { friendLoading.value = false }
}

async function handleApplyFriend(target: { id: number; name: string; username: string; role: string; isFriend: number }) {
  if (target.role !== currentRole.value) { alert('仅可向同角色用户发送好友申请'); return }
  try {
    await applyFriend(target.id, target.role)
    alert(`已向 ${target.name} 发送好友申请！`)
    searchUsername.value = ''; searchResults.value = []
    // 刷新好友数据并自动打开好友面板查看申请状态
    await loadFriendData()
    showFriendPanel.value = true
  } catch (e: unknown) { alert(e instanceof Error ? e.message : '申请失败') }
}

async function handleAcceptFriend(r: FriendVO) {
  try { await acceptFriend(r.relationId); await loadFriendData(); alert('已接受好友申请') }
  catch (e: unknown) { alert(e instanceof Error ? e.message : '操作失败') }
}
async function handleRejectFriend(r: FriendVO) {
  try { await rejectFriend(r.relationId); await loadFriendData(); alert('已拒绝好友申请') }
  catch (e: unknown) { alert(e instanceof Error ? e.message : '操作失败') }
}

</script>

<template>
  <section class="chat-layout">
    <!-- 回纹装饰边 -->
    <div class="deco-border top"></div>

    <!-- 左侧：会话列表 -->
    <aside class="chat-sidebar">
      <div class="sidebar-header">
        <h3>💬 消息</h3>
        <button class="icon-btn" @click="showNewChat = !showNewChat" title="新建对话">+</button>
        <button v-if="canFriend" class="icon-btn" @click="showFriendPanel = !showFriendPanel; showFriendPanel && loadFriendData()">👥</button>
      </div>

      <div v-if="showNewChat" class="new-chat-box">
        <button v-if="currentRole !== 'ADMIN'" class="admin-quick-btn" @click="handleChatWithAdmin">📞 联系管理员</button>
        <div class="search-divider"><span>或搜索用户</span></div>
        <div class="search-row">
          <input v-model="searchUsername" type="text" placeholder="输入姓名或用户名模糊搜索" @keyup.enter="handleSearchUser" />
          <button class="btn-sm-primary" @click="handleSearchUser">搜索</button>
        </div>
        <p v-if="searchError" class="err-text">{{ searchError }}</p>
        <div v-if="searchResults.length > 0" class="search-results-list">
          <div v-for="(sr, idx) in searchResults" :key="sr.id + sr.role" class="search-result fade-in" :style="{ animationDelay: idx * 0.05 + 's' }">
            <div class="result-row">
              <span class="result-name">{{ sr.name }}</span>
              <span class="result-role">{{ roleLabel(sr.role) }}</span>
              <span class="result-id">@{{ sr.username }}</span>
            </div>
            <div class="result-actions">
              <!-- 管理员 -->
              <button v-if="sr.role === 'ADMIN'" class="btn-sm-primary" @click="handleStartChat(sr.id, sr.role)">联系管理员</button>
              <!-- 同角色 + 好友：对话 -->
              <button v-else-if="canFriend && sr.role === currentRole && sr.isFriend" class="btn-sm-primary" @click="handleStartChat(sr.id, sr.role)">发起对话</button>
              <!-- 同角色 + 非好友：加好友 -->
              <button v-else-if="canFriend && sr.role === currentRole && !sr.isFriend" class="btn-sm-outline" @click="handleApplyFriend(sr)">加好友</button>
              <!-- 跨角色：可直接对话（不加好友） -->
              <button v-else-if="canFriend" class="btn-sm-primary" @click="handleStartChat(sr.id, sr.role)">发起对话</button>
            </div>
          </div>
        </div>
      </div>

      <div class="conv-list">
        <div
          v-for="conv in conversations" :key="conv.conversationId"
          class="conv-item slide-in"
          :class="{ active: activeConv?.conversationId === conv.conversationId, unread: conv.unreadCount > 0 }"
          @click="selectConversation(conv)"
        >
          <div class="conv-avatar">{{ conv.otherUserName?.charAt(0) || '?' }}</div>
          <div class="conv-info">
            <div class="conv-top">
              <span class="conv-name">{{ conv.otherUserName || '未知' }}</span>
              <span class="conv-time">{{ conv.lastMessageTime?.slice(0,16)?.replace('T',' ') || '' }}</span>
            </div>
            <div class="conv-bottom">
              <span class="conv-preview">{{ conv.lastMessage || '暂无消息' }}</span>
              <span v-if="conv.unreadCount > 0" class="conv-badge pulse">{{ conv.unreadCount }}</span>
            </div>
          </div>
        </div>
        <div v-if="!loading && conversations.length === 0" class="empty-text">暂无会话</div>
      </div>
    </aside>

    <!-- 右侧：消息区 -->
    <main class="chat-main">
      <template v-if="activeConv">
        <div class="chat-header">
          <span class="chat-partner">{{ activeConv.otherUserName || '未知用户' }}</span>
          <span class="chat-role-tag">{{ roleLabel(activeConv.otherUserRole) }}</span>
        </div>
        <div ref="msgContainer" class="chat-messages">
          <div v-if="msgLoading" class="empty-text">加载中…</div>
          <template v-else>
            <div v-for="msg in messages" :key="msg.id" class="msg-row msg-pop" :class="msg.mine ? 'msg-mine' : 'msg-other'">
              <div class="msg-bubble">
                <div class="msg-text">{{ msg.content }}</div>
                <div class="msg-time">{{ msg.createdAt?.slice(0,16)?.replace('T',' ') || '' }}</div>
              </div>
            </div>
            <div v-if="messages.length === 0" class="empty-text">暂无消息，发送第一条消息吧</div>
          </template>
        </div>
        <div class="chat-input-area">
          <input v-model="inputText" type="text" placeholder="输入消息..." @keyup.enter="handleSend" />
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

    <!-- 好友面板 -->
    <aside v-if="showFriendPanel && canFriend" class="friend-panel">
      <div class="panel-header"><h3>好友</h3><button class="icon-btn" @click="showFriendPanel=false">✕</button></div>
      <div class="friend-section">
        <h4>我的好友 ({{ friends.length }})</h4>
        <div v-if="friendLoading" class="empty-text">加载中…</div>
        <div v-else>
          <div v-for="f in friends" :key="f.userId" class="friend-row">
            <span class="friend-name">{{ f.name }}</span><span class="friend-id">ID:{{ f.userId }}</span>
            <button class="btn-sm-primary" @click="handleStartChat(f.userId,'USER')">发消息</button>
          </div>
          <div v-if="friends.length===0" class="empty-text">暂无好友</div>
        </div>
      </div>
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
          <div v-if="pendingRequests.length===0" class="empty-text">暂无待处理申请</div>
        </div>
      </div>
      <!-- 我发出的申请 -->
      <div class="friend-section">
        <h4>我的申请 ({{ myRequests.length }})</h4>
        <div v-if="friendLoading" class="empty-text">加载中…</div>
        <div v-else>
          <div v-for="r in myRequests" :key="r.userId" class="friend-row">
            <span class="friend-name">{{ r.name }}</span>
            <span class="request-status" :class="r.status === 1 ? 's-accepted' : 's-pending'">
              {{ r.status === 1 ? '已接受' : r.status === 2 ? '已拒绝' : '等待中' }}
            </span>
          </div>
          <div v-if="myRequests.length===0" class="empty-text">暂无发出的申请</div>
        </div>
      </div>
    </aside>
  </section>
</template>

<style scoped>
/* ==================== 布局 ==================== */
.chat-layout {
  display: flex; height: calc(100vh - 180px); min-height: 480px;
  background: linear-gradient(180deg, #fff 0%, #fefaf7 100%);
  border-radius: 16px; overflow: hidden;
  box-shadow: 0 4px 24px rgba(139,26,43,0.08);
  position: relative; border: 1px solid rgba(196,30,58,0.06);
}

/* 回纹顶边装饰 */
.deco-border.top {
  position: absolute; top: 0; left: 0; right: 0; height: 4px; z-index: 10;
  background: repeating-linear-gradient(90deg, #c41e3a 0px, #c41e3a 6px, transparent 6px, transparent 10px, #e6b85c 10px, #e6b85c 14px, transparent 14px, transparent 18px);
  opacity: 0.5;
}

/* ==================== 侧边栏 ==================== */
.chat-sidebar { width: 310px; border-right: 1px solid rgba(196,30,58,0.08); display: flex; flex-direction: column; flex-shrink: 0; background: #fefaf7; }
.sidebar-header { display: flex; align-items: center; gap: 8px; padding: 18px 16px; border-bottom: 1px solid rgba(196,30,58,0.06); }
.sidebar-header h3 { flex: 1; font-size: 17px; color: #c41e3a; margin: 0; letter-spacing: 2px; }
.icon-btn { width: 34px; height: 34px; border: 1px solid rgba(196,30,58,0.15); background: #fff; border-radius: 8px; cursor: pointer; font-size: 18px; display: flex; align-items: center; justify-content: center; transition: all 0.2s; }
.icon-btn:hover { background: #fff5f7; border-color: #c41e3a; transform: scale(1.05); }

.new-chat-box { padding: 14px 16px; border-bottom: 1px solid rgba(196,30,58,0.06); background: #fff; }
.search-row { display: flex; gap: 8px; }
.search-row input { flex: 1; height: 38px; padding: 0 12px; border: 1px solid rgba(196,30,58,0.12); border-radius: 8px; font-size: 13px; outline: none; background: #fefaf7; transition: 0.2s; }
.search-row input:focus { border-color: #c41e3a; box-shadow: 0 0 0 3px rgba(196,30,58,0.06); }
.search-result { margin-top: 10px; padding: 12px; background: linear-gradient(135deg, #fff5f7, #fff); border-radius: 10px; border: 1px solid rgba(196,30,58,0.08); }
.result-row { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; }
.result-name { font-weight: 600; font-size: 14px; color: #333; }
.result-role { font-size: 11px; background: #ffe9ec; color: #c41e3a; padding: 2px 8px; border-radius: 8px; }
.result-id { font-size: 12px; color: #999; }
.result-actions { display: flex; gap: 8px; align-items: center; }
.friend-badge { font-size: 12px; color: #27ae60; }
.err-text { color: #dc3545; font-size: 12px; margin-top: 6px; }

/* 搜索结果滚动列表 */
.search-results-list {
  max-height: 240px; overflow-y: auto; margin-top: 4px;
}
.search-results-list::-webkit-scrollbar { width: 5px; }
.search-results-list::-webkit-scrollbar-track { background: transparent; }
.search-results-list::-webkit-scrollbar-thumb { background: rgba(196,30,58,0.15); border-radius: 3px; }
.search-results-list::-webkit-scrollbar-thumb:hover { background: rgba(196,30,58,0.3); }

/* 搜索项过渡动画 */
.search-result {
  animation: resultSlideIn 0.35s ease-out both;
}
@keyframes resultSlideIn {
  0% { opacity: 0; transform: translateY(-8px) scale(0.97); }
  100% { opacity: 1; transform: translateY(0) scale(1); }
}
.admin-quick-btn { width: 100%; padding: 10px; font-size: 14px; color: #fff; background: linear-gradient(135deg, #c41e3a, #d43030); border: none; border-radius: 8px; cursor: pointer; font-weight: 600; transition: 0.2s; }
.admin-quick-btn:hover { background: linear-gradient(135deg, #a8162b, #b82028); transform: translateY(-1px); }
.search-divider { display: flex; align-items: center; margin: 12px 0; gap: 10px; }
.search-divider::before, .search-divider::after { content: ''; flex: 1; height: 1px; background: rgba(196,30,58,0.08); }
.search-divider span { font-size: 12px; color: #bbb; }

/* 会话列表 */
.conv-list { flex: 1; overflow-y: auto; scroll-behavior: smooth; }
.conv-list::-webkit-scrollbar { width: 4px; }
.conv-list::-webkit-scrollbar-thumb { background: rgba(196,30,58,0.12); border-radius: 2px; }
.conv-item { display: flex; align-items: center; gap: 12px; padding: 14px 16px; cursor: pointer; transition: all 0.2s; border-bottom: 1px solid rgba(196,30,58,0.04); }
.conv-item:hover { background: linear-gradient(90deg, rgba(196,30,58,0.03), transparent); }
.conv-item.active { background: linear-gradient(90deg, rgba(196,30,58,0.06), transparent); border-left: 3px solid #c41e3a; padding-left: 13px; }
.conv-item.unread { background: linear-gradient(90deg, rgba(255,215,0,0.06), transparent); }
.conv-avatar { width: 46px; height: 46px; border-radius: 50%; background: linear-gradient(135deg, #e8647a, #c41e3a); color: #fff; display: flex; align-items: center; justify-content: center; font-size: 18px; font-weight: bold; flex-shrink: 0; box-shadow: 0 2px 8px rgba(196,30,58,0.15); }
.conv-info { flex: 1; min-width: 0; }
.conv-top { display: flex; justify-content: space-between; margin-bottom: 4px; }
.conv-name { font-size: 14px; font-weight: 600; color: #333; }
.conv-time { font-size: 11px; color: #bbb; }
.conv-bottom { display: flex; justify-content: space-between; align-items: center; }
.conv-preview { font-size: 12px; color: #999; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 160px; }
.conv-badge { background: #ff4d4f; color: #fff; font-size: 11px; padding: 2px 8px; border-radius: 10px; min-width: 20px; text-align: center; font-weight: 600; box-shadow: 0 2px 6px rgba(255,77,79,0.3); }

/* ==================== 消息区 ==================== */
.chat-main { flex: 1; display: flex; flex-direction: column; min-width: 0; }
.chat-header { display: flex; align-items: center; gap: 10px; padding: 16px 22px; border-bottom: 1px solid rgba(196,30,58,0.06); background: #fff; }
.chat-partner { font-size: 16px; font-weight: 600; color: #333; letter-spacing: 1px; }
.chat-role-tag { font-size: 11px; background: #ffe9ec; color: #c41e3a; padding: 3px 10px; border-radius: 8px; font-weight: 600; }
.chat-messages { flex: 1; overflow-y: auto; padding: 22px; display: flex; flex-direction: column; gap: 14px; background:
  repeating-linear-gradient(0deg, transparent, transparent 40px, rgba(196,30,58,0.008) 40px, rgba(196,30,58,0.008) 41px);
}
.chat-messages::-webkit-scrollbar { width: 4px; }
.chat-messages::-webkit-scrollbar-thumb { background: rgba(196,30,58,0.12); border-radius: 2px; }

.msg-row { display: flex; max-width: 72%; animation: msgSlideIn 0.3s ease-out; }
@keyframes msgSlideIn { 0% { opacity: 0; transform: translateY(12px); } 100% { opacity: 1; transform: translateY(0); } }
.msg-mine { align-self: flex-end; }
.msg-other { align-self: flex-start; }
.msg-bubble { padding: 10px 16px; border-radius: 14px; position: relative; }
.msg-mine .msg-bubble { background: linear-gradient(135deg, #c41e3a, #d43030); color: #fff; border-bottom-right-radius: 4px; box-shadow: 0 2px 8px rgba(196,30,58,0.2); }
.msg-other .msg-bubble { background: #fff; color: #333; border-bottom-left-radius: 4px; box-shadow: 0 1px 3px rgba(0,0,0,0.06); border: 1px solid rgba(0,0,0,0.04); }
.msg-text { font-size: 14px; line-height: 1.7; word-break: break-word; }
.msg-time { font-size: 10px; opacity: 0.65; margin-top: 4px; text-align: right; }

.chat-input-area { display: flex; gap: 10px; padding: 14px 22px; border-top: 1px solid rgba(196,30,58,0.06); background: #fff; }
.chat-input-area input { flex: 1; height: 44px; padding: 0 16px; border: 1px solid rgba(196,30,58,0.12); border-radius: 22px; font-size: 14px; outline: none; transition: 0.2s; background: #fefaf7; }
.chat-input-area input:focus { border-color: #c41e3a; box-shadow: 0 0 0 3px rgba(196,30,58,0.06); }
.btn-send { padding: 0 28px; height: 44px; background: linear-gradient(135deg, #c41e3a, #d43030); color: #fff; border: none; border-radius: 22px; font-size: 14px; font-weight: 600; cursor: pointer; transition: 0.2s; letter-spacing: 1px; }
.btn-send:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 4px 12px rgba(196,30,58,0.3); }
.btn-send:disabled { opacity: 0.4; cursor: not-allowed; }

.no-chat { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; color: #ccc; }
.no-chat-icon { font-size: 72px; margin-bottom: 16px; filter: grayscale(0.4); }
.no-chat p { font-size: 16px; margin: 4px 0; color: #bbb; }
.sub-text { font-size: 13px !important; color: #ddd !important; }

/* ==================== 好友面板 ==================== */
.friend-panel { width: 260px; border-left: 1px solid rgba(196,30,58,0.06); background: #fefaf7; display: flex; flex-direction: column; flex-shrink: 0; }
.panel-header { display: flex; align-items: center; padding: 16px; border-bottom: 1px solid rgba(196,30,58,0.06); }
.panel-header h3 { flex: 1; font-size: 15px; color: #c41e3a; margin: 0; }
.friend-section { padding: 14px 16px; border-bottom: 1px solid rgba(196,30,58,0.04); overflow-y: auto; }
.friend-section h4 { font-size: 12px; color: #888; margin: 0 0 8px; }
.friend-row { display: flex; align-items: center; gap: 6px; padding: 8px 0; border-bottom: 1px solid rgba(0,0,0,0.03); flex-wrap: wrap; }
.friend-name { font-size: 13px; font-weight: 600; color: #333; }
.friend-id { font-size: 11px; color: #bbb; }
.request-actions { display: flex; gap: 4px; margin-left: auto; }

/* ==================== 动画 ==================== */
.fade-in { animation: fadeIn 0.3s ease-out; }
@keyframes fadeIn { 0% { opacity: 0; transform: translateY(-6px); } 100% { opacity: 1; transform: translateY(0); } }
.slide-in { animation: slideIn 0.35s ease-out; }
@keyframes slideIn { 0% { opacity: 0; transform: translateX(-20px); } 100% { opacity: 1; transform: translateX(0); } }
.pulse { animation: pulse 1.5s ease-in-out infinite; }
@keyframes pulse { 0%,100% { transform: scale(1); } 50% { transform: scale(1.1); } }
.msg-pop { }

/* ==================== 通用按钮 ==================== */
.btn-sm-primary { padding: 5px 14px; font-size: 12px; color: #fff; background: #c41e3a; border: none; border-radius: 6px; cursor: pointer; transition: 0.2s; font-weight: 600; }
.btn-sm-primary:hover { background: #a8162b; }
.btn-sm-outline { padding: 5px 14px; font-size: 12px; color: #c41e3a; background: #fff; border: 1px solid #c41e3a; border-radius: 6px; cursor: pointer; transition: 0.2s; }
.btn-sm-outline:hover { background: #fff5f7; }
.btn-sm-accept { padding: 4px 12px; font-size: 11px; color: #fff; background: #27ae60; border: none; border-radius: 4px; cursor: pointer; }
.btn-sm-reject { padding: 4px 12px; font-size: 11px; color: #fff; background: #dc3545; border: none; border-radius: 4px; cursor: pointer; }
.empty-text { text-align: center; padding: 30px 16px; color: #ccc; font-size: 13px; }
.request-status { font-size: 11px; padding: 2px 8px; border-radius: 6px; font-weight: 600; }
.s-pending { background: #fff3cd; color: #856404; }
.s-accepted { background: #d4edda; color: #155724; }

@media (max-width: 768px) {
  .chat-layout { flex-direction: column; height: auto; }
  .chat-sidebar { width: 100%; max-height: 250px; }
  .friend-panel { width: 100%; }
}
</style>
