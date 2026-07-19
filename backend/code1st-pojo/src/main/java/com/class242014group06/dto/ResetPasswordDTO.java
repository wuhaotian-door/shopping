package com.class242014group06.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 忘记密码 — 重置密码请求
 */
@Data
@ApiModel(description = "重置密码请求")
public class ResetPasswordDTO implements Serializable {

    @ApiModelProperty("用户名")
    private String username;

    @ApiModelProperty("邮箱验证码")
    private String code;

    @ApiModelProperty("新密码")
    private String newPassword;
}
