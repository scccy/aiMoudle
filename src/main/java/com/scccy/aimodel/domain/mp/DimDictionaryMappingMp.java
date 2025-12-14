package com.scccy.aimodel.domain.mp;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.scccy.aimodel.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serial;

/**
 * 字典映射表(DimDictionaryMapping)实体类
 *
 * @author scccy
 * @since 2025-12-14 15:17:21
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("dim_dictionary_mapping")
public class DimDictionaryMappingMp extends BaseEntity<DimDictionaryMappingMp> {
    @Serial
    private static final long serialVersionUID = -59788849388319189L;

    /**
     * 业务入参的 key 名称
     */

    @TableField("`key`")
    private String key;

    /**
     * 写入的目标字段名
     */
    @TableField("post_param")
    private String postParam;

    /**
     * 中文说明
     */
    @TableField("description")
    private String description;

    /**
     * 关联的 item ID，关联 dim_ai_model_item.id
     */
    @TableField("item_id")
    private Long itemId;



}
