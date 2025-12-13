package com.origin.aimodel.util.spel;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 配置驱动的参数映射 DSL 工具，用于从 headerItem/paramItem 配置生成示例结构。
 * 支持字符串模板替换（{value}），无实际 SpEL 解析能力。
 * 
 * 重构为门面模式，协调 ConfigParser、PayloadEngine 和 ConfigValidator 组件。
 * 反向解析功能请使用 ReverseDsl。
 */
public final class PostDsl {

    private PostDsl() {
    }

    /**
     * 创建 POST 请求构建器
     * 
     * @param config 配置对象
     * @param payload 业务入参
     * @return POST 请求构建器实例
     */
    public static Builder build(PostDslConfig config, Map<String, Object> payload) {
        return new Builder(config, payload);
    }

    /** 解析 headerItem/paramItem 配置字符串，提取字段属性。 */
    public static List<MappingItem> parseConfig(String config) {
        return ConfigParser.parseConfig(config);
    }

    /** 根据配置与输入 payload 构造结果结构。 */
    public static Map<String, Object> buildResult(List<MappingItem> entries, Map<String, Object> payload) {
        return PayloadEngine.buildResult(entries, payload);
    }

    /** 使用配置对象 + 外部 payload 生成 param 预览，自动合并 baseInfo。 */
    public static String getParamPreview(PostDslConfig config, Map<String, Object> payload) {
        return PayloadEngine.getParamPreview(config, payload);
    }

    /** 使用新的引擎生成 param 预览 */
    public static String getParamPreviewNew(PostDslConfig config, Map<String, Object> payload) {
        return PayloadEngine.getParamPreview(config, payload);
    }

    /** 使用配置对象 + 外部 payload 生成 header 预览，自动合并 baseInfo。 */
    public static String getHeaderPreview(PostDslConfig config, Map<String, Object> payload) {
        return PayloadEngine.getHeaderPreview(config, payload);
    }

    /** 使用新的引擎生成 header 预览 */
    public static String getHeaderPreviewNew(PostDslConfig config, Map<String, Object> payload) {
        return PayloadEngine.getHeaderPreview(config, payload);
    }

    /** 校验 payload 与配置的匹配度（长度/数值范围/枚举）。 */
    public static ConfigValidator.ValidationResult validateParam(PostDslConfig config, Map<String, Object> payload) {
        return ConfigValidator.validateParam(config, payload);
    }

    /** 使用配置对象 + 外部 payload 生成 URL 预览（base_url + point）。 */
    public static String getUrlPreview(PostDslConfig config, Map<String, Object> payload) {
        return PayloadEngine.getUrlPreview(config, payload);
    }

    /** 使用新的引擎生成 URL 预览 */
    public static String getUrlPreviewNew(PostDslConfig config, Map<String, Object> payload) {
        return PayloadEngine.getUrlPreview(config, payload);
    }

    /** 构造 header 预览字符串，支持传入 payload 覆盖默认值。 */
    public static String buildHeaderPreview(List<MappingItem> entries, Map<String, Object> payload) {
        return PayloadEngine.buildHeaderPreview(entries, payload);
    }

    /** Pretty JSON 输出，方便示例查看。 */
    public static String toPrettyJson(Object obj, int indent) {
        return PayloadEngine.toPrettyJson(obj, indent);
    }

    /** 根据 payload 生成请求 URL，默认拼接 base_url + point。 */
    public static String getUrl(Map<String, Object> payload) {
        return PayloadEngine.getUrl(payload);
    }

    /** 解析 baseInfo JSON 字符串为 Map，兼容大小写 key。 */
    public static Map<String, Object> parseBaseInfo(String baseInfo) {
        return ConfigParser.parseBaseInfo(baseInfo);
    }

    /**
     * POST 请求构建器
     */
    public static final class Builder {
        private final PostDslConfig config;
        private final Map<String, Object> payload;

        private Builder(PostDslConfig config, Map<String, Object> payload) {
            this.config = config;
            this.payload = payload != null ? new LinkedHashMap<>(payload) : new LinkedHashMap<>();
        }

        /**
         * 生成 header 预览
         * 
         * @return header 预览字符串
         */
        public String getHeader() {
            return PostDsl.getHeaderPreviewNew(config, payload);
        }

        /**
         * 生成 param 预览
         * 
         * @return param 预览字符串
         */
        public String getParam() {
            return PostDsl.getParamPreviewNew(config, payload);
        }

        /**
         * 生成 URL 预览
         * 
         * @return URL 预览字符串
         */
        public String getUrl() {
            return PostDsl.getUrlPreviewNew(config, payload);
        }

        /**
         * 校验 payload
         * 
         * @return 校验结果
         */
        public ConfigValidator.ValidationResult validate() {
            return PostDsl.validateParam(config, payload);
        }

        public Map<String, Object> payload() {
            return payload;
        }

        public PostDslConfig config() {
            return config;
        }
    }
}

