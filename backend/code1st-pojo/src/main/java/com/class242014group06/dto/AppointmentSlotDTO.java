package com.class242014group06.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 可预约时段 DTO（用于创建/更新请求）
 */
@Data
public class AppointmentSlotDTO {

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

    /** 最大可预约人数 */
    private Integer maxPeople;

    /** 状态：1-可用 0-不可用 */
    private Integer status;
}
