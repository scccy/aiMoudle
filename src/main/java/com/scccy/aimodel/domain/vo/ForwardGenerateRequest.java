package com.scccy.aimodel.domain.vo;

import lombok.Data;

import java.util.Map;

/**
 * 正向请求生成的入参 VO
 */
@Data
public class ForwardGenerateRequest {

    /**
     * 模型名称（关联 dim_ai_model.model_name）
     */
    private String modelName;

    /**
     * 用户输入的业务参数
     */
    private Map<String, Object> payload;
}
