package com.class242014group06.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 订单查询返回视图对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderVO implements Serializable {

    private Long id;

    /** 传承人ID */
    private Long inheritorId;

    /** 预约用户姓名 */
    private String userName;

    /** 联系电话 */
    private String phone;

    /** 预约日期 */
    private LocalDate appointmentDate;

    /** 预约时段 */
    private String timeSlot;

    /** 预约人数 */
    private Integer peopleCount;

    /** 备注说明 */
    private String remark;

    /** 订单状态 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;
}
