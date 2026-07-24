package com.class242014group06.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 非遗传承人实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Inheritor implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String username;
    private String password;
    private String phone;
    private String sex;
    private String idNumber;
    /** 所属地区，如"江苏省苏州市" */
    private String region;
    /** 邮箱（用于接收审核通知等） */
    private String email;
    /** 出生日期 */
    private LocalDate birthDate;
    /** 出生日期上次修改时间（限制一年只能改一次） */
    private LocalDateTime birthDateUpdatedAt;
    /** 年龄（由出生日期自动计算） */
    private Integer age;
    private Integer status;
    /** 封号截止时间（null表示未封号或永久禁用） */
    private LocalDateTime banUntil;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Long createUser;
    private Long updateUser;
    private String avatarUrl;
}
