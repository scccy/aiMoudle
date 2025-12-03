package com.origin.aimodel.util.spel;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 封装 SpEL 解析后的结构，便于业务层直接使用
 */
@Data
@Accessors(chain = true)
public class SpelEvaluationResult {

    private String resolvedUrl;

    private Map<String, Object> resolvedHeader = new LinkedHashMap<>();

    private Map<String, Object> resolvedParam = new LinkedHashMap<>();

    private Map<String, Object> payloadContext = new LinkedHashMap<>();

    private Map<String, Object> builtinContext = new LinkedHashMap<>();

    private Map<String, Object> contextDataSource = new LinkedHashMap<>();
}
