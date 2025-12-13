-- 数据库迁移脚本：重新设计 dim_ai_model 表结构
-- 将 headerItem 和 paramItem 拆分到独立的明细表 dim_ai_model_item
-- 废弃字段：in_parameter, out_parameter, template_spel, template_header, template_param_json_plus, header_item, param_item


-- 1. 创建新的主表结构（移除 header_item 和 param_item）
CREATE TABLE dim_ai_model (
    model_name VARCHAR(255) NOT NULL COMMENT '模型名称',
    origin_name VARCHAR(255) COMMENT '原始名称',
    base_url VARCHAR(500) COMMENT '基础URL',
    point VARCHAR(500) COMMENT '接口路径',
    authorization VARCHAR(500) COMMENT '授权信息',
    created_by VARCHAR(100) COMMENT '创建人',
    created_time DATETIME COMMENT '创建时间',
    updated_by VARCHAR(100) COMMENT '更新人',
    updated_time DATETIME COMMENT '更新时间',
    del_flag TINYINT DEFAULT 0 COMMENT '删除标志：0-未删除，1-已删除',
    PRIMARY KEY (model_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI模型配置表';

-- 2. 创建明细表，存储 headerItem 和 paramItem 配置项
CREATE TABLE dim_ai_model_item (
    id BIGINT AUTO_INCREMENT NOT NULL COMMENT '主键ID',
    model_name VARCHAR(255) NOT NULL COMMENT '模型名称，关联dim_ai_model.model_name',
    item_type VARCHAR(20) NOT NULL COMMENT '配置项类型：header-请求头配置，param-请求体参数配置',
    key VARCHAR(255) NOT NULL COMMENT '业务入参的key名称',
    category VARCHAR(50) NOT NULL COMMENT '处理类型：key、map、list',
    node VARCHAR(500) NOT NULL COMMENT '目标路径，支持点号和数组索引，如content[0].text',
    post_param VARCHAR(255) COMMENT '写入的目标字段名，为空则取node尾段',
    spel_temp TEXT COMMENT '字符串模板，{value}会被替换为实际值',
    default_value VARCHAR(500) COMMENT '缺省值，当入参不存在时使用',
    value_object VARCHAR(50) COMMENT '类型提示：string、int、double、list<string>、list<json>、map',
    sort_order INT DEFAULT 0 COMMENT '排序顺序，用于保持配置项的顺序',
    created_by VARCHAR(100) COMMENT '创建人',
    created_time DATETIME COMMENT '创建时间',
    updated_by VARCHAR(100) COMMENT '更新人',
    updated_time DATETIME COMMENT '更新时间',
    del_flag TINYINT DEFAULT 0 COMMENT '删除标志：0-未删除，1-已删除',
    PRIMARY KEY (id),
    INDEX idx_model_name (model_name),
    INDEX idx_model_type (model_name, item_type),
    INDEX idx_sort (model_name, item_type, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI模型配置项明细表';

-- 3. 如果需要迁移旧数据，可以执行以下SQL（根据实际业务需求调整）
-- 注意：需要将旧表中的 header_item 和 param_item JSON 数据解析后插入到 dim_ai_model_item 表
-- INSERT INTO dim_ai_model (
--     model_name, origin_name, base_url, point, authorization,
--     template_attribute_mapping, alias_mapping_json_plus,
--     created_by, created_time, updated_by, updated_time, del_flag
-- )
-- SELECT 
--     model_name, origin_name, base_url, point, authorization,
--     template_attribute_mapping, alias_mapping_json_plus,
--     created_by, created_time, updated_by, updated_time, del_flag
-- FROM dim_ai_model_bck;
