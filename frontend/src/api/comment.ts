import request from './request'
import type { Result, ArticleCommentVO, ArticleCommentDTO } from '@/types/api'

/** 查询某文章的所有评论（公开） */
export function listComments(articleId: number): Promise<Result<ArticleCommentVO[]>> {
  return request.get(`/article-comment/public/${articleId}`)
}

/** 提交文章评论（需登录） */
export function submitComment(articleId: number, data: ArticleCommentDTO): Promise<Result<string>> {
  return request.post(`/article-comment/${articleId}`, data)
}

/** 获取文章评论数（公开） */
export function getCommentCount(articleId: number): Promise<Result<number>> {
  return request.get(`/article-comment/public/${articleId}/count`)
}