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

/** 注册新管理员请求参数 */
export interface RegisterRequest {
  username: string
  name: string
  phone?: string
  sex?: string
  idNumber?: string
}
