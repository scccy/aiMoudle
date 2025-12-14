package com.origin.aimodel;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.origin.aimodel.domain.vo.AiTaskQuery;
import com.origin.aimodel.domain.vo.ReverseParseRequest;
import com.origin.aimodel.domain.vo.ReverseParseResponse;
import com.origin.aimodel.service.ReverseParseService;
import com.origin.aimodel.util.spel.ReverseDsl;
import com.origin.aimodel.util.spel.demo.SpelDemo;
import com.origin.aimodel.util.spel.demo.SpelDemoNew;
import com.origin.aimodel.util.spel.demo.SpelDemoThreeNew;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest
class AiModelCallApplicationTests {

    @Autowired
    SpelDemo spelDemo;

    @Autowired
    SpelDemoNew spelDemoNew;
    
    @Autowired
    SpelDemoThreeNew spelDemoThreeNew;

    @Autowired
    ReverseParseService reverseParseService;

    @Test
    void testExampleOne() {
        AiTaskQuery query = new AiTaskQuery();
        Map<String, Object> payload = new HashMap<>();
        payload.put("contentType", "text");
        payload.put("prompt", "多个镜头。一名侦探进入一间光线昏暗的房间。他检查桌上的线索，手里拿起桌上的某个物品。镜头转向他正在思索。");
        payload.put("ratio", "16:9");
        payload.put("resolution", "1280x720");
        query.setModelName("chat12");
        query.setParams(payload);
        spelDemo.taskStart(query);
    }

    @Test
    void testExampleTwo() {
        AiTaskQuery query = new AiTaskQuery();
        query.setModelName("model2");
        Map<String, Object> payload = new HashMap<>();
        payload.put("prompt", "a futuristic city at night with neon lights");
        payload.put("negative_prompt", "low resolution, blurry, distorted face");
        payload.put("cfg_scale1", 7);
        payload.put("mode", "image");
        payload.put("type", "track");
        payload.put("config", "smooth");
        payload.put("horizontal", 10);
        payload.put("vertical", 5);
        payload.put("pan", 3);
        payload.put("tilt", 2);
        payload.put("roll", 0);
        payload.put("zoom", 1.2);
        payload.put("aspect_ratio", "16:9");
        payload.put("duration", 5);
        payload.put("callback_url", "https://example.com/callback");
        payload.put("external_task_id", "task-123456");
        query.setParams(payload);
        spelDemoNew.taskStart(query);
    }

    @Test
    void testExampleThree() {
        AiTaskQuery query = new AiTaskQuery();
        query.setModelName("doubao-seedance-1-0-lite-i2v-250428");
        Map<String, Object> payload = new HashMap<>();
        payload.put("prompt", "[图1]戴着眼镜穿着蓝色T恤的男生和[图2]的柯基小狗，坐在[图3]的草坪上，3D卡通风格");
        payload.put("ratio", "16:9");
        payload.put("resolution", "1280x720");
        // 使用三个Map表示图片参数
        Map<String, Object> image1 = new HashMap<>();
        image1.put("type", "image_url");
        HashMap<String, Object> urlMap1 = new HashMap<>();
        urlMap1.put("url", "https://ark-project.tos-cn-beijing.volces.com/doc_image/seelite_ref_1.png");
        image1.put("image_url", urlMap1);
        image1.put("role", "reference_image");
        payload.put("image1", image1);
        
        Map<String, Object> image2 = new HashMap<>();
        image2.put("type", "image_url");
        HashMap<String, Object> urlMap2 = new HashMap<>();
        urlMap2.put("url", "https://ark-project.tos-cn-beijing.volces.com/doc_image/seelite_ref_2.png");
        image2.put("image_url", urlMap2);
        image2.put("role", "reference_image");
        payload.put("image2", image2);
        
        Map<String, Object> image3 = new HashMap<>();
        image3.put("type", "image_url");
        HashMap<String, Object> urlMap3 = new HashMap<>();
        urlMap3.put("url", "https://ark-project.tos-cn-beijing.volces.com/doc_image/seelite_ref_3.png");
        image3.put("image_url", urlMap3);
        image3.put("role", "reference_image");
        payload.put("image3", image3);
        
        query.setParams(payload);
        spelDemoThreeNew.taskStart(query);
    }

    @Test
    void testExampleThreeNew() {
        AiTaskQuery query = new AiTaskQuery();
        query.setModelName("doubao-seedance-1-0-lite-i2v-250428");
        Map<String, Object> payload = new HashMap<>();
        payload.put("prompt", "戴着眼镜穿着蓝色T恤的男生和[图2]的柯基小狗");
        payload.put("ratio", "16:9");
        payload.put("resolution", "1280x720");
        // 使用列表方式表示图片参数，每个元素是一个Map，包含type、url和role字段
        List<Map<String, Object>> imageList = new ArrayList<>();
        
        Map<String, Object> image1 = new HashMap<>();
        image1.put("type", "image_url");
        image1.put("url", "https://ark-project.tos-cn-beijing.volces.com/doc_image/seelite_ref_1.png");
        image1.put("role", "reference_image");
        imageList.add(image1);
        
        Map<String, Object> image2 = new HashMap<>();
        image2.put("type", "image_url");
        image2.put("url", "https://ark-project.tos-cn-beijing.volces.com/doc_image/seelite_ref_2.png");
        image2.put("role", "reference_image");
        imageList.add(image2);
        
        Map<String, Object> image3 = new HashMap<>();
        image3.put("type", "image_url");
        image3.put("url", "https://ark-project.tos-cn-beijing.volces.com/doc_image/seelite_ref_3.png");
        image3.put("role", "reference_image");
        imageList.add(image3);
        
        payload.put("image_list", imageList);
        
        query.setParams(payload);
        spelDemoThreeNew.taskStart(query);
    }

    /**
     * 测试反向DSL解析功能
     * 
     * 该测试方法演示了如何使用ReverseDsl工具从实际的HTTP请求（包括body和header）
     * 反向生成paramItem和headerItem配置。这在需要根据已有的API请求示例自动生成
     * DSL配置时非常有用。
     * 
     * 测试流程：
     * 1. 构造一个模拟的实际请求（包括body和header）
     * 2. 定义paramItem和headerItem的映射关系配置
     * 3. 使用ReverseDsl工具进行反向解析，生成对应的配置项
     * 4. 输出结果以供验证
     */
    @Test
    void c() {
        // 模拟实际请求的 JSON body
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", "doubao-seedance-1-0-pro-250528");
        requestBody.put("temperature", 0.8);  // 添加数值字段用于 range 校验示例
        
        // 构建 content 数组
        List<JSONObject> contentList = new ArrayList<>();
        JSONObject contentItem = new JSONObject();
        contentItem.put("type", "text");
        contentItem.put("text", "多个镜头。一名侦探进入一间光线昏暗的房间。他检查桌上的线索，手里拿起桌上的某个物品。镜头转向他正在思索。 --ratio 16:9");
        contentList.add(contentItem);
        requestBody.put("content", contentList);

        // 模拟实际请求的 header
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("Content-Type", "application/json");
        requestHeaders.put("Authorization", "Bearer $ARK_API_KEY");

        // paramItem 映射关系配置：定义业务字段名到实际请求字段的映射
        // 反向解析会自动从请求 body 中发现 node 路径，只需要提供 key 和 post_param（目标字段名）
        // 可选的 validate 字段可以包含校验规则（参考 ConfigParser 和 ConfigValidator）
        JSONObject mappingModel = new JSONObject();
        mappingModel.put("key", "model");
        mappingModel.put("post_param", "model");  // 目标字段名，工具会自动发现 node 路径
        
        // 添加 validate 字段示例：数字区间校验
        JSONObject mappingTemperature = new JSONObject();
        mappingTemperature.put("key", "temperature");
        mappingTemperature.put("post_param", "temperature");  // 目标字段名，工具会自动发现 node 路径
        JSONObject temperatureValidate = new JSONObject();
        temperatureValidate.put("range", new com.alibaba.fastjson2.JSONArray().fluentAdd(0.0).fluentAdd(2.0));
        mappingTemperature.put("validate", temperatureValidate.toJSONString());  // validate 是 JSON 字符串格式
        
        JSONObject mappingType = new JSONObject();
        mappingType.put("key", "content_type");
        mappingType.put("post_param", "type");  // 目标字段名，工具会自动发现 node 为 content[0].type
        // 添加 validate 字段示例：枚举值校验
        JSONObject typeValidate = new JSONObject();
        typeValidate.put("enum", new com.alibaba.fastjson2.JSONArray().fluentAdd("text").fluentAdd("image_url"));
        mappingType.put("validate", typeValidate.toJSONString());  // validate 是 JSON 字符串格式
        
        JSONObject mappingRatio = new JSONObject();
        mappingRatio.put("key", "ratio");
        mappingRatio.put("post_param", "text");  // 目标字段名，工具会自动发现 node 为 content[0].text
        
        JSONObject mappingPrompt = new JSONObject();
        mappingPrompt.put("key", "prompt");
        mappingPrompt.put("post_param", "text");  // 目标字段名，工具会自动发现 node 为 content[0].text
        // 添加 validate 字段示例：最大长度校验
        JSONObject promptValidate = new JSONObject();
        promptValidate.put("maxLength", 2000);
        mappingPrompt.put("validate", promptValidate.toJSONString());  // validate 是 JSON 字符串格式

        JSONArray paramMappingArray = new JSONArray();
        paramMappingArray.add(mappingModel);
        paramMappingArray.add(mappingTemperature);  // 添加 temperature 映射配置（包含 range 校验）
        paramMappingArray.add(mappingType);
        paramMappingArray.add(mappingRatio);
        paramMappingArray.add(mappingPrompt);
        JSONObject paramMappingConfig = new JSONObject();
        paramMappingConfig.put("paramItem", paramMappingArray);

        // headerItem 映射关系配置
        // 对于 header，node 就是 header 字段名，可以直接指定或使用 post_param
        JSONObject mappingContentType = new JSONObject();
        mappingContentType.put("key", "contentTypeHeader");
        mappingContentType.put("post_param", "Content-Type");  // header 字段名
        
        JSONObject mappingAuthorization = new JSONObject();
        mappingAuthorization.put("key", "authorization");
        mappingAuthorization.put("post_param", "Authorization");  // header 字段名

        JSONArray headerMappingArray = new JSONArray();
        headerMappingArray.add(mappingContentType);
        headerMappingArray.add(mappingAuthorization);
        JSONObject headerMappingConfig = new JSONObject();
        headerMappingConfig.put("headerItem", headerMappingArray);

        // 反向解析：从实际请求 body + 映射关系生成 paramItem 配置（使用 build 模式）
        ReverseDsl.Builder reverseBuilder = ReverseDsl.build(requestBody, paramMappingConfig);
        JSONObject generatedParamItem = reverseBuilder.getParam();
        
        // 反向解析：从实际请求 header + 映射关系生成 headerItem 配置（使用 build 模式）
        ReverseDsl.Builder headerBuilder = ReverseDsl.build(requestBody, headerMappingConfig)
                .withHeaders(requestHeaders);
        JSONObject generatedHeaderItem = headerBuilder.getHeader();
        
        // 打印结果
        System.out.println("========== 实际请求 body ==========");
        System.out.println(requestBody.toJSONString());
        System.out.println("\n========== 实际请求 header ==========");
        System.out.println("Content-Type: " + requestHeaders.get("Content-Type"));
        System.out.println("Authorization: " + requestHeaders.get("Authorization"));
        System.out.println("\n========== paramItem 映射关系配置 ==========");
        System.out.println(paramMappingConfig.toJSONString());
        System.out.println("\n========== headerItem 映射关系配置 ==========");
        System.out.println(headerMappingConfig.toJSONString());
        System.out.println("\n========== 反向解析生成的 paramItem 配置 ==========");
        System.out.println(generatedParamItem.toJSONString());
        System.out.println("\n========== 反向解析生成的 headerItem 配置 ==========");
        System.out.println(generatedHeaderItem.toJSONString());
    }

    /**
     * 验证 Ark curl 示例在反向解析后能展开 content 列表的子字段，并带上 spel_temp
     */
    @Test
    void reverseParseArkContentList() {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "doubao-seedance-1-0-lite-i2v-250428");

        List<Map<String, Object>> contentList = new ArrayList<>();
        Map<String, Object> textPart = new HashMap<>();
        textPart.put("type", "text");
        textPart.put("text", "[图1]戴着眼镜穿着蓝色T恤的男生和[图2]的柯基小狗，坐在[图3]的草坪上，3D卡通风格");
        contentList.add(textPart);

        contentList.add(buildImagePart("https://ark-project.tos-cn-beijing.volces.com/doc_image/seelite_ref_1.png", "user3"));
        contentList.add(buildImagePart("https://ark-project.tos-cn-beijing.volces.com/doc_image/seelite_ref_2.png", "user2"));
        contentList.add(buildImagePart("https://ark-project.tos-cn-beijing.volces.com/doc_image/seelite_ref_3.png", "user1"));
        requestBody.put("content", contentList);

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Authorization", "Bearer $ARK_API_KEY");

        ReverseParseRequest request = new ReverseParseRequest();
        request.setRequestBody(requestBody);
        request.setRequestHeaders(headers);

        ReverseParseResponse response = reverseParseService.generateConfig(request);
        System.out.println("==== ParamItems ====");
        response.getParamItems().forEach(item -> System.out.println(JSONObject.toJSONString(item)));
        System.out.println("==== HeaderItems ====");
        response.getHeaderItems().forEach(item -> System.out.println(JSONObject.toJSONString(item)));
    }

    private Map<String, Object> buildImagePart(String url, String role) {
        Map<String, Object> imagePart = new HashMap<>();
        imagePart.put("type", "image_url");
        Map<String, Object> imageUrl = new HashMap<>();
        imageUrl.put("url", url);
        imagePart.put("image_url", imageUrl);
        imagePart.put("role", role);
        return imagePart;
    }
}
