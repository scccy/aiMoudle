package com.origin.aimodel.util.spel.demo;

import com.origin.aimodel.domain.vo.AiTaskQuery;
import com.origin.aimodel.util.spel.PostDsl;
import com.origin.aimodel.util.spel.PostDslConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * SpEL 配置示例，仅包含配置项字符串，不做任何解析逻辑。
 * 具体解析/预览逻辑委托给 {@link PostDsl}。
 */
@Slf4j
@Service
public final class SpelDemo {


    /**
     * 模拟从数据库读取的模型基础数据。
     */
    public static final String BASE_INFO = """
            {
              "model": "chat1",
              "Authorization": "Bearer ahudhasw2xx",
              "base_url":"https://www.baidu.com",
              "point":"/point1/video"
            }
            """;

    /**
     * 示例一：将 Doubao Seedance 文生视频的 curl 请求拆解为 headerItem（头部配置）部分（无解析逻辑）。
     */
    public static final String TEXT_GENERATION_HEADER_ITEM = """
            { "headerItem":
              {
                "key": "contentTypeHeader",
                "category": "key",
                "node": "Content-Type",
                "post_param": "Content-Type",
                "default_value": "application/json",
                "value_object": "string"
              },
              {
                "key": "authorization",
                "category": "key",
                "node": "Authorization",
                "post_param": "Authorization",
                "default_value": "Bearer ${ARK_API_KEY}",
                "value_object": "string"
              }
            }
            """;

    /**
     * 示例一：对应的 paramItem（请求体配置）部分（无解析逻辑）。
     */
    public static final String TEXT_GENERATION_PARAM_ITEM = """
            { "paramItem":
              {
                "key": "model",
                "category": "key",
                "node": "model",
                "post_param": "model_name",
                "value_object": "string"
              },
              {
                "key": "contentType",
                "category": "key",
                "node": "content[0]",
                "post_param": "type",
                "default_value": "text",
                "value_object": "string"
              },
              {
                "key": "prompt",
                "category": "key",
                "node": "content[0].text",
                "post_param": "text",
                "spel_temp": "{value}",
                "value_object": "string"
              },
              {
                "key": "ratio",
                "category": "key",
                "node": "content[0].text",
                "post_param": "text",
                "spel_temp": " --ratio {value}",
                "value_object": "string"
              },
              {
                "key": "resolution",
                "category": "key",
                "node": "content[0].text",
                "post_param": "text",
                "spel_temp": " --resolution {value}",
                "value_object": "string"
              }
            }
            """;

    private static final PostDslConfig CONFIG =
            new PostDslConfig(BASE_INFO, TEXT_GENERATION_HEADER_ITEM, TEXT_GENERATION_PARAM_ITEM);

    public void taskStart(AiTaskQuery aiTaskQuery) {
        java.util.LinkedHashMap<String, Object> payload = new java.util.LinkedHashMap<>();
        if (aiTaskQuery.getParams() != null) {
            payload.putAll(aiTaskQuery.getParams());
        }
        if (aiTaskQuery.getModelName() != null) {
            payload.put("model", aiTaskQuery.getModelName());
        }

        PostDsl.Builder builder = PostDsl.build(CONFIG, payload);

        String headerPreview = builder.getHeader();
        String paramPreview = builder.getParam();
        String urlPreview = builder.getUrl();

        log.info("header preview:\n{}", headerPreview);
        log.info("param preview:\n{}", paramPreview);
        log.info("url preview: {}", urlPreview);
    }


}
