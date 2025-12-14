package com.origin.aimodel.util.spel;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import com.origin.aimodel.domain.mp.DimAiModelItemMp;
import com.origin.aimodel.domain.mp.DimAiModelMp;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 反向解析 DSL 工厂：第一层（核心识别层）
 * 根据数据库的模型与配置项构建 PostDslConfig。
 * 同时提供从实际请求数据生成基础映射的功能。
 */
public class ReverseDslFactory {

    private ReverseDslFactory() {
    }

    /**
     * 从模型与配置项生成 DSL 配置。
     */
    public static PostDslConfig fromModel(DimAiModelMp model, List<DimAiModelItemMp> items) {
        String baseInfo = buildBaseInfo(model);
        String headerItem = buildItemSection(items, "header");
        String paramItem = buildItemSection(items, "param");
        return new PostDslConfig(baseInfo, headerItem, paramItem);
    }

    private static String buildBaseInfo(DimAiModelMp model) {
        JSONObject obj = new JSONObject();
        if (StringUtils.hasText(model.getModelName())) {
            obj.put("model", model.getModelName());
        }
        if (StringUtils.hasText(model.getAuthorization())) {
            obj.put("Authorization", model.getAuthorization());
        }
        if (StringUtils.hasText(model.getBaseUrl())) {
            obj.put("base_url", model.getBaseUrl());
        }
        if (StringUtils.hasText(model.getPoint())) {
            obj.put("point", model.getPoint());
        }
        return JSON.toJSONString(obj, JSONWriter.Feature.PrettyFormat);
    }

    private static String buildItemSection(List<DimAiModelItemMp> items, String type) {
        if (items == null || items.isEmpty()) {
            return "";
        }
        List<DimAiModelItemMp> filtered = items.stream()
                .filter(i -> type.equalsIgnoreCase(i.getItemType()))
                .toList();
        if (filtered.isEmpty()) {
            return "";
        }
        String sectionName = type + "Item";
        JSONObject root = new JSONObject();
        List<JSONObject> list = filtered.stream().map(item -> {
            JSONObject obj = new JSONObject();
            putIfHasText(obj, "key", item.getItemKey());
            putIfHasText(obj, "category", item.getCategory());
            putIfHasText(obj, "node", item.getNode());
            putIfHasText(obj, "post_param", item.getPostParam());
            putIfHasText(obj, "spel_temp", item.getSpelTemp());
            putIfHasText(obj, "default_value", item.getDefaultValue());
            putIfHasText(obj, "value_object", item.getValueObject());
            return obj;
        }).collect(Collectors.toList());
        root.put(sectionName, list);
        return JSON.toJSONString(root, JSONWriter.Feature.PrettyFormat);
    }

    private static void putIfHasText(JSONObject obj, String key, String value) {
        if (StringUtils.hasText(value)) {
            obj.put(key, value);
        }
    }

    /**
     * 从实际请求 body 生成基础 Param Mapping
     * 
     * @param bodyMap 请求体 Map
     * @return 生成的映射项列表
     */
    public static List<MappingItem> generateBasicParamMapping(Map<String, Object> bodyMap) {
        List<MappingItem> items = new ArrayList<>();

        if (bodyMap == null || bodyMap.isEmpty()) {
            return items;
        }

        // 第一步：检测所有 List 字段，分析其中的 Map 结构
        Map<String, ListAnalysisResult> listAnalysisResults = analyzeListFields(bodyMap);
        
        // 第二步：生成配置项
        for (Map.Entry<String, Object> entry : bodyMap.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            // 如果是 List 类型，检查分析结果
            if (value instanceof List) {
                ListAnalysisResult analysis = listAnalysisResults.get(key);
                if (isValidListAnalysis(analysis)) {
                    // 使用 list<json> 类型
                    MappingItem item = new MappingItem();
                    item.setKey(key);
                    item.setCategory("list");
                    item.setNode(key);
                    item.setPostParam(key);
                    item.setValueObject("list<json>");
                    item.setValueSource("USER");
                    
                    String spelTemp = generateSpelTempFromRepeatedStructure(key, analysis);
                    if (spelTemp != null) {
                        item.setSpelTemp(spelTemp);
                    }
                    
                    items.add(item);
                } else {
                    // 普通 list 类型
                    MappingItem item = createItemFromValue(key, key, value);
                    items.add(item);
                }
            } else if (value instanceof Map) {
                // Map 类型
                MappingItem item = createItemFromValue(key, key, value);
                items.add(item);
            } else {
                // 简单类型
                MappingItem item = createItemFromValue(key, key, value);
                items.add(item);
            }
        }

        return items;
    }
    
    /**
     * 分析所有 List 字段，找出包含 Map 的 List 及其结构
     */
    private static Map<String, ListAnalysisResult> analyzeListFields(Map<String, Object> bodyMap) {
        Map<String, ListAnalysisResult> results = new java.util.HashMap<>();
        
        for (Map.Entry<String, Object> entry : bodyMap.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (value instanceof List) {
                List<?> list = (List<?>) value;
                ListAnalysisResult analysis = analyzeListStructure(list);
                if (analysis != null) {
                    results.put(key, analysis);
                }
            }
        }
        
        return results;
    }
    
    /**
     * 分析单个 List 的结构
     * 统一方法：提取所有 Map 对象，分析重复结构
     */
    private static ListAnalysisResult analyzeListStructure(List<?> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        
        // 提取所有 Map 对象
        List<Map<?, ?>> mapObjects = extractMapObjectsFromList(list);
        if (mapObjects.isEmpty()) {
            return null;
        }
        
        // 分析重复结构
        Map<String, List<Map<?, ?>>> repeatedBySignature = analyzeRepeatedStructures(mapObjects);
        
        return new ListAnalysisResult(repeatedBySignature, mapObjects);
    }
    
    /**
     * 从 List 中提取所有 Map 对象
     * 统一方法：确保所有地方使用相同的提取逻辑
     */
    private static List<Map<?, ?>> extractMapObjectsFromList(List<?> list) {
        List<Map<?, ?>> mapObjects = new ArrayList<>();
        if (list == null || list.isEmpty()) {
            return mapObjects;
        }
        
        for (Object item : list) {
            if (item instanceof Map) {
                mapObjects.add((Map<?, ?>) item);
            }
        }
        
        return mapObjects;
    }
    
    /**
     * 分析重复结构
     * 统一方法：统计相同结构的 Map 对象（出现次数 >= 2）
     */
    private static Map<String, List<Map<?, ?>>> analyzeRepeatedStructures(List<Map<?, ?>> mapObjects) {
        // 统计所有 Map 对象的结构（缓存签名避免重复计算）
        Map<String, Integer> structureCounts = new java.util.HashMap<>();
        Map<Map<?, ?>, String> signatureCache = new java.util.IdentityHashMap<>();
        
        for (Map<?, ?> itemMap : mapObjects) {
            String structureSignature = generateStructureSignature(itemMap);
            signatureCache.put(itemMap, structureSignature);
            structureCounts.put(structureSignature, structureCounts.getOrDefault(structureSignature, 0) + 1);
        }
        
        // 找出所有重复结构（出现次数 >= 2）
        Map<String, List<Map<?, ?>>> repeatedBySignature = new java.util.HashMap<>();
        for (Map.Entry<Map<?, ?>, String> entry : signatureCache.entrySet()) {
            String sig = entry.getValue();
            int count = structureCounts.getOrDefault(sig, 0);
            if (count >= 2) {
                repeatedBySignature
                    .computeIfAbsent(sig, k -> new ArrayList<>())
                    .add(entry.getKey());
            }
        }
        
        return repeatedBySignature;
    }
    
    /**
     * 生成 Map 的结构签名（基于字段名，不考虑值）
     */
    private static String generateStructureSignature(Map<?, ?> map) {
        List<String> keys = new ArrayList<>();
        collectKeys(map, "", keys);
        keys.sort(String::compareTo);
        return String.join(",", keys);
    }
    
    /**
     * 递归收集所有字段路径
     */
    private static void collectKeys(Object obj, String prefix, List<String> keys) {
        if (obj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) obj;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = entry.getKey().toString();
                String currentPath = prefix.isEmpty() ? key : prefix + "." + key;
                keys.add(currentPath);
                
                Object value = entry.getValue();
                if (value instanceof Map && !((Map<?, ?>) value).isEmpty()) {
                    collectKeys(value, currentPath, keys);
                } else if (value instanceof List) {
                    List<?> list = (List<?>) value;
                    // 仅继续下钻包含 Map 的列表，使用 [] 作为占位
                    list.stream()
                            .filter(e -> e instanceof Map)
                            .forEach(e -> collectKeys(e, currentPath + "[]", keys));
                }
            }
        }
    }
    
    /**
     * 判断 ListAnalysisResult 是否有效（可以生成 spel_temp）
     * 统一判断标准：有重复结构（>=2个相同结构）或者至少有一个 Map 对象
     */
    private static boolean isValidListAnalysis(ListAnalysisResult analysis) {
        if (analysis == null) {
            return false;
        }
        // 有重复结构（出现次数 >= 2）或者至少有一个 Map 对象
        return analysis.hasRepeatedStructure() || 
               (analysis.getAllMaps() != null && !analysis.getAllMaps().isEmpty());
    }
    
    /**
     * 根据重复结构生成 spel_temp（支持嵌套）
     */
    private static String generateSpelTempFromRepeatedStructure(String parentKey, ListAnalysisResult analysis) {
        if (!isValidListAnalysis(analysis)) {
            return null;
        }
        
        Map<String, List<Map<?, ?>>> repeated = analysis.getRepeatedBySignature();
        List<Map<?, ?>> allMaps = analysis.getAllMaps();
        
        // 聚合所有需要描述的结构：优先重复结构；若无重复但存在单个结构，也生成描述
        Map<String, FieldInfo> mergedFields = new java.util.HashMap<>();
        if (!repeated.isEmpty()) {
            for (List<Map<?, ?>> maps : repeated.values()) {
                if (maps == null || maps.isEmpty()) {
                    continue;
                }
                analyzeCommonFields(maps, mergedFields);
            }
        } else {
            analyzeCommonFields(allMaps, mergedFields);
        }
        
        if (mergedFields.isEmpty()) {
            return null;
        }
        
        List<MappingItem> paramItems = new ArrayList<>();
        mergedFields.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    String fieldPath = entry.getKey();
                    FieldInfo fieldInfo = entry.getValue();
                    
                    // 统一方法：提取字段名（最后一个点后的部分，去掉 [] 占位符）
                    String fieldName = extractFieldNameFromPath(fieldPath);
                    
                    // spel_temp 中的 node 使用相对路径（相对于父级 node）
                    // 例如：如果字段路径是 "type"，则 node 为 "type"
                    // 如果字段路径是 "image_url.url"，则 node 为 "image_url.url"
                    // 如果字段路径是 "trajectories[].x"，则 node 为 "trajectories.x"（去掉 []）
                    // 在 PayloadEngine 处理时，会将父级 node（如 "content"）与这个相对路径组合
                    String nodePath = fieldPath.replace("[]", "");
                    
                    // 统一使用公共方法创建 MappingItem 并处理嵌套结构
                    MappingItem fieldItem = createMappingItemFromFieldInfo(
                        fieldName, 
                        nodePath, 
                        fieldPath, 
                        fieldInfo, 
                        analysis
                    );
                    
                    paramItems.add(fieldItem);
                });
        
        return buildSpelTempFromMappingItems(paramItems);
    }
    
    /**
     * 统一的公共方法：从 FieldInfo 创建 MappingItem 并处理嵌套结构
     * 确保所有层级使用相同的识别和处理逻辑
     * 
     * @param fieldName 字段名
     * @param nodePath node 路径（已去掉 []）
     * @param fieldPath 完整字段路径（可能包含 []）
     * @param fieldInfo 字段信息
     * @param analysis 父级分析结果（可选）
     * @param nestedFields 嵌套字段映射（可选，用于 list<json> 的 fallback）
     * @return 创建的 MappingItem
     */
    private static MappingItem createMappingItemFromFieldInfo(
            String fieldName,
            String nodePath,
            String fieldPath,
            FieldInfo fieldInfo,
            ListAnalysisResult analysis,
            Map<String, FieldInfo> nestedFields) {
        
        return createMappingItemFromFieldInfo(fieldName, nodePath, fieldPath, fieldInfo, analysis, nestedFields, null);
    }
    
    /**
     * 统一的公共方法：从 FieldInfo 创建 MappingItem 并处理嵌套结构（重载方法，支持指定嵌套字段）
     */
    private static MappingItem createMappingItemFromFieldInfo(
            String fieldName,
            String nodePath,
            String fieldPath,
            FieldInfo fieldInfo,
            ListAnalysisResult analysis) {
        
        return createMappingItemFromFieldInfo(fieldName, nodePath, fieldPath, fieldInfo, analysis, null, null);
    }
    
    /**
     * 统一的公共方法：从 FieldInfo 创建 MappingItem 并处理嵌套结构（完整版本）
     */
    private static MappingItem createMappingItemFromFieldInfo(
            String fieldName,
            String nodePath,
            String fieldPath,
            FieldInfo fieldInfo,
            ListAnalysisResult analysis,
            Map<String, FieldInfo> nestedFields,
            Map<String, FieldInfo> parentNestedFields) {
        
        // 确保 fieldName 不为空，如果为空则从 fieldPath 或 nodePath 提取
        if (fieldName == null || fieldName.isEmpty()) {
            if (fieldPath != null && !fieldPath.isEmpty()) {
                fieldName = extractFieldNameFromPath(fieldPath);
            } else if (nodePath != null && !nodePath.isEmpty()) {
                fieldName = extractFieldNameFromPath(nodePath);
            } else {
                fieldName = "unnamed"; // 默认值，避免空字段名
            }
        }
        
        MappingItem fieldItem = new MappingItem();
        fieldItem.key = fieldName;
        String valueObjectType = fieldInfo.getType();
        
        // 统一方法：根据 value_object 类型设置 category
        fieldItem.category = inferCategoryFromValueObjectType(valueObjectType);
        // 默认值来源：如果值完全一致则视为常量，否则用户输入
        fieldItem.valueSource = fieldInfo.hasCommonValue() ? "CONST" : "USER";
        
        fieldItem.node = nodePath;
        fieldItem.postParam = fieldName;
        fieldItem.valueObject = valueObjectType;
        
        // 统一方法：处理 map/json 类型的嵌套 spel_temp
        if (isMapType(valueObjectType)) {
            String mapSpelTemp = generateSpelTempForMapField(fieldInfo, analysis);
            if (mapSpelTemp != null) {
                fieldItem.spelTemp = mapSpelTemp;
            }
        }
        // 统一方法：处理 list<json> 类型的嵌套 spel_temp
        else if (isListJsonType(valueObjectType)) {
            String listSpelTemp = generateSpelTempForListField(
                fieldName, 
                fieldInfo, 
                fieldPath, 
                nestedFields != null ? nestedFields : (parentNestedFields != null ? parentNestedFields : new java.util.HashMap<>()), 
                analysis
            );
            if (listSpelTemp != null) {
                fieldItem.spelTemp = listSpelTemp;
            }
        }
        
        // 仅在所有值相同的情况下设置 defaultValue，避免多样值被当作常量
        if (fieldInfo.hasCommonValue()) {
            fieldItem.defaultValue = fieldInfo.getCommonValue();
        }
        
        return fieldItem;
    }
    
    /**
     * 统一方法：从路径中提取字段名
     * 例如："type" -> "type", "image_url.url" -> "url", "trajectories[].x" -> "x"
     */
    private static String extractFieldNameFromPath(String fieldPath) {
        if (fieldPath == null || fieldPath.isEmpty()) {
            return "unnamed";
        }
        // 去掉 [] 占位符
        String cleanPath = fieldPath.replace("[]", "");
        // 提取最后一个点后的部分
        String fieldName = cleanPath.contains(".") 
            ? cleanPath.substring(cleanPath.lastIndexOf(".") + 1) 
            : cleanPath;
        // 确保不为空
        return fieldName.isEmpty() ? "unnamed" : fieldName;
    }
    
    /**
     * 统一方法：根据 value_object 类型推断 category
     */
    private static String inferCategoryFromValueObjectType(String valueObjectType) {
        if (valueObjectType == null) {
            return "key";
        }
        if ("map".equals(valueObjectType) || "json".equals(valueObjectType)) {
            return "map";
        } else if (valueObjectType.startsWith("list")) {
            return "list";
        }
        return "key";
    }
    
    /**
     * 统一方法：判断是否为 map/json 类型
     */
    private static boolean isMapType(String valueObjectType) {
        return "map".equals(valueObjectType) || "json".equals(valueObjectType);
    }
    
    /**
     * 统一方法：判断是否为 list<json> 类型
     */
    private static boolean isListJsonType(String valueObjectType) {
        return "list<json>".equals(valueObjectType) || 
               (valueObjectType != null && valueObjectType.startsWith("list") && valueObjectType.contains("json"));
    }
    
    /**
     * 统一方法：为 map/json 类型的字段生成 spel_temp
     */
    private static String generateSpelTempForMapField(FieldInfo fieldInfo, ListAnalysisResult analysis) {
        List<Map<?, ?>> samples = fieldInfo.getMapSamples();
        Map<?, ?> mapValue = fieldInfo.getMapValue();
        if ((samples == null || samples.isEmpty()) && (mapValue == null || mapValue.isEmpty())) {
            return null;
        }
        
        // 分析 Map 的内部结构，优先使用全部样本以避免把不同值当作常量
        Map<String, FieldInfo> nestedFields = new java.util.HashMap<>();
        if (samples != null && !samples.isEmpty()) {
            analyzeCommonFields(samples, nestedFields);
        } else {
            analyzeMapFields(mapValue, "", nestedFields);
        }
        
        // 生成嵌套的 spel_temp，需要递归处理嵌套的 list<json> 和 map
        List<MappingItem> nestedParamItems = buildNestedMappingItems(nestedFields, analysis);
        
        if (!nestedParamItems.isEmpty()) {
            return buildSpelTempFromMappingItems(nestedParamItems);
        }
        
        return null;
    }
    
    /**
     * 构建嵌套的 MappingItem 列表，递归处理 list<json> 和 map 类型
     */
    private static List<MappingItem> buildNestedMappingItems(Map<String, FieldInfo> nestedFields, ListAnalysisResult analysis) {
        List<MappingItem> nestedParamItems = new ArrayList<>();
        for (Map.Entry<String, FieldInfo> nestedEntry : nestedFields.entrySet()) {
            String nestedFieldPath = nestedEntry.getKey();
            FieldInfo nestedFieldInfo = nestedEntry.getValue();
            
            // 统一方法：提取字段名（去掉 [] 占位符）
            String nestedFieldName = extractFieldNameFromPath(nestedFieldPath);
            
            // node 路径去掉 [] 占位符，使用相对路径
            String nodePath = nestedFieldPath.replace("[]", "");
            
            // 统一使用公共方法创建 MappingItem 并处理嵌套结构
            MappingItem nestedFieldItem = createMappingItemFromFieldInfo(
                nestedFieldName, 
                nodePath, 
                nestedFieldPath, 
                nestedFieldInfo, 
                analysis,
                nestedFields
            );
            
            nestedParamItems.add(nestedFieldItem);
        }
        return nestedParamItems;
    }
    
    /**
     * 统一的方法：为 list<json> 类型的字段生成 spel_temp
     * 这个方法会尝试多种方式获取列表数据，确保识别的一致性
     * 
     * @param fieldName 字段名（如 "trajectories"）
     * @param fieldInfo 字段信息
     * @param fieldPath 字段路径（如 "trajectories"）
     * @param nestedFields 嵌套字段映射（可能包含 "trajectories[].x" 这样的路径）
     * @param analysis 父级分析结果（可选）
     * @return 生成的 spel_temp JSON 字符串，如果无法生成则返回 null
     */
    private static String generateSpelTempForListField(
            String fieldName,
            FieldInfo fieldInfo,
            String fieldPath,
            Map<String, FieldInfo> nestedFields,
            ListAnalysisResult analysis) {
        
        List<Map<?, ?>> nestedMaps = new ArrayList<>();
        
        // 方式1：从 fieldInfo.getListValue() 获取列表值（优先）
        List<?> listValue = fieldInfo.getListValue();
        if (listValue != null && !listValue.isEmpty()) {
            for (Object item : listValue) {
                if (item instanceof Map) {
                    nestedMaps.add((Map<?, ?>) item);
                }
            }
        }
        
        // 方式2：如果 listValue 为空，尝试从 nestedFields 中提取 list 内部字段
        // 在 analyzeMapFields 中，当遇到 List 类型时，会使用 fieldPath + "[]" 作为前缀
        // 来递归分析列表内部的 Map 元素，所以 nestedFields 中会包含像 
        // "trajectories[].x"、"trajectories[].y" 这样的路径。
        // 我们需要从这些路径中重建 Map 对象
        if (nestedMaps.isEmpty()) {
            String listPrefix = fieldPath + "[]";
            // 收集所有以 listPrefix 开头的字段路径
            Map<String, FieldInfo> listInternalFields = new java.util.HashMap<>();
            for (Map.Entry<String, FieldInfo> entry : nestedFields.entrySet()) {
                String path = entry.getKey();
                if (path.startsWith(listPrefix + ".") || path.equals(listPrefix)) {
                    // 提取相对路径（去掉 listPrefix + "."）
                    // 例如：trajectories[].x -> x, trajectories[].y -> y
                    String relativePath = path.substring(listPrefix.length());
                    if (relativePath.startsWith(".")) {
                        relativePath = relativePath.substring(1);
                    }
                    if (!relativePath.isEmpty() && !relativePath.equals("[]")) {
                        listInternalFields.put(relativePath, entry.getValue());
                    }
                }
            }
            
            // 如果有内部字段，尝试从父级分析结果中提取对应的 Map 对象
            if (!listInternalFields.isEmpty() && analysis != null) {
                // 从父级分析结果中提取该字段路径对应的列表
                for (Map<?, ?> map : analysis.getAllMaps()) {
                    Object nestedValue = extractNestedValue(map, fieldPath);
                    if (nestedValue instanceof List) {
                        List<?> nestedList = (List<?>) nestedValue;
                        for (Object item : nestedList) {
                            if (item instanceof Map) {
                                nestedMaps.add((Map<?, ?>) item);
                            }
                        }
                    }
                }
            }
        }
        
        // 方式3：如果前两种方式都失败，尝试从父级分析结果中直接提取
        if (nestedMaps.isEmpty() && analysis != null) {
            ListAnalysisResult nestedAnalysis = analyzeNestedListStructure(fieldPath, analysis);
            if (nestedAnalysis != null) {
                nestedMaps.addAll(nestedAnalysis.getAllMaps());
            }
        }
        
        // 如果找到了 Map 元素，使用统一的识别方法生成 spel_temp
        if (!nestedMaps.isEmpty()) {
            ListAnalysisResult nestedAnalysis = analyzeListStructure(nestedMaps);
            if (isValidListAnalysis(nestedAnalysis)) {
                return generateSpelTempFromRepeatedStructure(fieldName, nestedAnalysis);
            }
        }
        
        return null;
    }
    
    /**
     * 将 MappingItem 列表转换为 spel_temp JSON 字符串
     * 注意：需要包含嵌套的 spel_temp，以支持三级及以上的嵌套结构
     */
    private static String buildSpelTempFromMappingItems(List<MappingItem> items) {
        if (items == null || items.isEmpty()) {
            return null;
        }
        
        JSONArray paramItems = new JSONArray();
        for (MappingItem item : items) {
            JSONObject obj = new JSONObject();
            if (item.key != null) obj.put("key", item.key);
            if (item.category != null) obj.put("category", item.category);
            if (item.node != null) obj.put("node", item.node);
            if (item.postParam != null) obj.put("post_param", item.postParam);
            if (item.valueObject != null) obj.put("value_object", item.valueObject);
            if (item.defaultValue != null) obj.put("default_value", item.defaultValue);
            if (item.validate != null) obj.put("validate", item.validate);
            if (item.valueSource != null) obj.put("value_source", item.valueSource);
            // 重要：需要包含嵌套的 spel_temp，以支持三级及以上的嵌套结构
            // 例如：dynamic_masks -> trajectories -> 更深层的嵌套
            if (item.spelTemp != null && !item.spelTemp.trim().isEmpty()) {
                obj.put("spel_temp", item.spelTemp);
            }
            paramItems.add(obj);
        }
        
        JSONObject nestedConfig = new JSONObject();
        nestedConfig.put("paramItem", paramItems);
        return nestedConfig.toJSONString();
    }
    
    /**
     * 分析嵌套列表结构（用于嵌套字段的 list<json> 类型）
     */
    private static ListAnalysisResult analyzeNestedListStructure(String fieldPath, ListAnalysisResult parentAnalysis) {
        // 从父级分析结果中提取该字段路径对应的列表
        List<Map<?, ?>> nestedMaps = new ArrayList<>();
        for (Map<?, ?> map : parentAnalysis.getAllMaps()) {
            Object nestedValue = extractNestedValue(map, fieldPath);
            if (nestedValue instanceof List) {
                List<?> nestedList = (List<?>) nestedValue;
                for (Object item : nestedList) {
                    if (item instanceof Map) {
                        nestedMaps.add((Map<?, ?>) item);
                    }
                }
            }
        }
        
        if (nestedMaps.isEmpty()) {
            return null;
        }
        
        return analyzeListStructure(nestedMaps);
    }
    
    /**
     * 从 Map 中提取嵌套字段的值（支持点号路径）
     */
    private static Object extractNestedValue(Map<?, ?> map, String fieldPath) {
        if (fieldPath == null || fieldPath.isEmpty()) {
            return null;
        }
        
        // 处理路径中的 [] 占位符，先去掉
        String cleanPath = fieldPath.replace("[]", "");
        String[] parts = cleanPath.split("\\.");
        Object current = map;
        
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
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
    
    /**
     * 分析 Map 的字段信息
     */
    private static void analyzeMapFields(Map<?, ?> map, String prefix, Map<String, FieldInfo> fieldInfos) {
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = entry.getKey().toString();
            Object value = entry.getValue();
            String fieldPath = prefix.isEmpty() ? key : prefix + "." + key;
            
            if (!fieldInfos.containsKey(fieldPath)) {
                fieldInfos.put(fieldPath, new FieldInfo());
            }
            
            FieldInfo fieldInfo = fieldInfos.get(fieldPath);
            fieldInfo.incrementCount();
            
            if (value instanceof Map && !((Map<?, ?>) value).isEmpty()) {
                // Map 类型的值：记录字段本身是 map 类型，并递归分析其内部结构
                fieldInfo.setType("map");
                fieldInfo.setMapValue((Map<?, ?>) value); // 保存一个样本
                fieldInfo.addMapSample((Map<?, ?>) value); // 收集所有样本，便于聚合值
                analyzeMapFields((Map<?, ?>) value, fieldPath, fieldInfos);
            } else if (value instanceof List) {
                // List 类型的值：检查是否包含 Map 元素
                List<?> list = (List<?>) value;
                boolean hasMapElements = list.stream().anyMatch(e -> e instanceof Map);
                if (hasMapElements) {
                    // 包含 Map 元素，识别为 list<json> 类型
                    fieldInfo.setType("list<json>");
                    fieldInfo.setListValue(list); // 保存 List 值用于后续生成 spel_temp
                    // 继续下钻 Map 元素，路径使用 [] 占位
                    list.stream()
                            .filter(e -> e instanceof Map)
                            .forEach(e -> analyzeMapFields((Map<?, ?>) e, fieldPath + "[]", fieldInfos));
                } else {
                    // 不包含 Map 元素，按普通列表处理
                    fieldInfo.addValue(value);
                    // 对于 List，需要识别元素类型，如 list<string>, list<int> 等
                    if (!list.isEmpty()) {
                        Object firstElement = list.get(0);
                        String elementType = inferValueObjectType(firstElement);
                        fieldInfo.setType("list<" + elementType + ">");
                    } else {
                        // 空列表，默认为 list<string>
                        fieldInfo.setType("list<string>");
                    }
                }
            } else {
                fieldInfo.addValue(value);
                fieldInfo.setType(inferValueObjectType(value));
            }
        }
    }

    /**
     * 针对一组重复 Map，累积公共字段信息到 mergedFields
     */
    private static void analyzeCommonFields(List<Map<?, ?>> maps, Map<String, FieldInfo> mergedFields) {
        if (maps == null || maps.isEmpty()) {
            return;
        }
        Map<String, FieldInfo> local = new java.util.HashMap<>();
        for (Map<?, ?> map : maps) {
            analyzeMapFields(map, "", local);
        }
        // 只保留在该组全部 Map 中出现的字段
        local.entrySet().stream()
                .filter(e -> e.getValue().getCount() == maps.size())
                .forEach(e -> mergedFields.merge(
                        e.getKey(),
                        e.getValue(),
                        (oldVal, newVal) -> {
                            oldVal.getValues().addAll(newVal.getValues());
                            
                            // 合并类型：如果类型不同，统一为 "string"（类型不一致时使用通用类型）
                            // 如果类型相同，保留该类型；如果类型不同，使用 "string" 作为通用类型
                            String oldType = oldVal.getType();
                            String newType = newVal.getType();
                            if (oldType == null || oldType.isEmpty()) {
                                oldVal.setType(newType != null ? newType : "string");
                            } else if (newType != null && !newType.isEmpty() && !oldType.equals(newType)) {
                                // 类型不一致，统一为 "string"
                                oldVal.setType("string");
                            }
                            // 如果类型相同，保持原类型不变
                            
                            // 合并 Map 值（如果存在，保留第一个非空的 Map 值）
                            if (newVal.getMapValue() != null && oldVal.getMapValue() == null) {
                                oldVal.setMapValue(newVal.getMapValue());
                            }
                            // 合并 List 值（如果存在，保留第一个非空的 List 值）
                            if (newVal.getListValue() != null && oldVal.getListValue() == null) {
                                oldVal.setListValue(newVal.getListValue());
                            }
                            return oldVal;
                        }
                ));
    }
    
    /**
     * 推断值的类型
     */
    private static String inferValueObjectType(Object value) {
        if (value instanceof String) return "string";
        if (value instanceof Integer) return "int";
        if (value instanceof Long) return "long";
        // BigDecimal 统一识别为 double，与前端保持一致
        if (value instanceof java.math.BigDecimal) return "double";
        if (value instanceof Boolean) return "boolean";
        if (value instanceof Double || value instanceof Float) return "double";
        if (value instanceof Map) return "map"; // 统一使用 "map"，与前端保持一致
        if (value instanceof List) return "list";
        return "string";
    }
    
    /**
     * List 分析结果
     */
    private static class ListAnalysisResult {
        private final Map<String, List<Map<?, ?>>> repeatedBySignature;
        private final List<Map<?, ?>> allMaps;

        public ListAnalysisResult(Map<String, List<Map<?, ?>>> repeatedBySignature,
                                  List<Map<?, ?>> allMaps) {
            this.repeatedBySignature = repeatedBySignature != null
                    ? repeatedBySignature
                    : new java.util.HashMap<>();
            this.allMaps = allMaps != null ? allMaps : new ArrayList<>();
        }

        public boolean hasRepeatedStructure() {
            return repeatedBySignature.values().stream()
                    .anyMatch(list -> list != null && list.size() >= 2);
        }

        public Map<String, List<Map<?, ?>>> getRepeatedBySignature() {
            return repeatedBySignature;
        }

        public List<Map<?, ?>> getAllMaps() {
            return allMaps;
        }
    }
    
    /**
     * 字段信息
     */
    private static class FieldInfo {
        private int count = 0;
        private final List<Object> values = new ArrayList<>();
        private String type = "string";
        private Map<?, ?> mapValue; // 用于存储 Map 类型的值，用于生成嵌套 spel_temp
        private List<?> listValue; // 用于存储 List 类型的值，用于生成嵌套 spel_temp
        private final List<Map<?, ?>> mapSamples = new ArrayList<>(); // 收集 Map 样本，便于聚合多个值
        
        public FieldInfo() {
        }
        
        public void incrementCount() {
            count++;
        }
        
        public void addValue(Object value) {
            values.add(value);
        }
        
        public void setMapValue(Map<?, ?> mapValue) {
            this.mapValue = mapValue;
        }
        
        public Map<?, ?> getMapValue() {
            return mapValue;
        }

        public void addMapSample(Map<?, ?> mapSample) {
            if (mapSample != null) {
                this.mapSamples.add(mapSample);
            }
        }

        public List<Map<?, ?>> getMapSamples() {
            return mapSamples;
        }
        
        public void setListValue(List<?> listValue) {
            this.listValue = listValue;
        }
        
        public List<?> getListValue() {
            return listValue;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public int getCount() {
            return count;
        }
        
        public String getType() {
            return type;
        }

        public List<Object> getValues() {
            return values;
        }
        
        public boolean hasCommonValue() {
            if (values.isEmpty()) return false;
            Object firstValue = values.get(0);
            return values.stream().allMatch(v -> 
                (v == null && firstValue == null) || 
                (v != null && v.equals(firstValue))
            );
        }
        
        public String getCommonValue() {
            if (values.isEmpty()) return null;
            Object firstValue = values.get(0);
            return firstValue != null ? firstValue.toString() : null;
        }
    }

    /**
     * 从实际请求 headers 生成基础 Header Mapping
     * 
     * @param requestHeaders 请求头 Map
     * @return 生成的映射项列表
     */
    public static List<MappingItem> generateBasicHeaderMapping(Map<String, String> requestHeaders) {
        List<MappingItem> items = new ArrayList<>();

        if (requestHeaders == null || requestHeaders.isEmpty()) {
            return items;
        }

        for (Map.Entry<String, String> entry : requestHeaders.entrySet()) {
            String headerName = entry.getKey();
            String headerValue = entry.getValue();
            
            // 确保 headerName 不为空
            if (headerName == null || headerName.isEmpty()) {
                continue; // 跳过空的 header 名称
            }

            MappingItem item = new MappingItem();
            // 生成 key：将 header 名称转换为小写，去掉特殊字符，如果为空则使用原始名称
            String key = headerName.toLowerCase().replaceAll("[^a-z0-9]", "_");
            if (key.isEmpty() || key.equals("_")) {
                key = headerName.toLowerCase().replaceAll("[^a-z0-9]", "");
                if (key.isEmpty()) {
                    key = headerName; // 如果还是为空，使用原始名称
                }
            }
            // 确保 key 不为空
            if (key.isEmpty()) {
                key = "header_" + items.size(); // 最后的 fallback
            }
            
            item.setKey(key);
            item.setCategory("key");
            item.setNode(headerName);
            item.setPostParam(headerName);
            item.setValueObject("string");
            item.setValueSource("USER");
            if (StringUtils.hasText(headerValue)) {
                item.setDefaultValue(headerValue);
            }
            items.add(item);
        }

        return items;
    }

    /**
     * 根据值类型创建 Item
     */
    private static MappingItem createItemFromValue(String key, String nodePath, Object value) {
        MappingItem item = new MappingItem();
        item.setKey(key);
        item.setNode(nodePath);
        item.setValueSource("USER");
        
        // 推断 category 和 valueObject
        if (value instanceof List) {
            item.setCategory("list");
            List<?> list = (List<?>) value;
            if (list.isEmpty()) {
                item.setValueObject("list<string>");
            } else {
                Object first = list.get(0);
                if (first instanceof Map) {
                    item.setValueObject("list<json>");
                } else {
                    String elementType = inferValueObjectType(first);
                    item.setValueObject("list<" + elementType + ">");
                }
            }
        } else if (value instanceof Map) {
            item.setCategory("map");
            // 统一使用 "map"，与前端保持一致（前端有 map 和 json 两个选项，但后端统一识别为 map）
            item.setValueObject("map");
        } else {
            item.setCategory("key");
            item.setValueObject(inferValueObjectType(value));
            // 对于简单类型，设置 defaultValue
            if (value != null) {
                item.setDefaultValue(value.toString());
            }
        }
        
        return item;
    }


    /**
     * 从 node 路径中提取 post_param
     */
    private static String extractPostParamFromNode(String nodePath) {
        if (nodePath == null || nodePath.isEmpty()) {
            return null;
        }

        if (nodePath.contains(".")) {
            return nodePath.substring(nodePath.lastIndexOf(".") + 1);
        }

        if (nodePath.contains("[")) {
            int bracketIndex = nodePath.indexOf("[");
            return bracketIndex > 0 ? nodePath.substring(0, bracketIndex) : nodePath;
        }

        return nodePath;
    }
}
