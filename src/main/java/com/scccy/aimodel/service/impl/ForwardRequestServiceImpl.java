package com.scccy.aimodel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.scccy.aimodel.dao.service.DimAiModelItemMpService;
import com.scccy.aimodel.dao.service.DimAiModelMpService;
import com.scccy.aimodel.domain.mp.DimAiModelItemMp;
import com.scccy.aimodel.domain.mp.DimAiModelMp;
import com.scccy.aimodel.domain.vo.ForwardGenerateRequest;
import com.scccy.aimodel.domain.vo.ForwardModelVO;
import com.scccy.aimodel.domain.vo.ForwardRequestResult;
import com.scccy.aimodel.service.ForwardRequestService;
import com.scccy.aimodel.util.dsl.ForwardDsl;
import com.scccy.aimodel.util.dsl.ForwardDslFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 正向请求相关服务
 */
@Service
public class ForwardRequestServiceImpl implements ForwardRequestService {
    @Autowired
     DimAiModelMpService dimAiModelMpService;
    @Autowired
    DimAiModelItemMpService dimAiModelItemMpService;



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
        return ForwardDsl.build(request, model, items).generate();
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

        return ForwardDslFactory.buildBasePayloadByNode(items);
    }

}
