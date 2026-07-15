package com.class242014group06.service;

import com.class242014group06.dto.OrderSubmitDTO;
import com.class242014group06.vo.OrderVO;

import java.util.List;

public interface OrderService {

    /**
     * 用户提交预约订单
     * @param userId 当前登录用户ID
     * @param dto    订单提交信息
     */
    void submit(Long userId, OrderSubmitDTO dto);

    /**
     * 查询当前用户的订单列表
     * @param userId 当前登录用户ID
     * @return 订单VO列表
     */
    List<OrderVO> listByUser(Long userId);

    /**
     * 查询传承人工坊收到的订单列表
     * @param inheritorId 传承人ID
     * @return 订单VO列表
     */
    List<OrderVO> listByInheritor(Long inheritorId);

    /**
     * 传承人确认订单
     * @param orderId 订单ID
     * @param inheritorId 传承人ID（用于校验权限）
     */
    void confirm(Long orderId, Long inheritorId);

    /**
     * 传承人拒绝/取消订单
     * @param orderId 订单ID
     * @param inheritorId 传承人ID（用于校验权限）
     */
    void reject(Long orderId, Long inheritorId);

    /**
     * 传承人将订单标记为已完成
     * @param orderId 订单ID
     * @param inheritorId 传承人ID（用于校验权限）
     */
    void complete(Long orderId, Long inheritorId);

    /**
     * 管理员查询所有订单
     * @return 订单VO列表
     */
    List<OrderVO> listAll();

    /**
     * 管理员取消订单
     * @param orderId 订单ID
     */
    void cancelByAdmin(Long orderId);
}
