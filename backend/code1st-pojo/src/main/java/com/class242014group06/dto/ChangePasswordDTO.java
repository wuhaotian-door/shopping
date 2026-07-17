package com.class242014group06.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 修改密码请求（已登录用户）
 */
@Data
@ApiModel(description = "修改密码请求")
public class ChangePasswordDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "邮箱验证码", required = true)
    private String code;

    @ApiModelProperty(value = "新密码（至少6位）", required = true)
    private String newPassword;
}
