package com.scccy.aimodel.service;

import com.scccy.aimodel.domain.vo.ForwardGenerateRequest;
import com.scccy.aimodel.domain.vo.ForwardModelVO;
import com.scccy.aimodel.domain.vo.ForwardRequestResult;

import java.util.List;
import java.util.Map;

/**
 * 正向请求相关服务
 */
public interface ForwardRequestService {

    /**
     * 查询可用模型列表（用于下拉框）
     */
    List<ForwardModelVO> listModels();

    /**
     * 根据模型配置和用户入参生成请求数据
     */
    ForwardRequestResult generateRequest(ForwardGenerateRequest request);

    /**
     * 获取模型的基础请求参数（根据 item 配置自动生成）
     * 根据 item 表的 key、category 生成基础请求参数，default_value 作为默认值
     *
     * @param modelName 模型名称
     * @return 基础请求参数对象
     */
    Map<String, Object> getModelBaseParams(String modelName);
}
