import request from './request'
import type { Result } from '@/types/api'

export interface StatItem {
  name: string
  value: number
}

export interface OverviewData {
  totalInheritors: number
  regionCount: number
  avgAge: number
}

export interface CityItem {
  city: string
  province: string
  value: number   // 该城市传承人数量
}

export interface AllStats {
  overview: OverviewData
  region: StatItem[]
  cities?: CityItem[]
  age: StatItem[]
  craft: StatItem[]
}

/** 统计概览 */
export function getOverview(): Promise<Result<OverviewData>> {
  return request.get('/statistics/overview')
}

/** 地域分布 */
export function getRegionDistribution(): Promise<Result<StatItem[]>> {
  return request.get('/statistics/region')
}

/** 年龄分布 */
export function getAgeDistribution(): Promise<Result<StatItem[]>> {
  return request.get('/statistics/age')
}

/** 技艺分布 */
export function getCraftDistribution(): Promise<Result<StatItem[]>> {
  return request.get('/statistics/craft')
}

/** 指定省份的城市分布（下钻用） */
export function getCitiesByProvince(province: string): Promise<Result<StatItem[]>> {
  return request.get('/statistics/cities-by-province', { params: { province } })
}

/** 全部数据 */
export function getAllStats(): Promise<Result<AllStats>> {
  return request.get('/statistics/all')
}

// ==================== 管理员监控 ====================

export interface RegTrendItem { month: string; users: number; inheritors: number; total: number }
export interface OrderStatsData { statusDistribution: StatItem[]; totalOrders: number }
export interface MonitorData {
  registrationTrend: RegTrendItem[]
  orderStats: OrderStatsData
  overview: OverviewData
}

/** 管理员监控面板数据 */
export function getMonitorData(): Promise<Result<MonitorData>> {
  return request.get('/statistics/monitor')
}
