package com.origin.aimodel.dao.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.origin.aimodel.domain.mp.AiModelItemMp;

import java.util.List;

/**
 * AI模型配置项明细表服务接口
 *
 * @author scccy
 * @since 2025-12-01
 */
public interface AiModelItemMpService extends IService<AiModelItemMp> {
    
    /**
     * 根据模型名称和配置项类型查询配置项列表
     *
     * @param modelName 模型名称
     * @param itemType  配置项类型：header 或 param
     * @return 配置项列表，按sort_order排序
     */
    List<AiModelItemMp> getItemsByModelAndType(String modelName, String itemType);
    
    /**
     * 根据模型名称查询所有配置项（包括header和param）
     *
     * @param modelName 模型名称
     * @return 配置项列表，按item_type和sort_order排序
     */
    List<AiModelItemMp> getItemsByModel(String modelName);
}
