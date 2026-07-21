import request from './request'
import type { Result } from '@/types/api'

/** 请求向消息接收方发送邮件通知（后端校验冷却） */
export function notifyRecipient(conversationId: number): Promise<Result<string>> {
  return request.post('/notify/message', { conversationId })
}
