package com.origin.aimodel.util.spel;

/**
 * SpEL DSL 配置载体，封装 baseInfo/headerItem/paramItem 字符串。
 */
public record SpelDslConfig(String baseInfo, String headerItem, String paramItem) {
}
