package com.class242014group06.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

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

    /** 时段，如 上午 9:00-11:00 */
    private String timeSlot;

    /** 该时段最大可预约人数 */
    private Integer maxPeople;

    /** 已预约人数 */
    private Integer bookedCount;

    /** 状态：1-可用 0-不可用 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
