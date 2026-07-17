package com.class242014group06.controller;

import com.class242014group06.context.BaseContext;
import com.class242014group06.dto.ProductReviewDTO;
import com.class242014group06.result.Result;
import com.class242014group06.service.ProductReviewService;
import com.class242014group06.vo.ProductReviewVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 商品评价管理
 */
@RestController
@RequestMapping("/admin/product-review")
@Slf4j
@Api(tags = "商品评价接口")
public class ProductReviewController {

    @Autowired
    private ProductReviewService reviewService;

    /**
     * 用户提交商品评价（需登录）
     */
    @PostMapping("/{productId}")
    @ApiOperation("提交商品评价")
    public Result<String> addReview(@PathVariable Long productId, @RequestBody ProductReviewDTO dto) {
        Long userId = BaseContext.getCurrentId();
        log.info("用户提交评价，userId：{}，productId：{}，rating：{}", userId, productId, dto.getRating());
        reviewService.addReview(userId, productId, dto);
        return Result.success("评价提交成功");
    }

    /**
     * 查询某商品的所有评价（公开）
     */
    @GetMapping("/public/{productId}")
    @ApiOperation("查询商品评价列表（公开）")
    public Result<List<ProductReviewVO>> listReviews(@PathVariable Long productId) {
        log.info("查询商品评价（公开），productId：{}", productId);
        List<ProductReviewVO> reviews = reviewService.listByProductId(productId);
        return Result.success(reviews);
    }

    /**
     * 获取商品评分统计（公开）
     */
    @GetMapping("/public/{productId}/stats")
    @ApiOperation("获取商品评分统计（公开）")
    public Result<Map<String, Object>> getRatingStats(@PathVariable Long productId) {
        log.info("获取商品评分统计（公开），productId：{}", productId);
        Double avgRating = reviewService.getAvgRating(productId);
        Long reviewCount = reviewService.countByProductId(productId);
        Map<String, Object> stats = new HashMap<>();
        stats.put("avgRating", Math.round(avgRating * 10.0) / 10.0);
        stats.put("reviewCount", reviewCount);
        return Result.success(stats);
    }
}
