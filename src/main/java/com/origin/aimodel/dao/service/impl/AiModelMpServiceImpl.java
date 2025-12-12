package com.origin.aimodel.dao.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.origin.aimodel.dao.mapper.AiModelMapper;
import com.origin.aimodel.dao.service.AiModelItemMpService;
import com.origin.aimodel.domain.mp.AiModelMp;
import com.origin.aimodel.dao.service.AiModelMpService;
import com.origin.aimodel.domain.mp.AiModelItemMp;
import com.origin.aimodel.domain.vo.AiTaskQuery;
import com.origin.aimodel.domain.vo.AiTaskResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * (AiModel)服务实现类
 *
 * @author scccy
 * @since 2025-12-01 16:40:02
 */
@Service
public class AiModelMpServiceImpl
        extends ServiceImpl<AiModelMapper, AiModelMp>
        implements AiModelMpService {

    @Autowired
    private AiModelItemMpService aiModelItemMpService;

    @Override
    public AiTaskResult taskStart(AiTaskQuery aiTaskQuery) {
        // 查询主表数据
        AiModelMp model = this.lambdaQuery()
                .eq(AiModelMp::getModelName, aiTaskQuery.getModelName())
                .one();
        
        if (model == null) {
            return null;
        }
        
        // 构建 baseInfo JSON
        String baseInfo = buildBaseInfo(model);
        
        // 查询并构建 headerItem JSON
        List<AiModelItemMp> headerItems = aiModelItemMpService.getItemsByModelAndType(
                model.getModelName(), "header");
        String headerItemJson = buildItemJson(headerItems, "headerItem");
        
        // 查询并构建 paramItem JSON
        List<AiModelItemMp> paramItems = aiModelItemMpService.getItemsByModelAndType(
                model.getModelName(), "param");
        String paramItemJson = buildItemJson(paramItems, "paramItem");
        
        System.out.println("baseInfo: " + baseInfo);
        System.out.println("headerItem: " + headerItemJson);
        System.out.println("paramItem: " + paramItemJson);
        
        return null;
    }
    
    /**
     * 构建 baseInfo JSON 字符串
     */
    private String buildBaseInfo(AiModelMp model) {
        Map<String, Object> baseInfoMap = new HashMap<>();
        if (model.getBasUrl() != null) {
            baseInfoMap.put("base_url", model.getBasUrl());
        }
        if (model.getPoint() != null) {
            baseInfoMap.put("point", model.getPoint());
        }
        if (model.getAuthorization() != null) {
            baseInfoMap.put("Authorization", model.getAuthorization());
        }
        return JSON.toJSONString(baseInfoMap);
    }
    
    /**
     * 将配置项列表转换为 JSON 字符串
     *
     * @param items 配置项列表
     * @param rootKey JSON根key，如 "headerItem" 或 "paramItem"
     * @return JSON字符串
     */
    private String buildItemJson(List<AiModelItemMp> items, String rootKey) {
        JSONArray jsonArray = new JSONArray();
        for (AiModelItemMp item : items) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("key", item.getItemKey());
            jsonObject.put("category", item.getCategory());
            jsonObject.put("node", item.getNode());
            if (item.getPostParam() != null) {
                jsonObject.put("post_param", item.getPostParam());
            }
            if (item.getSpelTemp() != null) {
                jsonObject.put("spel_temp", item.getSpelTemp());
            }
            if (item.getDefaultValue() != null) {
                jsonObject.put("default_value", item.getDefaultValue());
            }
            if (item.getValueObject() != null) {
                jsonObject.put("value_object", item.getValueObject());
            }
            jsonArray.add(jsonObject);
        }
        JSONObject result = new JSONObject();
        result.put(rootKey, jsonArray);
        return result.toJSONString();
    }
}
