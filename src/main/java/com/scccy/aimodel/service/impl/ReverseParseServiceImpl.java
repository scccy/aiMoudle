package com.scccy.aimodel.service.impl;

import com.scccy.aimodel.dao.mapper.DimAiModelItemMapper;
import com.scccy.aimodel.dao.service.DimAiModelItemMpService;
import com.scccy.aimodel.dao.service.DimAiModelMpService;
import com.scccy.aimodel.domain.mp.DimAiModelItemMp;
import com.scccy.aimodel.domain.mp.DimAiModelMp;
import com.scccy.aimodel.domain.vo.ReverseParseRequest;
import com.scccy.aimodel.domain.vo.ReverseParseResponse;
import com.scccy.aimodel.service.ReverseParseService;
import com.scccy.aimodel.domain.vo.MappingItem;
import com.scccy.aimodel.util.dsl.ReverseDsl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
            ReverseDsl.Builder builder = ReverseDsl.build(request);
            List<MappingItem> generatedParamItems = builder.getParamItems();
            List<MappingItem> generatedHeaderItems = builder.getHeaderItems();

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
        Set<String> seen = new HashSet<>();
        for (MappingItem configItem : items) {
            sortOrder = saveOneItem(request, type, configItem, sortOrder, seen);
        }
    }

    /**
     * 保存单条配置项，扁平化已在 ReverseParser 处理
     */
    private int saveOneItem(ReverseParseRequest request,
                            String type,
                            MappingItem configItem,
                            int sortOrder,
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

        return sortOrder + 1;
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

}
