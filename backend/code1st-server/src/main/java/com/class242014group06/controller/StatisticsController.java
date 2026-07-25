package com.class242014group06.controller;

import com.class242014group06.result.Result;
import com.class242014group06.service.StatisticsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/statistics")
@Slf4j
@Api(tags = "数据统计接口")
public class StatisticsController {

    @Autowired
    private StatisticsService statisticsService;

    @GetMapping("/overview")
    @ApiOperation("统计概览")
    public Result<Map<String, Object>> overview() {
        return Result.success(statisticsService.getOverview());
    }

    @GetMapping("/region")
    @ApiOperation("地域分布")
    public Result<List<Map<String, Object>>> regionDistribution() {
        return Result.success(statisticsService.getRegionDistribution());
    }

    @GetMapping("/age")
    @ApiOperation("年龄分布")
    public Result<List<Map<String, Object>>> ageDistribution() {
        return Result.success(statisticsService.getAgeDistribution());
    }

    @GetMapping("/craft")
    @ApiOperation("技艺分布")
    public Result<List<Map<String, Object>>> craftDistribution() {
        return Result.success(statisticsService.getCraftDistribution());
    }

    @GetMapping("/cities")
    @ApiOperation("城市分布（市级）")
    public Result<List<Map<String, Object>>> cityDistribution() {
        return Result.success(statisticsService.getCityDistribution());
    }

    @GetMapping("/cities-by-province")
    @ApiOperation("指定省份的城市分布（下钻用）")
    public Result<List<Map<String, Object>>> citiesByProvince(@RequestParam String province) {
        return Result.success(statisticsService.getCityDistributionByProvince(province));
    }

    @GetMapping("/registration-trend")
    @ApiOperation("注册趋势")
    public Result<List<Map<String, Object>>> registrationTrend() {
        return Result.success(statisticsService.getRegistrationTrend());
    }

    @GetMapping("/order-stats")
    @ApiOperation("订单统计")
    public Result<Map<String, Object>> orderStats() {
        return Result.success(statisticsService.getOrderStats());
    }

    @GetMapping("/monitor")
    @ApiOperation("管理员监控面板数据")
    public Result<Map<String, Object>> monitor() {
        Map<String, Object> result = new HashMap<>();
        result.put("registrationTrend", statisticsService.getRegistrationTrend());
        result.put("orderStats", statisticsService.getOrderStats());
        result.put("overview", statisticsService.getOverview());
        result.put("dailyViews", statisticsService.getDailyViews());
        result.put("transactionTrend", statisticsService.getTransactionTrend());
        return Result.success(result);
    }

    @GetMapping("/all")
    @ApiOperation("全部统计数据")
    public Result<Map<String, Object>> all() {
        Map<String, Object> result = new HashMap<>();
        result.put("overview", statisticsService.getOverview());
        result.put("region", statisticsService.getRegionDistribution());
        result.put("cities", statisticsService.getCityDistribution());
        result.put("age", statisticsService.getAgeDistribution());
        result.put("craft", statisticsService.getCraftDistribution());
        result.put("endangered", statisticsService.getEndangered());
        result.put("hotRanking", statisticsService.getHotRanking());
        result.put("dailyViews", statisticsService.getDailyViews());
        result.put("transactionTrend", statisticsService.getTransactionTrend());
        return Result.success(result);
    }

    @GetMapping("/endangered")
    @ApiOperation("濒危预警看板")
    public Result<List<Map<String, Object>>> endangered() {
        return Result.success(statisticsService.getEndangered());
    }

    @GetMapping("/hot-ranking")
    @ApiOperation("排行热门")
    public Result<List<Map<String, Object>>> hotRanking() {
        return Result.success(statisticsService.getHotRanking());
    }

    @GetMapping("/daily-views")
    @ApiOperation("每日浏览量")
    public Result<List<Map<String, Object>>> dailyViews() {
        return Result.success(statisticsService.getDailyViews());
    }

    @GetMapping("/transaction-trend")
    @ApiOperation("交易额趋势")
    public Result<List<Map<String, Object>>> transactionTrend() {
        return Result.success(statisticsService.getTransactionTrend());
    }
}
