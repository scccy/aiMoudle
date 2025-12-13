package com.origin.aimodel.domain.vo;

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
    private List<ConfigItem> paramItems;

    /**
     * 生成的 headerItem 配置列表
     */
    private List<ConfigItem> headerItems;

    /**
     * 配置项
     */
    @Data
    public static class ConfigItem {
        private String key;
        private String category;
        private String node;
        private String postParam;
        private String spelTemp;
        private String defaultValue;
        private String valueObject;
        private String validate;
    }
}

