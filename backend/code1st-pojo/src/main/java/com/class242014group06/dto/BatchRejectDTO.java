package com.class242014group06.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@ApiModel(description = "批量驳回传承人请求")
public class BatchRejectDTO implements Serializable {

    @ApiModelProperty("ID列表")
    private List<Long> ids;

    @ApiModelProperty("驳回原因（将发送至各申请人邮箱）")
    private String reason;
}
