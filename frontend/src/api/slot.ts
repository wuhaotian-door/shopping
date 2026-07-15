import request from './request'
import type { Result, AppointmentSlot } from '@/types/api'

/** 新增可预约时段 */
export function createSlot(data: AppointmentSlot): Promise<Result<string>> {
  return request.post('/slot', data)
}

/** 更新时段 */
export function updateSlot(id: number, data: AppointmentSlot): Promise<Result<string>> {
  return request.put(`/slot/${id}`, data)
}

/** 删除时段 */
export function deleteSlot(id: number): Promise<Result<string>> {
  return request.delete(`/slot/${id}`)
}

/** 查看自己的时段列表 */
export function listMySlots(): Promise<Result<AppointmentSlot[]>> {
  return request.get('/slot/my')
}

/** 查询传承人的可用时段（公开） */
export function getAvailableSlots(inheritorId: number, date: string): Promise<Result<AppointmentSlot[]>> {
  return request.get('/slot/available', { params: { inheritorId, date } })
}
