-- =====================================================
-- 迁移脚本：为 inheritor 表添加 email 字段
-- 执行时间：2026-07-19
-- =====================================================
USE code1st;

ALTER TABLE inheritor
    ADD COLUMN email VARCHAR(128) DEFAULT NULL COMMENT '邮箱（用于接收审核结果通知等）'
    AFTER region;

-- 为已有测试数据设置默认邮箱
UPDATE inheritor SET email = '3326353413@qq.com' WHERE email IS NULL;
