package com.class242014group06.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 可预约时段实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentSlot implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /** 传承人ID */
    private Long inheritorId;

    /** 日期 */
    private LocalDate slotDate;

    /** 开始时间 */
    private LocalTime startTime;

    /** 结束时间 */
    private LocalTime endTime;

    /** 重复类型：NONE-不重复, WEEKLY-每周, MONTHLY-每月 */
    private String recurrenceType;

    /** 重复截止日期 */
    private LocalDate recurrenceEndDate;

    /** 该时段最大可预约人数 */
    private Integer maxPeople;

    /** 已预约人数 */
    private Integer bookedCount;

    /** 状态：1-可用 0-不可用 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
