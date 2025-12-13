-- 为 dim_ai_model_item 表添加 validate 字段
-- 用于存储校验规则（JSON 字符串格式）
-- 示例：{"maxLength": 2000} 或 {"range": [0.0, 2.0]} 或 {"enum": ["text", "image_url"]}

ALTER TABLE dim_ai_model_item 
ADD COLUMN validate TEXT COMMENT '校验规则，JSON 字符串格式，如：{"maxLength": 2000} 或 {"range": [0.0, 2.0]} 或 {"enum": ["text", "image_url"]}';

