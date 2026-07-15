package com.class242014group06.service.impl;

import com.class242014group06.constant.MessageConstant;
import com.class242014group06.dto.InheritorProfileDTO;
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
        InheritorProfile existing = profileMapper.getByEmployeeId(inheritorId);
        if (existing == null) {
            // 新建
            InheritorProfile profile = new InheritorProfile();
            BeanUtils.copyProperties(dto, profile);
            profile.setEmployeeId(inheritorId);
            profileMapper.insert(profile);
        } else {
            // 更新
            InheritorProfile profile = new InheritorProfile();
            BeanUtils.copyProperties(dto, profile);
            profile.setId(existing.getId());
            profileMapper.update(profile);
        }
    }

    @Override
    public List<InheritorProfileVO> listAllWithEmployee() {
        List<InheritorProfile> profiles = profileMapper.listAll();
        List<InheritorProfileVO> result = new ArrayList<>();
        for (InheritorProfile profile : profiles) {
            Inheritor inheritor = inheritorMapper.getById(profile.getEmployeeId());
            if (inheritor != null) {
                result.add(buildVO(inheritor, profile));
            }
        }
        return result;
    }

    private InheritorProfileVO buildVO(Inheritor inheritor, InheritorProfile profile) {
        InheritorProfileVO.InheritorProfileVOBuilder builder = InheritorProfileVO.builder()
                .employeeId(inheritor.getId())
                .name(inheritor.getName())
                .phone(inheritor.getPhone())
                .sex(inheritor.getSex());

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
}
