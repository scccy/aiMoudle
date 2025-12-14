package com.origin.aimodel.domain.vo;

import lombok.Data;

/**
 * 字典映射 VO
 * 表达同一个 key 在不同 model 下映射到不同的 postParam
 *
 * @author origin
 * @since 2025-12-14
 */
@Data
public class DictionaryMappingVO {

    /**
     * 模型名称
     */
    private String modelName;

    /**
     * 业务入参的 key 名称
     */
    private String key;

    /**
     * 写入的目标字段名
     */
    private String postParam;

    /**
     * 中文说明（可选，用于显示额外信息）
     */
    private String description;

    /**
     * Item ID，用于查看详情
     */
    private Long itemId;
}

