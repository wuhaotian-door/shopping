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
  role: RoleType | 'ADMIN'
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
  /** 所属地区，如：江苏省苏州市 */
  region: string
}

/** 待审核用户信息（管理员审核列表用） */
export interface PendingUser {
  id: number
  username: string
  name: string
  phone: string
  sex: string
  idNumber: string
  /** 所属地区 */
  region: string
  role: string
  status: number
  createTime: string
}

// ==================== 订单 ====================

/** 订单状态 */
export type OrderStatus = 0 | 1 | 2 | 3
// 0-待确认, 1-已确认, 2-已完成, 3-已取消

/** 订单提交请求 */
export interface OrderSubmitRequest {
  userName: string
  phone: string
  inheritorId: number
  appointmentDate: string // yyyy-MM-dd
  timeSlot: string
  peopleCount: number
  remark: string
}

/** 订单返回对象 */
export interface OrderVO {
  id: number
  userName: string
  phone: string
  appointmentDate: string
  timeSlot: string
  peopleCount: number
  remark: string
  status: OrderStatus
  createTime: string
}

// ==================== 商品 ====================

/** 商品返回对象 */
export interface ProductVO {
  id: number
  name: string
  description: string
  price: number
  stock: number
  imageUrl: string
  category: string
  inheritorId: number
  inheritorName: string
  status: number
  createTime: string
}

// ==================== 商品订单 ====================

/** 商品订单状态 */
export type ProductOrderStatus = 0 | 1 | 2 | 3
// 0-待发货, 1-已发货, 2-已完成, 3-已取消

/** 商品订单提交请求 */
export interface ProductOrderSubmitRequest {
  phone: string
  address: string
  productId: number
  quantity: number
  remark?: string
}

/** 商品订单返回对象 */
export interface ProductOrderVO {
  id: number
  userName: string
  phone: string
  address: string
  productId: number
  productName: string
  productImage: string
  price: number
  quantity: number
  totalAmount: number
  inheritorId: number
  remark: string
  status: ProductOrderStatus
  createTime: string
}