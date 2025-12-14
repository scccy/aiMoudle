package com.origin.aimodel.domain.vo;

import lombok.Data;

/**
 * 创建字典映射请求 VO
 *
 * @author origin
 * @since 2025-12-14
 */
@Data
public class DictionaryCreateRequest {

    /**
     * 业务入参的 key 名称（必填）
     */
    private String key;

    /**
     * 写入的目标字段名（必填）
     */
    private String postParam;

    /**
     * 中文说明（可选）
     */
    private String description;

    /**
     * 关联的 item ID（可选）
     */
    private Long itemId;
}

