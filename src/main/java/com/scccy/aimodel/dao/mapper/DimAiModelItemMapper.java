package com.scccy.aimodel.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scccy.aimodel.domain.mp.DimAiModelItemMp;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * AI模型配置项明细表(DimAiModelItem)Mapper 接口
 *
 * @author scccy
 * @since 2025-12-13 14:48:19
 */
@Mapper
public interface DimAiModelItemMapper extends BaseMapper<DimAiModelItemMp> {

    /**
     * 物理删除指定模型名称下的所有配置项（绕过逻辑删除）
     * 
     * @param modelName 模型名称
     * @return 删除的记录数
     */
    @Delete("DELETE FROM dim_ai_model_item WHERE model_name = #{modelName}")
    int physicalDeleteByModelName(@Param("modelName") String modelName);
}
