package com.origin.aimodel.dao.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.origin.aimodel.domain.mp.DimAiModelItemMp;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

/**
 * AI模型配置项明细表(DimAiModelItem)表服务接口
 *
 * @author scccy
 * @since 2025-12-13 14:48:19
 */
public interface DimAiModelItemMpService extends IService<DimAiModelItemMp> {

    /**
     * 分页查询，Service 层负责构造分页和查询条件
     */
    Page<DimAiModelItemMp> pageLike(Integer pageNum, Integer pageSize, DimAiModelItemMp query);

    /**
     * 分页查询，Service 层负责构造分页和查询条件
     */
    Page<DimAiModelItemMp> pageEq(Integer pageNum, Integer pageSize, DimAiModelItemMp query);

    /**
     * 查询所有，Service 层负责构造分页和查询条件
     */
    List<DimAiModelItemMp> listEq(DimAiModelItemMp query);

}
