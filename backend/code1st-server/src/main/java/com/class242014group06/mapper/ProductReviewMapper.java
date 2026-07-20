package com.class242014group06.mapper;

import com.class242014group06.entity.ProductReview;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 商品评价 Mapper
 */
@Mapper
public interface ProductReviewMapper {

    @Insert("insert into product_review (product_id, user_id, user_name, rating, content, parent_id, is_deleted, create_time) " +
            "values (#{productId}, #{userId}, #{userName}, #{rating}, #{content}, #{parentId}, #{isDeleted}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(ProductReview review);

    @Select("select * from product_review where id = #{id}")
    ProductReview findById(@Param("id") Long id);

    @Update("update product_review set is_deleted = 1 where id = #{id}")
    void deleteById(@Param("id") Long id);

    @Select("select * from product_review where product_id = #{productId} order by create_time desc")
    List<ProductReview> listByProductId(Long productId);

    @Select("select coalesce(avg(rating), 0) from product_review where product_id = #{productId} and is_deleted = 0")
    Double getAvgRating(Long productId);

    @Select("select count(*) from product_review where product_id = #{productId} and is_deleted = 0")
    Long countByProductId(Long productId);
}
