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
        // addkey：切换模型；addlist：参考图列表；addmap：常量对象 c1
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
        // addmap 不依赖 payload，本例使用常量 c1:{d1:xxx,d2:[1,2,3]}
        query.setParams(requestPayload);
        spelDemo.taskStart(query);
    }

}
