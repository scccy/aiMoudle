package com.origin.aimodel.dao.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.origin.aimodel.dao.mapper.DimAiModelItemMapper;
import com.origin.aimodel.domain.mp.DimAiModelItemMp;
import com.origin.aimodel.dao.service.DimAiModelItemMpService;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import java.util.List;

/**
 * AI模型配置项明细表(DimAiModelItem)服务实现类
 *
 * @author scccy
 * @since 2025-12-13 14:48:19
 */
@Service
public class DimAiModelItemMpServiceImpl
        extends ServiceImpl<DimAiModelItemMapper, DimAiModelItemMp>
        implements DimAiModelItemMpService {
    @Override
    public Page<DimAiModelItemMp> pageEq(Integer pageNum, Integer pageSize, DimAiModelItemMp dimAiModelItemMp) {
        Page<DimAiModelItemMp> page = new Page<>(pageNum, pageSize);
        QueryWrapper<DimAiModelItemMp> wrapper = new QueryWrapper<>();

        if (dimAiModelItemMp.getId() != null) {
            wrapper.eq("id", dimAiModelItemMp.getId());
        }
        if (dimAiModelItemMp.getModelName() != null && !dimAiModelItemMp.getModelName().isEmpty()) {
            wrapper.eq("model_name", dimAiModelItemMp.getModelName());
        }
        if (dimAiModelItemMp.getItemType() != null && !dimAiModelItemMp.getItemType().isEmpty()) {
            wrapper.eq("item_type", dimAiModelItemMp.getItemType());
        }
        if (dimAiModelItemMp.getItemKey() != null && !dimAiModelItemMp.getItemKey().isEmpty()) {
            wrapper.eq("item_key", dimAiModelItemMp.getItemKey());
        }
        if (dimAiModelItemMp.getCategory() != null && !dimAiModelItemMp.getCategory().isEmpty()) {
            wrapper.eq("category", dimAiModelItemMp.getCategory());
        }
        if (dimAiModelItemMp.getNode() != null && !dimAiModelItemMp.getNode().isEmpty()) {
            wrapper.eq("node", dimAiModelItemMp.getNode());
        }
        if (dimAiModelItemMp.getPostParam() != null && !dimAiModelItemMp.getPostParam().isEmpty()) {
            wrapper.eq("post_param", dimAiModelItemMp.getPostParam());
        }
        if (dimAiModelItemMp.getSpelTemp() != null && !dimAiModelItemMp.getSpelTemp().isEmpty()) {
            wrapper.eq("spel_temp", dimAiModelItemMp.getSpelTemp());
        }
        if (dimAiModelItemMp.getDefaultValue() != null && !dimAiModelItemMp.getDefaultValue().isEmpty()) {
            wrapper.eq("default_value", dimAiModelItemMp.getDefaultValue());
        }
        if (dimAiModelItemMp.getValueObject() != null && !dimAiModelItemMp.getValueObject().isEmpty()) {
            wrapper.eq("value_object", dimAiModelItemMp.getValueObject());
        }
        if (dimAiModelItemMp.getSortOrder() != null) {
            wrapper.eq("sort_order", dimAiModelItemMp.getSortOrder());
        }
        if (dimAiModelItemMp.getCreatedBy() != null && !dimAiModelItemMp.getCreatedBy().isEmpty()) {
            wrapper.eq("created_by", dimAiModelItemMp.getCreatedBy());
        }
        if (dimAiModelItemMp.getCreatedTime() != null) {
            wrapper.eq("created_time", dimAiModelItemMp.getCreatedTime());
        }
        if (dimAiModelItemMp.getUpdatedBy() != null && !dimAiModelItemMp.getUpdatedBy().isEmpty()) {
            wrapper.eq("updated_by", dimAiModelItemMp.getUpdatedBy());
        }
        if (dimAiModelItemMp.getUpdatedTime() != null) {
            wrapper.eq("updated_time", dimAiModelItemMp.getUpdatedTime());
        }
        if (dimAiModelItemMp.getDelFlag() != null) {
            wrapper.eq("del_flag", dimAiModelItemMp.getDelFlag());
        }

        return this.page(page, wrapper);
    }


    @Override
    public Page<DimAiModelItemMp> pageLike(Integer pageNum, Integer pageSize, DimAiModelItemMp dimAiModelItemMp) {
        Page<DimAiModelItemMp> page = new Page<>(pageNum, pageSize);
        QueryWrapper<DimAiModelItemMp> wrapper = new QueryWrapper<>();

        if (dimAiModelItemMp.getId() != null) {
            wrapper.like("id", dimAiModelItemMp.getId());
        }
        if (dimAiModelItemMp.getModelName() != null && !dimAiModelItemMp.getModelName().isEmpty()) {
            wrapper.like("model_name", dimAiModelItemMp.getModelName());
        }
        if (dimAiModelItemMp.getItemType() != null && !dimAiModelItemMp.getItemType().isEmpty()) {
            wrapper.like("item_type", dimAiModelItemMp.getItemType());
        }
        if (dimAiModelItemMp.getItemKey() != null && !dimAiModelItemMp.getItemKey().isEmpty()) {
            wrapper.like("item_key", dimAiModelItemMp.getItemKey());
        }
        if (dimAiModelItemMp.getCategory() != null && !dimAiModelItemMp.getCategory().isEmpty()) {
            wrapper.like("category", dimAiModelItemMp.getCategory());
        }
        if (dimAiModelItemMp.getNode() != null && !dimAiModelItemMp.getNode().isEmpty()) {
            wrapper.like("node", dimAiModelItemMp.getNode());
        }
        if (dimAiModelItemMp.getPostParam() != null && !dimAiModelItemMp.getPostParam().isEmpty()) {
            wrapper.like("post_param", dimAiModelItemMp.getPostParam());
        }
        if (dimAiModelItemMp.getSpelTemp() != null && !dimAiModelItemMp.getSpelTemp().isEmpty()) {
            wrapper.like("spel_temp", dimAiModelItemMp.getSpelTemp());
        }
        if (dimAiModelItemMp.getDefaultValue() != null && !dimAiModelItemMp.getDefaultValue().isEmpty()) {
            wrapper.like("default_value", dimAiModelItemMp.getDefaultValue());
        }
        if (dimAiModelItemMp.getValueObject() != null && !dimAiModelItemMp.getValueObject().isEmpty()) {
            wrapper.like("value_object", dimAiModelItemMp.getValueObject());
        }
        if (dimAiModelItemMp.getSortOrder() != null) {
            wrapper.like("sort_order", dimAiModelItemMp.getSortOrder());
        }
        if (dimAiModelItemMp.getCreatedBy() != null && !dimAiModelItemMp.getCreatedBy().isEmpty()) {
            wrapper.like("created_by", dimAiModelItemMp.getCreatedBy());
        }
        if (dimAiModelItemMp.getCreatedTime() != null) {
            wrapper.like("created_time", dimAiModelItemMp.getCreatedTime());
        }
        if (dimAiModelItemMp.getUpdatedBy() != null && !dimAiModelItemMp.getUpdatedBy().isEmpty()) {
            wrapper.like("updated_by", dimAiModelItemMp.getUpdatedBy());
        }
        if (dimAiModelItemMp.getUpdatedTime() != null) {
            wrapper.like("updated_time", dimAiModelItemMp.getUpdatedTime());
        }
        if (dimAiModelItemMp.getDelFlag() != null) {
            wrapper.like("del_flag", dimAiModelItemMp.getDelFlag());
        }

        return this.page(page, wrapper);
    }

    @Override
    public List<DimAiModelItemMp> listEq(DimAiModelItemMp dimAiModelItemMp) {

        QueryWrapper<DimAiModelItemMp> wrapper = new QueryWrapper<>();

        if (dimAiModelItemMp.getId() != null) {
            wrapper.like("id", dimAiModelItemMp.getId());
        }
        if (dimAiModelItemMp.getModelName() != null && !dimAiModelItemMp.getModelName().isEmpty()) {
            wrapper.like("model_name", dimAiModelItemMp.getModelName());
        }
        if (dimAiModelItemMp.getItemType() != null && !dimAiModelItemMp.getItemType().isEmpty()) {
            wrapper.like("item_type", dimAiModelItemMp.getItemType());
        }
        if (dimAiModelItemMp.getItemKey() != null && !dimAiModelItemMp.getItemKey().isEmpty()) {
            wrapper.like("item_key", dimAiModelItemMp.getItemKey());
        }
        if (dimAiModelItemMp.getCategory() != null && !dimAiModelItemMp.getCategory().isEmpty()) {
            wrapper.like("category", dimAiModelItemMp.getCategory());
        }
        if (dimAiModelItemMp.getNode() != null && !dimAiModelItemMp.getNode().isEmpty()) {
            wrapper.like("node", dimAiModelItemMp.getNode());
        }
        if (dimAiModelItemMp.getPostParam() != null && !dimAiModelItemMp.getPostParam().isEmpty()) {
            wrapper.like("post_param", dimAiModelItemMp.getPostParam());
        }
        if (dimAiModelItemMp.getSpelTemp() != null && !dimAiModelItemMp.getSpelTemp().isEmpty()) {
            wrapper.like("spel_temp", dimAiModelItemMp.getSpelTemp());
        }
        if (dimAiModelItemMp.getDefaultValue() != null && !dimAiModelItemMp.getDefaultValue().isEmpty()) {
            wrapper.like("default_value", dimAiModelItemMp.getDefaultValue());
        }
        if (dimAiModelItemMp.getValueObject() != null && !dimAiModelItemMp.getValueObject().isEmpty()) {
            wrapper.like("value_object", dimAiModelItemMp.getValueObject());
        }
        if (dimAiModelItemMp.getSortOrder() != null) {
            wrapper.like("sort_order", dimAiModelItemMp.getSortOrder());
        }
        if (dimAiModelItemMp.getCreatedBy() != null && !dimAiModelItemMp.getCreatedBy().isEmpty()) {
            wrapper.like("created_by", dimAiModelItemMp.getCreatedBy());
        }
        if (dimAiModelItemMp.getCreatedTime() != null) {
            wrapper.like("created_time", dimAiModelItemMp.getCreatedTime());
        }
        if (dimAiModelItemMp.getUpdatedBy() != null && !dimAiModelItemMp.getUpdatedBy().isEmpty()) {
            wrapper.like("updated_by", dimAiModelItemMp.getUpdatedBy());
        }
        if (dimAiModelItemMp.getUpdatedTime() != null) {
            wrapper.like("updated_time", dimAiModelItemMp.getUpdatedTime());
        }
        if (dimAiModelItemMp.getDelFlag() != null) {
            wrapper.like("del_flag", dimAiModelItemMp.getDelFlag());
        }

        return this.list(wrapper);
    }

}
