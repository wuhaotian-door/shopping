package com.class242014group06.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 传承人个人资料返回对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InheritorProfileVO implements Serializable {

    private Long id;

    /** 关联employee表ID */
    private Long employeeId;

    /** 姓名（来自inheritor表） */
    private String name;

    /** 手机号 */
    private String phone;

    /** 性别 */
    private String sex;

    /** 所属地区（来自inheritor表，如"江苏省苏州市"） */
    private String region;

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

    /** 出生日期 */
    private LocalDate birthDate;

    /** 年龄（由出生日期计算） */
    private Integer age;
}
