package com.class242014group06.service;

import com.class242014group06.entity.User;

/**
 * 注册用户服务
 */
public interface UserService {

    User login(String username, String password);
}
