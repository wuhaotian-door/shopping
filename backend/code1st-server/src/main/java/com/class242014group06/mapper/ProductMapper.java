package com.class242014group06.mapper;

import com.class242014group06.annotation.AutoFill;
import com.class242014group06.entity.Product;
import com.class242014group06.enumeration.OperationType;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ProductMapper {

    /**
     * 新增商品
     */
    @AutoFill(value = OperationType.INSERT)
    @Insert("insert into product (name, description, price, stock, image_url, model_url, video_url, category, inheritor_id, inheritor_name, " +
            "status, create_time, update_time, create_user, update_user) " +
            "values (#{name}, #{description}, #{price}, #{stock}, #{imageUrl}, #{modelUrl}, #{videoUrl}, #{category}, #{inheritorId}, #{inheritorName}, " +
            "#{status}, #{createTime}, #{updateTime}, #{createUser}, #{updateUser})")
    void insert(Product product);

    /**
     * 根据主键动态修改属性
     */
    @AutoFill(value = OperationType.UPDATE)
    void update(Product product);

    /**
     * 根据ID查询商品
     */
    @Select("select * from product where id = #{id}")
    Product getById(Long id);

    /**
     * 查询所有上架商品，按创建时间倒序
     */
    @Select("select * from product where status = 1 order by create_time desc")
    List<Product> listAll();

    /**
     * 管理员查询所有商品（含上下架），按创建时间倒序
     */
    @Select("select * from product order by create_time desc")
    List<Product> listAllAdmin();

    /**
     * 根据分类查询上架商品
     */
    @Select("select * from product where status = 1 and category = #{category} order by create_time desc")
    List<Product> listByCategory(String category);

    /**
     * 查询传承人自己的所有商品（含上下架）
     */
    @Select("select * from product where inheritor_id = #{inheritorId} order by create_time desc")
    List<Product> listByInheritor(Long inheritorId);

    @Delete("delete from product where id = #{id}")
    void deleteById(Long id);
}
