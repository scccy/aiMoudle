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
import java.util.Map;

@Slf4j
@Service
public class SpelDemo {
    @Autowired
    OkHttpManager okHttpManager;

    @Autowired
    AiModelMpService aiModelMpServiceImpl;


    public AiTaskResult taskStart(AiTaskQuery aiTaskQuery) throws IOException {


//        todo:
        // 兜底处理空对象，避免解析时出现 NPE
        AiTaskQuery safeQuery = aiTaskQuery == null ? new AiTaskQuery() : aiTaskQuery;
        if (safeQuery.getModelName() == null) {
            safeQuery.setModelName("default-model");
        }

        // --------------------- 原始数据：模拟数据库或前端输入 ---------------------
        SpelTemplateConfig templateConfig = loadModelTemplateConfig(safeQuery.getModelName());
        Map<String, Object> params = aiTaskQuery != null && aiTaskQuery.getParams() != null
                ? aiTaskQuery.getParams()
                : new HashMap<>();
        templateConfig
                .setModelName(safeQuery.getModelName())
                .setFrontPayloadJson(JSON.toJSONString(params))
                .addBuiltinContext("task", safeQuery);

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

        // --------------------- 提交阶段：可替换为真实 HTTP 调用 ---------------------
        postDemo(resolvedUrl, resolvedHeader, resolvedParam);
        return null;
    }

    /**
     * 根据模型名称加载模板配置（可替换为真实的数据库读取）
     */
    private SpelTemplateConfig loadModelTemplateConfig(String modelName) {
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
        JSONObject envJson = new JSONObject();
        envJson.put("model", "seedream-3.0");
        envJson.put("origin", "doubaoseedream-3-0-t2i-250415");
        envJson.put("baseUrl", "https://ark.cn-beijing.volces.com/api/v3/");
        envJson.put("endpoint", "generations/tasks");
        envJson.put("authorization", "Bearer 166ed6aa");

        JSONObject aliasMapping = new JSONObject();
        aliasMapping.put("a1", "text");
        aliasMapping.put("a2", "resolution");
        aliasMapping.put("a3", "ratio");
        aliasMapping.put("a4", "duration");
        aliasMapping.put("a5", "frames");
        aliasMapping.put("a6", "framesPerSecond");
        aliasMapping.put("a7", "seed");
        aliasMapping.put("a8", "cameraFixed");
        aliasMapping.put("a9", "watermark");

        JSONObject headerTemplate = new JSONObject();
        headerTemplate.put("Content-Type", "application/json");
        headerTemplate.put("Authorization", "#{#env['authorization']}");

        String contentExpression = "#{T(com.alibaba.fastjson2.JSON).parseArray('[{\"type\":\"text\",\"text\":\"'"
                + " + #payload['text']"
                + " + (#payload['resolution'] != null ? ' --resolution ' + #payload['resolution'] : '')"
                + " + (#payload['ratio'] != null ? ' --ratio ' + #payload['ratio'] : '')"
                + " + (#payload['duration'] != null ? ' --duration ' + #payload['duration'] : '')"
                + " + (#payload['frames'] != null ? ' --frames ' + #payload['frames'] : '')"
                + " + (#payload['framesPerSecond'] != null ? ' --framespersecond ' + #payload['framesPerSecond'] : '')"
                + " + (#payload['seed'] != null ? ' --seed ' + #payload['seed'] : '')"
                + " + (#payload['cameraFixed'] != null ? ' --camerafixed ' + #payload['cameraFixed'] : '')"
                + " + (#payload['watermark'] != null ? ' --watermark ' + #payload['watermark'] : '')"
                + " + '\"}]')}";

        JSONObject paramTemplate = new JSONObject();
        paramTemplate.put("model", "#{#env['model']}");
        paramTemplate.put("content", contentExpression);
        paramTemplate.put("resolution", "#{#payload['resolution']}");
        paramTemplate.put("ratio", "#{#payload['ratio']}");
        paramTemplate.put("duration", "#{#payload['duration']}");
        paramTemplate.put("frames", "#{#payload['frames']}");
        paramTemplate.put("frames_per_second", "#{#payload['framesPerSecond']}");
        paramTemplate.put("seed", "#{#payload['seed']}");
        paramTemplate.put("camera_fixed", "#{#payload['cameraFixed']}");
        paramTemplate.put("watermark", "#{#payload['watermark']}");

        return new SpelTemplateConfig()
                .setModelName("seedream-3.0")
                .setUrlTemplate("#{#env['baseUrl']}#{#env['endpoint']}")
                .setEnvJson(envJson.toJSONString())
                .setAliasMappingJson(aliasMapping.toJSONString())
                .setHeaderTemplateJson(headerTemplate.toJSONString())
                .setParamTemplateJson(paramTemplate.toJSONString());
    }

    public void postDemo(String url, Map<String, Object> header, Map<String, Object> param) throws IOException {
        log.info("模拟提交 -> URL:{} header:{} param:{}", url, header, param);
//        JSONObject post = okHttpManager.post(url, header, param);
//        log.info("返回: {}", post);
//        初始化
    }
}
