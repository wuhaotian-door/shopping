import request from './request'
import type { Result, LoginRequest, LoginResponse, AdminCreateRequest, RegisterRequest, InheritorRegisterRequest, PendingUser } from '@/types/api'

/** 登录 */
export function login(data: LoginRequest): Promise<Result<LoginResponse>> {
  return request.post('/auth/login', data)
}

/** 注册新管理员（需登录后调用） */
export function adminCreate(data: AdminCreateRequest): Promise<Result<null>> {
  return request.post('/auth', data)
}

/** 普通用户自行注册（公开接口，无需登录） */
export function selfRegister(data: RegisterRequest): Promise<Result<string>> {
  return request.post('/auth/register', data)
}

/** 非遗传承人注册（公开接口，无需登录，含地区信息） */
export function inheritorRegister(data: InheritorRegisterRequest): Promise<Result<string>> {
  return request.post('/auth/register/inheritor', data)
}

/** 获取待审核的传承人列表（管理员专用） */
export function getPendingList(): Promise<Result<PendingUser[]>> {
  return request.get('/auth/pending')
}

/** 审核通过传承人（管理员专用） */
export function approveInheritor(id: number): Promise<Result<string>> {
  return request.put(`/auth/${id}/approve`)
}

/** 驳回传承人申请（管理员专用） */
export function rejectInheritor(id: number): Promise<Result<string>> {
  return request.put(`/auth/${id}/reject`)
}