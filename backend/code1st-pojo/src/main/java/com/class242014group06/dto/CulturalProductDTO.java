package com.class242014group06.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 文创产品编辑DTO
 */
@Data
@ApiModel(description = "文创产品编辑")
public class CulturalProductDTO implements Serializable {

    @ApiModelProperty("产品名称")
    private String productName;

    @ApiModelProperty("产品分类")
    private String category;

    @ApiModelProperty("简介")
    private String detailBrief;

    @ApiModelProperty("详细描述")
    private String description;

    @ApiModelProperty("价格")
    private BigDecimal price;

    @ApiModelProperty("封面图")
    private String coverUrl;

    @ApiModelProperty("库存")
    private Integer stock;

    @ApiModelProperty("状态：0-下架 1-上架")
    private Integer status;
}
