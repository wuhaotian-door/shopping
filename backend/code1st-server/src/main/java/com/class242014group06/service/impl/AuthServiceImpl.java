package com.class242014group06.service.impl;

import com.class242014group06.constant.MessageConstant;
import com.class242014group06.constant.RoleConstant;
import com.class242014group06.constant.StatusConstant;
import com.class242014group06.dto.EmployeeLoginDTO;
import com.class242014group06.dto.EmployeeRegisterDTO;
import com.class242014group06.dto.ForgotPasswordDTO;
import com.class242014group06.dto.InheritorRegisterDTO;
import com.class242014group06.dto.ResetPasswordDTO;
import com.class242014group06.entity.Admin;
import com.class242014group06.entity.Inheritor;
import com.class242014group06.entity.User;
import com.class242014group06.exception.AccountLockedException;
import com.class242014group06.exception.AccountNotFoundException;
import com.class242014group06.exception.AccountPendingApprovalException;
import com.class242014group06.exception.PasswordErrorException;
import com.class242014group06.exception.RegistrationFailedException;
import com.class242014group06.mapper.AdminMapper;
import com.class242014group06.mapper.InheritorMapper;
import com.class242014group06.mapper.UserMapper;
import com.class242014group06.service.AuthService;
import com.class242014group06.service.EmailService;
import com.class242014group06.vo.EmployeeLoginVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    @Autowired
    private AdminMapper adminMapper;
    @Autowired
    private InheritorMapper inheritorMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private EmailService emailService;

    /** 验证码存储：key=username, value=[验证码, 过期时间戳(ms)] */
    private final ConcurrentHashMap<String, String[]> codeStore = new ConcurrentHashMap<>();

    /** 验证码有效期：5 分钟 */
    private static final long CODE_TTL_MS = 5 * 60 * 1000L;

    @Override
    public EmployeeLoginVO login(EmployeeLoginDTO dto) {
        String username = dto.getUsername();
        String password = DigestUtils.md5DigestAsHex(dto.getPassword().getBytes());

        // 1. 先查管理员表
        Admin admin = adminMapper.getByUsername(username);
        if (admin != null) {
            if (!password.equals(admin.getPassword())) {
                throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
            }
            if (admin.getStatus() == StatusConstant.DISABLE) {
                throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
            }
            return EmployeeLoginVO.builder()
                    .id(admin.getId())
                    .userName(admin.getUsername())
                    .name(admin.getName())
                    .role(RoleConstant.ADMIN)
                    .build();
        }

        // 2. 再查传承人表
        Inheritor inheritor = inheritorMapper.getByUsername(username);
        if (inheritor != null) {
            if (!password.equals(inheritor.getPassword())) {
                throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
            }
            if (inheritor.getStatus() == StatusConstant.DISABLE) {
                throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
            }
            if (inheritor.getStatus() == StatusConstant.PENDING) {
                throw new AccountPendingApprovalException(MessageConstant.ACCOUNT_PENDING);
            }
            return EmployeeLoginVO.builder()
                    .id(inheritor.getId())
                    .userName(inheritor.getUsername())
                    .name(inheritor.getName())
                    .role(RoleConstant.INHERITOR)
                    .build();
        }

        // 3. 最后查用户表
        User user = userMapper.getByUsername(username);
        if (user != null) {
            if (!password.equals(user.getPassword())) {
                throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
            }
            if (user.getStatus() == StatusConstant.DISABLE) {
                throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
            }
            return EmployeeLoginVO.builder()
                    .id(user.getId())
                    .userName(user.getUsername())
                    .name(user.getName())
                    .role(RoleConstant.USER)
                    .build();
        }

        throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
    }

    @Override
    public void registerUser(EmployeeRegisterDTO dto) {
        // 检查用户名是否已存在（跨表检查）
        if (userMapper.getByUsername(dto.getUsername()) != null
                || inheritorMapper.getByUsername(dto.getUsername()) != null
                || adminMapper.getByUsername(dto.getUsername()) != null) {
            throw new RegistrationFailedException(MessageConstant.ALREADY_EXISTS);
        }
        User user = new User();
        BeanUtils.copyProperties(dto, user);
        user.setPassword(DigestUtils.md5DigestAsHex(dto.getPassword().getBytes()));
        user.setStatus(StatusConstant.ENABLE);
        // 截断手机号防止超出数据库 VARCHAR(11) 限制
        if (user.getPhone() != null && user.getPhone().length() > 11) {
            user.setPhone(user.getPhone().substring(0, 11));
        }
        // 邮箱统一设置为默认值（用于找回密码）
        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            user.setEmail("3326353413@qq.com");
        }
        userMapper.insert(user);
    }

    @Override
    public void registerInheritor(InheritorRegisterDTO dto) {
        // 检查用户名是否已存在（跨表检查）
        if (userMapper.getByUsername(dto.getUsername()) != null
                || inheritorMapper.getByUsername(dto.getUsername()) != null
                || adminMapper.getByUsername(dto.getUsername()) != null) {
            throw new RegistrationFailedException(MessageConstant.ALREADY_EXISTS);
        }
        Inheritor inheritor = new Inheritor();
        BeanUtils.copyProperties(dto, inheritor);
        inheritor.setPassword(DigestUtils.md5DigestAsHex(dto.getPassword().getBytes()));
        inheritor.setStatus(StatusConstant.PENDING); // 传承人注册后待审核
        // 截断手机号防止超出数据库 VARCHAR(11) 限制
        if (inheritor.getPhone() != null && inheritor.getPhone().length() > 11) {
            inheritor.setPhone(inheritor.getPhone().substring(0, 11));
        }
        inheritorMapper.insert(inheritor);
    }

    @Override
    public void sendResetCode(ForgotPasswordDTO dto) {
        String username = dto.getUsername();

        // 查找用户（目前仅支持普通用户找回密码）
        User user = userMapper.getByUsername(username);
        if (user == null) {
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }
        if (user.getStatus() == StatusConstant.DISABLE) {
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
        }

        // 获取邮箱（统一默认值）
        String email = user.getEmail();
        if (email == null || email.isEmpty()) {
            email = "3326353413@qq.com";
        }

        // 生成 6 位数字验证码
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
        long expireTime = System.currentTimeMillis() + CODE_TTL_MS;

        // 存储验证码
        codeStore.put(username, new String[]{code, String.valueOf(expireTime)});

        // 发送邮件
        emailService.sendVerificationCode(email, code);
        log.info("密码重置验证码已生成，用户：{}", username);
    }

    @Override
    public void resetPassword(ResetPasswordDTO dto) {
        String username = dto.getUsername();
        String code = dto.getCode();
        String newPassword = dto.getNewPassword();

        // 校验验证码
        String[] entry = codeStore.get(username);
        if (entry == null) {
            throw new PasswordErrorException(MessageConstant.VERIFICATION_CODE_NOT_SENT);
        }

        long expireTime = Long.parseLong(entry[1]);
        if (System.currentTimeMillis() > expireTime) {
            codeStore.remove(username);
            throw new PasswordErrorException(MessageConstant.VERIFICATION_CODE_EXPIRED);
        }

        if (!code.equals(entry[0])) {
            throw new PasswordErrorException(MessageConstant.VERIFICATION_CODE_ERROR);
        }

        // 验证码正确，重置密码
        User user = userMapper.getByUsername(username);
        if (user == null) {
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        String md5Password = DigestUtils.md5DigestAsHex(newPassword.getBytes());
        userMapper.updatePassword(user.getId(), md5Password, LocalDateTime.now());

        // 清除验证码
        codeStore.remove(username);
        log.info("用户 {} 密码已重置", username);
    }
}
