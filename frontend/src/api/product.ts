import request from './request'
import type { Result, ProductVO, ProductOrderVO, ProductOrderSubmitRequest } from '@/types/api'

// ==================== 鍟嗗搧 ====================

/** 鏌ヨ鎵€鏈変笂鏋跺晢鍝?*/
export function listProducts(): Promise<Result<ProductVO[]>> {
  return request.get('/product')
}

/** 鎸夊垎绫绘煡璇㈠晢鍝?*/
export function listProductsByCategory(category: string): Promise<Result<ProductVO[]>> {
  return request.get(`/product/category/${encodeURIComponent(category)}`)
}

/** 鏌ヨ鍟嗗搧璇︽儏 */
export function getProductById(id: number): Promise<Result<ProductVO>> {
  return request.get(`/product/${id}`)
}

/** 浼犳壙浜轰笂鏋跺晢鍝?*/
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

/** 浼犳壙浜轰慨鏀瑰晢鍝?*/
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

/** 浼犳壙浜轰笅鏋跺晢鍝?*/
export function offProduct(id: number): Promise<Result<string>> {
  return request.put(`/product/${id}/off`)
}

/** 浼犳壙浜轰笂鏋跺晢鍝?*/
export function onProduct(id: number): Promise<Result<string>> {
  return request.put(`/product/${id}/on`)
}

/** 浼犳壙浜烘煡鐪嬭嚜宸辩殑鍟嗗搧 */
export function listMyProducts(): Promise<Result<ProductVO[]>> {
  return request.get('/product/my')
}

// ==================== 鍟嗗搧璁㈠崟 ====================

/** 鐢ㄦ埛鎻愪氦鍟嗗搧璁㈠崟 */
export function submitProductOrder(data: ProductOrderSubmitRequest): Promise<Result<string>> {
  return request.post('/product-order', data)
}

/** 鐢ㄦ埛鏌ョ湅鑷繁鐨勫晢鍝佽鍗?*/
export function listMyProductOrders(): Promise<Result<ProductOrderVO[]>> {
  return request.get('/product-order/my')
}

/** 鏌ヨ鍟嗗搧璁㈠崟璇︽儏 */
export function getProductOrderDetail(id: number): Promise<Result<ProductOrderVO>> {
  return request.get(`/product-order/${id}`)
}

/** 浼犳壙浜烘煡鐪嬭嚜宸卞晢鍝佺殑璁㈠崟 */
export function listInheritorProductOrders(): Promise<Result<ProductOrderVO[]>> {
  return request.get('/product-order/inheritor')
}

/** 浼犳壙浜哄彂璐э紙鍚揩閫掍俊鎭級 */
export function shipProductOrder(id: number, courierCompany?: string, trackingNumber?: string): Promise<Result<string>> {
  return request.put(`/product-order/${id}/ship`, null, { params: { courierCompany, trackingNumber } })
}

/** 鐢ㄦ埛纭鏀惰揣 */
export function completeProductOrder(id: number): Promise<Result<string>> {
  return request.put(`/product-order/${id}/complete`)
}

/** 鍙栨秷鍟嗗搧璁㈠崟 */
export function cancelProductOrder(id: number): Promise<Result<string>> {
  return request.put(`/product-order/${id}/cancel`)
}

/** 绠＄悊鍛樻煡鐪嬫墍鏈夊晢鍝佽鍗?*/
export function listAllProductOrders(): Promise<Result<ProductOrderVO[]>> {
  return request.get('/product-order/all')
}

/** 绠＄悊鍛樺垹闄や换鎰忓晢鍝?*/
export function adminDeleteProduct(id: number): Promise<Result<string>> {
  return request.delete(`/product/${id}/admin`)
}

/** 绠＄悊鍛樺垏鎹㈠晢鍝佷笂涓嬫灦 */
export function adminToggleProduct(id: number, status: number): Promise<Result<string>> {
  return request.put(`/product/${id}/admin/toggle`, null, { params: { status } })
}

/** 绠＄悊鍛樹慨鏀瑰晢鍝佽鍗曠姸鎬?*/
export function adminUpdateProductOrderStatus(id: number, status: number): Promise<Result<string>> {
  return request.put(`/product-order/${id}/admin/status`, null, { params: { status } })
}

/** 绠＄悊鍛樹慨鏀瑰晢鍝佽鍗曟敹璐у湴鍧€ */
export function adminUpdateProductOrderAddress(id: number, address: string): Promise<Result<string>> {
  return request.put(`/product-order/${id}/admin/address`, null, { params: { address } })
}

/** 绠＄悊鍛樻壒閲忓垏鎹㈠晢鍝佷笂涓嬫灦 */
export function batchToggleProduct(ids: number[], status: number): Promise<Result<string>> {
  return request.put('/product/batch-toggle', { ids, status })
}

/** 绠＄悊鍛樻煡璇㈡墍鏈夊晢鍝侊紙鍚凡涓嬫灦锛?*/
export function listAllProducts(): Promise<Result<ProductVO[]>> {
  return request.get('/product/all')
}

/** 绠＄悊鍛樻壒閲忓垹闄ゅ晢鍝?*/
export function batchDeleteProduct(ids: number[]): Promise<Result<string>> {
  return request.post('/product/batch', { ids })
}

/** 绠＄悊鍛樺彇娑堝晢鍝佽鍗?*/
export function adminCancelProductOrder(id: number): Promise<Result<string>> {
  return request.put(`/product-order/${id}/admin/cancel`)
}
