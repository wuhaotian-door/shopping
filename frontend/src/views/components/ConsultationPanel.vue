<script setup lang="ts">
import { ref } from 'vue'

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
</script>

<template>
  <section class="tab-content">
    <div class="section-header">
      <h2>用户咨询</h2>
    </div>
    <div class="chat-panel">
      <div class="chat-messages">
        <div v-for="msg in messages" :key="msg.id" class="chat-msg" :class="msg.from === 'inheritor' ? 'msg-self' : 'msg-other'">
          <div class="msg-avatar">{{ msg.from === 'inheritor' ? '我' : msg.userName.charAt(0) }}</div>
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
        <textarea v-model="replyText" class="reply-input" rows="3" placeholder="输入回复内容..."></textarea>
        <button class="btn-primary" @click="sendReply" :disabled="!replyText.trim()">发送回复</button>
      </div>
    </div>
  </section>
</template>

<style scoped>
.tab-content { }
.section-header { display: flex; align-items: center; margin-bottom: 24px; }
.section-header h2 { font-size: 22px; color: #c41e3a; padding-left: 14px; border-left: 4px solid #c41e3a; }
.chat-panel { background: #fff; border-radius: 12px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.06); display: flex; flex-direction: column; min-height: 500px; }
.chat-messages { flex: 1; padding: 24px; display: flex; flex-direction: column; gap: 20px; max-height: 480px; overflow-y: auto; }
.chat-msg { display: flex; gap: 12px; max-width: 80%; }
.msg-self { align-self: flex-end; flex-direction: row-reverse; }
.msg-other { align-self: flex-start; }
.msg-avatar { width: 40px; height: 40px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 16px; font-weight: bold; color: #fff; flex-shrink: 0; }
.msg-other .msg-avatar { background: linear-gradient(135deg, #e8647a, #c41e3a); }
.msg-self .msg-avatar { background: linear-gradient(135deg, #ffd700, #e8b800); color: #8b4513; }
.msg-body { display: flex; flex-direction: column; gap: 4px; }
.msg-self .msg-body { align-items: flex-end; }
.msg-header { display: flex; gap: 10px; align-items: center; }
.msg-sender { font-size: 13px; font-weight: 600; color: #444; }
.msg-time { font-size: 11px; color: #999; }
.msg-content { padding: 12px 16px; border-radius: 12px; font-size: 14px; line-height: 1.7; }
.msg-other .msg-content { background: #fff5f7; color: #333; border-bottom-left-radius: 4px; }
.msg-self .msg-content { background: #c41e3a; color: #fff; border-bottom-right-radius: 4px; }
.chat-reply { display: flex; gap: 12px; padding: 16px 24px; border-top: 1px solid #eee; background: #fafafa; align-items: flex-end; }
.reply-input { flex: 1; padding: 12px; border: 1px solid #e0e0e0; border-radius: 8px; font-size: 14px; outline: none; resize: none; font-family: inherit; line-height: 1.6; }
.reply-input:focus { border-color: #c41e3a; box-shadow: 0 0 0 3px rgba(196,30,58,0.08); }
.btn-primary { padding: 10px 24px; font-size: 14px; color: #fff; background: #c41e3a; border: none; border-radius: 6px; cursor: pointer; font-weight: bold; }
.btn-primary:hover { background: #a8162b; }
.btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }
.empty-state { text-align: center; padding: 60px 20px; color: #999; font-size: 14px; }
</style>
