package com.class242014group06.service.impl;

import com.class242014group06.constant.MessageConstant;
import com.class242014group06.dto.CulturalProductDTO;
import com.class242014group06.entity.CulturalProduct;
import com.class242014group06.entity.Inheritor;
import com.class242014group06.exception.BaseException;
import com.class242014group06.mapper.CulturalProductMapper;
import com.class242014group06.mapper.InheritorMapper;
import com.class242014group06.service.CulturalProductService;
import com.class242014group06.vo.CulturalProductVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CulturalProductServiceImpl implements CulturalProductService {

    @Autowired
    private CulturalProductMapper productMapper;

    @Autowired
    private InheritorMapper inheritorMapper;

    @Override
    public void create(Long inheritorId, CulturalProductDTO dto) {
        CulturalProduct product = new CulturalProduct();
        BeanUtils.copyProperties(dto, product);
        product.setInheritorId(inheritorId);
        if (product.getStatus() == null) {
            product.setStatus(0); // 默认下架
        }
        productMapper.insert(product);
    }

    @Override
    public void update(Long productId, Long inheritorId, CulturalProductDTO dto) {
        CulturalProduct existing = productMapper.getById(productId);
        if (existing == null) {
            throw new BaseException(MessageConstant.PRODUCT_NOT_FOUND);
        }
        if (!existing.getInheritorId().equals(inheritorId)) {
            throw new BaseException(MessageConstant.PRODUCT_NO_PERMISSION);
        }
        CulturalProduct product = new CulturalProduct();
        BeanUtils.copyProperties(dto, product);
        product.setId(productId);
        productMapper.update(product);
    }

    @Override
    public void delete(Long productId, Long inheritorId) {
        CulturalProduct existing = productMapper.getById(productId);
        if (existing == null) {
            throw new BaseException(MessageConstant.PRODUCT_NOT_FOUND);
        }
        if (!existing.getInheritorId().equals(inheritorId)) {
            throw new BaseException(MessageConstant.PRODUCT_NO_PERMISSION);
        }
        productMapper.deleteById(productId);
    }

    @Override
    public void toggleStatus(Long productId, Long inheritorId, Integer status) {
        CulturalProduct existing = productMapper.getById(productId);
        if (existing == null) {
            throw new BaseException(MessageConstant.PRODUCT_NOT_FOUND);
        }
        if (!existing.getInheritorId().equals(inheritorId)) {
            throw new BaseException(MessageConstant.PRODUCT_NO_PERMISSION);
        }
        CulturalProduct product = CulturalProduct.builder()
                .id(productId)
                .status(status)
                .build();
        productMapper.update(product);
    }

    @Override
    public List<CulturalProductVO> listByInheritor(Long inheritorId) {
        List<CulturalProduct> products = productMapper.getByInheritorId(inheritorId);
        Inheritor inheritor = inheritorMapper.getById(inheritorId);
        String inheritorName = inheritor != null ? inheritor.getName() : "";
        return products.stream()
                .map(p -> toVO(p, inheritorName))
                .collect(Collectors.toList());
    }

    @Override
    public List<CulturalProductVO> listPublished() {
        List<CulturalProduct> products = productMapper.listPublished();
        return products.stream()
                .map(p -> {
                    Inheritor inheritor = inheritorMapper.getById(p.getInheritorId());
                    String inheritorName = inheritor != null ? inheritor.getName() : "";
                    return toVO(p, inheritorName);
                })
                .collect(Collectors.toList());
    }

    private CulturalProductVO toVO(CulturalProduct product, String inheritorName) {
        return CulturalProductVO.builder()
                .id(product.getId())
                .inheritorId(product.getInheritorId())
                .inheritorName(inheritorName)
                .productName(product.getProductName())
                .category(product.getCategory())
                .detailBrief(product.getDetailBrief())
                .description(product.getDescription())
                .price(product.getPrice())
                .coverUrl(product.getCoverUrl())
                .stock(product.getStock())
                .status(product.getStatus())
                .createTime(product.getCreateTime())
                .build();
    }
}
