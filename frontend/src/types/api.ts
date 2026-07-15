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

/** 用户列表信息（管理员用户管理用） */
export interface UserInfo {
  id: number
  username: string
  name: string
  phone: string
  sex: string
  idNumber: string
  role: string
  status: number
  createTime: string
}

/** 仪表盘统计数据 */
export interface DashboardStats {
  totalUsers: number
  userCount: number
  inheritorCount: number
  adminCount: number
  pendingCount: number
  enabledCount: number
  disabledCount: number
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
  inheritorId: number
  userName: string
  phone: string
  appointmentDate: string
  timeSlot: string
  peopleCount: number
  remark: string
  status: OrderStatus
  createTime: string
}

// ==================== 传承人个人资料 ====================

export interface InheritorProfileVO {
  id: number
  employeeId: number
  name: string
  phone: string
  sex: string
  honorTitle: string
  selfIntro: string
  craftTypes: string
  address: string
  avatarUrl: string
}

export interface InheritorProfileDTO {
  honorTitle?: string
  selfIntro?: string
  craftTypes?: string
  address?: string
  avatarUrl?: string
}

// ==================== 非遗知识库 ====================

export interface KnowledgeArticleVO {
  id: number
  authorId: number
  authorName: string
  title: string
  category: string
  content: string
  coverUrl: string
  status: number
  viewCount: number
  createTime: string
}

export interface KnowledgeArticleDTO {
  title: string
  category?: string
  content: string
  coverUrl?: string
  status?: number
}

// ==================== 非遗项目 ====================

export interface HeritageProjectVO {
  id: number
  inheritorId: number
  inheritorName: string
  title: string
  category: string
  description: string
  coverUrl: string
  location: string
  startDate: string
  endDate: string
  maxPeople: number
  currentPeople: number
  status: number
  createTime: string
}

export interface HeritageProjectDTO {
  title: string
  category?: string
  description?: string
  coverUrl?: string
  location?: string
  startDate?: string
  endDate?: string
  maxPeople?: number
  status?: number
}

// ==================== 文创产品 ====================

export interface CulturalProductVO {
  id: number
  inheritorId: number
  inheritorName: string
  productName: string
  category: string
  detailBrief: string
  description: string
  price: number
  coverUrl: string
  stock: number
  status: number
  createTime: string
}

export interface CulturalProductDTO {
  productName: string
  category?: string
  detailBrief?: string
  description?: string
  price?: number
  coverUrl?: string
  stock?: number
  status?: number
}

// ==================== 商城商品 ====================

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
  status: number
  createTime: string
}

export interface ProductOrderSubmitRequest {
  phone: string
  address: string
  productId: number
  quantity: number
  remark: string
}

// ==================== 预约时段 ====================

export interface AppointmentSlot {
  id: number
  inheritorId: number
  slotDate: string
  timeSlot: string
  maxPeople: number
  bookedCount: number
  status: number
  createTime?: string
  updateTime?: string
}