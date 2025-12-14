package com.scccy.aimodel.util.dsl;

import com.alibaba.fastjson2.JSON;
import com.scccy.aimodel.domain.mp.DimAiModelItemMp;
import com.scccy.aimodel.domain.mp.DimAiModelMp;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Forward 子包的 DSL 工厂，承载模型配置到 DSL 配置、默认 payload 结构生成等逻辑。
 * 旧包中的 ForwardDslFactory 将委托到这里，方便逐步迁移。
 */
public class ForwardDslFactory {

    private ForwardDslFactory() {
    }

    /**
     * 从模型与配置项生成 DSL 配置（复用反向工厂逻辑）。
     */
    public static PostDslConfig fromModel(DimAiModelMp model, List<DimAiModelItemMp> items) {
        String baseInfo = buildBaseInfo(model);
        String headerItem = buildItemSection(items, "header");
        String paramItem = buildItemSection(items, "param");
        return new PostDslConfig(baseInfo, headerItem, paramItem);
    }

    /**
     * 根据配置项生成基础 payload 结构（用于前端填充默认值）。
     */
    public static Map<String, Object> buildBasePayloadByNode(List<DimAiModelItemMp> items) {
        Map<String, Object> root = new LinkedHashMap<>();
        if (CollectionUtils.isEmpty(items)) {
            return root;
        }

        // 记录哪些路径是 list / map，用于构造容器
        Set<String> listPaths = new HashSet<>();

        for (DimAiModelItemMp item : items) {
            String path = resolvePath(item);
            if (!StringUtils.hasText(path)) {
                continue;
            }
            String category = item.getCategory() != null ? item.getCategory() : "key";
            String valueObject = item.getValueObject() != null ? item.getValueObject() : "string";
            Object value;
            if ("list".equalsIgnoreCase(category)) {
                value = new java.util.ArrayList<>();
            } else if ("map".equalsIgnoreCase(category)) {
                value = new LinkedHashMap<>();
            } else {
                if (StringUtils.hasText(item.getDefaultValue())) {
                    value = parseDefaultValue(item.getDefaultValue(), valueObject);
                } else {
                    value = getInitialValueByType(valueObject);
                }
            }
            applyValueByPath(root, path, item.getItemKey(), value, listPaths);
        }

        return root;
    }

    private static String buildBaseInfo(DimAiModelMp model) {
        com.alibaba.fastjson2.JSONObject obj = new com.alibaba.fastjson2.JSONObject();
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
        return JSON.toJSONString(obj);
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
        com.alibaba.fastjson2.JSONObject root = new com.alibaba.fastjson2.JSONObject();
        List<com.alibaba.fastjson2.JSONObject> list = filtered.stream().map(item -> {
            com.alibaba.fastjson2.JSONObject obj = new com.alibaba.fastjson2.JSONObject();
            putIfHasText(obj, "key", item.getItemKey());
            putIfHasText(obj, "node", item.getNode());
            putIfHasText(obj, "post_param", item.getPostParam());
            putIfHasText(obj, "default_value", item.getDefaultValue());
            putIfHasText(obj, "value_object", item.getValueObject());
            putIfHasText(obj, "spel_temp", item.getSpelTemp());
            putIfHasText(obj, "validate", item.getValidate());
            return obj;
        }).collect(java.util.stream.Collectors.toList());
        root.put(sectionName, list);
        return JSON.toJSONString(root);
    }

    private static void putIfHasText(com.alibaba.fastjson2.JSONObject obj, String key, String value) {
        if (StringUtils.hasText(value)) {
            obj.put(key, value);
        }
    }

    private static String resolvePath(DimAiModelItemMp item) {
        if (StringUtils.hasText(item.getNode())) {
            return item.getNode();
        }
        if (StringUtils.hasText(item.getPostParam())) {
            return item.getPostParam();
        }
        return item.getItemKey();
    }

    /**
     * 根据 path 写入值，自动创建 map/list 容器。
     * list 路径采用“模板第一个元素”的方式填充。
     */
    @SuppressWarnings("unchecked")
    private static void applyValueByPath(Map<String, Object> root,
                                  String path,
                                  String leafKey,
                                  Object value,
                                  Set<String> listPaths) {
        String[] parts = path.split("\\.");
        Map<String, Object> current = root;
        for (int i = 0; i < parts.length; i++) {
            ForwardBuilder.Token token = ForwardBuilder.Token.parse(parts[i]);
            boolean isLast = i == parts.length - 1;
            if (isLast) {
                String field = StringUtils.hasText(leafKey) ? leafKey : token.name;
                if (token.hasIndex && !token.mapPlaceholder) {
                    @SuppressWarnings("unchecked")
                    List<Object> list = (List<Object>) current.computeIfAbsent(token.name, k -> new java.util.ArrayList<>());
                    int idx = token.wildcard ? 0 : token.index;
                    while (list.size() <= idx) {
                        list.add(new LinkedHashMap<String, Object>());
                    }
                    Object slot = list.get(idx);
                    if (!(slot instanceof Map)) {
                        slot = new LinkedHashMap<String, Object>();
                        list.set(idx, slot);
                    }
                    @SuppressWarnings("unchecked")
                    Map<String, Object> mapSlot = (Map<String, Object>) slot;
                    mapSlot.put(field, value);
                } else {
                    current.put(field, value);
                }
            } else {
                if (token.mapPlaceholder) {
                    continue;
                }
                if (token.hasIndex) {
                    @SuppressWarnings("unchecked")
                    List<Object> list = (List<Object>) current.computeIfAbsent(token.name, k -> new java.util.ArrayList<>());
                    int idx = token.wildcard ? 0 : token.index;
                    while (list.size() <= idx) {
                        list.add(new LinkedHashMap<String, Object>());
                    }
                    Object slot = list.get(idx);
                    if (!(slot instanceof Map)) {
                        slot = new LinkedHashMap<String, Object>();
                        list.set(idx, slot);
                    }
                    @SuppressWarnings("unchecked")
                    Map<String, Object> mapSlot = (Map<String, Object>) slot;
                    current = mapSlot;
                } else {
                    Object container = current.get(token.name);
                    if (!(container instanceof Map)) {
                        container = new LinkedHashMap<String, Object>();
                        current.put(token.name, container);
                    }
                    @SuppressWarnings("unchecked")
                    Map<String, Object> mapSlot = (Map<String, Object>) container;
                    current = mapSlot;
                }
            }
        }
    }

    /**
     * 解析默认值
     */
    private static Object parseDefaultValue(String defaultValue, String valueObject) {
        if (!StringUtils.hasText(defaultValue)) {
            return getInitialValueByType(valueObject);
        }

        String trimmed = defaultValue.trim();

        // 尝试解析 JSON
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            try {
                return JSON.parse(trimmed);
            } catch (Exception e) {
                // JSON 解析失败，继续按类型转换
            }
        }

        // 根据 valueObject 类型转换
        if ("int".equals(valueObject) || "long".equals(valueObject)) {
            try {
                return Long.parseLong(trimmed);
            } catch (NumberFormatException e) {
                return 0L;
            }
        } else if ("double".equals(valueObject) || "decimal".equals(valueObject)) {
            try {
                return Double.parseDouble(trimmed);
            } catch (NumberFormatException e) {
                return 0.0;
            }
        } else if ("boolean".equals(valueObject)) {
            return "true".equalsIgnoreCase(trimmed) || "1".equals(trimmed);
        } else {
            return defaultValue;
        }
    }

    /**
     * 根据类型获取初始值
     */
    private static Object getInitialValueByType(String valueObject) {
        if (valueObject == null) {
            return "";
        }

        if ("int".equals(valueObject) || "long".equals(valueObject)) {
            return 0L;
        } else if ("double".equals(valueObject) || "decimal".equals(valueObject)) {
            return 0.0;
        } else if ("boolean".equals(valueObject)) {
            return false;
        } else {
            return "";
        }
    }
}
