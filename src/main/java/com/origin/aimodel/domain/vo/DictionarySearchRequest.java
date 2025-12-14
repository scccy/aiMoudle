package com.origin.aimodel.domain.vo;

import lombok.Data;

/**
 * 字典映射查询请求 VO
 *
 * @author origin
 * @since 2025-12-14
 */
@Data
public class DictionarySearchRequest {

    /**
     * 模型名称（可选，支持模糊查询）
     */
    private String modelName;

    /**
     * 业务入参的 key 名称（可选，支持模糊查询）
     */
    private String key;

    /**
     * 写入的目标字段名（可选，支持模糊查询）
     */
    private String postParam;
}

