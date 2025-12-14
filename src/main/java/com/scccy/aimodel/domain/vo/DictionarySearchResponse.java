package com.scccy.aimodel.domain.vo;

import lombok.Data;

import java.util.List;

/**
 * 字典映射查询响应 VO（分页）
 *
 * @author origin
 * @since 2025-12-14
 */
@Data
public class DictionarySearchResponse {

    /**
     * 映射关系列表
     */
    private List<DictionaryMappingVO> mappingList;

    /**
     * 总记录数
     */
    private Long total;

    /**
     * 总页数
     */
    private Long pages;

    /**
     * 当前页码
     */
    private Long current;

    /**
     * 每页大小
     */
    private Long size;
}

