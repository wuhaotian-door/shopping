package com.class242014group06.service;

import com.class242014group06.dto.ProductSubmitDTO;
import com.class242014group06.vo.ProductVO;

import java.util.List;

public interface ProductService {

    /**
     * 传承人上架商品
     */
    void add(Long inheritorId, ProductSubmitDTO dto);

    /**
     * 传承人修改商品
     */
    void update(Long productId, Long inheritorId, ProductSubmitDTO dto);

    /**
     * 传承人下架/上架商品
     */
    void toggleStatus(Long productId, Long inheritorId, Integer status);

    /**
     * 查询所有上架商品（商城首页）
     */
    List<ProductVO> listAll();

    /**
     * 按分类查询上架商品
     */
    List<ProductVO> listByCategory(String category);

    /**
     * 查询商品详情
     */
    ProductVO getById(Long id);

    /**
     * 查询传承人自己的所有商品
     */
    List<ProductVO> listByInheritor(Long inheritorId);
}
