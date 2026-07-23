package com.class242014group06.service;

import com.class242014group06.entity.UserReport;

import java.util.List;

public interface UserReportService {

    /** 提交举报 */
    void submit(UserReport report);

    /** 获取所有举报列表 */
    List<UserReport> listAll();

    /** 处理举报（封号或驳回） */
    void handle(Long reportId, String handleResult, Integer banDays);
}
