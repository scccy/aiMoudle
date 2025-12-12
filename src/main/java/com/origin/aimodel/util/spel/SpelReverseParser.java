package com.origin.aimodel.util.spel;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 反向解析工具：从实际请求的 header/body + 映射关系生成 headerItem 和 paramItem 配置。
 */
public class SpelReverseParser {
    
    private SpelReverseParser() {
    }
    
    /**
     * 反向解析：从实际请求 body + 映射关系生成 paramItem 配置
     * 
     * @param requestBody 实际请求的 JSON body
     * @param mappingConfig 映射关系配置，包含 key 和 post_param（实际请求中的字段路径）
     * @return 生成的 paramItem 配置 JSON 字符串
     */
    public static String getParamItem(JSONObject requestBody, JSONObject mappingConfig) {
        JSONObject result = getParamItemObject(requestBody, mappingConfig);
        return result.toJSONString();
    }
    
    /**
     * 反向解析：从实际请求 header + 映射关系生成 headerItem 配置
     * 
     * @param requestHeaders 实际请求的 header Map
     * @param mappingConfig 映射关系配置，包含 key 和 post_param（header 字段名）
     * @return 生成的 headerItem 配置 JSON 字符串
     */
    public static String getHeaderItem(Map<String, String> requestHeaders, JSONObject mappingConfig) {
        JSONObject result = getHeaderItemObject(requestHeaders, mappingConfig);
        return result.toJSONString();
    }
    
    /**
     * 反向解析：从实际请求 body + 映射关系生成 paramItem 配置对象
     */
    public static JSONObject getParamItemObject(JSONObject requestBody, JSONObject mappingConfig) {
        JSONArray resultParamItems = new JSONArray();
        JSONArray mappingItems = mappingConfig.getJSONArray("paramItem");
        
        if (mappingItems == null) {
            JSONObject result = new JSONObject();
            result.put("paramItem", resultParamItems);
            return result;
        }
        
        // 将 requestBody 转换为 Map 以便递归遍历
        Map<String, Object> bodyMap = requestBody.toJavaObject(Map.class);
        
        // 遍历映射关系配置
        for (int i = 0; i < mappingItems.size(); i++) {
            JSONObject mapping = mappingItems.getJSONObject(i);
            String key = mapping.getString("key");
            String postParam = mapping.getString("post_param");
            
            // 从 requestBody 中提取值
            Object value = extractValueByPath(bodyMap, postParam);
            
            if (value != null) {
                JSONObject paramItem = buildParamItemEntry(key, postParam, value);
                resultParamItems.add(paramItem);
            }
        }
        
        JSONObject result = new JSONObject();
        result.put("paramItem", resultParamItems);
        return result;
    }
    
    /**
     * 反向解析：从实际请求 header + 映射关系生成 headerItem 配置对象
     */
    public static JSONObject getHeaderItemObject(Map<String, String> requestHeaders, JSONObject mappingConfig) {
        JSONArray resultHeaderItems = new JSONArray();
        JSONArray mappingItems = mappingConfig.getJSONArray("headerItem");
        
        if (mappingItems == null) {
            JSONObject result = new JSONObject();
            result.put("headerItem", resultHeaderItems);
            return result;
        }
        
        // 遍历映射关系配置
        for (int i = 0; i < mappingItems.size(); i++) {
            JSONObject mapping = mappingItems.getJSONObject(i);
            String key = mapping.getString("key");
            String postParam = mapping.getString("post_param");
            
            // 从 requestHeaders 中提取值（支持大小写不敏感）
            String value = null;
            if (requestHeaders != null) {
                value = requestHeaders.get(postParam);
                if (value == null) {
                    // 尝试小写匹配
                    for (Map.Entry<String, String> entry : requestHeaders.entrySet()) {
                        if (entry.getKey().equalsIgnoreCase(postParam)) {
                            value = entry.getValue();
                            break;
                        }
                    }
                }
            }
            
            if (value != null) {
                JSONObject headerItem = buildHeaderItemEntry(key, postParam, value);
                resultHeaderItems.add(headerItem);
            }
        }
        
        JSONObject result = new JSONObject();
        result.put("headerItem", resultHeaderItems);
        return result;
    }
    
    /**
     * 构建 paramItem 配置项
     */
    private static JSONObject buildParamItemEntry(String key, String postParam, Object value) {
        JSONObject paramItem = new JSONObject();
        paramItem.put("key", key);
        
        // 推断 category
        String category = inferCategory(value, postParam);
        paramItem.put("category", category);
        
        // 设置 node（目标路径）
        paramItem.put("node", postParam);
        
        // 设置 post_param（如果与 node 不同）
        String postParamField = extractPostParamField(postParam);
        if (!postParamField.equals(key)) {
            paramItem.put("post_param", postParamField);
        }
        
        // 推断 value_object
        String valueObject = inferValueObject(value);
        paramItem.put("value_object", valueObject);
        
        // 识别模板（如果文本中包含模式）
        if (value instanceof String && postParam.contains("text")) {
            String textValue = (String) value;
            String template = detectTemplate(textValue, key);
            if (template != null) {
                paramItem.put("spel_temp", template);
            }
        }
        
        // 识别默认值（如果值与常见默认值匹配）
        if (value instanceof String && "text".equals(value)) {
            paramItem.put("default_value", "text");
        }
        
        return paramItem;
    }
    
    /**
     * 构建 headerItem 配置项
     */
    private static JSONObject buildHeaderItemEntry(String key, String postParam, String value) {
        JSONObject headerItem = new JSONObject();
        headerItem.put("key", key);
        headerItem.put("category", "key");
        headerItem.put("node", postParam);
        headerItem.put("post_param", postParam);
        headerItem.put("value_object", "string");
        
        // 如果值是常见的默认值，设置 default_value
        if ("application/json".equals(value)) {
            headerItem.put("default_value", "application/json");
        } else if (value != null && value.startsWith("Bearer ")) {
            headerItem.put("default_value", "Bearer ${TOKEN}");
        }
        
        return headerItem;
    }
    
    /**
     * 根据路径从 Map 中提取值
     * 支持简单路径如 "model" 和复杂路径如 "content[0].text"
     */
    private static Object extractValueByPath(Map<String, Object> map, String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        
        Object current = map;
        
        // 处理数组索引路径，如 "content[0].text"
        if (path.contains("[") && path.contains("]")) {
            Pattern pattern = Pattern.compile("(\\w+)\\[(\\d+)\\](\\.(.+))?");
            Matcher matcher = pattern.matcher(path);
            
            if (matcher.find()) {
                String arrayName = matcher.group(1);
                int index = Integer.parseInt(matcher.group(2));
                String remainingPath = matcher.group(4); // 去掉开头的点号
                
                // 获取数组
                Object arrayObj = ((Map<?, ?>) current).get(arrayName);
                if (arrayObj instanceof List) {
                    List<?> list = (List<?>) arrayObj;
                    if (index < list.size()) {
                        current = list.get(index);
                        
                        // 处理剩余路径
                        if (remainingPath != null && !remainingPath.isEmpty()) {
                            if (current instanceof Map) {
                                return extractValueByPath((Map<String, Object>) current, remainingPath);
                            }
                        }
                        return current;
                    }
                }
            }
            return null;
        }
        
        // 处理点号路径，如 "camera_control.type"
        if (path.contains(".")) {
            String[] parts = path.split("\\.");
            for (String part : parts) {
                if (current instanceof Map) {
                    current = ((Map<?, ?>) current).get(part);
                    if (current == null) {
                        return null;
                    }
                } else {
                    return null;
                }
            }
            return current;
        }
        
        // 简单路径
        if (current instanceof Map) {
            return ((Map<?, ?>) current).get(path);
        }
        return null;
    }
    
    /**
     * 从路径中提取 post_param 字段名
     * 如 "content[0].text" -> "text"
     */
    private static String extractPostParamField(String path) {
        if (path.contains(".")) {
            return path.substring(path.lastIndexOf(".") + 1);
        }
        if (path.contains("[")) {
            int bracketEnd = path.indexOf("]");
            if (bracketEnd > 0 && bracketEnd < path.length() - 1 && path.charAt(bracketEnd + 1) == '.') {
                return path.substring(bracketEnd + 2); // 跳过 "]."
            }
        }
        return path;
    }
    
    /**
     * 推断 category 类型
     */
    private static String inferCategory(Object value, String path) {
        if (value instanceof List) {
            return "list";
        }
        if (value instanceof Map) {
            return "map";
        }
        return "key";
    }
    
    /**
     * 推断 value_object 类型
     */
    private static String inferValueObject(Object value) {
        if (value instanceof String) {
            return "string";
        }
        if (value instanceof Integer) {
            return "int";
        }
        if (value instanceof Double || value instanceof Float) {
            return "double";
        }
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            if (!list.isEmpty() && list.get(0) instanceof Map) {
                return "list<json>";
            }
            return "list<string>";
        }
        if (value instanceof Map) {
            return "map";
        }
        return "string";
    }
    
    /**
     * 检测文本中的模板模式
     * 如 "text --ratio 16:9" -> " --ratio {value}"
     */
    private static String detectTemplate(String text, String key) {
        if (text == null || key == null) {
            return null;
        }
        
        // 检测 ratio 模式
        if ("ratio".equals(key)) {
            Pattern pattern = Pattern.compile(" --ratio (\\S+)");
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return " --ratio {value}";
            }
        }
        
        // 检测 resolution 模式
        if ("resolution".equals(key)) {
            Pattern pattern = Pattern.compile(" --resolution (\\S+)");
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return " --resolution {value}";
            }
        }
        
        return null;
    }
}

