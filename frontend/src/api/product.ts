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

/** 传承人查看自己商品的订单 */
export function listInheritorProductOrders(): Promise<Result<ProductOrderVO[]>> {
  return request.get('/product-order/inheritor')
}

/** 传承人发货 */
export function shipProductOrder(id: number): Promise<Result<string>> {
  return request.put(`/product-order/${id}/ship`)
}

/** 传承人完成订单 */
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
