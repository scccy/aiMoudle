package com.origin.aimodel;

import com.origin.aimodel.domain.vo.AiTaskQuery;
import com.origin.aimodel.util.spel.SpelDemo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SpringBootTest
class AiModelCallApplicationTests {

    @Autowired
    SpelDemo spelDemo;

    @Test
    void contextLoads() throws IOException {
        AiTaskQuery query = new AiTaskQuery();
        query.setModelName("context-load-demo");
        spelDemo.taskStart(query);
    }

    @Test
    void spelTemplatePreview() throws IOException {
        AiTaskQuery query = new AiTaskQuery();
        query.setModelName("video2");
        Map<String, Object> requestPayload = new HashMap<>();
//        // 旧版增强2模拟：文本 + 分辨率参数 + 首/尾帧
//        requestPayload.put("a1", "小猫在后空翻");
//        requestPayload.put("a4", 8);
//        requestPayload.put("a5", 32);
//        requestPayload.put("a6", 16);
//        requestPayload.put("a10", 1);
//        requestPayload.put("a12", "https://b0.bdstatic.com/ugc/zvzOJuVeTtOKY7th7APUKQ4a77fedc16936b3ed5cdf694bd28674e.jpg");
//        requestPayload.put("a13", "https://ark-project.tos-cn-beijing.volces.com/doc_image/seepro_last_frame.jpeg");
//        HashMap<String, Object> moreRole = new HashMap<>();
//        moreRole.put("type","https://demo.com/1.jdpg");
        // 增强3模拟：lite 模型 + 参考图列表（不固定数量）
        requestPayload.put("a1", "[图1]戴着眼镜穿着蓝色T恤的男生和[图2]的柯基小狗，坐在[图3]的草坪上，3D卡通风格");
        List<Map<String, Object>> refImages = new ArrayList<>();
        refImages.add(new HashMap<String, Object>() {{
            put("type", "image_url");
            put("image_url", new HashMap<String, Object>() {{
                put("url", "https://ark-project.tos-cn-beijing.volces.com/doc_image/seelite_ref_1.png");
            }});
            put("role", "reference_image");
        }});
        refImages.add(new HashMap<String, Object>() {{
            put("type", "image_url");
            put("image_url", new HashMap<String, Object>() {{
                put("url", "https://ark-project.tos-cn-beijing.volces.com/doc_image/seelite_ref_2.png");
            }});
            put("role", "reference_image");
        }});
        refImages.add(new HashMap<String, Object>() {{
            put("type", "image_url");
            put("image_url", new HashMap<String, Object>() {{
                put("url", "https://ark-project.tos-cn-beijing.volces.com/doc_image/seelite_ref_3.png");
            }});
            put("role", "reference_image");
        }});
        requestPayload.put("a12", refImages);
        requestPayload.put("a15", "doubao-seedance-1-0-lite-i2v-250428");
        query.setParams(requestPayload);
        spelDemo.taskStart(query);
    }

}
