package com.class242014group06.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 商品新增/修改请求体
 */
@Data
public class ProductSubmitDTO implements Serializable {

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
}
