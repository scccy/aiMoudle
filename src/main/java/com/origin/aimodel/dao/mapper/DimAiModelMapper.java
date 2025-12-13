package com.origin.aimodel.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.origin.aimodel.domain.mp.DimAiModelMp;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI模型配置表(DimAiModel)Mapper 接口
 *
 * @author scccy
 * @since 2025-12-13 14:48:33
 */
@Mapper
public interface DimAiModelMapper extends BaseMapper<DimAiModelMp> {

}
