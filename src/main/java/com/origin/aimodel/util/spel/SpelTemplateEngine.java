package com.origin.aimodel.util.spel;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.origin.aimodel.util.jsonplus.JsonPlusTemplateCodec;
import com.origin.aimodel.util.jsonplus.JsonPlusMerger;
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
import java.util.Collection;
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
        Map<String, String> aliasMapping = parseAliasMappingJson(config);
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

        Map<String, Object> resolvedHeader = evaluateTemplateMap(parseTemplateJson(config.getHeaderTemplateJson(), null), evaluationContext);

        // 先解析 base param
        Map<String, Object> resolvedParam = evaluateTemplateMap(parseTemplateJson(config.getParamTemplateJson(), null), evaluationContext);
        // param patch
        String paramPatchTemplate = firstNonEmpty(config.getParamPatchTemplateJson(), config.getParamTemplateJsonPlus());
        if (StringUtils.hasText(paramPatchTemplate)) {
            Map<String, Object> plusParam = evaluateTemplateMap(parseTemplateJson(paramPatchTemplate, null), evaluationContext);
            JSONObject baseObj = JSONObject.parseObject(JSON.toJSONString(resolvedParam));
            JSONObject plusObj = JSONObject.parseObject(JSON.toJSONString(plusParam));
            JsonPlusMerger.merge(baseObj, plusObj, JsonPlusMerger.MergeStrategy.AUTO);
            resolvedParam = new LinkedHashMap<>(baseObj);
        }

        // map patch
        applyMapPatch(config, evaluationContext, resolvedParam);

        // array patch
        applyArrayPatch(config, evaluationContext, resolvedParam);

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

    private Map<String, String> parseTemplateJson(String baseJson, String plusJson) {
        return JsonPlusTemplateCodec.mergeTemplateToMap(
                baseJson == null ? "" : baseJson,
                plusJson == null ? "" : plusJson);
    }

    private Map<String, Object> parseJsonToObjectMap(String json) {
        if (!StringUtils.hasText(json)) {
            return new LinkedHashMap<>();
        }
        JSONObject jsonObject = JSONObject.parseObject(json);
        Map<String, Object> result = new LinkedHashMap<>();
        if (jsonObject != null) {
            result.putAll(jsonObject);
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

    private Map<String, String> parseAliasMappingJson(SpelTemplateConfig config) {
        String base = config.getAliasMappingJson();
        String patch = firstNonEmpty(config.getAliasPatchJson(), config.getAliasMappingJsonPlus());
        String arrayAlias = firstNonEmpty(config.getArrayPatchAliasJson(), config.getAddListAliasMappingJsonPlus());
        Map<String, String> merged = JsonPlusTemplateCodec.mergeTemplateToMap(base == null ? "" : base, patch == null ? "" : patch);
        if (StringUtils.hasText(arrayAlias)) {
            merged = JsonPlusTemplateCodec.mergeTemplateToMap(JsonPlusTemplateCodec.encodeTemplate(merged), arrayAlias);
        }
        return merged;
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

    private void applyArrayPatch(SpelTemplateConfig config, StandardEvaluationContext evaluationContext, Map<String, Object> resolvedParam) {
        String arrayTemplate = firstNonEmpty(config.getArrayPatchTemplateJson(), config.getAddListPlusTemplateJson());
        if (!StringUtils.hasText(arrayTemplate)) {
            return;
        }
        Object addition = evaluateRawExpression(arrayTemplate, evaluationContext);
        JSONArray additionArray = normalizeToArray(addition);
        if (additionArray == null || additionArray.isEmpty()) {
            return;
        }
        String targetKey = firstNonEmpty(config.getArrayPatchTargetKey(), config.getAddListTargetKey(), "content");
        JsonPlusMerger.MergeStrategy strategy = resolveMergeStrategy(firstNonEmpty(config.getArrayPatchStrategy(), config.getAddListMergeStrategy(), "APPEND"), JsonPlusMerger.MergeStrategy.APPEND);

        JSONObject baseObj = JSONObject.parseObject(JSON.toJSONString(resolvedParam));
        JSONArray targetArray = baseObj.getJSONArray(targetKey);
        if (targetArray == null || strategy == JsonPlusMerger.MergeStrategy.OVERWRITE) {
            targetArray = new JSONArray();
            baseObj.put(targetKey, targetArray);
        }
        if (strategy == JsonPlusMerger.MergeStrategy.OVERWRITE) {
            targetArray.clear();
        }
        for (Object item : additionArray) {
            targetArray.add(deepCopy(item));
        }
        resolvedParam.clear();
        resolvedParam.putAll(baseObj);
    }

    private void applyMapPatch(SpelTemplateConfig config, StandardEvaluationContext evaluationContext, Map<String, Object> resolvedParam) {
        String mapTemplate = config.getMapPatchTemplateJson();
        if (!StringUtils.hasText(mapTemplate)) {
            return;
        }
        Object addition = evaluateRawExpression(mapTemplate, evaluationContext);
        JSONObject additionObj = normalizeToObject(addition);
        if (additionObj == null || additionObj.isEmpty()) {
            return;
        }
        String targetKey = config.getMapPatchTargetKey();
        JsonPlusMerger.MergeStrategy strategy = resolveMergeStrategy(config.getMapPatchStrategy(), JsonPlusMerger.MergeStrategy.AUTO);

        JSONObject baseObj = JSONObject.parseObject(JSON.toJSONString(resolvedParam));
        if (!StringUtils.hasText(targetKey)) {
            JsonPlusMerger.merge(baseObj, additionObj, strategy);
        } else {
            JSONObject target = baseObj.getJSONObject(targetKey);
            if (target == null || strategy == JsonPlusMerger.MergeStrategy.OVERWRITE) {
                baseObj.put(targetKey, deepCopy(additionObj));
            } else {
                JsonPlusMerger.merge(target, additionObj, strategy);
            }
        }
        resolvedParam.clear();
        resolvedParam.putAll(baseObj);
    }

    private JSONArray normalizeToArray(Object addition) {
        if (addition == null) {
            return null;
        }
        if (addition instanceof JSONArray) {
            return JSONArray.parseArray(((JSONArray) addition).toJSONString());
        }
        if (addition instanceof Collection<?>) {
            JSONArray array = new JSONArray();
            ((Collection<?>) addition).forEach(item -> array.add(deepCopy(item)));
            return array;
        }
        if (addition instanceof Object[]) {
            JSONArray array = new JSONArray();
            for (Object item : (Object[]) addition) {
                array.add(deepCopy(item));
            }
            return array;
        }
        if (addition instanceof String && StringUtils.hasText((String) addition)) {
            String value = (String) addition;
            try {
                return JSONArray.parseArray(value);
            } catch (Exception ignored) {
                // ignore and wrap below
            }
        }
        JSONArray array = new JSONArray();
        array.add(deepCopy(addition));
        return array;
    }

    private JSONObject normalizeToObject(Object addition) {
        if (addition == null) {
            return null;
        }
        if (addition instanceof JSONObject) {
            return JSONObject.parseObject(((JSONObject) addition).toJSONString());
        }
        if (addition instanceof Map<?, ?> || addition instanceof String) {
            try {
                return JSONObject.parseObject(JSON.toJSONString(addition));
            } catch (Exception ignored) {
                // fall through
            }
        }
        return null;
    }

    private Object deepCopy(Object value) {
        if (value == null) {
            return null;
        }
        return JSON.parse(JSON.toJSONString(value));
    }

    /**
     * 评估可能是模板表达式（#{...}）的 SpEL，并返回原始对象，避免强制转换为字符串
     */
    private Object evaluateRawExpression(String expression, StandardEvaluationContext evaluationContext) {
        if (!StringUtils.hasText(expression)) {
            return null;
        }
        String exprToEval = expression.trim();
        if (exprToEval.startsWith("#{") && exprToEval.endsWith("}")) {
            exprToEval = exprToEval.substring(2, exprToEval.length() - 1).trim();
        }
        return parser.parseExpression(exprToEval).getValue(evaluationContext);
    }

    private JsonPlusMerger.MergeStrategy resolveMergeStrategy(String strategyName, JsonPlusMerger.MergeStrategy defaultStrategy) {
        if (!StringUtils.hasText(strategyName)) {
            return defaultStrategy;
        }
        try {
            return JsonPlusMerger.MergeStrategy.valueOf(strategyName.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return defaultStrategy;
        }
    }

    private String firstNonEmpty(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
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
