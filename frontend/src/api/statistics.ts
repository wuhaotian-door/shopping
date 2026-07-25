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

/** 濒危预警项 */
export interface EndangeredItem {
  craft: string
  count: number
  avgAge: number
  level: '严重濒危' | '高度濒危' | '濒危'
  inheritors: Array<{ name: string; age: number; region: string }>
}

export interface AllStats {
  overview: OverviewData
  region: StatItem[]
  cities?: CityItem[]
  age: StatItem[]
  craft: StatItem[]
  endangered?: EndangeredItem[]
  hotRanking?: StatItem[]
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

/** 濒危预警看板 */
export function getEndangered(): Promise<Result<EndangeredItem[]>> {
  return request.get('/statistics/endangered')
}

/** 排行热门 */
export function getHotRanking(): Promise<Result<StatItem[]>> {
  return request.get('/statistics/hot-ranking')
}

// ==================== 浏览量 & 交易趋势 ====================

export interface DailyViewItem { date: string; count: number }
export interface TransactionTrendItem { month: string; amount: number }

// ==================== 管理员监控 ====================

export interface RegTrendItem { month: string; users: number; inheritors: number; total: number }
export interface OrderStatsData { statusDistribution: StatItem[]; totalOrders: number }
export interface MonitorData {
  registrationTrend: RegTrendItem[]
  orderStats: OrderStatsData
  overview: OverviewData
  dailyViews?: DailyViewItem[]
  transactionTrend?: TransactionTrendItem[]
}

/** 管理员监控面板数据 */
export function getMonitorData(): Promise<Result<MonitorData>> {
  return request.get('/statistics/monitor')
}

/** 每日浏览量 */
export function getDailyViews(): Promise<Result<DailyViewItem[]>> {
  return request.get('/statistics/daily-views')
}

/** 交易额趋势 */
export function getTransactionTrend(): Promise<Result<TransactionTrendItem[]>> {
  return request.get('/statistics/transaction-trend')
}
