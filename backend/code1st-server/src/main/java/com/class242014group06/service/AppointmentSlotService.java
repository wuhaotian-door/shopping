package com.class242014group06.service;

import com.class242014group06.entity.AppointmentSlot;

import java.util.List;

public interface AppointmentSlotService {

    /**
     * 新增可预约时段
     */
    void create(Long inheritorId, AppointmentSlot slot);

    /**
     * 更新时段
     */
    void update(Long slotId, Long inheritorId, AppointmentSlot slot);

    /**
     * 删除时段
     */
    void delete(Long slotId, Long inheritorId);

    /**
     * 查询自己的时段列表
     */
    List<AppointmentSlot> listByInheritor(Long inheritorId);

    /**
     * 查询传承人某日期的可用时段（供用户预约时选择）
     */
    List<AppointmentSlot> getAvailableSlots(Long inheritorId, String date);
}
