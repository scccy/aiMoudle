package com.origin.aimodel.util.spel.demo;

import com.origin.aimodel.domain.vo.AiTaskQuery;
import com.origin.aimodel.util.spel.SpelDsl;
import com.origin.aimodel.util.spel.SpelDslConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 另一个示例：生成 text-to-video 请求的头/体/URL 预览。
 */
@Slf4j
@Service
public class SpelDemoNew {

    /**
     * 模拟从数据库读取的基础信息（含模型、鉴权与 URL 信息）。
     */
    public static final String BASE_INFO = """
            {
              "model": "sdxl-v1.2",
              "Authorization": "Bearer 1234422",
              "base_url": "https://api.klingai.com",
              "point": "/v1/text-to-video"
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
                "default_value": "Bearer ${TOKEN}",
                "value_object": "string"
              }
            }
            """;

    /**
     * 文本转视频 body 配置（参考 klingai textToVideo）。
     */
    public static final String TEXT_TO_VIDEO_PARAM_ITEM = """
            { "paramItem":
              {
                "key": "model_name",
                "category": "key",
                "node": "model_name",
                "post_param": "model_name",
                "value_object": "string"
              },
              {
                "key": "prompt",
                "category": "key",
                "node": "prompt",
                "post_param": "prompt",
                "value_object": "string"
              },
              {
                "key": "negative_prompt",
                "category": "key",
                "node": "negative_prompt",
                "post_param": "negative_prompt",
                "value_object": "string"
              },
              {
                "key": "cfg_scale",
                "category": "key",
                "node": "cfg_scale",
                "post_param": "cfg_scale",
                "value_object": "double"
              },
              {
                "key": "mode",
                "category": "key",
                "node": "mode",
                "post_param": "mode",
                "value_object": "string"
              },
              {
                "key": "type",
                "category": "key",
                "node": "camera_control.type",
                "post_param": "type",
                "value_object": "string"
              },
              {
                "key": "config",
                "category": "key",
                "node": "camera_control.config",
                "post_param": "config",
                "value_object": "string"
              },
              {
                "key": "horizontal",
                "category": "key",
                "node": "camera_control.horizontal",
                "post_param": "horizontal",
                "value_object": "int"
              },
              {
                "key": "vertical",
                "category": "key",
                "node": "camera_control.vertical",
                "post_param": "vertical",
                "value_object": "int"
              },
              {
                "key": "pan",
                "category": "key",
                "node": "camera_control.pan",
                "post_param": "pan",
                "value_object": "int"
              },
              {
                "key": "tilt",
                "category": "key",
                "node": "camera_control.tilt",
                "post_param": "tilt",
                "value_object": "int"
              },
              {
                "key": "roll",
                "category": "key",
                "node": "camera_control.roll",
                "post_param": "roll",
                "value_object": "int"
              },
              {
                "key": "zoom",
                "category": "key",
                "node": "camera_control.zoom",
                "post_param": "zoom",
                "value_object": "double"
              },
              {
                "key": "aspect_ratio",
                "category": "key",
                "node": "aspect_ratio",
                "post_param": "aspect_ratio",
                "value_object": "string"
              },
              {
                "key": "duration",
                "category": "key",
                "node": "duration",
                "post_param": "duration",
                "value_object": "int"
              },
              {
                "key": "callback_url",
                "category": "key",
                "node": "callback_url",
                "post_param": "callback_url",
                "value_object": "string"
              },
              {
                "key": "external_task_id",
                "category": "key",
                "node": "external_task_id",
                "post_param": "external_task_id",
                "value_object": "string"
              }
            }
            """;

    private static final SpelDslConfig CONFIG =
            new SpelDslConfig(BASE_INFO, HEADER_ITEM, TEXT_TO_VIDEO_PARAM_ITEM);

    public void taskStart(AiTaskQuery aiTaskQuery) {
        java.util.LinkedHashMap<String, Object> payload = new java.util.LinkedHashMap<>();
        if (aiTaskQuery.getParams() != null) {
            payload.putAll(aiTaskQuery.getParams());
        }
        if (aiTaskQuery.getModelName() != null) {
            payload.put("model_name", aiTaskQuery.getModelName());
        }

        SpelDsl.Runner runner = SpelDsl.Runner(CONFIG, payload);
        log.info("header preview:\n{}", runner.headerPreview());
        log.info("param preview:\n{}", runner.paramPreview());
        log.info("url preview: {}", runner.urlPreview());
    }
}
