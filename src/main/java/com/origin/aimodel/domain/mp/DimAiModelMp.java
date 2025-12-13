package com.origin.aimodel.domain.mp;

import java.util.Date;
import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import com.baomidou.mybatisplus.extension.activerecord.Model;

import java.io.Serial;

/**
 * AI模型配置表(DimAiModel)实体类
 *
 * @author scccy
 * @since 2025-12-13 14:48:33
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("dim_ai_model")
public class DimAiModelMp extends Model<DimAiModelMp> implements Serializable {
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

    /**
     * 创建人
     */
    @TableField("created_by")
    private String createdBy;

    /**
     * 创建时间
     */
    @TableField("created_time")
    private Date createdTime;

    /**
     * 更新人
     */
    @TableField("updated_by")
    private String updatedBy;

    /**
     * 更新时间
     */
    @TableField("updated_time")
    private Date updatedTime;

    /**
     * 删除标志：0-未删除，1-已删除
     */
    @TableField("del_flag")
    private Integer delFlag;


}
