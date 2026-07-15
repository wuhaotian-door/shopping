package com.class242014group06.mapper;

import com.class242014group06.annotation.AutoFill;
import com.class242014group06.entity.User;
import com.class242014group06.enumeration.OperationType;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 注册用户 Mapper
 */
@Mapper
public interface UserMapper {

    @Select("select * from user where username = #{username}")
    User getByUsername(String username);

    @AutoFill(value = OperationType.INSERT)
    @Insert("insert into user (name, username, password, phone, sex, create_time, update_time, create_user, update_user, status) " +
            "values (#{name},#{username},#{password},#{phone},#{sex},#{createTime},#{updateTime},#{createUser},#{updateUser},#{status})")
    void insert(User user);

    @Select("select * from user where id = #{id}")
    User getById(Long id);

    @AutoFill(value = OperationType.UPDATE)
    void update(User user);

    @Select("select * from user order by create_time desc")
    List<User> listAll();

    @Select("select * from user where name like concat('%', #{keyword}, '%') or username like concat('%', #{keyword}, '%') order by create_time desc")
    List<User> searchByKeyword(String keyword);

    @Select("select count(*) from user")
    Long countAll();

    @Select("select count(*) from user where status = #{status}")
    Long countByStatus(Integer status);
}
