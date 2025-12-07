package com.origin.aimodel.util.spel;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.origin.aimodel.config.OkHttpManager;
import com.origin.aimodel.dao.service.AiModelMpService;
import com.origin.aimodel.domain.mp.AiModelMp;
import com.origin.aimodel.domain.vo.AiTaskQuery;
import com.origin.aimodel.domain.vo.AiTaskResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
public class SpelDemo {
    @Autowired
    OkHttpManager okHttpManager;

    @Autowired
    AiModelMpService aiModelMpServiceImpl;


    public AiTaskResult taskStart(AiTaskQuery aiTaskQuery) throws IOException {


        // --------------------- 原始数据：模拟数据库或前端输入（addkey/addlist/addmap） ---------------------
        SpelTemplateConfig templateConfig = loadModelTemplateConfig(aiTaskQuery);
        Map<String, Object> params = aiTaskQuery != null && aiTaskQuery.getParams() != null
                ? aiTaskQuery.getParams()
                : new HashMap<>();
        templateConfig
                .setFrontPayloadJson(JSON.toJSONString(params));
//                .addBuiltinContext("task", safeQuery);

        // --------------------- SpEL 解析：模板 -> 实际值 ---------------------
        SpelTemplateEngine templateEngine = new SpelTemplateEngine();
        SpelEvaluationResult evaluationResult = templateEngine.evaluate(templateConfig);
        String resolvedUrl = evaluationResult.getResolvedUrl();
        Map<String, Object> resolvedHeader = evaluationResult.getResolvedHeader();
        Map<String, Object> resolvedParam = evaluationResult.getResolvedParam();
        Map<String, Object> payloadContext = evaluationResult.getPayloadContext();
        Map<String, Object> contextDataSource = evaluationResult.getContextDataSource();

        // --------------------- 转换结果：打印分段日志 ---------------------
//        log.info("【原始数据】config={}", templateConfig);
//        log.info("【解析后Map】payloadContext={}, contextDataSource={}", payloadContext, contextDataSource);
        log.info("【转换结果】解析后URL={}", resolvedUrl);
        log.info("【转换结果】解析后header={}", resolvedHeader);
        log.info("【转换结果】解析后param={}", resolvedParam);
        log.info("【转换结果】解析后contextDataSource={}", contextDataSource);

        // --------------------- 提交阶段：可替换为真实 HTTP 调用 ---------------------
        postDemo(resolvedUrl, resolvedHeader, resolvedParam);
        return null;
    }

    /**
     * 根据模型名称加载模板配置（可替换为真实的数据库读取）
     */
    private SpelTemplateConfig loadModelTemplateConfig(AiTaskQuery aiTaskQuery) {
//        //
//        // 兜底处理空对象，避免解析时出现 NPE
//        AiTaskQuery safeQuery = aiTaskQuery == null ? new AiTaskQuery() : aiTaskQuery;
//        if (safeQuery.getModelName() == null) {
//            safeQuery.setModelName("default-model");
//        }
//
//
////        实际数据库调用
//        AiModelMp aiModelMp = aiModelMpServiceImpl.lambdaQuery().eq(AiModelMp::getModelName, safeQuery.getModelName()).one();
//        if (aiModelMp == null) {
//            throw new IllegalArgumentException("未找到模型配置: " + safeQuery.getModelName());
//        }
//
//
//
//        JSONObject envJson =  new JSONObject();
//        envJson.put("model",  aiModelMp.getOriginName());
//        envJson.put("origin", aiModelMp.getOriginName());
//        envJson.put("baseUrl", aiModelMp.getBasUrl());
//        envJson.put("endpoint", aiModelMp.getPoint());
//        envJson.put("authorization", aiModelMp.getAuthorization());
//
//
//        return new SpelTemplateConfig()
//                .setModelName(aiModelMp.getOriginName())
//                .setUrlTemplate("#{#env['baseUrl']}#{#env['endpoint']}")
//                .setEnvJson(envJson.toString())
//                .setAliasMappingJson(aiModelMp.getTemplateAttributeMapping())
//                .setAliasMappingJsonPlus(aiModelMp.getAliasMappingJsonPlus())
//                .setParamTemplateJsonPlus(aiModelMp.getTemplateParamTJsonPlus())
//                .setHeaderTemplateJson(aiModelMp.getTemplateHeader())
//                .setParamTemplateJson(aiModelMp.getTemplateSpel());


        // --------------------- 本地字符串模拟 ---------------------
        // 直接使用 docs/test.md 中 seedream-3.0 的实际数据，避免依赖数据库
//        String envJson = null;
//             envJson = "{"
//                    + "\"model\":\"chat1\","
//                    + "\"origin\":\"origin-chat1\","
//                    + "\"baseUrl\":\"https://ark.cn-beijing.volces.com/api/v3/\","
//                    + "\"endpoint\":\"generations/tasks\","
//                    + "\"authorization\":\"Bearer 166ed6aa\""
//                    + "}";


        JSONObject envJson = new JSONObject();
        envJson.put("model", "video1");
        envJson.put("origin", "doubao-seedance-1-0-pro-250528");
        envJson.put("model_plus", "doubao-seedance-1-0-lite-i2v-250428");
        envJson.put("baseUrl", "https://ark.cn-beijing.volces.com/api/v3/");
        envJson.put("endpoint", "contents/generations/tasks");
        envJson.put("authorization", "Bearer 166ed6aa");


        String headerTemplateJson = """
                {
                  "Content-Type": "application/json",
                  "Authorization": "#{#env['authorization']}"
                }""";

//         前端别名 -> 实际字段映射，模拟数据库中的 JSON 字符串（保持 Java 8 兼容拼接）
        String aliasMappingJson = """
                {
                  "a1": "text",
                  "a2": "resolution",
                  "a3": "ratio",
                  "a4": "duration",
                  "a5": "frames",
                  "a6": "framesPerSecond",
                  "a7": "seed",
                  "a8": "cameraFixed",
                  "a9": "watermark"
                }
                """;


//         参数模板保持“数据库存的纯字符串”，完全按给定的 SpEL 规则写成 JSON 字符串
        String paramTemplateJson = """
                {
                  "model": "#{#env['origin']}",
                  "content": "#{T(com.alibaba.fastjson2.JSON).parseArray('[{\\"type\\":\\"text\\",\\"text\\":\\"' 
                                + #payload['text']
                                + (#payload['resolution'] != null ? ' --resolution ' + #payload['resolution'] : '')
                                + (#payload['ratio'] != null ? ' --ratio ' + #payload['ratio'] : '')
                                + (#payload['duration'] != null ? ' --duration ' + #payload['duration'] : '')
                                + (#payload['frames'] != null ? ' --frames ' + #payload['frames'] : '')
                                + (#payload['framesPerSecond'] != null ? ' --framespersecond ' + #payload['framesPerSecond'] : '')
                                + (#payload['seed'] != null ? ' --seed ' + #payload['seed'] : '')
                                + (#payload['cameraFixed'] != null ? ' --camerafixed ' + #payload['cameraFixed'] : '')
                                + (#payload['watermark'] != null ? ' --watermark ' + #payload['watermark'] : '')
                                + '\\"}]')}",
                  "resolution": "#{#payload['resolution']}",
                  "ratio": "#{#payload['ratio']}",
                  "duration": "#{#payload['duration']}",
                  "frames": "#{#payload['frames']}",
                  "frames_per_second": "#{#payload['framesPerSecond']}",
                  "seed": "#{#payload['seed']}",
                  "camera_fixed": "#{#payload['cameraFixed']}",
                  "watermark": "#{#payload['watermark']}"
                }
                """;
        String paramTemplateJsonPlus = null;
        String aliasMappingJsonPlus = null;
        String aliasPatchJson = null;
        String paramPatchTemplateJson = null;
        String arrayPatchTemplateJson = null;

//         增强1：单图追加（保留原逻辑，注释停用）
//        paramTemplateJsonPlus = """
//                {
//                  "content": "#{T(com.alibaba.fastjson2.JSON).parseArray(
//                  '[{\\"type\\":\\"image_url\\",
//                  \\"image_url\\":{\\"url\\":\\"' + #payload['imageUrl'] + '\\"}}]')}"
//                }
//                """;
//        aliasMappingJsonPlus = """
//               {
//               "a12": "imageUrl"
//               }
//               """;

//         增强2：首/尾帧（保留原逻辑，注释停用）
//        paramTemplateJsonPlus = """
//                {
//                    "content": "#{T(com.alibaba.fastjson2.JSON).parseArray('
//                    [{\\"type\\":\\"image_url\\",\\"image_url\\":{\\"url\\":\\"' + #payload['imageUrlFirst'] + '\\"},
//                    \\"role\\":\\"first_frame\\"},
//                    {\\"type\\":\\"image_url\\",\\"image_url\\":{\\"url\\":\\"' + #payload['imageUrlLast'] + '\\"},
//                    \\"role\\":\\"last_frame\\"}]')}"
//                }
//                """;
//        aliasMappingJsonPlus = """
//                {
//                    "a12": "imageUrlFirst",
//                    "a13": "imageUrlLast"
//                }
//                """;
//      增强3
        aliasPatchJson = """
                {
                  "a12": "refImages",
                  "a15": "model_plus"
                }
                """;
         paramPatchTemplateJson = """
                {
                  "model": "#{#payload['model_plus'] ?: #env['model_plus'] ?: 'doubao-seedance-1-0-lite-i2v-250428'}"
                }
                """;
         arrayPatchTemplateJson = """
                #{T(com.alibaba.fastjson2.JSON).parseArray(T(com.alibaba.fastjson2.JSON).toJSONString(#payload['refImages']))}
                """;

        return new SpelTemplateConfig()
                .setModelName("seedream-3.0")
                .setUrlTemplate("#{#env['baseUrl']}#{#env['endpoint']}")
                .setEnvJson(envJson.toString())
                .setAliasMappingJson(aliasMappingJson)
                .setAddKeyAliasPatchJson(aliasPatchJson)
                .setHeaderTemplateJson(headerTemplateJson)
                .setParamTemplateJson(paramTemplateJson)
                .setAddKeyParamPatchJson(paramPatchTemplateJson)
                .setAddListTemplateJson(arrayPatchTemplateJson)
                .setAddListTargetKey("content")
                .setAddMapTemplateJson("{\"c1\":{\"d1\":\"xxx\",\"d2\":[1,2,3]}}")
                .setAddMapStrategy("AUTO");
    }

    public void postDemo(String url, Map<String, Object> header, Map<String, Object> param) throws IOException {
        String jsonParam = JSON.toJSONString(param);
        String headerParam = JSON.toJSONString(header);
        String urlParam = JSON.toJSONString(url);
        log.info("-----------模拟实际请求-----------");
        log.info("url:{}", urlParam);
        log.info("header:{}", headerParam);
        log.info("param:{}", jsonParam);
//        JSONObject post = okHttpManager.post(url, header, param);
//        log.info("post:{}", post);
//        log.info("返回: {}", post);
//        初始化
    }
}
