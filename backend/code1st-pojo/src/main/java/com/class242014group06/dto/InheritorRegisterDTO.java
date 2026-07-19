package com.class242014group06.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel(description = "非遗传承人注册时传递的数据模型（无需登录，比普通用户更详细）")
public class InheritorRegisterDTO implements Serializable {

    @ApiModelProperty("用户名")
    private String username;

    @ApiModelProperty("姓名")
    private String name;

    @ApiModelProperty("密码")
    private String password;

    @ApiModelProperty("手机号")
    private String phone;

    @ApiModelProperty("性别")
    private String sex;

    @ApiModelProperty("身份证号")
    private String idNumber;

    @ApiModelProperty("邮箱（用于接收审核结果通知）")
    private String email;

    @ApiModelProperty("所属地区（如：江苏省苏州市）")
    private String region;

    @ApiModelProperty("角色：INHERITOR-非遗传承人")
    private String role;
}
