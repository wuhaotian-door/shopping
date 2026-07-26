package com.class242014group06.service;

import com.class242014group06.entity.BrowseHistory;

import java.util.List;

/**
 * 浏览记录服务
 */
public interface BrowseHistoryService {

    /** 记录一次浏览 */
    void record(Long userId, String userRole, String targetType, Long targetId,
                String targetTitle, String targetCover, String targetPath);

    /** 查询用户浏览记录（去重，最近50条） */
    List<BrowseHistory> listByUser(Long userId, String userRole);

    /** 删除单条记录 */
    void deleteById(Long id);

    /** 清空用户全部浏览记录 */
    void deleteAllByUser(Long userId, String userRole);
}
