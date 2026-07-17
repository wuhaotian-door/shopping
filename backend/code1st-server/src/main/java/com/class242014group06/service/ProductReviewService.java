package com.class242014group06.service;

import com.class242014group06.dto.ProductReviewDTO;
import com.class242014group06.vo.ProductReviewVO;

import java.util.List;

/**
 * 商品评价服务
 */
public interface ProductReviewService {

    /**
     * 用户提交商品评价
     */
    void addReview(Long userId, Long productId, ProductReviewDTO dto);

    /**
     * 查询某商品的所有评价
     */
    List<ProductReviewVO> listByProductId(Long productId);

    /**
     * 获取商品平均评分
     */
    Double getAvgRating(Long productId);

    /**
     * 获取商品评价数量
     */
    Long countByProductId(Long productId);
}
