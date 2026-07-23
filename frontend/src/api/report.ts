import request from './request'
import type { Result } from '@/types/api'

export interface UserReportVO {
  id: number
  reporterId: number
  reporterRole: string
  reportedUserId: number
  reportedUserRole: string
  reason: string
  screenshotUrl?: string
  status: number       // 0-待处理 1-已处理
  handleResult?: string
  banDays?: number
  handleTime?: string
  createTime: string
}

/** 提交举报 */
export function submitReport(data: {
  reportedUserId: number
  reportedUserRole: string
  reason: string
  screenshotUrl?: string
}): Promise<Result<string>> {
  return request.post('/report', data)
}

/** 管理员查看举报列表 */
export function listReports(): Promise<Result<UserReportVO[]>> {
  return request.get('/report')
}

/** 管理员处理举报 */
export function handleReport(id: number, handleResult: string, banDays: number): Promise<Result<string>> {
  return request.put(`/report/${id}/handle`, { handleResult, banDays })
}

/** 上传文件 */
export function uploadFile(file: File): Promise<Result<string>> {
  const form = new FormData()
  form.append('file', file)
  return request.post('/upload', form, { headers: { 'Content-Type': 'multipart/form-data' } })
}

/** 封禁用户（带天数） */
export function banUser(id: number, status: number, role: string, banDays?: number): Promise<Result<string>> {
  const params: Record<string, any> = { status, role }
  if (banDays != null) params.banDays = banDays
  return request.put(`/employee/${id}/ban`, null, { params })
}
