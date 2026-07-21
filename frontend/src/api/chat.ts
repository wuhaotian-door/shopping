import request from './request'
import type { Result, ConversationVO, ChatMessageVO, ChatSendDTO } from '@/types/api'

/** 获取会话列表 */
export function listConversations(): Promise<Result<ConversationVO[]>> {
  return request.get('/chat/conversations')
}

/** 获取会话消息 */
export function getMessages(conversationId: number): Promise<Result<ChatMessageVO[]>> {
  return request.get(`/chat/messages/${conversationId}`)
}

/** 发送消息 */
export function sendMessage(data: ChatSendDTO): Promise<Result<ChatMessageVO>> {
  return request.post('/chat/send', data)
}

/** 模糊搜索用户（关键词匹配用户名或姓名） */
export function searchUser(keyword: string): Promise<Result<{
  id: number; name: string; username: string; role: string; isFriend: number
}>> {
  // 发送 %keyword% 格式，后端 LIKE 查询可直接拼接
  const like = `%${keyword}%`
  return request.get('/chat/search', { params: { keyword: like, username: like, name: like } })
}
