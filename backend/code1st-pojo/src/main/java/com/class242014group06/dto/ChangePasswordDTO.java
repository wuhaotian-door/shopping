package com.class242014group06.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel(description = "修改密码请求")
public class ChangePasswordDTO implements Serializable {

    @ApiModelProperty("验证码（忘记密码场景使用）")
    private String code;

    @ApiModelProperty("旧密码（已登录改密码场景使用）")
    private String oldPassword;

    @ApiModelProperty("新密码")
    private String newPassword;
}
