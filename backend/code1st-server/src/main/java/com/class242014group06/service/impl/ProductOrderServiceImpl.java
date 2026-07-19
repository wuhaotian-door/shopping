package com.class242014group06.service.impl;

import com.class242014group06.constant.MessageConstant;
import com.class242014group06.constant.ProductOrderStatusConstant;
import com.class242014group06.dto.ProductOrderSubmitDTO;
import com.class242014group06.entity.Product;
import com.class242014group06.entity.ProductOrder;
import com.class242014group06.exception.OrderBusinessException;
import com.class242014group06.mapper.ProductMapper;
import com.class242014group06.mapper.ProductOrderMapper;
import com.class242014group06.service.ProductOrderService;
import com.class242014group06.vo.ProductOrderVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductOrderServiceImpl implements ProductOrderService {

    @Autowired
    private ProductOrderMapper productOrderMapper;

    @Autowired
    private ProductMapper productMapper;

    @Override
    @Transactional
    public void submit(Long userId, String userName, ProductOrderSubmitDTO dto) {
        Product product = productMapper.getById(dto.getProductId());
        if (product == null || product.getStatus() != 1) {
            throw new OrderBusinessException(MessageConstant.PRODUCT_NOT_FOUND);
        }
        if (product.getStock() < dto.getQuantity()) {
            throw new OrderBusinessException(MessageConstant.PRODUCT_STOCK_INSUFFICIENT);
        }

        BigDecimal totalAmount = product.getPrice().multiply(BigDecimal.valueOf(dto.getQuantity()));

        ProductOrder order = ProductOrder.builder()
                .userId(userId)
                .userName(userName)
                .phone(dto.getPhone())
                .address(dto.getAddress())
                .productId(product.getId())
                .productName(product.getName())
                .productImage(product.getImageUrl())
                .price(product.getPrice())
                .quantity(dto.getQuantity())
                .totalAmount(totalAmount)
                .inheritorId(product.getInheritorId())
                .remark(dto.getRemark())
                .status(ProductOrderStatusConstant.PENDING)
                .build();
        productOrderMapper.insert(order);

        // 减库存
        Product update = Product.builder()
                .id(product.getId())
                .stock(product.getStock() - dto.getQuantity())
                .build();
        productMapper.update(update);
    }

    @Override
    public List<ProductOrderVO> listByUser(Long userId) {
        List<ProductOrder> orders = productOrderMapper.listByUserId(userId);
        return orders.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public List<ProductOrderVO> listByInheritor(Long inheritorId) {
        List<ProductOrder> orders = productOrderMapper.listByInheritorId(inheritorId);
        return orders.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public void ship(Long orderId, Long inheritorId) {
        ProductOrder order = productOrderMapper.getById(orderId);
        if (order == null) {
            throw new OrderBusinessException(MessageConstant.PRODUCT_ORDER_NOT_FOUND);
        }
        if (!order.getInheritorId().equals(inheritorId)) {
            throw new OrderBusinessException(MessageConstant.PRODUCT_ORDER_PERMISSION_DENIED);
        }
        if (!order.getStatus().equals(ProductOrderStatusConstant.PENDING)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        ProductOrder update = ProductOrder.builder()
                .id(orderId)
                .status(ProductOrderStatusConstant.SHIPPED)
                .build();
        productOrderMapper.update(update);
    }

    @Override
    public void complete(Long orderId, Long inheritorId) {
        ProductOrder order = productOrderMapper.getById(orderId);
        if (order == null) {
            throw new OrderBusinessException(MessageConstant.PRODUCT_ORDER_NOT_FOUND);
        }
        if (!order.getInheritorId().equals(inheritorId)) {
            throw new OrderBusinessException(MessageConstant.PRODUCT_ORDER_PERMISSION_DENIED);
        }
        if (!order.getStatus().equals(ProductOrderStatusConstant.SHIPPED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        ProductOrder update = ProductOrder.builder()
                .id(orderId)
                .status(ProductOrderStatusConstant.COMPLETED)
                .build();
        productOrderMapper.update(update);
    }

    @Override
    public void cancel(Long orderId, Long userId) {
        ProductOrder order = productOrderMapper.getById(orderId);
        if (order == null) {
            throw new OrderBusinessException(MessageConstant.PRODUCT_ORDER_NOT_FOUND);
        }
        // 只允许下单用户取消，或传承人取消
        if (!order.getUserId().equals(userId) && !order.getInheritorId().equals(userId)) {
            throw new OrderBusinessException(MessageConstant.PRODUCT_ORDER_PERMISSION_DENIED);
        }
        if (!order.getStatus().equals(ProductOrderStatusConstant.PENDING)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        ProductOrder update = ProductOrder.builder()
                .id(orderId)
                .status(ProductOrderStatusConstant.CANCELLED)
                .build();
        productOrderMapper.update(update);

        // 恢复库存
        Product product = productMapper.getById(order.getProductId());
        if (product != null) {
            Product stockUpdate = Product.builder()
                    .id(product.getId())
                    .stock(product.getStock() + order.getQuantity())
                    .build();
            productMapper.update(stockUpdate);
        }
    }

    @Override
    public void updateAddressByAdmin(Long orderId, String address) {
        ProductOrder order = productOrderMapper.getById(orderId);
        if (order == null) {
            throw new OrderBusinessException(MessageConstant.PRODUCT_ORDER_NOT_FOUND);
        }
        ProductOrder update = ProductOrder.builder().id(orderId).address(address).build();
        productOrderMapper.update(update);
    }

    @Override
    public void updateStatusByAdmin(Long orderId, Integer status) {
        ProductOrder order = productOrderMapper.getById(orderId);
        if (order == null) {
            throw new OrderBusinessException(MessageConstant.PRODUCT_ORDER_NOT_FOUND);
        }
        ProductOrder update = ProductOrder.builder().id(orderId).status(status).build();
        productOrderMapper.update(update);
    }

    @Override
    public List<ProductOrderVO> listAll() {
        List<ProductOrder> orders = productOrderMapper.listAll();
        return orders.stream().map(this::toVO).collect(Collectors.toList());
    }

    private ProductOrderVO toVO(ProductOrder order) {
        return ProductOrderVO.builder()
                .id(order.getId())
                .userName(order.getUserName())
                .phone(order.getPhone())
                .address(order.getAddress())
                .productId(order.getProductId())
                .productName(order.getProductName())
                .productImage(order.getProductImage())
                .price(order.getPrice())
                .quantity(order.getQuantity())
                .totalAmount(order.getTotalAmount())
                .inheritorId(order.getInheritorId())
                .remark(order.getRemark())
                .status(order.getStatus())
                .createTime(order.getCreateTime())
                .build();
    }
}
