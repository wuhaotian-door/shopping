import request from './request'
import type { Result, HeritageProjectVO, HeritageProjectDTO } from '@/types/api'

/** 创建项目 */
export function createProject(data: HeritageProjectDTO): Promise<Result<string>> {
  return request.post('/project', data)
}

/** 更新项目 */
export function updateProject(id: number, data: HeritageProjectDTO): Promise<Result<string>> {
  return request.put(`/project/${id}`, data)
}

/** 删除项目 */
export function deleteProject(id: number): Promise<Result<string>> {
  return request.delete(`/project/${id}`)
}

/** 切换项目上下架 */
export function toggleProjectStatus(id: number, status: number): Promise<Result<string>> {
  return request.put(`/project/${id}/status`, null, { params: { status } })
}

/** 查看自己的项目列表，可按类型筛选 */
export function listMyProjects(projectType?: number): Promise<Result<HeritageProjectVO[]>> {
  const params: Record<string, any> = {}
  if (projectType !== undefined) {
    params.projectType = projectType
  }
  return request.get('/project/my', { params })
}

/** 查询所有已上架项目（公开） */
export function listPublicProjects(): Promise<Result<HeritageProjectVO[]>> {
  return request.get('/project/public')
}

/** 校验项目日期是否在时段范围内 */
export function validateProjectDates(startDate: string, endDate: string): Promise<Result<string>> {
  return request.get('/project/validate-dates', { params: { startDate, endDate } })
}

/** 管理员查询所有项目 */
export function listAllProjects(): Promise<Result<HeritageProjectVO[]>> {
  return request.get('/project/all')
}

/** 查询指定传承人的已上架项目（公开） */
export function listPublicProjectsByInheritor(inheritorId: number): Promise<Result<HeritageProjectVO[]>> {
  return request.get(`/project/public/inheritor/${inheritorId}`)
}

/** 管理员删除任意项目 */
export function adminDeleteProject(id: number): Promise<Result<string>> {
  return request.delete(`/project/${id}/admin`)
}
