package com.origin.aimodel.dao.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.origin.aimodel.domain.mp.DimDictionaryMappingMp;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

/**
 * 字典映射表(DimDictionaryMapping)表服务接口
 *
 * @author scccy
 * @since 2025-12-14 15:17:21
 */
public interface DimDictionaryMappingMpService extends IService<DimDictionaryMappingMp> {

    /**
     * 分页查询，Service 层负责构造分页和查询条件
     */
    Page<DimDictionaryMappingMp> pageLike(Integer pageNum, Integer pageSize, DimDictionaryMappingMp query);

    /**
     * 分页查询，Service 层负责构造分页和查询条件
     */
    Page<DimDictionaryMappingMp> pageEq(Integer pageNum, Integer pageSize, DimDictionaryMappingMp query);

    /**
     * 查询所有，Service 层负责构造分页和查询条件
     */
    List<DimDictionaryMappingMp> listEq(DimDictionaryMappingMp query);

}
