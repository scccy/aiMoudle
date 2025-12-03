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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
        AiModelMp aiModelMp = aiModelMpServiceImpl.lambdaQuery().eq(AiModelMp::getModelName, modelName).one();
        if (aiModelMp == null) {
            throw new IllegalArgumentException("未找到模型配置: " + modelName);
        }

        Map<String, Object> envMock = new HashMap<>();
        envMock.put("apiUrl", aiModelMp.getBasUrl() + aiModelMp.getPoint());
        envMock.put("authorization", aiModelMp.getAuthorization());
        envMock.put("model", aiModelMp.getOriginName());

        return new SpelTemplateConfig()
                .setModelName(modelName)
                .setUrlTemplate("#{#env['apiUrl']}")
                .setEnvJson(JSON.toJSONString(envMock))
//                .setParamPlusJson(aiModelMp.getTemplateParamPlus())
                .setAliasMappingJson(aiModelMp.getTemplateAttributeMapping())
                .setHeaderTemplateJson(aiModelMp.getTemplateHeader())
                .setParamTemplateJson(aiModelMp.getTemplateSpel())
                ;
    }

    public void postDemo(String url, Map<String, Object> header, Map<String, Object> param) throws IOException {
        log.info("模拟提交 -> URL:{} header:{} param:{}", url, header, param);
//        JSONObject post = okHttpManager.post(url, header, param);
//        log.info("返回: {}", post);
//        初始化
    }
}
