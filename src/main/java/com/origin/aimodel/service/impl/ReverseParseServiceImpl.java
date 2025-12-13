package com.origin.aimodel.service.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.origin.aimodel.dao.service.DimAiModelItemMpService;
import com.origin.aimodel.dao.service.DimAiModelMpService;
import com.origin.aimodel.domain.mp.DimAiModelItemMp;
import com.origin.aimodel.domain.mp.DimAiModelMp;
import com.origin.aimodel.domain.vo.ForwardRequestResult;
import com.origin.aimodel.domain.vo.ReverseParseRequest;
import com.origin.aimodel.domain.vo.ReverseParseResponse;
import com.origin.aimodel.service.ReverseParseService;
import com.origin.aimodel.util.spel.MappingItem;
import com.origin.aimodel.util.spel.PostDslConfigFactory;
import com.origin.aimodel.util.spel.ReverseDsl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Date;
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

    @Autowired
    public ReverseParseServiceImpl(DimAiModelMpService dimAiModelMpService,
                                   DimAiModelItemMpService dimAiModelItemMpService) {
        this.dimAiModelMpService = dimAiModelMpService;
        this.dimAiModelItemMpService = dimAiModelItemMpService;
    }

    @Override
    public ReverseParseResponse generateConfig(ReverseParseRequest request) {
        try {
            JSONObject requestBody = new JSONObject(request.getRequestBody());
            Map<String, Object> bodyMap = requestBody.toJavaObject(Map.class);

            // 使用 PostDslConfigFactory 直接生成完整配置
            List<MappingItem> paramItems = PostDslConfigFactory.generateBasicParamMapping(bodyMap);
            List<MappingItem> headerItems = PostDslConfigFactory.generateBasicHeaderMapping(request.getRequestHeaders());

            // 如果用户提供了自定义映射，则使用 ReverseDsl 进行转换
            // 否则直接使用生成的配置
            if ((request.getParamMapping() != null && !request.getParamMapping().isEmpty()) ||
                (request.getHeaderMapping() != null && !request.getHeaderMapping().isEmpty())) {
                // 用户提供了自定义映射，使用 ReverseDsl 转换
                JSONObject paramMappingConfig = buildParamMappingConfig(request.getParamMapping());
                JSONObject headerMappingConfig = buildHeaderMappingConfig(request.getHeaderMapping());

                ReverseDsl.Builder paramBuilder = ReverseDsl.build(requestBody, paramMappingConfig);
                JSONObject generatedParamItem = paramBuilder.getParam();

                ReverseDsl.Builder headerBuilder = ReverseDsl.build(requestBody, headerMappingConfig)
                        .withHeaders(request.getRequestHeaders());
                JSONObject generatedHeaderItem = headerBuilder.getHeader();

                ReverseParseResponse response = new ReverseParseResponse();
                response.setParamItems(convertToMappingItemList(generatedParamItem.getJSONArray("paramItem")));
                response.setHeaderItems(convertToMappingItemList(generatedHeaderItem.getJSONArray("headerItem")));
                return response;
            } else {
                // 直接使用生成的完整配置
                ReverseParseResponse response = new ReverseParseResponse();
                response.setParamItems(convertItemsToMappingItemList(paramItems));
                response.setHeaderItems(convertItemsToMappingItemList(headerItems));
                return response;
            }
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
            model.setUpdatedTime(new Date());

            DimAiModelMp existingModel = dimAiModelMpService.getById(request.getModelName());
            if (existingModel != null) {
                dimAiModelMpService.updateById(model);
            } else {
                model.setCreatedTime(new Date());
                model.setDelFlag(0);
                dimAiModelMpService.save(model);
            }

            QueryWrapper<DimAiModelItemMp> removeWrapper = new QueryWrapper<>();
            removeWrapper.eq("model_name", request.getModelName());
            dimAiModelItemMpService.remove(removeWrapper);

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
        for (int i = 0; i < items.size(); i++) {
            MappingItem configItem = items.get(i);
            if (configItem == null || configItem.getKey() == null || configItem.getKey().isEmpty()) {
                log.warn("跳过无效的 {}Item 配置项，索引: {}", type, i);
                continue;
            }
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
            item.setValidate(configItem.getValidate());
            item.setSortOrder(i);
            item.setCreatedTime(new Date());
            item.setDelFlag(0);
            dimAiModelItemMpService.save(item);
        }
    }

    private JSONObject buildParamMappingConfig(List<ReverseParseRequest.MappingConfig> mappings) {
        JSONObject config = new JSONObject();
        JSONArray mappingArray = new JSONArray();

        if (mappings != null) {
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
        }

        config.put("paramItem", mappingArray);
        return config;
    }

    private JSONObject buildHeaderMappingConfig(List<ReverseParseRequest.MappingConfig> mappings) {
        JSONObject config = new JSONObject();
        JSONArray mappingArray = new JSONArray();

        if (mappings != null) {
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
        }

        config.put("headerItem", mappingArray);
        return config;
    }

    private List<com.origin.aimodel.util.spel.MappingItem> convertToMappingItemList(JSONArray array) {
        List<com.origin.aimodel.util.spel.MappingItem> items = new ArrayList<>();

        if (array != null) {
            for (int i = 0; i < array.size(); i++) {
                JSONObject itemObj = array.getJSONObject(i);
                com.origin.aimodel.util.spel.MappingItem item = new com.origin.aimodel.util.spel.MappingItem();
                item.key = itemObj.getString("key");
                item.category = itemObj.getString("category");
                item.node = itemObj.getString("node");
                item.postParam = itemObj.getString("post_param");
                item.spelTemp = itemObj.getString("spel_temp");
                item.defaultValue = itemObj.getString("default_value");
                item.valueObject = itemObj.getString("value_object");
                item.validate = itemObj.getString("validate");
                items.add(item);
            }
        }

        return items;
    }

    /**
     * 将 MappingItem 列表转换为 MappingItem 列表（直接返回，无需转换）
     */
    private List<com.origin.aimodel.util.spel.MappingItem> convertItemsToMappingItemList(List<com.origin.aimodel.util.spel.MappingItem> items) {
        return items != null ? items : new ArrayList<>();
    }
}
