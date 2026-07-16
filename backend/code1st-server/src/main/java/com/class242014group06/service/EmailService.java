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
}
