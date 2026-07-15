package com.class242014group06.service.impl;

import com.class242014group06.dto.HeritageProjectDTO;
import com.class242014group06.entity.Employee;
import com.class242014group06.entity.HeritageProject;
import com.class242014group06.exception.BaseException;
import com.class242014group06.mapper.EmployeeMapper;
import com.class242014group06.mapper.HeritageProjectMapper;
import com.class242014group06.service.HeritageProjectService;
import com.class242014group06.vo.HeritageProjectVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class HeritageProjectServiceImpl implements HeritageProjectService {

    @Autowired
    private HeritageProjectMapper projectMapper;

    @Autowired
    private EmployeeMapper employeeMapper;

    @Override
    public void create(Long inheritorId, HeritageProjectDTO dto) {
        HeritageProject project = new HeritageProject();
        BeanUtils.copyProperties(dto, project);
        project.setInheritorId(inheritorId);
        project.setCurrentPeople(0);
        if (project.getStatus() == null) {
            project.setStatus(0); // 默认下架
        }
        projectMapper.insert(project);
    }

    @Override
    public void update(Long projectId, Long inheritorId, HeritageProjectDTO dto) {
        HeritageProject existing = projectMapper.getById(projectId);
        if (existing == null) {
            throw new BaseException("项目不存在");
        }
        if (!existing.getInheritorId().equals(inheritorId)) {
            throw new BaseException("无权修改此项目");
        }
        HeritageProject project = new HeritageProject();
        BeanUtils.copyProperties(dto, project);
        project.setId(projectId);
        projectMapper.update(project);
    }

    @Override
    public void delete(Long projectId, Long inheritorId) {
        HeritageProject existing = projectMapper.getById(projectId);
        if (existing == null) {
            throw new BaseException("项目不存在");
        }
        if (!existing.getInheritorId().equals(inheritorId)) {
            throw new BaseException("无权删除此项目");
        }
        projectMapper.deleteById(projectId);
    }

    @Override
    public void toggleStatus(Long projectId, Long inheritorId, Integer status) {
        HeritageProject existing = projectMapper.getById(projectId);
        if (existing == null) {
            throw new BaseException("项目不存在");
        }
        if (!existing.getInheritorId().equals(inheritorId)) {
            throw new BaseException("无权操作此项目");
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
        Employee inheritor = employeeMapper.getById(inheritorId);
        String inheritorName = inheritor != null ? inheritor.getName() : "";
        return projects.stream()
                .map(p -> toVO(p, inheritorName))
                .collect(Collectors.toList());
    }

    @Override
    public List<HeritageProjectVO> listPublished() {
        List<HeritageProject> projects = projectMapper.listPublished();
        return projects.stream()
                .map(p -> {
                    Employee inheritor = employeeMapper.getById(p.getInheritorId());
                    String inheritorName = inheritor != null ? inheritor.getName() : "";
                    return toVO(p, inheritorName);
                })
                .collect(Collectors.toList());
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
