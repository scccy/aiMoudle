package com.origin.aimodel.domain.vo;

import com.origin.aimodel.util.spel.MappingItem;
import lombok.Data;

import java.util.List;

/**
 * 正向请求生成的预览 DTO，直接返回配置项列表，便于前端展示/复制。
 */
@Data
public class ForwardRequestPreview {

    /**
     * 完整请求 URL
     */
    private String url;

    /**
     * Header 配置项列表
     */
    private List<MappingItem> headerItems;

    /**
     * Body/Param 配置项列表
     */
    private List<MappingItem> paramItems;
}
