package com.origin.aimodel.util.spel.demo;

import com.origin.aimodel.domain.vo.AiTaskQuery;
import com.origin.aimodel.util.spel.PostDsl;
import com.origin.aimodel.util.spel.PostDslConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 样式三示例（新引擎版本）：图生视频请求的头/体/URL预览。
 */
@Slf4j
@Service
public class SpelDemoThreeNew {

    /**
     * 模拟从数据库读取的基础信息（含模型、鉴权与 URL 信息）。
     */
    public static final String BASE_INFO = """
            {
              "model": "doubao-seedance-1-0-lite-i2v-250428",
              "Authorization": "Bearer ahudhasw2xx",
              "base_url": "https://ark.cn-beijing.volces.com",
              "point": "/api/v3/contents/generations/tasks"
            }
            """;

    /**
     * 请求头配置。
     */
    public static final String HEADER_ITEM = """
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
     * 图生视频 body 配置（使用新的list处理方式）。
     */
    public static final String IMAGE_TO_VIDEO_PARAM_ITEM = """
            { "paramItem":
              {
                "key": "model",
                "category": "key",
                "node": "model",
                "post_param": "model",
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
              },
              {
                "key": "content_type",
                "category": "key",
                "node": "content[0].type",
                "post_param": "type",
                "default_value": "text",
                "value_object": "string"
              },
              {
                "key": "prompt",
                "category": "key",
                "node": "content[0].text",
                "post_param": "text",
                "value_object": "string"
              },
              {
                "key": "image_list",
                "category": "list",
                "node": "content",
                "spel_temp": "{"paramItem":[{"key":"type","node":"type","post_param":"type","default_value":"image_url","value_object":"string"},{"key":"url","node":"image_url.url","post_param":"url","value_object":"string"},{"key":"role","node":"role","post_param":"role","default_value":"reference_image","value_object":"string"}]}",
                "value_object": "list<json>"
              }
            }
            """;

    private static final PostDslConfig CONFIG =
            new PostDslConfig(BASE_INFO, HEADER_ITEM, IMAGE_TO_VIDEO_PARAM_ITEM);

    public void taskStart(AiTaskQuery aiTaskQuery) {
        java.util.LinkedHashMap<String, Object> payload = new java.util.LinkedHashMap<>();
        if (aiTaskQuery.getParams() != null) {
            payload.putAll(aiTaskQuery.getParams());
        }
        if (aiTaskQuery.getModelName() != null) {
            payload.put("model", aiTaskQuery.getModelName());
        }

        PostDsl.Builder builder = PostDsl.build(CONFIG, payload);
        log.info("NEW ENGINE - header preview:\n{}", builder.getHeader());
        log.info("NEW ENGINE - param preview:\n{}", builder.getParam());
        log.info("NEW ENGINE - url preview: {}", builder.getUrl());
    }
}
