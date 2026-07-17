package com.class242014group06.service.impl;

import com.class242014group06.constant.MessageConstant;
import com.class242014group06.dto.HeritageProjectDTO;
import com.class242014group06.entity.Inheritor;
import com.class242014group06.entity.HeritageProject;
import com.class242014group06.exception.BaseException;
import com.class242014group06.mapper.AppointmentSlotMapper;
import com.class242014group06.mapper.InheritorMapper;
import com.class242014group06.mapper.HeritageProjectMapper;
import com.class242014group06.service.HeritageProjectService;
import com.class242014group06.vo.HeritageProjectVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class HeritageProjectServiceImpl implements HeritageProjectService {

    @Autowired
    private HeritageProjectMapper projectMapper;

    @Autowired
    private InheritorMapper inheritorMapper;

    @Autowired
    private AppointmentSlotMapper slotMapper;

    @Override
    public String create(Long inheritorId, HeritageProjectDTO dto) {
        HeritageProject project = new HeritageProject();
        BeanUtils.copyProperties(dto, project);
        project.setInheritorId(inheritorId);
        project.setCurrentPeople(0);
        if (project.getStatus() == null) {
            project.setStatus(0); // 默认下架
        }
        projectMapper.insert(project);

        // 校验项目日期是否在时段范围内
        return validateProjectDates(inheritorId, dto.getStartDate(), dto.getEndDate());
    }

    @Override
    public String update(Long projectId, Long inheritorId, HeritageProjectDTO dto) {
        HeritageProject existing = projectMapper.getById(projectId);
        if (existing == null) {
            throw new BaseException(MessageConstant.PROJECT_NOT_FOUND);
        }
        if (!existing.getInheritorId().equals(inheritorId)) {
            throw new BaseException(MessageConstant.PROJECT_NO_PERMISSION);
        }
        HeritageProject project = new HeritageProject();
        BeanUtils.copyProperties(dto, project);
        project.setId(projectId);
        projectMapper.update(project);

        // 校验项目日期是否在时段范围内
        return validateProjectDates(inheritorId, dto.getStartDate(), dto.getEndDate());
    }

    @Override
    public void delete(Long projectId, Long inheritorId) {
        HeritageProject existing = projectMapper.getById(projectId);
        if (existing == null) {
            throw new BaseException(MessageConstant.PROJECT_NOT_FOUND);
        }
        if (!existing.getInheritorId().equals(inheritorId)) {
            throw new BaseException(MessageConstant.PROJECT_NO_PERMISSION);
        }
        projectMapper.deleteById(projectId);
    }

    @Override
    public void toggleStatus(Long projectId, Long inheritorId, Integer status) {
        HeritageProject existing = projectMapper.getById(projectId);
        if (existing == null) {
            throw new BaseException(MessageConstant.PROJECT_NOT_FOUND);
        }
        if (!existing.getInheritorId().equals(inheritorId)) {
            throw new BaseException(MessageConstant.PROJECT_NO_PERMISSION);
        }
        HeritageProject project = HeritageProject.builder()
                .id(projectId)
                .status(status)
                .build();
        projectMapper.update(project);
    }

    @Override
    public List<HeritageProjectVO> listByInheritor(Long inheritorId) {
        List<HeritageProject> projects = projectMapper.getByInheritorId(inheritorId);
        Inheritor inheritor = inheritorMapper.getById(inheritorId);
        String inheritorName = inheritor != null ? inheritor.getName() : "";
        return projects.stream()
                .map(p -> toVO(p, inheritorName))
                .collect(Collectors.toList());
    }

    @Override
    public List<HeritageProjectVO> listAll() {
        List<HeritageProject> projects = projectMapper.listAll();
        return projects.stream()
                .map(p -> {
                    Inheritor inheritor = inheritorMapper.getById(p.getInheritorId());
                    String inheritorName = inheritor != null ? inheritor.getName() : "";
                    return toVO(p, inheritorName);
                })
                .collect(Collectors.toList());
    }

    @Override
    public void deleteByAdmin(Long id) {
        HeritageProject existing = projectMapper.getById(id);
        if (existing == null) {
            throw new BaseException(MessageConstant.PROJECT_NOT_FOUND);
        }
        projectMapper.deleteById(id);
    }

    @Override
    public List<HeritageProjectVO> listPublished() {
        List<HeritageProject> projects = projectMapper.listPublished();
        return projects.stream()
                .map(p -> {
                    Inheritor inheritor = inheritorMapper.getById(p.getInheritorId());
                    String inheritorName = inheritor != null ? inheritor.getName() : "";
                    return toVO(p, inheritorName);
                })
                .collect(Collectors.toList());
    }

    /**
     * 校验项目日期是否在时段覆盖范围内
     * @return 警告信息，无问题时返回null
     */
    private String validateProjectDates(Long inheritorId, LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return null;
        }

        AppointmentSlotMapper.SlotDateRange range = slotMapper.getSlotDateRange(inheritorId);
        if (range == null || range.getMinDate() == null) {
            return MessageConstant.PROJECT_NO_SLOTS_CONFIGURED;
        }

        LocalDate slotMin = range.getMinDate();
        LocalDate slotMax = range.getMaxDate();

        boolean outOfRange = false;
        StringBuilder sb = new StringBuilder();

        if (startDate.isBefore(slotMin)) {
            sb.append("项目开始日期（").append(startDate).append("）早于最早时段日期（").append(slotMin).append("）");
            outOfRange = true;
        }
        if (endDate.isAfter(slotMax)) {
            if (outOfRange) sb.append("；");
            sb.append("项目结束日期（").append(endDate).append("）晚于最晚时段日期（").append(slotMax).append("）");
            outOfRange = true;
        }

        if (outOfRange) {
            sb.append("。").append(MessageConstant.PROJECT_DATE_OUT_OF_SLOT_RANGE);
            return sb.toString();
        }

        return null;
    }

    private HeritageProjectVO toVO(HeritageProject project, String inheritorName) {
        return HeritageProjectVO.builder()
                .id(project.getId())
                .inheritorId(project.getInheritorId())
                .inheritorName(inheritorName)
                .title(project.getTitle())
                .category(project.getCategory())
                .description(project.getDescription())
                .coverUrl(project.getCoverUrl())
                .location(project.getLocation())
                .startDate(project.getStartDate())
                .endDate(project.getEndDate())
                .maxPeople(project.getMaxPeople())
                .currentPeople(project.getCurrentPeople())
                .status(project.getStatus())
                .createTime(project.getCreateTime())
                .build();
    }
}
