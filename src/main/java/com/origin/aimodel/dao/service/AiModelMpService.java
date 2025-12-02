package com.origin.aimodel.dao.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.origin.aimodel.domain.mp.AiModelMp;
import com.origin.aimodel.domain.vo.AiTaskQuery;
import com.origin.aimodel.domain.vo.AiTaskResult;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * (AiModel)表服务接口
 *
 * @author scccy
 * @since 2025-12-01 16:40:02
 */
public interface AiModelMpService extends IService<AiModelMp> {
    AiTaskResult taskStart(@RequestBody AiTaskQuery aiTaskQuery);
}
