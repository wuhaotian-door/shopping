package com.class242014group06.service;

import com.class242014group06.entity.Inheritor;
import com.class242014group06.entity.User;

import java.util.List;

/**
 * 非遗传承人服务
 */
public interface InheritorService {

    Inheritor login(String username, String password);

    List<Inheritor> getPendingList();

    void approve(Long id);

    void reject(Long id);
}
