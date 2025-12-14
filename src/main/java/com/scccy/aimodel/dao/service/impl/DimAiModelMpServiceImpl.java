package com.scccy.aimodel.dao.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scccy.aimodel.dao.mapper.DimAiModelMapper;
import com.scccy.aimodel.domain.mp.DimAiModelMp;
import com.scccy.aimodel.dao.service.DimAiModelMpService;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import java.util.List;

/**
 * AI模型配置表(DimAiModel)服务实现类
 *
 * @author scccy
 * @since 2025-12-13 14:48:33
 */
@Service
public class DimAiModelMpServiceImpl
        extends ServiceImpl<DimAiModelMapper, DimAiModelMp>
        implements DimAiModelMpService {
    @Override
    public Page<DimAiModelMp> pageEq(Integer pageNum, Integer pageSize, DimAiModelMp dimAiModelMp) {
        Page<DimAiModelMp> page = new Page<>(pageNum, pageSize);
        QueryWrapper<DimAiModelMp> wrapper = new QueryWrapper<>();

        if (dimAiModelMp.getModelName() != null && !dimAiModelMp.getModelName().isEmpty()) {
            wrapper.eq("model_name", dimAiModelMp.getModelName());
        }
        if (dimAiModelMp.getOriginName() != null && !dimAiModelMp.getOriginName().isEmpty()) {
            wrapper.eq("origin_name", dimAiModelMp.getOriginName());
        }
        if (dimAiModelMp.getBaseUrl() != null && !dimAiModelMp.getBaseUrl().isEmpty()) {
            wrapper.eq("base_url", dimAiModelMp.getBaseUrl());
        }
        if (dimAiModelMp.getPoint() != null && !dimAiModelMp.getPoint().isEmpty()) {
            wrapper.eq("point", dimAiModelMp.getPoint());
        }
        if (dimAiModelMp.getAuthorization() != null && !dimAiModelMp.getAuthorization().isEmpty()) {
            wrapper.eq("authorization", dimAiModelMp.getAuthorization());
        }
        if (dimAiModelMp.getCreatedBy() != null && !dimAiModelMp.getCreatedBy().isEmpty()) {
            wrapper.eq("created_by", dimAiModelMp.getCreatedBy());
        }
        if (dimAiModelMp.getCreatedTime() != null) {
            wrapper.eq("created_time", dimAiModelMp.getCreatedTime());
        }
        if (dimAiModelMp.getUpdatedBy() != null && !dimAiModelMp.getUpdatedBy().isEmpty()) {
            wrapper.eq("updated_by", dimAiModelMp.getUpdatedBy());
        }
        if (dimAiModelMp.getUpdatedTime() != null) {
            wrapper.eq("updated_time", dimAiModelMp.getUpdatedTime());
        }
        if (dimAiModelMp.getDelFlag() != null) {
            wrapper.eq("del_flag", dimAiModelMp.getDelFlag());
        }

        return this.page(page, wrapper);
    }


    @Override
    public Page<DimAiModelMp> pageLike(Integer pageNum, Integer pageSize, DimAiModelMp dimAiModelMp) {
        Page<DimAiModelMp> page = new Page<>(pageNum, pageSize);
        QueryWrapper<DimAiModelMp> wrapper = new QueryWrapper<>();

        if (dimAiModelMp.getModelName() != null && !dimAiModelMp.getModelName().isEmpty()) {
            wrapper.like("model_name", dimAiModelMp.getModelName());
        }
        if (dimAiModelMp.getOriginName() != null && !dimAiModelMp.getOriginName().isEmpty()) {
            wrapper.like("origin_name", dimAiModelMp.getOriginName());
        }
        if (dimAiModelMp.getBaseUrl() != null && !dimAiModelMp.getBaseUrl().isEmpty()) {
            wrapper.like("base_url", dimAiModelMp.getBaseUrl());
        }
        if (dimAiModelMp.getPoint() != null && !dimAiModelMp.getPoint().isEmpty()) {
            wrapper.like("point", dimAiModelMp.getPoint());
        }
        if (dimAiModelMp.getAuthorization() != null && !dimAiModelMp.getAuthorization().isEmpty()) {
            wrapper.like("authorization", dimAiModelMp.getAuthorization());
        }
        if (dimAiModelMp.getCreatedBy() != null && !dimAiModelMp.getCreatedBy().isEmpty()) {
            wrapper.like("created_by", dimAiModelMp.getCreatedBy());
        }
        if (dimAiModelMp.getCreatedTime() != null) {
            wrapper.like("created_time", dimAiModelMp.getCreatedTime());
        }
        if (dimAiModelMp.getUpdatedBy() != null && !dimAiModelMp.getUpdatedBy().isEmpty()) {
            wrapper.like("updated_by", dimAiModelMp.getUpdatedBy());
        }
        if (dimAiModelMp.getUpdatedTime() != null) {
            wrapper.like("updated_time", dimAiModelMp.getUpdatedTime());
        }
        if (dimAiModelMp.getDelFlag() != null) {
            wrapper.like("del_flag", dimAiModelMp.getDelFlag());
        }

        return this.page(page, wrapper);
    }

    @Override
    public List<DimAiModelMp> listEq(DimAiModelMp dimAiModelMp) {

        QueryWrapper<DimAiModelMp> wrapper = new QueryWrapper<>();

        if (dimAiModelMp.getModelName() != null && !dimAiModelMp.getModelName().isEmpty()) {
            wrapper.like("model_name", dimAiModelMp.getModelName());
        }
        if (dimAiModelMp.getOriginName() != null && !dimAiModelMp.getOriginName().isEmpty()) {
            wrapper.like("origin_name", dimAiModelMp.getOriginName());
        }
        if (dimAiModelMp.getBaseUrl() != null && !dimAiModelMp.getBaseUrl().isEmpty()) {
            wrapper.like("base_url", dimAiModelMp.getBaseUrl());
        }
        if (dimAiModelMp.getPoint() != null && !dimAiModelMp.getPoint().isEmpty()) {
            wrapper.like("point", dimAiModelMp.getPoint());
        }
        if (dimAiModelMp.getAuthorization() != null && !dimAiModelMp.getAuthorization().isEmpty()) {
            wrapper.like("authorization", dimAiModelMp.getAuthorization());
        }
        if (dimAiModelMp.getCreatedBy() != null && !dimAiModelMp.getCreatedBy().isEmpty()) {
            wrapper.like("created_by", dimAiModelMp.getCreatedBy());
        }
        if (dimAiModelMp.getCreatedTime() != null) {
            wrapper.like("created_time", dimAiModelMp.getCreatedTime());
        }
        if (dimAiModelMp.getUpdatedBy() != null && !dimAiModelMp.getUpdatedBy().isEmpty()) {
            wrapper.like("updated_by", dimAiModelMp.getUpdatedBy());
        }
        if (dimAiModelMp.getUpdatedTime() != null) {
            wrapper.like("updated_time", dimAiModelMp.getUpdatedTime());
        }
        if (dimAiModelMp.getDelFlag() != null) {
            wrapper.like("del_flag", dimAiModelMp.getDelFlag());
        }

        return this.list(wrapper);
    }

}
