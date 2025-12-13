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
public class ReverseParser {
    
    private ReverseParser() {
    }
    
    /**
     * 反向解析：从实际请求 body + 映射关系生成 paramItem 配置
     * 
     * @param requestBody 实际请求的 JSON body
     * @param mappingConfig 映射关系配置，包含 key 和 post_param（目标字段名）或 node（完整路径）
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
     * @param mappingConfig 映射关系配置，包含 key 和 node（header 字段名，如 Content-Type）
     * @return 生成的 headerItem 配置 JSON 字符串
     */
    public static String getHeaderItem(Map<String, String> requestHeaders, JSONObject mappingConfig) {
        JSONObject result = getHeaderItemObject(requestHeaders, mappingConfig);
        return result.toJSONString();
    }
    
    /**
     * 反向解析：从实际请求 body + 映射关系生成 paramItem 配置对象
     * 
     * 映射配置支持两种模式：
     * 1. 自动发现模式：只提供 key 和 post_param（目标字段名），工具会自动在请求 body 中搜索匹配的 node 路径
     * 2. 手动指定模式：提供 key 和 node（完整路径），直接使用指定的路径
     * 3. 全自动模式：如果 paramMapping 为空或为空数组，自动从 requestBody 生成所有字段的映射
     */
    public static JSONObject getParamItemObject(JSONObject requestBody, JSONObject mappingConfig) {
        JSONArray resultParamItems = new JSONArray();
        JSONArray mappingItems = mappingConfig.getJSONArray("paramItem");
        
        // 将 requestBody 转换为 Map 以便递归遍历
        Map<String, Object> bodyMap = requestBody.toJavaObject(Map.class);
        
        // 如果映射配置为空或null，自动生成所有 JSON 字段的映射（不包含特殊参数）
        if (mappingItems == null || mappingItems.isEmpty()) {
            // 自动生成映射：收集所有字段路径
            Map<String, String> fieldPaths = collectAllFieldPaths(bodyMap, "");
            
            // 为每个字段生成映射配置
            for (Map.Entry<String, String> entry : fieldPaths.entrySet()) {
                String key = entry.getKey();
                String nodePath = entry.getValue();
                Object value = extractValueByPath(bodyMap, nodePath);
                
                if (value != null) {
                    JSONObject paramItem = buildParamItemEntry(key, nodePath, value, null);
                    resultParamItems.add(paramItem);
                }
            }
            // 注意：不自动发现特殊参数（如 --ratio），需要用户在 Param Mapping 中手动指定
        } else {
            // 如果映射配置中没有 node，需要自动发现：先收集所有字段路径
            boolean needAutoDiscover = false;
            for (int i = 0; i < mappingItems.size(); i++) {
                JSONObject mapping = mappingItems.getJSONObject(i);
                if (mapping.getString("node") == null) {
                    needAutoDiscover = true;
                    break;
                }
            }
            
            // 收集所有字段路径（用于自动发现）
            Map<String, String> fieldPaths = null;
            if (needAutoDiscover) {
                fieldPaths = collectAllFieldPaths(bodyMap, "");
            }
            
            // 遍历映射关系配置
            for (int i = 0; i < mappingItems.size(); i++) {
                JSONObject mapping = mappingItems.getJSONObject(i);
                String key = mapping.getString("key");
                String nodePath = mapping.getString("node");
                String postParam = mapping.getString("post_param");
                
                // 如果没有指定 node，尝试自动发现
                if (nodePath == null && postParam != null && fieldPaths != null) {
                    // 在收集的路径中搜索匹配的字段名
                    nodePath = findMatchingPath(fieldPaths, postParam);
                }
                
                // 如果仍然没有 node，但用户指定了 key 和 postParam，允许生成配置
                // 这种情况适用于特殊参数（如 ratio），可能不在 requestBody 中，而是通过 spel_temp 拼接
                if (nodePath == null) {
                    // 如果用户只提供了 key 和 postParam，尝试使用 postParam 作为 node
                    // 或者使用一个默认的 node（如 content[0].text，用于模板拼接）
                    if (postParam != null && !postParam.isEmpty()) {
                        // 尝试在 text 字段中查找，用于模板拼接场景
                        String textNodePath = findMatchingPath(fieldPaths, "text");
                        if (textNodePath != null) {
                            nodePath = textNodePath;
                        } else {
                            // 如果还是找不到，使用 postParam 作为 node（可能是一个新字段）
                            nodePath = postParam;
                        }
                    } else {
                        // 如果连 postParam 都没有，跳过
                        continue;
                    }
                }
                
                // 从 requestBody 中提取值（可能为 null，对于特殊参数）
                Object value = extractValueByPath(bodyMap, nodePath);
                
                // 即使 value 为 null，也生成配置（用于特殊参数，通过 spel_temp 处理）
                // 从映射配置中获取 validate 字段（如果存在）
                Object validate = mapping.get("validate");
                JSONObject paramItem = buildParamItemEntry(key, nodePath, value != null ? value : "", validate);
                resultParamItems.add(paramItem);
            }
            // 注意：不自动发现特殊参数，只根据用户指定的映射关系生成配置
        }
        
        JSONObject result = new JSONObject();
        result.put("paramItem", resultParamItems);
        return result;
    }
    
    /**
     * 递归收集所有字段路径（公共方法，供外部调用）
     * 例如：{"model": "xxx", "content": [{"type": "text"}]} 
     * 返回：{"model": "model", "type": "content[0].type", "text": "content[0].text"}
     */
    public static Map<String, String> collectAllFieldPathsForMapping(Object obj, String prefix) {
        return collectAllFieldPaths(obj, prefix);
    }
    
    /**
     * 递归收集所有字段路径
     * 例如：{"model": "xxx", "content": [{"type": "text"}]} 
     * 返回：{"model": "model", "type": "content[0].type", "text": "content[0].text"}
     */
    private static Map<String, String> collectAllFieldPaths(Object obj, String prefix) {
        Map<String, String> paths = new java.util.HashMap<>();
        
        if (obj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) obj;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = entry.getKey().toString();
                Object value = entry.getValue();
                String currentPath = prefix.isEmpty() ? key : prefix + "." + key;
                
                if (value instanceof List) {
                    List<?> list = (List<?>) value;
                    for (int i = 0; i < list.size(); i++) {
                        Object item = list.get(i);
                        String arrayPath = prefix.isEmpty() ? key + "[" + i + "]" : prefix + "." + key + "[" + i + "]";
                        if (item instanceof Map) {
                            // 递归处理数组中的对象
                            paths.putAll(collectAllFieldPaths(item, arrayPath));
                        } else if (item instanceof List) {
                            paths.putAll(collectAllFieldPaths(item, arrayPath));
                        } else {
                            // 对于数组中的简单值，使用字段名作为 key
                            paths.put(key, arrayPath);
                        }
                    }
                } else if (value instanceof Map) {
                    paths.putAll(collectAllFieldPaths(value, currentPath));
                } else {
                    // 简单值：使用字段名作为 key，完整路径作为 value
                    paths.put(key, currentPath);
                }
            }
        } else if (obj instanceof List) {
            List<?> list = (List<?>) obj;
            for (int i = 0; i < list.size(); i++) {
                Object item = list.get(i);
                String arrayPath = prefix.isEmpty() ? "[" + i + "]" : prefix + "[" + i + "]";
                if (item instanceof Map || item instanceof List) {
                    paths.putAll(collectAllFieldPaths(item, arrayPath));
                }
            }
        }
        
        return paths;
    }
    
    /**
     * 在收集的路径中查找匹配的字段名
     * 例如：postParam = "type"，查找所有以 "type" 结尾的路径
     */
    private static String findMatchingPath(Map<String, String> fieldPaths, String postParam) {
        // 优先精确匹配字段名（Map 的 key）
        String exactMatch = fieldPaths.get(postParam);
        if (exactMatch != null) {
            return exactMatch;
        }
        
        // 如果精确匹配失败，查找路径末尾匹配的
        // 例如：postParam = "type"，匹配 "content[0].type"
        for (Map.Entry<String, String> entry : fieldPaths.entrySet()) {
            String path = entry.getValue();
            // 检查路径是否以 .postParam 结尾，或者就是 postParam
            if (path.equals(postParam) || path.endsWith("." + postParam)) {
                return path;
            }
            // 检查数组路径，如 content[0].type
            if (path.matches(".*\\[\\d+\\]\\." + postParam + "$")) {
                return path;
            }
        }
        
        return null;
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
            // 对于 header，node 就是 header 字段名（如 Content-Type）
            String nodePath = mapping.getString("node");
            if (nodePath == null) {
                // 兼容旧版本：如果没有 node，尝试使用 post_param
                nodePath = mapping.getString("post_param");
            }
            
            // 从 requestHeaders 中提取值（支持大小写不敏感）
            String value = null;
            if (requestHeaders != null) {
                value = requestHeaders.get(nodePath);
                if (value == null) {
                    // 尝试小写匹配
                    for (Map.Entry<String, String> entry : requestHeaders.entrySet()) {
                        if (entry.getKey().equalsIgnoreCase(nodePath)) {
                            value = entry.getValue();
                            break;
                        }
                    }
                }
            }
            
            if (value != null) {
                JSONObject headerItem = buildHeaderItemEntry(key, nodePath, value);
                resultHeaderItems.add(headerItem);
            }
        }
        
        JSONObject result = new JSONObject();
        result.put("headerItem", resultHeaderItems);
        return result;
    }
    
    /**
     * 构建 paramItem 配置项
     * @param key 业务字段名
     * @param nodePath node 路径（如 "content[0].type"）
     * @param value 实际值
     * @param validate validate 配置（可以是 JSONObject 或 JSON 字符串，从映射配置中传入）
     */
    private static JSONObject buildParamItemEntry(String key, String nodePath, Object value, Object validate) {
        JSONObject paramItem = new JSONObject();
        paramItem.put("key", key);
        
        // 推断 category
        String category = inferCategory(value, nodePath);
        paramItem.put("category", category);
        
        // 设置 node（目标路径）
        paramItem.put("node", nodePath);
        
        // 设置 post_param（如果与 node 不同）
        String postParamField = extractPostParamField(nodePath);
        if (!postParamField.equals(key)) {
            paramItem.put("post_param", postParamField);
        }
        
        // 推断 value_object
        String valueObject = inferValueObject(value);
        paramItem.put("value_object", valueObject);
        
        // 如果有 validate 配置，添加到 paramItem 中
        if (validate != null) {
            if (validate instanceof String) {
                // 如果是字符串，直接使用
                paramItem.put("validate", validate);
            } else if (validate instanceof JSONObject) {
                // 如果是 JSONObject，转换为字符串
                paramItem.put("validate", ((JSONObject) validate).toJSONString());
            } else {
                // 其他类型，转为字符串
                paramItem.put("validate", validate.toString());
            }
        }
        
        // 识别模板（如果文本中包含模式）
        // 只有当用户明确指定了映射关系（key 和 node）时，才检测模板模式
        // 例如：key=ratio, node=content[0].text，如果 text 中包含 "--ratio 16:9"，则生成 spel_temp
        if (value instanceof String && nodePath.contains("text")) {
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

