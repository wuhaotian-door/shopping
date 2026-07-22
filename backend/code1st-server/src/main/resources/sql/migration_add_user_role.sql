-- =====================================================
-- 迁移：为评论/评价表添加 user_role 列，修复跨表 ID 冲突导致的越权删除问题
-- 执行前请确认已备份数据库
-- =====================================================

-- 文章评论表
ALTER TABLE article_comment
    ADD COLUMN user_role VARCHAR(16) NOT NULL DEFAULT 'USER' COMMENT '评论用户角色：USER/INHERITOR/ADMIN'
    AFTER user_id;

-- 商品评价表
ALTER TABLE product_review
    ADD COLUMN user_role VARCHAR(16) NOT NULL DEFAULT 'USER' COMMENT '评价用户角色：USER/INHERITOR/ADMIN'
    AFTER user_id;
