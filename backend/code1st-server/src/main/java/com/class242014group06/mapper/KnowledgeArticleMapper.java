package com.class242014group06.mapper;

import com.class242014group06.annotation.AutoFill;
import com.class242014group06.entity.KnowledgeArticle;
import com.class242014group06.enumeration.OperationType;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface KnowledgeArticleMapper {

    /**
     * 新增文章
     */
    @AutoFill(value = OperationType.INSERT)
    @Insert("insert into knowledge_article (author_id, title, category, content, cover_url, status, view_count, create_time, update_time, create_user, update_user) " +
            "values (#{authorId}, #{title}, #{category}, #{content}, #{coverUrl}, #{status}, #{viewCount}, #{createTime}, #{updateTime}, #{createUser}, #{updateUser})")
    void insert(KnowledgeArticle article);

    /**
     * 动态更新文章
     */
    @AutoFill(value = OperationType.UPDATE)
    void update(KnowledgeArticle article);

    /**
     * 根据ID查询文章
     */
    @Select("select * from knowledge_article where id = #{id}")
    KnowledgeArticle getById(Long id);

    /**
     * 根据作者ID查询文章列表
     */
    @Select("select * from knowledge_article where author_id = #{authorId} order by create_time desc")
    List<KnowledgeArticle> getByAuthorId(Long authorId);

    /**
     * 查询所有已发布文章（首页展示）
     */
    @Select("select * from knowledge_article where status = 1 order by create_time desc")
    List<KnowledgeArticle> listPublished();

    /**
     * 删除文章
     */
    @Delete("delete from knowledge_article where id = #{id}")
    void deleteById(Long id);
}
