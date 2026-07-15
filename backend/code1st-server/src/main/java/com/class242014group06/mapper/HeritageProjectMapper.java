package com.class242014group06.mapper;

import com.class242014group06.annotation.AutoFill;
import com.class242014group06.entity.HeritageProject;
import com.class242014group06.enumeration.OperationType;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface HeritageProjectMapper {

    /**
     * 新增项目
     */
    @AutoFill(value = OperationType.INSERT)
    @Insert("insert into heritage_project (inheritor_id, title, category, description, cover_url, location, start_date, end_date, max_people, current_people, status, create_time, update_time, create_user, update_user) " +
            "values (#{inheritorId}, #{title}, #{category}, #{description}, #{coverUrl}, #{location}, #{startDate}, #{endDate}, #{maxPeople}, #{currentPeople}, #{status}, #{createTime}, #{updateTime}, #{createUser}, #{updateUser})")
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
     * 查询所有已上架项目（首页展示）
     */
    @Select("select * from heritage_project where status = 1 order by create_time desc")
    List<HeritageProject> listPublished();

    /**
     * 删除项目
     */
    @Delete("delete from heritage_project where id = #{id}")
    void deleteById(Long id);
}
