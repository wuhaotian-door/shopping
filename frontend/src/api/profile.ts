import request from './request'
import type { Result, InheritorProfileVO, InheritorProfileDTO } from '@/types/api'

/** 获取当前传承人的个人资料 */
export function getMyProfile(): Promise<Result<InheritorProfileVO>> {
  return request.get('/inheritor/profile')
}

/** 保存或更新个人资料 */
export function saveProfile(data: InheritorProfileDTO): Promise<Result<string>> {
  return request.put('/inheritor/profile', data)
}

/** 查询所有传承人资料（公开） */
export function listPublicProfiles(): Promise<Result<InheritorProfileVO[]>> {
  return request.get('/inheritor/profile/public')
}
