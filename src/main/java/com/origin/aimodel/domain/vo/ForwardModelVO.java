package com.origin.aimodel.domain.vo;

import lombok.Data;

/**
 * 正向请求下拉列表展示的模型信息
 */
@Data
public class ForwardModelVO {

    /**
     * 模型昵称
     */
    private String modelName;

    /**
     * 原始模型名称
     */
    private String originName;

    /**
     * 基础 URL
     */
    private String baseUrl;

    /**
     * 端点
     */
    private String point;

    /**
     * 预置鉴权信息
     */
    private String authorization;
}
