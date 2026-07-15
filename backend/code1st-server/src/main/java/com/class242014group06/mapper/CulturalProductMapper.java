package com.class242014group06.mapper;

import com.class242014group06.annotation.AutoFill;
import com.class242014group06.entity.CulturalProduct;
import com.class242014group06.enumeration.OperationType;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CulturalProductMapper {

    /**
     * 新增产品
     */
    @AutoFill(value = OperationType.INSERT)
    @Insert("insert into cultural_product (inheritor_id, product_name, category, detail_brief, description, price, cover_url, stock, status, create_time, update_time, create_user, update_user) " +
            "values (#{inheritorId}, #{productName}, #{category}, #{detailBrief}, #{description}, #{price}, #{coverUrl}, #{stock}, #{status}, #{createTime}, #{updateTime}, #{createUser}, #{updateUser})")
    void insert(CulturalProduct product);

    /**
     * 动态更新产品
     */
    @AutoFill(value = OperationType.UPDATE)
    void update(CulturalProduct product);

    /**
     * 根据ID查询产品
     */
    @Select("select * from cultural_product where id = #{id}")
    CulturalProduct getById(Long id);

    /**
     * 根据传承人ID查询产品列表
     */
    @Select("select * from cultural_product where inheritor_id = #{inheritorId} order by create_time desc")
    List<CulturalProduct> getByInheritorId(Long inheritorId);

    /**
     * 查询所有已上架产品（首页展示）
     */
    @Select("select * from cultural_product where status = 1 order by create_time desc")
    List<CulturalProduct> listPublished();

    /**
     * 删除产品
     */
    @Select("delete from cultural_product where id = #{id}")
    void deleteById(Long id);
}
