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

}
