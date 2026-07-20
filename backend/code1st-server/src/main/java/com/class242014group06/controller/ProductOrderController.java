package com.class242014group06.controller;

import com.class242014group06.constant.RoleConstant;
import com.class242014group06.context.BaseContext;
import com.class242014group06.dto.ProductOrderSubmitDTO;
import com.class242014group06.entity.User;
import com.class242014group06.mapper.UserMapper;
import com.class242014group06.result.Result;
import com.class242014group06.service.OperationLogService;
import com.class242014group06.service.ProductOrderService;
import com.class242014group06.vo.ProductOrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品订单管理 — 用户下单 & 传承人处理订单
 */
@RestController
@RequestMapping("/admin/product-order")
@Slf4j
@Api(tags = "商品订单相关接口")
public class ProductOrderController {

    @Autowired
    private ProductOrderService productOrderService;

    @Autowired
    private OperationLogService operationLogService;

    @Autowired
    private UserMapper userMapper;

    /**
     * 用户提交商品购买订单
     */
    @PostMapping
    @ApiOperation("用户提交商品购买订单")
    public Result<String> submit(@RequestBody ProductOrderSubmitDTO productOrderSubmitDTO) {
        Long userId = BaseContext.getCurrentId();
        log.info("用户提交商品订单，userId：{}，订单：{}", userId, productOrderSubmitDTO);
        // 从数据库查询用户姓名
        String userName = "";
        User user = userMapper.getById(userId);
        if (user != null) {
            userName = user.getName();
        }
        productOrderService.submit(userId, userName, productOrderSubmitDTO);
        return Result.success("下单成功");
    }

    /**
     * 用户查看自己的商品订单
     */
    @GetMapping("/my")
    @ApiOperation("用户查看自己的商品订单")
    public Result<List<ProductOrderVO>> listMyOrders() {
        Long userId = BaseContext.getCurrentId();
        log.info("用户查看自己的商品订单，userId：{}", userId);
        List<ProductOrderVO> orders = productOrderService.listByUser(userId);
        return Result.success(orders);
    }

    /**
     * 传承人查看自己商品的订单
     */
    @GetMapping("/inheritor")
    @ApiOperation("传承人查看自己商品的订单")
    public Result<List<ProductOrderVO>> listInheritorOrders() {
        Long inheritorId = BaseContext.getCurrentId();
        log.info("传承人查看商品订单，inheritorId：{}", inheritorId);
        List<ProductOrderVO> orders = productOrderService.listByInheritor(inheritorId);
        return Result.success(orders);
    }

    /**
     * 传承人发货
     */
    @PutMapping("/{id}/ship")
    @ApiOperation("传承人发货")
    public Result<String> ship(@PathVariable Long id) {
        Long inheritorId = BaseContext.getCurrentId();
        log.info("传承人发货，orderId：{}，inheritorId：{}", id, inheritorId);
        productOrderService.ship(id, inheritorId);
        return Result.success("已发货");
    }

    /**
     * 传承人完成订单
     */
    @PutMapping("/{id}/complete")
    @ApiOperation("传承人完成订单")
    public Result<String> complete(@PathVariable Long id) {
        Long inheritorId = BaseContext.getCurrentId();
        log.info("传承人完成订单，orderId：{}，inheritorId：{}", id, inheritorId);
        productOrderService.complete(id, inheritorId);
        return Result.success("订单已完成");
    }

    /**
     * 管理员查看所有商品订单
     */
    @GetMapping("/all")
    @ApiOperation("管理员查看所有商品订单")
    public Result<List<ProductOrderVO>> listAll() {
        log.info("管理员查看所有商品订单");
        return Result.success(productOrderService.listAll());
    }

    /**
     * 管理员修改商品订单状态
     */
    @PutMapping("/{id}/admin/status")
    @ApiOperation("管理员修改商品订单状态")
    public Result<String> updateStatusByAdmin(@PathVariable Long id, @RequestParam Integer status) {
        BaseContext.requireAdmin();
        Long operatorId = BaseContext.getCurrentId();
        log.info("管理员修改商品订单状态，orderId：{}，status：{}", id, status);
        productOrderService.updateStatusByAdmin(id, status);
        String st = status == 0 ? "待发货" : status == 1 ? "已发货" : status == 2 ? "已完成" : "已取消";
        operationLogService.log(operatorId, "管理员", RoleConstant.ADMIN,
                "UPDATE_ORDER_STATUS", "product_order", id, "修改商品订单ID=" + id + "状态为" + st);
        return Result.success("状态已更新");
    }

    /**
     * 管理员修改商品订单收货地址
     */
    @PutMapping("/{id}/admin/address")
    @ApiOperation("管理员修改商品订单收货地址")
    public Result<String> updateAddressByAdmin(@PathVariable Long id, @RequestParam String address) {
        BaseContext.requireAdmin();
        Long operatorId = BaseContext.getCurrentId();
        log.info("管理员修改商品订单地址，orderId：{}，address：{}", id, address);
        productOrderService.updateAddressByAdmin(id, address);
        operationLogService.log(operatorId, "管理员", RoleConstant.ADMIN,
                "UPDATE_ORDER_ADDRESS", "product_order", id,
                "修改商品订单ID=" + id + "收货地址");
        return Result.success("地址已更新");
    }

    /**
     * 管理员取消商品订单
     */
    @PutMapping("/{id}/admin/cancel")
    @ApiOperation("管理员取消商品订单")
    public Result<String> cancelByAdmin(@PathVariable Long id) {
        BaseContext.requireAdmin();
        Long operatorId = BaseContext.getCurrentId();
        log.info("管理员取消商品订单，orderId：{}", id);
        productOrderService.cancelByAdmin(id);
        operationLogService.log(operatorId, "管理员", RoleConstant.ADMIN,
                "CANCEL_ORDER", "product_order", id, "取消商品订单ID=" + id);
        return Result.success("订单已取消");
    }

    /**
     * 取消订单（用户/传承人）
     */
    @PutMapping("/{id}/cancel")
    @ApiOperation("取消商品订单")
    public Result<String> cancel(@PathVariable Long id) {
        Long userId = BaseContext.getCurrentId();
        log.info("取消商品订单，orderId：{}，userId：{}", id, userId);
        productOrderService.cancel(id, userId);
        return Result.success("订单已取消");
    }
}
