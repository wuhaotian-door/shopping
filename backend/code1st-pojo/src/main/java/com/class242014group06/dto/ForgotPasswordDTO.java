package com.class242014group06.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 忘记密码 — 发送验证码请求
 */
@Data
@ApiModel(description = "忘记密码发送验证码请求")
public class ForgotPasswordDTO implements Serializable {

    @ApiModelProperty("用户名")
    private String username;
}
