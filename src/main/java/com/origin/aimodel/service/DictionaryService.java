package com.origin.aimodel.service;

import com.origin.aimodel.domain.vo.*;

/**
 * 字典映射业务接口
 *
 * @author origin
 * @since 2025-12-14
 */
public interface DictionaryService {

    /**
     * 查询字典映射关系（分页）
     * 支持多条件组合查询（modelName、key、postParam）
     *
     * @param pageSize 每页大小
     * @param pageNumber 页码（从1开始）
     * @param request 查询请求
     * @return 查询结果（包含分页信息）
     */
    DictionarySearchResponse searchDictionary(Integer pageSize, Integer pageNumber, DictionarySearchRequest request);

    /**
     * 获取 Item 详情
     * 根据 itemId 查询 dim_ai_model_item 表的详细信息
     *
     * @param itemId item ID
     * @return Item 详情
     */
    DictionaryItemDetailResponse getItemDetail(Long itemId);

    /**
     * 更新 Item 详情
     * 根据 itemId 更新 dim_ai_model_item 表的详细信息
     *
     * @param itemId item ID
     * @param request 更新请求（包含所有可更新字段）
     * @return 更新结果消息
     */
    String updateItemDetail(Long itemId, DictionaryItemDetailResponse request);

    /**
     * 创建字典映射
     *
     * @param request 创建请求
     * @return 创建结果消息
     */
    String createDictionary(DictionaryCreateRequest request);

    /**
     * 更新字典映射
     *
     * @param request 更新请求
     * @return 更新结果消息
     */
    String updateDictionary(DictionaryUpdateRequest request);

    /**
     * 删除字典映射
     *
     * @param key key
     * @param postParam postParam
     * @return 删除结果消息
     */
    String deleteDictionary(String key, String postParam);

    /**
     * 获取字典查询选项（用于下拉选择框）
     * 返回所有不重复的模型名称、Key、Post Param
     *
     * @return 查询选项
     */
    DictionaryOptionsResponse getDictionaryOptions();
}

