import request from './request'
import type { Result, FriendVO } from '@/types/api'

/** 发送好友申请 */
export function applyFriend(targetUserId: number): Promise<Result<string>> {
  return request.post('/friend/apply', { targetUserId })
}

/** 接受好友申请 */
export function acceptFriend(relationId: number): Promise<Result<string>> {
  return request.put(`/friend/${relationId}/accept`)
}

/** 拒绝好友申请 */
export function rejectFriend(relationId: number): Promise<Result<string>> {
  return request.put(`/friend/${relationId}/reject`)
}

/** 获取好友列表 */
export function listFriends(): Promise<Result<FriendVO[]>> {
  return request.get('/friend/list')
}

/** 获取待处理的好友申请 */
export function listPendingRequests(): Promise<Result<FriendVO[]>> {
  return request.get('/friend/pending')
}
