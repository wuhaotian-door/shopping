import request from './request'
import type { Result, LoginRequest, LoginResponse, AdminCreateRequest, RegisterRequest, InheritorRegisterRequest, PendingUser, UserInfo, DashboardStats } from '@/types/api'

/** 登录 */
export function login(data: LoginRequest): Promise<Result<LoginResponse>> {
  return request.post('/auth/login', data)
}

/** 注册新管理员（需登录后调用） */
export function adminCreate(data: AdminCreateRequest): Promise<Result<null>> {
  return request.post('/employee', data)
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

/** 获取所有用户列表（管理员专用） */
export function listAllUsers(): Promise<Result<UserInfo[]>> {
  return request.get('/employee/list')
}

/** 搜索用户（管理员专用） */
export function searchUsers(keyword: string): Promise<Result<UserInfo[]>> {
  return request.get('/employee/search', { params: { keyword } })
}

/** 修改用户状态（管理员专用） */
export function updateUserStatus(id: number, status: number): Promise<Result<string>> {
  return request.put(`/employee/${id}/status`, null, { params: { status } })
}

/** 获取仪表盘统计数据（管理员专用） */
export function getDashboardStats(): Promise<Result<DashboardStats>> {
  return request.get('/employee/statistics')
}