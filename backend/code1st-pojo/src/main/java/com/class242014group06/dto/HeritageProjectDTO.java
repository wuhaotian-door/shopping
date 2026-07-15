package com.class242014group06.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 非遗项目编辑DTO
 */
@Data
@ApiModel(description = "非遗项目编辑")
public class HeritageProjectDTO implements Serializable {

    @ApiModelProperty("项目标题")
    private String title;

    @ApiModelProperty("项目分类")
    private String category;

    @ApiModelProperty("项目描述")
    private String description;

    @ApiModelProperty("封面图")
    private String coverUrl;

    @ApiModelProperty("活动地点")
    private String location;

    @ApiModelProperty("开始日期")
    private LocalDate startDate;

    @ApiModelProperty("结束日期")
    private LocalDate endDate;

    @ApiModelProperty("最大参与人数")
    private Integer maxPeople;

    @ApiModelProperty("状态：0-下架 1-上架")
    private Integer status;
}
