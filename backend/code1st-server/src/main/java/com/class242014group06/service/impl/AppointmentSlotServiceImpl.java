package com.class242014group06.service.impl;

import com.class242014group06.constant.MessageConstant;
import com.class242014group06.dto.AppointmentSlotDTO;
import com.class242014group06.entity.AppointmentSlot;
import com.class242014group06.exception.BaseException;
import com.class242014group06.mapper.AppointmentSlotMapper;
import com.class242014group06.service.AppointmentSlotService;
import com.class242014group06.vo.AppointmentSlotVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AppointmentSlotServiceImpl implements AppointmentSlotService {

    @Autowired
    private AppointmentSlotMapper slotMapper;

    @Override
    public void create(Long inheritorId, AppointmentSlotDTO dto) {
        AppointmentSlot slot = dtoToEntity(dto);
        slot.setInheritorId(inheritorId);
        slot.setBookedCount(0);
        if (slot.getMaxPeople() == null) {
            slot.setMaxPeople(5);
        }
        if (slot.getStatus() == null) {
            slot.setStatus(1);
        }
        if (slot.getRecurrenceType() == null) {
            slot.setRecurrenceType("NONE");
        }

        // 时间冲突检测
        checkTimeConflict(inheritorId, slot, null);

        slotMapper.insert(slot);
    }

    @Override
    public void update(Long slotId, Long inheritorId, AppointmentSlotDTO dto) {
        AppointmentSlot existing = slotMapper.getById(slotId);
        if (existing == null) {
            throw new BaseException(MessageConstant.SLOT_NOT_FOUND);
        }
        if (!existing.getInheritorId().equals(inheritorId)) {
            throw new BaseException(MessageConstant.SLOT_NO_PERMISSION);
        }

        AppointmentSlot slot = dtoToEntity(dto);
        slot.setId(slotId);

        // 时间冲突检测（排除自身）
        checkTimeConflict(inheritorId, slot, slotId);

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
    public List<AppointmentSlotVO> listByInheritor(Long inheritorId) {
        List<AppointmentSlot> slots = slotMapper.getByInheritorId(inheritorId);
        return slots.stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<AppointmentSlotVO> getAvailableSlots(Long inheritorId, String date) {
        List<AppointmentSlot> slots = slotMapper.getAvailableSlots(inheritorId, date);
        return slots.stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getSlotDateRange(Long inheritorId) {
        AppointmentSlotMapper.SlotDateRange range = slotMapper.getSlotDateRange(inheritorId);
        if (range == null || range.getMinDate() == null) {
            return null;
        }
        Map<String, Object> result = new HashMap<>();
        result.put("minDate", range.getMinDate());
        result.put("maxDate", range.getMaxDate());
        return result;
    }

    /**
     * 检测时间冲突
     * 对非重复时段检查 slotDate；对重复时段展开所有日期后逐日检查
     */
    private void checkTimeConflict(Long inheritorId, AppointmentSlot slot, Long excludeId) {
        List<LocalDate> dates = expandSlotDates(slot);

        for (LocalDate date : dates) {
            List<AppointmentSlot> overlapping = slotMapper.getOverlappingSlots(
                    inheritorId, date, slot.getStartTime(), slot.getEndTime(), excludeId);
            if (!overlapping.isEmpty()) {
                AppointmentSlot conflict = overlapping.get(0);
                String msg = String.format("时段冲突：%s 的 %s-%s 与已有时段（%s-%s）重叠",
                        date, slot.getStartTime(), slot.getEndTime(),
                        conflict.getStartTime(), conflict.getEndTime());
                throw new BaseException(msg);
            }
        }
    }

    /**
     * 根据重复类型展开时段日期列表
     */
    private List<LocalDate> expandSlotDates(AppointmentSlot slot) {
        List<LocalDate> dates = new ArrayList<>();
        String type = slot.getRecurrenceType();
        LocalDate start = slot.getSlotDate();
        LocalDate end = slot.getRecurrenceEndDate();

        if (type == null || "NONE".equals(type) || end == null || !end.isAfter(start)) {
            dates.add(start);
            return dates;
        }

        LocalDate current = start;
        while (!current.isAfter(end)) {
            dates.add(current);
            if ("WEEKLY".equals(type)) {
                current = current.plusWeeks(1);
            } else if ("MONTHLY".equals(type)) {
                current = current.plusMonths(1);
            } else {
                break; // 未知类型，只添加起始日期
            }
        }
        return dates;
    }

    private AppointmentSlot dtoToEntity(AppointmentSlotDTO dto) {
        AppointmentSlot slot = new AppointmentSlot();
        BeanUtils.copyProperties(dto, slot);
        return slot;
    }

    private AppointmentSlotVO toVO(AppointmentSlot slot) {
        return AppointmentSlotVO.builder()
                .id(slot.getId())
                .inheritorId(slot.getInheritorId())
                .slotDate(slot.getSlotDate())
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .recurrenceType(slot.getRecurrenceType())
                .recurrenceEndDate(slot.getRecurrenceEndDate())
                .maxPeople(slot.getMaxPeople())
                .bookedCount(slot.getBookedCount())
                .status(slot.getStatus())
                .createTime(slot.getCreateTime())
                .build();
    }
}
