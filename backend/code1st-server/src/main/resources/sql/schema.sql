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
INSERT INTO inheritor (name, username, password, phone, sex, id_number, region, status)
VALUES
    ('李秀英', 'inheritor_li',    'e10adc3949ba59abbe56e057f20f883e', '13920001111', '女', '320501197503120021', '江苏省苏州市',     1),
    ('张德明', 'inheritor_zhang', 'e10adc3949ba59abbe56e057f20f883e', '13920002222', '男', '320282196808150013', '江苏省宜兴市',     1),
    ('王师傅', 'inheritor_wang',  'e10adc3949ba59abbe56e057f20f883e', '13920003333', '男', '330523197211080034', '浙江省安吉县',     1),
    ('陈巧云', 'inheritor_chen',  'e10adc3949ba59abbe56e057f20f883e', '13920004444', '女', '510105198204220025', '四川省成都市',     1),
    ('刘建国', 'inheritor_liu',   'e10adc3949ba59abbe56e057f20f883e', '13920005555', '男', '360202197808160016', '江西省景德镇市',   1),
    ('赵玉兰', 'inheritor_zhao',  'e10adc3949ba59abbe56e057f20f883e', '13920006666', '女', '410204198511200027', '河南省开封市',     1),
    ('孙大师', 'inheritor_sun',   'e10adc3949ba59abbe56e057f20f883e', '13920007777', '男', '350505196503300018', '福建省泉州市',     1),
    ('周小燕', 'inheritor_zhou',  'e10adc3949ba59abbe56e057f20f883e', '13920008888', '女', '530102198811050029', '云南省大理市',     1),
    ('杨志强', 'inheritor_yang',  'e10adc3949ba59abbe56e057f20f883e', '13920009999', '男', '610102199003220010', '陕西省西安市',     1),
    ('待审核传承人', 'inheritor_pending', 'e10adc3949ba59abbe56e057f20f883e', '13920000001', '男', '440101199501010011', '广东省广州市', 2);
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
    friend_id    BIGINT       NOT NULL COMMENT '接收方用户ID',
    status       TINYINT      NOT NULL DEFAULT 0 COMMENT '状态: 0-待接受, 1-已接受, 2-已拒绝',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_friend (user_id, friend_id),
    INDEX idx_user (user_id),
    INDEX idx_friend (friend_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='好友关系表';

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

