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
 * AI模型配置表(DimAiModel)实体类
 *
 * @author scccy
 * @since 2025-12-13 14:48:33
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("dim_ai_model")
public class DimAiModelMp extends BaseEntity<DimAiModelMp> {
    @Serial
    private static final long serialVersionUID = 569819800017061585L;

    /**
     * 模型名称
     */
    @TableId("model_name")
    private String modelName;

    /**
     * 原始名称
     */
    @TableField("origin_name")
    private String originName;

    /**
     * 基础URL
     */
    @TableField("base_url")
    private String baseUrl;

    /**
     * 接口路径
     */
    @TableField("point")
    private String point;

    /**
     * 授权信息
     */
    @TableField("authorization")
    private String authorization;




}
