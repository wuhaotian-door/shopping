package com.class242014group06.mapper;

import com.class242014group06.annotation.AutoFill;
import com.class242014group06.entity.ProductOrder;
import com.class242014group06.enumeration.OperationType;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ProductOrderMapper {

    /**
     * 新增商品订单
     */
    @AutoFill(value = OperationType.INSERT)
    @Insert("insert into product_order (user_id, user_name, phone, address, product_id, product_name, product_image, " +
            "price, quantity, total_amount, inheritor_id, remark, status, create_time, update_time, create_user, update_user) " +
            "values (#{userId}, #{userName}, #{phone}, #{address}, #{productId}, #{productName}, #{productImage}, " +
            "#{price}, #{quantity}, #{totalAmount}, #{inheritorId}, #{remark}, #{status}, #{createTime}, #{updateTime}, #{createUser}, #{updateUser})")
    void insert(ProductOrder order);

    /**
     * 根据主键动态修改属性
     */
    @AutoFill(value = OperationType.UPDATE)
    void update(ProductOrder order);

    /**
     * 根据ID查询订单
     */
    @Select("select * from product_order where id = #{id}")
    ProductOrder getById(Long id);

    /**
     * 根据用户ID查询该用户的所有商品订单
     */
    @Select("select * from product_order where user_id = #{userId} order by create_time desc")
    List<ProductOrder> listByUserId(Long userId);

    /**
     * 根据传承人ID查询该传承人的所有商品订单
     */
    @Select("select * from product_order where inheritor_id = #{inheritorId} order by create_time desc")
    List<ProductOrder> listByInheritorId(Long inheritorId);

    @Select("select * from product_order order by create_time desc")
    List<ProductOrder> listAll();
}
