package com.class242014group06.service.impl;

import com.class242014group06.constant.MessageConstant;
import com.class242014group06.dto.ProductSubmitDTO;
import com.class242014group06.entity.Inheritor;
import org.springframework.beans.factory.annotation.Value;
import java.io.File;
import com.class242014group06.entity.Product;
import com.class242014group06.exception.OrderBusinessException;
import com.class242014group06.mapper.InheritorMapper;
import com.class242014group06.mapper.ProductMapper;
import com.class242014group06.service.ProductCatalogCache;
import com.class242014group06.service.ProductReviewService;
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

    @Autowired
    private ProductReviewService reviewService;

    @Autowired
    private ProductCatalogCache catalogCache;

    @Override
    public void add(Long inheritorId, ProductSubmitDTO dto) {
        // 从数据库查询传承人姓名
        String name = "";
        Inheritor inheritor = inheritorMapper.getById(inheritorId);
        if (inheritor != null) {
            name = inheritor.getName();
        }
        Product product = new Product();
        BeanUtils.copyProperties(dto, product);
        product.setInheritorId(inheritorId);
        product.setInheritorName(name);
        product.setStatus(1); // 默认上架
        productMapper.insert(product);
        catalogCache.invalidateAfterCommit();
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
        // 删除旧3D模型文件（保护默认模型和 static/ 下的文件不删除）
        if (dto.getModelUrl() != null && !dto.getModelUrl().isEmpty()
            && product.getModelUrl() != null && !product.getModelUrl().equals(dto.getModelUrl())) {
            deleteOldModelFile(product.getModelUrl());
        }
        // 删除旧视频文件（保护默认文件）
        if (dto.getVideoUrl() != null && !dto.getVideoUrl().isEmpty()
            && product.getVideoUrl() != null && !product.getVideoUrl().equals(dto.getVideoUrl())) {
            deleteOldVideoFile(product.getVideoUrl());
        }
        Product update = new Product();
        BeanUtils.copyProperties(dto, update);
        update.setId(productId);
        productMapper.update(update);
        catalogCache.invalidateAfterCommit();
    }

    @Value("${code1st.static.path:./static}")
    private String staticPath;
    private static final String DEFAULT_MODEL = "a_chinese_tongguan_kiln_teapot-1.glb";

    private void deleteOldVideoFile(String videoUrl) {
        if (videoUrl == null || videoUrl.isEmpty()) return;
        try {
            String fileName = videoUrl.replaceFirst("^.*/", "");
            if (fileName.isEmpty()) return;
            File file = new File(new File(staticPath).getAbsoluteFile(), "videos/" + fileName);
            if (file.exists()) file.delete();
        } catch (Exception e) { /* ignore */ }
    }

    private void deleteOldModelFile(String modelUrl) {
        if (modelUrl == null || modelUrl.isEmpty()) return;
        if (modelUrl.contains(DEFAULT_MODEL)) return;
        try {
            String fileName = modelUrl.replaceFirst("^.*/", "");
            if (fileName.isEmpty()) return;
            File file = new File(new File(staticPath).getAbsoluteFile(), "models/" + fileName);
            if (file.exists()) file.delete();
        } catch (Exception e) { /* ignore */ }
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
        catalogCache.invalidateAfterCommit();
    }

    @Override
    public List<ProductVO> listAll() {
        return catalogCache.listAll(() -> toRatedProducts(productMapper.listAll()));
    }

    @Override
    public List<ProductVO> listAllAdmin() {
        return toRatedProducts(productMapper.listAllAdmin());
    }

    @Override
    public List<ProductVO> listByCategory(String category) {
        return catalogCache.listByCategory(category, () -> toRatedProducts(productMapper.listByCategory(category)));
    }

    @Override
    public ProductVO getById(Long id) {
        ProductVO result = catalogCache.getById(id, () -> {
            Product product = productMapper.getById(id);
            return product == null ? null : enrichWithRating(toVO(product));
        });
        if (result == null) {
            throw new OrderBusinessException(MessageConstant.PRODUCT_NOT_FOUND);
        }
        return result;
    }

    @Override
    public List<ProductVO> listByInheritor(Long inheritorId) {
        return toRatedProducts(productMapper.listByInheritor(inheritorId));
    }

    @Override
    public void deleteByAdmin(Long id) {
        productMapper.deleteById(id);
        catalogCache.invalidateAfterCommit();
    }

    @Override
    public void toggleByAdmin(Long id, Integer status) {
        Product product = Product.builder().id(id).status(status).build();
        productMapper.update(product);
        catalogCache.invalidateAfterCommit();
    }

    @Override
    public void batchToggle(java.util.List<Long> ids, Integer status) {
        for (Long id : ids) {
            toggleByAdmin(id, status);
        }
    }

    @Override
    public void batchDelete(java.util.List<Long> ids) {
        for (Long id : ids) {
            deleteByAdmin(id);
        }
    }

    private List<ProductVO> toRatedProducts(List<Product> products) {
        return products.stream().map(this::toVO).map(this::enrichWithRating).collect(Collectors.toList());
    }

    private ProductVO toVO(Product product) {
        return ProductVO.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stock(product.getStock())
                .imageUrl(product.getImageUrl())
                .modelUrl(product.getModelUrl())
                .videoUrl(product.getVideoUrl())
                .category(product.getCategory())
                .inheritorId(product.getInheritorId())
                .inheritorName(product.getInheritorName())
                .status(product.getStatus())
                .createTime(product.getCreateTime())
                .build();
    }

    /** 填充评分信息 */
    private ProductVO enrichWithRating(ProductVO vo) {
        try {
            Double avg = reviewService.getAvgRating(vo.getId());
            Long count = reviewService.countByProductId(vo.getId());
            vo.setAvgRating(avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0);
            vo.setReviewCount(count != null ? count : 0L);
        } catch (Exception e) {
            // 评分查询失败不影响主流程
            vo.setAvgRating(0.0);
            vo.setReviewCount(0L);
        }
        return vo;
    }
}
