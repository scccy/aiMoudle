package com.origin.aimodel.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.origin.aimodel.domain.mp.DimAiModelItemMp;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI模型配置项明细表(DimAiModelItem)Mapper 接口
 *
 * @author scccy
 * @since 2025-12-13 14:48:19
 */
@Mapper
public interface DimAiModelItemMapper extends BaseMapper<DimAiModelItemMp> {

}
