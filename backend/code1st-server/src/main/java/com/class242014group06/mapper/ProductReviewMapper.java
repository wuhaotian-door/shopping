package com.class242014group06.mapper;

import com.class242014group06.entity.ProductReview;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 商品评价 Mapper
 */
@Mapper
public interface ProductReviewMapper {

    @Insert("insert into product_review (product_id, user_id, user_name, rating, content, create_time) " +
            "values (#{productId}, #{userId}, #{userName}, #{rating}, #{content}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(ProductReview review);

    @Select("select * from product_review where product_id = #{productId} order by create_time desc")
    List<ProductReview> listByProductId(Long productId);

    @Select("select coalesce(avg(rating), 0) from product_review where product_id = #{productId}")
    Double getAvgRating(Long productId);

    @Select("select count(*) from product_review where product_id = #{productId}")
    Long countByProductId(Long productId);
}
