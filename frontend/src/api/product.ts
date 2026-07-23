import request from './request'
import type { Result, ProductVO, ProductOrderVO, ProductOrderSubmitRequest } from '@/types/api'

// ==================== 商品 ====================

/** 查询所有上架商品 */
export function listProducts(): Promise<Result<ProductVO[]>> {
  return request.get('/product')
}

/** 按分类查询商品 */
export function listProductsByCategory(category: string): Promise<Result<ProductVO[]>> {
  return request.get(`/product/category/${encodeURIComponent(category)}`)
}

/** 查询商品详情 */
export function getProductById(id: number): Promise<Result<ProductVO>> {
  return request.get(`/product/${id}`)
}

/** 传承人上架商品 */
export function addProduct(data: {
  name: string
  description?: string
  price: number
  stock: number
  imageUrl?: string
  modelUrl?: string
  category?: string
}): Promise<Result<string>> {
  return request.post('/product', data)
}

/** 传承人修改商品 */
export function updateProduct(id: number, data: {
  name?: string
  description?: string
  price?: number
  stock?: number
  imageUrl?: string
  modelUrl?: string
  category?: string
}): Promise<Result<string>> {
  return request.put(`/product/${id}`, data)
}

/** 传承人下架商品 */
export function offProduct(id: number): Promise<Result<string>> {
  return request.put(`/product/${id}/off`)
}

/** 传承人上架商品 */
export function onProduct(id: number): Promise<Result<string>> {
  return request.put(`/product/${id}/on`)
}

/** 传承人查看自己的商品 */
export function listMyProducts(): Promise<Result<ProductVO[]>> {
  return request.get('/product/my')
}

// ==================== 商品订单 ====================

/** 用户提交商品订单 */
export function submitProductOrder(data: ProductOrderSubmitRequest): Promise<Result<string>> {
  return request.post('/product-order', data)
}

/** 用户查看自己的商品订单 */
export function listMyProductOrders(): Promise<Result<ProductOrderVO[]>> {
  return request.get('/product-order/my')
}

/** 查询商品订单详情 */
export function getProductOrderDetail(id: number): Promise<Result<ProductOrderVO>> {
  return request.get(`/product-order/${id}`)
}

/** 传承人查看自己商品的订单 */
export function listInheritorProductOrders(): Promise<Result<ProductOrderVO[]>> {
  return request.get('/product-order/inheritor')
}

/** 传承人发货（含快递信息） */
export function shipProductOrder(id: number, courierCompany?: string, trackingNumber?: string): Promise<Result<string>> {
  return request.put(`/product-order/${id}/ship`, null, { params: { courierCompany, trackingNumber } })
}

/** 用户确认收货 */
export function completeProductOrder(id: number): Promise<Result<string>> {
  return request.put(`/product-order/${id}/complete`)
}

/** 取消商品订单 */
export function cancelProductOrder(id: number): Promise<Result<string>> {
  return request.put(`/product-order/${id}/cancel`)
}

/** 管理员查看所有商品订单 */
export function listAllProductOrders(): Promise<Result<ProductOrderVO[]>> {
  return request.get('/product-order/all')
}

/** 管理员删除任意商品 */
export function adminDeleteProduct(id: number): Promise<Result<string>> {
  return request.delete(`/product/${id}/admin`)
}

/** 管理员切换商品上下架 */
export function adminToggleProduct(id: number, status: number): Promise<Result<string>> {
  return request.put(`/product/${id}/admin/toggle`, null, { params: { status } })
}

/** 管理员修改商品订单状态 */
export function adminUpdateProductOrderStatus(id: number, status: number): Promise<Result<string>> {
  return request.put(`/product-order/${id}/admin/status`, null, { params: { status } })
}

/** 管理员修改商品订单收货地址 */
export function adminUpdateProductOrderAddress(id: number, address: string): Promise<Result<string>> {
  return request.put(`/product-order/${id}/admin/address`, null, { params: { address } })
}

/** 管理员批量切换商品上下架 */
export function batchToggleProduct(ids: number[], status: number): Promise<Result<string>> {
  return request.put('/product/batch-toggle', { ids, status })
}

/** 管理员查询所有商品（含已下架） */
export function listAllProducts(): Promise<Result<ProductVO[]>> {
  return request.get('/product/all')
}

/** 管理员批量删除商品 */
export function batchDeleteProduct(ids: number[]): Promise<Result<string>> {
  return request.post('/product/batch', { ids })
}

/** 管理员取消商品订单 */
export function adminCancelProductOrder(id: number): Promise<Result<string>> {
  return request.put(`/product-order/${id}/admin/cancel`)
}