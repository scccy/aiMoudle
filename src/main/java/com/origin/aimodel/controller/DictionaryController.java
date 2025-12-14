package com.origin.aimodel.controller;

import com.origin.aimodel.base.ResultData;
import com.origin.aimodel.domain.vo.*;
import com.origin.aimodel.service.DictionaryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 字典映射 Controller
 * 提供字典映射的查询、创建、更新、删除接口
 *
 * @author origin
 * @since 2025-12-14
 */
@Slf4j
@RestController
@RequestMapping("/api/dictionary")
public class DictionaryController {


    @Autowired
    DictionaryService dictionaryService;

    /**
     * 查询字典映射关系（分页）
     * 支持多条件组合查询（modelName、key、postParam）
     *
     * @param pageSize 每页大小
     * @param pageNumber 页码（从1开始）
     * @param request 查询请求
     * @return 查询结果
     */
    @PostMapping("/search")
    public ResultData<?> searchDictionary(
            @RequestParam Integer pageSize,
            @RequestParam Integer pageNumber,
            @RequestBody DictionarySearchRequest request) {
        try {
            DictionarySearchResponse response = dictionaryService.searchDictionary(pageSize, pageNumber, request);
            return ResultData.ok(response);
        } catch (Exception e) {
            log.error("查询字典映射失败", e);
            return ResultData.fail("查询字典映射失败: " + e.getMessage());
        }
    }

    /**
     * 获取 Item 详情
     * 根据 itemId 查询 dim_ai_model_item 表的详细信息
     *
     * @param itemId item ID
     * @return Item 详情
     */
    @GetMapping("/item/{itemId}")
    public ResultData<DictionaryItemDetailResponse> getItemDetail(@PathVariable Long itemId) {
        try {
            DictionaryItemDetailResponse response = dictionaryService.getItemDetail(itemId);
            return ResultData.ok(response);
        } catch (Exception e) {
            log.error("获取 Item 详情失败，itemId: {}", itemId, e);
            return ResultData.fail("获取 Item 详情失败: " + e.getMessage());
        }
    }

    /**
     * 更新 Item 详情
     * 根据 itemId 更新 dim_ai_model_item 表的详细信息
     *
     * @param itemId item ID
     * @param request 更新请求（包含所有可更新字段）
     * @return 更新结果
     */
    @PutMapping("/item/{itemId}")
    public ResultData<String> updateItemDetail(@PathVariable Long itemId, @RequestBody DictionaryItemDetailResponse request) {
        try {
            String result = dictionaryService.updateItemDetail(itemId, request);
            return ResultData.ok(result);
        } catch (Exception e) {
            log.error("更新 Item 详情失败，itemId: {}", itemId, e);
            return ResultData.fail("更新 Item 详情失败: " + e.getMessage());
        }
    }

    /**
     * 创建字典映射
     *
     * @param request 创建请求
     * @return 创建结果
     */
    @PostMapping("/create")
    public ResultData<String> createDictionary(@RequestBody DictionaryCreateRequest request) {
        try {
            String result = dictionaryService.createDictionary(request);
            return ResultData.ok(result);
        } catch (Exception e) {
            log.error("创建字典映射失败", e);
            return ResultData.fail("创建字典映射失败: " + e.getMessage());
        }
    }

    /**
     * 更新字典映射
     *
     * @param request 更新请求
     * @return 更新结果
     */
    @PutMapping("/update")
    public ResultData<String> updateDictionary(@RequestBody DictionaryUpdateRequest request) {
        try {
            String result = dictionaryService.updateDictionary(request);
            return ResultData.ok(result);
        } catch (Exception e) {
            log.error("更新字典映射失败", e);
            return ResultData.fail("更新字典映射失败: " + e.getMessage());
        }
    }

    /**
     * 删除字典映射
     *
     * @param key key
     * @param postParam postParam
     * @return 删除结果
     */
    @DeleteMapping("/delete")
    public ResultData<String> deleteDictionary(@RequestParam String key, @RequestParam String postParam) {
        try {
            String result = dictionaryService.deleteDictionary(key, postParam);
            return ResultData.ok(result);
        } catch (Exception e) {
            log.error("删除字典映射失败，key: {}, postParam: {}", key, postParam, e);
            return ResultData.fail("删除字典映射失败: " + e.getMessage());
        }
    }

    /**
     * 获取字典查询选项（用于下拉选择框）
     * 返回所有不重复的模型名称、Key、Post Param
     *
     * @return 查询选项
     */
    @GetMapping("/options")
    public ResultData<DictionaryOptionsResponse> getDictionaryOptions() {
        try {
            DictionaryOptionsResponse response = dictionaryService.getDictionaryOptions();
            return ResultData.ok(response);
        } catch (Exception e) {
            log.error("获取字典查询选项失败", e);
            return ResultData.fail("获取字典查询选项失败: " + e.getMessage());
        }
    }
}

