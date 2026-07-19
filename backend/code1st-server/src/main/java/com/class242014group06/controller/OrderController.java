package com.class242014group06.controller;

import com.class242014group06.context.BaseContext;
import com.class242014group06.dto.OrderSubmitDTO;
import com.class242014group06.result.Result;
import com.class242014group06.service.OrderService;
import com.class242014group06.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 预约订单管理 — 用户下单 & 传承人工坊订单处理
 */
@RestController
@RequestMapping("/admin/order")
@Slf4j
@Api(tags = "预约订单相关接口")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 用户提交预约订单
     */
    @PostMapping
    @ApiOperation("用户提交预约订单")
    public Result<String> submit(@RequestBody OrderSubmitDTO orderSubmitDTO) {
        log.info("用户提交预约订单：{}", orderSubmitDTO);
        Long userId = BaseContext.getCurrentId();
        orderService.submit(userId, orderSubmitDTO);
        return Result.success("预约提交成功");
    }

    /**
     * 用户查看自己的订单列表
     */
    @GetMapping("/my")
    @ApiOperation("用户查看自己的订单")
    public Result<List<OrderVO>> listMyOrders() {
        Long userId = BaseContext.getCurrentId();
        log.info("用户查看自己的订单，userId：{}", userId);
        List<OrderVO> orders = orderService.listByUser(userId);
        return Result.success(orders);
    }

    /**
     * 传承人查看自己工坊收到的订单
     */
    @GetMapping("/inheritor")
    @ApiOperation("传承人查看工坊订单")
    public Result<List<OrderVO>> listInheritorOrders() {
        Long inheritorId = BaseContext.getCurrentId();
        log.info("传承人查看工坊订单，inheritorId：{}", inheritorId);
        List<OrderVO> orders = orderService.listByInheritor(inheritorId);
        return Result.success(orders);
    }

    /**
     * 传承人确认订单
     */
    @PutMapping("/{id}/confirm")
    @ApiOperation("传承人确认订单")
    public Result<String> confirm(@PathVariable Long id) {
        Long inheritorId = BaseContext.getCurrentId();
        log.info("传承人确认订单，orderId：{}，inheritorId：{}", id, inheritorId);
        orderService.confirm(id, inheritorId);
        return Result.success("订单已确认");
    }

    /**
     * 传承人拒绝订单
     */
    @PutMapping("/{id}/reject")
    @ApiOperation("传承人拒绝订单")
    public Result<String> reject(@PathVariable Long id) {
        Long inheritorId = BaseContext.getCurrentId();
        log.info("传承人拒绝订单，orderId：{}，inheritorId：{}", id, inheritorId);
        orderService.reject(id, inheritorId);
        return Result.success("订单已拒绝");
    }

    /**
     * 传承人将订单标记为已完成
     */
    @PutMapping("/{id}/complete")
    @ApiOperation("传承人完成订单")
    public Result<String> complete(@PathVariable Long id) {
        Long inheritorId = BaseContext.getCurrentId();
        log.info("传承人完成订单，orderId：{}，inheritorId：{}", id, inheritorId);
        orderService.complete(id, inheritorId);
        return Result.success("订单已完成");
    }

    /**
     * 管理员查看所有订单
     */
    @GetMapping("/all")
    @ApiOperation("管理员查看所有订单")
    public Result<List<OrderVO>> listAll() {
        log.info("管理员查看所有订单");
        return Result.success(orderService.listAll());
    }

    /**
     * 管理员取消订单
     */
    @PutMapping("/{id}/cancel")
    @ApiOperation("管理员取消订单")
    public Result<String> cancelByAdmin(@PathVariable Long id) {
        log.info("管理员取消订单，orderId：{}", id);
        orderService.cancelByAdmin(id);
        return Result.success("订单已取消");
    }

    /**
     * 管理员修改订单状态
     */
    @PutMapping("/{id}/admin/status")
    @ApiOperation("管理员修改订单状态")
    public Result<String> updateStatusByAdmin(@PathVariable Long id, @RequestParam Integer status) {
        log.info("管理员修改订单状态，orderId：{}，status：{}", id, status);
        orderService.updateStatusByAdmin(id, status);
        return Result.success("状态已更新");
    }

    /**
     * 管理员修改预约订单日期和时段
     */
    @PutMapping("/{id}/admin/fields")
    @ApiOperation("管理员修改预约订单日期和时段")
    public Result<String> updateFieldsByAdmin(@PathVariable Long id,
            @RequestParam(required = false) String appointmentDate,
            @RequestParam(required = false) String timeSlot) {
        log.info("管理员修改预约订单，id：{}，date：{}，slot：{}", id, appointmentDate, timeSlot);
        orderService.updateFieldsByAdmin(id, appointmentDate, timeSlot);
        return Result.success("订单已更新");
    }
}
