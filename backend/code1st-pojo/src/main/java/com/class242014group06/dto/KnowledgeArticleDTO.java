package com.class242014group06.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 知识库文章编辑DTO
 */
@Data
@ApiModel(description = "知识库文章编辑")
public class KnowledgeArticleDTO implements Serializable {

    @ApiModelProperty("文章标题")
    private String title;

    @ApiModelProperty("分类")
    private String category;

    @ApiModelProperty("文章内容")
    private String content;

    @ApiModelProperty("封面图")
    private String coverUrl;

    @ApiModelProperty("视频URL")
    private String videoUrl;

    @ApiModelProperty("多张图片URL（逗号分隔）")
    private String images;

    @ApiModelProperty("状态：0-草稿 1-已发布")
    private Integer status;
}
