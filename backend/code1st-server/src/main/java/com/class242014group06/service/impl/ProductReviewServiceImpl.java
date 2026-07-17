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
import java.util.List;
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
        // 参数校验
        if (dto.getRating() == null || dto.getRating() < 1 || dto.getRating() > 5) {
            throw new OrderBusinessException("评分必须在1-5之间");
        }
        if (dto.getContent() == null || dto.getContent().trim().isEmpty()) {
            throw new OrderBusinessException("评价内容不能为空");
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
                .createTime(LocalDateTime.now())
                .build();
        reviewMapper.insert(review);
    }

    @Override
    public List<ProductReviewVO> listByProductId(Long productId) {
        List<ProductReview> reviews = reviewMapper.listByProductId(productId);
        return reviews.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public Double getAvgRating(Long productId) {
        return reviewMapper.getAvgRating(productId);
    }

    @Override
    public Long countByProductId(Long productId) {
        return reviewMapper.countByProductId(productId);
    }

    private ProductReviewVO toVO(ProductReview review) {
        return ProductReviewVO.builder()
                .id(review.getId())
                .productId(review.getProductId())
                .userId(review.getUserId())
                .userName(review.getUserName())
                .rating(review.getRating())
                .content(review.getContent())
                .createTime(review.getCreateTime())
                .build();
    }
}
