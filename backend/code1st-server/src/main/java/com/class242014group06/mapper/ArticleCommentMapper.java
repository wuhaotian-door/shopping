package com.class242014group06.mapper;

import com.class242014group06.entity.ArticleComment;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 文章评论 Mapper
 */
@Mapper
public interface ArticleCommentMapper {

    @Insert("insert into article_comment (article_id, user_id, user_name, content, create_time) " +
            "values (#{articleId}, #{userId}, #{userName}, #{content}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(ArticleComment comment);

    @Select("select * from article_comment where article_id = #{articleId} order by create_time desc")
    List<ArticleComment> listByArticleId(Long articleId);

    @Select("select count(*) from article_comment where article_id = #{articleId}")
    Long countByArticleId(Long articleId);
}