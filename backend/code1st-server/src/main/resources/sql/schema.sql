-- 创建数据库
CREATE DATABASE IF NOT EXISTS code1st DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE code1st;

-- =====================================================
-- 旧表清理（拆分为 user / inheritor / admin 三张独立表）
-- =====================================================
DROP TABLE IF EXISTS `employee`;

-- =====================================================
-- 注册用户表（普通用户）
-- =====================================================
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`        VARCHAR(32)  NOT NULL COMMENT '姓名',
    `username`    VARCHAR(32)  NOT NULL COMMENT '用户名',
    `password`    VARCHAR(64)  NOT NULL COMMENT '密码（MD5加密）',
    `phone`       VARCHAR(11)  DEFAULT NULL COMMENT '手机号',
    `email`       VARCHAR(128) DEFAULT NULL COMMENT '邮箱（用于找回密码）',
    `avatar_url`  VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
    `sex`         VARCHAR(2)   DEFAULT NULL COMMENT '性别',
    `status`      INT          NOT NULL DEFAULT 1 COMMENT '状态 1:启用 0:禁用',
    `create_time` DATETIME     DEFAULT NULL COMMENT '创建时间',
    `update_time` DATETIME     DEFAULT NULL COMMENT '修改时间',
    `create_user` BIGINT       DEFAULT NULL COMMENT '创建人',
    `update_user` BIGINT       DEFAULT NULL COMMENT '修改人',
    PRIMARY KEY (`id`),
    UNIQUE KEY `idx_user_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='注册用户表';

-- =====================================================
-- 非遗传承人表
-- =====================================================
DROP TABLE IF EXISTS `inheritor`;
CREATE TABLE `inheritor` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`        VARCHAR(32)  NOT NULL COMMENT '姓名',
    `username`    VARCHAR(32)  NOT NULL COMMENT '用户名',
    `password`    VARCHAR(64)  NOT NULL COMMENT '密码（MD5加密）',
    `phone`       VARCHAR(11)  DEFAULT NULL COMMENT '手机号',
    `sex`         VARCHAR(2)   DEFAULT NULL COMMENT '性别',
    `id_number`   VARCHAR(18)  DEFAULT NULL COMMENT '身份证号',
    `region`      VARCHAR(100) DEFAULT NULL COMMENT '所属地区（如：江苏省苏州市）',
    `birth_date`            DATE         DEFAULT NULL COMMENT '出生日期',
    `birth_date_updated_at` DATETIME     DEFAULT NULL COMMENT '出生日期上次修改时间（一年限制）',
    `age`                   INT          DEFAULT NULL COMMENT '年龄（由出生日期自动计算）',
    `email`       VARCHAR(128) DEFAULT NULL COMMENT '邮箱（用于找回密码和审核通知）',
    `avatar_url`  VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
    `status`      INT          NOT NULL DEFAULT 2 COMMENT '状态 2:待审核 1:启用 0:禁用',
    `create_time` DATETIME     DEFAULT NULL COMMENT '创建时间',
    `update_time` DATETIME     DEFAULT NULL COMMENT '修改时间',
    `create_user` BIGINT       DEFAULT NULL COMMENT '创建人',
    `update_user` BIGINT       DEFAULT NULL COMMENT '修改人',
    PRIMARY KEY (`id`),
    UNIQUE KEY `idx_inheritor_username` (`username`),
    KEY `idx_inheritor_region` (`region`),
    KEY `idx_inheritor_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='非遗传承人表';

-- =====================================================
-- 管理员表
-- =====================================================
DROP TABLE IF EXISTS `admin`;
CREATE TABLE `admin` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`        VARCHAR(32)  NOT NULL COMMENT '姓名',
    `username`    VARCHAR(32)  NOT NULL COMMENT '用户名',
    `password`    VARCHAR(64)  NOT NULL COMMENT '密码（MD5加密）',
    `phone`       VARCHAR(11)  DEFAULT NULL COMMENT '手机号',
    `sex`         VARCHAR(2)   DEFAULT NULL COMMENT '性别',
    `avatar_url`  VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
    `status`      INT          NOT NULL DEFAULT 1 COMMENT '状态 1:启用 0:禁用',
    `create_time` DATETIME     DEFAULT NULL COMMENT '创建时间',
    `update_time` DATETIME     DEFAULT NULL COMMENT '修改时间',
    `create_user` BIGINT       DEFAULT NULL COMMENT '创建人',
    `update_user` BIGINT       DEFAULT NULL COMMENT '修改人',
    PRIMARY KEY (`id`),
    UNIQUE KEY `idx_admin_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员表';

-- =====================================================
-- 管理员测试数据（密码均为 123456 的 MD5）
-- =====================================================
INSERT INTO admin (name, username, password, phone, sex, status)
VALUES
    ('管理员',       'admin',     'e10adc3949ba59abbe56e057f20f883e', '13800138000', '男', 1),
    ('审核管理员',   'admin2',    'e10adc3949ba59abbe56e057f20f883e', '13800138001', '女', 1);

-- =====================================================
-- 注册用户测试数据（密码均为 123456 的 MD5）
-- =====================================================
INSERT INTO user (name, username, password, phone, email, sex, status)
VALUES
    ('张同学', 'user_zhang', 'e10adc3949ba59abbe56e057f20f883e', '13810001111', '3326353413@qq.com', '男', 1),
    ('李老师', 'user_li',    'e10adc3949ba59abbe56e057f20f883e', '13810002222', '3326353413@qq.com', '女', 1),
    ('王同学', 'user_wang',  'e10adc3949ba59abbe56e057f20f883e', '13810003333', '3326353413@qq.com', '男', 1),
    ('赵女士', 'user_zhao',  'e10adc3949ba59abbe56e057f20f883e', '13810004444', '3326353413@qq.com', '女', 1),
    ('陈先生', 'user_chen',  'e10adc3949ba59abbe56e057f20f883e', '13810005555', '3326353413@qq.com', '男', 1),
    ('刘女士', 'user_liu',   'e10adc3949ba59abbe56e057f20f883e', '13810006666', '3326353413@qq.com', '女', 1),
    ('周同学', 'user_zhou',  'e10adc3949ba59abbe56e057f20f883e', '13810007777', '3326353413@qq.com', '男', 1),
    ('吴老师', 'user_wu',    'e10adc3949ba59abbe56e057f20f883e', '13810008888', '3326353413@qq.com', '女', 1),
    ('郑先生', 'user_zheng', 'e10adc3949ba59abbe56e057f20f883e', '13810009999', '3326353413@qq.com', '男', 1),
    ('孙女士', 'user_sun',   'e10adc3949ba59abbe56e057f20f883e', '13810000001', '3326353413@qq.com', '女', 1);

-- =====================================================
-- 非遗传承人测试数据（密码均为 123456 的 MD5，status=1 表示已审核通过）
-- =====================================================
INSERT INTO inheritor (name, username, password, phone, sex, id_number, region, birth_date, age, email, status)
VALUES
    ('李秀英', 'inheritor_li',    'e10adc3949ba59abbe56e057f20f883e', '13920001111', '女', '320501197503120021', '江苏省苏州市',     '1975-03-12', 51, '3326353413@qq.com', 1),
    ('张德明', 'inheritor_zhang', 'e10adc3949ba59abbe56e057f20f883e', '13920002222', '男', '320282196808150013', '江苏省宜兴市',     '1968-08-15', 58, '3326353413@qq.com', 1),
    ('王师傅', 'inheritor_wang',  'e10adc3949ba59abbe56e057f20f883e', '13920003333', '男', '330523197211080034', '浙江省安吉县',     '1972-11-08', 54, '3326353413@qq.com', 1),
    ('陈巧云', 'inheritor_chen',  'e10adc3949ba59abbe56e057f20f883e', '13920004444', '女', '510105198204220025', '四川省成都市',     '1982-04-22', 44, '3326353413@qq.com', 1),
    ('刘建国', 'inheritor_liu',   'e10adc3949ba59abbe56e057f20f883e', '13920005555', '男', '360202197808160016', '江西省景德镇市',   '1978-08-16', 48, '3326353413@qq.com', 1),
    ('赵玉兰', 'inheritor_zhao',  'e10adc3949ba59abbe56e057f20f883e', '13920006666', '女', '410204198511200027', '河南省开封市',     '1985-11-20', 41, '3326353413@qq.com', 1),
    ('孙大师', 'inheritor_sun',   'e10adc3949ba59abbe56e057f20f883e', '13920007777', '男', '350505196503300018', '福建省泉州市',     '1965-03-30', 61, '3326353413@qq.com', 1),
    ('周小燕', 'inheritor_zhou',  'e10adc3949ba59abbe56e057f20f883e', '13920008888', '女', '530102198811050029', '云南省大理市',     '1988-11-05', 38, '3326353413@qq.com', 1),
    ('杨志强', 'inheritor_yang',  'e10adc3949ba59abbe56e057f20f883e', '13920009999', '男', '610102199003220010', '陕西省西安市',     '1990-03-22', 36, '3326353413@qq.com', 1),
    ('待审核传承人', 'inheritor_pending', 'e10adc3949ba59abbe56e057f20f883e', '13920000001', '男', '440101199501010011', '广东省广州市', '1995-01-01', 31, '3326353413@qq.com', 2);
-- =====================================================
-- 预约订单表 DDL
-- 用户向工坊（传承人）发送预约订单
-- =====================================================
DROP TABLE IF EXISTS appointment_order;

CREATE TABLE appointment_order (
    id               BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '订单ID',
    user_id          BIGINT       NOT NULL COMMENT '预约用户ID（关联user表）',
    user_name        VARCHAR(255) COMMENT '预约用户姓名',
    phone            VARCHAR(50)  COMMENT '联系电话',
    inheritor_id     BIGINT       COMMENT '传承人工坊ID（关联inheritor表）',
    project_id       BIGINT       COMMENT '关联非遗项目ID（可选）',
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
-- 用户ID对应: 1=张同学 2=李老师 3=王同学 4=赵女士 5=陈先生 6=刘女士 7=周同学 8=吴老师
-- 传承人ID对应: 1=李秀英(苏州) 2=张德明(宜兴) 3=王师傅(安吉) 4=陈巧云(成都) 5=刘建国(景德镇)
-- =====================================================
INSERT INTO appointment_order (user_id, user_name, phone, inheritor_id, appointment_date, time_slot, people_count, remark, status, create_time, update_time)
VALUES
    -- 李秀英（苏州苏绣/剪纸）的订单
    (1, '张同学', '138****6789', 1, '2026-07-20', '上午 9:00-11:00',  3, '想学习剪纸基础技法',                         0, NOW(), NOW()),
    (2, '李老师', '139****8901', 1, '2026-07-18', '下午 14:00-16:00', 5, '学校社团体验活动',                             1, NOW(), NOW()),
    (3, '王同学', '136****2345', 1, '2026-07-15', '上午 10:00-12:00', 2, '一对一深入学习苏绣针法',                       2, NOW(), NOW()),
    (4, '赵女士', '137****4567', 1, '2026-07-22', '下午 15:00-17:00', 4, '临时有事取消',                                 3, NOW(), NOW()),
    (5, '陈先生', '138****7890', 1, '2026-07-25', '上午 9:00-11:00',  2, '想带女儿体验剪纸文化',                         0, NOW(), NOW()),
    -- 张德明（宜兴紫砂）的订单
    (6, '刘女士', '138****8901', 2, '2026-07-19', '下午 14:00-16:00', 3, '学习紫砂壶制作入门',                           1, NOW(), NOW()),
    (7, '周同学', '138****9012', 2, '2026-07-28', '上午 10:00-12:00', 1, '毕业论文研究紫砂工艺',                         0, NOW(), NOW()),
    -- 王师傅（安吉竹编）的订单
    (8, '吴老师', '138****0123', 3, '2026-07-17', '上午 9:00-11:00',  4, '带学生参观学习竹编技艺',                       1, NOW(), NOW()),
    (1, '张同学', '138****6789', 3, '2026-08-01', '下午 14:00-16:00', 2, '竹编进阶学习',                                 0, NOW(), NOW()),
    -- 陈巧云（成都蜀绣）的订单
    (2, '李老师', '139****8901', 4, '2026-07-23', '上午 10:00-12:00', 3, '体验蜀绣双面绣技法',                           0, NOW(), NOW()),
    -- 刘建国（景德镇陶瓷）的订单
    (9, '郑先生', '138****1111', 5, '2026-07-26', '下午 15:00-17:00', 2, '想制作一对青花瓷杯',                           0, NOW(), NOW()),
    (10,'孙女士', '138****2222', 5, '2026-07-30', '上午 9:00-11:00',  4, '亲子陶艺体验活动',                             0, NOW(), NOW());

-- =====================================================
-- 文创商品表 DDL
-- 传承人可上架自己的文创商品，用户可在商城浏览购买
-- =====================================================
DROP TABLE IF EXISTS product;

CREATE TABLE product (
    id               BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '商品ID',
    name             VARCHAR(255)   NOT NULL COMMENT '商品名称',
    description      VARCHAR(2000)  COMMENT '商品描述',
    price            DECIMAL(10,2)  NOT NULL COMMENT '商品价格',
    stock            INT            NOT NULL DEFAULT 0 COMMENT '库存数量',
    image_url        VARCHAR(500)   COMMENT '商品图片URL',
    model_url        VARCHAR(500)   DEFAULT NULL COMMENT '3D模型URL（glb/gltf格式）',
    category         VARCHAR(100)   COMMENT '商品分类（如剪纸、刺绣、陶艺、竹编等）',
    inheritor_id     BIGINT         COMMENT '传承人ID（关联对应表）',
    inheritor_name   VARCHAR(255)   COMMENT '传承人姓名',
    status           INT            NOT NULL DEFAULT 1 COMMENT '状态：1-上架 0-下架',
    create_time      DATETIME       COMMENT '创建时间',
    update_time      DATETIME       COMMENT '更新时间',
    create_user      BIGINT         COMMENT '创建人ID',
    update_user      BIGINT         COMMENT '更新人ID'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文创商品表';

CREATE INDEX idx_product_category    ON product(category);
CREATE INDEX idx_product_inheritor   ON product(inheritor_id);
CREATE INDEX idx_product_status      ON product(status);

-- =====================================================
-- 商品订单表 DDL
-- 用户购买文创商品的订单
-- =====================================================
DROP TABLE IF EXISTS product_order;

CREATE TABLE product_order (
    id               BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '订单ID',
    user_id          BIGINT         NOT NULL COMMENT '下单用户ID（关联对应表）',
    user_name        VARCHAR(255)   COMMENT '下单用户姓名',
    phone            VARCHAR(50)    COMMENT '联系电话',
    address          VARCHAR(500)   COMMENT '收货地址',
    product_id       BIGINT         NOT NULL COMMENT '商品ID（关联product表）',
    product_name     VARCHAR(255)   COMMENT '商品名称（冗余）',
    product_image    VARCHAR(500)   COMMENT '商品图片（冗余）',
    price            DECIMAL(10,2)  NOT NULL COMMENT '购买时单价',
    quantity         INT            NOT NULL DEFAULT 1 COMMENT '购买数量',
    total_amount     DECIMAL(10,2)  NOT NULL COMMENT '订单总金额',
    inheritor_id     BIGINT         COMMENT '传承人ID（关联对应表）',
    remark           VARCHAR(500)   COMMENT '用户备注',
    status           INT            NOT NULL DEFAULT 0 COMMENT '订单状态：0-待发货, 1-已发货, 2-已完成, 3-已取消',
    courier_company  VARCHAR(100)   COMMENT '快递公司',
    tracking_number  VARCHAR(200)   COMMENT '快递单号',
    create_time      DATETIME       COMMENT '创建时间',
    update_time      DATETIME       COMMENT '更新时间',
    create_user      BIGINT         COMMENT '创建人ID',
    update_user      BIGINT         COMMENT '更新人ID'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品订单表';

CREATE INDEX idx_po_user_id        ON product_order(user_id);
CREATE INDEX idx_po_product_id     ON product_order(product_id);
CREATE INDEX idx_po_inheritor_id   ON product_order(inheritor_id);
CREATE INDEX idx_po_status         ON product_order(status);

-- =====================================================
-- 商品评价表
-- =====================================================
DROP TABLE IF EXISTS `product_review`;
CREATE TABLE `product_review` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `product_id`  BIGINT       NOT NULL COMMENT '商品ID',
    `user_id`     BIGINT       NOT NULL COMMENT '评价用户ID',
    `user_name`   VARCHAR(32)  NOT NULL COMMENT '评价用户名',
    `rating`      INT          DEFAULT NULL COMMENT '评分 1-5，顶级评价必填，回复时为 NULL',
    `content`     TEXT         NOT NULL COMMENT '评价内容',
    `parent_id`   BIGINT       DEFAULT NULL COMMENT '父评价ID，NULL表示顶级评价',
    `is_deleted`  TINYINT      DEFAULT 0 COMMENT '软删除：0正常 1已删除',
    `create_time` DATETIME     DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_pr_product_id` (`product_id`),
    INDEX `idx_pr_user_id` (`user_id`),
    INDEX `idx_pr_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品评价表';

-- =====================================================
-- 文创商品测试数据
-- 传承人ID对应: 1=李秀英(苏州) 2=张德明(宜兴) 3=王师傅(安吉) 4=陈巧云(成都)
--              5=刘建国(景德镇) 6=赵玉兰(开封) 7=孙大师(泉州) 8=周小燕(大理)
-- =====================================================
INSERT INTO product (name, description, price, stock, image_url, category, inheritor_id, inheritor_name, status, create_time, update_time)
VALUES
    -- 李秀英 - 剪纸/苏绣类
    ('非遗剪纸书签套装', '精选校园剪纸作品印制，一套四枚，包含梅兰竹菊主题，送礼收藏皆宜。', 39.90, 100, 'https://picsum.photos/seed/jianzhi-bookmark/400/300', '剪纸', 1, '李秀英', 1, NOW(), NOW()),
    ('苏绣手帕礼盒',     '绣娘纯手工制作，传统花鸟纹样，精致典雅，含精美礼盒包装。', 128.00, 50, 'https://picsum.photos/seed/suxiu-handkerchief/400/300', '刺绣', 1, '李秀英', 1, NOW(), NOW()),
    ('校园非遗明信片集', '精选12张非遗主题手绘明信片，记录校园中的非遗之美。', 25.00, 200, 'https://picsum.photos/seed/feiyi-postcard/400/300', '综合', 1, '李秀英', 1, NOW(), NOW()),
    ('刺绣随身镜',       '小巧便携，双面刺绣装饰，古典与现代的完美结合。', 58.00, 80, 'https://picsum.photos/seed/cixiu-mirror/400/300', '刺绣', 1, '李秀英', 1, NOW(), NOW()),
    ('剪纸窗花年画套装', '传统纹样手工剪纸，含福字、生肖等图案，适合新年装饰。', 35.00, 120, 'https://picsum.photos/seed/jianzhi-window/400/300', '剪纸', 1, '李秀英', 1, NOW(), NOW()),
    -- 张德明 - 紫砂陶艺类
    ('紫砂小壶·禅意',    '手工紫砂壶，容量200ml，传统石瓢器型，适合日常品茗。', 368.00, 20, 'https://picsum.photos/seed/zisha-teapot/400/300', '陶艺', 2, '张德明', 1, NOW(), NOW()),
    ('紫砂主人杯·圆润',  '原矿紫泥手作，口感温润，一壶一盏的文人雅趣。', 158.00, 35, 'https://picsum.photos/seed/zisha-cup/400/300', '陶艺', 2, '张德明', 1, NOW(), NOW()),
    ('紫砂茶宠·金蟾',    '萌趣金蟾造型，养茶佳品，越养越润。', 68.00, 60, 'https://picsum.photos/seed/zisha-teapet/400/300', '陶艺', 2, '张德明', 1, NOW(), NOW()),
    -- 王师傅 - 竹编类
    ('竹编收纳盒三件套', '手工竹编工艺，大中小三件套，天然环保，实用美观。', 88.00, 30, 'https://picsum.photos/seed/zhubian-box/400/300', '竹编', 3, '王师傅', 1, NOW(), NOW()),
    ('竹编茶具托盘',     '天然竹材手工编织，简约雅致，茶席必备。', 128.00, 25, 'https://picsum.photos/seed/zhubian-tray/400/300', '竹编', 3, '王师傅', 1, NOW(), NOW()),
    ('竹编果篮·手提',    '经典手提造型，结实耐用，田园风家居好物。', 65.00, 40, 'https://picsum.photos/seed/zhubian-basket/400/300', '竹编', 3, '王师傅', 1, NOW(), NOW()),
    -- 陈巧云 - 蜀绣类
    ('蜀绣团扇·芙蓉锦鲤', '双面蜀绣，芙蓉花与锦鲤图案，团扇一柄，古韵悠然。', 298.00, 15, 'https://picsum.photos/seed/shuxiu-fan/400/300', '刺绣', 4, '陈巧云', 1, NOW(), NOW()),
    ('蜀绣胸针·牡丹',    '手工蜀绣胸针，牡丹纹样，精致百搭。', 45.00, 90, 'https://picsum.photos/seed/shuxiu-pin/400/300', '刺绣', 4, '陈巧云', 1, NOW(), NOW()),
    -- 刘建国 - 景德镇陶瓷类
    ('青花瓷茶杯套装',   '景德镇手绘青花，一壶四杯，传统缠枝莲纹。', 458.00, 18, 'https://picsum.photos/seed/qinghua-cup/400/300', '陶艺', 5, '刘建国', 1, NOW(), NOW()),
    ('粉彩小碟·四季',    '春夏秋冬四只小碟，粉彩手绘，雅致实用。', 188.00, 28, 'https://picsum.photos/seed/fencai-dish/400/300', '陶艺', 5, '刘建国', 1, NOW(), NOW()),
    ('陶瓷风铃·莲花',    '手工捏塑莲花造型，清脆悦耳，禅意装饰。', 48.00, 70, 'https://picsum.photos/seed/taoci-bell/400/300', '陶艺', 5, '刘建国', 1, NOW(), NOW()),
    -- 赵玉兰 - 汴绣类
    ('汴绣卷轴·清明上河图局部', '北宋名画汴绣复刻，局部精绣，含卷轴装裱。', 880.00, 5, 'https://picsum.photos/seed/bianxiu-scroll/400/300', '刺绣', 6, '赵玉兰', 1, NOW(), NOW()),
    ('汴绣书签·宋韵',    '宋代花鸟纹样汴绣书签，精致小巧，文房雅物。', 32.00, 150, 'https://picsum.photos/seed/bianxiu-bookmark/400/300', '刺绣', 6, '赵玉兰', 1, NOW(), NOW()),
    -- 孙大师 - 泉州木雕类
    ('木雕摆件·福鹿',    '泉州木雕，黄杨木手工雕刻，福鹿呈祥，案头雅玩。', 520.00, 10, 'https://picsum.photos/seed/mudiao-deer/400/300', '木雕', 7, '孙大师', 1, NOW(), NOW()),
    ('木雕挂饰·平安扣',  '小叶紫檀平安扣，手工打磨，随身佩戴平安吉祥。', 78.00, 50, 'https://picsum.photos/seed/mudiao-pendant/400/300', '木雕', 7, '孙大师', 1, NOW(), NOW()),
    -- 周小燕 - 大理扎染类
    ('扎染围巾·苍山洱海', '大理白族扎染，纯植物染料，蓝白渐变如苍山洱海。', 98.00, 45, 'https://picsum.photos/seed/zharan-scarf/400/300', '扎染', 8, '周小燕', 1, NOW(), NOW()),
    ('扎染桌布·靛蓝',    '手工扎染棉麻桌布，几何纹样，自然质朴。', 138.00, 30, 'https://picsum.photos/seed/zharan-tablecloth/400/300', '扎染', 8, '周小燕', 1, NOW(), NOW()),
    ('扎染帆布包·大理蓝', '帆布材质，扎染工艺，轻便实用，文艺范十足。', 65.00, 80, 'https://picsum.photos/seed/zharan-bag/400/300', '扎染', 8, '周小燕', 1, NOW(), NOW()),
    -- 下架商品示例
    ('限量版苏绣团扇·百鸟朝凤', '百鸟朝凤主题精工苏绣团扇，限量发售。', 1280.00, 0, 'https://picsum.photos/seed/suxiu-fan-limited/400/300', '刺绣', 1, '李秀英', 0, NOW(), NOW());

-- =====================================================
-- 商品订单测试数据
-- 状态: 0-待发货, 1-已发货, 2-已完成, 3-已取消
-- 用户ID: 1=张同学 2=李老师 3=王同学 4=赵女士 5=陈先生 6=刘女士 7=周同学 8=吴老师
-- 商品ID: 1=剪纸书签 2=苏绣手帕 6=紫砂壶 9=竹编收纳盒 12=蜀绣团扇 14=青花瓷套装 19=木雕摆件 21=扎染围巾
-- =====================================================
INSERT INTO product_order (user_id, user_name, phone, address, product_id, product_name, product_image, price, quantity, total_amount, inheritor_id, remark, status, create_time, update_time)
VALUES
    -- 张同学的订单
    (1, '张同学', '138****6789', '北京市海淀区中关村大街1号',  1,  '非遗剪纸书签套装',   'https://picsum.photos/seed/jianzhi-bookmark/400/300', 39.90,  2, 79.80,    1, '送同学校庆礼物',         2, NOW(), NOW()),
    (1, '张同学', '138****6789', '北京市海淀区中关村大街1号',  21, '扎染围巾·苍山洱海', 'https://picsum.photos/seed/zharan-scarf/400/300', 98.00,  1, 98.00,    8, '围巾颜色请发蓝白渐变款', 0, NOW(), NOW()),
    -- 李老师的订单
    (2, '李老师', '139****8901', '上海市浦东新区张江路100号',  2,  '苏绣手帕礼盒',       'https://picsum.photos/seed/suxiu-handkerchief/400/300', 128.00, 1, 128.00,   1, '作为教师节礼物',         1, NOW(), NOW()),
    (2, '李老师', '139****8901', '上海市浦东新区张江路100号',  9,  '竹编收纳盒三件套',   'https://picsum.photos/seed/zhubian-box/400/300', 88.00,  2, 176.00,   3, NULL,                      0, NOW(), NOW()),
    -- 王同学的订单
    (3, '王同学', '136****2345', '广州市天河区体育西路88号',    6,  '紫砂小壶·禅意',      'https://picsum.photos/seed/zisha-teapot/400/300', 368.00, 1, 368.00,   2, '请问可以刻字吗',         2, NOW(), NOW()),
    (3, '王同学', '136****2345', '广州市天河区体育西路88号',    18, '汴绣书签·宋韵',     'https://picsum.photos/seed/bianxiu-bookmark/400/300', 32.00,  3, 96.00,    6, NULL,                      1, NOW(), NOW()),
    -- 赵女士的订单
    (4, '赵女士', '137****4567', '杭州市西湖区文三路200号',     12, '蜀绣团扇·芙蓉锦鲤', 'https://picsum.photos/seed/shuxiu-fan/400/300', 298.00, 1, 298.00,   4, '包装请精美一些',         0, NOW(), NOW()),
    (4, '赵女士', '137****4567', '杭州市西湖区文三路200号',     13, '蜀绣胸针·牡丹',     'https://picsum.photos/seed/shuxiu-pin/400/300', 45.00,  2, 90.00,    4, NULL,                      2, NOW(), NOW()),
    -- 陈先生的订单
    (5, '陈先生', '138****7890', '成都市武侯区天府大道300号',   14, '青花瓷茶杯套装',    'https://picsum.photos/seed/qinghua-cup/400/300', 458.00, 1, 458.00,   5, '请附赠杯垫',             1, NOW(), NOW()),
    (5, '陈先生', '138****7890', '成都市武侯区天府大道300号',   22, '扎染桌布·靛蓝',     'https://picsum.photos/seed/zharan-tablecloth/400/300', 138.00, 1, 138.00,   8, NULL,                      3, NOW(), NOW()),
    -- 刘女士的订单
    (6, '刘女士', '138****8901', '西安市雁塔区科技路50号',      19, '木雕摆件·福鹿',     'https://picsum.photos/seed/mudiao-deer/400/300', 520.00, 1, 520.00,   7, '希望木色偏深一些',       2, NOW(), NOW()),
    -- 周同学的订单
    (7, '周同学', '138****9012', '南京市鼓楼区中山路66号',      3,  '校园非遗明信片集',  'https://picsum.photos/seed/feiyi-postcard/400/300', 25.00,  5, 125.00,   1, '班级活动纪念品',         2, NOW(), NOW()),
    (7, '周同学', '138****9012', '南京市鼓楼区中山路66号',      16, '陶瓷风铃·莲花',     'https://picsum.photos/seed/taoci-bell/400/300', 48.00,  2, 96.00,    5, NULL,                      0, NOW(), NOW()),
    -- 吴老师的订单
    (8, '吴老师', '138****0123', '苏州市姑苏区干将路168号',     7,  '紫砂主人杯·圆润',   'https://picsum.photos/seed/zisha-cup/400/300', 158.00, 2, 316.00,   2, '分别寄两个地址',         1, NOW(), NOW()),
    (8, '吴老师', '138****0123', '苏州市姑苏区干将路168号',     10, '竹编茶具托盘',      'https://picsum.photos/seed/zhubian-tray/400/300', 128.00, 1, 128.00,   3, NULL,                      2, NOW(), NOW());

-- =====================================================
-- 传承人详细信息表
-- =====================================================
DROP TABLE IF EXISTS inheritor_profile;

CREATE TABLE inheritor_profile (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    employee_id     BIGINT NOT NULL UNIQUE COMMENT '关联employee表',
    honor_title     VARCHAR(100) COMMENT '荣誉称号',
    self_intro      TEXT COMMENT '个人简介',
    craft_types     VARCHAR(500) COMMENT '擅长技艺，逗号分隔',
    address         VARCHAR(200) COMMENT '所在地',
    avatar_url      VARCHAR(500) COMMENT '头像URL',
    create_time     DATETIME COMMENT '创建时间',
    update_time     DATETIME COMMENT '修改时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='传承人详细信息表';

-- 传承人详细信息测试数据（employee_id 对应 inheritor 表的 id）
INSERT INTO inheritor_profile (employee_id, honor_title, self_intro, craft_types, address, avatar_url, create_time, update_time)
VALUES
    (1, '国家级非遗传承人 · 苏绣',
     '从事苏绣创作近五十年，作品多次获国家级大奖，致力于将刺绣技艺带入校园，让更多年轻人了解和喜爱传统手工艺。',
     '苏绣,双面绣,乱针绣,剪纸',
     '江苏省苏州市姑苏区十全街128号',
     'https://picsum.photos/seed/inheritor1/200/200', NOW(), NOW()),

    (2, '江苏省工艺美术大师 · 紫砂',
     '宜兴紫砂世家第四代传人，从事紫砂壶制作四十余年，擅长传统器型与创新造型结合，作品远销海内外。',
     '紫砂壶,陶艺,紫砂雕塑,紫砂杯',
     '江苏省宜兴市丁蜀镇紫砂路56号',
     'https://picsum.photos/seed/inheritor2/200/200', NOW(), NOW()),

    (3, '浙江省非遗传承人 · 竹编',
     '安吉竹编技艺传承人，自幼学习竹编工艺，将传统竹编与现代生活美学结合，开创了竹编文创系列产品。',
     '竹编,竹雕,竹工艺',
     '浙江省湖州市安吉县天荒坪镇竹艺村8号',
     'https://picsum.photos/seed/inheritor3/200/200', NOW(), NOW()),

    (4, '四川省工艺美术大师 · 蜀绣',
     '蜀绣代表性传承人，师从多位蜀绣大师，擅长双面绣、芙蓉锦鲤等经典题材，作品温婉细腻。',
     '蜀绣,双面绣,刺绣',
     '四川省成都市锦江区春熙路189号',
     'https://picsum.photos/seed/inheritor4/200/200', NOW(), NOW()),

    (5, '国家级非遗传承人 · 景德镇陶瓷',
     '景德镇青花瓷非遗传承人，从事陶瓷艺术创作三十余年，擅长青花、粉彩、颜色釉等传统工艺。',
     '青花瓷,粉彩,陶艺,陶瓷雕塑,颜色釉',
     '江西省景德镇市珠山区陶阳路99号',
     'https://picsum.photos/seed/inheritor5/200/200', NOW(), NOW()),

    (6, '河南省非遗传承人 · 汴绣',
     '开封汴绣技艺传承人，专攻宋代名画复刻绣制，作品《清明上河图》刺绣卷轴获全国工艺美术金奖。',
     '汴绣,刺绣,双面绣,宋绣',
     '河南省开封市龙亭区宋都御街32号',
     'https://picsum.photos/seed/inheritor6/200/200', NOW(), NOW()),

    (7, '国家级非遗传承人 · 木雕',
     '泉州木雕技艺代表性传承人，从事木雕创作五十载，精通黄杨木雕、龙眼木雕等多种雕刻技法。',
     '木雕,黄杨木雕,根雕,木刻',
     '福建省泉州市鲤城区中山路201号',
     'https://picsum.photos/seed/inheritor7/200/200', NOW(), NOW()),

    (8, '云南省非遗传承人 · 扎染',
     '大理白族扎染技艺传承人，坚持使用纯植物染料手工扎染，作品充满苍山洱海的自然之美。',
     '扎染,白族扎染,植物染,布艺',
     '云南省大理白族自治州大理古城人民路78号',
     'https://picsum.photos/seed/inheritor8/200/200', NOW(), NOW()),

    (9, '陕西省非遗传承人 · 皮影',
     '陕西皮影戏传承人，精通皮影雕刻与表演，致力于将皮影艺术与现代舞台技术融合创新。',
     '皮影,皮影雕刻,剪纸',
     '陕西省西安市碑林区书院门15号',
     'https://picsum.photos/seed/inheritor9/200/200', NOW(), NOW()),

    (10, '待审核传承人',
     '广东广彩技艺爱好者，正在申请非遗传承人资格，擅长广彩瓷绘制与岭南传统工艺研究。',
     '广彩,陶艺,岭南工艺',
     '广东省广州市荔湾区恩宁路56号',
     'https://picsum.photos/seed/inheritor10/200/200', NOW(), NOW());

-- =====================================================
-- 非遗知识库文章表
-- =====================================================
DROP TABLE IF EXISTS knowledge_article;

CREATE TABLE knowledge_article (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    author_id       BIGINT NOT NULL COMMENT '作者ID（传承人）',
    title           VARCHAR(200) NOT NULL COMMENT '文章标题',
    category        VARCHAR(50) COMMENT '分类',
    content         TEXT COMMENT '文章内容',
    cover_url       VARCHAR(500) COMMENT '封面图',
    status          INT DEFAULT 0 COMMENT '0-草稿 1-已发布',
    view_count      INT DEFAULT 0 COMMENT '浏览量',
    create_time     DATETIME COMMENT '创建时间',
    update_time     DATETIME COMMENT '修改时间',
    create_user     BIGINT COMMENT '创建人',
    update_user     BIGINT COMMENT '修改人',
    INDEX idx_author (author_id),
    INDEX idx_category (category),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='非遗知识库文章表';

-- =====================================================
-- 非遗知识库测试数据
-- 作者ID对应: 1=李秀英 2=张德明 3=王师傅 4=陈巧云
-- =====================================================
INSERT INTO knowledge_article (author_id, title, category, content, cover_url, status, view_count, create_time)
VALUES
    (1, '苏绣的千年传承：从闺阁到世界非遗', '刺绣',
     '苏绣，中国四大名绣之首，发源于苏州吴县一带，已有两千余年历史。苏绣以"平、齐、细、密、匀、顺、和、光"八大特色著称，针法细腻，色彩清雅。\n\n早在春秋战国时期，苏州刺绣就已出现。明清两代，苏州成为全国丝织业中心，苏绣达到鼎盛，不仅宫廷刺绣选用苏绣，民间刺绣也极为发达。沈寿、杨守玉等刺绣大师将苏绣推向艺术高峰。\n\n2006年，苏绣被列入第一批国家级非物质文化遗产名录。如今，苏州镇湖、高新区等地仍是苏绣的主要传承地，新一代传承人正在将传统技艺与现代设计相融合，让苏绣走进年轻人的日常生活。',
     'https://picsum.photos/seed/suxiu/400/300', 1, 1280, NOW()),

    (2, '紫砂壶鉴赏入门：泥料、器型与工艺', '陶艺',
     '紫砂壶产自江苏宜兴，以其独特的双气孔结构和优良的泡茶性能，被誉为"壶中之王"。对于初学者来说，鉴赏紫砂壶可以从三个维度入手。\n\n首先是泥料。紫砂泥分为紫泥、红泥、绿泥（本山绿泥）三大类，不同泥料烧成后呈现不同的色泽和质感。原矿泥料制成的好壶，色泽温润内敛，手感细腻。\n\n其次是器型。紫砂壶历经数百年发展，形成了光素器、筋纹器、花器三大类型。石瓢、仿古、西施、井栏等经典器型百看不厌。\n\n最后是工艺。一把好壶讲究"三点一线"（壶嘴、壶钮、壶把在一条直线）、口盖严密、出水流畅、断水干脆。手工拍打成型、明针功夫等传统工艺更显匠人精神。',
     'https://picsum.photos/seed/zisha/400/300', 1, 960, NOW()),

    (3, '安吉竹编：指尖上的绿色非遗', '竹编',
     '浙江安吉，中国著名的竹乡，孕育了源远流长的竹编技艺。安吉竹编以当地优质毛竹为原料，经过选料、破篾、刮青、编织等数十道工序，将翠竹化为精美的工艺品和生活用品。\n\n传统安吉竹编工艺可分为细丝竹编和粗丝竹编，细丝竹编薄如蝉翼，可编织花鸟鱼虫等精细图案；粗丝竹编则坚固耐用，常用于制作篮筐、屏风等实用器物。\n\n如今，安吉竹编传承人将传统技艺与现代设计理念相结合，开发出了竹编灯饰、竹编包袋、竹编茶具等一系列文创产品，让古老的竹编技艺焕发出新的生机。',
     'https://picsum.photos/seed/bamboo/400/300', 1, 850, NOW()),

    (4, '蜀绣与蜀锦：巴蜀大地的丝织双绝', '刺绣',
     '四川成都，古称蜀郡，是丝绸之路的重要节点。蜀绣与蜀锦并称"蜀中双绝"，各具特色，相得益彰。\n\n蜀绣以软缎和彩丝为主要原料，针法多达120余种，尤以"晕针"最具特色，能使色彩过渡自然柔和。蜀绣题材丰富，熊猫、芙蓉花、锦鲤等四川元素是其标志性图案。\n\n蜀锦则是一种提花织锦，历史悠久，可追溯至战国时期。蜀锦以经线起花、色彩艳丽著称，"寸锦寸金"，曾是古代皇室贡品。\n\n2006年，蜀绣被列入第一批国家级非物质文化遗产名录；2009年，蜀锦织造技艺入选联合国人类非物质文化遗产代表作名录。',
     'https://picsum.photos/seed/shuxiu/400/300', 1, 1120, NOW()),

    (1, '校园非遗教育：让传统技艺走进课堂', '校园活动',
     '近年来，越来越多的高校将非物质文化遗产引入校园，开设非遗选修课、成立非遗社团、举办非遗文化节，让青年学子近距离感受传统技艺的魅力。\n\n在课程设置方面，剪纸、陶艺、刺绣、书法等非遗项目成为最受欢迎的选修课。学生们在学习技艺的同时，也加深了对传统文化的理解和认同。\n\n在社团活动方面，非遗社团通过举办手作体验、传承人讲座、非遗市集等活动，吸引了大量学生参与。一些学生还利用新媒体平台，拍摄短视频传播非遗知识，形成了线上线下相结合的非遗传播新生态。\n\n非遗进校园，不仅是技艺的传承，更是文化自信的培育。让年轻一代成为非遗的传承者、传播者和创新者，是非遗保护工作的重要方向。',
     'https://picsum.photos/seed/campus/400/300', 1, 1560, NOW()),

    (2, '景德镇陶瓷七十二道工序探秘', '陶艺',
     '景德镇，享誉世界的"瓷都"，其手工制瓷技艺已有千年历史。从一抔瓷土到一件精美的瓷器，需要经过整整七十二道工序，道道讲究，环环相扣。\n\n主要工序包括：采石、粉碎、淘洗、炼泥、拉坯、利坯、晒坯、刻花、施釉、烧窑等。其中，拉坯是最考验功力的环节，陶工需要在旋转的辘轳上将泥团拉成理想的器型。施釉则决定了瓷器表面的光泽和色彩。\n\n入窑一色，出窑万彩。景德镇传统柴窑和现代气窑各有特色，窑变产生的意外之美往往令人惊叹。\n\n如今，景德镇吸引了来自世界各地的年轻陶艺家，他们在这里建立了自己的工作室，将现代审美融入传统工艺，让千年瓷都焕发出新的活力。',
     'https://picsum.photos/seed/jingdezhen/400/300', 1, 780, NOW()),

    (4, '剪纸艺术的南北流派与当代创新', '剪纸',
     '中国剪纸艺术历史悠久，地域风格鲜明，大致可分为北方剪纸和南方剪纸两大流派。\n\n北方剪纸以陕西、河北、山东为代表，风格粗犷豪放、造型简练，多用大块面的阴刻手法，内容以人物、动物、吉祥图案为主。每逢春节，北方农家窗户上贴满红色窗花，喜庆热烈。\n\n南方剪纸以江苏、浙江、广东为代表，风格纤细秀丽、精巧繁复，善于运用阳刻和细线造型。扬州剪纸、乐清细纹刻纸、佛山铜凿剪纸等各具特色。\n\n在当代，剪纸艺术已从传统窗花拓展到装置艺术、服装设计、动画创作等领域。一些年轻艺术家用剪纸语言表达当代社会议题，让这一古老技艺展现出全新的艺术魅力。',
     'https://picsum.photos/seed/jianzhi/400/300', 1, 690, NOW()),

    (3, '竹文化在中国传统文化中的地位', '竹编',
     '竹，在中国文化中有着独特而崇高的地位。它不仅是实用的建筑材料和生活器具的原料，更是一种文化符号和精神象征。\n\n在物质层面，竹子被广泛应用于建筑（竹楼、竹桥）、家具（竹椅、竹榻）、餐具（竹筷、竹篮）、文具（竹简、竹笔）等各个领域。浙江安吉、江西井冈山、四川宜宾等地都是著名的竹产区。\n\n在精神层面，竹与梅、兰、菊并称"四君子"，象征正直、谦逊、坚韧的品格。"宁可食无肉，不可居无竹"（苏轼）、"未出土时先有节，及凌云处尚虚心"等诗句表达了中国文人对竹的钟爱。\n\n如今，竹文化已成为中国传统文化的重要组成部分，竹编技艺作为非物质文化遗产，正在通过文创产品、体验工坊等形式走进现代生活。',
     'https://picsum.photos/seed/zhubian/400/300', 1, 540, NOW()),

    (1, '对话非遗传承人：我为什么坚守苏绣三十载', '传承人专访',
     '本期专访我们邀请到了苏州刺绣传承人李秀英老师。李老师从事苏绣创作已有三十余年，作品多次获得国家级工艺美术大奖。\n\n问：是什么让您坚持苏绣这么多年？\n\n答：热爱。从小看着母亲和祖母在绣架前飞针走线，那种娴静的美深深印在了我的心里。苏绣不仅仅是一门手艺，更是一种生活态度。一针一线，需要耐心，需要专注，需要心静如水。\n\n问：苏绣传承面临哪些挑战？\n\n答：最大的挑战是人才断层。现在年轻人生活节奏快，很少有愿意花几年甚至十几年时间潜心学艺的。苏绣学习周期长，经济回报慢，这让很多年轻人望而却步。但我们也在努力改变，比如开设短期体验课，开发苏绣文创产品，让更多人先接触、再了解、最后爱上苏绣。\n\n问：您对苏绣的未来有什么期待？\n\n答：我希望苏绣不仅是博物馆里的展品，更能走进寻常百姓家。我们正在尝试将苏绣元素融入服装、包袋、家居用品，让传统技艺服务于现代生活。',
     'https://picsum.photos/seed/interview/400/300', 1, 2100, NOW());


-- =====================================================
-- 文章评论表
-- =====================================================
DROP TABLE IF EXISTS article_comment;

CREATE TABLE article_comment (
    id          BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    article_id  BIGINT NOT NULL COMMENT '文章ID',
    user_id     BIGINT NOT NULL COMMENT '评论用户ID',
    user_name   VARCHAR(32) NOT NULL COMMENT '评论用户名',
    content     TEXT NOT NULL COMMENT '评论内容',
    parent_id   BIGINT DEFAULT NULL COMMENT '父评论ID，NULL表示顶级评论',
    is_deleted  TINYINT DEFAULT 0 COMMENT '软删除：0正常 1已删除',
    create_time DATETIME DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (id),
    INDEX idx_ac_article_id (article_id),
    INDEX idx_ac_user_id (user_id),
    INDEX idx_ac_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章评论表';

-- =====================================================
-- 非遗项目表
-- =====================================================
DROP TABLE IF EXISTS heritage_project;

CREATE TABLE heritage_project (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    inheritor_id    BIGINT NOT NULL COMMENT '传承人ID',
    title           VARCHAR(200) NOT NULL COMMENT '项目标题',
    category        VARCHAR(50) COMMENT '项目分类',
    description     TEXT COMMENT '项目描述',
    cover_url       VARCHAR(500) COMMENT '封面图',
    location        VARCHAR(200) COMMENT '活动地点',
    start_date      DATE COMMENT '开始日期',
    end_date        DATE COMMENT '结束日期',
    max_people      INT COMMENT '最大参与人数',
    current_people  INT DEFAULT 0 COMMENT '当前报名人数',
    status          INT DEFAULT 0 COMMENT '0-下架 1-上架',
    project_type    TINYINT NOT NULL DEFAULT 0 COMMENT '项目类型：0-公益(非盈利), 1-非公益(盈利)',
    price           DECIMAL(10,2) DEFAULT NULL COMMENT '价格（非公益项目使用）',
    create_time     DATETIME COMMENT '创建时间',
    update_time     DATETIME COMMENT '修改时间',
    create_user     BIGINT COMMENT '创建人',
    update_user     BIGINT COMMENT '修改人',
    INDEX idx_inheritor (inheritor_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='非遗项目表';

-- =====================================================
-- 可预约时段表
-- =====================================================
DROP TABLE IF EXISTS appointment_slot;

CREATE TABLE appointment_slot (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    inheritor_id        BIGINT NOT NULL COMMENT '传承人ID',
    slot_date           DATE NOT NULL COMMENT '日期',
    start_time          TIME NOT NULL COMMENT '开始时间',
    end_time            TIME NOT NULL COMMENT '结束时间',
    recurrence_type     VARCHAR(20) DEFAULT 'NONE' COMMENT '重复类型: NONE-不重复, WEEKLY-每周, MONTHLY-每月',
    recurrence_end_date DATE COMMENT '重复截止日期',
    max_people          INT DEFAULT 5 COMMENT '该时段最大可预约人数',
    booked_count        INT DEFAULT 0 COMMENT '已预约人数',
    status              INT DEFAULT 1 COMMENT '1-可用 0-不可用',
    create_time         DATETIME COMMENT '创建时间',
    update_time         DATETIME COMMENT '修改时间',
    INDEX idx_inheritor_date (inheritor_id, slot_date),
    INDEX idx_inheritor_date_time (inheritor_id, slot_date, start_time, end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='可预约时段表';

-- =====================================================
-- 会话表 — 存储已登录角色间的一对一或群组会话
-- =====================================================
DROP TABLE IF EXISTS chat_message;
DROP TABLE IF EXISTS chat_participant;
DROP TABLE IF EXISTS chat_conversation;

CREATE TABLE chat_conversation (
    id                BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '会话ID',
    conversation_type VARCHAR(20)  NOT NULL DEFAULT 'PRIVATE' COMMENT '会话类型: PRIVATE-一对一, GROUP-群组',
    title             VARCHAR(200) COMMENT '会话标题（群组时使用）',
    last_message      VARCHAR(500) COMMENT '最后一条消息预览',
    last_message_time DATETIME     COMMENT '最后消息时间',
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话表';

CREATE TABLE chat_participant (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    conversation_id BIGINT       NOT NULL COMMENT '会话ID',
    user_id         BIGINT       NOT NULL COMMENT '参与者ID',
    user_role       VARCHAR(20)  NOT NULL COMMENT '参与者角色: USER/INHERITOR/ADMIN',
    user_name       VARCHAR(50)  COMMENT '参与者姓名（冗余，方便展示）',
    joined_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    UNIQUE KEY uk_conv_user (conversation_id, user_id, user_role),
    INDEX idx_user (user_id, user_role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话参与者表';

CREATE TABLE chat_message (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '消息ID',
    conversation_id BIGINT       NOT NULL COMMENT '会话ID',
    sender_id       BIGINT       NOT NULL COMMENT '发送者ID',
    sender_role     VARCHAR(20)  NOT NULL COMMENT '发送者角色: USER/INHERITOR/ADMIN',
    sender_name     VARCHAR(50)  COMMENT '发送者姓名（冗余，方便展示）',
    content         TEXT         NOT NULL COMMENT '消息内容',
    message_type    VARCHAR(20)  NOT NULL DEFAULT 'TEXT' COMMENT '消息类型: TEXT/IMAGE',
    is_read         TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已读: 0-未读 1-已读',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    INDEX idx_conversation (conversation_id, created_at),
    INDEX idx_unread (conversation_id, is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天消息表';

-- =====================================================
-- 好友关系表 — 用户之间的好友申请与关系管理
-- =====================================================
DROP TABLE IF EXISTS friend_relation;

CREATE TABLE friend_relation (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    user_id      BIGINT       NOT NULL COMMENT '发起方用户ID',
    user_role    VARCHAR(20)  NOT NULL DEFAULT 'USER' COMMENT '发起方角色(USER/INHERITOR/ADMIN)',
    friend_id    BIGINT       NOT NULL COMMENT '接收方用户ID',
    friend_role  VARCHAR(20)  NOT NULL DEFAULT 'USER' COMMENT '接收方角色(USER/INHERITOR/ADMIN)',
    status       TINYINT      NOT NULL DEFAULT 0 COMMENT '状态: 0-待接受, 1-已接受, 2-已拒绝',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_friend (user_id, friend_id),
    INDEX idx_user (user_id),
    INDEX idx_friend (friend_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='好友关系表';

-- =====================================================
-- 管理员操作日志表
-- =====================================================
DROP TABLE IF EXISTS admin_operation_log;

CREATE TABLE admin_operation_log (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    operator_id     BIGINT       NOT NULL COMMENT '操作人ID',
    operator_name   VARCHAR(64)  NOT NULL COMMENT '操作人姓名',
    operator_role   VARCHAR(32)  NOT NULL COMMENT '操作人角色',
    operation_type  VARCHAR(64)  NOT NULL COMMENT '操作类型',
    target_type     VARCHAR(32)  DEFAULT NULL COMMENT '操作目标类型',
    target_id       BIGINT       DEFAULT NULL COMMENT '操作目标ID',
    detail          VARCHAR(500) DEFAULT NULL COMMENT '操作详情描述',
    create_time     DATETIME     DEFAULT NULL COMMENT '操作时间',
    INDEX idx_operator (operator_id),
    INDEX idx_type (operation_type),
    INDEX idx_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员操作日志表';

-- =====================================================
-- RAG AI 对话会话表 — 存储用户与AI助手的对话记录
-- =====================================================
DROP TABLE IF EXISTS rag_chat_message;
DROP TABLE IF EXISTS rag_chat_session;

CREATE TABLE rag_chat_session (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '会话ID',
    user_id        BIGINT       NOT NULL COMMENT '用户ID',
    user_role      VARCHAR(20)  NOT NULL COMMENT '用户角色: USER/INHERITOR/ADMIN',
    session_title  VARCHAR(200) COMMENT '会话标题',
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user (user_id, user_role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RAG AI对话会话表';

CREATE TABLE rag_chat_message (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '消息ID',
    session_id   BIGINT       NOT NULL COMMENT '会话ID',
    role         VARCHAR(20)  NOT NULL COMMENT '角色: USER/AI',
    content      TEXT         NOT NULL COMMENT '消息内容',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_session (session_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RAG AI对话消息表';
