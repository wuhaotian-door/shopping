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
     * 用户查看自己的商品订单
     */
    List<ProductOrderVO> listByUser(Long userId);

    /**
     * 传承人查看自己商品的订单
     */
    List<ProductOrderVO> listByInheritor(Long inheritorId);

    /**
     * 传承人发货
     */
    void ship(Long orderId, Long inheritorId);

    /**
     * 传承人完成订单
     */
    void complete(Long orderId, Long inheritorId);

    /**
     * 取消订单（用户取消 或 传承人取消）
     */
    void cancel(Long orderId, Long userId);
    List<ProductOrderVO> listAll();
    void updateStatusByAdmin(Long orderId, Integer status);
    void updateAddressByAdmin(Long orderId, String address);
    /** 管理员取消商品订单（恢复库存） */
    void cancelByAdmin(Long orderId);
}
