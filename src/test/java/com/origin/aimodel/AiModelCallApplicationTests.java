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
        query.setModelName("seedream-3.0");
        Map<String, Object> requestPayload = new HashMap<>();
        requestPayload.put("a1", "小狗101");
        requestPayload.put("a2", "1080p");
        requestPayload.put("a3", "16:12222");
        requestPayload.put("a4", 8);
        requestPayload.put("a5", 32);
        requestPayload.put("a6", 16);
        requestPayload.put("a7", 123456789L);
        requestPayload.put("a8", true);
        requestPayload.put("a9", null);
        requestPayload.put("a10", 1);
        // 增强1模拟：文本强化与 image_url（文本沿用 a1）
        requestPayload.put("a12", "https://ark-project.tos-cn-beijing.volces.com/doc_image/i2v_foxrgirl.png");
        query.setParams(requestPayload);
        spelDemo.taskStart(query);
    }

    @Test
    void test(){
        // 支持多种横杠: 英文 - 和 --，中文 – 和 ——，可有可无空格
        Pattern TIME_RANGE_PATTERN = Pattern.compile(
                "(\\d{2}:\\d{2})\\s*([\\-–—]{1,2})\\s*(\\d{2}:\\d{2})"
        );

            String[] texts = {
                    "09:00-18:00",
                    "09:00 -- 18:00",
                    "09:00–18:00",
                    "09:00——18:00",
                    " 09:00 - 18:00 ",
                    "09:00—18:00",
                    "09:00——18:00",
                    "09:00-18:00",
                    "09:00--18:00",
                    "09:00-18:00",
                    "09:00--18:00",

            };

            for (String text : texts) {
                Matcher matcher = TIME_RANGE_PATTERN.matcher(text);
                if (matcher.find()) {
                    System.out.println("匹配成功: " + text);
                    System.out.println("开始时间: " + matcher.group(1));
                    System.out.println("结束时间: " + matcher.group(3));
                    System.out.println("------");
                } else {
                    System.out.println("未匹配: " + text);
                }
            }
        }
}
