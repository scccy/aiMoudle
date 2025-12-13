package com.origin.aimodel.controller;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.origin.aimodel.base.ResultData;
import com.origin.aimodel.domain.vo.ReverseParseRequest;
import com.origin.aimodel.domain.vo.ReverseParseResponse;
import com.origin.aimodel.util.spel.ReverseDsl;
import com.origin.aimodel.util.spel.ReverseParser;
import com.origin.aimodel.domain.mp.DimAiModelMp;
import com.origin.aimodel.domain.mp.DimAiModelItemMp;
import com.origin.aimodel.dao.service.DimAiModelMpService;
import com.origin.aimodel.dao.service.DimAiModelItemMpService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 反向解析 Controller
 * 提供接口用于从实际请求数据生成 paramItem 和 headerItem 配置
 *
 * @author origin
 * @since 2025-12-13
 */
@Slf4j
@RestController
@RequestMapping("/api/reverse-parse")
public class ReverseParseController {

    @Autowired
    private DimAiModelMpService dimAiModelMpService;

    @Autowired
    private DimAiModelItemMpService dimAiModelItemMpService;

    /**
     * 生成基础映射：根据 requestBody 自动生成基础的 paramMapping 和 headerMapping
     * 
     * @param request 反向解析请求（只需要 requestBody 和 requestHeaders）
     * @return 生成的基础映射关系
     */
    @PostMapping("/generate-mapping")
    public ResultData<ReverseParseRequest> generateMapping(@RequestBody ReverseParseRequest request) {
        try {
            // 1. 构建请求 body JSONObject
            JSONObject requestBody = new JSONObject(request.getRequestBody());
            Map<String, Object> bodyMap = requestBody.toJavaObject(Map.class);
            
            // 2. 生成基础的 paramMapping
            List<ReverseParseRequest.MappingConfig> paramMapping = generateBasicParamMapping(bodyMap);
            
            // 3. 生成基础的 headerMapping
            List<ReverseParseRequest.MappingConfig> headerMapping = generateBasicHeaderMapping(request.getRequestHeaders());
            
            // 4. 构建响应
            ReverseParseRequest response = new ReverseParseRequest();
            response.setParamMapping(paramMapping);
            response.setHeaderMapping(headerMapping);
            
            return ResultData.ok(response);
        } catch (Exception e) {
            log.error("生成基础映射失败", e);
            return ResultData.fail("生成基础映射失败: " + e.getMessage());
        }
    }

    /**
     * 反向解析：从实际请求数据生成 paramItem 和 headerItem 配置
     *z
     * @param request 反向解析请求（包含请求 body、headers 和映射关系）
     * @return 生成的配置项列表
     */
    @PostMapping("/generate")
    public ResultData<ReverseParseResponse> generateConfig(@RequestBody ReverseParseRequest request) {
        try {
            // 1. 构建请求 body JSONObject
            JSONObject requestBody = new JSONObject(request.getRequestBody());

            // 2. 构建 paramItem 映射配置
            JSONObject paramMappingConfig = buildParamMappingConfig(request.getParamMapping());
            
            // 3. 构建 headerItem 映射配置
            JSONObject headerMappingConfig = buildHeaderMappingConfig(request.getHeaderMapping());

            // 4. 使用 ReverseDsl 进行反向解析
            ReverseDsl.Builder paramBuilder = ReverseDsl.build(requestBody, paramMappingConfig);
            JSONObject generatedParamItem = paramBuilder.getParam();

            ReverseDsl.Builder headerBuilder = ReverseDsl.build(requestBody, headerMappingConfig)
                    .withHeaders(request.getRequestHeaders());
            JSONObject generatedHeaderItem = headerBuilder.getHeader();

            // 5. 转换为响应 VO
            ReverseParseResponse response = new ReverseParseResponse();
            response.setParamItems(convertToConfigItemList(generatedParamItem.getJSONArray("paramItem")));
            response.setHeaderItems(convertToConfigItemList(generatedHeaderItem.getJSONArray("headerItem")));

            return ResultData.ok(response);
        } catch (Exception e) {
            log.error("反向解析失败", e);
            return ResultData.fail("反向解析失败: " + e.getMessage());
        }
    }

    /**
     * 构建 paramItem 映射配置 JSONObject
     */
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

    /**
     * 将 JSONArray 转换为 ConfigItem 列表
     */
    private List<ReverseParseResponse.ConfigItem> convertToConfigItemList(JSONArray array) {
        List<ReverseParseResponse.ConfigItem> items = new ArrayList<>();
        
        if (array != null) {
            for (int i = 0; i < array.size(); i++) {
                JSONObject itemObj = array.getJSONObject(i);
                ReverseParseResponse.ConfigItem item = new ReverseParseResponse.ConfigItem();
                item.setKey(itemObj.getString("key"));
                item.setCategory(itemObj.getString("category"));
                item.setNode(itemObj.getString("node"));
                item.setPostParam(itemObj.getString("post_param"));
                item.setSpelTemp(itemObj.getString("spel_temp"));
                item.setDefaultValue(itemObj.getString("default_value"));
                item.setValueObject(itemObj.getString("value_object"));
                item.setValidate(itemObj.getString("validate"));
                items.add(item);
            }
        }

        return items;
    }

    /**
     * 构建 headerItem 映射配置 JSONObject
     */
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

    /**
     * 根据 requestBody 生成基础的 paramMapping
     */
    private List<ReverseParseRequest.MappingConfig> generateBasicParamMapping(Map<String, Object> bodyMap) {
        List<ReverseParseRequest.MappingConfig> mappings = new ArrayList<>();
        
        if (bodyMap == null || bodyMap.isEmpty()) {
            return mappings;
        }
        
        // 使用 ReverseParser 收集所有字段路径
        Map<String, String> fieldPaths = ReverseParser.collectAllFieldPathsForMapping(bodyMap, "");
        
        // 为每个字段生成基础映射
        for (Map.Entry<String, String> entry : fieldPaths.entrySet()) {
            String key = entry.getKey();
            String nodePath = entry.getValue();
            
            ReverseParseRequest.MappingConfig mapping = new ReverseParseRequest.MappingConfig();
            mapping.setKey(key);
            mapping.setNode(nodePath);
            // postParam 从 node 路径中提取最后的字段名
            String postParam = extractPostParamFromNode(nodePath);
            mapping.setPostParam(postParam);
            mapping.setValidate(""); // 初始为空，用户后续添加
            
            mappings.add(mapping);
        }
        
        return mappings;
    }

    /**
     * 根据 requestHeaders 生成基础的 headerMapping
     */
    private List<ReverseParseRequest.MappingConfig> generateBasicHeaderMapping(Map<String, String> requestHeaders) {
        List<ReverseParseRequest.MappingConfig> mappings = new ArrayList<>();
        
        if (requestHeaders == null || requestHeaders.isEmpty()) {
            return mappings;
        }
        
        for (Map.Entry<String, String> entry : requestHeaders.entrySet()) {
            String headerName = entry.getKey();
            
            ReverseParseRequest.MappingConfig mapping = new ReverseParseRequest.MappingConfig();
            // 生成业务字段名：将 Header 名称转换为小写并移除特殊字符
            String key = headerName.toLowerCase().replaceAll("[^a-z0-9]", "");
            if (key.isEmpty()) {
                key = headerName;
            }
            mapping.setKey(key);
            mapping.setNode(headerName);
            mapping.setPostParam(headerName);
            mapping.setValidate(""); // 初始为空，用户后续添加
            
            mappings.add(mapping);
        }
        
        return mappings;
    }

    /**
     * 从 node 路径中提取 postParam（最后的字段名）
     * 例如：content[0].text -> text
     */
    private String extractPostParamFromNode(String nodePath) {
        if (nodePath == null || nodePath.isEmpty()) {
            return "";
        }
        
        // 如果包含点号，取最后一部分
        if (nodePath.contains(".")) {
            return nodePath.substring(nodePath.lastIndexOf(".") + 1);
        }
        
        // 如果包含数组索引，提取数组名
        if (nodePath.contains("[")) {
            int bracketIndex = nodePath.indexOf("[");
            return bracketIndex > 0 ? nodePath.substring(0, bracketIndex) : nodePath;
        }
        
        return nodePath;
    }

    /**
     * 保存配置：将生成的配置保存到数据库
     * 
     * @param request 保存请求（包含模型信息和编辑后的配置项）
     * @return 保存结果
     */
    @PostMapping("/save")
    public ResultData<String> saveConfig(@RequestBody ReverseParseRequest request) {
        try {
            // 1. 保存或更新模型基础信息
            DimAiModelMp model = new DimAiModelMp();
            model.setModelName(request.getModelName());
            model.setOriginName(request.getOriginName());
            model.setBaseUrl(request.getBaseUrl());
            model.setPoint(request.getPoint());
            model.setAuthorization(request.getAuthorization());
            model.setUpdatedTime(new Date());
            
            // 检查是否存在
            DimAiModelMp existingModel = dimAiModelMpService.getById(request.getModelName());
            if (existingModel != null) {
                // 更新
                dimAiModelMpService.updateById(model);
            } else {
                // 新增
                model.setCreatedTime(new Date());
                model.setDelFlag(0);
                dimAiModelMpService.save(model);
            }

            // 2. 删除该模型的所有旧配置项
            DimAiModelItemMp queryItem = new DimAiModelItemMp();
            queryItem.setModelName(request.getModelName());
            List<DimAiModelItemMp> existingItems = dimAiModelItemMpService.listEq(queryItem);
            if (existingItems != null && !existingItems.isEmpty()) {
                for (DimAiModelItemMp item : existingItems) {
                    dimAiModelItemMpService.removeById(item.getId());
                }
            }

            // 3. 保存编辑后的 paramItem 配置项
            if (request.getParamItems() != null && !request.getParamItems().isEmpty()) {
                for (int i = 0; i < request.getParamItems().size(); i++) {
                    ReverseParseRequest.ConfigItem configItem = request.getParamItems().get(i);
                    if (configItem == null || configItem.getKey() == null || configItem.getKey().isEmpty()) {
                        log.warn("跳过无效的 paramItem 配置项，索引: {}", i);
                        continue;
                    }
                    DimAiModelItemMp item = new DimAiModelItemMp();
                    item.setModelName(request.getModelName());
                    item.setItemType("param");
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

            // 4. 保存编辑后的 headerItem 配置项
            if (request.getHeaderItems() != null && !request.getHeaderItems().isEmpty()) {
                for (int i = 0; i < request.getHeaderItems().size(); i++) {
                    ReverseParseRequest.ConfigItem configItem = request.getHeaderItems().get(i);
                    if (configItem == null || configItem.getKey() == null || configItem.getKey().isEmpty()) {
                        log.warn("跳过无效的 headerItem 配置项，索引: {}", i);
                        continue;
                    }
                    DimAiModelItemMp item = new DimAiModelItemMp();
                    item.setModelName(request.getModelName());
                    item.setItemType("header");
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

            return ResultData.ok("配置保存成功");
        } catch (Exception e) {
            log.error("保存配置失败", e);
            return ResultData.fail("保存配置失败: " + e.getMessage());
        }
    }
}

