package com.scccy.aimodel.domain.mp;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.scccy.aimodel.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serial;

/**
 * AI模型配置项明细表(DimAiModelItem)实体类
 *
 * @author scccy
 * @since 2025-12-13 14:48:19
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("dim_ai_model_item")
public class DimAiModelItemMp extends BaseEntity<DimAiModelItemMp> {
    @Serial
    private static final long serialVersionUID = -87268884493238198L;

    /**
     * 主键ID
     */
    @TableId("id")
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
     * 值来源：CONST | USER | DERIVED
     */
    @TableField("value_source")
    private String valueSource;

    /**
     * 校验规则，JSON 字符串格式
     * 示例：{"maxLength": 2000} 或 {"range": [0.0, 2.0]} 或 {"enum": ["text", "image_url"]}
     */
    @TableField("validate")
    private String validate;

    /**
     * 排序顺序，用于保持配置项的顺序
     */
    @TableField("sort_order")
    private Integer sortOrder;




}
