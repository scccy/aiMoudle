package com.origin.aimodel.service.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.origin.aimodel.dao.mapper.DimAiModelItemMapper;
import com.origin.aimodel.dao.service.DimAiModelItemMpService;
import com.origin.aimodel.dao.service.DimAiModelMpService;
import com.origin.aimodel.domain.mp.DimAiModelItemMp;
import com.origin.aimodel.domain.mp.DimAiModelMp;
import com.origin.aimodel.domain.vo.ReverseParseRequest;
import com.origin.aimodel.domain.vo.ReverseParseResponse;
import com.origin.aimodel.service.ReverseParseService;
import com.origin.aimodel.util.spel.MappingItem;
import com.origin.aimodel.util.spel.ReverseDsl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 反向解析业务实现
 */
@Slf4j
@Service
public class ReverseParseServiceImpl implements ReverseParseService {

    private final DimAiModelMpService dimAiModelMpService;
    private final DimAiModelItemMpService dimAiModelItemMpService;
    private final DimAiModelItemMapper dimAiModelItemMapper;

    @Autowired
    public ReverseParseServiceImpl(DimAiModelMpService dimAiModelMpService,
                                   DimAiModelItemMpService dimAiModelItemMpService,
                                   DimAiModelItemMapper dimAiModelItemMapper) {
        this.dimAiModelMpService = dimAiModelMpService;
        this.dimAiModelItemMpService = dimAiModelItemMpService;
        this.dimAiModelItemMapper = dimAiModelItemMapper;
    }

    @Override
    public ReverseParseResponse generateConfig(ReverseParseRequest request) {
        try {
            JSONObject requestBody = new JSONObject(request.getRequestBody());
            Map<String, Object> bodyMap = requestBody.toJavaObject(Map.class);
            JSONObject paramMappingConfig = buildParamMappingConfig(request.getParamMapping());
            JSONObject headerMappingConfig = buildHeaderMappingConfig(request.getHeaderMapping());

            ReverseDsl.Builder paramBuilder = ReverseDsl.build(requestBody, paramMappingConfig);
            List<MappingItem> generatedParamItems = paramBuilder.getParamItems();

            ReverseDsl.Builder headerBuilder = ReverseDsl.build(requestBody, headerMappingConfig)
                    .withHeaders(request.getRequestHeaders());
            List<MappingItem> generatedHeaderItems = headerBuilder.getHeaderItems();

            ReverseParseResponse response = new ReverseParseResponse();
            response.setParamItems(generatedParamItems);
            response.setHeaderItems(generatedHeaderItems);
            return response;
        } catch (Exception e) {
            throw new RuntimeException("生成配置失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String saveConfig(ReverseParseRequest request) {
        try {
            DimAiModelMp model = new DimAiModelMp();
            model.setModelName(request.getModelName());
            model.setOriginName(request.getOriginName());
            model.setBaseUrl(request.getBaseUrl());
            model.setPoint(request.getPoint());
            model.setAuthorization(request.getAuthorization());


            DimAiModelMp existingModel = dimAiModelMpService.getById(request.getModelName());
            if (existingModel != null) {
                dimAiModelMpService.updateById(model);
            } else {
                dimAiModelMpService.save(model);
            }

            // 物理删除该模型下的所有旧配置项（只保留新的结构）
            // 使用自定义 SQL 执行物理删除，绕过逻辑删除
            int deletedCount = dimAiModelItemMapper.physicalDeleteByModelName(request.getModelName());
            log.info("物理删除模型 {} 的旧配置项，共删除 {} 条记录", request.getModelName(), deletedCount);

            saveConfigItems(request, "param");
            saveConfigItems(request, "header");

            return "配置保存成功";
        } catch (Exception e) {
            log.error("保存配置失败", e);
            throw new RuntimeException("保存配置失败: " + e.getMessage(), e);
        }
    }

    private void saveConfigItems(ReverseParseRequest request, String type) {
        List<MappingItem> items = "param".equalsIgnoreCase(type)
                ? request.getParamItems()
                : request.getHeaderItems();
        if (CollectionUtils.isEmpty(items)) {
            return;
        }
        int sortOrder = 0;
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (MappingItem configItem : items) {
            sortOrder = saveOneItem(request, type, configItem, sortOrder, configItem.getNode(), configItem.getCategory(), seen);
        }
    }

    private JSONObject buildParamMappingConfig(List<ReverseParseRequest.MappingConfig> mappings) {
        if (mappings == null || mappings.isEmpty()) {
            return null;
        }
        JSONObject config = new JSONObject();
        JSONArray mappingArray = new JSONArray();

        for (ReverseParseRequest.MappingConfig mapping : mappings) {
            JSONObject mappingObj = new JSONObject();
            mappingObj.put("key", mapping.getKey());
            if (mapping.getNode() != null && !mapping.getNode().isEmpty()) {
                mappingObj.put("node", mapping.getNode());
            }
            if (mapping.getPostParam() != null && !mapping.getPostParam().isEmpty()) {
                mappingObj.put("post_param", mapping.getPostParam());
            }
            if (mapping.getValidate() != null && !mapping.getValidate().isEmpty()) {
                mappingObj.put("validate", mapping.getValidate());
            }
            mappingArray.add(mappingObj);
        }

        config.put("paramItem", mappingArray);
        return config;
    }

    /**
     * 保存单条配置项，并对 spel_temp 中的嵌套字段进行扁平化落库
     */
    private int saveOneItem(ReverseParseRequest request,
                            String type,
                            MappingItem configItem,
                            int sortOrder,
                            String parentNode,
                            String parentCategory,
                            java.util.Set<String> seenKeyNode) {
        if (configItem == null || configItem.getKey() == null || configItem.getKey().isEmpty()) {
            log.warn("跳过无效的 {}Item 配置项，索引: {}", type, sortOrder);
            return sortOrder;
        }

        String dedupKey = buildDedupKey(configItem.getKey(), configItem.getNode(), configItem.getPostParam());
        if (seenKeyNode.contains(dedupKey)) {
            return sortOrder;
        }
        seenKeyNode.add(dedupKey);

        DimAiModelItemMp item = new DimAiModelItemMp();
        item.setModelName(request.getModelName());
        item.setItemType(type);
        item.setItemKey(configItem.getKey());
        item.setCategory(configItem.getCategory() != null ? configItem.getCategory() : "key");
        item.setNode(configItem.getNode() != null ? configItem.getNode() : "");
        item.setPostParam(configItem.getPostParam());
        item.setSpelTemp(configItem.getSpelTemp());
        item.setDefaultValue(configItem.getDefaultValue());
        item.setValueObject(configItem.getValueObject() != null ? configItem.getValueObject() : "string");
        item.setValueSource(configItem.getValueSource());
        item.setValidate(configItem.getValidate());
        item.setSortOrder(sortOrder);
        dimAiModelItemMpService.save(item);

        int currentSort = sortOrder + 1;

        // 扁平化 spel_temp 中的嵌套字段，生成独立的 item 记录
        List<MappingItem> nestedItems = parseNestedMappingItems(configItem.getSpelTemp());
        if (!nestedItems.isEmpty()) {
            String parentPath = buildParentPath(parentNode, parentCategory);
            for (MappingItem nested : nestedItems) {
                String childNode = buildChildNode(parentPath, nested.getNode());
                MappingItem flattened = new MappingItem();
                flattened.setKey(nested.getKey());
                flattened.setCategory(nested.getCategory());
                flattened.setNode(childNode);
                flattened.setPostParam(nested.getPostParam());
                flattened.setSpelTemp(nested.getSpelTemp());
                flattened.setDefaultValue(nested.getDefaultValue());
                flattened.setValueObject(nested.getValueObject());
                flattened.setValidate(nested.getValidate());
                flattened.setValueSource(nested.getValueSource());

                currentSort = saveOneItem(request, type, flattened, currentSort, childNode, nested.getCategory(), seenKeyNode);
            }
        }

        return currentSort;
    }

    private String buildParentPath(String parentNode, String parentCategory) {
        if (!StringUtils.hasText(parentNode)) {
            return "";
        }
        return parentNode;
    }

    private String buildChildNode(String parentPath, String childNode) {
        if (!StringUtils.hasText(parentPath)) {
            return childNode != null ? childNode : "";
        }
        if (!StringUtils.hasText(childNode)) {
            return parentPath;
        }
        if (childNode.startsWith(".")) {
            return parentPath + childNode;
        }
        return parentPath + "." + childNode;
    }

    /**
     * 将生成结果扁平化，返回与落库一致的结构
     */
    private List<MappingItem> flattenForResponse(List<MappingItem> items) {
        List<MappingItem> result = new ArrayList<>();
        java.util.Map<String, MappingItem> dedup = new java.util.LinkedHashMap<>();
        if (CollectionUtils.isEmpty(items)) {
            return result;
        }
        for (MappingItem item : items) {
            flattenMappingItem(item, item != null ? item.getNode() : null, item != null ? item.getCategory() : null, dedup);
        }
        result.addAll(dedup.values());
        return result;
    }

    private void flattenMappingItem(MappingItem item, String parentNode, String parentCategory, java.util.Map<String, MappingItem> out) {
        if (item == null || !StringUtils.hasText(item.getKey())) {
            return;
        }
        MappingItem copy = new MappingItem();
        copy.setKey(item.getKey());
        copy.setCategory(item.getCategory());
        copy.setNode(item.getNode());
        copy.setPostParam(item.getPostParam());
        copy.setSpelTemp(item.getSpelTemp());
        copy.setDefaultValue(item.getDefaultValue());
        copy.setValueObject(item.getValueObject());
        copy.setValidate(item.getValidate());
        copy.setValueSource(item.getValueSource());
        out.put(buildDedupKey(copy.getKey(), copy.getNode(), copy.getPostParam()), copy);

        List<MappingItem> nestedItems = parseNestedMappingItems(item.getSpelTemp());
        if (!nestedItems.isEmpty()) {
            String parentPath = buildParentPath(parentNode, parentCategory);
            for (MappingItem nested : nestedItems) {
                String childNode = buildChildNode(parentPath, nested.getNode());
                MappingItem flattened = new MappingItem();
                flattened.setKey(nested.getKey());
                flattened.setCategory(nested.getCategory());
                flattened.setNode(childNode);
                flattened.setPostParam(nested.getPostParam());
                flattened.setSpelTemp(nested.getSpelTemp());
                flattened.setDefaultValue(nested.getDefaultValue());
                flattened.setValueObject(nested.getValueObject());
                flattened.setValidate(nested.getValidate());
                flattened.setValueSource(nested.getValueSource());
                flattenMappingItem(flattened, childNode, nested.getCategory(), out);
            }
        }
    }

    private String buildDedupKey(String key, String node, String postParam) {
        StringBuilder sb = new StringBuilder();
        sb.append(key != null ? key : "");
        sb.append("|");
        sb.append(node != null ? node : "");
        sb.append("|");
        sb.append(postParam != null ? postParam : "");
        return sb.toString();
    }

    /**
     * 解析 spel_temp 中的 paramItem，兼容已转义和未转义的 JSON 字符串
     */
    private List<MappingItem> parseNestedMappingItems(String spelTemp) {
        if (!StringUtils.hasText(spelTemp)) {
            return java.util.Collections.emptyList();
        }
        String normalized = spelTemp.trim();
        JSONObject spelJson = null;
        try {
            spelJson = JSONObject.parseObject(normalized);
        } catch (Exception parseErr) {
            // 尝试去除转义符后再解析
            try {
                String unescaped = normalized.replace("\\\"", "\"");
                spelJson = JSONObject.parseObject(unescaped);
            } catch (Exception retryErr) {
                log.warn("解析嵌套 spel_temp 失败，内容: {}", spelTemp, retryErr);
                return java.util.Collections.emptyList();
            }
        }

        JSONArray paramItems = spelJson.getJSONArray("paramItem");
        if (paramItems == null || paramItems.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        List<MappingItem> nestedItems = new ArrayList<>();
        for (int i = 0; i < paramItems.size(); i++) {
            JSONObject obj = paramItems.getJSONObject(i);
            if (obj == null) {
                continue;
            }
            MappingItem nested = new MappingItem();
            nested.setKey(obj.getString("key"));
            nested.setCategory(obj.getString("category"));
            nested.setNode(obj.getString("node"));
            nested.setPostParam(obj.getString("post_param"));
            nested.setSpelTemp(obj.getString("spel_temp"));
            nested.setDefaultValue(obj.getString("default_value"));
            nested.setValueObject(obj.getString("value_object"));
            nested.setValidate(obj.getString("validate"));
            nested.setValueSource(obj.getString("value_source"));
            nestedItems.add(nested);
        }
        return nestedItems;
    }

    private JSONObject buildHeaderMappingConfig(List<ReverseParseRequest.MappingConfig> mappings) {
        if (mappings == null || mappings.isEmpty()) {
            return null;
        }
        JSONObject config = new JSONObject();
        JSONArray mappingArray = new JSONArray();

        for (ReverseParseRequest.MappingConfig mapping : mappings) {
            JSONObject mappingObj = new JSONObject();
            mappingObj.put("key", mapping.getKey());
            if (mapping.getNode() != null && !mapping.getNode().isEmpty()) {
                mappingObj.put("node", mapping.getNode());
            }
            if (mapping.getPostParam() != null && !mapping.getPostParam().isEmpty()) {
                mappingObj.put("post_param", mapping.getPostParam());
            }
            if (mapping.getValidate() != null && !mapping.getValidate().isEmpty()) {
                mappingObj.put("validate", mapping.getValidate());
            }
            mappingArray.add(mappingObj);
        }

        config.put("headerItem", mappingArray);
        return config;
    }

}
