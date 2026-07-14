/** 后端统一响应包装 */
export interface Result<T = unknown> {
  /** 1 = 成功, 0 = 失败 */
  code: number
  /** 错误消息 */
  msg: string | null
  /** 响应数据 */
  data: T
}

/** 登录请求参数 */
export interface LoginRequest {
  username: string
  password: string
}

/** 登录成功返回的用户信息 */
export interface LoginResponse {
  id: number
  userName: string
  name: string
  token: string
}

/** 角色类型 */
export type RoleType = 'USER' | 'INHERITOR'

/** 管理员创建员工请求参数（需登录后调用） */
export interface AdminCreateRequest {
  username: string
  name: string
  phone?: string
  sex?: string
  idNumber?: string
}

/** 普通用户注册请求参数（公开接口，简化版） */
export interface RegisterRequest {
  username: string
  name: string
  password: string
  phone?: string
  sex?: string
}

/** 非遗传承人注册请求参数（公开接口，详细信息） */
export interface InheritorRegisterRequest {
  username: string
  name: string
  password: string
  phone?: string
  sex?: string
  idNumber: string
}