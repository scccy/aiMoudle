package com.origin.aimodel;

import com.origin.aimodel.domain.vo.AiTaskQuery;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import com.origin.aimodel.config.OkHttpManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@SpringBootTest(classes = {SpelDemoNew.class, AiModelCallApplicationTests.TestBeans.class})
class AiModelCallApplicationTests {

    @Autowired
    SpelDemoNew spelDemoNew;

    @Test
    void testExampleOne() throws IOException {
//        AiTaskQuery query = new AiTaskQuery();
//        query.setModelName("model1");
//        Map<String, Object> payload = new HashMap<>();
//        payload.put("model", "doubao-seedance-1-0-pro-250528");
//        payload.put("contentType", "text");
//        payload.put("prompt", "多个镜头。一名侦探进入一间光线昏暗的房间。他检查桌上的线索，手里拿起桌上的某个物品。镜头转向他正在思索。");
//        payload.put("resolution", "1280x720");
//        payload.put("ratio", "16:9");
//        payload.put("duration", "5");
//        payload.put("framesNew", "8");
//        query.setParams(payload);
//        spelDemoNew.taskStart(query);
    }

    @Test
    void testExampleTwo() throws IOException {
//        AiTaskQuery query = new AiTaskQuery();
//        query.setModelName("model2");
//        Map<String, Object> payload = new HashMap<>();
//        payload.put("model_name", "sdxl-v1.2");
//        payload.put("prompt", "a futuristic city at night with neon lights");
//        payload.put("negative_prompt", "low resolution, blurry, distorted face");
//        payload.put("cfg_scale", 7);
//        payload.put("mode", "image");
//        payload.put("type", "track");
//        payload.put("config", "smooth");
//        payload.put("horizontal", 10);
//        payload.put("vertical", 5);
//        payload.put("pan", 3);
//        payload.put("tilt", 2);
//        payload.put("roll", 0);
//        payload.put("zoom", 1.2);
//        payload.put("aspect_ratio", "16:9");
//        payload.put("duration", 5);
//        payload.put("callback_url", "http://example.com/callback");
//        payload.put("external_task_id", "task-123456");
//        query.setParams(payload);
//        spelDemoNew.taskStart(query);
    }

    @TestConfiguration
    static class TestBeans {
        @Bean
        OkHttpClient okHttpClient() {
            return new OkHttpClient.Builder().build();
        }

        @Bean
        OkHttpManager okHttpManager(OkHttpClient client) {
            return new OkHttpManager(client);
        }
    }
}