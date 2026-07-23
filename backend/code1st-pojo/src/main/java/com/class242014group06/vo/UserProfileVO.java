package com.class242014group06.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 普通用户个人资料返回对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "普通用户个人资料")
public class UserProfileVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("用户ID")
    private Long id;

    @ApiModelProperty("姓名")
    private String name;

    @ApiModelProperty("用户名")
    private String username;

    @ApiModelProperty("手机号")
    private String phone;

    @ApiModelProperty("邮箱")
    private String email;

    @ApiModelProperty("头像URL")
    private String avatarUrl;

    @ApiModelProperty("性别")
    private String sex;
}
