package com.class242014group06.service.impl;

import com.class242014group06.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * 邮件发送服务实现
 * <p>
 * 如果 SMTP 授权码未正确配置，邮件发送会失败，
 * 此时验证码会输出到日志中，方便开发测试。
 */
@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:18155255201@163.com}")
    private String fromEmail;

    @Override
    public void sendVerificationCode(String toEmail, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("【码上非遗】密码重置验证码");
        message.setText("您正在重置密码，验证码为：" + code + "\n\n"
                + "验证码有效期为 5 分钟，请尽快使用。\n"
                + "如非本人操作，请忽略此邮件。");

        try {
            mailSender.send(message);
            log.info("验证码邮件已发送至 {}", toEmail);
        } catch (Exception e) {
            // 邮件发送失败（通常是授权码未配置），将验证码输出到日志方便测试
            log.warn("邮件发送失败，请检查 SMTP 配置。验证码 [{}] 已输出到日志，可手动查看。收件人: {}", code, toEmail);
            log.warn("异常详情: {}", e.getMessage());
        }
    }

    @Override
    public void sendRejectNotification(String toEmail, String name, String reason) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("【码上非遗】非遗传承人申请审核结果通知");
        message.setText(name + "，您好！\n\n"
                + "您提交的非遗传承人入驻申请经审核未被通过。\n\n"
                + "驳回原因：" + reason + "\n\n"
                + "如有疑问，请联系平台管理员。\n"
                + "您可以修改资料后重新提交申请。\n\n"
                + "—— 码上非遗平台");

        try {
            mailSender.send(message);
            log.info("驳回通知邮件已发送至 {}", toEmail);
        } catch (Exception e) {
            log.warn("驳回通知邮件发送失败，收件人: {}，异常: {}", toEmail, e.getMessage());
        }
    }

    @Override
    public void sendApprovalNotification(String toEmail, String name) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("【码上非遗】非遗传承人申请审核结果通知");
        message.setText(name + "，您好！\n\n"
                + "恭喜！您提交的非遗传承人入驻申请已审核通过。\n\n"
                + "您现在可以使用注册的账号登录平台，开始管理您的非遗项目、\n"
                + "上架非遗商品、发布非遗知识文章。\n\n"
                + "登录地址：请返回平台首页进行登录。\n\n"
                + "—— 码上非遗平台");

        try {
            mailSender.send(message);
            log.info("审核通过通知邮件已发送至 {}", toEmail);
        } catch (Exception e) {
            log.warn("审核通过通知邮件发送失败，收件人: {}，异常: {}", toEmail, e.getMessage());
        }
    }
}
