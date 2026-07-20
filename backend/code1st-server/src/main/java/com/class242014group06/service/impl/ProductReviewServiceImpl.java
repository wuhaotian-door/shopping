package com.class242014group06.service.impl;

import com.class242014group06.dto.ProductReviewDTO;
import com.class242014group06.entity.Admin;
import com.class242014group06.entity.Inheritor;
import com.class242014group06.entity.ProductReview;
import com.class242014group06.entity.User;
import com.class242014group06.exception.OrderBusinessException;
import com.class242014group06.mapper.AdminMapper;
import com.class242014group06.mapper.InheritorMapper;
import com.class242014group06.mapper.ProductReviewMapper;
import com.class242014group06.mapper.UserMapper;
import com.class242014group06.service.ProductReviewService;
import com.class242014group06.vo.ProductReviewVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProductReviewServiceImpl implements ProductReviewService {

    @Autowired
    private ProductReviewMapper reviewMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private InheritorMapper inheritorMapper;

    @Autowired
    private AdminMapper adminMapper;

    @Override
    public void addReview(Long userId, Long productId, ProductReviewDTO dto) {
        // 参数校验：顶级评价必须有评分，回复无需评分
        if (dto.getParentId() == null) {
            if (dto.getRating() == null || dto.getRating() < 1 || dto.getRating() > 5) {
                throw new OrderBusinessException("评分必须在1-5之间");
            }
        }
        if (dto.getContent() == null || dto.getContent().trim().isEmpty()) {
            throw new OrderBusinessException("评价内容不能为空");
        }

        // 校验父评价（如果传了 parentId）
        if (dto.getParentId() != null) {
            ProductReview parent = reviewMapper.findById(dto.getParentId());
            if (parent == null || !parent.getProductId().equals(productId)) {
                throw new OrderBusinessException("父评价不存在");
            }
        }

        // 跨表查询用户名（支持普通用户/传承人/管理员评价）
        String userName = "";
        User user = userMapper.getById(userId);
        if (user != null) {
            userName = user.getName();
        } else {
            Inheritor inheritor = inheritorMapper.getById(userId);
            if (inheritor != null) {
                userName = inheritor.getName();
            } else {
                Admin admin = adminMapper.getById(userId);
                if (admin != null) {
                    userName = admin.getName();
                }
            }
        }

        ProductReview review = ProductReview.builder()
                .productId(productId)
                .userId(userId)
                .userName(userName)
                .rating(dto.getRating())
                .content(dto.getContent().trim())
                .parentId(dto.getParentId())
                .isDeleted(0)
                .createTime(LocalDateTime.now())
                .build();
        reviewMapper.insert(review);
    }

    @Override
    public List<ProductReviewVO> listByProductId(Long productId) {
        List<ProductReview> reviews = reviewMapper.listByProductId(productId);
        List<ProductReviewVO> flatVos = reviews.stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return buildTree(flatVos);
    }

    @Override
    public Double getAvgRating(Long productId) {
        return reviewMapper.getAvgRating(productId);
    }

    @Override
    public Long countByProductId(Long productId) {
        return reviewMapper.countByProductId(productId);
    }

    @Override
    public void deleteReview(Long userId, Long reviewId) {
        ProductReview review = reviewMapper.findById(reviewId);
        if (review == null) {
            throw new OrderBusinessException("评价不存在");
        }
        if (!review.getUserId().equals(userId)) {
            throw new OrderBusinessException("无权删除他人评价");
        }
        reviewMapper.deleteById(reviewId);
    }

    /**
     * 将扁平评价列表构建为树形结构
     */
    private List<ProductReviewVO> buildTree(List<ProductReviewVO> flatList) {
        Map<Long, ProductReviewVO> map = new LinkedHashMap<>();
        List<ProductReviewVO> roots = new ArrayList<>();

        // 第一遍：建立 ID 映射并初始化 children
        for (ProductReviewVO vo : flatList) {
            vo.setChildren(new ArrayList<>());
            map.put(vo.getId(), vo);
        }

        // 第二遍：设置 replyToUserName 并挂到父节点
        for (ProductReviewVO vo : flatList) {
            if (vo.getParentId() != null) {
                ProductReviewVO parent = map.get(vo.getParentId());
                if (parent != null) {
                    vo.setReplyToUserName(
                            parent.getIsDeleted() != null && parent.getIsDeleted() == 1
                                    ? "已删除用户"
                                    : parent.getUserName()
                    );
                    parent.getChildren().add(vo);
                } else {
                    // 父评价被删除或孤立，降级为顶级评价
                    roots.add(vo);
                }
            } else {
                roots.add(vo);
            }
        }
        return roots;
    }

    private ProductReviewVO toVO(ProductReview review) {
        return ProductReviewVO.builder()
                .id(review.getId())
                .productId(review.getProductId())
                .userId(review.getUserId())
                .userName(review.getUserName())
                .rating(review.getRating())
                .content(review.getContent())
                .parentId(review.getParentId())
                .isDeleted(review.getIsDeleted())
                .createTime(review.getCreateTime())
                .build();
        // replyToUserName 和 children 在 buildTree() 中设置
    }
}
