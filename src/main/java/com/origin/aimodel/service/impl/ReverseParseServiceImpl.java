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
import com.origin.aimodel.util.spel.ReverseDslFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

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

            // 使用 ReverseDslFactory 直接生成完整配置
            List<MappingItem> paramItems = ReverseDslFactory.generateBasicParamMapping(bodyMap);
            List<MappingItem> headerItems = ReverseDslFactory.generateBasicHeaderMapping(request.getRequestHeaders());

            // 如果用户提供了自定义映射，则使用 ReverseDsl 进行转换
            // 否则直接使用生成的配置
            if ((request.getParamMapping() != null && !request.getParamMapping().isEmpty()) ||
                (request.getHeaderMapping() != null && !request.getHeaderMapping().isEmpty())) {
                // 用户提供了自定义映射，使用 ReverseDsl 转换
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
            } else {
                // 直接使用生成的完整配置
                ReverseParseResponse response = new ReverseParseResponse();
                response.setParamItems(paramItems);
                response.setHeaderItems(headerItems);
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
            dimAiModelItemMpService.save(item);
            // 字典映射直接使用 item 表，不需要单独保存
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

}
