package com.origin.aimodel.domain.mp;

import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.origin.aimodel.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * AI模型配置项明细表实体类
 * 用于存储 headerItem 和 paramItem 的具体配置项
 *
 * @author scccy
 * @since 2025-12-01
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("dim_ai_model_item")
public class AiModelItemMp extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 模型名称，关联dim_ai_model.model_name
     */
    @TableField("model_name")
    private String modelName;

    /**
     * 配置项类型：header-请求头配置，param-请求体参数配置
     */
    @TableField("item_type")
    private String itemType;

    /**
     * 业务入参的key名称
     */
    @TableField("item_key")
    private String itemKey;

    /**
     * 处理类型：key、map、list
     */
    @TableField("category")
    private String category;

    /**
     * 目标路径，支持点号和数组索引，如content[0].text
     */
    @TableField("node")
    private String node;

    /**
     * 写入的目标字段名，为空则取node尾段
     */
    @TableField("post_param")
    private String postParam;

    /**
     * 字符串模板，{value}会被替换为实际值
     */
    @TableField("spel_temp")
    private String spelTemp;

    /**
     * 缺省值，当入参不存在时使用
     */
    @TableField("default_value")
    private String defaultValue;

    /**
     * 类型提示：string、int、double、list<string>、list<json>、map
     */
    @TableField("value_object")
    private String valueObject;

    /**
     * 排序顺序，用于保持配置项的顺序
     */
    @TableField("sort_order")
    private Integer sortOrder;
}
