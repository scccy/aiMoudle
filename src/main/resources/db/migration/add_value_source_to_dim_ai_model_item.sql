-- 为 dim_ai_model_item 表新增 value_source 字段
-- 用途：标记值来源（CONST | USER | DERIVED）

ALTER TABLE dim_ai_model_item
    ADD COLUMN value_source VARCHAR(50) COMMENT '值来源：CONST | USER | DERIVED' AFTER value_object;
