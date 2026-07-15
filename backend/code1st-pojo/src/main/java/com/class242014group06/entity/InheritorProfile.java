package com.class242014group06.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 传承人详细信息实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InheritorProfile implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /** 关联employee表ID */
    private Long employeeId;

    /** 荣誉称号 */
    private String honorTitle;

    /** 个人简介 */
    private String selfIntro;

    /** 擅长技艺，逗号分隔 */
    private String craftTypes;

    /** 所在地 */
    private String address;

    /** 头像URL */
    private String avatarUrl;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
