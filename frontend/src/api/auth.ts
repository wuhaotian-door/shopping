import request from './request'
import type { Result, LoginRequest, LoginResponse, RegisterRequest } from '@/types/api'

/** 登录 */
export function login(data: LoginRequest): Promise<Result<LoginResponse>> {
  return request.post('/employee/login', data)
}

/** 注册新管理员（需登录后调用） */
export function register(data: RegisterRequest): Promise<Result<null>> {
  return request.post('/employee', data)
}
