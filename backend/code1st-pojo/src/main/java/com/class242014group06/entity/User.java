package com.class242014group06.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 注册用户实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String username;
    private String password;
    private String phone;
    private String email;
    private String avatarUrl;
    private String sex;
    private Integer status;
    /** 封号截止时间（null表示未封号或永久禁用） */
    private LocalDateTime banUntil;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Long createUser;
    private Long updateUser;
}
