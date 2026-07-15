package com.class242014group06.mapper;

import com.class242014group06.annotation.AutoFill;
import com.class242014group06.entity.Employee;


import com.class242014group06.enumeration.OperationType;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface EmployeeMapper {

    /**
     * 根据用户名查询员工
     * @param username
     * @return
     */
    @Select("select * from employee where username = #{username}")
    Employee getByUsername(String username);

    /**
     * 新增员工
     * @param employee
     */
    @AutoFill(value = OperationType.INSERT)
    @Insert("insert into employee (name, username, password, phone, sex, id_number, role, create_time, update_time, create_user, update_user, status) " +
            "values " +
            "(#{name},#{username},#{password},#{phone},#{sex},#{idNumber},#{role},#{createTime},#{updateTime},#{createUser},#{updateUser},#{status})")
    void insert(Employee employee);


    /**
     * 根据主键动态修改属性
     * @param employee
     */
    @AutoFill(value = OperationType.UPDATE)
    void update(Employee employee);

    /**
     * 根据id查询员工
     * @param id
     * @return
     */
    @Select("select * from employee where id = #{id}")
    Employee getById(Long id);

    /**
     * 根据状态查询员工列表
     * @param status
     * @return
     */
    @Select("select * from employee where status = #{status} order by create_time desc")
    List<Employee> getByStatus(Integer status);

    /**
     * 根据角色和状态查询员工列表（用于首页展示已启用的传承人）
     * @param role
     * @param status
     * @return
     */
    @Select("select * from employee where role = #{role} and status = #{status} order by create_time desc")
    List<Employee> getByRoleAndStatus(String role, Integer status);
}
