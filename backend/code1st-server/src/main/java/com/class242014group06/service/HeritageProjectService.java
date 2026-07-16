package com.class242014group06.service;

import com.class242014group06.dto.HeritageProjectDTO;
import com.class242014group06.vo.HeritageProjectVO;

import java.util.List;

public interface HeritageProjectService {

    /**
     * 创建项目
     * @return 警告信息（如日期超出时段范围），无警告时返回null
     */
    String create(Long inheritorId, HeritageProjectDTO dto);

    /**
     * 更新项目
     * @return 警告信息（如日期超出时段范围），无警告时返回null
     */
    String update(Long projectId, Long inheritorId, HeritageProjectDTO dto);

    /**
     * 删除项目
     */
    void delete(Long projectId, Long inheritorId);

    /**
     * 切换上下架状态
     */
    void toggleStatus(Long projectId, Long inheritorId, Integer status);

    /**
     * 查询自己的项目列表
     */
    List<HeritageProjectVO> listByInheritor(Long inheritorId);

    /**
     * 查询所有已上架项目（首页展示）
     */
    List<HeritageProjectVO> listPublished();
}
