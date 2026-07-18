-- =====================================================
-- 传承人详细信息 & 年龄 种子数据
-- 可直接在现有数据库上执行，无需重建表
-- =====================================================

USE code1st;

-- 2. 更新现有传承人的年龄
UPDATE inheritor SET age = 51 WHERE username = 'inheritor_li';
UPDATE inheritor SET age = 58 WHERE username = 'inheritor_zhang';
UPDATE inheritor SET age = 54 WHERE username = 'inheritor_wang';
UPDATE inheritor SET age = 44 WHERE username = 'inheritor_chen';
UPDATE inheritor SET age = 48 WHERE username = 'inheritor_liu';
UPDATE inheritor SET age = 41 WHERE username = 'inheritor_zhao';
UPDATE inheritor SET age = 61 WHERE username = 'inheritor_sun';
UPDATE inheritor SET age = 38 WHERE username = 'inheritor_zhou';
UPDATE inheritor SET age = 36 WHERE username = 'inheritor_yang';
UPDATE inheritor SET age = 31 WHERE username = 'inheritor_pending';

-- 3. 清空并重新填充传承人详细信息
DELETE FROM inheritor_profile;

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
