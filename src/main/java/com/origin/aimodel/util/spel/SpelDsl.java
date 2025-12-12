package com.origin.aimodel.util.spel;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 轻量级的 SpEL 配置解析/预览工具，用于从 headerItem/paramItem 配置生成示例结构。
 * 无实际 SpEL 解析，仅按 {value} 模板拼接。
 * 
 * 重构为门面模式，协调 SpelConfigParser、SpelPayloadEngineNew 和 SpelValidator 组件。
 */
public final class SpelDsl {

    private SpelDsl() {
    }

    /** 创建基于配置与 payload 的运行器，便于实例化调用。 */
    public static Runner Runner(SpelDslConfig config, Map<String, Object> payload) {
        return new Runner(config, payload);
    }

    /** 解析 headerItem/paramItem 配置字符串，提取字段属性。 */
    public static List<SpelConfigParser.ParamConfigEntry> parseConfig(String config) {
        return SpelConfigParser.parseConfig(config);
    }

    /** 根据配置与输入 payload 构造结果结构。 */
    public static Map<String, Object> buildResult(List<SpelConfigParser.ParamConfigEntry> entries, Map<String, Object> payload) {
        return SpelPayloadEngine.buildResult(entries, payload);
    }

    /** 使用配置对象 + 外部 payload 生成 param 预览，自动合并 baseInfo。 */
    public static String getParamPreview(SpelDslConfig config, Map<String, Object> payload) {
        return SpelPayloadEngine.getParamPreview(config, payload);
    }

    /** 使用新的引擎生成 param 预览 */
    public static String getParamPreviewNew(SpelDslConfig config, Map<String, Object> payload) {
        return SpelPayloadEngine.getParamPreview(config, payload);
    }

    /** 使用配置对象 + 外部 payload 生成 header 预览，自动合并 baseInfo。 */
    public static String getHeaderPreview(SpelDslConfig config, Map<String, Object> payload) {
        return SpelPayloadEngine.getHeaderPreview(config, payload);
    }

    /** 使用新的引擎生成 header 预览 */
    public static String getHeaderPreviewNew(SpelDslConfig config, Map<String, Object> payload) {
        return SpelPayloadEngine.getHeaderPreview(config, payload);
    }

    /** 校验 payload 与配置的匹配度（长度/数值范围/枚举）。 */
    public static SpelValidator.ValidationResult validateParam(SpelDslConfig config, Map<String, Object> payload) {
        return SpelValidator.validateParam(config, payload);
    }

    /** 使用配置对象 + 外部 payload 生成 URL 预览（base_url + point）。 */
    public static String getUrlPreview(SpelDslConfig config, Map<String, Object> payload) {
        return SpelPayloadEngine.getUrlPreview(config, payload);
    }

    /** 使用新的引擎生成 URL 预览 */
    public static String getUrlPreviewNew(SpelDslConfig config, Map<String, Object> payload) {
        return SpelPayloadEngine.getUrlPreview(config, payload);
    }

    /** 构造 header 预览字符串，支持传入 payload 覆盖默认值。 */
    public static String buildHeaderPreview(List<SpelConfigParser.ParamConfigEntry> entries, Map<String, Object> payload) {
        return SpelPayloadEngine.buildHeaderPreview(entries, payload);
    }

    /** Pretty JSON 输出，方便示例查看。 */
    public static String toPrettyJson(Object obj, int indent) {
        return SpelPayloadEngine.toPrettyJson(obj, indent);
    }

    /** 根据 payload 生成请求 URL，默认拼接 base_url + point。 */
    public static String getUrl(Map<String, Object> payload) {
        return SpelPayloadEngine.getUrl(payload);
    }

    /** 解析 baseInfo JSON 字符串为 Map，兼容大小写 key。 */
    public static Map<String, Object> parseBaseInfo(String baseInfo) {
        return SpelConfigParser.parseBaseInfo(baseInfo);
    }

    /** 便捷实例化调用类。 */
    public static final class Runner {
        private final SpelDslConfig config;
        private final Map<String, Object> payload;

        private Runner(SpelDslConfig config, Map<String, Object> payload) {
            this.config = config;
            this.payload = payload != null ? new LinkedHashMap<>(payload) : new LinkedHashMap<>();
        }

        public String headerPreview() {
            return SpelDsl.getHeaderPreview(config, payload);
        }

        public String paramPreview() {
            return SpelDsl.getParamPreview(config, payload);
        }

        public String urlPreview() {
            return SpelDsl.getUrlPreview(config, payload);
        }

        public Map<String, Object> payload() {
            return payload;
        }

        public SpelValidator.ValidationResult validate() {
            return SpelDsl.validateParam(config, payload);
        }
        
        // 新的预览方法
        public String headerPreviewNew() {
            return SpelDsl.getHeaderPreviewNew(config, payload);
        }
        
        public String paramPreviewNew() {
            return SpelDsl.getParamPreviewNew(config, payload);
        }
        
        public String urlPreviewNew() {
            return SpelDsl.getUrlPreviewNew(config, payload);
        }
    }
}