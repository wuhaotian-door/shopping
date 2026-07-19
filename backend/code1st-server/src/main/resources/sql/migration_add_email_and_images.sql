-- =====================================================
-- 增量迁移脚本：为 user 表添加 email 字段，为 product 表填充图片 URL
-- 适用于已运行 schema.sql 初始化过的数据库（MySQL 8.0）
-- 本脚本可安全重复执行（幂等）
-- =====================================================

USE code1st;

-- =====================================================
-- 1. 为 user 表添加 email 字段（如果尚不存在）
--    MySQL 8.0 的 ALTER TABLE 不支持 IF NOT EXISTS，
--    使用存储过程检查 INFORMATION_SCHEMA 实现幂等
-- =====================================================
DELIMITER $$

CREATE PROCEDURE IF NOT EXISTS add_email_column_if_needed()
BEGIN
    DECLARE col_count INT DEFAULT 0;

    SELECT COUNT(*) INTO col_count
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'code1st'
      AND TABLE_NAME   = 'user'
      AND COLUMN_NAME  = 'email';

    IF col_count = 0 THEN
        ALTER TABLE user
            ADD COLUMN email VARCHAR(128) DEFAULT NULL COMMENT '邮箱（用于找回密码）'
            AFTER phone;
    END IF;
END$$

DELIMITER ;

CALL add_email_column_if_needed();
DROP PROCEDURE IF EXISTS add_email_column_if_needed;

-- =====================================================
-- 2. 将所有现有用户的 email 统一设置为 3326353413@qq.com
-- =====================================================
UPDATE user
SET email = '3326353413@qq.com'
WHERE email IS NULL OR email = '';

-- =====================================================
-- 3. 更新 product 表所有商品的图片 URL
-- =====================================================
UPDATE product SET image_url = 'https://picsum.photos/seed/jianzhi-bookmark/400/300'    WHERE name = '非遗剪纸书签套装';
UPDATE product SET image_url = 'https://picsum.photos/seed/suxiu-handkerchief/400/300'  WHERE name = '苏绣手帕礼盒';
UPDATE product SET image_url = 'https://picsum.photos/seed/feiyi-postcard/400/300'      WHERE name = '校园非遗明信片集';
UPDATE product SET image_url = 'https://picsum.photos/seed/cixiu-mirror/400/300'        WHERE name = '刺绣随身镜';
UPDATE product SET image_url = 'https://picsum.photos/seed/jianzhi-window/400/300'      WHERE name = '剪纸窗花年画套装';
UPDATE product SET image_url = 'https://picsum.photos/seed/zisha-teapot/400/300'        WHERE name = '紫砂小壶·禅意';
UPDATE product SET image_url = 'https://picsum.photos/seed/zisha-cup/400/300'           WHERE name = '紫砂主人杯·圆润';
UPDATE product SET image_url = 'https://picsum.photos/seed/zisha-teapet/400/300'        WHERE name = '紫砂茶宠·金蟾';
UPDATE product SET image_url = 'https://picsum.photos/seed/zhubian-box/400/300'         WHERE name = '竹编收纳盒三件套';
UPDATE product SET image_url = 'https://picsum.photos/seed/zhubian-tray/400/300'        WHERE name = '竹编茶具托盘';
UPDATE product SET image_url = 'https://picsum.photos/seed/zhubian-basket/400/300'      WHERE name = '竹编果篮·手提';
UPDATE product SET image_url = 'https://picsum.photos/seed/shuxiu-fan/400/300'          WHERE name = '蜀绣团扇·芙蓉锦鲤';
UPDATE product SET image_url = 'https://picsum.photos/seed/shuxiu-pin/400/300'          WHERE name = '蜀绣胸针·牡丹';
UPDATE product SET image_url = 'https://picsum.photos/seed/qinghua-cup/400/300'         WHERE name = '青花瓷茶杯套装';
UPDATE product SET image_url = 'https://picsum.photos/seed/fencai-dish/400/300'         WHERE name = '粉彩小碟·四季';
UPDATE product SET image_url = 'https://picsum.photos/seed/taoci-bell/400/300'          WHERE name = '陶瓷风铃·莲花';
UPDATE product SET image_url = 'https://picsum.photos/seed/bianxiu-scroll/400/300'      WHERE name = '汴绣卷轴·清明上河图局部';
UPDATE product SET image_url = 'https://picsum.photos/seed/bianxiu-bookmark/400/300'    WHERE name = '汴绣书签·宋韵';
UPDATE product SET image_url = 'https://picsum.photos/seed/mudiao-deer/400/300'         WHERE name = '木雕摆件·福鹿';
UPDATE product SET image_url = 'https://picsum.photos/seed/mudiao-pendant/400/300'      WHERE name = '木雕挂饰·平安扣';
UPDATE product SET image_url = 'https://picsum.photos/seed/zharan-scarf/400/300'        WHERE name = '扎染围巾·苍山洱海';
UPDATE product SET image_url = 'https://picsum.photos/seed/zharan-tablecloth/400/300'   WHERE name = '扎染桌布·靛蓝';
UPDATE product SET image_url = 'https://picsum.photos/seed/zharan-bag/400/300'          WHERE name = '扎染帆布包·大理蓝';
UPDATE product SET image_url = 'https://picsum.photos/seed/suxiu-fan-limited/400/300'   WHERE name = '限量版苏绣团扇·百鸟朝凤';

-- =====================================================
-- 4. 更新 product_order 表中订单的商品图片（冗余字段）
-- =====================================================
UPDATE product_order SET product_image = 'https://picsum.photos/seed/jianzhi-bookmark/400/300'    WHERE product_name = '非遗剪纸书签套装';
UPDATE product_order SET product_image = 'https://picsum.photos/seed/suxiu-handkerchief/400/300'  WHERE product_name = '苏绣手帕礼盒';
UPDATE product_order SET product_image = 'https://picsum.photos/seed/feiyi-postcard/400/300'      WHERE product_name = '校园非遗明信片集';
UPDATE product_order SET product_image = 'https://picsum.photos/seed/zisha-teapot/400/300'        WHERE product_name = '紫砂小壶·禅意';
UPDATE product_order SET product_image = 'https://picsum.photos/seed/zisha-cup/400/300'           WHERE product_name = '紫砂主人杯·圆润';
UPDATE product_order SET product_image = 'https://picsum.photos/seed/zhubian-box/400/300'         WHERE product_name = '竹编收纳盒三件套';
UPDATE product_order SET product_image = 'https://picsum.photos/seed/zhubian-tray/400/300'        WHERE product_name = '竹编茶具托盘';
UPDATE product_order SET product_image = 'https://picsum.photos/seed/shuxiu-fan/400/300'          WHERE product_name = '蜀绣团扇·芙蓉锦鲤';
UPDATE product_order SET product_image = 'https://picsum.photos/seed/shuxiu-pin/400/300'          WHERE product_name = '蜀绣胸针·牡丹';
UPDATE product_order SET product_image = 'https://picsum.photos/seed/qinghua-cup/400/300'         WHERE product_name = '青花瓷茶杯套装';
UPDATE product_order SET product_image = 'https://picsum.photos/seed/taoci-bell/400/300'          WHERE product_name = '陶瓷风铃·莲花';
UPDATE product_order SET product_image = 'https://picsum.photos/seed/bianxiu-bookmark/400/300'    WHERE product_name = '汴绣书签·宋韵';
UPDATE product_order SET product_image = 'https://picsum.photos/seed/mudiao-deer/400/300'         WHERE product_name = '木雕摆件·福鹿';
UPDATE product_order SET product_image = 'https://picsum.photos/seed/zharan-scarf/400/300'        WHERE product_name = '扎染围巾·苍山洱海';
UPDATE product_order SET product_image = 'https://picsum.photos/seed/zharan-tablecloth/400/300'   WHERE product_name = '扎染桌布·靛蓝';
