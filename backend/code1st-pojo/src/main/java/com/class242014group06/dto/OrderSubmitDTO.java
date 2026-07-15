package com.class242014group06.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 用户提交预约订单的请求体
 */
@Data
public class OrderSubmitDTO implements Serializable {

    /** 预约用户姓名 */
    private String userName;

    /** 联系电话 */
    private String phone;

    /** 传承人工坊ID */
    private Long inheritorId;

    /** 预约日期 */
    private LocalDate appointmentDate;

    /** 预约时段 */
    private String timeSlot;

    /** 预约人数 */
    private Integer peopleCount;

    /** 备注说明 */
    private String remark;
}
