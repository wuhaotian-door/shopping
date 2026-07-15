package com.class242014group06.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 传承人个人资料编辑DTO
 */
@Data
@ApiModel(description = "传承人个人资料编辑")
public class InheritorProfileDTO implements Serializable {

    @ApiModelProperty("荣誉称号")
    private String honorTitle;

    @ApiModelProperty("个人简介")
    private String selfIntro;

    @ApiModelProperty("擅长技艺，逗号分隔")
    private String craftTypes;

    @ApiModelProperty("所在地")
    private String address;

    @ApiModelProperty("头像URL")
    private String avatarUrl;
}
