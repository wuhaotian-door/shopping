package com.class242014group06.service;

import com.class242014group06.dto.EmployeeLoginDTO;
import com.class242014group06.dto.EmployeeRegisterDTO;
import com.class242014group06.dto.ForgotPasswordDTO;
import com.class242014group06.dto.InheritorRegisterDTO;
import com.class242014group06.dto.ResetPasswordDTO;
import com.class242014group06.vo.EmployeeLoginVO;

/**
 * 统一认证服务 — 处理三端登录、注册和找回密码
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

    /**
     * 发送密码重置验证码到用户邮箱
     */
    void sendResetCode(ForgotPasswordDTO dto);

    /**
     * 验证码校验并重置密码
     */
    void resetPassword(ResetPasswordDTO dto);
}
