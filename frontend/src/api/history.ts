import request from './request'
import type { Result } from '@/types/api'

export interface BrowseHistoryVO {
  id: number
  userId: number
  userRole: string
  targetType: string    // product / news / project
  targetId: number
  targetTitle: string
  targetCover?: string
  targetPath?: string
  createTime: string
}

export interface RecordBrowseDTO {
  targetType: string
  targetId: number
  targetTitle: string
  targetCover?: string
  targetPath?: string
}

/** 记录一次浏览 */
export function recordBrowse(data: RecordBrowseDTO): Promise<Result<string>> {
  return request.post('/user/browse-history', data)
}

/** 获取当前用户浏览记录（去重，最近 50 条） */
export function listBrowseHistory(): Promise<Result<BrowseHistoryVO[]>> {
  return request.get('/user/browse-history')
}

/** 删除单条浏览记录 */
export function deleteBrowseHistory(id: number): Promise<Result<string>> {
  return request.delete(`/user/browse-history/${id}`)
}

/** 清空当前用户全部浏览记录 */
export function clearBrowseHistory(): Promise<Result<string>> {
  return request.delete('/user/browse-history')
}
