package com.class242014group06.service;

import com.class242014group06.dto.CulturalProductDTO;
import com.class242014group06.vo.CulturalProductVO;

import java.util.List;

public interface CulturalProductService {

    /**
     * 创建产品
     */
    void create(Long inheritorId, CulturalProductDTO dto);

    /**
     * 更新产品
     */
    void update(Long productId, Long inheritorId, CulturalProductDTO dto);

    /**
     * 删除产品
     */
    void delete(Long productId, Long inheritorId);

    /**
     * 切换上下架状态
     */
    void toggleStatus(Long productId, Long inheritorId, Integer status);

    /**
     * 查询自己的产品列表
     */
    List<CulturalProductVO> listByInheritor(Long inheritorId);

    /**
     * 查询所有已上架产品（首页展示）
     */
    List<CulturalProductVO> listPublished();
}
