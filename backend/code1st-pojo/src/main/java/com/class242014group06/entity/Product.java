package com.class242014group06.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 文创商品实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /** 商品名称 */
    private String name;

    /** 商品描述 */
    private String description;

    /** 商品价格 */
    private BigDecimal price;

    /** 库存数量 */
    private Integer stock;

    /** 商品图片URL */
    private String imageUrl;

    /** 商品分类 */
    private String category;

    /** 传承人ID */
    private Long inheritorId;

    /** 传承人姓名 */
    private String inheritorName;

    /** 状态：1-上架 0-下架 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Long createUser;

    private Long updateUser;
}
