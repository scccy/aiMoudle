package com.origin.aimodel.domain.vo;

import lombok.Data;

/**
 * Item 详情响应 VO
 *
 * @author origin
 * @since 2025-12-14
 */
@Data
public class DictionaryItemDetailResponse {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 模型名称
     */
    private String modelName;

    /**
     * 配置项类型：header-请求头配置，param-请求体参数配置
     */
    private String itemType;

    /**
     * 业务入参的key名称
     */
    private String itemKey;

    /**
     * 处理类型：key、map、list
     */
    private String category;

    /**
     * 目标路径，支持点号和数组索引，如content[0].text
     */
    private String node;

    /**
     * 写入的目标字段名，为空则取node尾段
     */
    private String postParam;

    /**
     * 字符串模板，{value}会被替换为实际值
     */
    private String spelTemp;

    /**
     * 缺省值，当入参不存在时使用
     */
    private String defaultValue;

    /**
     * 类型提示：string、int、double、list<string>、list<json>、map
     */
    private String valueObject;

    /**
     * 值来源：CONST | USER | DERIVED
     */
    private String valueSource;

    /**
     * 校验规则，JSON 字符串格式
     */
    private String validate;

    /**
     * 排序顺序，用于保持配置项的顺序
     */
    private Integer sortOrder;
}
