package com.origin.aimodel.dao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.origin.aimodel.dao.mapper.AiModelItemMapper;
import com.origin.aimodel.dao.service.AiModelItemMpService;
import com.origin.aimodel.domain.mp.AiModelItemMp;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AI模型配置项明细表服务实现类
 *
 * @author scccy
 * @since 2025-12-01
 */
@Service
public class AiModelItemMpServiceImpl
        extends ServiceImpl<AiModelItemMapper, AiModelItemMp>
        implements AiModelItemMpService {

    @Override
    public List<AiModelItemMp> getItemsByModelAndType(String modelName, String itemType) {
        return this.lambdaQuery()
                .eq(AiModelItemMp::getModelName, modelName)
                .eq(AiModelItemMp::getItemType, itemType)
                .orderByAsc(AiModelItemMp::getSortOrder)
                .list();
    }

    @Override
    public List<AiModelItemMp> getItemsByModel(String modelName) {
        return this.lambdaQuery()
                .eq(AiModelItemMp::getModelName, modelName)
                .orderByAsc(AiModelItemMp::getItemType)
                .orderByAsc(AiModelItemMp::getSortOrder)
                .list();
    }
}
