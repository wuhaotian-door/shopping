import request from './request'
import type { Result, KnowledgeArticleVO, KnowledgeArticleDTO } from '@/types/api'

/** 上传文件（图片），返回可访问的 URL */
export function uploadFile(file: File): Promise<Result<string>> {
  const formData = new FormData()
  formData.append('file', file)
  // 不要手动设置 Content-Type，让 axios 自动添加 boundary
  return request.post('/common/upload', formData)
}

/** 创建文章 */
export function createArticle(data: KnowledgeArticleDTO): Promise<Result<string>> {
  return request.post('/knowledge', data)
}

/** 更新文章 */
export function updateArticle(id: number, data: KnowledgeArticleDTO): Promise<Result<string>> {
  return request.put(`/knowledge/${id}`, data)
}

/** 删除文章 */
export function deleteArticle(id: number): Promise<Result<string>> {
  return request.delete(`/knowledge/${id}`)
}

/** 查看自己的文章列表 */
export function listMyArticles(): Promise<Result<KnowledgeArticleVO[]>> {
  return request.get('/knowledge/my')
}

/** 查询所有已发布文章（公开） */
export function listPublicArticles(): Promise<Result<KnowledgeArticleVO[]>> {
  return request.get('/knowledge/public')
}

/** 管理员查询所有文章 */
export function listAllArticles(): Promise<Result<KnowledgeArticleVO[]>> {
  return request.get('/knowledge/all')
}

/** 管理员删除任意文章 */
export function adminDeleteArticle(id: number): Promise<Result<string>> {
  return request.delete(`/knowledge/${id}/admin`)
}

/** 管理员批量删除文章 */
export function batchDeleteArticle(ids: number[]): Promise<Result<string>> {
  return request.post('/knowledge/batch', { ids })
}

/** 查询单篇文章详情（公开） */
export function getArticleById(id: number): Promise<Result<KnowledgeArticleVO>> {
  return request.get(`/knowledge/public/${id}`)
}
