package com.class242014group06.service;

import com.class242014group06.entity.Admin;

/**
 * 管理员服务
 */
public interface AdminService {

    Admin login(String username, String password);
}
