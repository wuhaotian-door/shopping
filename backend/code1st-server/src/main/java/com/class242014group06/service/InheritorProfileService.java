package com.class242014group06.service;

import com.class242014group06.dto.InheritorProfileDTO;
import com.class242014group06.vo.InheritorProfileVO;

import java.util.List;

public interface InheritorProfileService {

    /**
     * 获取传承人的个人资料（含employee基础信息）
     */
    InheritorProfileVO getByEmployeeId(Long employeeId);

    /**
     * 保存或更新传承人个人资料
     */
    void saveOrUpdate(Long employeeId, InheritorProfileDTO dto);

    /**
     * 查询所有传承人资料（首页展示）
     */
    List<InheritorProfileVO> listAllWithEmployee();
}
