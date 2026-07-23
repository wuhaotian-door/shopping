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
    private com.class242014group06.mapper.FriendRelationMapper friendRelationMapper;
    @Autowired
    private com.class242014group06.mapper.ChatMapper chatMapper;
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
            checkBanAndAutoUnban(admin.getStatus(), admin.getBanUntil(),
                    () -> adminMapper.update(Admin.builder().id(admin.getId()).status(StatusConstant.ENABLE).banUntil(null).build()));
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
            if (inheritor.getStatus() == StatusConstant.PENDING) {
                throw new AccountPendingApprovalException(MessageConstant.ACCOUNT_PENDING);
            }
            checkBanAndAutoUnban(inheritor.getStatus(), inheritor.getBanUntil(),
                    () -> inheritorMapper.update(Inheritor.builder().id(inheritor.getId()).status(StatusConstant.ENABLE).banUntil(null).build()));
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
            checkBanAndAutoUnban(user.getStatus(), user.getBanUntil(),
                    () -> userMapper.update(User.builder().id(user.getId()).status(StatusConstant.ENABLE).banUntil(null).build()));
            return EmployeeLoginVO.builder()
                    .id(user.getId())
                    .userName(user.getUsername())
                    .name(user.getName())
                    .role(RoleConstant.USER)
                    .build();
        }

        throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
    }

    /** 检查封禁状态：过期自动解封并允许登录，未过期抛异常 */
    private void checkBanAndAutoUnban(Integer status, LocalDateTime banUntil, Runnable unbanAction) {
        if (status != StatusConstant.DISABLE) return;
        if (banUntil != null && banUntil.isBefore(LocalDateTime.now())) {
            // 封禁已过期，自动解封
            log.info("用户封禁已过期，自动解封");
            unbanAction.run();
            return;
        }
        String msg = MessageConstant.ACCOUNT_LOCKED;
        if (banUntil != null) {
            msg = "账号已被封禁至 " + banUntil.toLocalDate().toString();
        }
        throw new AccountLockedException(msg);
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
        inheritor.setStatus(StatusConstant.PENDING);
        // 根据出生日期计算年龄
        if (dto.getBirthDate() != null) {
            inheritor.setAge(java.time.Period.between(dto.getBirthDate(), java.time.LocalDate.now()).getYears());
        }
        // 截断手机号防止超出数据库 VARCHAR(11) 限制
        if (inheritor.getPhone() != null && inheritor.getPhone().length() > 11) {
            inheritor.setPhone(inheritor.getPhone().substring(0, 11));
        }
        inheritorMapper.insert(inheritor);
    }

    @Override
    public void sendResetCode(ForgotPasswordDTO dto) {
        String username = dto.getUsername();
        String email = null;
        String role = null;

        // 查找用户（支持三种角色）
        User user = userMapper.getByUsername(username);
        if (user != null) {
            if (user.getStatus() == StatusConstant.DISABLE) throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
            email = user.getEmail();
            role = "USER";
        } else {
            Inheritor inheritor = inheritorMapper.getByUsername(username);
            if (inheritor != null) {
                if (inheritor.getStatus() == StatusConstant.DISABLE) throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
                email = inheritor.getEmail();
                role = "INHERITOR";
            } else {
                Admin admin = adminMapper.getByUsername(username);
                if (admin != null) {
                    if (admin.getStatus() == StatusConstant.DISABLE) throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
                    email = null; // admin 表无 email 字段，使用下方默认值
                    role = "ADMIN";
                }
            }
        }

        if (role == null) {
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        if (email == null || email.isEmpty()) {
            email = "3326353413@qq.com";
        }

        // 生成 6 位数字验证码，存储时带上角色标记
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
        long expireTime = System.currentTimeMillis() + CODE_TTL_MS;
        codeStore.put(username, new String[]{code, String.valueOf(expireTime), role});

        emailService.sendVerificationCode(email, code);
        log.info("密码重置验证码已发送，username={}, role={}, email={}", username, role, email);
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

        String md5Password = DigestUtils.md5DigestAsHex(newPassword.getBytes());
        String role = entry.length > 2 ? entry[2] : "USER";

        // 根据角色更新对应表
        if ("INHERITOR".equals(role)) {
            Inheritor inheritor = inheritorMapper.getByUsername(username);
            if (inheritor == null) throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
            inheritorMapper.updatePassword(inheritor.getId(), md5Password, LocalDateTime.now());
        } else if ("ADMIN".equals(role)) {
            Admin admin = adminMapper.getByUsername(username);
            if (admin == null) throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
            adminMapper.updatePassword(admin.getId(), md5Password, LocalDateTime.now());
        } else {
            User user = userMapper.getByUsername(username);
            if (user == null) throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
            userMapper.updatePassword(user.getId(), md5Password, LocalDateTime.now());
        }

        codeStore.remove(username);
        log.info("用户 {} (角色={}) 密码已重置", username, role);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void deleteAccount(Long userId, String role) {
        log.info("注销账号：userId={}, role={}", userId, role);
        // 1. 删除好友关系（双向）
        friendRelationMapper.deleteByUser(userId);
        // 2. 删除聊天相关数据
        chatMapper.deleteByUser(userId, role);
        // 3. 删除用户/传承人记录
        if ("INHERITOR".equals(role)) {
            inheritorMapper.deleteById(userId);
        } else {
            userMapper.deleteById(userId);
        }
        log.info("账号已注销：userId={}, role={}", userId, role);
    }
}
