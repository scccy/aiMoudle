package com.scccy.aimodel.domain.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 正向请求生成结果
 */
@Data
public class ForwardRequestResult {

    /**
     * 完整请求 URL（baseUrl + point）
     */
    private String url;

    /**
     * 请求头
     */
    private Map<String, Object> headers;

    /**
     * 请求体
     */
    private Map<String, Object> body;

    /**
     * 配置项列表（可用于前端展示原始 DSL 配置）
     */
    private List<MappingItem> headerItems;
    private List<MappingItem> paramItems;
}
