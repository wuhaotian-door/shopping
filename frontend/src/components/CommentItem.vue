<script setup lang="ts">
import { ref } from 'vue'

interface CommentNode {
  id: number
  userId: number
  userName: string
  content: string
  parentId: number | null
  replyToUserName: string | null
  isDeleted: number
  children: CommentNode[]
  createTime: string
  rating?: number
}

const props = defineProps<{
  comment: CommentNode
  currentUserId: number | null
  depth: number
}>()

const emit = defineEmits<{
  reply: [parentId: number, content: string]
  delete: [commentId: number]
}>()

const showReplyForm = ref(false)
const replyContent = ref('')
const submitLoading = ref(false)

const indent = Math.min(props.depth, 5) * 28

function handleReply() {
  showReplyForm.value = !showReplyForm.value
  replyContent.value = ''
}

function handleSubmitReply() {
  if (!replyContent.value.trim()) return
  emit('reply', props.comment.id, replyContent.value.trim())
  replyContent.value = ''
  showReplyForm.value = false
}

function handleDelete() {
  if (confirm('确定要删除这条评论吗？')) {
    emit('delete', props.comment.id)
  }
}
</script>

<template>
  <div class="comment-thread">
    <div class="comment-node" :style="{ marginLeft: indent + 'px' }">
      <!-- 已删除评论 -->
      <template v-if="comment.isDeleted === 1 && comment.children.length === 0">
        <div class="comment-card deleted">
          <span class="deleted-text">[该评论已被删除]</span>
        </div>
      </template>

      <!-- 已删除但有子评论：保留壳子，显示回复数 -->
      <template v-else-if="comment.isDeleted === 1">
        <div class="comment-card deleted has-children">
          <span class="deleted-text">[该评论已被删除，共 {{ comment.children.length }} 条回复]</span>
          <span class="children-hint">▼ 以下为子评论</span>
        </div>
        <!-- 子评论容器：用左侧边框线视觉分组 -->
        <div class="deleted-children-group">
          <CommentItem
            v-for="child in comment.children"
            :key="child.id"
            :comment="child"
            :current-user-id="currentUserId"
            :depth="0"
            @reply="(parentId: number, content: string) => emit('reply', parentId, content)"
            @delete="emit('delete', $event)"
          >
            <template v-if="$slots.extra" #extra="{ comment: childComment }">
              <slot name="extra" :comment="childComment" />
            </template>
          </CommentItem>
        </div>
      </template>

      <!-- 正常评论 -->
      <template v-else>
        <div class="comment-card">
          <div class="comment-top">
            <div class="comment-user-avatar">{{ (comment.userName || '用')[0] }}</div>
            <div class="comment-user-info">
              <span class="comment-user-name">{{ comment.userName || '匿名用户' }}</span>
              <span class="comment-time">{{ comment.createTime?.slice(0, 10) }}</span>
            </div>
            <!-- 星级（由父组件通过插槽注入） -->
            <slot name="extra" :comment="comment" />
          </div>
          <!-- 回复提示 -->
          <p v-if="comment.replyToUserName" class="reply-hint">
            回复 <span class="reply-target">@{{ comment.replyToUserName }}</span>
          </p>
          <p class="comment-content">{{ comment.content }}</p>
          <div class="comment-actions">
            <button
              v-if="currentUserId"
              class="action-btn reply-btn"
              @click="handleReply"
            >
              {{ showReplyForm ? '取消回复' : '回复' }}
            </button>
            <button
              v-if="currentUserId && comment.userId === currentUserId && comment.isDeleted !== 1"
              class="action-btn delete-btn"
              @click="handleDelete"
            >
              删除
            </button>
          </div>

          <!-- 内联回复表单 -->
          <div v-if="showReplyForm" class="inline-reply-form">
            <textarea
              v-model="replyContent"
              class="form-textarea"
              rows="3"
              :placeholder="`回复 @${comment.userName}...`"
            ></textarea>
            <div class="reply-form-actions">
              <button
                class="btn-sm btn-primary-sm"
                :disabled="submitLoading || !replyContent.trim()"
                @click="handleSubmitReply"
              >
                回复
              </button>
              <button class="btn-sm btn-cancel-sm" @click="showReplyForm = false">取消</button>
            </div>
          </div>
        </div>

        <!-- 正常评论的子评论递归 -->
        <CommentItem
          v-for="child in comment.children"
          :key="child.id"
          :comment="child"
          :current-user-id="currentUserId"
          :depth="depth + 1"
          @reply="(parentId: number, content: string) => emit('reply', parentId, content)"
          @delete="emit('delete', $event)"
        >
          <template v-if="$slots.extra" #extra="{ comment: childComment }">
            <slot name="extra" :comment="childComment" />
          </template>
        </CommentItem>
      </template>
    </div>
  </div>
</template>

<style scoped>
.comment-thread {
  /* container for one root + all descendants */
}

.comment-node {
  margin-bottom: 8px;
}

.comment-card {
  padding: 16px 20px;
  border: 1px solid #f0f0f0;
  border-radius: 10px;
  transition: 0.2s;
  background: #fff;
}
.comment-card:hover {
  border-color: #f0c0c8;
  background: #fffafa;
}
.comment-card.deleted {
  opacity: 0.55;
  background: #fafafa;
}
.comment-card.deleted.has-children {
  opacity: 0.7;
  border-bottom-left-radius: 0;
  border-bottom-right-radius: 0;
  border-bottom: none;
  margin-bottom: 0;
}
.deleted-text {
  color: #aaa;
  font-size: 14px;
  font-style: italic;
}
.children-hint {
  display: block;
  margin-top: 6px;
  font-size: 12px;
  color: #bbb;
}
.deleted-children-group {
  border-left: 3px solid #e0c0c8;
  margin-left: 28px;
  padding-left: 16px;
  padding-bottom: 8px;
}

.comment-top {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}
.comment-user-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, #e8647a, #c41e3a);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 700;
  flex-shrink: 0;
}
.comment-user-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}
.comment-user-name {
  font-size: 14px;
  color: #333;
  font-weight: 600;
}
.comment-time {
  font-size: 12px;
  color: #999;
  margin-top: 1px;
}

.reply-hint {
  font-size: 13px;
  color: #999;
  margin-bottom: 6px;
  padding-left: 48px;
}
.reply-target {
  color: #c41e3a;
  font-weight: 600;
}

.comment-content {
  font-size: 14px;
  color: #555;
  line-height: 1.7;
  padding-left: 48px;
}

/* ---- 操作按钮 ---- */
.comment-actions {
  padding-left: 48px;
  margin-top: 10px;
  display: flex;
  gap: 12px;
}
.action-btn {
  font-size: 13px;
  padding: 4px 12px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  background: none;
  transition: 0.15s;
}
.reply-btn {
  color: #666;
}
.reply-btn:hover {
  color: #c41e3a;
  background: #fff5f7;
}
.delete-btn {
  color: #dc3545;
}
.delete-btn:hover {
  background: #fff5f5;
}

/* ---- 内联回复表单 ---- */
.inline-reply-form {
  margin-top: 12px;
  padding-left: 48px;
}
.form-textarea {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  resize: vertical;
  box-sizing: border-box;
  outline: none;
  font-family: inherit;
  line-height: 1.6;
}
.form-textarea:focus {
  border-color: #c41e3a;
  box-shadow: 0 0 0 3px rgba(196, 30, 58, 0.08);
}
.reply-form-actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}
.btn-sm {
  padding: 6px 16px;
  font-size: 13px;
  border-radius: 6px;
  cursor: pointer;
  border: none;
  font-weight: 600;
}
.btn-primary-sm {
  background: #c41e3a;
  color: #fff;
}
.btn-primary-sm:hover {
  background: #a8162b;
}
.btn-primary-sm:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.btn-cancel-sm {
  background: #f0f0f0;
  color: #666;
}
.btn-cancel-sm:hover {
  background: #e0e0e0;
}
</style>
