package com.class242014group06.mapper;

import com.class242014group06.annotation.AutoFill;
import com.class242014group06.entity.User;
import com.class242014group06.enumeration.OperationType;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 注册用户 Mapper
 */
@Mapper
public interface UserMapper {

    @Select("select * from user where username = #{username}")
    User getByUsername(String username);

    @AutoFill(value = OperationType.INSERT)
    @Insert("insert into user (name, username, password, phone, email, sex, avatar_url, create_time, update_time, create_user, update_user, status) " +
            "values (#{name},#{username},#{password},#{phone},#{email},#{sex},#{avatarUrl},#{createTime},#{updateTime},#{createUser},#{updateUser},#{status})")
    void insert(User user);

    @Select("select * from user where id = #{id}")
    User getById(Long id);

    @AutoFill(value = OperationType.UPDATE)
    void update(User user);

    @Update("update user set password = #{password}, update_time = #{updateTime} where id = #{id}")
    void updatePassword(@org.apache.ibatis.annotations.Param("id") Long id, @org.apache.ibatis.annotations.Param("password") String password, @org.apache.ibatis.annotations.Param("updateTime") java.time.LocalDateTime updateTime);

    @Select("select * from user order by create_time desc")
    List<User> listAll();

    @Delete("delete from user where id = #{id}")
    void deleteById(Long id);

    @Select("select * from user where name like concat('%', #{keyword}, '%') or username like concat('%', #{keyword}, '%') order by create_time desc")
    List<User> searchByKeyword(String keyword);

    @Select("select count(*) from user")
    Long countAll();

    @Select("select count(*) from user where status = #{status}")
    Long countByStatus(Integer status);
}
