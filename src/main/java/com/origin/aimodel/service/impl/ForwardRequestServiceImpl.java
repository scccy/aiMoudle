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
import java.util.Set;
import java.util.HashSet;
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

        return buildBasePayloadByNode(items);
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

    /**
     * 根据 node + category 还原层级结构
     */
    private Map<String, Object> buildBasePayloadByNode(List<DimAiModelItemMp> items) {
        Map<String, Object> root = new LinkedHashMap<>();
        if (CollectionUtils.isEmpty(items)) {
            return root;
        }

        // 记录哪些路径是 list / map，用于构造容器
        Set<String> listPaths = new HashSet<>();
        Set<String> mapPaths = new HashSet<>();
        for (DimAiModelItemMp item : items) {
            String path = resolvePath(item);
            String category = item.getCategory() != null ? item.getCategory() : "key";
            if (!StringUtils.hasText(path)) {
                continue;
            }
            if ("list".equalsIgnoreCase(category)) {
                listPaths.add(path);
            } else if ("map".equalsIgnoreCase(category)) {
                mapPaths.add(path);
            }
        }

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
            applyValueByPath(root, path, value, listPaths, mapPaths);
        }

        return root;
    }

    private String resolvePath(DimAiModelItemMp item) {
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
    private void applyValueByPath(Map<String, Object> root,
                                  String path,
                                  Object value,
                                  Set<String> listPaths,
                                  Set<String> mapPaths) {
        String[] parts = path.split("\\.");
        Map<String, Object> current = root;
        StringBuilder acc = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (acc.length() > 0) {
                acc.append(".").append(part);
            } else {
                acc.append(part);
            }
            boolean isLast = (i == parts.length - 1);
            String accPath = acc.toString();

            if (isLast) {
                current.put(part, value);
                return;
            }

            if (listPaths.contains(accPath)) {
                Object container = current.get(part);
                if (!(container instanceof List)) {
                    container = new java.util.ArrayList<>();
                    current.put(part, container);
                }
                List<?> list = (List<?>) container;
                if (list.isEmpty()) {
                    list = new java.util.ArrayList<>();
                    ((List<Object>) container).add(new LinkedHashMap<>());
                }
                Object first = ((List<?>) container).get(0);
                if (!(first instanceof Map)) {
                    first = new LinkedHashMap<>();
                    ((List<Object>) container).set(0, first);
                }
                current = (Map<String, Object>) first;
            } else {
                Object container = current.get(part);
                if (!(container instanceof Map)) {
                    container = new LinkedHashMap<>();
                    current.put(part, container);
                }
                current = (Map<String, Object>) container;
            }
        }
    }

}
