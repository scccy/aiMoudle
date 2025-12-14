package com.scccy.aimodel.util.dsl;

import com.alibaba.fastjson2.JSONObject;
import com.scccy.aimodel.domain.vo.MappingItem;
import com.scccy.aimodel.domain.vo.ReverseParseRequest;

import java.util.List;
import java.util.Map;

/**
     * 反向解析 DSL：第三层（API层）
     * 提供建造者模式接口，便于链式调用。
     * 全部基于 ReverseParser 实现。
     */
public final class ReverseDsl {

    private ReverseDsl() {
    }

    /**
     * 创建反向解析构建器
     * 
     * @param requestBody 实际请求的 JSON body
     * @param mappingConfig 映射关系配置
     * @return 反向解析构建器实例
     */
    public static Builder build(JSONObject requestBody, JSONObject mappingConfig) {
        return new Builder(requestBody, mappingConfig, mappingConfig, null);
    }

    /**
     * 创建反向解析构建器
     *
     * @param request 含 body/header 及映射配置的请求对象
     * @return 反向解析构建器实例
     */
    public static Builder build(ReverseParseRequest request) {
        JSONObject requestBody = new JSONObject(request.getRequestBody());
        JSONObject paramMappingConfig = ReverseDslFactory.buildParamMappingConfig(request.getParamMapping());
        JSONObject headerMappingConfig = ReverseDslFactory.buildHeaderMappingConfig(request.getHeaderMapping());
        Map<String, String> headers = request.getRequestHeaders();
        return new Builder(requestBody, paramMappingConfig, headerMappingConfig, headers);
    }

    /**
     * 反向解析构建器
     */
    public static final class Builder {
        private final JSONObject requestBody;
        private final JSONObject paramMappingConfig;
        private final JSONObject headerMappingConfig;
        private Map<String, String> requestHeaders;

        private Builder(JSONObject requestBody,
                        JSONObject paramMappingConfig,
                        JSONObject headerMappingConfig,
                        Map<String, String> requestHeaders) {
            this.requestBody = requestBody;
            this.paramMappingConfig = paramMappingConfig;
            this.headerMappingConfig = headerMappingConfig;
            this.requestHeaders = requestHeaders;
        }

        /**
         * 设置请求头（可选）
         * 
         * @param requestHeaders 请求头 Map
         * @return 构建器实例，支持链式调用
         */
        public Builder withHeaders(Map<String, String> requestHeaders) {
            this.requestHeaders = requestHeaders;
            return this;
        }

        /**
         * 生成 paramItem 配置列表
         * 
         * @return paramItem 配置列表
         */
        public List<MappingItem> getParamItems() {
            Map<String, Object> bodyMap = requestBody.toJavaObject(Map.class);
            return ReverseParser.parseParamItems(bodyMap, paramMappingConfig);
        }

        /**
         * 生成 headerItem 配置列表
         * 
         * @return headerItem 配置列表
         */
        public List<MappingItem> getHeaderItems() {
            if (requestHeaders == null) {
                throw new IllegalStateException("Request headers not set. Call withHeaders() first.");
            }
            return ReverseParser.parseHeaderItems(requestHeaders, headerMappingConfig);
        }

    }
}
