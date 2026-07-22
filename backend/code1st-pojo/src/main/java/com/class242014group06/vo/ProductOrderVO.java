package com.class242014group06.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品订单查询返回视图对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductOrderVO implements Serializable {

    private Long id;

    /** 下单用户姓名 */
    private String userName;

    /** 联系电话 */
    private String phone;

    /** 收货地址 */
    private String address;

    /** 商品ID */
    private Long productId;

    /** 商品名称 */
    private String productName;

    /** 商品图片 */
    private String productImage;

    /** 购买时单价 */
    private BigDecimal price;

    /** 购买数量 */
    private Integer quantity;

    /** 订单总金额 */
    private BigDecimal totalAmount;

    /** 传承人ID */
    private Long inheritorId;

    /** 用户备注 */
    private String remark;

    /** 订单状态：0-待发货, 1-已发货, 2-已完成, 3-已取消 */
    private Integer status;

    /** 下单用户ID */
    private Long userId;

    /** 快递公司 */
    private String courierCompany;

    /** 快递单号 */
    private String trackingNumber;

    /** 创建时间 */
    private LocalDateTime createTime;
}
