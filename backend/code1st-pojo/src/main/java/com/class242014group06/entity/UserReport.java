package com.class242014group06.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户举报实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserReport implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /** 举报人ID */
    private Long reporterId;

    /** 举报人角色 */
    private String reporterRole;

    /** 被举报人ID */
    private Long reportedUserId;

    /** 被举报人角色 */
    private String reportedUserRole;

    /** 举报原因 */
    private String reason;

    /** 截图URL（可选） */
    private String screenshotUrl;

    /** 状态：0-待处理 1-已处理 */
    private Integer status;

    /** 处理结果（管理员填写） */
    private String handleResult;

    /** 封号天数（0表示仅驳回，不封号） */
    private Integer banDays;

    /** 处理时间 */
    private LocalDateTime handleTime;

    private LocalDateTime createTime;
}
