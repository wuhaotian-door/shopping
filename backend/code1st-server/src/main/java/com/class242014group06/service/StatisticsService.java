package com.class242014group06.service;

import java.util.List;
import java.util.Map;

public interface StatisticsService {

    /** 传承人地域分布（省级） */
    List<Map<String, Object>> getRegionDistribution();

    /** 传承人城市分布（市级，含完整地名用于地图标注） */
    List<Map<String, Object>> getCityDistribution();

    /** 指定省份的城市级分布（下钻用，仅返回该省城市） */
    List<Map<String, Object>> getCityDistributionByProvince(String province);

    /** 传承人年龄分布（按年龄段） */
    List<Map<String, Object>> getAgeDistribution();

    /** 技艺类型分布 */
    List<Map<String, Object>> getCraftDistribution();

    /** 综合统计数据 */
    Map<String, Object> getOverview();

    /** 注册趋势（按月统计用户+传承人新增数量） */
    List<Map<String, Object>> getRegistrationTrend();

    /** 订单统计（状态分布 + 月度趋势） */
    Map<String, Object> getOrderStats();

    /** 濒危预警：传承人数 ≤3 的技艺及传承人年龄信息 */
    List<Map<String, Object>> getEndangered();

    /** 排行热门：按传承人数排序的技艺排行榜 */
    List<Map<String, Object>> getHotRanking();

    /** 每日浏览量（最近30天） */
    List<Map<String, Object>> getDailyViews();

    /** 交易额趋势（按月统计商品订单总额） */
    List<Map<String, Object>> getTransactionTrend();
}
