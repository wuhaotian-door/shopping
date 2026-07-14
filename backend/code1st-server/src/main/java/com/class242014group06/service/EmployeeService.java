package com.class242014group06.service;


import com.class242014group06.dto.EmployeeDTO;
import com.class242014group06.dto.EmployeeLoginDTO;
import com.class242014group06.dto.EmployeeRegisterDTO;
import com.class242014group06.dto.InheritorRegisterDTO;
import com.class242014group06.entity.Employee;

public interface EmployeeService {

    /**
     * 员工登录
     * @param employeeLoginDTO
     * @return
     */
    Employee login(EmployeeLoginDTO employeeLoginDTO);

    /**
     * 新增员工
     * @param employeeDTO
     */
    void save(EmployeeDTO employeeDTO);

    /**
     * 普通用户自行注册（无需登录）
     * @param employeeRegisterDTO
     */
    void register(EmployeeRegisterDTO employeeRegisterDTO);

    /**
     * 非遗传承人注册（无需登录，信息更详细）
     * @param inheritorRegisterDTO
     */
    void registerInheritor(InheritorRegisterDTO inheritorRegisterDTO);

    /**
     * 员工分页查询
     * @param employeePageQueryDTO
     * @return
     */

}
