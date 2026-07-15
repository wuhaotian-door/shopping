package com.class242014group06.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 文创产品实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CulturalProduct implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /** 传承人ID */
    private Long inheritorId;

    /** 产品名称 */
    private String productName;

    /** 产品分类 */
    private String category;

    /** 简介 */
    private String detailBrief;

    /** 详细描述 */
    private String description;

    /** 价格 */
    private BigDecimal price;

    /** 封面图 */
    private String coverUrl;

    /** 库存 */
    private Integer stock;

    /** 状态：0-下架 1-上架 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Long createUser;

    private Long updateUser;
}
