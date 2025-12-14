package com.origin.aimodel.domain.vo;

import lombok.Data;
import java.util.List;

/**
 * 字典查询选项响应
 * 用于下拉选择框的数据源
 *
 * @author origin
 * @since 2025-12-14
 */
@Data
public class DictionaryOptionsResponse {
    /**
     * 模型名称列表
     */
    private List<String> modelNames;

    /**
     * Key 列表
     */
    private List<String> keys;

    /**
     * Post Param 列表
     */
    private List<String> postParams;
}

