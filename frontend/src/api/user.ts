import request from './request'
import type { Result, UserProfileVO, UserProfileDTO, ChangePasswordDTO } from '@/types/api'

/** 获取当前普通用户的个人资料 */
export function getMyProfile(): Promise<Result<UserProfileVO>> {
  return request.get('/user/profile')
}

/** 更新当前普通用户的个人资料 */
export function updateProfile(data: UserProfileDTO): Promise<Result<string>> {
  return request.put('/user/profile', data)
}

/** 发送修改密码的邮箱验证码 */
export function sendChangePasswordCode(): Promise<Result<string>> {
  return request.post('/user/send-change-password-code')
}

/** 验证码校验通过后修改密码 */
export function changePassword(data: ChangePasswordDTO): Promise<Result<string>> {
  return request.put('/user/change-password', data)
}
