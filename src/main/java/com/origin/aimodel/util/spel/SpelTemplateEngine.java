package com.origin.aimodel.util.spel;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 将 JSON 配置、上下文数据与 SpEL 模板解耦的工具类
 */
@Slf4j
public class SpelTemplateEngine {

    private final ExpressionParser parser = new SpelExpressionParser();
    private final TemplateParserContext parserContext = new TemplateParserContext();

    public SpelEvaluationResult evaluate(SpelTemplateConfig config) {
        Objects.requireNonNull(config, "SpelTemplateConfig 不能为空");

        Map<String, Object> envContext = parseJsonToObjectMap(config.getEnvJson());
        Map<String, Object> frontPayload = parseJsonToObjectMap(config.getFrontPayloadJson());
        Map<String, String> aliasMapping = parseAliasMappingJson(config.getAliasMappingJson());
        Map<String, Object> payloadContext = remapPayload(frontPayload, aliasMapping);

        Map<String, Object> builtinContext = new LinkedHashMap<>(Optional.ofNullable(config.getBuiltinContext()).orElseGet(LinkedHashMap::new));
        if (envContext != null && !envContext.isEmpty() && StringUtils.hasText(config.getEnvContextKey())) {
            builtinContext.put(config.getEnvContextKey(), envContext);
        }
        if (payloadContext != null && !payloadContext.isEmpty() && StringUtils.hasText(config.getPayloadContextKey())) {
            builtinContext.put(config.getPayloadContextKey(), payloadContext);
        }

        Map<String, Object> contextDataSource = buildContextDataSource(config.getContextDataJson(), builtinContext);
        List<ContextVariableDefinition> variableDefinitions = parseContextVariableDefinitions(config.getContextVariableJson());
        StandardEvaluationContext evaluationContext = new StandardEvaluationContext();
        registerContextVariables(evaluationContext, variableDefinitions, contextDataSource);

        Map<String, Object> resolvedHeader = evaluateTemplateMap(parseTemplateJson(config.getHeaderTemplateJson()), evaluationContext);
        Map<String, Object> resolvedParam = evaluateTemplateMap(parseTemplateJson(config.getParamTemplateJson()), evaluationContext);
        String resolvedUrl = null;
        if (StringUtils.hasText(config.getUrlTemplate())) {
            resolvedUrl = parser.parseExpression(config.getUrlTemplate(), parserContext).getValue(evaluationContext, String.class);
        }

        return new SpelEvaluationResult()
                .setResolvedUrl(resolvedUrl)
                .setResolvedHeader(resolvedHeader)
                .setResolvedParam(resolvedParam)
                .setPayloadContext(payloadContext)
                .setBuiltinContext(builtinContext)
                .setContextDataSource(contextDataSource);
    }

    private Map<String, String> parseTemplateJson(String templateJson) {
        Map<String, String> template = new LinkedHashMap<>();
        if (!StringUtils.hasText(templateJson)) {
            return template;
        }
        JSONObject jsonObject;
        String toParse = templateJson;
        try {
            jsonObject = JSONObject.parseObject(toParse);
        } catch (Exception ex) {
            // 尝试自动转义 SpEL 表达式中的未转义双引号，避免常见拼接错误
            toParse = escapeSpelQuotes(toParse);
            try {
                jsonObject = JSONObject.parseObject(toParse);
            } catch (Exception retryEx) {
                // 兜底使用宽松解析，按 key:value 手工拆分，尽可能给出有意义的错误
                jsonObject = lenientFlatParse(toParse);
                if (jsonObject == null) {
                    throw new IllegalArgumentException("模板 JSON 解析失败，检查转义/引号是否正确: " + templateJson, retryEx);
                }
            }
        }
        if (jsonObject != null) {
            jsonObject.forEach((key, value) -> template.put(key, value == null ? null : String.valueOf(value)));
        }
        // 兜底：如果模板值里残留了 JSON 转义的 \"，会导致 SpEL 中的字符串字面量仍携带反斜杠，这里统一去掉
        template.replaceAll((k, v) -> unescapeTemplateValue(v));
        return template;
    }

    /**
     * 针对 value 形如 ":{\"#{" ... "}" 的场景，将内部未转义的双引号自动加上反斜杠，避免 JSON 解析失败。
     * 仅在 parse 失败时作为降级修复。
     */
    private String escapeSpelQuotes(String raw) {
        StringBuilder sb = new StringBuilder(raw.length() + 16);
        int len = raw.length();
        int i = 0;
        boolean inSpelValue = false;
        while (i < len) {
            char c = raw.charAt(i);
            if (!inSpelValue) {
                if (c == ':') {
                    sb.append(c);
                    int j = i + 1;
                    while (j < len && Character.isWhitespace(raw.charAt(j))) {
                        sb.append(raw.charAt(j));
                        j++;
                    }
                    if (j + 2 < len && raw.charAt(j) == '"' && raw.charAt(j + 1) == '#' && raw.charAt(j + 2) == '{') {
                        sb.append('"').append('#').append('{');
                        i = j + 3;
                        inSpelValue = true;
                        continue;
                    }
                } else {
                    sb.append(c);
                }
                i++;
                continue;
            }

            // in SpEL value
            if (c == '"' && raw.charAt(i - 1) != '\\') {
                sb.append('\\').append('"');
            } else {
                sb.append(c);
            }
            if (c == '}') {
                int j = i + 1;
                while (j < len && Character.isWhitespace(raw.charAt(j))) {
                    j++;
                }
                if (j < len && raw.charAt(j) == '"') {
                    inSpelValue = false;
                }
            }
            i++;
        }
        return sb.toString();
    }

    /**
     * 宽松解析：仅支持扁平 key:value 结构，按顶层逗号拆分后取字符串值，避免因转义问题导致整体不可解析。
     */
    private JSONObject lenientFlatParse(String raw) {
        String text = raw == null ? "" : raw.trim();
        if (text.startsWith("{") && text.endsWith("}")) {
            text = text.substring(1, text.length() - 1);
        }
        List<String> pairs = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"' && (i == 0 || text.charAt(i - 1) != '\\')) {
                inQuotes = !inQuotes;
            }
            if (c == ',' && !inQuotes) {
                pairs.add(current.toString());
                current.setLength(0);
                continue;
            }
            current.append(c);
        }
        if (current.length() > 0) {
            pairs.add(current.toString());
        }
        JSONObject obj = new JSONObject();
        for (String pair : pairs) {
            if (!StringUtils.hasText(pair)) {
                continue;
            }
            int colonIdx = pair.indexOf(':');
            if (colonIdx <= 0) {
                continue;
            }
            String keyPart = pair.substring(0, colonIdx).trim();
            String valPart = pair.substring(colonIdx + 1).trim();
            String key = trimQuotes(keyPart);
            String value = trimQuotes(valPart);
            obj.put(key, value);
        }
        return obj;
    }

    private String unescapeTemplateValue(String v) {
        if (v == null) {
            return null;
        }
        String fixed = v;
        // 先将多余的反斜杠压缩一层
        while (fixed.contains("\\\\")) {
            fixed = fixed.replace("\\\\", "\\");
        }
        if (fixed.contains("\\\"")) {
            fixed = fixed.replace("\\\"", "\"");
        }
        return fixed;
    }

    private String trimQuotes(String text) {
        if (text == null) {
            return null;
        }
        String t = text.trim();
        if (t.length() >= 2 && t.charAt(0) == '"' && t.charAt(t.length() - 1) == '"' && t.charAt(t.length() - 2) != '\\') {
            return t.substring(1, t.length() - 1);
        }
        return t;
    }

    private Map<String, Object> parseJsonToObjectMap(String json) {
        if (!StringUtils.hasText(json)) {
            return new LinkedHashMap<>();
        }
        JSONObject jsonObject = JSONObject.parseObject(json);
        Map<String, Object> result = new LinkedHashMap<>();
        if (jsonObject != null) {
            jsonObject.forEach(result::put);
        }
        return result;
    }

    private Map<String, Object> remapPayload(Map<String, Object> aliasPayload, Map<String, String> aliasToRealKey) {
        Map<String, Object> remapped = new LinkedHashMap<>();
        if (aliasPayload == null || aliasToRealKey == null) {
            return remapped;
        }
        aliasPayload.forEach((aliasKey, value) -> {
            String realKey = aliasToRealKey.get(aliasKey);
            if (realKey != null) {
                remapped.put(realKey, value);
            }
        });
        return remapped;
    }

    private Map<String, String> parseAliasMappingJson(String aliasMappingJson) {
        Map<String, String> mapping = new LinkedHashMap<>();
        if (!StringUtils.hasText(aliasMappingJson)) {
            return mapping;
        }
        JSONObject jsonObject = JSONObject.parseObject(aliasMappingJson);
        if (jsonObject != null) {
            jsonObject.forEach((key, value) -> mapping.put(key, value == null ? null : String.valueOf(value)));
        }
        return mapping;
    }

    private List<ContextVariableDefinition> parseContextVariableDefinitions(String contextVariableJson) {
        List<ContextVariableDefinition> definitions = new ArrayList<>();
        if (StringUtils.hasText(contextVariableJson)) {
            List<ContextVariableDefinition> parsed = JSONArray.parseArray(contextVariableJson, ContextVariableDefinition.class);
            if (parsed != null) {
                definitions.addAll(parsed);
            }
        }
        if (definitions.isEmpty()) {
            definitions.add(new ContextVariableDefinition("env", "env"));
            definitions.add(new ContextVariableDefinition("payload", "payload"));
        }
        return definitions;
    }

    private Map<String, Object> buildContextDataSource(String contextDataJson, Map<String, Object> builtinContext) {
        Map<String, Object> contextDataSource = new LinkedHashMap<>();
        JSONObject configurableContext = JSONObject.parseObject(contextDataJson);
        if (configurableContext != null && !configurableContext.isEmpty()) {
            configurableContext.forEach((contextKey, sourceKeyObj) -> {
                if (contextKey == null) {
                    return;
                }
                String sourceKey = sourceKeyObj == null ? contextKey : String.valueOf(sourceKeyObj);
                Object value = builtinContext.get(sourceKey);
                if (value != null) {
                    contextDataSource.put(contextKey, value);
                } else {
                    log.warn("上下文 key:{} 未在 builtinContext 中找到 source:{}", contextKey, sourceKey);
                }
            });
        }
        if (contextDataSource.isEmpty()) {
            contextDataSource.putAll(builtinContext);
        }
        return contextDataSource;
    }

    private void registerContextVariables(StandardEvaluationContext evaluationContext,
                                          List<ContextVariableDefinition> definitions,
                                          Map<String, Object> contextDataSource) {
        definitions.forEach(definition -> {
            if (definition == null) {
                return;
            }
            Object value = contextDataSource.get(definition.getSource());
            if (value != null) {
                evaluationContext.setVariable(definition.getVarName(), value);
            }
        });
    }

    private Map<String, Object> evaluateTemplateMap(Map<String, String> template,
                                                    StandardEvaluationContext evaluationContext) {
        Map<String, Object> resolved = new LinkedHashMap<>();
        template.forEach((key, value) -> {
            Object evalValue = parser.parseExpression(value, parserContext).getValue(evaluationContext);
            resolved.put(key, evalValue);
        });
        return resolved;
    }

    /**
     * 描述 SpEL 变量名与上下文来源关系
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ContextVariableDefinition {
        private String varName;
        private String source;
    }
}
