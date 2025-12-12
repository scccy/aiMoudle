package com.origin.aimodel.util.spel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 负责 mergePayload（baseInfo + 外部 payload + model 兜底）。
 * 负责 buildResult/applyNode（模板渲染、list/map 紧凑 JSON 化）。
 * 提供 getUrl/getHeaderPreview/getParamPreview/newRunner（Runner 持有 config+payload）。
 */
public class SpelPayloadEngine {
    
    private SpelPayloadEngine() {
    }
    
    /** 根据配置与输入 payload 构造结果结构。 */
    public static Map<String, Object> buildResult(List<SpelConfigParser.ParamConfigEntry> entries, Map<String, Object> payload) {
        Map<String, Object> root = new LinkedHashMap<>();
        for (SpelConfigParser.ParamConfigEntry entry : entries) {
            Object value = payload.get(entry.key);
            if (value == null) {
                value = entry.defaultValue != null ? normalizeDefaultValue(entry.defaultValue) : null;
            }
            if (value == null) {
                // 无入参且无默认值时，跳过可选字段，不再填充示例值
                continue;
            }
            applyNode(root, entry, value);
        }
        return root;
    }
    
    /** 使用配置对象 + 外部 payload 生成 header 预览，自动合并 baseInfo。 */
    public static String getHeaderPreview(SpelDslConfig config, Map<String, Object> payload) {
        var headerEntries = SpelConfigParser.parseConfig(config.headerItem());
        var merged = mergePayload(config, payload, headerEntries);
        return buildHeaderPreview(headerEntries, merged);
    }
    
    /** 使用配置对象 + 外部 payload 生成 param 预览，自动合并 baseInfo。 */
    public static String getParamPreview(SpelDslConfig config, Map<String, Object> payload) {
        var entries = SpelConfigParser.parseConfig(config.paramItem());
        var merged = mergePayload(config, payload, entries);
        var result = buildResult(entries, merged);
        return toPrettyJson(result, 0);
    }
    
    /** 使用配置对象 + 外部 payload 生成 URL 预览（base_url + point）。 */
    public static String getUrlPreview(SpelDslConfig config, Map<String, Object> payload) {
        var merged = mergePayload(config, payload, null);
        return getUrl(merged);
    }
    
    /** 构造 header 预览字符串，支持传入 payload 覆盖默认值。 */
    public static String buildHeaderPreview(List<SpelConfigParser.ParamConfigEntry> entries, Map<String, Object> payload) {
        List<String> items = new ArrayList<>();
        for (SpelConfigParser.ParamConfigEntry entry : entries) {
            String key = entry.postParam != null ? entry.postParam : entry.node;
            Object raw = payload != null ? payload.getOrDefault(entry.key, payload.get(entry.key != null ? entry.key.toLowerCase() : null)) : null;
            String value = raw != null ? raw.toString()
                    : (entry.defaultValue != null ? normalizeDefaultValue(entry.defaultValue) : "example-" + entry.key);
            items.add(key + ": " + value);
        }
        return formatHeaderResult(items);
    }
    
    /** Pretty JSON 输出，方便示例查看。 */
    public static String toPrettyJson(Object obj, int indent) {
        String indentStr = "    ".repeat(indent);
        if (obj instanceof Map<?, ?> map) {
            StringBuilder sb = new StringBuilder();
            sb.append("{\n");
            int i = 0;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                sb.append(indentStr).append("    \"").append(entry.getKey()).append("\": ").append(toPrettyJson(entry.getValue(), indent + 1));
                if (++i < map.size()) {
                    sb.append(",");
                }
                sb.append("\n");
            }
            sb.append(indentStr).append("}");
            return sb.toString();
        } else if (obj instanceof List<?> list) {
            StringBuilder sb = new StringBuilder();
            sb.append("[\n");
            for (int i = 0; i < list.size(); i++) {
                sb.append(indentStr).append("    ").append(toPrettyJson(list.get(i), indent + 1));
                if (i < list.size() - 1) {
                    sb.append(",");
                }
                sb.append("\n");
            }
            sb.append(indentStr).append("]");
            return sb.toString();
        } else if (obj instanceof String s) {
            return "\"" + s + "\"";
        } else {
            return String.valueOf(obj);
        }
    }
    
    /** 根据 payload 生成请求 URL，默认拼接 base_url + point。 */
    public static String getUrl(Map<String, Object> payload) {
        String base = stringVal(payload, "base_url");
        String point = stringVal(payload, "point");
        if (base == null && point == null) {
            return "";
        }
        if (base == null) {
            return point;
        }
        if (point == null) {
            return base;
        }
        if (base.endsWith("/") && point.startsWith("/")) {
            return base + point.substring(1);
        }
        if (!base.endsWith("/") && !point.startsWith("/")) {
            return base + "/" + point;
        }
        return base + point;
    }
    
    // 私有辅助方法
    
    private static void applyNode(Map<String, Object> root, SpelConfigParser.ParamConfigEntry entry, Object value) {
        String[] tokens = entry.node.split("\\.");
        Map<String, Object> currentObj = root;
        for (int i = 0; i < tokens.length - 1; i++) {
            SpelConfigParser.Token token = SpelConfigParser.Token.parse(tokens[i]);
            currentObj = getOrCreateChildMap(currentObj, token);
        }
        
        SpelConfigParser.Token lastToken = SpelConfigParser.Token.parse(tokens[tokens.length - 1]);
        Map<String, Object> targetObj = lastToken.hasIndex ? getOrCreateChildMap(currentObj, lastToken) : currentObj;
        String fieldName = entry.postParam != null ? entry.postParam : lastToken.name;
        
        if ("list".equalsIgnoreCase(entry.category)) {
            List<Object> list = (List<Object>) targetObj.computeIfAbsent(fieldName, k -> new ArrayList<>());
            if (value instanceof List<?> collection) {
                list.addAll(collection);
            } else {
                list.add(value);
            }
            return;
        }
        
        Object rendered = renderForValue(entry.spelTemp, value);
        Object existing = targetObj.get(fieldName);
        if (existing instanceof String && rendered instanceof String) {
            targetObj.put(fieldName, ((String) existing) + rendered);
        } else {
            targetObj.put(fieldName, rendered);
        }
    }
    
    private static Map<String, Object> getOrCreateChildMap(Map<String, Object> parent, SpelConfigParser.Token token) {
        if (token.hasIndex) {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) parent.computeIfAbsent(token.name, k -> new ArrayList<>());
            while (list.size() <= token.index) {
                list.add(new LinkedHashMap<String, Object>());
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> child = (Map<String, Object>) list.get(token.index);
            return child;
        } else {
            @SuppressWarnings("unchecked")
            Map<String, Object> child = (Map<String, Object>) parent.computeIfAbsent(token.name, k -> new LinkedHashMap<String, Object>());
            return child;
        }
    }
    
    private static String formatHeaderResult(List<String> items) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        for (int i = 0; i < items.size(); i++) {
            sb.append("  \"").append(items.get(i)).append("\"");
            if (i < items.size() - 1) {
                sb.append(" ,");
            }
            sb.append("\n");
        }
        sb.append("}");
        return sb.toString();
    }
    
    private static String renderWithTemplate(String spelTemp, String value) {
        if (spelTemp == null || spelTemp.isEmpty()) {
            return value;
        }
        return spelTemp.replace("{value}", value);
    }
    
    private static Object renderForValue(String spelTemp, Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            return renderWithTemplate(spelTemp, (String) value);
        }
        return value;
    }
    
    private static String normalizeDefaultValue(String value) {
        if (value.contains("${") && value.contains("}")) {
            return value.replace("${", "$").replace("}", "");
        }
        return value;
    }
    
    private static String stringVal(Map<String, Object> map, String key) {
        if (map == null) {
            return null;
        }
        Object v = map.get(key);
        if (v == null) {
            v = map.get(key.toLowerCase());
        }
        return v != null ? v.toString() : null;
    }
    
    private static Map<String, Object> mergePayload(SpelDslConfig config, Map<String, Object> payload,
                                                    List<SpelConfigParser.ParamConfigEntry> entries) {
        Map<String, Object> merged = new LinkedHashMap<>(SpelConfigParser.parseBaseInfo(config.baseInfo()));
        if (payload != null) {
            merged.putAll(payload);
        }
        if (entries != null) {
            for (SpelConfigParser.ParamConfigEntry entry : entries) {
                if ("model".equalsIgnoreCase(entry.key) && !merged.containsKey(entry.key)) {
                    String baseModel = stringVal(merged, "model");
                    if (baseModel != null) {
                        merged.put(entry.key, baseModel);
                    }
                }
            }
        }
        return merged;
    }
}