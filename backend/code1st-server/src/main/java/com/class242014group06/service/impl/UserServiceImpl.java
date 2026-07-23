package com.class242014group06.service.impl;

import com.class242014group06.constant.MessageConstant;
import com.class242014group06.constant.StatusConstant;
import com.class242014group06.dto.UserProfileDTO;
import com.class242014group06.entity.User;
import com.class242014group06.exception.AccountLockedException;
import com.class242014group06.exception.AccountNotFoundException;
import com.class242014group06.exception.PasswordErrorException;
import com.class242014group06.exception.RegistrationFailedException;
import com.class242014group06.mapper.AdminMapper;
import com.class242014group06.mapper.InheritorMapper;
import com.class242014group06.mapper.UserMapper;
import com.class242014group06.service.EmailService;
import org.springframework.beans.factory.annotation.Value;
import java.io.File;
import com.class242014group06.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private AdminMapper adminMapper;
    @Autowired
    private InheritorMapper inheritorMapper;
    @Autowired
    private EmailService emailService;

    /** 验证码存储：key=userId, value=[验证码, 过期时间戳(ms)] */
    private final ConcurrentHashMap<Long, String[]> codeStore = new ConcurrentHashMap<>();

    /** 验证码有效期：5 分钟 */
    private static final long CODE_TTL_MS = 5 * 60 * 1000L;

    @Override
    public User login(String username, String password) {
        User user = userMapper.getByUsername(username);
        if (user == null) {
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }
        password = DigestUtils.md5DigestAsHex(password.getBytes());
        if (!password.equals(user.getPassword())) {
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }
        if (user.getStatus() == StatusConstant.DISABLE) {
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
        }
        return user;
    }

    @Override
    public User getById(Long id) {
        return userMapper.getById(id);
    }

    @Override
    public void updateProfile(Long id, UserProfileDTO dto) {
        // 如果要修改用户名，检查是否与其他用户冲突
        if (dto.getUsername() != null && !dto.getUsername().isEmpty()) {
            User currentUser = userMapper.getById(id);
            if (!dto.getUsername().equals(currentUser.getUsername())) {
                // 跨表检查用户名是否已存在
                if (userMapper.getByUsername(dto.getUsername()) != null
                        || inheritorMapper.getByUsername(dto.getUsername()) != null
                        || adminMapper.getByUsername(dto.getUsername()) != null) {
                    throw new RegistrationFailedException(MessageConstant.ALREADY_EXISTS);
                }
            }
        }

        // 删除旧头像文件
        if (dto.getAvatarUrl() != null && !dto.getAvatarUrl().isEmpty()) {
            User old = userMapper.getById(id);
            if (old != null && old.getAvatarUrl() != null && !old.getAvatarUrl().equals(dto.getAvatarUrl())) {
                deleteAvatarFile(old.getAvatarUrl());
            }
        }

        User user = User.builder()
                .id(id)
                .username(dto.getUsername())
                .name(dto.getName())
                .phone(dto.getPhone())
                .email(dto.getEmail())
                .sex(dto.getSex())
                .avatarUrl(dto.getAvatarUrl())
                .build();
        userMapper.update(user);
    }

    @Override
    public void sendChangePasswordCode(Long id) {
        User user = userMapper.getById(id);
        if (user == null) {
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }
        if (user.getStatus() == StatusConstant.DISABLE) {
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
        }

        // 获取邮箱
        String email = user.getEmail();
        if (email == null || email.isEmpty()) {
            email = "3326353413@qq.com";
        }

        // 生成 6 位数字验证码
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
        long expireTime = System.currentTimeMillis() + CODE_TTL_MS;

        // 存储验证码（按用户ID）
        codeStore.put(id, new String[]{code, String.valueOf(expireTime)});

        // 发送邮件
        emailService.sendVerificationCode(email, code);
    }

    @Override
    public void changePassword(Long id, String code, String newPassword) {
        // 校验验证码
        String[] entry = codeStore.get(id);
        if (entry == null) {
            throw new PasswordErrorException(MessageConstant.VERIFICATION_CODE_NOT_SENT);
        }

        long expireTime = Long.parseLong(entry[1]);
        if (System.currentTimeMillis() > expireTime) {
            codeStore.remove(id);
            throw new PasswordErrorException(MessageConstant.VERIFICATION_CODE_EXPIRED);
        }

        if (!code.equals(entry[0])) {
            throw new PasswordErrorException(MessageConstant.VERIFICATION_CODE_ERROR);
        }

        // 验证码正确，修改密码
        String md5Password = DigestUtils.md5DigestAsHex(newPassword.getBytes());
        userMapper.updatePassword(id, md5Password, LocalDateTime.now());

        // 清除验证码
        codeStore.remove(id);
    }

    @Override
    public void changePasswordWithOld(Long id, String oldPassword, String newPassword) {
        User user = userMapper.getById(id);
        if (user == null) {
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }
        // 校验旧密码
        String oldMd5 = DigestUtils.md5DigestAsHex(oldPassword.getBytes());
        if (!oldMd5.equals(user.getPassword())) {
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }
        // 更新为新密码
        String md5Password = DigestUtils.md5DigestAsHex(newPassword.getBytes());
        userMapper.updatePassword(id, md5Password, LocalDateTime.now());
    }

    @Value("${code1st.upload.path:./uploads}")
    private String uploadPath;

    private void deleteAvatarFile(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isEmpty()) return;
        try {
            // avatarUrl 格式如 /uploads/avatar/xxx.jpg
            String fileName = avatarUrl.replaceFirst("^/uploads/", "");
            File file = new File(new File(uploadPath).getAbsoluteFile(), fileName);
            if (file.exists()) file.delete();
        } catch (Exception e) { /* ignore */ }
    }
}
