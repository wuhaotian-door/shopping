package com.class242014group06.service;

/**
 * 邮件发送服务
 */
public interface EmailService {

    /**
     * 发送密码重置验证码邮件
     *
     * @param toEmail 收件人邮箱
     * @param code    验证码
     */
    void sendVerificationCode(String toEmail, String code);

    /**
     * 发送传承人申请驳回通知邮件
     *
     * @param toEmail 收件人邮箱
     * @param name    申请人姓名
     * @param reason  驳回原因
     */
    void sendRejectNotification(String toEmail, String name, String reason);

    /**
     * 发送传承人申请审核通过通知邮件
     *
     * @param toEmail 收件人邮箱
     * @param name    申请人姓名
     */
    void sendApprovalNotification(String toEmail, String name);
}
