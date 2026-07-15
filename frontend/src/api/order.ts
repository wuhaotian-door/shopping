import request from './request'
import type { Result, OrderSubmitRequest, OrderVO } from '@/types/api'

/** 用户提交预约订单 */
export function submitOrder(data: OrderSubmitRequest): Promise<Result<string>> {
  return request.post('/order', data)
}

/** 用户查看自己的订单 */
export function listMyOrders(): Promise<Result<OrderVO[]>> {
  return request.get('/order/my')
}

/** 传承人查看工坊收到的订单 */
export function listInheritorOrders(): Promise<Result<OrderVO[]>> {
  return request.get('/order/inheritor')
}

/** 传承人确认订单 */
export function confirmOrder(id: number): Promise<Result<string>> {
  return request.put(`/order/${id}/confirm`)
}

/** 传承人拒绝订单 */
export function rejectOrder(id: number): Promise<Result<string>> {
  return request.put(`/order/${id}/reject`)
}

/** 传承人完成订单 */
export function completeOrder(id: number): Promise<Result<string>> {
  return request.put(`/order/${id}/complete`)
}
