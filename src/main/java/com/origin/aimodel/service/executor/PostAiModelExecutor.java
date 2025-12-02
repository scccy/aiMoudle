package com.origin.aimodel.service.executor;

import com.origin.aimodel.config.OkHttpManager;
import com.origin.aimodel.dao.service.AiModelMpService;
import com.origin.aimodel.domain.mp.AiModelMp;
import com.origin.aimodel.domain.vo.AiTaskQuery;
import com.origin.aimodel.domain.vo.AiTaskResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Slf4j
@Service
public class PostAiModelExecutor {

    @Autowired
    OkHttpManager okHttpManager;

    @Autowired
    AiModelMpService aiModelMpServiceImpl;

    public AiTaskResult taskStart(AiTaskQuery aiTaskQuery) {
//        todo:
//        AiModelMp aiModelMp = aiModelMpServiceImpl.lambdaQuery().eq(AiModelMp::getModelName, aiTaskQuery.getModelName()).one();
        HashMap<String, Object> headers = new HashMap<>();
        HashMap<String, Object> param = new HashMap<>();

//                okHttpManager.post()
        log.info("模拟执行成功");
        log.info("原始header:{}", headers);
        log.info("替换后header:{}", headers);
        log.info("原始param:{}", param);
        log.info("替换后header:{}", param);
        return null;
    }
}
