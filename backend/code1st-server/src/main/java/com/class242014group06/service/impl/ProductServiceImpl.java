package com.class242014group06.service.impl;

import com.class242014group06.constant.MessageConstant;
import com.class242014group06.dto.ProductSubmitDTO;
import com.class242014group06.entity.Inheritor;
import com.class242014group06.entity.Product;
import com.class242014group06.exception.OrderBusinessException;
import com.class242014group06.mapper.InheritorMapper;
import com.class242014group06.mapper.ProductMapper;
import com.class242014group06.service.ProductService;
import com.class242014group06.vo.ProductVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private InheritorMapper inheritorMapper;

    @Override
    public void add(Long inheritorId, String inheritorName, ProductSubmitDTO dto) {
        // 如果传入了名称就直接用，否则从数据库查询
        String name = inheritorName;
        if (name == null || "传承人".equals(name)) {
            Inheritor inheritor = inheritorMapper.getById(inheritorId);
            if (inheritor != null) {
                name = inheritor.getName();
            }
        }
        Product product = new Product();
        BeanUtils.copyProperties(dto, product);
        product.setInheritorId(inheritorId);
        product.setInheritorName(name);
        product.setStatus(1); // 默认上架
        productMapper.insert(product);
    }

    @Override
    public void update(Long productId, Long inheritorId, ProductSubmitDTO dto) {
        Product product = productMapper.getById(productId);
        if (product == null) {
            throw new OrderBusinessException(MessageConstant.PRODUCT_NOT_FOUND);
        }
        if (!product.getInheritorId().equals(inheritorId)) {
            throw new OrderBusinessException(MessageConstant.PRODUCT_PERMISSION_DENIED);
        }
        Product update = new Product();
        BeanUtils.copyProperties(dto, update);
        update.setId(productId);
        productMapper.update(update);
    }

    @Override
    public void toggleStatus(Long productId, Long inheritorId, Integer status) {
        Product product = productMapper.getById(productId);
        if (product == null) {
            throw new OrderBusinessException(MessageConstant.PRODUCT_NOT_FOUND);
        }
        if (!product.getInheritorId().equals(inheritorId)) {
            throw new OrderBusinessException(MessageConstant.PRODUCT_PERMISSION_DENIED);
        }
        Product update = Product.builder()
                .id(productId)
                .status(status)
                .build();
        productMapper.update(update);
    }

    @Override
    public List<ProductVO> listAll() {
        List<Product> products = productMapper.listAll();
        return products.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public List<ProductVO> listByCategory(String category) {
        List<Product> products = productMapper.listByCategory(category);
        return products.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public ProductVO getById(Long id) {
        Product product = productMapper.getById(id);
        if (product == null) {
            throw new OrderBusinessException(MessageConstant.PRODUCT_NOT_FOUND);
        }
        return toVO(product);
    }

    @Override
    public List<ProductVO> listByInheritor(Long inheritorId) {
        List<Product> products = productMapper.listByInheritor(inheritorId);
        return products.stream().map(this::toVO).collect(Collectors.toList());
    }

    private ProductVO toVO(Product product) {
        return ProductVO.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stock(product.getStock())
                .imageUrl(product.getImageUrl())
                .category(product.getCategory())
                .inheritorId(product.getInheritorId())
                .inheritorName(product.getInheritorName())
                .status(product.getStatus())
                .createTime(product.getCreateTime())
                .build();
    }
}
