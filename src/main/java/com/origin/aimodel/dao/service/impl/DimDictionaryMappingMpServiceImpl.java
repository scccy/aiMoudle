package com.origin.aimodel.dao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.origin.aimodel.dao.mapper.DimDictionaryMappingMapper;
import com.origin.aimodel.dao.service.DimDictionaryMappingMpService;
import com.origin.aimodel.domain.mp.DimDictionaryMappingMp;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 字典映射表(DimDictionaryMapping)服务实现类
 *
 * @author scccy
 * @since 2025-12-14 15:17:21
 */
@Service
public class DimDictionaryMappingMpServiceImpl
        extends ServiceImpl<DimDictionaryMappingMapper, DimDictionaryMappingMp>
        implements DimDictionaryMappingMpService {
    @Override
    public Page<DimDictionaryMappingMp> pageEq(Integer pageNum, Integer pageSize, DimDictionaryMappingMp dimDictionaryMappingMp) {
        Page<DimDictionaryMappingMp> page = new Page<>(pageNum, pageSize);
        QueryWrapper<DimDictionaryMappingMp> wrapper = new QueryWrapper<>();

        if (dimDictionaryMappingMp.getKey() != null && !dimDictionaryMappingMp.getKey().isEmpty()) {
            wrapper.eq("key", dimDictionaryMappingMp.getKey());
        }
        if (dimDictionaryMappingMp.getPostParam() != null && !dimDictionaryMappingMp.getPostParam().isEmpty()) {
            wrapper.eq("post_param", dimDictionaryMappingMp.getPostParam());
        }
        if (dimDictionaryMappingMp.getDescription() != null && !dimDictionaryMappingMp.getDescription().isEmpty()) {
            wrapper.eq("description", dimDictionaryMappingMp.getDescription());
        }
        if (dimDictionaryMappingMp.getItemId() != null) {
            wrapper.eq("item_id", dimDictionaryMappingMp.getItemId());
        }
        if (dimDictionaryMappingMp.getCreatedBy() != null && !dimDictionaryMappingMp.getCreatedBy().isEmpty()) {
            wrapper.eq("created_by", dimDictionaryMappingMp.getCreatedBy());
        }
        if (dimDictionaryMappingMp.getCreatedTime() != null) {
            wrapper.eq("created_time", dimDictionaryMappingMp.getCreatedTime());
        }
        if (dimDictionaryMappingMp.getUpdatedBy() != null && !dimDictionaryMappingMp.getUpdatedBy().isEmpty()) {
            wrapper.eq("updated_by", dimDictionaryMappingMp.getUpdatedBy());
        }
        if (dimDictionaryMappingMp.getUpdatedTime() != null) {
            wrapper.eq("updated_time", dimDictionaryMappingMp.getUpdatedTime());
        }
        if (dimDictionaryMappingMp.getDelFlag() != null) {
            wrapper.eq("del_flag", dimDictionaryMappingMp.getDelFlag());
        }

        return this.page(page, wrapper);
    }


    @Override
    public Page<DimDictionaryMappingMp> pageLike(Integer pageNum, Integer pageSize, DimDictionaryMappingMp dimDictionaryMappingMp) {
        Page<DimDictionaryMappingMp> page = new Page<>(pageNum, pageSize);
        QueryWrapper<DimDictionaryMappingMp> wrapper = new QueryWrapper<>();

        if (dimDictionaryMappingMp.getKey() != null && !dimDictionaryMappingMp.getKey().isEmpty()) {
            wrapper.like("key", dimDictionaryMappingMp.getKey());
        }
        if (dimDictionaryMappingMp.getPostParam() != null && !dimDictionaryMappingMp.getPostParam().isEmpty()) {
            wrapper.like("post_param", dimDictionaryMappingMp.getPostParam());
        }
        if (dimDictionaryMappingMp.getDescription() != null && !dimDictionaryMappingMp.getDescription().isEmpty()) {
            wrapper.like("description", dimDictionaryMappingMp.getDescription());
        }
        if (dimDictionaryMappingMp.getItemId() != null) {
            wrapper.like("item_id", dimDictionaryMappingMp.getItemId());
        }
        if (dimDictionaryMappingMp.getCreatedBy() != null && !dimDictionaryMappingMp.getCreatedBy().isEmpty()) {
            wrapper.like("created_by", dimDictionaryMappingMp.getCreatedBy());
        }
        if (dimDictionaryMappingMp.getCreatedTime() != null) {
            wrapper.like("created_time", dimDictionaryMappingMp.getCreatedTime());
        }
        if (dimDictionaryMappingMp.getUpdatedBy() != null && !dimDictionaryMappingMp.getUpdatedBy().isEmpty()) {
            wrapper.like("updated_by", dimDictionaryMappingMp.getUpdatedBy());
        }
        if (dimDictionaryMappingMp.getUpdatedTime() != null) {
            wrapper.like("updated_time", dimDictionaryMappingMp.getUpdatedTime());
        }
        if (dimDictionaryMappingMp.getDelFlag() != null) {
            wrapper.like("del_flag", dimDictionaryMappingMp.getDelFlag());
        }

        return this.page(page, wrapper);
    }

    @Override
    public List<DimDictionaryMappingMp> listEq(DimDictionaryMappingMp dimDictionaryMappingMp) {

        QueryWrapper<DimDictionaryMappingMp> wrapper = new QueryWrapper<>();

        if (dimDictionaryMappingMp.getKey() != null && !dimDictionaryMappingMp.getKey().isEmpty()) {
            wrapper.like("key", dimDictionaryMappingMp.getKey());
        }
        if (dimDictionaryMappingMp.getPostParam() != null && !dimDictionaryMappingMp.getPostParam().isEmpty()) {
            wrapper.like("post_param", dimDictionaryMappingMp.getPostParam());
        }
        if (dimDictionaryMappingMp.getDescription() != null && !dimDictionaryMappingMp.getDescription().isEmpty()) {
            wrapper.like("description", dimDictionaryMappingMp.getDescription());
        }
        if (dimDictionaryMappingMp.getItemId() != null) {
            wrapper.like("item_id", dimDictionaryMappingMp.getItemId());
        }
        if (dimDictionaryMappingMp.getCreatedBy() != null && !dimDictionaryMappingMp.getCreatedBy().isEmpty()) {
            wrapper.like("created_by", dimDictionaryMappingMp.getCreatedBy());
        }
        if (dimDictionaryMappingMp.getCreatedTime() != null) {
            wrapper.like("created_time", dimDictionaryMappingMp.getCreatedTime());
        }
        if (dimDictionaryMappingMp.getUpdatedBy() != null && !dimDictionaryMappingMp.getUpdatedBy().isEmpty()) {
            wrapper.like("updated_by", dimDictionaryMappingMp.getUpdatedBy());
        }
        if (dimDictionaryMappingMp.getUpdatedTime() != null) {
            wrapper.like("updated_time", dimDictionaryMappingMp.getUpdatedTime());
        }
        if (dimDictionaryMappingMp.getDelFlag() != null) {
            wrapper.like("del_flag", dimDictionaryMappingMp.getDelFlag());
        }

        return this.list (wrapper);
    }

}
