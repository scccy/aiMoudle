package com.origin.aimodel.util.spel;

import com.alibaba.fastjson2.JSON;
import com.origin.aimodel.config.OkHttpManager;
import com.origin.aimodel.dao.service.AiModelMpService;
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


        // --------------------- 原始数据：模拟数据库或前端输入 ---------------------
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
//        log.info("【转换结果】解析后URL={}", resolvedUrl);
//        log.info("【转换结果】解析后header={}", resolvedHeader);
//        log.info("【转换结果】解析后param={}", resolvedParam);

        // --------------------- 提交阶段：可替换为真实 HTTP 调用 ---------------------
        postDemo(resolvedUrl, resolvedHeader, resolvedParam);
        return null;
    }

    /**
     * 根据模型名称加载模板配置（可替换为真实的数据库读取）
     */
    private SpelTemplateConfig loadModelTemplateConfig(AiTaskQuery aiTaskQuery) {
        //        todo:
        // 兜底处理空对象，避免解析时出现 NPE
        AiTaskQuery safeQuery = aiTaskQuery == null ? new AiTaskQuery() : aiTaskQuery;
        if (safeQuery.getModelName() == null) {
            safeQuery.setModelName("default-model");
        }


//        实际数据库调用
//        AiModelMp aiModelMp = aiModelMpServiceImpl.lambdaQuery().eq(AiModelMp::getModelName, modelName).one();
//        if (aiModelMp == null) {
//            throw new IllegalArgumentException("未找到模型配置: " + modelName);
//        }
//
//        Map<String, Object> envMock = new HashMap<>();
//        envMock.put("apiUrl", aiModelMp.getBasUrl() + aiModelMp.getPoint());
//        envMock.put("authorization", aiModelMp.getAuthorization());
//        envMock.put("model", aiModelMp.getOriginName());
//
//        return new SpelTemplateConfig()
//                .setModelName(modelName)
//                .setUrlTemplate("#{#env['apiUrl']}")
//                .setEnvJson(JSON.toJSONString(envMock))
//                .setAliasMappingJson(aiModelMp.getTemplateAttributeMapping())
//                .setHeaderTemplateJson(aiModelMp.getTemplateHeader())
//                .setParamTemplateJson(aiModelMp.getTemplateSpel());


        // --------------------- 本地字符串模拟 ---------------------
        // 直接使用 docs/test.md 中 seedream-3.0 的实际数据，避免依赖数据库
        String envJson = "{"
                + "\"model\":\"seedream-3.0\","
                + "\"origin\":\"doubaoseedream-3-0-t2i-250415\","
                + "\"baseUrl\":\"https://ark.cn-beijing.volces.com/api/v3/\","
                + "\"endpoint\":\"generations/tasks\","
                + "\"authorization\":\"Bearer 166ed6aa\""
                + "}";

        String headerTemplateJson = "{"
                + "\"Content-Type\":\"application/json\","
                + "\"Authorization\":\"#{#env['authorization']}\""
                + "}";

        // 前端别名 -> 实际字段映射，模拟数据库中的 JSON 字符串（保持 Java 8 兼容拼接）
        String aliasMappingJson = "{"
                + "\"a1\":\"text\","
                + "\"a2\":\"resolution\","
                + "\"a3\":\"ratio\","
                + "\"a4\":\"duration\","
                + "\"a5\":\"frames\","
                + "\"a6\":\"framesPerSecond\","
                + "\"a7\":\"seed\","
                + "\"a8\":\"cameraFixed\","
                + "\"a9\":\"watermark\""
                + "}";


        // 参数模板保持“数据库存的纯字符串”，完全按给定的 SpEL 规则写成 JSON 字符串
        String paramTemplateJson = "{"
                + "\"model\":\"#{#env['origin']}\","
                + "\"content\":\"#{T(com.alibaba.fastjson2.JSON).parseArray('[{\\\\\"type\\\\\":\\\\\"text\\\\\",\\\\\"text\\\\\":\\\\\"' "
                + "                + #payload['text']"
                + "                + (#payload['resolution'] != null ? ' --resolution ' + #payload['resolution'] : '')"
                + "                + (#payload['ratio'] != null ? ' --ratio ' + #payload['ratio'] : '')"
                + "                + (#payload['duration'] != null ? ' --duration ' + #payload['duration'] : '')"
                + "                + (#payload['frames'] != null ? ' --frames ' + #payload['frames'] : '')"
                + "                + (#payload['framesPerSecond'] != null ? ' --framespersecond ' + #payload['framesPerSecond'] : '')"
                + "                + (#payload['seed'] != null ? ' --seed ' + #payload['seed'] : '')"
                + "                + (#payload['cameraFixed'] != null ? ' --camerafixed ' + #payload['cameraFixed'] : '')"
                + "                + (#payload['watermark'] != null ? ' --watermark ' + #payload['watermark'] : '')"
                + "                + '\\\\\"}]')}\","
                + "\"resolution\":\"#{#payload['resolution']}\","
                + "\"ratio\":\"#{#payload['ratio']}\","
                + "\"duration\":\"#{#payload['duration']}\","
                + "\"frames\":\"#{#payload['frames']}\","
                + "\"frames_per_second\":\"#{#payload['framesPerSecond']}\","
                + "\"seed\":\"#{#payload['seed']}\","
                + "\"camera_fixed\":\"#{#payload['cameraFixed']}\","
                + "\"watermark\":\"#{#payload['watermark']}\""
                + "}";

        String paramTemplateJsonPlus = null;
        String aliasMappingJsonPlus = null;
        // 本地追加模拟（增强1示例）：切换模型 + 文本强化 + 追加 image_url
//        paramTemplateJsonPlus = "{"
////                + "\"model\":\"doubao-seedance-1-0-pro-fast-251015\","
//                + "\"content\":\"#{T(com.alibaba.fastjson2.JSON).parseArray('[{\\\\\"type\\\\\":\\\\\"image_url\\\\\",\\\\\"image_url\\\\\":{\\\\\"url\\\\\":\\\\\"' + #payload['imageUrl'] + '\\\\\"}}]')}\""
//                + "}";
//        aliasMappingJsonPlus = "{"
//                + "\"a12\":\"imageUrl\""
//                + "}";

        return new SpelTemplateConfig()
                .setModelName("seedream-3.0")
                .setUrlTemplate("#{#env['baseUrl']}#{#env['endpoint']}")
                .setEnvJson(envJson)
                .setAliasMappingJson(aliasMappingJson)
                .setAliasMappingJsonPlus(aliasMappingJsonPlus)
                .setHeaderTemplateJson(headerTemplateJson)
                .setParamTemplateJson(paramTemplateJson)
                .setParamTemplateJsonPlus(paramTemplateJsonPlus);
    }

    public void postDemo(String url, Map<String, Object> header, Map<String, Object> param) throws IOException {
//        log.info("模拟提交 -> URL:{} header:{} param:{}", url, header, param);
        String jsonParam = JSON.toJSONString(param);
        String headerParam = JSON.toJSONString(header);
        String urlParam = JSON.toJSONString(url);
        log.info("url:{}", urlParam);
        log.info("header:{}", headerParam);
        log.info("param:{}", jsonParam);
//        JSONObject post = okHttpManager.post(url, header, param);
//        log.info("返回: {}", post);
//        初始化
    }
}
