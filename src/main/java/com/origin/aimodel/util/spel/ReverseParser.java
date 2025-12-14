package com.origin.aimodel.util.spel;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
            return baseItems;
        }
        
        JSONArray mappingItems = mappingConfig.getJSONArray("paramItem");
        if (mappingItems == null || mappingItems.isEmpty()) {
            return baseItems;
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
        
        return resultItems;
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
            return baseItems;
        }
        
        JSONArray mappingItems = mappingConfig.getJSONArray("headerItem");
        if (mappingItems == null || mappingItems.isEmpty()) {
            return baseItems;
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
                resultItems.add(resultItem);
            } else {
                // 如果没有找到匹配项，创建一个新项
                MappingItem resultItem = new MappingItem();
                resultItem.key = key;
                resultItem.category = "key";
                resultItem.node = nodePath != null ? nodePath : key;
                resultItem.postParam = nodePath != null ? nodePath : key;
                resultItem.valueObject = "string";
        
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
        
        return resultItems;
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
}
