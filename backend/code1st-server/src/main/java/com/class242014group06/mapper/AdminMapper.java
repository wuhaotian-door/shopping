package com.class242014group06.mapper;

import com.class242014group06.annotation.AutoFill;
import com.class242014group06.entity.Admin;
import com.class242014group06.enumeration.OperationType;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 管理员 Mapper
 */
@Mapper
public interface AdminMapper {

    @Select("select * from admin where username = #{username}")
    Admin getByUsername(String username);

    @AutoFill(value = OperationType.INSERT)
    @Insert("insert into admin (name, username, password, phone, sex, create_time, update_time, create_user, update_user, status) " +
            "values (#{name},#{username},#{password},#{phone},#{sex},#{createTime},#{updateTime},#{createUser},#{updateUser},#{status})")
    void insert(Admin admin);

    @Select("select * from admin where id = #{id}")
    Admin getById(Long id);

    @AutoFill(value = OperationType.UPDATE)
    void update(Admin admin);
}
