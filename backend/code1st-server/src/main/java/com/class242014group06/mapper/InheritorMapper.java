package com.class242014group06.mapper;

import com.class242014group06.annotation.AutoFill;
import com.class242014group06.entity.Inheritor;
import com.class242014group06.enumeration.OperationType;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 非遗传承人 Mapper
 */
@Mapper
public interface InheritorMapper {

    @Select("select * from inheritor where username = #{username}")
    Inheritor getByUsername(String username);

    @AutoFill(value = OperationType.INSERT)
    @Insert("insert into inheritor (name, username, password, phone, sex, id_number, region, birth_date, email, age, avatar_url, create_time, update_time, create_user, update_user, status) " +
            "values (#{name},#{username},#{password},#{phone},#{sex},#{idNumber},#{region},#{birthDate},#{email},#{age},#{avatarUrl},#{createTime},#{updateTime},#{createUser},#{updateUser},#{status})")
    void insert(Inheritor inheritor);

    @Select("select * from inheritor where id = #{id}")
    Inheritor getById(Long id);

    @Select("select * from inheritor where status = #{status} order by create_time desc")
    List<Inheritor> getByStatus(Integer status);

    @AutoFill(value = OperationType.UPDATE)
    void update(Inheritor inheritor);

    @Select("select * from inheritor order by create_time desc")
    List<Inheritor> listAll();

    @Select("select * from inheritor where status = 1 order by create_time desc")
    List<Inheritor> getAllEnabled();

    @Select("select * from inheritor where name like concat('%', #{keyword}, '%') or username like concat('%', #{keyword}, '%') order by create_time desc")
    List<Inheritor> searchByKeyword(String keyword);

    @Select("select count(*) from inheritor")
    Long countAll();

    @Select("select count(*) from inheritor where status = #{status}")
    Long countByStatus(Integer status);

    @Update("update inheritor set password = #{password}, update_time = #{updateTime} where id = #{id}")
    void updatePassword(@org.apache.ibatis.annotations.Param("id") Long id, @org.apache.ibatis.annotations.Param("password") String password, @org.apache.ibatis.annotations.Param("updateTime") java.time.LocalDateTime updateTime);

    @Delete("delete from inheritor where id = #{id}")
    void deleteById(Long id);
}
