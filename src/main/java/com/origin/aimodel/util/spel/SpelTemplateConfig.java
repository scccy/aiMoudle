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
     * Header 动态模板 JSON
     */
    private String headerTemplateJson;

    /**
     * Param 动态模板 JSON
     */
    private String paramTemplateJson;

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
}
