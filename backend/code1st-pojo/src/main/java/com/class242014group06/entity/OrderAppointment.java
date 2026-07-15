package com.class242014group06.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 预约订单实体 — 用户向传承人工坊发送预约
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderAppointment implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /** 预约用户ID（关联employee表） */
    private Long userId;

    /** 预约用户姓名 */
    private String userName;

    /** 联系电话 */
    private String phone;

    /** 传承人工坊ID（关联employee表，role=INHERITOR） */
    private Long inheritorId;

    /** 预约日期 */
    private LocalDate appointmentDate;

    /** 预约时段，如 上午 9:00-11:00 */
    private String timeSlot;

    /** 预约人数 */
    private Integer peopleCount;

    /** 备注说明 */
    private String remark;

    /** 订单状态：0-待确认, 1-已确认, 2-已完成, 3-已取消 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Long createUser;

    private Long updateUser;
}
