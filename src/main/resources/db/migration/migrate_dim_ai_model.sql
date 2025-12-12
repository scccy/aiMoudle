-- 数据库迁移脚本：将 dim_ai_model 表迁移到新结构
-- 废弃字段：in_parameter, out_parameter, template_spel, template_header, template_param_json_plus
-- 新增字段：header_item, param_item

-- 1. 备份原表
RENAME TABLE dim_ai_model TO dim_ai_model_bck;

-- 2. 创建新表结构
CREATE TABLE dim_ai_model (
    model_name VARCHAR(255) NOT NULL COMMENT '模型名称',
    origin_name VARCHAR(255) COMMENT '原始名称',
    base_url VARCHAR(500) COMMENT '基础URL',
    point VARCHAR(500) COMMENT '接口路径',
    authorization VARCHAR(500) COMMENT '授权信息',
    template_attribute_mapping TEXT COMMENT '模板属性映射',
    alias_mapping_json_plus TEXT COMMENT '别名映射JSON',
    header_item TEXT COMMENT '请求头配置列表（JSON格式）',
    param_item TEXT COMMENT '请求体参数配置列表（JSON格式）',
    created_by VARCHAR(100) COMMENT '创建人',
    created_time DATETIME COMMENT '创建时间',
    updated_by VARCHAR(100) COMMENT '更新人',
    updated_time DATETIME COMMENT '更新时间',
    del_flag TINYINT DEFAULT 0 COMMENT '删除标志：0-未删除，1-已删除',
    PRIMARY KEY (model_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI模型配置表';

-- 3. 如果需要迁移旧数据，可以执行以下SQL（根据实际业务需求调整）
-- INSERT INTO dim_ai_model (
--     model_name, origin_name, base_url, point, authorization,
--     template_attribute_mapping, alias_mapping_json_plus,
--     header_item, param_item,
--     created_by, created_time, updated_by, updated_time, del_flag
-- )
-- SELECT 
--     model_name, origin_name, base_url, point, authorization,
--     template_attribute_mapping, alias_mapping_json_plus,
--     NULL as header_item, NULL as param_item,  -- 新字段需要手动配置或通过其他方式迁移
--     created_by, created_time, updated_by, updated_time, del_flag
-- FROM dim_ai_model_bck;
