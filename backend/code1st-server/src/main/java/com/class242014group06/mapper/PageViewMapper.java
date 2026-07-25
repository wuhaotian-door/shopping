package com.class242014group06.mapper;

import com.class242014group06.entity.PageView;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface PageViewMapper {

    @Insert("insert into page_view (path, ip, create_time) values (#{path}, #{ip}, #{createTime})")
    void insert(PageView view);

    /** 按日统计浏览量（最近30天） */
    @Select("select date(create_time) as date, count(*) as count from page_view " +
            "where create_time >= date_sub(curdate(), interval 30 day) " +
            "group by date(create_time) order by date(create_time)")
    List<Map<String, Object>> countByDay30();

    /** 总浏览量 */
    @Select("select count(*) from page_view")
    Long countTotal();
}
