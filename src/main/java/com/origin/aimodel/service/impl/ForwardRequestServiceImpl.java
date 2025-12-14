package com.origin.aimodel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.origin.aimodel.dao.service.DimAiModelItemMpService;
import com.origin.aimodel.dao.service.DimAiModelMpService;
import com.origin.aimodel.domain.mp.DimAiModelItemMp;
import com.origin.aimodel.domain.mp.DimAiModelMp;
import com.origin.aimodel.domain.vo.ForwardGenerateRequest;
import com.origin.aimodel.domain.vo.ForwardModelVO;
import com.origin.aimodel.domain.vo.ForwardRequestResult;
import com.origin.aimodel.service.ForwardRequestService;
import com.origin.aimodel.util.spel.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.alibaba.fastjson2.JSON;

/**
 * 正向请求相关服务
 */
@Service
public class ForwardRequestServiceImpl implements ForwardRequestService {

    private final DimAiModelMpService dimAiModelMpService;
    private final DimAiModelItemMpService dimAiModelItemMpService;

    @Autowired
    public ForwardRequestServiceImpl(DimAiModelMpService dimAiModelMpService,
                                     DimAiModelItemMpService dimAiModelItemMpService) {
        this.dimAiModelMpService = dimAiModelMpService;
        this.dimAiModelItemMpService = dimAiModelItemMpService;
    }

    @Override
    public List<ForwardModelVO> listModels() {
        QueryWrapper<DimAiModelMp> wrapper = new QueryWrapper<>();
        wrapper.eq("del_flag", 0);
        List<DimAiModelMp> models = dimAiModelMpService.list(wrapper);
        return models.stream().map(this::toModelVO).collect(Collectors.toList());
    }

    @Override
    public ForwardRequestResult generateRequest(ForwardGenerateRequest request) {
        if (request == null || !StringUtils.hasText(request.getModelName())) {
            throw new IllegalArgumentException("modelName 不能为空");
        }

        DimAiModelMp model = dimAiModelMpService.getById(request.getModelName());
        if (model == null || (model.getDelFlag() != null && model.getDelFlag() == 1)) {
            throw new IllegalArgumentException("未找到模型配置：" + request.getModelName());
        }

        List<DimAiModelItemMp> items = loadModelItems(request.getModelName());
        PostDslConfig dslConfig = ReverseDslFactory.fromModel(model, items);

        // 合并 baseInfo 与前端 payload
        Map<String, Object> baseInfoMap = ConfigParser.parseBaseInfo(dslConfig.baseInfo());
        Map<String, Object> payload = CollectionUtils.isEmpty(request.getPayload())
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(request.getPayload());
        Map<String, Object> mergedPayload = new LinkedHashMap<>(baseInfoMap);
        mergedPayload.putAll(payload);

        List<MappingItem> headerEntries = PostDsl.parseConfig(dslConfig.headerItem());
        List<MappingItem> paramEntries = PostDsl.parseConfig(dslConfig.paramItem());

        ForwardRequestResult result = new ForwardRequestResult();

        result.setUrl(PayloadEngine.getUrl(mergedPayload));
        result.setHeaders(buildHeaders(headerEntries, mergedPayload));
        Map<String, Object> body = paramEntries.isEmpty()
                ? payload
                : PayloadEngine.buildResult(paramEntries, mergedPayload);
        result.setBody(body);

        return result;
    }

    private ForwardModelVO toModelVO(DimAiModelMp model) {
        ForwardModelVO vo = new ForwardModelVO();
        BeanUtils.copyProperties(model, vo);
        return vo;
    }

    private List<DimAiModelItemMp> loadModelItems(String modelName) {
        QueryWrapper<DimAiModelItemMp> wrapper = new QueryWrapper<>();
        wrapper.eq("model_name", modelName)
                .eq("del_flag", 0)
                .orderByAsc("sort_order")
                .orderByAsc("id");
        return dimAiModelItemMpService.list(wrapper);
    }

    private Map<String, Object> buildHeaders(List<MappingItem> entries,
                                             Map<String, Object> mergedPayload) {
        Map<String, Object> headers = new LinkedHashMap<>();
        for (MappingItem entry : entries) {
            Object value = mergedPayload.get(entry.getKey());
            if (value == null && StringUtils.hasText(entry.getDefaultValue())) {
                value = entry.getDefaultValue();
            }
            if (value == null) {
                continue;
            }
            String rendered = render(entry.getSpelTemp(), value);
            String headerName = StringUtils.hasText(entry.getPostParam()) ? entry.getPostParam() : entry.getNode();
            headers.put(headerName, rendered);
        }
        return headers;
    }

    private String render(String spelTemp, Object value) {
        if (!StringUtils.hasText(spelTemp)) {
            return value == null ? "" : value.toString();
        }
        String safeValue = value == null ? "" : value.toString();
        return spelTemp.replace("{value}", safeValue);
    }

    @Override
    public Map<String, Object> getModelBaseParams(String modelName) {
        if (!StringUtils.hasText(modelName)) {
            throw new IllegalArgumentException("modelName 不能为空");
        }

        // 查询该模型所有 param 类型的 item
        QueryWrapper<DimAiModelItemMp> wrapper = new QueryWrapper<>();
        wrapper.eq("model_name", modelName)
                .eq("item_type", "param")
                .eq("del_flag", 0)
                .orderByAsc("sort_order")
                .orderByAsc("id");

        List<DimAiModelItemMp> items = dimAiModelItemMpService.list(wrapper);

        // 根据 item 的 key、category 生成基础请求参数
        Map<String, Object> baseParams = new LinkedHashMap<>();
        
        for (DimAiModelItemMp item : items) {
            if (!StringUtils.hasText(item.getItemKey())) {
                continue;
            }
            
            Object defaultValue = null;
            String category = item.getCategory() != null ? item.getCategory() : "key";
            String valueObject = item.getValueObject() != null ? item.getValueObject() : "string";
            
            // 根据 category 确定参数结构类型
            if ("map".equals(category)) {
                // map 类型，初始化为空对象
                defaultValue = new LinkedHashMap<>();
            } else if ("list".equals(category)) {
                // list 类型，初始化为空数组
                defaultValue = new java.util.ArrayList<>();
            } else {
                // key 类型，根据 valueObject 和 defaultValue 确定默认值
                if (StringUtils.hasText(item.getDefaultValue())) {
                    defaultValue = parseDefaultValue(item.getDefaultValue(), valueObject);
                } else {
                    // 没有默认值，根据 valueObject 设置初始值
                    defaultValue = getInitialValueByType(valueObject);
                }
            }
            
            baseParams.put(item.getItemKey(), defaultValue);
        }
        
        return baseParams;
    }
    
    /**
     * 解析默认值
     */
    private Object parseDefaultValue(String defaultValue, String valueObject) {
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
    private Object getInitialValueByType(String valueObject) {
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
