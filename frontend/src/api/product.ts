import request from './request'
import type { Result, CulturalProductVO, CulturalProductDTO } from '@/types/api'

/** 创建产品 */
export function createProduct(data: CulturalProductDTO): Promise<Result<string>> {
  return request.post('/product', data)
}

/** 更新产品 */
export function updateProduct(id: number, data: CulturalProductDTO): Promise<Result<string>> {
  return request.put(`/product/${id}`, data)
}

/** 删除产品 */
export function deleteProduct(id: number): Promise<Result<string>> {
  return request.delete(`/product/${id}`)
}

/** 切换产品上下架 */
export function toggleProductStatus(id: number, status: number): Promise<Result<string>> {
  return request.put(`/product/${id}/status`, null, { params: { status } })
}

/** 查看自己的产品列表 */
export function listMyProducts(): Promise<Result<CulturalProductVO[]>> {
  return request.get('/product/my')
}

/** 查询所有已上架产品（公开） */
export function listPublicProducts(): Promise<Result<CulturalProductVO[]>> {
  return request.get('/product/public')
}
