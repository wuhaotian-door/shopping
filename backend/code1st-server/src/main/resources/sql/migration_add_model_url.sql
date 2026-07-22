-- =====================================================
-- 迁移：为 product 表添加 model_url 列，支持3D模型预览
-- =====================================================
ALTER TABLE product ADD COLUMN model_url VARCHAR(500) DEFAULT NULL COMMENT '3D模型URL（glb/gltf格式）' AFTER image_url;