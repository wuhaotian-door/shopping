package com.class242014group06.mapper;

import com.class242014group06.annotation.AutoFill;
import com.class242014group06.entity.InheritorProfile;
import com.class242014group06.enumeration.OperationType;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface InheritorProfileMapper {

    /**
     * 根据传承人employeeId查询详细信息
     */
    @Select("select * from inheritor_profile where employee_id = #{employeeId}")
    InheritorProfile getByEmployeeId(Long employeeId);

    /**
     * 新增传承人详细信息
     */
    @AutoFill(value = OperationType.INSERT)
    @Insert("insert into inheritor_profile (employee_id, honor_title, self_intro, craft_types, address, avatar_url, create_time, update_time) " +
            "values (#{employeeId}, #{honorTitle}, #{selfIntro}, #{craftTypes}, #{address}, #{avatarUrl}, #{createTime}, #{updateTime})")
    void insert(InheritorProfile profile);

    /**
     * 动态更新传承人详细信息
     */
    @AutoFill(value = OperationType.UPDATE)
    void update(InheritorProfile profile);

    /**
     * 查询所有传承人详细信息（用于首页展示）
     */
    @Select("select * from inheritor_profile")
    List<InheritorProfile> listAll();
}
