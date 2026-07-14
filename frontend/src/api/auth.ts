import request from './request'
import type { Result, LoginRequest, LoginResponse, AdminCreateRequest, RegisterRequest, InheritorRegisterRequest } from '@/types/api'

/** 登录 */
export function login(data: LoginRequest): Promise<Result<LoginResponse>> {
  return request.post('/employee/login', data)
}

/** 注册新管理员（需登录后调用） */
export function adminCreate(data: AdminCreateRequest): Promise<Result<null>> {
  return request.post('/employee', data)
}

/** 普通用户自行注册（公开接口，无需登录） */
export function selfRegister(data: RegisterRequest): Promise<Result<string>> {
  return request.post('/employee/register', data)
}

/** 非遗传承人注册（公开接口，无需登录，信息更详细） */
export function inheritorRegister(data: InheritorRegisterRequest): Promise<Result<string>> {
  return request.post('/employee/register/inheritor', data)
}