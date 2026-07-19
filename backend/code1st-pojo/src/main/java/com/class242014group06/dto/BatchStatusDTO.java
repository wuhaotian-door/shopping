package com.class242014group06.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@ApiModel(description = "批量修改状态请求")
public class BatchStatusDTO implements Serializable {

    @ApiModelProperty("ID列表")
    private List<Long> ids;

    @ApiModelProperty("目标状态：1-启用/上架, 0-禁用/下架")
    private Integer status;

    @ApiModelProperty("角色：ADMIN/INHERITOR/USER（用户批量操作时必填）")
    private String role;
}
