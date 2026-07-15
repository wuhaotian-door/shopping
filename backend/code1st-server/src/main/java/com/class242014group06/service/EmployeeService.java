package com.class242014group06.service;


import com.class242014group06.dto.EmployeeDTO;
import com.class242014group06.dto.EmployeeLoginDTO;
import com.class242014group06.dto.EmployeeRegisterDTO;
import com.class242014group06.dto.InheritorRegisterDTO;
import com.class242014group06.entity.Employee;

import java.util.List;
import java.util.Map;

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
     * 获取待审核的传承人列表
     * @return
     */
    List<Employee> getPendingList();

    /**
     * 审核通过传承人
     * @param id
     */
    void approveInheritor(Long id);

    /**
     * 驳回传承人申请
     * @param id
     */
    void rejectInheritor(Long id);

    /**
     * 查询所有员工
     * @return
     */
    List<Employee> listAll();

    /**
     * 根据关键字搜索员工
     * @param keyword
     * @return
     */
    List<Employee> search(String keyword);

    /**
     * 修改员工状态
     * @param id
     * @param status
     */
    void updateStatus(Long id, Integer status);

    /**
     * 获取仪表盘统计数据
     * @return Map包含各维度统计数量
     */
    Map<String, Long> getStatistics();

    /**
     * 员工分页查询
     * @param employeePageQueryDTO
     * @return
     */

}
