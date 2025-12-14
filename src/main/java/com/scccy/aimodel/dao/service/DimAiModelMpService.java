package com.scccy.aimodel.dao.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.scccy.aimodel.domain.mp.DimAiModelMp;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

/**
 * AI模型配置表(DimAiModel)表服务接口
 *
 * @author scccy
 * @since 2025-12-13 14:48:33
 */
public interface DimAiModelMpService extends IService<DimAiModelMp> {

    /**
     * 分页查询，Service 层负责构造分页和查询条件
     */
    Page<DimAiModelMp> pageLike(Integer pageNum, Integer pageSize, DimAiModelMp query);

    /**
     * 分页查询，Service 层负责构造分页和查询条件
     */
    Page<DimAiModelMp> pageEq(Integer pageNum, Integer pageSize, DimAiModelMp query);

    /**
     * 查询所有，Service 层负责构造分页和查询条件
     */
    List<DimAiModelMp> listEq(DimAiModelMp query);

}
