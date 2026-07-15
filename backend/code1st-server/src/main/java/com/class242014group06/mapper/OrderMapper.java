package com.class242014group06.mapper;

import com.class242014group06.annotation.AutoFill;
import com.class242014group06.entity.OrderAppointment;
import com.class242014group06.enumeration.OperationType;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderMapper {

    /**
     * 新增预约订单
     */
    @AutoFill(value = OperationType.INSERT)
    @Insert("insert into appointment_order (user_id, user_name, phone, inheritor_id, appointment_date, time_slot, " +
            "people_count, remark, status, create_time, update_time, create_user, update_user) " +
            "values (#{userId}, #{userName}, #{phone}, #{inheritorId}, #{appointmentDate}, #{timeSlot}, " +
            "#{peopleCount}, #{remark}, #{status}, #{createTime}, #{updateTime}, #{createUser}, #{updateUser})")
    void insert(OrderAppointment order);

    /**
     * 根据主键动态修改属性
     */
    @AutoFill(value = OperationType.UPDATE)
    void update(OrderAppointment order);

    /**
     * 根据ID查询订单
     */
    @Select("select * from appointment_order where id = #{id}")
    OrderAppointment getById(Long id);

    /**
     * 根据用户ID查询该用户的所有订单
     */
    @Select("select * from appointment_order where user_id = #{userId} order by create_time desc")
    List<OrderAppointment> getByUserId(Long userId);

    /**
     * 根据传承人ID查询该工坊收到的所有订单
     */
    @Select("select * from appointment_order where inheritor_id = #{inheritorId} order by create_time desc")
    List<OrderAppointment> getByInheritorId(Long inheritorId);

    /**
     * 管理员查询所有订单
     */
    @Select("select * from appointment_order order by create_time desc")
    List<OrderAppointment> listAll();
}
