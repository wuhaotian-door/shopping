package com.class242014group06.service;


import com.class242014group06.dto.EmployeeDTO;

import com.class242014group06.dto.EmployeeLoginDTO;

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
     * 员工分页查询
     * @param employeePageQueryDTO
     * @return
     */

}
