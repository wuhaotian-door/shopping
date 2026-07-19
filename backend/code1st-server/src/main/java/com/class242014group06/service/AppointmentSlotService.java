package com.class242014group06.service;

import com.class242014group06.dto.AppointmentSlotDTO;
import com.class242014group06.vo.AppointmentSlotVO;

import java.util.List;
import java.util.Map;

public interface AppointmentSlotService {

    /**
     * 创建时段
     */
    void create(Long inheritorId, AppointmentSlotDTO dto);

    /**
     * 更新时段
     */
    void update(Long slotId, Long inheritorId, AppointmentSlotDTO dto);

    /**
     * 删除时段
     */
    void delete(Long slotId, Long inheritorId);

    /**
     * 查看自己的时段列表
     */
    List<AppointmentSlotVO> listByInheritor(Long inheritorId);

    /**
     * 查询传承人某日期的可用时段（公开）
     */
    List<AppointmentSlotVO> getAvailableSlots(Long inheritorId, String date);

    /**
     * 获取时段覆盖的日期范围（用于项目校验）
     * @return Map with "minDate" and "maxDate" keys (LocalDate values), or null if no slots
     */
    Map<String, Object> getSlotDateRange(Long inheritorId);
}
