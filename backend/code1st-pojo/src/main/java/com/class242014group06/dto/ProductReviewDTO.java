package com.class242014group06.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 商品评价提交请求
 */
@Data
@ApiModel(description = "商品评价提交")
public class ProductReviewDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "评分 1-5，顶级评价必填，回复时无需传入")
    private Integer rating;

    @ApiModelProperty(value = "评价内容", required = true)
    private String content;

    @ApiModelProperty(value = "父评价ID，回复评价时传入")
    private Long parentId;
}
