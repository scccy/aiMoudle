package com.origin.aimodel.dao.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.origin.aimodel.dao.mapper.AiModelMapper;
import com.origin.aimodel.domain.mp.AiModelMp;
import com.origin.aimodel.dao.service.AiModelMpService;
import com.origin.aimodel.domain.vo.AiTaskQuery;
import com.origin.aimodel.domain.vo.AiTaskResult;
import org.springframework.stereotype.Service;

/**
 * (AiModel)服务实现类
 *
 * @author scccy
 * @since 2025-12-01 16:40:02
 */
@Service
public class AiModelMpServiceImpl
        extends ServiceImpl<AiModelMapper, AiModelMp>
        implements AiModelMpService {




    @Override
    public AiTaskResult taskStart(AiTaskQuery aiTaskQuery) {
        AiModelMp one = this.lambdaQuery().eq(AiModelMp::getModelName, aiTaskQuery.getModelName()).one();
        String inParameter = one.getInParameter();
        System.out.println(inParameter);
        return null;
    }
}
