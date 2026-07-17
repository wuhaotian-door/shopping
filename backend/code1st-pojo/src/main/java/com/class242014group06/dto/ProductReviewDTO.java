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

    @ApiModelProperty(value = "评分 1-5", required = true)
    private Integer rating;

    @ApiModelProperty(value = "评价内容", required = true)
    private String content;
}
