package com.class242014group06.service.impl;

import com.class242014group06.constant.MessageConstant;
import com.class242014group06.constant.OrderStatusConstant;
import com.class242014group06.dto.OrderSubmitDTO;
import com.class242014group06.entity.OrderAppointment;
import com.class242014group06.exception.OrderBusinessException;
import com.class242014group06.mapper.OrderMapper;
import com.class242014group06.service.OrderService;
import com.class242014group06.vo.OrderVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 用户提交预约订单
     */
    @Override
    public void submit(Long userId, OrderSubmitDTO dto) {
        OrderAppointment order = new OrderAppointment();
        BeanUtils.copyProperties(dto, order);
        order.setUserId(userId);
        order.setStatus(OrderStatusConstant.PENDING);
        orderMapper.insert(order);
    }

    /**
     * 查询当前用户的订单列表
     */
    @Override
    public List<OrderVO> listByUser(Long userId) {
        List<OrderAppointment> orders = orderMapper.getByUserId(userId);
        return orders.stream().map(this::toVO).collect(Collectors.toList());
    }

    /**
     * 查询传承人工坊收到的订单列表
     */
    @Override
    public List<OrderVO> listByInheritor(Long inheritorId) {
        List<OrderAppointment> orders = orderMapper.getByInheritorId(inheritorId);
        return orders.stream().map(this::toVO).collect(Collectors.toList());
    }

    /**
     * 传承人确认订单
     */
    @Override
    public void confirm(Long orderId, Long inheritorId) {
        OrderAppointment order = orderMapper.getById(orderId);
        if (order == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if (!order.getInheritorId().equals(inheritorId)) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if (!order.getStatus().equals(OrderStatusConstant.PENDING)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        OrderAppointment update = OrderAppointment.builder()
                .id(orderId)
                .status(OrderStatusConstant.CONFIRMED)
                .build();
        orderMapper.update(update);
    }

    /**
     * 传承人拒绝订单
     */
    @Override
    public void reject(Long orderId, Long inheritorId) {
        OrderAppointment order = orderMapper.getById(orderId);
        if (order == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if (!order.getInheritorId().equals(inheritorId)) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if (!order.getStatus().equals(OrderStatusConstant.PENDING)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        OrderAppointment update = OrderAppointment.builder()
                .id(orderId)
                .status(OrderStatusConstant.CANCELLED)
                .build();
        orderMapper.update(update);
    }

    /**
     * 传承人将订单标记为已完成
     */
    @Override
    public void complete(Long orderId, Long inheritorId) {
        OrderAppointment order = orderMapper.getById(orderId);
        if (order == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if (!order.getInheritorId().equals(inheritorId)) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if (!order.getStatus().equals(OrderStatusConstant.CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        OrderAppointment update = OrderAppointment.builder()
                .id(orderId)
                .status(OrderStatusConstant.COMPLETED)
                .build();
        orderMapper.update(update);
    }

    /**
     * 管理员查询所有订单
     */
    @Override
    public List<OrderVO> listAll() {
        List<OrderAppointment> orders = orderMapper.listAll();
        return orders.stream().map(this::toVO).collect(Collectors.toList());
    }

    /**
     * 管理员取消订单
     */
    @Override
    public void cancelByAdmin(Long orderId) {
        OrderAppointment order = orderMapper.getById(orderId);
        if (order == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        OrderAppointment update = OrderAppointment.builder()
                .id(orderId)
                .status(OrderStatusConstant.CANCELLED)
                .build();
        orderMapper.update(update);
    }

    /**
     * Entity → VO
     */
    private OrderVO toVO(OrderAppointment order) {
        return OrderVO.builder()
                .id(order.getId())
                .inheritorId(order.getInheritorId())
                .userName(order.getUserName())
                .phone(order.getPhone())
                .appointmentDate(order.getAppointmentDate())
                .timeSlot(order.getTimeSlot())
                .peopleCount(order.getPeopleCount())
                .remark(order.getRemark())
                .status(order.getStatus())
                .createTime(order.getCreateTime())
                .build();
    }
}
