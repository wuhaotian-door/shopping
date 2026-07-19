package com.class242014group06.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 管理员操作日志实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationLog implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    /** 操作人ID */
    private Long operatorId;
    /** 操作人姓名 */
    private String operatorName;
    /** 操作人角色 */
    private String operatorRole;
    /** 操作类型：APPROVE_INHERITOR / REJECT_INHERITOR / TOGGLE_USER_STATUS / DELETE_PRODUCT / TOGGLE_PRODUCT / DELETE_PROJECT / DELETE_ARTICLE / UPDATE_ORDER_STATUS / CANCEL_ORDER / CREATE_ADMIN / CHANGE_PASSWORD */
    private String operationType;
    /** 操作目标类型：inheritor / user / product / project / article / order */
    private String targetType;
    /** 操作目标ID */
    private Long targetId;
    /** 操作详情 */
    private String detail;
    /** 操作时间 */
    private LocalDateTime createTime;
}
