package com.scccy.aimodel.util.dsl;

/**
 * SpEL DSL 配置载体，封装 baseInfo/headerItem/paramItem 字符串。
 */
public record PostDslConfig(String baseInfo, String headerItem, String paramItem) {
}
