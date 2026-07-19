package com.class242014group06.service.impl;

import com.class242014group06.constant.MessageConstant;
import com.class242014group06.constant.StatusConstant;
import com.class242014group06.entity.Inheritor;
import com.class242014group06.exception.AccountLockedException;
import com.class242014group06.exception.AccountNotFoundException;
import com.class242014group06.exception.AccountPendingApprovalException;
import com.class242014group06.exception.PasswordErrorException;
import com.class242014group06.mapper.InheritorMapper;
import com.class242014group06.service.EmailService;
import com.class242014group06.service.InheritorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.List;

@Service
@Slf4j
public class InheritorServiceImpl implements InheritorService {

    @Autowired
    private InheritorMapper inheritorMapper;

    @Autowired
    private EmailService emailService;

    @Override
    public Inheritor login(String username, String password) {
        Inheritor inheritor = inheritorMapper.getByUsername(username);
        if (inheritor == null) {
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }
        password = DigestUtils.md5DigestAsHex(password.getBytes());
        if (!password.equals(inheritor.getPassword())) {
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }
        if (inheritor.getStatus() == StatusConstant.DISABLE) {
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
        }
        if (inheritor.getStatus() == StatusConstant.PENDING) {
            throw new AccountPendingApprovalException(MessageConstant.ACCOUNT_PENDING);
        }
        return inheritor;
    }

    @Override
    public List<Inheritor> getPendingList() {
        return inheritorMapper.getByStatus(StatusConstant.PENDING);
    }

    @Override
    public void approve(Long id) {
        // 获取传承人信息（用于发邮件）
        Inheritor inheritor = inheritorMapper.getById(id);
        if (inheritor == null) {
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }
        // 更新状态为启用
        Inheritor update = Inheritor.builder()
                .id(id)
                .status(StatusConstant.ENABLE)
                .build();
        inheritorMapper.update(update);

        // 发送审核通过通知邮件
        String email = inheritor.getEmail();
        if (email != null && !email.isEmpty()) {
            emailService.sendApprovalNotification(email, inheritor.getName());
        } else {
            log.warn("传承人 {} 未填写邮箱，无法发送审核通过通知", inheritor.getName());
        }
    }

    @Override
    public void reject(Long id) {
        Inheritor inheritor = Inheritor.builder()
                .id(id)
                .status(StatusConstant.DISABLE)
                .build();
        inheritorMapper.update(inheritor);
    }

    @Override
    public void reject(Long id, String reason) {
        // 获取传承人信息
        Inheritor inheritor = inheritorMapper.getById(id);
        if (inheritor == null) {
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }
        // 更新状态为禁用
        Inheritor update = Inheritor.builder()
                .id(id)
                .status(StatusConstant.DISABLE)
                .build();
        inheritorMapper.update(update);

        // 发送驳回通知邮件
        String email = inheritor.getEmail();
        if (email != null && !email.isEmpty()) {
            emailService.sendRejectNotification(email, inheritor.getName(),
                    reason != null && !reason.isEmpty() ? reason : "未提供具体原因");
        } else {
            log.warn("传承人 {} 未填写邮箱，无法发送驳回通知", inheritor.getName());
        }
    }

    @Override
    public void changePassword(Long inheritorId, String oldPassword, String newPassword) {
        Inheritor inheritor = inheritorMapper.getById(inheritorId);
        if (inheritor == null) {
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }
        // 校验旧密码
        String oldMd5 = DigestUtils.md5DigestAsHex(oldPassword.getBytes());
        if (!oldMd5.equals(inheritor.getPassword())) {
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }
        // 更新为新密码
        Inheritor update = Inheritor.builder()
                .id(inheritorId)
                .password(DigestUtils.md5DigestAsHex(newPassword.getBytes()))
                .build();
        inheritorMapper.update(update);
    }

    @Override
    public int batchApprove(java.util.List<Long> ids) {
        int count = 0;
        for (Long id : ids) {
            Inheritor inheritor = inheritorMapper.getById(id);
            if (inheritor != null && inheritor.getStatus().equals(StatusConstant.PENDING)) {
                approve(id);
                count++;
            }
        }
        return count;
    }

    @Override
    public int batchReject(java.util.List<Long> ids, String reason) {
        int count = 0;
        for (Long id : ids) {
            Inheritor inheritor = inheritorMapper.getById(id);
            if (inheritor != null && inheritor.getStatus().equals(StatusConstant.PENDING)) {
                reject(id, reason);
                count++;
            }
        }
        return count;
    }
}
