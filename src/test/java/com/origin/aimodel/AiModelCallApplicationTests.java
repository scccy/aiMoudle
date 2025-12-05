package com.origin.aimodel;

import com.origin.aimodel.domain.vo.AiTaskQuery;
import com.origin.aimodel.util.spel.SpelDemo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.HashMap;
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
        requestPayload.put("a1", "小猫在后空翻");
//        requestPayload.put("a2", "1080p");
//        requestPayload.put("a3", "16:12222");
//        requestPayload.put("a4", 8);
//        requestPayload.put("a5", 32);
//        requestPayload.put("a6", 16);
//        requestPayload.put("a7", 123456789L);
//        requestPayload.put("a8", true);
//        requestPayload.put("a9", null);
        requestPayload.put("a10", 1);
        // 增强2模拟：文本沿用 a1，追加首尾帧 image_url
        requestPayload.put("a12", "https://b0.bdstatic.com/ugc/zvzOJuVeTtOKY7th7APUKQ4a77fedc16936b3ed5cdf694bd28674e.jpg");
        requestPayload.put("a13", "https://ark-project.tos-cn-beijing.volces.com/doc_image/seepro_last_frame.jpeg");
        query.setParams(requestPayload);
        spelDemo.taskStart(query);
    }

}
