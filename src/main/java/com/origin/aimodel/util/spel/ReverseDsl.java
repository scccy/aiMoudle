package com.origin.aimodel.util.spel;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

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
         * 生成 paramItem 配置列表
         * 
         * @return paramItem 配置列表
         */
        public List<MappingItem> getParamItems() {
            Map<String, Object> bodyMap = requestBody.toJavaObject(Map.class);
            return ReverseParser.parseParamItems(bodyMap, mappingConfig);
        }

        /**
         * 生成 paramItem 配置对象（兼容旧接口）
         * 
         * @return paramItem 配置 JSONObject
         */
        public JSONObject getParam() {
            List<MappingItem> items = getParamItems();
            JSONObject result = new JSONObject();
            JSONArray paramItems = new JSONArray();
            for (MappingItem item : items) {
                JSONObject obj = new JSONObject();
                if (item.key != null) obj.put("key", item.key);
                if (item.category != null) obj.put("category", item.category);
                if (item.node != null) obj.put("node", item.node);
                if (item.postParam != null) obj.put("post_param", item.postParam);
                if (item.valueObject != null) obj.put("value_object", item.valueObject);
                if (item.spelTemp != null) obj.put("spel_temp", item.spelTemp);
                if (item.defaultValue != null) obj.put("default_value", item.defaultValue);
                if (item.validate != null) obj.put("validate", item.validate);
                if (item.valueSource != null) obj.put("value_source", item.valueSource);
                paramItems.add(obj);
            }
            result.put("paramItem", paramItems);
            return result;
        }

        /**
         * 生成 paramItem 配置（JSON 字符串，兼容旧接口）
         * 
         * @return paramItem 配置 JSON 字符串
         */
        public String getParamString() {
            return getParam().toJSONString();
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
            return ReverseParser.parseHeaderItems(requestHeaders, mappingConfig);
        }

        /**
         * 生成 headerItem 配置对象（兼容旧接口）
         * 
         * @return headerItem 配置 JSONObject
         */
        public JSONObject getHeader() {
            List<MappingItem> items = getHeaderItems();
            JSONObject result = new JSONObject();
            JSONArray headerItems = new JSONArray();
            for (MappingItem item : items) {
                JSONObject obj = new JSONObject();
                if (item.key != null) obj.put("key", item.key);
                if (item.category != null) obj.put("category", item.category);
                if (item.node != null) obj.put("node", item.node);
                if (item.postParam != null) obj.put("post_param", item.postParam);
                if (item.valueObject != null) obj.put("value_object", item.valueObject);
                if (item.spelTemp != null) obj.put("spel_temp", item.spelTemp);
                if (item.defaultValue != null) obj.put("default_value", item.defaultValue);
                if (item.validate != null) obj.put("validate", item.validate);
                if (item.valueSource != null) obj.put("value_source", item.valueSource);
                headerItems.add(obj);
            }
            result.put("headerItem", headerItems);
            return result;
        }

        /**
         * 生成 headerItem 配置（JSON 字符串，兼容旧接口）
         * 
         * @return headerItem 配置 JSON 字符串
         */
        public String getHeaderString() {
            return getHeader().toJSONString();
        }
    }
}
