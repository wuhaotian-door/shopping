package com.class242014group06.mapper;

import com.class242014group06.annotation.AutoFill;
import com.class242014group06.entity.AppointmentSlot;
import com.class242014group06.enumeration.OperationType;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Mapper
public interface AppointmentSlotMapper {

    /**
     * 新增时段
     */
    @AutoFill(value = OperationType.INSERT)
    @Insert("insert into appointment_slot (inheritor_id, slot_date, start_time, end_time, recurrence_type, recurrence_end_date, max_people, booked_count, status, create_time, update_time) " +
            "values (#{inheritorId}, #{slotDate}, #{startTime}, #{endTime}, #{recurrenceType}, #{recurrenceEndDate}, #{maxPeople}, #{bookedCount}, #{status}, #{createTime}, #{updateTime})")
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
    @Select("select * from appointment_slot where inheritor_id = #{inheritorId} order by slot_date desc, start_time")
    List<AppointmentSlot> getByInheritorId(Long inheritorId);

    /**
     * 查询传承人某日期的可用时段（供用户预约时选择）
     */
    @Select("select * from appointment_slot where inheritor_id = #{inheritorId} and slot_date = #{date} and status = 1 and booked_count < max_people order by start_time")
    List<AppointmentSlot> getAvailableSlots(@Param("inheritorId") Long inheritorId, @Param("date") String date);

    /**
     * 删除时段
     */
    @Delete("delete from appointment_slot where id = #{id}")
    void deleteById(Long id);

    /**
     * 查询传承人在指定日期范围内的所有时段（用于冲突检测和项目校验）
     */
    @Select("select * from appointment_slot where inheritor_id = #{inheritorId} and slot_date between #{startDate} and #{endDate} order by slot_date, start_time")
    List<AppointmentSlot> getByInheritorIdAndDateRange(@Param("inheritorId") Long inheritorId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * 查询指定日期内与 [startTime, endTime) 有时间重叠的时段（排除指定ID，用于编辑时的冲突检测）
     */
    List<AppointmentSlot> getOverlappingSlots(@Param("inheritorId") Long inheritorId,
                                               @Param("slotDate") LocalDate slotDate,
                                               @Param("startTime") LocalTime startTime,
                                               @Param("endTime") LocalTime endTime,
                                               @Param("excludeId") Long excludeId);

    /**
     * 查询传承人所有时段的最小日期和最大日期（含重复截止日期）
     */
    @Select("select min(slot_date) as minDate, max(case when recurrence_end_date is not null and recurrence_end_date > slot_date then recurrence_end_date else slot_date end) as maxDate " +
            "from appointment_slot where inheritor_id = #{inheritorId}")
    SlotDateRange getSlotDateRange(Long inheritorId);

    /**
     * 日期范围内部类（用于getSlotDateRange返回）
     */
    class SlotDateRange {
        private LocalDate minDate;
        private LocalDate maxDate;

        public LocalDate getMinDate() { return minDate; }
        public void setMinDate(LocalDate minDate) { this.minDate = minDate; }
        public LocalDate getMaxDate() { return maxDate; }
        public void setMaxDate(LocalDate maxDate) { this.maxDate = maxDate; }
    }
}
