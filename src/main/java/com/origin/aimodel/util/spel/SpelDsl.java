package com.origin.aimodel.util.spel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 轻量级的 SpEL 配置解析/预览工具，用于从 headerItem/paramItem 配置生成示例结构。
 * 无实际 SpEL 解析，仅按 {value} 模板拼接。
 */
public final class SpelDsl {

    private SpelDsl() {
    }

    private static final Map<String, Set<String>> ENUM_RULES = Map.of(
            // 示例：可按业务补充更多 key 的枚举约束
            "movement", Set.of("simple", "down_back", "forward_up", "right_turn_forward", "left_turn_forward")
    );

    /** 创建基于配置与 payload 的运行器，便于实例化调用。 */
    public static Runner Runner(SpelDslConfig config, Map<String, Object> payload) {
        return new Runner(config, payload);
    }

    /** 解析 headerItem/paramItem 配置字符串，提取字段属性。 */
    public static List<ParamConfigEntry> parseConfig(String config) {
        List<ParamConfigEntry> list = new ArrayList<>();
        String[] lines = config.split("\\R");
        ParamConfigEntry current = null;
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.startsWith("\"key\"")) {
                if (current != null) {
                    list.add(current);
                }
                current = new ParamConfigEntry();
                current.key = extractQuotedValue(line);
            } else if (line.startsWith("\"category\"") && current != null) {
                current.category = extractQuotedValue(line);
            } else if (line.startsWith("\"node\"") && current != null) {
                current.node = extractQuotedValue(line);
            } else if (line.startsWith("\"post_param\"") && current != null) {
                current.postParam = extractQuotedValue(line);
            } else if (line.startsWith("\"spel_temp\"") && current != null) {
                current.spelTemp = extractQuotedValue(line);
            } else if (line.startsWith("\"default_value\"") && current != null) {
                current.defaultValue = extractQuotedValue(line);
            } else if (line.startsWith("\"value_object\"") && current != null) {
                current.valueObject = extractQuotedValue(line);
            }
        }
        if (current != null) {
            list.add(current);
        }
        return list;
    }

    /** 基于配置生成示例 payload（优先使用 default_value，否则按 value_object 补齐示例值）。 */
    public static Map<String, Object> buildSamplePayload(List<ParamConfigEntry> entries) {
        Map<String, Object> payload = new LinkedHashMap<>();
        for (ParamConfigEntry entry : entries) {
            Object value = entry.defaultValue != null ? normalizeDefaultValue(entry.defaultValue) : sampleValueByType(entry);
            payload.put(entry.key, value);
        }
        return payload;
    }

    /** 根据配置与输入 payload 构造结果结构。 */
    public static Map<String, Object> buildResult(List<ParamConfigEntry> entries, Map<String, Object> payload) {
        Map<String, Object> root = new LinkedHashMap<>();
        for (ParamConfigEntry entry : entries) {
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
        var headerEntries = parseConfig(config.headerItem());
        var merged = mergePayload(config, payload, headerEntries);
        return buildHeaderPreview(headerEntries, merged);
    }

    /** 使用配置对象 + 外部 payload 生成 param 预览，自动合并 baseInfo。 */
    public static String getParamPreview(SpelDslConfig config, Map<String, Object> payload) {
        var entries = parseConfig(config.paramItem());
        var merged = mergePayload(config, payload, entries);
        var result = buildResult(entries, merged);
        return toPrettyJson(result, 0);
    }

    /** 校验 payload 与配置的匹配度（长度/数值范围/枚举）。 */
    public static ValidationResult validateParam(SpelDslConfig config, Map<String, Object> payload) {
        var entries = parseConfig(config.paramItem());
        var merged = mergePayload(config, payload, entries);
        return validateEntries(entries, merged);
    }

    /** 使用配置对象 + 外部 payload 生成 URL 预览（base_url + point）。 */
    public static String getUrlPreview(SpelDslConfig config, Map<String, Object> payload) {
        var merged = mergePayload(config, payload, null);
        return getUrl(merged);
    }

    /** 构造 header 预览字符串，支持传入 payload 覆盖默认值。 */
    public static String buildHeaderPreview(List<ParamConfigEntry> entries, Map<String, Object> payload) {
        List<String> items = new ArrayList<>();
        for (ParamConfigEntry entry : entries) {
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

    private static void applyNode(Map<String, Object> root, ParamConfigEntry entry, Object value) {
        String[] tokens = entry.node.split("\\.");
        Map<String, Object> currentObj = root;
        for (int i = 0; i < tokens.length - 1; i++) {
            Token token = Token.parse(tokens[i]);
            currentObj = getOrCreateChildMap(currentObj, token);
        }

        Token lastToken = Token.parse(tokens[tokens.length - 1]);
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

    private static Map<String, Object> getOrCreateChildMap(Map<String, Object> parent, Token token) {
        if (token.hasIndex) {
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

    private static Object sampleValueByType(ParamConfigEntry entry) {
        String type = entry.valueObject != null ? entry.valueObject : "string";
        switch (type) {
            case "int":
                return 1;
            case "double":
                return 1.0;
            case "list<string>":
                return List.of("example-" + entry.key);
            case "list<json>":
                Map<String, Object> nested = new LinkedHashMap<>();
                nested.put("field", "example-" + entry.key);
                return List.of(nested);
            case "map":
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("field", "example-" + entry.key);
                return map;
            default:
                return "example-" + entry.key;
        }
    }

    /**
     * 根据 payload 生成请求 URL，默认拼接 base_url + point。
     */
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

    /** 解析 baseInfo JSON 字符串为 Map，兼容大小写 key。 */
    public static Map<String, Object> parseBaseInfo(String baseInfo) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (baseInfo == null || baseInfo.isBlank()) {
            return map;
        }
        String json = baseInfo.replace("\n", "").replace("\r", "").trim();
        if (json.startsWith("{") && json.endsWith("}")) {
            json = json.substring(1, json.length() - 1);
        }
        String[] pairs = json.split(",");
        for (String pair : pairs) {
            String[] kv = pair.split(":", 2);
            if (kv.length == 2) {
                String key = kv[0].trim().replace("\"", "");
                String value = kv[1].trim().replace("\"", "");
                map.put(key, value);
                map.put(key.toLowerCase(), value);
            }
        }
        return map;
    }

    private static String normalizeDefaultValue(String value) {
        if (value.contains("${") && value.contains("}")) {
            return value.replace("${", "$").replace("}", "");
        }
        return value;
    }

    private static String extractQuotedValue(String line) {
        int first = line.indexOf('"');
        int second = line.indexOf('"', first + 1);
        int third = line.indexOf('"', second + 1);
        int fourth = line.indexOf('"', third + 1);
        if (third >= 0 && fourth > third) {
            return line.substring(third + 1, fourth);
        }
        return "";
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
                                                    List<ParamConfigEntry> entries) {
        Map<String, Object> merged = new LinkedHashMap<>(parseBaseInfo(config.baseInfo()));
        if (payload != null) {
            merged.putAll(payload);
        }
        if (entries != null) {
            for (ParamConfigEntry entry : entries) {
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

    private static ValidationResult validateEntries(List<ParamConfigEntry> entries, Map<String, Object> payload) {
        List<String> errors = new ArrayList<>();
        for (ParamConfigEntry entry : entries) {
            Object val = payload.get(entry.key);
            if (val == null) {
                continue;
            }
            String type = entry.valueObject != null ? entry.valueObject : "string";
            if (val instanceof String s) {
                if ("string".equals(type) && s.length() > 2500) {
                    errors.add(entry.key + " length exceeds 2500");
                }
                Set<String> enums = ENUM_RULES.get(entry.key);
                if (enums != null && !enums.contains(s)) {
                    errors.add(entry.key + " not in enum " + enums);
                }
            }
            if (type.equals("int") || type.equals("double") || type.equals("float")) {
                Double num = parseNumber(val);
                if (num == null) {
                    errors.add(entry.key + " is not a number");
                } else if (num < 0.0 || num > 1.0) {
                    errors.add(entry.key + " out of range [0,1]");
                }
            }
        }
        return new ValidationResult(errors);
    }

    private static Double parseNumber(Object val) {
        if (val instanceof Number n) {
            return n.doubleValue();
        }
        try {
            return Double.parseDouble(val.toString());
        } catch (Exception e) {
            return null;
        }
    }

    /** 校验结果承载类。 */
    public static final class ValidationResult {
        private final List<String> errors;

        ValidationResult(List<String> errors) {
            this.errors = errors;
        }

        public boolean isValid() {
            return errors.isEmpty();
        }

        public List<String> getErrors() {
            return errors;
        }
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

        public ValidationResult validate() {
            return SpelDsl.validateParam(config, payload);
        }
    }

    /** 配置项承载类。 */
    public static final class ParamConfigEntry {
        public String key;
        public String category;
        public String node;
        public String postParam;
        public String spelTemp;
        public String defaultValue;
        public String valueObject;
    }

    private static final class Token {
        final String name;
        final boolean hasIndex;
        final int index;

        private Token(String name, boolean hasIndex, int index) {
            this.name = name;
            this.hasIndex = hasIndex;
            this.index = index;
        }

        static Token parse(String token) {
            int l = token.indexOf('[');
            int r = token.indexOf(']');
            if (l > 0 && r > l) {
                String name = token.substring(0, l);
                int idx = Integer.parseInt(token.substring(l + 1, r));
                return new Token(name, true, idx);
            }
            return new Token(token, false, -1);
        }
    }
}
