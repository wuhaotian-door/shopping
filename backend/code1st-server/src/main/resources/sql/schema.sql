-- 创建数据库
CREATE DATABASE IF NOT EXISTS code1st DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE code1st;
DROP TABLE `employee`
-- 员工表
CREATE TABLE `employee` (
                            `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
                            `name` VARCHAR(32) NOT NULL COMMENT '姓名',
                            `username` VARCHAR(32) NOT NULL COMMENT '用户名',
                            `password` VARCHAR(64) NOT NULL COMMENT '密码',
                            `phone` VARCHAR(11) DEFAULT NULL COMMENT '手机号',
                            `sex` VARCHAR(2) DEFAULT NULL COMMENT '性别',
                            `id_number` VARCHAR(18) DEFAULT NULL COMMENT '身份证号',
                            `role` VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '角色: USER-普通用户, INHERITOR-非遗传承人, ADMIN-管理员',
                            `status` INT NOT NULL DEFAULT 1 COMMENT '状态 1:启用 0:禁用',
                            `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
                            `update_time` DATETIME DEFAULT NULL COMMENT '修改时间',
                            `create_user` BIGINT DEFAULT NULL COMMENT '创建人',
                            `update_user` BIGINT DEFAULT NULL COMMENT '修改人',
                            PRIMARY KEY (`id`),
                            UNIQUE KEY `idx_username` (`username`),
                            KEY `idx_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 初始化管理员账号（密码为 123456 的 MD5）
INSERT INTO employee (name, username, password, phone, sex, id_number, role, status)
VALUES ('管理员', 'admin', 'e10adc3949ba59abbe56e057f20f883e', '13800138000', '男', '440101199001010001', 'ADMIN', 1);
ALTER TABLE `employee` MODIFY COLUMN `status` INT NOT NULL DEFAULT 1 COMMENT '状态 2:待审核 1:启用 0:禁用';
-- =====================================================
-- 预约订单表 DDL
-- 用户向工坊（传承人）发送预约订单
-- =====================================================
DROP TABLE IF EXISTS appointment_order;

CREATE TABLE appointment_order (
    id               BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '订单ID',
    user_id          BIGINT       NOT NULL COMMENT '预约用户ID（关联employee表）',
    user_name        VARCHAR(255) COMMENT '预约用户姓名',
    phone            VARCHAR(50)  COMMENT '联系电话',
    inheritor_id     BIGINT       COMMENT '传承人工坊ID（关联employee表，role=INHERITOR）',
    appointment_date DATE         COMMENT '预约日期',
    time_slot        VARCHAR(100) COMMENT '预约时段，如 上午 9:00-11:00',
    people_count     INT          COMMENT '预约人数',
    remark           VARCHAR(500) COMMENT '备注说明',
    status           INT          NOT NULL DEFAULT 0 COMMENT '订单状态：0-待确认, 1-已确认, 2-已完成, 3-已取消',
    create_time      DATETIME     COMMENT '创建时间',
    update_time      DATETIME     COMMENT '更新时间',
    create_user      BIGINT       COMMENT '创建人ID',
    update_user      BIGINT       COMMENT '更新人ID'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预约订单表';

-- 索引
CREATE INDEX idx_order_user_id      ON appointment_order(user_id);
CREATE INDEX idx_order_inheritor_id ON appointment_order(inheritor_id);
CREATE INDEX idx_order_status       ON appointment_order(status);
-- =====================================================
-- 预约订单测试数据
-- 请先执行 schema.sql 创建表结构
-- =====================================================

-- 假设员工表中存在以下记录：
--   user_id=5: 张同学 (普通用户)
--   user_id=6: 李老师 (普通用户)
--   user_id=7: 王同学 (普通用户)
--   user_id=8: 赵女士 (普通用户)
--   inheritor_id=3: 李秀英 (非遗传承人-苏绣)

INSERT INTO appointment_order (user_id, user_name, phone, inheritor_id, appointment_date, time_slot, people_count, remark, status, create_time, update_time)
VALUES
    (5, '张同学', '138****6789', 3, '2026-07-20', '上午 9:00-11:00', 3, '想学习剪纸基础技法',          0, NOW(), NOW()),
    (6, '李老师', '139****8901', 3, '2026-07-18', '下午 14:00-16:00', 5, '学校社团体验活动',              1, NOW(), NOW()),
    (7, '王同学', '136****2345', 3, '2026-07-15', '上午 10:00-12:00', 2, '一对一深入学习',                2, NOW(), NOW()),
    (8, '赵女士', '137****4567', 3, '2026-07-22', '下午 15:00-17:00', 4, '临时有事取消',                  3, NOW(), NOW());

