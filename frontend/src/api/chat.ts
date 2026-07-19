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

/** 按用户名搜索用户（在user和inheritor表中查找） */
export function searchUser(username: string): Promise<Result<{
  id: number; name: string; username: string; role: string; isFriend: number
}>> {
  return request.get('/chat/search', { params: { username } })
}
