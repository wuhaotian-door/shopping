package com.class242014group06.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@ApiModel(description = "批量ID请求")
public class BatchIdsDTO implements Serializable {

    @ApiModelProperty("ID列表")
    private List<Long> ids;
}
