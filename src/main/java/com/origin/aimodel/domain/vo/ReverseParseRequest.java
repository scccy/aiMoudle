package com.origin.aimodel.domain.vo;

import com.origin.aimodel.util.spel.MappingItem;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 反向解析请求 VO
 * 用于接收前端提交的实际请求数据，生成 paramItem 和 headerItem 配置
 *
 * @author origin
 * @since 2025-12-13
 */
@Data
public class ReverseParseRequest {

    /**
     * 模型昵称（对应 dim_ai_model.model_name）
     */
    private String modelName;

    /**
     * 原始模型名称（对应 dim_ai_model.origin_name）
     */
    private String originName;

    /**
     * 请求地址（对应 dim_ai_model.base_url）
     */
    private String baseUrl;

    /**
     * 端点（对应 dim_ai_model.point）
     */
    private String point;

    /**
     * 授权信息（对应 dim_ai_model.authorization）
     */
    private String authorization;

    /**
     * 实际请求的 JSON body
     */
    private Map<String, Object> requestBody;

    /**
     * 实际请求的 headers
     */
    private Map<String, String> requestHeaders;

    /**
     * paramItem 映射关系配置
     * 格式：[{"key": "model", "post_param": "model"}, ...]
     */
    private List<MappingConfig> paramMapping;

    /**
     * headerItem 映射关系配置
     * 格式：[{"key": "contentTypeHeader", "post_param": "Content-Type"}, ...]
     */
    private List<MappingConfig> headerMapping;

    /**
     * 编辑后的 paramItem 配置项（用于保存）
     * 格式：[{"key": "prompt", "category": "key", "node": "content[0].text", ...}, ...]
     */
    private List<MappingItem> paramItems;

    /**
     * 编辑后的 headerItem 配置项（用于保存）
     * 格式：[{"key": "contentTypeHeader", "category": "key", "node": "Content-Type", ...}, ...]
     */
    private List<MappingItem> headerItems;

    /**
     * 映射配置项
     */
    @Data
    public static class MappingConfig {
        /**
         * 业务字段名（key）
         */
        private String key;

        /**
         * node 路径（可选，如果不提供则自动发现）
         */
        private String node;

        /**
         * 目标字段名（post_param）
         */
        private String postParam;

        /**
         * 校验规则（可选，JSON 字符串格式）
         * 示例：{"maxLength": 2000} 或 {"range": [0.0, 2.0]} 或 {"enum": ["text", "image_url"]}
         */
        private String validate;
    }

}
