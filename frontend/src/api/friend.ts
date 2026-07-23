import request from './request'
import type { Result, FriendVO } from '@/types/api'

/** 发送好友申请（targetUserId + targetRole 确保 UID 唯一定位） */
export function applyFriend(targetUserId: number, targetRole: string): Promise<Result<string>> {
  return request.post('/friend/apply', { targetUserId, targetRole })
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

/** 获取待处理的好友申请（别人发给我的） */
export function listPendingRequests(): Promise<Result<FriendVO[]>> {
  return request.get('/friend/pending')
}

/** 获取我发出的好友申请 */
export function listMyRequests(): Promise<Result<FriendVO[]>> {
  return request.get('/friend/my-requests')
}
