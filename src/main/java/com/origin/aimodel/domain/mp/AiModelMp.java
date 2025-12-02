package com.origin.aimodel.domain.mp;

import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.origin.aimodel.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;





/**
 * (AiModel)实体类
 *
 * @author scccy
 * @since 2025-12-01 16:40:02
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("ai_model")
public class AiModelMp extends BaseEntity implements Serializable {

    private static final long serialVersionUID = -93706987435890484L;


    @TableId("model_name")
    private String modelName;


    @TableField("in_parameter")
    private String inParameter;


    @TableField("out_parameter")
    private String outParameter;




}
