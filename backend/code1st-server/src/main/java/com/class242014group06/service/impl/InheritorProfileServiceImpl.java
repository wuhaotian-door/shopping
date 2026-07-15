package com.class242014group06.service.impl;

import com.class242014group06.constant.MessageConstant;
import com.class242014group06.dto.InheritorProfileDTO;
import com.class242014group06.entity.Employee;
import com.class242014group06.entity.InheritorProfile;
import com.class242014group06.exception.BaseException;
import com.class242014group06.mapper.EmployeeMapper;
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
    private EmployeeMapper employeeMapper;

    @Override
    public InheritorProfileVO getByEmployeeId(Long employeeId) {
        Employee employee = employeeMapper.getById(employeeId);
        if (employee == null) {
            throw new BaseException(MessageConstant.ACCOUNT_NOT_FOUND);
        }
        InheritorProfile profile = profileMapper.getByEmployeeId(employeeId);
        return buildVO(employee, profile);
    }

    @Override
    public void saveOrUpdate(Long employeeId, InheritorProfileDTO dto) {
        InheritorProfile existing = profileMapper.getByEmployeeId(employeeId);
        if (existing == null) {
            // 新建
            InheritorProfile profile = new InheritorProfile();
            BeanUtils.copyProperties(dto, profile);
            profile.setEmployeeId(employeeId);
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
            Employee employee = employeeMapper.getById(profile.getEmployeeId());
            if (employee != null) {
                result.add(buildVO(employee, profile));
            }
        }
        return result;
    }

    private InheritorProfileVO buildVO(Employee employee, InheritorProfile profile) {
        InheritorProfileVO.InheritorProfileVOBuilder builder = InheritorProfileVO.builder()
                .employeeId(employee.getId())
                .name(employee.getName())
                .phone(employee.getPhone())
                .sex(employee.getSex());

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
