package com.class242014group06.mapper;

import com.class242014group06.annotation.AutoFill;
import com.class242014group06.entity.HeritageProject;
import com.class242014group06.enumeration.OperationType;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface HeritageProjectMapper {

    /**
     * 新增项目
     */
    @AutoFill(value = OperationType.INSERT)
    @Insert("insert into heritage_project (inheritor_id, title, category, description, cover_url, location, start_date, end_date, max_people, current_people, status, project_type, price, create_time, update_time, create_user, update_user) " +
            "values (#{inheritorId}, #{title}, #{category}, #{description}, #{coverUrl}, #{location}, #{startDate}, #{endDate}, #{maxPeople}, #{currentPeople}, #{status}, #{projectType}, #{price}, #{createTime}, #{updateTime}, #{createUser}, #{updateUser})")
    void insert(HeritageProject project);

    /**
     * 动态更新项目
     */
    @AutoFill(value = OperationType.UPDATE)
    void update(HeritageProject project);

    /**
     * 根据ID查询项目
     */
    @Select("select * from heritage_project where id = #{id}")
    HeritageProject getById(Long id);

    /**
     * 根据传承人ID查询项目列表
     */
    @Select("select * from heritage_project where inheritor_id = #{inheritorId} order by create_time desc")
    List<HeritageProject> getByInheritorId(Long inheritorId);

    /**
     * 根据传承人ID和项目类型查询项目列表
     */
    @Select("select * from heritage_project where inheritor_id = #{inheritorId} and project_type = #{projectType} order by create_time desc")
    List<HeritageProject> getByInheritorIdAndType(@Param("inheritorId") Long inheritorId, @Param("projectType") Integer projectType);

    /**
     * 查询所有已上架项目（首页展示）
     */
    @Select("select * from heritage_project where status = 1 order by create_time desc")
    List<HeritageProject> listPublished();

    /**
     * 删除项目
     */
    @Delete("delete from heritage_project where id = #{id}")
    void deleteById(Long id);

    @Select("select * from heritage_project order by create_time desc")
    List<HeritageProject> listAll();

    /**
     * 根据传承人ID查询已上架项目列表（公开）
     */
    @Select("select * from heritage_project where inheritor_id = #{inheritorId} and status = 1 order by create_time desc")
    List<HeritageProject> getPublishedByInheritorId(Long inheritorId);

    /**
     * 增加项目报名人数
     */
    @Update("update heritage_project set current_people = current_people + #{delta} where id = #{projectId} and current_people + #{delta} <= max_people")
    int incrementCurrentPeople(@Param("projectId") Long projectId, @Param("delta") Integer delta);

    /**
     * 减少项目报名人数（不低于0）
     */
    @Update("update heritage_project set current_people = greatest(current_people - #{delta}, 0) where id = #{projectId}")
    int decrementCurrentPeople(@Param("projectId") Long projectId, @Param("delta") Integer delta);
}
