package com.origin.aimodel.util.spel;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 负责承载模型在数据库中配置的 JSON 模板及运行期上下文
 */
@Data
@Accessors(chain = true)
public class SpelTemplateConfig {

    /**
     * 用于日志或缓存的模型标识
     */
    private String modelName;

    /**
     * URL 模板，可引用 SpEL 变量
     */
    private String urlTemplate;

    /**
     * 原始环境变量 JSON（如 baseUrl/token 等）
     */
    private String envJson;

    /**
     * 原始前端 payload JSON（别名参数）
     */
    private String frontPayloadJson;

    /**
     * 别名与真实字段关系 JSON
     */
    private String aliasMappingJson;

    /**
     * 追加别名映射的增量 JSON（可选）
     */
    private String aliasMappingJsonPlus;

    /**
     * 新命名：追加别名映射的增量 JSON（可选）
     */
    private String aliasPatchJson;

    /**
     * Header 动态模板 JSON
     */
    private String headerTemplateJson;

    /**
     * Param 动态模板 JSON
     */
    private String paramTemplateJson;

    /**
     * Param 动态模板增量 JSON（可选）
     */
    private String paramTemplateJsonPlus;

    /**
     * 新命名：Param 动态模板增量 JSON（可选）
     */
    private String paramPatchTemplateJson;

    /**
     * 数组追加场景：别名补丁 JSON（兼容 addListAliasMappingJsonPlus）
     */
    private String arrayPatchAliasJson;

    /**
     * 数组追加场景：模板 JSON（SpEL 输出 JSONArray / 单对象）
     */
    private String arrayPatchTemplateJson;

    /**
     * 数组追加场景：目标 key，默认 content
     */
    private String arrayPatchTargetKey = "content";

    /**
     * 数组追加场景：合并策略，默认 APPEND，可选 OVERWRITE
     */
    private String arrayPatchStrategy = "APPEND";

    /**
     * Map 追加/覆盖：模板 JSON（SpEL 输出 JSONObject）
     */
    private String mapPatchTemplateJson;

    /**
     * Map 追加/覆盖：目标 key，默认顶层
     */
    private String mapPatchTargetKey;

    /**
     * Map 追加/覆盖：合并策略，默认 AUTO，可选 OVERWRITE
     */
    private String mapPatchStrategy = "AUTO";

    /**
     * 兼容旧命名：数组追加字段
     */
    private String addListAliasMappingJsonPlus;
    private String addListPlusTemplateJson;
    private String addListTargetKey;
    private String addListMergeStrategy;

    /**
     * SpEL 变量定义 JSON 数组，默认注册 env/payload/task
     */
    private String contextVariableJson = "["
            + "{\"varName\":\"env\",\"source\":\"env\"},"
            + "{\"varName\":\"payload\",\"source\":\"payload\"},"
            + "{\"varName\":\"task\",\"source\":\"task\"}"
            + "]";

    /**
     * 上下文数据源映射 JSON，默认暴露 env/payload/task
     */
    private String contextDataJson = "{"
            + "\"env\":\"env\","
            + "\"payload\":\"payload\","
            + "\"task\":\"task\""
            + "}";

    /**
     * 解析后的 payload 需挂载到的 key（默认 payload）
     */
    private String payloadContextKey = "payload";

    /**
     * 解析后的 env 需挂载到的 key（默认 env）
     */
    private String envContextKey = "env";

    /**
     * 供外部扩展的运行期上下文
     */
    private final Map<String, Object> builtinContext = new LinkedHashMap<>();

    public SpelTemplateConfig addBuiltinContext(String key, Object value) {
        if (key != null && value != null) {
            builtinContext.put(key, value);
        }
        return this;
    }

    public SpelTemplateConfig appendContextVariable(String varName, String source) {
        if (varName == null || source == null) {
            return this;
        }
        String variableJson = contextVariableJson == null ? "[]" : contextVariableJson;
        String toAppend = String.format("{\"varName\":\"%s\",\"source\":\"%s\"}", varName, source);
        if ("[]".equals(variableJson.trim())) {
            contextVariableJson = "[" + toAppend + "]";
        } else if (!variableJson.contains(toAppend)) {
            contextVariableJson = variableJson.replaceFirst("\\]$", "," + toAppend + "]");
        }
        return this;
    }

    public SpelTemplateConfig appendContextData(String contextKey, String sourceKey) {
        if (contextKey == null || sourceKey == null) {
            return this;
        }
        String dataJson = contextDataJson == null ? "{}" : contextDataJson;
        if ("{}".equals(dataJson.trim())) {
            contextDataJson = String.format("{\"%s\":\"%s\"}", contextKey, sourceKey);
        } else if (!dataJson.contains("\"" + contextKey + "\"")) {
            contextDataJson = dataJson.replaceFirst("}$", String.format(",\"%s\":\"%s\"}", contextKey, sourceKey));
        }
        return this;
    }

    /**
     * 追加增量 alias 映射，保持 JSON 字符串形态
     */
    public SpelTemplateConfig addAliasMappingPlus(String alias, String realKey) {
        if (alias == null || realKey == null) {
            return this;
        }
        String baseJson = aliasPatchJson != null ? aliasPatchJson : aliasMappingJsonPlus;
        if (baseJson == null || baseJson.trim().isEmpty()) {
            baseJson = "{}";
        }
        String merged = com.origin.aimodel.util.jsonplus.JsonPlusTemplateCodec.encodeTemplate(
                com.origin.aimodel.util.jsonplus.JsonPlusTemplateCodec.mergeTemplateToMap(baseJson,
                        String.format("{\"%s\":\"%s\"}", alias, realKey)));
        this.aliasMappingJsonPlus = merged;
        this.aliasPatchJson = merged;
        return this;
    }

    /**
     * 追加增量 param 模板，保持 JSON 字符串形态
     */
    public SpelTemplateConfig addParamTemplatePlus(String key, String spelExpr) {
        if (key == null || spelExpr == null) {
            return this;
        }
        String baseJson = paramPatchTemplateJson != null ? paramPatchTemplateJson : paramTemplateJsonPlus;
        if (baseJson == null || baseJson.trim().isEmpty()) {
            baseJson = "{}";
        }
        String merged = com.origin.aimodel.util.jsonplus.JsonPlusTemplateCodec.encodeTemplate(
                com.origin.aimodel.util.jsonplus.JsonPlusTemplateCodec.mergeTemplateToMap(baseJson,
                        String.format("{\"%s\":\"%s\"}", key, spelExpr)));
        this.paramTemplateJsonPlus = merged;
        this.paramPatchTemplateJson = merged;
        return this;
    }
}
