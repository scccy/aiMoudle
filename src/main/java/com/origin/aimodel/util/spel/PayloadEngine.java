package com.origin.aimodel.util.spel;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 新版Payload引擎，支持更复杂的模板处理，包括嵌套paramItem和动态列表处理
 */
public class PayloadEngine {
    
    private PayloadEngine() {
    }
    
    /** 根据配置与输入 payload 构造结果结构。 */
    public static Map<String, Object> buildResult(List<MappingItem> entries, Map<String, Object> payload) {
        Map<String, Object> root = new LinkedHashMap<>();
        for (MappingItem entry : entries) {
            Object value = payload.get(entry.getKey());
            if (value == null && entry.getNode() != null) {
                value = getValueByPath(payload, entry.getNode());
            }
            if (value == null) {
                value = entry.getDefaultValue() != null ? normalizeDefaultValue(entry.getDefaultValue()) : null;
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
    public static String getHeaderPreview(PostDslConfig config, Map<String, Object> payload) {
        List<MappingItem> headerEntries = ConfigParser.parseConfig(config.headerItem());
        Map<String, Object> merged = mergePayload(config, payload, headerEntries);
        return buildHeaderPreview(headerEntries, merged);
    }
    
    /** 使用配置对象 + 外部 payload 生成 param 预览，自动合并 baseInfo。 */
    public static String getParamPreview(PostDslConfig config, Map<String, Object> payload) {
        List<MappingItem> entries = ConfigParser.parseConfig(config.paramItem());
        Map<String, Object> merged = mergePayload(config, payload, entries);
        Map<String, Object> result = buildResult(entries, merged);
        return toPrettyJson(result, 0);
    }
    
    /** 使用配置对象 + 外部 payload 生成 URL 预览（base_url + point）。 */
    public static String getUrlPreview(PostDslConfig config, Map<String, Object> payload) {
        Map<String, Object> merged = mergePayload(config, payload, null);
        return getUrl(merged);
    }
    
    /** 构造 header 预览字符串，支持传入 payload 覆盖默认值。 */
    public static String buildHeaderPreview(List<MappingItem> entries, Map<String, Object> payload) {
        List<String> items = new ArrayList<>();
        for (MappingItem entry : entries) {
            String key = entry.getPostParam() != null ? entry.getPostParam() : entry.getNode();
            Object raw = payload != null ? payload.getOrDefault(entry.getKey(), payload.get(entry.getKey() != null ? entry.getKey().toLowerCase() : null)) : null;
            String value = raw != null ? raw.toString()
                    : (entry.getDefaultValue() != null ? normalizeDefaultValue(entry.getDefaultValue()) : "example-" + entry.getKey());
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
    
    private static void applyNode(Map<String, Object> root, MappingItem entry, Object value) {
        String[] tokens = entry.getNode().split("\\.");
        Map<String, Object> currentObj = root;
        for (int i = 0; i < tokens.length - 1; i++) {
            ConfigParser.Token token = ConfigParser.Token.parse(tokens[i]);
            currentObj = getOrCreateChildMap(currentObj, token);
        }
        
        ConfigParser.Token lastToken = ConfigParser.Token.parse(tokens[tokens.length - 1]);
        Map<String, Object> targetObj = lastToken.hasIndex ? getOrCreateChildMap(currentObj, lastToken) : currentObj;
        String fieldName = entry.getPostParam() != null ? entry.getPostParam() : lastToken.name;
        
        // 处理不同类型的配置项
        switch (entry.getCategory() != null ? entry.getCategory().toLowerCase() : "") {
            case "list":
                // 处理列表类型
                handleListCategory(targetObj, entry, value, fieldName);
                break;
            case "map":
                // 处理map类型
                handleMapCategory(targetObj, entry, value, fieldName);
                break;
            default:
                // 默认按key类型处理
                Object rendered = renderForValue(entry.getSpelTemp(), value);
                Object existing = targetObj.get(fieldName);
                if (existing instanceof String && rendered instanceof String) {
                    String existingStr = (String) existing;
                    String renderedStr = (String) rendered;
                    // 如果新值有模板（spel_temp），追加到后面
                    // 如果新值没有模板，且 existing 以空格开头（说明是模板追加的内容），则新值放在前面
                    if (entry.getSpelTemp() != null && !entry.getSpelTemp().isEmpty()) {
                        // 有模板，追加到后面
                        targetObj.put(fieldName, existingStr + renderedStr);
                    } else if (existingStr.trim().isEmpty() || existingStr.startsWith(" ")) {
                        // 没有模板，且 existing 是空的或以空格开头（模板追加的内容），新值放在前面
                        targetObj.put(fieldName, renderedStr + existingStr);
                    } else {
                        // 其他情况，追加到后面
                        targetObj.put(fieldName, existingStr + renderedStr);
                    }
                } else {
                    targetObj.put(fieldName, rendered);
                }
                break;
        }
    }
    
    private static void handleListCategory(Map<String, Object> targetObj, MappingItem entry, Object value, String fieldName) {
        // 如果值是列表，则对每个元素应用模板
        if (value instanceof List<?> list) {
            List<Object> resultList = new ArrayList<>();
            for (Object item : list) {
                // 检查是否有嵌套的 paramItem 配置
                if (entry.getSpelTemp() != null && (entry.getSpelTemp().contains("\"paramItem\":") || entry.getSpelTemp().contains("paramItem"))) {
                    // 解析嵌套的 paramItem 配置
                    try {
                        // 先尝试直接解析
                        JSONObject spelTempJson = null;
                        try {
                            spelTempJson = JSON.parseObject(entry.getSpelTemp());
                        } catch (Exception parseEx) {
                            // 如果解析失败，尝试处理转义字符
                            String unescaped = entry.getSpelTemp().replace("\\\"", "\"").replace("\\\\", "\\");
                            spelTempJson = JSON.parseObject(unescaped);
                        }
                        if (spelTempJson.containsKey("paramItem")) {
                            // 处理嵌套的paramItem配置，传递父级的 node 值用于替换 @node
                            String rendered = processNestedParamItem(spelTempJson, item, entry.getNode());
                            // 尝试解析为JSON对象
                            if (rendered != null && rendered.trim().startsWith("{")) {
                                try {
                                    JSONObject jsonObj = JSON.parseObject(rendered);
                                    resultList.add(jsonObj);
                                } catch (Exception e) {
                                    // 如果解析失败，尝试作为 Map 处理
                                    try {
                                        Map<String, Object> mapObj = JSON.parseObject(rendered, Map.class);
                                        resultList.add(mapObj);
                                    } catch (Exception e2) {
                                        resultList.add(rendered);
                                    }
                                }
                            } else {
                                resultList.add(rendered);
                            }
                        } else {
                            // 回退到原来的处理方式
                            Object rendered = renderForValue(entry.getSpelTemp(), item);
                            resultList.add(rendered);
                        }
                    } catch (Exception e) {
                        // 如果解析失败，尝试直接处理嵌套 paramItem
                        try {
                            // 再次尝试解析，可能是转义字符问题
                            String unescapedSpelTemp = entry.getSpelTemp().replace("\\\"", "\"");
                            JSONObject spelTempJson = JSON.parseObject(unescapedSpelTemp);
                            if (spelTempJson.containsKey("paramItem")) {
                                String rendered = processNestedParamItem(spelTempJson, item, entry.getNode());
                                if (rendered != null && rendered.trim().startsWith("{")) {
                                    try {
                                        JSONObject jsonObj = JSON.parseObject(rendered);
                                        resultList.add(jsonObj);
                                    } catch (Exception e2) {
                                        try {
                                            Map<String, Object> mapObj = JSON.parseObject(rendered, Map.class);
                                            resultList.add(mapObj);
                                        } catch (Exception e3) {
                                            resultList.add(rendered);
                                        }
                                    }
                                } else {
                                    resultList.add(rendered);
                                }
                            } else {
                                Object rendered = renderForValue(entry.getSpelTemp(), item);
                                resultList.add(rendered);
                            }
                        } catch (Exception e2) {
                            // 如果还是失败，回退到原来的处理方式
                            Object rendered = renderForValue(entry.getSpelTemp(), item);
                            resultList.add(rendered);
                        }
                    }
                } else {
                    // 原来的处理方式
                    Object rendered = renderForValue(entry.getSpelTemp(), item);
                    // 如果渲染结果是JSON字符串，尝试解析它
                    if (rendered instanceof String renderedStr && renderedStr.trim().startsWith("{")) {
                        try {
                            JSONObject jsonObj = JSON.parseObject(renderedStr);
                            resultList.add(jsonObj);
                        } catch (Exception e) {
                            resultList.add(rendered);
                        }
                    } else {
                        resultList.add(rendered);
                    }
                }
            }
            
            // 特殊处理content节点
            if ("content".equals(fieldName)) {
                // 获取现有的content数组（如果存在）
                Object existingContent = targetObj.get("content");
                if (existingContent instanceof List<?>) {
                    // 合并现有的内容和新的内容
                    List<Object> mergedList = new ArrayList<>((List<?>) existingContent);
                    mergedList.addAll(resultList);
                    targetObj.put("content", mergedList);
                } else {
                    targetObj.put("content", resultList);
                }
            } else {
                targetObj.put(fieldName, resultList);
            }
        } else {
            // 如果不是列表，作为单个元素处理
            Object rendered = renderForValue(entry.getSpelTemp(), value);
            List<Object> resultList = new ArrayList<>();
            resultList.add(rendered);
            
            // 特殊处理content节点
            if ("content".equals(fieldName)) {
                // 获取现有的content数组（如果存在）
                Object existingContent = targetObj.get("content");
                if (existingContent instanceof List<?>) {
                    // 合并现有的内容和新的内容
                    List<Object> mergedList = new ArrayList<>((List<?>) existingContent);
                    mergedList.addAll(resultList);
                    targetObj.put("content", mergedList);
                } else {
                    targetObj.put("content", resultList);
                }
            } else {
                targetObj.put(fieldName, resultList);
            }
        }
    }
    
    private static void handleMapCategory(Map<String, Object> targetObj, MappingItem entry, Object value, String fieldName) {
        // map类型直接将值放入目标位置
        if (value instanceof Map) {
            targetObj.put(fieldName, value);
        } else {
            // 如果不是map类型，直接放置值
            targetObj.put(fieldName, value);
        }
    }
    
    private static Map<String, Object> getOrCreateChildMap(Map<String, Object> parent, ConfigParser.Token token) {
        if (token.hasIndex) {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) parent.computeIfAbsent(token.name, k -> new ArrayList<>());
            if (token.wildcard) {
                list.add(new LinkedHashMap<String, Object>());
            } else {
                while (list.size() <= token.index) {
                    list.add(new LinkedHashMap<String, Object>());
                }
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> child = (Map<String, Object>) list.get(token.wildcard ? list.size() - 1 : token.index);
            return child;
        } else {
            Object existing = parent.get(token.name);
            if (existing instanceof List<?> existingList) {
                // 当上一次写入的是 list，但本次需要进入其元素，取第一个元素的 map；没有则创建
                List<Object> list = (List<Object>) existingList;
                if (list.isEmpty() || !(list.get(0) instanceof Map)) {
                    if (list.isEmpty()) {
                        list.add(new LinkedHashMap<String, Object>());
                    } else {
                        list.set(0, new LinkedHashMap<String, Object>());
                    }
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> child = (Map<String, Object>) list.get(0);
                return child;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> child = (Map<String, Object>) parent.computeIfAbsent(token.name, k -> new LinkedHashMap<String, Object>());
            return child;
        }
    }

    /**
     * 按 node 路径从已有 payload 中提取值（用于前端已传完整层级时的反查）
     * 遇到 List 时取首个元素继续下钻。
     */
    @SuppressWarnings("unchecked")
    private static Object getValueByPath(Map<String, Object> payload, String path) {
        if (payload == null || path == null || path.isEmpty()) {
            return null;
        }
        String[] parts = path.split("\\.");
        Object current = payload;
        for (String part : parts) {
            if (current == null) {
                return null;
            }
            if (current instanceof Map<?, ?> map) {
                current = map.get(part);
            } else if (current instanceof List<?> list) {
                if (list.isEmpty()) {
                    return null;
                }
                current = list.get(0);
                // 重新处理同一个 part 针对 list 元素（因为 list 元素应该是 map）
                if (current instanceof Map<?, ?> innerMap) {
                    current = ((Map<String, Object>) innerMap).get(part);
                } else {
                    return current;
                }
            } else {
                return null;
            }
        }
        return current;
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
    
    private static String renderWithTemplate(String spelTemp, Object value) {
        if (spelTemp == null || spelTemp.isEmpty()) {
            return value != null ? value.toString() : "";
        }
        
        // 检查是否是嵌套paramItem的JSON模板
        if (spelTemp.trim().startsWith("{") && spelTemp.contains("\"paramItem\"")) {
            try {
                // 解析嵌套的paramItem配置
                JSONObject templateJson = JSON.parseObject(spelTemp);
                if (templateJson.containsKey("paramItem")) {
                    // 处理嵌套的paramItem配置（在 renderForValue 中无法获取父级 node，使用 null）
                    return processNestedParamItem(templateJson, value, null);
                }
            } catch (Exception e) {
                // 如果解析失败，回退到简单字符串替换
                return spelTemp.replace("{value}", value != null ? value.toString() : "");
            }
        }
        
        // 简单字符串替换
        if (value instanceof Map<?, ?> valueMap) {
            String result = spelTemp;
            // 替换 {xxx} 形式的占位符
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\{([^}]+)\\}");
            java.util.regex.Matcher matcher = pattern.matcher(spelTemp);
            while (matcher.find()) {
                String placeholder = matcher.group(0);
                String key = matcher.group(1);
                Object val = valueMap.get(key);
                if (val != null) {
                    result = result.replace(placeholder, val.toString());
                }
            }
            return result;
        }
        
        return spelTemp.replace("{value}", value != null ? value.toString() : "");
    }
    
    private static String processNestedParamItem(JSONObject templateJson, Object value, String parentNode) {
        // 获取嵌套的paramItem配置
        Object paramItemObj = templateJson.get("paramItem");
        if (paramItemObj instanceof JSONArray paramItems) {
            // 创建一个临时的Map来存储结果
            Map<String, Object> resultMap = new LinkedHashMap<>();
            
            // 遍历每个paramItem配置
            for (int i = 0; i < paramItems.size(); i++) {
                JSONObject paramItem = paramItems.getJSONObject(i);
                String key = paramItem.getString("key");
                String node = paramItem.getString("node");
                String postParam = paramItem.getString("post_param");
                String defaultValue = paramItem.getString("default_value");
                
                // spel_temp 中的 node 是相对路径（相对于父级 node）
                // 需要将父级 node（如 "content"）与相对路径（如 "type"）组合成完整路径（如 "content.type"）
                // 从传入的value中获取对应的值
                Object itemValue = null;
                if (value instanceof Map<?, ?> valueMap) {
                    itemValue = valueMap.get(key);
                }
                
                // 如果没有找到值，使用默认值
                if (itemValue == null) {
                    itemValue = defaultValue;
                }
                
                // 根据node路径设置值
                if (node != null && itemValue != null) {
                    setValueByPath(resultMap, node, itemValue);
                } else if (postParam != null && itemValue != null) {
                    resultMap.put(postParam, itemValue);
                }
            }
            
            // 返回JSON字符串
            return JSON.toJSONString(resultMap);
        }
        
        return value != null ? value.toString() : "";
    }
    
    private static Object renderForValue(String spelTemp, Object value) {
        if (value == null) {
            return null;
        }
        return renderWithTemplate(spelTemp, value);
    }

    /**
     * 支持含 [] 占位的路径写值，例如 dynamic_masks[].trajectories[].x
     */
    @SuppressWarnings("unchecked")
    private static void setValueByPath(Map<String, Object> root, String path, Object value) {
        String[] parts = path.split("\\.");
        Map<String, Object> current = root;
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            ConfigParser.Token token = ConfigParser.Token.parse(part);
            boolean isLast = i == parts.length - 1;
            if (isLast) {
                if (token.hasIndex) {
                    List<Object> list = (List<Object>) current.computeIfAbsent(token.name, k -> new ArrayList<>());
                    if (token.wildcard) {
                        list.add(value);
                    } else {
                        while (list.size() <= token.index) {
                            list.add(null);
                        }
                        list.set(token.index, value);
                    }
                } else {
                    current.put(token.name, value);
                }
            } else {
                if (token.hasIndex) {
                    List<Object> list = (List<Object>) current.computeIfAbsent(token.name, k -> new ArrayList<>());
                    if (token.wildcard) {
                        list.add(new LinkedHashMap<String, Object>());
                        current = (Map<String, Object>) list.get(list.size() - 1);
                    } else {
                        while (list.size() <= token.index) {
                            list.add(new LinkedHashMap<String, Object>());
                        }
                        current = (Map<String, Object>) list.get(token.index);
                    }
                } else {
                    current = (Map<String, Object>) current.computeIfAbsent(token.name, k -> new LinkedHashMap<String, Object>());
                }
            }
        }
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
    
    private static Map<String, Object> mergePayload(PostDslConfig config, Map<String, Object> payload,
                                                    List<MappingItem> entries) {
        Map<String, Object> merged = new LinkedHashMap<>(ConfigParser.parseBaseInfo(config.baseInfo()));
        if (payload != null) {
            merged.putAll(payload);
        }
        if (entries != null) {
            for (MappingItem entry : entries) {
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
