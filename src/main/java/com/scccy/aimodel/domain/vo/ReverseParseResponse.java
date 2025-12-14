package com.scccy.aimodel.domain.vo;

import lombok.Data;

import java.util.List;

/**
 * 反向解析响应 VO
 * 返回生成的 paramItem 和 headerItem 配置
 *
 * @author origin
 * @since 2025-12-13
 */
@Data
public class ReverseParseResponse {

    /**
     * 生成的 paramItem 配置列表
     */
    private List<MappingItem> paramItems;

    /**
     * 生成的 headerItem 配置列表
     */
    private List<MappingItem> headerItems;
}
