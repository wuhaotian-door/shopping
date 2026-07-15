package com.class242014group06.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户提交商品购买订单的请求体
 */
@Data
public class ProductOrderSubmitDTO implements Serializable {

    /** 联系电话 */
    private String phone;

    /** 收货地址 */
    private String address;

    /** 商品ID */
    private Long productId;

    /** 购买数量 */
    private Integer quantity;

    /** 用户备注 */
    private String remark;
}
