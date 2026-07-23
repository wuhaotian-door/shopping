import request from './request'
import type { Result, ProductReviewVO, ProductReviewDTO } from '@/types/api'

/** 查询某商品的所有评价（公开） */
export function listReviews(productId: number): Promise<Result<ProductReviewVO[]>> {
  return request.get(`/product-review/public/${productId}`)
}

/** 获取商品评分统计（公开） */
export function getRatingStats(productId: number): Promise<Result<{ avgRating: number; reviewCount: number }>> {
  return request.get(`/product-review/public/${productId}/stats`)
}

/** 提交商品评价（需登录） */
export function submitReview(productId: number, data: ProductReviewDTO): Promise<Result<string>> {
  return request.post(`/product-review/${productId}`, data)
}

/** 删除自己的评价（需登录） */
export function deleteReview(reviewId: number): Promise<Result<string>> {
  return request.delete(`/product-review/${reviewId}`)
}
