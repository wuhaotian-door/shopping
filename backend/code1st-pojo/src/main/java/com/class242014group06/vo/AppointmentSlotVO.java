package com.class242014group06.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 可预约时段 VO（用于返回给前端）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentSlotVO {

    private Long id;

    private Long inheritorId;

    private LocalDate slotDate;

    private LocalTime startTime;

    private LocalTime endTime;

    private String recurrenceType;

    private LocalDate recurrenceEndDate;

    private Integer maxPeople;

    private Integer bookedCount;

    private Integer status;

    private LocalDateTime createTime;
}
