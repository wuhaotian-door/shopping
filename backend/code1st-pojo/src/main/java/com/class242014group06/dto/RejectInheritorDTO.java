package com.class242014group06.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel(description = "驳回传承人申请请求")
public class RejectInheritorDTO implements Serializable {

    @ApiModelProperty("驳回原因（将发送至申请人邮箱）")
    private String reason;
}
