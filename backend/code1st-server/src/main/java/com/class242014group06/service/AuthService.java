package com.class242014group06.service;

import com.class242014group06.dto.EmployeeLoginDTO;
import com.class242014group06.dto.EmployeeRegisterDTO;
import com.class242014group06.dto.InheritorRegisterDTO;
import com.class242014group06.vo.EmployeeLoginVO;

/**
 * 统一认证服务 — 处理三端登录和注册
 */
public interface AuthService {

    /**
     * 统一登录：依次检查 admin → inheritor → user 三张表
     */
    EmployeeLoginVO login(EmployeeLoginDTO employeeLoginDTO);

    /**
     * 普通用户注册
     */
    void registerUser(EmployeeRegisterDTO dto);

    /**
     * 非遗传承人注册（含 region 字段）
     */
    void registerInheritor(InheritorRegisterDTO dto);
}
