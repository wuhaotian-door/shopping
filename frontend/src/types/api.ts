/** 鍚庣缁熶竴鍝嶅簲鍖呰 */
export interface Result<T = unknown> {
  /** 1 = 鎴愬姛, 0 = 澶辫触 */
  code: number
  /** 閿欒娑堟伅 */
  msg: string | null
  /** 鍝嶅簲鏁版嵁 */
  data: T
}

/** 鐧诲綍璇锋眰鍙傛暟 */
export interface LoginRequest {
  username: string
  password: string
}

/** 鐧诲綍鎴愬姛杩斿洖鐨勭敤鎴蜂俊鎭?*/
export interface LoginResponse {
  id: number
  userName: string
  name: string
  token: string
  role: RoleType | 'ADMIN'
}

/** 瑙掕壊绫诲瀷 */
export type RoleType = 'USER' | 'INHERITOR'

/** 绠＄悊鍛樺垱寤哄憳宸ヨ姹傚弬鏁帮紙闇€鐧诲綍鍚庤皟鐢級 */
export interface AdminCreateRequest {
  username: string
  name: string
  phone?: string
  sex?: string
  idNumber?: string
}

/** 鏅€氱敤鎴锋敞鍐岃姹傚弬鏁帮紙鍏紑鎺ュ彛锛岀畝鍖栫増锛?*/
export interface RegisterRequest {
  username: string
  name: string
  password: string
  phone?: string
  sex?: string
  email?: string
}

/** 蹇樿瀵嗙爜 鈥?鍙戦€侀獙璇佺爜璇锋眰 */
export interface ForgotPasswordRequest {
  username: string
}

/** 蹇樿瀵嗙爜 鈥?閲嶇疆瀵嗙爜璇锋眰 */
export interface ResetPasswordRequest {
  username: string
  code: string
  newPassword: string
}

/** 闈為仐浼犳壙浜烘敞鍐岃姹傚弬鏁帮紙鍏紑鎺ュ彛锛岃缁嗕俊鎭級 */
export interface InheritorRegisterRequest {
  username: string
  name: string
  password: string
  phone?: string
  sex?: string
  email?: string   // 閭锛堢敤浜庢帴鏀跺鏍哥粨鏋滈€氱煡锛?
  idNumber: string
  /** 鍑虹敓鏃ユ湡锛屽锛?985-03-12 */
  birthDate?: string
  /** 鎵€灞炲湴鍖猴紝濡傦細姹熻嫃鐪佽嫃宸炲競 */
  region: string
}

/** 寰呭鏍哥敤鎴蜂俊鎭紙绠＄悊鍛樺鏍稿垪琛ㄧ敤锛?*/
export interface PendingUser {
  id: number
  username: string
  name: string
  phone: string
  sex: string
  idNumber: string
  /** 鎵€灞炲湴鍖?*/
  region?: string
  role?: string
  status: number
  createTime: string
}

/** 鐢ㄦ埛鍒楄〃淇℃伅锛堢鐞嗗憳鐢ㄦ埛绠＄悊鐢級 */
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

/** 浠〃鐩樼粺璁℃暟鎹?*/
export interface DashboardStats {
  totalUsers: number
  userCount: number
  inheritorCount: number
  adminCount: number
  pendingCount: number
  enabledCount: number
  disabledCount: number
}

// ==================== 璁㈠崟 ====================

/** 璁㈠崟鐘舵€?*/
export type OrderStatus = 0 | 1 | 2 | 3
// 0-寰呯‘璁? 1-宸茬‘璁? 2-宸插畬鎴? 3-宸插彇娑?

/** 璁㈠崟鎻愪氦璇锋眰 */
export interface OrderSubmitRequest {
  userName: string
  phone: string
  inheritorId: number
  projectId?: number
  appointmentDate: string // yyyy-MM-dd
  timeSlot: string
  peopleCount: number
  remark: string
}

/** 璁㈠崟杩斿洖瀵硅薄 */
export interface OrderVO {
  id: number
  inheritorId: number
  projectId?: number
  userName: string
  phone: string
  appointmentDate: string
  timeSlot: string
  peopleCount: number
  remark: string
  status: OrderStatus
  createTime: string
}

// ==================== 浼犳壙浜轰釜浜鸿祫鏂?====================

export interface InheritorProfileVO {
  id: number
  employeeId: number
  name: string
  phone: string
  sex: string
  region: string
  honorTitle: string
  selfIntro: string
  craftTypes: string
  address: string
  avatarUrl: string
  birthDate: string | null
  age: number | null
}

export interface InheritorProfileDTO {
  honorTitle?: string
  selfIntro?: string
  craftTypes?: string
  address?: string
  avatarUrl?: string
}

// ==================== 闈為仐鐭ヨ瘑搴?====================

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

// ==================== 闈為仐椤圭洰 ====================

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
  projectType: number  // 0-鍏泭(闈炵泩鍒?, 1-闈炲叕鐩?鐩堝埄)
  price?: number       // 浠锋牸锛堥潪鍏泭椤圭洰浣跨敤锛?
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
  projectType?: number  // 0-鍏泭(闈炵泩鍒?, 1-闈炲叕鐩?鐩堝埄)
  price?: number        // 浠锋牸锛堥潪鍏泭椤圭洰浣跨敤锛?
}

// ==================== 鍟嗗煄鍟嗗搧 ====================

export interface ProductVO {
  id: number
  name: string
  description: string
  price: number
  stock: number
  imageUrl: string
  /** 3D模型URL（glb/gltf格式），可选 */
  modelUrl?: string
  category: string
  inheritorId: number
  inheritorName: string
  status: number
  createTime: string
  avgRating: number
  reviewCount: number
}

// ==================== 鍟嗗搧璇勪环 ====================

export interface ProductReviewVO {
  id: number
  productId: number
  userId: number
  userName: string
  rating: number | null
  content: string
  parentId: number | null
  replyToUserName: string | null
  isDeleted: number
  children: ProductReviewVO[]
  createTime: string
}

export interface ProductReviewDTO {
  rating?: number
  content: string
  parentId?: number
}

// ==================== 鍟嗗搧璁㈠崟 ====================

export interface ProductOrderVO {
  id: number
  userId?: number
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
  courierCompany?: string
  trackingNumber?: string
  createTime: string
}

export interface ProductOrderSubmitRequest {
  phone: string
  address: string
  productId: number
  quantity: number
  remark: string
}

// ==================== 棰勭害鏃舵 ====================

export interface AppointmentSlot {
  id: number
  inheritorId: number
  slotDate: string
  startTime: string       // "HH:mm"
  endTime: string         // "HH:mm"
  recurrenceType: string  // 'NONE' | 'WEEKLY' | 'MONTHLY'
  recurrenceEndDate?: string
  maxPeople: number
  bookedCount: number
  status: number
  createTime?: string
  updateTime?: string
}

export interface AppointmentSlotDTO {
  slotDate: string
  startTime: string
  endTime: string
  recurrenceType: string
  recurrenceEndDate?: string
  maxPeople: number
  status: number
}

export interface SlotDateRange {
  minDate?: string
  maxDate?: string
}

// ==================== 鑱婂ぉ绯荤粺 ====================

export interface ConversationVO {
  conversationId: number
  otherUserId: number
  otherUserRole: string
  otherUserName: string
  otherName: string
  lastMessage: string
  lastMessageTime: string
  unreadCount: number
}

export interface ChatMessageVO {
  id: number
  conversationId: number
  senderId: number
  senderRole: string
  senderName: string
  content: string
  messageType: string
  isRead: number
  createdAt: string
  mine: boolean
}

export interface ChatSendDTO {
  conversationId?: number
  receiverId?: number
  receiverRole?: string
  content: string
}

// ==================== 鏅€氱敤鎴蜂釜浜鸿祫鏂?====================

export interface UserProfileVO {
  id: number
  name: string
  username: string
  phone: string
  email: string
  sex: string
}

export interface UserProfileDTO {
  username?: string
  name?: string
  phone?: string
  email?: string
  sex?: string
}

export interface ChangePasswordDTO {
  code?: string        // 楠岃瘉鐮侊紙蹇樿瀵嗙爜鍦烘櫙锛?
  oldPassword?: string // 鏃у瘑鐮侊紙宸茬櫥褰曟敼瀵嗙爜鍦烘櫙锛?
  newPassword: string  // 鏂板瘑鐮?
}

// ==================== 濂藉弸绯荤粺 ====================

export interface FriendVO {
  relationId: number
  userId: number
  userName: string
  name: string
  role: string
  status: number
  createdAt: string
}

// ==================== 鏂囩珷璇勮 ====================

export interface ArticleCommentVO {
  id: number
  articleId: number
  userId: number
  userName: string
  content: string
  parentId: number | null
  replyToUserName: string | null
  isDeleted: number
  children: ArticleCommentVO[]
  createTime: string
}

export interface ArticleCommentDTO {
  content: string
  parentId?: number
}

// ==================== 鎿嶄綔鏃ュ織 ====================

export interface OperationLog {
  id: number
  operatorId: number
  operatorName: string
  operatorRole: string
  operationType: string
  targetType: string
  targetId: number
  detail: string
  createTime: string
}