package com.origin.aimodel.util.spel;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.origin.aimodel.util.jsonplus.JsonPlusTemplateCodec;
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
        return JsonPlusTemplateCodec.decodeTemplate(templateJson);
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
