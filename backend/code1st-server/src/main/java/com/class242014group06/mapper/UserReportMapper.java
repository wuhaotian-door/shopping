package com.class242014group06.mapper;

import com.class242014group06.entity.UserReport;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface UserReportMapper {

    @Insert("insert into user_report (reporter_id, reporter_role, reported_user_id, reported_user_role, reason, screenshot_url, status, create_time) " +
            "values (#{reporterId}, #{reporterRole}, #{reportedUserId}, #{reportedUserRole}, #{reason}, #{screenshotUrl}, 0, #{createTime})")
    void insert(UserReport report);

    @Select("select * from user_report order by create_time desc")
    List<UserReport> listAll();

    @Select("select * from user_report where id = #{id}")
    UserReport getById(Long id);

    @Update("update user_report set status = 1, handle_result = #{handleResult}, ban_days = #{banDays}, handle_time = now() where id = #{id}")
    void handle(Long id, String handleResult, Integer banDays);
}
