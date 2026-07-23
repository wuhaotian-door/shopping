package com.class242014group06.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商品评价实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductReview implements Serializable {

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

    /** 父评价ID，NULL表示顶级评价 */
    private Long parentId;

    /** 软删除标记：0正常 1已删除 */
    private Integer isDeleted;

    /** 创建时间 */
    private LocalDateTime createTime;
}
