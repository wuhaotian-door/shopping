-- =====================================================
-- 管理员操作日志表
-- 记录管理员的所有关键操作，用于审计追溯
-- 执行时间：2026-07-19
-- =====================================================
USE code1st;

DROP TABLE IF EXISTS admin_operation_log;

CREATE TABLE admin_operation_log (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    operator_id     BIGINT       NOT NULL COMMENT '操作人ID',
    operator_name   VARCHAR(64)  NOT NULL COMMENT '操作人姓名',
    operator_role   VARCHAR(32)  NOT NULL COMMENT '操作人角色：ADMIN/INHERITOR/USER',
    operation_type  VARCHAR(64)  NOT NULL COMMENT '操作类型：APPROVE_INHERITOR/REJECT_INHERITOR/TOGGLE_USER_STATUS/DELETE_PRODUCT/TOGGLE_PRODUCT/DELETE_PROJECT/DELETE_ARTICLE/UPDATE_ORDER_STATUS/CANCEL_ORDER/CREATE_ADMIN/CHANGE_PASSWORD',
    target_type     VARCHAR(32)  DEFAULT NULL COMMENT '操作目标类型：inheritor/user/product/project/article/order/admin',
    target_id       BIGINT       DEFAULT NULL COMMENT '操作目标ID',
    detail          VARCHAR(500) DEFAULT NULL COMMENT '操作详情描述',
    create_time     DATETIME     DEFAULT NULL COMMENT '操作时间',
    INDEX idx_operator (operator_id),
    INDEX idx_type (operation_type),
    INDEX idx_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员操作日志表';
