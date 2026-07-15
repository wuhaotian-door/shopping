package com.class242014group06.service.impl;

import com.class242014group06.constant.MessageConstant;
import com.class242014group06.entity.AppointmentSlot;
import com.class242014group06.exception.BaseException;
import com.class242014group06.mapper.AppointmentSlotMapper;
import com.class242014group06.service.AppointmentSlotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AppointmentSlotServiceImpl implements AppointmentSlotService {

    @Autowired
    private AppointmentSlotMapper slotMapper;

    @Override
    public void create(Long inheritorId, AppointmentSlot slot) {
        slot.setInheritorId(inheritorId);
        slot.setBookedCount(0);
        if (slot.getMaxPeople() == null) {
            slot.setMaxPeople(5);
        }
        if (slot.getStatus() == null) {
            slot.setStatus(1);
        }
        slotMapper.insert(slot);
    }

    @Override
    public void update(Long slotId, Long inheritorId, AppointmentSlot slot) {
        AppointmentSlot existing = slotMapper.getById(slotId);
        if (existing == null) {
            throw new BaseException(MessageConstant.SLOT_NOT_FOUND);
        }
        if (!existing.getInheritorId().equals(inheritorId)) {
            throw new BaseException(MessageConstant.SLOT_NO_PERMISSION);
        }
        slot.setId(slotId);
        slotMapper.update(slot);
    }

    @Override
    public void delete(Long slotId, Long inheritorId) {
        AppointmentSlot existing = slotMapper.getById(slotId);
        if (existing == null) {
            throw new BaseException(MessageConstant.SLOT_NOT_FOUND);
        }
        if (!existing.getInheritorId().equals(inheritorId)) {
            throw new BaseException(MessageConstant.SLOT_NO_PERMISSION);
        }
        slotMapper.deleteById(slotId);
    }

    @Override
    public List<AppointmentSlot> listByInheritor(Long inheritorId) {
        return slotMapper.getByInheritorId(inheritorId);
    }

    @Override
    public List<AppointmentSlot> getAvailableSlots(Long inheritorId, String date) {
        return slotMapper.getAvailableSlots(inheritorId, date);
    }
}
