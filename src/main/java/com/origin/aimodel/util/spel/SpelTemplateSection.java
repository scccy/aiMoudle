package com.origin.aimodel.util.spel;

/**
 * 标识 JSON 模板所属的功能片段，便于日志或开关控制
 */
public enum SpelTemplateSection {
    ENV,
    FRONT_PAYLOAD,
    ALIAS_MAPPING,
    HEADER_TEMPLATE,
    PARAM_TEMPLATE,
    CONTEXT_VARIABLE,
    CONTEXT_DATA,
    URL_TEMPLATE
}
