package com.class242014group06.service;

import com.class242014group06.dto.ProductOrderSubmitDTO;
import com.class242014group06.vo.ProductOrderVO;

import java.util.List;

public interface ProductOrderService {

    /**
     * 用户提交商品购买订单
     */
    void submit(Long userId, String userName, ProductOrderSubmitDTO dto);

    /**
     * 查询订单详情
     */
    ProductOrderVO getDetail(Long orderId);

    /**
     * 用户查看自己的商品订单
     */
    List<ProductOrderVO> listByUser(Long userId);

    /**
     * 传承人查看自己商品的订单
     */
    List<ProductOrderVO> listByInheritor(Long inheritorId);

    /**
     * 传承人发货（含快递信息）
     */
    void ship(Long orderId, Long inheritorId, String courierCompany, String trackingNumber);

    /**
     * 用户确认收货（完成订单）
     */
    void complete(Long orderId, Long userId);

    /**
     * 用户取消订单（仅待发货状态下可取消）
     */
    void cancel(Long orderId, Long userId);
    List<ProductOrderVO> listAll();
    void updateStatusByAdmin(Long orderId, Integer status);
    void updateAddressByAdmin(Long orderId, String address);
    /** 管理员取消商品订单（恢复库存） */
    void cancelByAdmin(Long orderId);
}
