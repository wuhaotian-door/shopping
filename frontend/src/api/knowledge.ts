import request from './request'
import type { Result, KnowledgeArticleVO, KnowledgeArticleDTO } from '@/types/api'

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
