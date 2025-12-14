package com.origin.aimodel.util.spel;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.HashSet;

/**
 * 反向解析工具：第二层（业务逻辑层）
 * 从实际请求的 header/body + 映射关系生成 headerItem 和 paramItem 配置。
 * 全部基于 ReverseDslFactory 实现。
 */
public class ReverseParser {
    
    private ReverseParser() {
    }
    
    /**
     * 反向解析：从实际请求 body + 映射关系生成 paramItem 配置
     * 
     * @param bodyMap 请求体 Map
     * @param mappingConfig 映射关系配置，包含 key 和 post_param（目标字段名）或 node（完整路径）
     * @return 生成的映射项列表
     */
    public static List<MappingItem> parseParamItems(Map<String, Object> bodyMap, JSONObject mappingConfig) {
        // 第一步：使用 ReverseDslFactory 生成基础映射
        List<MappingItem> baseItems = ReverseDslFactory.generateBasicParamMapping(bodyMap);
        
        // 如果没有自定义映射配置，直接返回基础映射
        if (mappingConfig == null) {
            return flattenMappingItems(baseItems);
        }
        
        JSONArray mappingItems = mappingConfig.getJSONArray("paramItem");
        if (mappingItems == null || mappingItems.isEmpty()) {
            return flattenMappingItems(baseItems);
        }
        
        // 第二步：根据自定义映射配置，从基础映射中筛选或合并
        List<MappingItem> resultItems = new ArrayList<>();
        
        for (int i = 0; i < mappingItems.size(); i++) {
            JSONObject mapping = mappingItems.getJSONObject(i);
            String key = mapping.getString("key");
            String nodePath = mapping.getString("node");
            String postParam = mapping.getString("post_param");
            Object validate = mapping.get("validate");
            
            // 查找基础映射中匹配的项
            MappingItem matchedItem = findMatchingItem(baseItems, key, nodePath, postParam);
            
            if (matchedItem != null) {
                // 如果找到匹配项，使用基础映射的值，但应用自定义配置
                MappingItem resultItem = new MappingItem();
                resultItem.key = key;
                resultItem.category = matchedItem.category;
                resultItem.node = nodePath != null ? nodePath : matchedItem.node;
                resultItem.postParam = postParam != null ? postParam : matchedItem.postParam;
                resultItem.valueObject = matchedItem.valueObject;
                resultItem.spelTemp = matchedItem.spelTemp;
                resultItem.defaultValue = matchedItem.defaultValue;
                resultItem.valueSource = matchedItem.valueSource;
                
                // 应用自定义 validate
                if (validate != null) {
                    if (validate instanceof String) {
                        resultItem.validate = (String) validate;
                    } else if (validate instanceof JSONObject) {
                        resultItem.validate = ((JSONObject) validate).toJSONString();
                        } else {
                        resultItem.validate = validate.toString();
                    }
                } else {
                    resultItem.validate = matchedItem.validate;
                }
                
                resultItems.add(resultItem);
            } else {
                // 如果没有找到匹配项，创建一个新项（用于特殊参数，可能不在 requestBody 中）
                MappingItem resultItem = new MappingItem();
                resultItem.key = key;
                resultItem.category = "key";
                resultItem.node = nodePath != null ? nodePath : (postParam != null ? postParam : key);
                resultItem.postParam = postParam != null ? postParam : key;
                resultItem.valueObject = "string";
                resultItem.valueSource = "USER";
                
                // 应用自定义 validate
                if (validate != null) {
                    if (validate instanceof String) {
                        resultItem.validate = (String) validate;
                    } else if (validate instanceof JSONObject) {
                        resultItem.validate = ((JSONObject) validate).toJSONString();
                    } else {
                        resultItem.validate = validate.toString();
                    }
                }
                
                resultItems.add(resultItem);
            }
        }
        
        return flattenMappingItems(resultItems);
    }
    
    /**
     * 反向解析：从实际请求 header + 映射关系生成 headerItem 配置
     * 
     * @param requestHeaders 请求头 Map
     * @param mappingConfig 映射关系配置，包含 key 和 node（header 字段名，如 Content-Type）
     * @return 生成的映射项列表
     */
    public static List<MappingItem> parseHeaderItems(Map<String, String> requestHeaders, JSONObject mappingConfig) {
        // 第一步：使用 ReverseDslFactory 生成基础映射
        List<MappingItem> baseItems = ReverseDslFactory.generateBasicHeaderMapping(requestHeaders);
        
        // 如果没有自定义映射配置，直接返回基础映射
        if (mappingConfig == null) {
            return flattenMappingItems(baseItems);
        }
        
        JSONArray mappingItems = mappingConfig.getJSONArray("headerItem");
        if (mappingItems == null || mappingItems.isEmpty()) {
            return flattenMappingItems(baseItems);
        }
        
        // 第二步：根据自定义映射配置，从基础映射中筛选或合并
        List<MappingItem> resultItems = new ArrayList<>();
        
        for (int i = 0; i < mappingItems.size(); i++) {
            JSONObject mapping = mappingItems.getJSONObject(i);
            String key = mapping.getString("key");
            String nodePath = mapping.getString("node");
            if (nodePath == null) {
                nodePath = mapping.getString("post_param");
            }
            
            // 查找基础映射中匹配的项（通过 node 匹配）
            MappingItem matchedItem = null;
            if (nodePath != null) {
                for (MappingItem item : baseItems) {
                    if (nodePath.equalsIgnoreCase(item.node)) {
                        matchedItem = item;
                            break;
                        }
                }
            }
            
            if (matchedItem != null) {
                // 如果找到匹配项，使用基础映射的值，但应用自定义配置
                MappingItem resultItem = new MappingItem();
                resultItem.key = key;
                resultItem.category = matchedItem.category;
                resultItem.node = nodePath != null ? nodePath : matchedItem.node;
                resultItem.postParam = nodePath != null ? nodePath : matchedItem.postParam;
                resultItem.valueObject = matchedItem.valueObject;
                resultItem.defaultValue = matchedItem.defaultValue;
                resultItem.valueSource = matchedItem.valueSource;
                resultItems.add(resultItem);
            } else {
                // 如果没有找到匹配项，创建一个新项
                MappingItem resultItem = new MappingItem();
                resultItem.key = key;
                resultItem.category = "key";
                resultItem.node = nodePath != null ? nodePath : key;
                resultItem.postParam = nodePath != null ? nodePath : key;
                resultItem.valueObject = "string";
                resultItem.valueSource = "USER";
        
                // 从请求头中获取值作为默认值
                if (requestHeaders != null && nodePath != null) {
                    String headerValue = requestHeaders.get(nodePath);
                    if (headerValue == null) {
                        // 尝试大小写不敏感匹配
                        for (Map.Entry<String, String> entry : requestHeaders.entrySet()) {
                            if (entry.getKey().equalsIgnoreCase(nodePath)) {
                                headerValue = entry.getValue();
                                break;
                            }
                        }
                    }
                    if (headerValue != null) {
                        resultItem.defaultValue = headerValue;
                    }
                }
                
                resultItems.add(resultItem);
                    }
        }
        
        return flattenMappingItems(resultItems);
    }
    
    /**
     * 在基础映射中查找匹配的项
     */
    private static MappingItem findMatchingItem(List<MappingItem> baseItems, String key, String nodePath, String postParam) {
        // 优先通过 key 匹配
        for (MappingItem item : baseItems) {
            if (key != null && key.equals(item.key)) {
                return item;
        }
    }
    
        // 其次通过 node 匹配
        if (nodePath != null) {
            for (MappingItem item : baseItems) {
                if (nodePath.equals(item.node)) {
                    return item;
        }
            }
    }
    
        // 最后通过 postParam 匹配
        if (postParam != null) {
            for (MappingItem item : baseItems) {
                if (postParam.equals(item.postParam)) {
                    return item;
                }
            }
        }
        
        return null;
    }

    /**
     * 扁平化 MappingItem 列表，展开 spel_temp 中的嵌套字段，node 使用全路径
     */
    private static List<MappingItem> flattenMappingItems(List<MappingItem> items) {
        if (items == null || items.isEmpty()) {
            return new ArrayList<>();
        }
        Map<String, MappingItem> dedup = new LinkedHashMap<>();
        for (MappingItem item : items) {
            flattenMappingItem(item, item != null ? item.node : null, item != null ? item.category : null, dedup);
        }
        return new ArrayList<>(dedup.values());
    }

    private static void flattenMappingItem(MappingItem item, String parentNode, String parentCategory, Map<String, MappingItem> out) {
        if (item == null || item.key == null || item.key.isEmpty()) {
            return;
        }
        String dedupKey = buildDedupKey(item.key, item.node, item.postParam);
        if (out.containsKey(dedupKey)) {
            return;
        }
        MappingItem copy = new MappingItem();
        copy.key = item.key;
        copy.category = item.category;
        copy.node = item.node;
        copy.postParam = item.postParam;
        copy.spelTemp = item.spelTemp;
        copy.defaultValue = item.defaultValue;
        copy.valueObject = item.valueObject;
        copy.validate = item.validate;
        copy.valueSource = item.valueSource;
        out.put(dedupKey, copy);

        List<MappingItem> nestedItems = parseNestedMappingItems(item.spelTemp);
        if (!nestedItems.isEmpty()) {
            String parentPath = buildParentPath(parentNode, parentCategory);
            for (MappingItem nested : nestedItems) {
                String childNode = buildChildNode(parentPath, nested.node);
                MappingItem flattened = new MappingItem();
                flattened.key = nested.key;
                flattened.category = nested.category;
                flattened.node = childNode;
                flattened.postParam = nested.postParam;
                flattened.spelTemp = nested.spelTemp;
                flattened.defaultValue = nested.defaultValue;
                flattened.valueObject = nested.valueObject;
                flattened.validate = nested.validate;
                flattened.valueSource = nested.valueSource;
                flattenMappingItem(flattened, childNode, nested.category, out);
            }
        }
    }

    private static String buildParentPath(String parentNode, String parentCategory) {
        if (parentNode == null || parentNode.isEmpty()) {
            return "";
        }
        return parentNode;
    }

    private static String buildChildNode(String parentPath, String childNode) {
        if (parentPath == null || parentPath.isEmpty()) {
            return childNode != null ? childNode : "";
        }
        if (childNode == null || childNode.isEmpty()) {
            return parentPath;
        }
        if (childNode.startsWith(".")) {
            return parentPath + childNode;
        }
        return parentPath + "." + childNode;
    }

    /**
     * 解析 spel_temp 的嵌套 paramItem
     */
    private static List<MappingItem> parseNestedMappingItems(String spelTemp) {
        if (spelTemp == null || spelTemp.trim().isEmpty()) {
            return new ArrayList<>();
        }
        String normalized = spelTemp.trim();
        JSONObject spelJson = null;
        try {
            spelJson = JSONObject.parseObject(normalized);
        } catch (Exception parseErr) {
            try {
                String unescaped = normalized.replace("\\\"", "\"");
                spelJson = JSONObject.parseObject(unescaped);
            } catch (Exception retryErr) {
                return new ArrayList<>();
            }
        }

        JSONArray paramItems = spelJson.getJSONArray("paramItem");
        if (paramItems == null || paramItems.isEmpty()) {
            return new ArrayList<>();
        }
        List<MappingItem> nestedItems = new ArrayList<>();
        for (int i = 0; i < paramItems.size(); i++) {
            JSONObject obj = paramItems.getJSONObject(i);
            if (obj == null) {
                continue;
            }
            MappingItem nested = new MappingItem();
            nested.key = obj.getString("key");
            nested.category = obj.getString("category");
            nested.node = obj.getString("node");
            nested.postParam = obj.getString("post_param");
            nested.spelTemp = obj.getString("spel_temp");
            nested.defaultValue = obj.getString("default_value");
            nested.valueObject = obj.getString("value_object");
            nested.validate = obj.getString("validate");
            nested.valueSource = obj.getString("value_source");
            nestedItems.add(nested);
        }
        return nestedItems;
    }

    private static String buildDedupKey(String key, String node, String postParam) {
        StringBuilder sb = new StringBuilder();
        sb.append(key != null ? key : "");
        sb.append("|");
        sb.append(node != null ? node : "");
        sb.append("|");
        sb.append(postParam != null ? postParam : "");
        return sb.toString();
    }
}
