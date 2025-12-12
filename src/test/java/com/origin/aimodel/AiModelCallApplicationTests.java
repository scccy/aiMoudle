package com.origin.aimodel;

import com.origin.aimodel.domain.vo.AiTaskQuery;
import com.origin.aimodel.util.spel.demo.SpelDemo;
import com.origin.aimodel.util.spel.demo.SpelDemoNew;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

@SpringBootTest
class AiModelCallApplicationTests {

    @Autowired
    SpelDemo spelDemo;

    @Autowired
    SpelDemoNew spelDemoNew;

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

}
