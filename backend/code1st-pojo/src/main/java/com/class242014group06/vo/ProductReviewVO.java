package com.class242014group06.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商品评价返回视图
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductReviewVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /** 商品ID */
    private Long productId;

    /** 评价用户ID */
    private Long userId;

    /** 评价用户名 */
    private String userName;

    /** 评分 1-5 */
    private Integer rating;

    /** 评价内容 */
    private String content;

    /** 评价时间 */
    private LocalDateTime createTime;
}
