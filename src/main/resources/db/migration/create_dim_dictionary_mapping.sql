-- 数据库迁移脚本：创建字典映射表
-- 表名：dim_dictionary_mapping
-- 用途：存储 key 与 post_param 的对应关系，作为字典映射表

CREATE TABLE dim_dictionary_mapping (
    `key` VARCHAR(255) NOT NULL COMMENT '业务入参的 key 名称',
    post_param VARCHAR(255) NOT NULL COMMENT '写入的目标字段名',
    description VARCHAR(500) COMMENT '中文说明',
    item_id BIGINT COMMENT '关联的 item ID，关联 dim_ai_model_item.id',
    created_by VARCHAR(100) COMMENT '创建人',
    created_time DATETIME COMMENT '创建时间',
    updated_by VARCHAR(100) COMMENT '更新人',
    updated_time DATETIME COMMENT '更新时间',
    del_flag TINYINT DEFAULT 0 COMMENT '删除标志：0-未删除，1-已删除',
    PRIMARY KEY (`key`, post_param),
    INDEX idx_item_id (item_id),
    INDEX idx_key (`key`),
    INDEX idx_post_param (post_param)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典映射表';

