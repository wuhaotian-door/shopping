package com.class242014group06.mapper;

import com.class242014group06.annotation.AutoFill;
import com.class242014group06.entity.AppointmentSlot;
import com.class242014group06.enumeration.OperationType;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AppointmentSlotMapper {

    /**
     * 新增时段
     */
    @AutoFill(value = OperationType.INSERT)
    @Insert("insert into appointment_slot (inheritor_id, slot_date, time_slot, max_people, booked_count, status, create_time, update_time) " +
            "values (#{inheritorId}, #{slotDate}, #{timeSlot}, #{maxPeople}, #{bookedCount}, #{status}, #{createTime}, #{updateTime})")
    void insert(AppointmentSlot slot);

    /**
     * 动态更新时段
     */
    @AutoFill(value = OperationType.UPDATE)
    void update(AppointmentSlot slot);

    /**
     * 根据ID查询时段
     */
    @Select("select * from appointment_slot where id = #{id}")
    AppointmentSlot getById(Long id);

    /**
     * 根据传承人ID查询所有时段
     */
    @Select("select * from appointment_slot where inheritor_id = #{inheritorId} order by slot_date desc, time_slot")
    List<AppointmentSlot> getByInheritorId(Long inheritorId);

    /**
     * 查询传承人某日期的可用时段（供用户预约时选择）
     */
    @Select("select * from appointment_slot where inheritor_id = #{inheritorId} and slot_date = #{date} and status = 1 and booked_count < max_people order by time_slot")
    List<AppointmentSlot> getAvailableSlots(Long inheritorId, String date);

    /**
     * 删除时段
     */
    @Delete("delete from appointment_slot where id = #{id}")
    void deleteById(Long id);
}
