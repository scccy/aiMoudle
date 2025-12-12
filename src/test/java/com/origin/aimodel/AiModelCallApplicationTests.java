package com.origin.aimodel;

import com.origin.aimodel.domain.vo.AiTaskQuery;
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


}