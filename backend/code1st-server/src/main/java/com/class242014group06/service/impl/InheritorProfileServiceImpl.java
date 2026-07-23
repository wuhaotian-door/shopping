package com.class242014group06.service.impl;

import com.class242014group06.constant.MessageConstant;
import com.class242014group06.constant.StatusConstant;
import com.class242014group06.dto.InheritorProfileDTO;
import org.springframework.beans.factory.annotation.Value;
import java.io.File;
import com.class242014group06.entity.Inheritor;
import com.class242014group06.entity.InheritorProfile;
import com.class242014group06.exception.BaseException;
import com.class242014group06.mapper.InheritorMapper;
import com.class242014group06.mapper.InheritorProfileMapper;
import com.class242014group06.service.InheritorProfileService;
import com.class242014group06.vo.InheritorProfileVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class InheritorProfileServiceImpl implements InheritorProfileService {

    @Autowired
    private InheritorProfileMapper profileMapper;

    @Autowired
    private InheritorMapper inheritorMapper;

    @Override
    public InheritorProfileVO getByEmployeeId(Long inheritorId) {
        Inheritor inheritor = inheritorMapper.getById(inheritorId);
        if (inheritor == null) {
            throw new BaseException(MessageConstant.ACCOUNT_NOT_FOUND);
        }
        InheritorProfile profile = profileMapper.getByEmployeeId(inheritorId);
        return buildVO(inheritor, profile);
    }

    @Override
    public void saveOrUpdate(Long inheritorId, InheritorProfileDTO dto) {
        // 处理出生日期更新（带一年限制）
        Inheritor inheritor = inheritorMapper.getById(inheritorId);
        if (dto.getBirthDate() != null && inheritor != null) {
            LocalDate newBirth = dto.getBirthDate();
            // 检查是否变动
            if (!newBirth.equals(inheritor.getBirthDate())) {
                // 检查一年限制
                if (inheritor.getBirthDateUpdatedAt() != null) {
                    long daysSinceLastUpdate = ChronoUnit.DAYS.between(inheritor.getBirthDateUpdatedAt().toLocalDate(), LocalDate.now());
                    if (daysSinceLastUpdate < 365) {
                        throw new RuntimeException("出生日期一年只能修改一次，距离上次修改仅过了 " + daysSinceLastUpdate + " 天");
                    }
                }
                // 更新年龄
                inheritor.setAge(Period.between(newBirth, LocalDate.now()).getYears());
                inheritor.setBirthDate(newBirth);
                inheritor.setBirthDateUpdatedAt(LocalDateTime.now());
                inheritorMapper.update(inheritor);
            }
        }

        // 更新个人资料（含旧头像清理）
        InheritorProfile existing = profileMapper.getByEmployeeId(inheritorId);
        if (existing == null) {
            InheritorProfile profile = new InheritorProfile();
            BeanUtils.copyProperties(dto, profile);
            profile.setEmployeeId(inheritorId);
            profileMapper.insert(profile);
        } else {
            // 删除旧头像文件
            if (dto.getAvatarUrl() != null && !dto.getAvatarUrl().isEmpty()
                && existing.getAvatarUrl() != null && !existing.getAvatarUrl().equals(dto.getAvatarUrl())) {
                deleteAvatarFile(existing.getAvatarUrl());
            }
            InheritorProfile profile = new InheritorProfile();
            BeanUtils.copyProperties(dto, profile);
            profile.setId(existing.getId());
            profileMapper.update(profile);
        }
    }

    @Override
    public List<InheritorProfileVO> listAllWithEmployee() {
        // 从 inheritor 表查出所有已审核通过的传承人
        List<Inheritor> inheritors = inheritorMapper.getByStatus(StatusConstant.ENABLE);
        List<InheritorProfileVO> result = new ArrayList<>();
        for (Inheritor inheritor : inheritors) {
            InheritorProfile profile = profileMapper.getByEmployeeId(inheritor.getId());
            result.add(buildVO(inheritor, profile));
        }
        return result;
    }

    private InheritorProfileVO buildVO(Inheritor inheritor, InheritorProfile profile) {
        InheritorProfileVO.InheritorProfileVOBuilder builder = InheritorProfileVO.builder()
                .employeeId(inheritor.getId())
                .name(inheritor.getName())
                .phone(inheritor.getPhone())
                .sex(inheritor.getSex())
                .region(inheritor.getRegion())
                .birthDate(inheritor.getBirthDate())
                .age(inheritor.getAge());

        if (profile != null) {
            builder.id(profile.getId())
                    .honorTitle(profile.getHonorTitle())
                    .selfIntro(profile.getSelfIntro())
                    .craftTypes(profile.getCraftTypes())
                    .address(profile.getAddress())
                    .avatarUrl(profile.getAvatarUrl());
        }
        return builder.build();
    }

    @Value("${code1st.static.path:./static}")
    private String staticPath;

    private void deleteAvatarFile(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isEmpty()) return;
        try {
            String fileName = avatarUrl.replaceFirst("^/images/", "");
            if (fileName.equals(avatarUrl)) return;
            File file = new File(new File(staticPath).getAbsoluteFile(), "images/" + fileName);
            if (file.exists()) file.delete();
        } catch (Exception e) { /* ignore */ }
    }
}
