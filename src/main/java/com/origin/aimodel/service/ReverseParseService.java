package com.origin.aimodel.service;

import com.origin.aimodel.domain.vo.ReverseParseRequest;
import com.origin.aimodel.domain.vo.ReverseParseResponse;

/**
 * 反向解析业务接口
 */
public interface ReverseParseService {

    /**
     * 生成完整配置：根据 requestBody 和 requestHeaders 直接生成完整的配置项
     * 返回包含所有字段的 MappingItem 列表
     * 
     * @param request 反向解析请求（只需要 requestBody 和 requestHeaders）
     * @return 生成的完整配置项
     */
    ReverseParseResponse generateConfig(ReverseParseRequest request);

    /**
     * 保存模型与配置项
     */
    String saveConfig(ReverseParseRequest request);
}
