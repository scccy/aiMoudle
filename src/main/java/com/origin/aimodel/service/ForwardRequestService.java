package com.origin.aimodel.service;

import com.origin.aimodel.domain.vo.ForwardGenerateRequest;
import com.origin.aimodel.domain.vo.ForwardModelVO;
import com.origin.aimodel.domain.vo.ForwardRequestResult;

import java.util.List;

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
}
