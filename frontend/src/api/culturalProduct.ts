import request from './request'
import type { Result, CulturalProductVO, CulturalProductDTO } from '@/types/api'

// ==================== 文创产品管理（传承人） ====================

/** 传承人查看自己的文创产品列表 */
export function listMyProducts(): Promise<Result<CulturalProductVO[]>> {
  return request.get('/cultural-product/my')
}

/** 查询所有已上架文创产品（公开访问） */
export function listPublicProducts(): Promise<Result<CulturalProductVO[]>> {
  return request.get('/cultural-product/public')
}

/** 传承人创建文创产品 */
export function createProduct(data: CulturalProductDTO): Promise<Result<string>> {
  return request.post('/cultural-product', data)
}

/** 传承人更新文创产品 */
export function updateProduct(id: number, data: CulturalProductDTO): Promise<Result<string>> {
  return request.put(`/cultural-product/${id}`, data)
}

/** 传承人删除文创产品 */
export function deleteProduct(id: number): Promise<Result<string>> {
  return request.delete(`/cultural-product/${id}`)
}

/** 传承人切换产品上下架状态 */
export function toggleProductStatus(id: number, status: number): Promise<Result<string>> {
  return request.put(`/cultural-product/${id}/status`, null, { params: { status } })
}
