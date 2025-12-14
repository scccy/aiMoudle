package com.scccy.aimodel.util.dsl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.scccy.aimodel.domain.vo.MappingItem;
import com.scccy.aimodel.domain.vo.ReverseParseRequest;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 反向解析 DSL 工厂（精简版）
 * 仅依赖 node 表达结构，category 固定为 key，postParam 与 key 一致。
 * list 用 [] 占位，list 中的 map 用 map{1}/map{*} 占位。
 */
public class ReverseDslFactory {

    private ReverseDslFactory() {
    }

    /** 构建 param 映射配置（兼容旧接口）。 */
    static JSONObject buildParamMappingConfig(List<ReverseParseRequest.MappingConfig> mappings) {
        if (mappings == null || mappings.isEmpty()) {
            return null;
        }
        JSONObject config = new JSONObject();
        JSONArray mappingArray = new JSONArray();
        for (ReverseParseRequest.MappingConfig mapping : mappings) {
            JSONObject mappingObj = new JSONObject();
            mappingObj.put("key", mapping.getKey());
            if (StringUtils.hasText(mapping.getNode())) {
                mappingObj.put("node", mapping.getNode());
            }
            if (StringUtils.hasText(mapping.getPostParam())) {
                mappingObj.put("post_param", mapping.getPostParam());
            }
            mappingArray.add(mappingObj);
        }
        config.put("paramItem", mappingArray);
        return config;
    }

    /** 构建 header 映射配置（兼容旧接口）。 */
    static JSONObject buildHeaderMappingConfig(List<ReverseParseRequest.MappingConfig> mappings) {
        if (mappings == null || mappings.isEmpty()) {
            return null;
        }
        JSONObject config = new JSONObject();
        JSONArray mappingArray = new JSONArray();
        for (ReverseParseRequest.MappingConfig mapping : mappings) {
            JSONObject mappingObj = new JSONObject();
            mappingObj.put("key", mapping.getKey());
            if (StringUtils.hasText(mapping.getNode())) {
                mappingObj.put("node", mapping.getNode());
            }
            if (StringUtils.hasText(mapping.getPostParam())) {
                mappingObj.put("post_param", mapping.getPostParam());
            }
            mappingArray.add(mappingObj);
        }
        config.put("headerItem", mappingArray);
        return config;
    }

    /** 反向生成基础 param Mapping：仅基于 node 路径。 */
    public static List<MappingItem> generateBasicParamMapping(Map<String, Object> bodyMap) {
        if (bodyMap == null || bodyMap.isEmpty()) {
            return new ArrayList<>();
        }
        Map<String, MappingItem> dedup = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : bodyMap.entrySet()) {
            collectMapping(entry.getKey(), entry.getValue(), dedup);
        }
        return new ArrayList<>(dedup.values());
    }

    /** 反向生成基础 header Mapping：仅 key/postParam/node。 */
    public static List<MappingItem> generateBasicHeaderMapping(Map<String, String> requestHeaders) {
        List<MappingItem> items = new ArrayList<>();
        if (requestHeaders == null || requestHeaders.isEmpty()) {
            return items;
        }
        for (Map.Entry<String, String> entry : requestHeaders.entrySet()) {
            String headerName = entry.getKey();
            if (!StringUtils.hasText(headerName)) {
                continue;
            }
            MappingItem item = new MappingItem();
            String key = headerName;
            item.setKey(key);
            item.setCategory("key");
            item.setNode(headerName);
            item.setPostParam(headerName);
            item.setValueObject("string");
            item.setValueSource("USER");
            if (StringUtils.hasText(entry.getValue())) {
                item.setDefaultValue(entry.getValue());
            }
            items.add(item);
        }
        return items;
    }

    /** 递归收集映射，list 用 []，list 中 map 用 map{1}/map{*}。 */
    private static void collectMapping(String path, Object value, Map<String, MappingItem> out) {
        if (value instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String childKey = entry.getKey().toString();
                Object childVal = entry.getValue();
                String childPath = path.isEmpty() ? childKey : path + "." + childKey;
                collectMapping(childPath, childVal, out);
            }
        } else if (value instanceof List<?> list) {
            String listPrefix = path + "[]";
            boolean hasMap = list.stream().anyMatch(e -> e instanceof Map);
            if (hasMap) {
                // 如果列表里的 map 都是单字段碎片，合并成一个结构
                if (allSingleFieldMaps(list)) {
                    Map<String, Object> merged = mergeSingleFieldMaps(list);
                    String mapPlaceholder = list.size() > 1 ? "map{*}" : "map{1}";
                    String elementPrefix = listPrefix + "." + mapPlaceholder;
                    collectMapping(elementPrefix, merged, out);
                } else {
                    Map<String, List<Map<?, ?>>> grouped = groupByStructure(list);
                    for (Map.Entry<String, List<Map<?, ?>>> entry : grouped.entrySet()) {
                        List<Map<?, ?>> maps = entry.getValue();
                        if (maps.isEmpty()) {
                            continue;
                        }
                        String mapPlaceholder = maps.size() > 1 ? "map{*}" : "map{1}";
                        String elementPrefix = listPrefix + "." + mapPlaceholder;
                        // 用该结构的一个样本下钻
                        collectMapping(elementPrefix, maps.get(0), out);
                    }
                }
            } else {
                String key = extractFieldNameFromPath(path);
                MappingItem item = createItem(key, listPrefix, list);
                putIfAbsent(out, item);
            }
        } else {
            String key = extractFieldNameFromPath(path);
            MappingItem item = createItem(key, path, value);
            putIfAbsent(out, item);
        }
    }

    /** 按结构签名分组列表里的 Map 元素。 */
    private static Map<String, List<Map<?, ?>>> groupByStructure(List<?> list) {
        Map<String, List<Map<?, ?>>> grouped = new LinkedHashMap<>();
        for (Object el : list) {
            if (el instanceof Map<?, ?> m) {
                String sig = generateStructureSignature(m);
                grouped.computeIfAbsent(sig, k -> new ArrayList<>()).add(m);
            }
        }
        return grouped;
    }

    private static boolean allSingleFieldMaps(List<?> list) {
        for (Object el : list) {
            if (el instanceof Map<?, ?> m) {
                if (m.size() != 1) {
                    return false;
                }
            }
        }
        return true;
    }

    private static Map<String, Object> mergeSingleFieldMaps(List<?> list) {
        Map<String, Object> merged = new LinkedHashMap<>();
        for (Object el : list) {
            if (el instanceof Map<?, ?> m) {
                for (Map.Entry<?, ?> entry : m.entrySet()) {
                    merged.put(entry.getKey().toString(), entry.getValue());
                }
            }
        }
        return merged;
    }

    private static String generateStructureSignature(Map<?, ?> map) {
        Set<String> keys = new HashSet<>();
        collectKeys(map, "", keys);
        List<String> sorted = new ArrayList<>(keys);
        java.util.Collections.sort(sorted);
        return String.join(",", sorted);
    }

    private static void collectKeys(Map<?, ?> map, String prefix, Set<String> keys) {
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String k = entry.getKey().toString();
            String current = prefix.isEmpty() ? k : prefix + "." + k;
            keys.add(current);
            Object v = entry.getValue();
            if (v instanceof Map<?, ?> nested) {
                collectKeys(nested, current, keys);
            } else if (v instanceof List<?> list) {
                for (Object el : list) {
                    if (el instanceof Map<?, ?> inner) {
                        collectKeys(inner, current + "[]", keys);
                    }
                }
            }
        }
    }

    private static MappingItem createItem(String key, String node, Object value) {
        MappingItem item = new MappingItem();
        item.setKey(key);
        item.setPostParam(key);
        item.setNode(node);
        item.setCategory("key");
        item.setValueSource("USER");
        item.setValueObject(inferValueObjectType(value));
        if (value != null && !(value instanceof Map) && !(value instanceof List)) {
            item.setDefaultValue(value.toString());
        }
        return item;
    }

    private static void putIfAbsent(Map<String, MappingItem> out, MappingItem item) {
        String dedupKey = item.getKey() + "|" + item.getNode();
        out.putIfAbsent(dedupKey, item);
    }

    private static String extractFieldNameFromPath(String fieldPath) {
        if (fieldPath == null || fieldPath.isEmpty()) {
            return "unnamed";
        }
        String clean = fieldPath.replace("[]", "");
        clean = clean.replaceAll("\\{[^}]*}", "");
        int dot = clean.lastIndexOf('.');
        if (dot >= 0 && dot < clean.length() - 1) {
            return clean.substring(dot + 1);
        }
        return clean;
    }

    private static String inferValueObjectType(Object value) {
        if (value instanceof String) return "string";
        if (value instanceof Integer) return "int";
        if (value instanceof Long) return "long";
        if (value instanceof java.math.BigDecimal || value instanceof Double || value instanceof Float) return "double";
        if (value instanceof Boolean) return "boolean";
        if (value instanceof Map) return "map";
        if (value instanceof List) return "list";
        return "string";
    }
}
