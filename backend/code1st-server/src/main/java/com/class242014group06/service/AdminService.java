package com.class242014group06.service;

import com.class242014group06.entity.Admin;

/**
 * 管理员服务
 */
public interface AdminService {

    Admin login(String username, String password);

    /**
     * 管理员修改自己的密码
     */
    void changePassword(Long adminId, String oldPassword, String newPassword);
}
