package com.origin.aimodel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.origin.aimodel.dao.service.DimAiModelItemMpService;
import com.origin.aimodel.domain.mp.DimAiModelItemMp;
import com.origin.aimodel.domain.vo.*;
import com.origin.aimodel.service.DictionaryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 字典映射业务实现
 * 直接使用 dim_ai_model_item 表作为字典表
 *
 * @author origin
 * @since 2025-12-14
 */
@Slf4j
@Service
public class DictionaryServiceImpl implements DictionaryService {
    @Autowired
    DimAiModelItemMpService dimAiModelItemMpService;



    @Override
    public DictionarySearchResponse searchDictionary(Integer pageSize, Integer pageNumber, DictionarySearchRequest request) {
        DictionarySearchResponse response = new DictionarySearchResponse();

        try {
            // 构建查询条件，直接查询 item 表
            LambdaQueryWrapper<DimAiModelItemMp> queryWrapper = new LambdaQueryWrapper<>();

            // 按 modelName 查询（模糊匹配）
            if (StringUtils.hasText(request.getModelName())) {
                queryWrapper.like(DimAiModelItemMp::getModelName, request.getModelName());
            }

            // 按 key 查询（模糊匹配，使用 item_key 字段）
            if (StringUtils.hasText(request.getKey())) {
                queryWrapper.like(DimAiModelItemMp::getItemKey, request.getKey());
            }

            // 按 postParam 查询（模糊匹配）
            if (StringUtils.hasText(request.getPostParam())) {
                queryWrapper.like(DimAiModelItemMp::getPostParam, request.getPostParam());
            }

            // 使用 MyBatis-Plus 分页查询
            Page<DimAiModelItemMp> page = new Page<>(pageNumber, pageSize);
            Page<DimAiModelItemMp> pageResult = dimAiModelItemMpService.page(page, queryWrapper);

            // 转换为 VO，每个 item 对应一条记录（不去重，因为每个 item 都是独立的）
            List<DictionaryMappingVO> mappingList = pageResult.getRecords().stream()
                .map(item -> {
                    DictionaryMappingVO vo = new DictionaryMappingVO();
                    vo.setModelName(item.getModelName());
                    vo.setKey(item.getItemKey());
                    vo.setPostParam(item.getPostParam());
                    vo.setItemId(item.getId());
                    // description 可以使用 node 或其他字段作为补充说明
                    vo.setDescription(item.getNode() != null ? item.getNode() : "");
                    return vo;
                })
                .collect(Collectors.toList());

            // 设置分页信息
            response.setMappingList(mappingList);
            response.setTotal(pageResult.getTotal());
            response.setPages(pageResult.getPages());
            response.setCurrent(pageResult.getCurrent());
            response.setSize(pageResult.getSize());

            log.info("查询字典映射成功，共 {} 条记录，当前第 {} 页，每页 {} 条", 
                    pageResult.getTotal(), pageNumber, pageSize);

        } catch (Exception e) {
            log.error("查询字典映射失败", e);
            throw new RuntimeException("查询字典映射失败: " + e.getMessage(), e);
        }

        return response;
    }

    @Override
    public DictionaryItemDetailResponse getItemDetail(Long itemId) {
        if (itemId == null) {
            throw new IllegalArgumentException("itemId 不能为空");
        }

        try {
            // 根据 itemId 查询 item 详情
            DimAiModelItemMp item = dimAiModelItemMpService.getById(itemId);
            if (item == null) {
                throw new RuntimeException("未找到 item，itemId: " + itemId);
            }

            // 转换为 VO
            DictionaryItemDetailResponse response = new DictionaryItemDetailResponse();
            BeanUtils.copyProperties(item, response);
            // 注意：itemKey 字段映射
            response.setItemKey(item.getItemKey());

            log.info("获取 Item 详情成功，itemId: {}", itemId);
            return response;

        } catch (Exception e) {
            log.error("获取 Item 详情失败，itemId: {}", itemId, e);
            throw new RuntimeException("获取 Item 详情失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String updateItemDetail(Long itemId, DictionaryItemDetailResponse request) {
        if (itemId == null) {
            throw new IllegalArgumentException("itemId 不能为空");
        }

        try {
            // 查询 item
            DimAiModelItemMp item = dimAiModelItemMpService.getById(itemId);
            if (item == null) {
                throw new RuntimeException("未找到 item，itemId: " + itemId);
            }

            // 更新字段（只更新可修改的字段，不更新 id、modelName、itemType 等基础字段）
            if (StringUtils.hasText(request.getItemKey())) {
                item.setItemKey(request.getItemKey());
            }
            if (StringUtils.hasText(request.getCategory())) {
                item.setCategory(request.getCategory());
            }
            if (StringUtils.hasText(request.getNode())) {
                item.setNode(request.getNode());
            }
            if (StringUtils.hasText(request.getPostParam())) {
                item.setPostParam(request.getPostParam());
            }
            // spel_temp 是 JSON 类型，空字符串需要转换为 null
            if (request.getSpelTemp() != null) {
                String spelTemp = request.getSpelTemp().trim();
                item.setSpelTemp(spelTemp.isEmpty() ? null : spelTemp);
            }
            if (request.getDefaultValue() != null) {
                item.setDefaultValue(request.getDefaultValue());
            }
            if (StringUtils.hasText(request.getValueObject())) {
                item.setValueObject(request.getValueObject());
            }
            // validate 是 JSON 类型，空字符串需要转换为 null
            if (request.getValidate() != null) {
                String validate = request.getValidate().trim();
                item.setValidate(validate.isEmpty() ? null : validate);
            }
            if (request.getSortOrder() != null) {
                item.setSortOrder(request.getSortOrder());
            }

            boolean success = dimAiModelItemMpService.updateById(item);
            if (!success) {
                throw new RuntimeException("更新 Item 详情失败");
            }

            log.info("更新 Item 详情成功，itemId: {}", itemId);
            return "更新成功";

        } catch (Exception e) {
            log.error("更新 Item 详情失败，itemId: {}", itemId, e);
            throw new RuntimeException("更新 Item 详情失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String createDictionary(DictionaryCreateRequest request) {
        // 字典映射现在直接使用 item 表，创建操作应该通过 item 表进行
        // 如果 itemId 已提供，说明 item 已存在，不需要创建
        // 如果没有 itemId，需要先创建 item，但这里缺少必要信息（如 modelName, itemType 等）
        throw new UnsupportedOperationException("字典映射直接使用 item 表，请通过反向解析页面创建配置项");
    }

    @Override
    public String updateDictionary(DictionaryUpdateRequest request) {
        // 字典映射现在直接使用 item 表，更新操作应该通过 item 表进行
        if (request.getItemId() == null) {
            throw new IllegalArgumentException("itemId 不能为空，请提供要更新的 item ID");
        }

        try {
            // 查询 item
            DimAiModelItemMp item = dimAiModelItemMpService.getById(request.getItemId());
            if (item == null) {
                throw new RuntimeException("未找到 item，itemId: " + request.getItemId());
            }

            // 更新 item 的 key 和 postParam（如果提供）
            if (StringUtils.hasText(request.getKey())) {
                item.setItemKey(request.getKey());
            }
            if (StringUtils.hasText(request.getPostParam())) {
                item.setPostParam(request.getPostParam());
            }

            boolean success = dimAiModelItemMpService.updateById(item);
            if (!success) {
                throw new RuntimeException("更新字典映射失败");
            }

            log.info("更新字典映射成功，itemId: {}, key: {}, postParam: {}", request.getItemId(), request.getKey(), request.getPostParam());
            return "更新成功";

        } catch (Exception e) {
            log.error("更新字典映射失败", e);
            throw new RuntimeException("更新字典映射失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String deleteDictionary(String key, String postParam) {
        // 字典映射现在直接使用 item 表，删除操作应该通过 item 表进行
        // 这里可以根据 key 和 postParam 查找并删除对应的 item
        try {
            LambdaQueryWrapper<DimAiModelItemMp> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(DimAiModelItemMp::getItemKey, key);
            queryWrapper.eq(DimAiModelItemMp::getPostParam, postParam);
            
            List<DimAiModelItemMp> items = dimAiModelItemMpService.list(queryWrapper);
            if (items.isEmpty()) {
                throw new RuntimeException("未找到匹配的 item，key: " + key + ", postParam: " + postParam);
            }

            // 逻辑删除所有匹配的 item
            for (DimAiModelItemMp item : items) {
                dimAiModelItemMpService.removeById(item);
            }

            log.info("删除字典映射成功，key: {}, postParam: {}, 删除了 {} 条记录", key, postParam, items.size());
            return "删除成功，共删除 " + items.size() + " 条记录";

        } catch (Exception e) {
            log.error("删除字典映射失败", e);
            throw new RuntimeException("删除字典映射失败: " + e.getMessage(), e);
        }
    }

    @Override
    public DictionaryOptionsResponse getDictionaryOptions() {
        DictionaryOptionsResponse response = new DictionaryOptionsResponse();
        try {
            // 查询所有未删除的 item
            LambdaQueryWrapper<DimAiModelItemMp> queryWrapper = new LambdaQueryWrapper<>();
            List<DimAiModelItemMp> items = dimAiModelItemMpService.list(queryWrapper);

            // 提取所有不重复的模型名称
            List<String> modelNames = items.stream()
                    .map(DimAiModelItemMp::getModelName)
                    .filter(StringUtils::hasText)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());

            // 提取所有不重复的 Key
            List<String> keys = items.stream()
                    .map(DimAiModelItemMp::getItemKey)
                    .filter(StringUtils::hasText)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());

            // 提取所有不重复的 Post Param
            List<String> postParams = items.stream()
                    .map(DimAiModelItemMp::getPostParam)
                    .filter(StringUtils::hasText)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());

            response.setModelNames(modelNames);
            response.setKeys(keys);
            response.setPostParams(postParams);

            log.info("获取字典查询选项成功，模型名称: {} 个，Key: {} 个，Post Param: {} 个",
                    modelNames.size(), keys.size(), postParams.size());
        } catch (Exception e) {
            log.error("获取字典查询选项失败", e);
            throw new RuntimeException("获取字典查询选项失败: " + e.getMessage(), e);
        }
        return response;
    }
}

