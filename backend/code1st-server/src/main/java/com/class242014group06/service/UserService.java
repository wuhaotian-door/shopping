package com.class242014group06.service;

import com.class242014group06.dto.UserProfileDTO;
import com.class242014group06.entity.User;

/**
 * 注册用户服务
 */
public interface UserService {

    User login(String username, String password);

    /**
     * 按 ID 查询用户
     */
    User getById(Long id);

    /**
     * 更新当前用户个人资料
     */
    void updateProfile(Long id, UserProfileDTO dto);

    /**
     * 发送修改密码验证码到当前用户邮箱
     */
    void sendChangePasswordCode(Long id);

    /**
     * 验证码校验通过后修改密码
     */
    void changePassword(Long id, String code, String newPassword);

    /**
     * 已登录用户用旧密码修改密码（无需验证码）
     */
    void changePasswordWithOld(Long id, String oldPassword, String newPassword);
}
