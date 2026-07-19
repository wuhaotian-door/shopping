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

    /**
     * 驳回传承人申请并发送邮件通知
     *
     * @param id     传承人ID
     * @param reason 驳回原因
     */
    void reject(Long id, String reason);

    /**
     * 传承人修改自己的密码（旧密码校验）
     */
    void changePassword(Long inheritorId, String oldPassword, String newPassword);

    /**
     * 批量审核通过
     */
    int batchApprove(java.util.List<Long> ids);

    /**
     * 批量驳回并发送邮件通知
     */
    int batchReject(java.util.List<Long> ids, String reason);
}
