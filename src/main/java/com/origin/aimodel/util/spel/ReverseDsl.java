package com.origin.aimodel.util.spel;

import com.alibaba.fastjson2.JSONObject;

import java.util.Map;

/**
 * 反向解析 DSL：从实际请求的 header/body + 映射关系生成 headerItem 和 paramItem 配置。
 * 提供建造者模式接口，便于链式调用。
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
        return new Builder(requestBody, mappingConfig);
    }

    /**
     * 反向解析构建器
     */
    public static final class Builder {
        private final JSONObject requestBody;
        private final JSONObject mappingConfig;
        private Map<String, String> requestHeaders;

        private Builder(JSONObject requestBody, JSONObject mappingConfig) {
            this.requestBody = requestBody;
            this.mappingConfig = mappingConfig;
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
         * 生成 paramItem 配置对象
         * 
         * @return paramItem 配置 JSONObject
         */
        public JSONObject getParam() {
            return ReverseParser.getParamItemObject(requestBody, mappingConfig);
        }

        /**
         * 生成 paramItem 配置（JSON 字符串）
         * 
         * @return paramItem 配置 JSON 字符串
         */
        public String getParamString() {
            return ReverseParser.getParamItem(requestBody, mappingConfig);
        }

        /**
         * 生成 headerItem 配置对象
         * 
         * @return headerItem 配置 JSONObject
         */
        public JSONObject getHeader() {
            if (requestHeaders == null) {
                throw new IllegalStateException("Request headers not set. Call withHeaders() first.");
            }
            return ReverseParser.getHeaderItemObject(requestHeaders, mappingConfig);
        }

        /**
         * 生成 headerItem 配置（JSON 字符串）
         * 
         * @return headerItem 配置 JSON 字符串
         */
        public String getHeaderString() {
            if (requestHeaders == null) {
                throw new IllegalStateException("Request headers not set. Call withHeaders() first.");
            }
            return ReverseParser.getHeaderItem(requestHeaders, mappingConfig);
        }

        public JSONObject requestBody() {
            return requestBody;
        }

        public JSONObject mappingConfig() {
            return mappingConfig;
        }

        public Map<String, String> requestHeaders() {
            return requestHeaders;
        }
    }
}
