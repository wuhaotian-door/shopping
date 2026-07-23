<script setup lang="ts">
import { ref, nextTick, onMounted, onUnmounted } from 'vue'
import { aiChatStream, aiSessions, aiSessionDelete, type StreamEvent } from '@/api/ai'

interface Message {
  id: number; role: 'user' | 'ai' | 'tool' | 'system'
  content: string; toolName?: string; toolArgs?: string
}

interface Session { id: string; title: string; created_at: string; msg_count: number }

const sessions = ref<Session[]>([])
const activeSid = ref('session-' + Date.now())
const msgs = ref<Message[]>([])
const input = ref('')
const streaming = ref(false)
const msgBox = ref<HTMLElement | null>(null)
let mid = 0, abortFn: (() => void) | null = null

async function loadSessions() {
  try { sessions.value = (await aiSessions()).sessions } catch { /* */ }
}

function newSession() {
  if (streaming.value) return
  activeSid.value = 'session-' + Date.now(); msgs.value = []
}

async function switchSession(sid: string) {
  if (streaming.value) return; activeSid.value = sid
  // 从后端加载消息历史
  try {
    const resp = await fetch(`/ai/sessions`); const data = await resp.json()
    const s = data.sessions.find((s: any) => s.id === sid)
    msgs.value = [] // 简化：后端目前不返回完整消息内容，仅显示空
  } catch { msgs.value = [] }
}

async function delSession(sid: string) {
  if (streaming.value) return
  await aiSessionDelete(sid)
  if (activeSid.value === sid) newSession()
  loadSessions()
}

function addMsg(role: Message['role'], content: string, extra?: Partial<Message>): Message {
  const m: Message = { id: ++mid, role, content, ...extra }
  msgs.value.push(m)
  return m
}

function scroll() { nextTick(() => { if (msgBox.value) msgBox.value.scrollTop = msgBox.value.scrollHeight }) }

async function send() {
  const q = input.value.trim(); if (!q || streaming.value) return
  input.value = ''; addMsg('user', q); scroll()

  streaming.value = true
  const aiMsg = addMsg('ai', '')
  let buf = ''

  abortFn = aiChatStream(q, activeSid.value,
    (e: StreamEvent) => {
      if (e.type === 'token' && e.content) { buf += e.content; aiMsg.content = buf; scroll() }
      else if (e.type === 'tool_start') { addMsg('tool', '', { toolName: e.name, toolArgs: JSON.stringify(e.input) }); scroll() }
    },
    (err) => { aiMsg.content = `错误: ${err.message}`; streaming.value = false },
    () => { streaming.value = false; aiMsg.content ||= '(无回答)'; loadSessions() }
  )
}

onMounted(loadSessions)
onUnmounted(() => abortFn?.())
</script>

<template>
  <div class="rag-layout">
    <!-- 侧栏 -->
    <aside class="sidebar">
      <div class="side-hd"><h3 @click="newSession">🤖 AI 助手</h3><button class="btn-icon" @click="newSession">＋</button></div>
      <div class="session-list">
        <div v-for="s in sessions" :key="s.id" class="s-item" :class="{ active: s.id === activeSid }" @click="switchSession(s.id)">
          <span class="s-title">{{ s.title || '新会话' }}</span>
          <button class="s-del" @click.stop="delSession(s.id)">×</button>
        </div>
        <div v-if="!sessions.length" class="empty">暂无会话</div>
      </div>
    </aside>

    <!-- 主区 -->
    <main class="main">
      <div class="main-hd">💬 智能对话 · AI 自主查资料、推荐内容</div>
      <div ref="msgBox" class="msgs">
        <div v-if="!msgs.length" class="welcome">
          <div class="w-icon">🤖</div>
          <h2>非遗 AI 助手</h2>
          <p>问我任何问题，我会自动查资料、推荐内容</p>
          <div class="quick">
            <button v-for="q in ['什么是景泰蓝？', '苏州有哪些刺绣传承人？', '推荐适合新手的非遗']" :key="q"
              class="q-btn" @click="input = q; send()">{{ q }}</button>
          </div>
        </div>
        <div v-for="m in msgs" :key="m.id" class="msg-row" :class="'m-' + m.role">
          <div v-if="m.role === 'user'" class="bubble bu">{{ m.content }}</div>
          <div v-else-if="m.role === 'ai'" class="bubble ba"><div class="ac">{{ m.content || (streaming ? '思考中…' : '') }}</div></div>
          <div v-else-if="m.role === 'tool'" class="tool-note">🔧 {{ m.toolName }}({{ m.toolArgs?.slice(0, 60) }})</div>
          <div v-else class="sys-note">{{ m.content }}</div>
        </div>
      </div>
      <div class="bar">
        <input v-model="input" @keyup.enter="send" :disabled="streaming" placeholder="输入你的问题…">
        <button v-if="!streaming" @click="send" :disabled="!input.trim()">发送</button>
        <button v-else class="btn-stop" @click="abortFn?.(); streaming = false">停止</button>
      </div>
    </main>
  </div>
</template>

<style scoped>
.rag-layout { display:flex; height:calc(100vh - 116px); min-height:520px; background:linear-gradient(180deg,#fff,#fefaf7); border-radius:16px; overflow:hidden; box-shadow:0 4px 24px rgba(196,30,58,0.08); border:1px solid rgba(196,30,58,0.06); }
.sidebar { width:240px; border-right:1px solid rgba(196,30,58,0.08); display:flex; flex-direction:column; background:#fefaf7; flex-shrink:0; }
.side-hd { display:flex; align-items:center; justify-content:space-between; padding:16px; border-bottom:1px solid rgba(196,30,58,0.06); }
.side-hd h3 { font-size:15px; color:#c41e3a; cursor:pointer; margin:0; }
.btn-icon { width:30px; height:30px; border:1px solid rgba(196,30,58,0.15); background:#fff; border-radius:8px; font-size:18px; cursor:pointer; color:#c41e3a; }
.session-list { flex:1; overflow-y:auto; }
.s-item { display:flex; align-items:center; padding:12px 16px; cursor:pointer; border-bottom:1px solid rgba(196,30,58,0.04); gap:8px; }
.s-item:hover { background:rgba(196,30,58,0.03); }
.s-item.active { background:rgba(196,30,58,0.06); border-left:3px solid #c41e3a; }
.s-title { flex:1; font-size:13px; color:#555; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
.s-item.active .s-title { color:#c41e3a; font-weight:600; }
.s-del { width:18px; height:18px; border:none; background:transparent; color:#ccc; cursor:pointer; border-radius:50%; }
.s-del:hover { color:#c41e3a; background:rgba(196,30,58,0.1); }

.main { flex:1; display:flex; flex-direction:column; min-width:0; }
.main-hd { padding:14px 20px; border-bottom:1px solid rgba(196,30,58,0.06); font-size:14px; font-weight:600; color:#c41e3a; background:#fff; }
.msgs { flex:1; overflow-y:auto; padding:20px; display:flex; flex-direction:column; gap:12px; }
.welcome { text-align:center; margin:auto; padding:40px 20px; }
.w-icon { font-size:60px; margin-bottom:12px; }
.welcome h2 { font-size:22px; color:#c41e3a; margin:0 0 8px; }
.welcome p { font-size:14px; color:#999; margin:0 0 20px; }
.quick { display:flex; flex-wrap:wrap; gap:8px; justify-content:center; }
.q-btn { padding:8px 16px; font-size:13px; background:#fff5f7; border:1px solid #ffe9ec; border-radius:18px; cursor:pointer; color:#c41e3a; }
.q-btn:hover { background:#ffe9ec; }

.msg-row { display:flex; }
.m-user { justify-content:flex-end; }
.m-ai, .m-tool, .m-system { justify-content:flex-start; }
.bubble { max-width:76%; padding:12px 18px; border-radius:14px; font-size:14px; line-height:1.7; animation:up 0.3s; }
@keyframes up { from { opacity:0; transform:translateY(8px); } }
.bu { background:linear-gradient(135deg,#c41e3a,#d43030); color:#fff; border-bottom-right-radius:4px; }
.ba { background:#fff; border:1px solid rgba(0,0,0,0.04); box-shadow:0 1px 3px rgba(0,0,0,0.06); border-bottom-left-radius:4px; color:#333; }
.ac { white-space:pre-wrap; word-break:break-word; }
.tool-note { font-size:12px; color:#8d6e63; padding:6px 12px; background:#f0ebe3; border:1px dashed #d4a574; border-radius:10px; max-width:80%; }
.sys-note { text-align:center; font-size:12px; color:#bbb; padding:6px; }
.bar { display:flex; gap:10px; padding:14px 20px; border-top:1px solid rgba(196,30,58,0.06); background:#fff; }
.bar input { flex:1; height:42px; padding:0 16px; border:1px solid rgba(196,30,58,0.12); border-radius:21px; font-size:14px; outline:none; background:#fefaf7; font-family:inherit; }
.bar input:focus { border-color:#c41e3a; }
.bar button { padding:0 28px; height:42px; border:none; border-radius:21px; font-size:14px; font-weight:600; cursor:pointer; background:linear-gradient(135deg,#c41e3a,#d43030); color:#fff; }
.bar button:hover:not(:disabled) { transform:translateY(-1px); }
.bar button:disabled { opacity:0.4; cursor:not-allowed; }
.btn-stop { background:#fff!important; color:#c41e3a!important; border:1px solid #c41e3a!important; }
.empty { text-align:center; padding:30px; color:#ccc; font-size:13px; }
@media(max-width:768px) { .rag-layout{flex-direction:column} .sidebar{width:100%;max-height:160px} }
</style>
