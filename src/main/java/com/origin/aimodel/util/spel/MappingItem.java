package com.origin.aimodel.util.spel;

import lombok.Data;

/**
 * 公用的映射项模型，供配置解析与生成结果复用。
 */
@Data
public class MappingItem {
    public String key;
    public String category;
    public String node;
    public String postParam;
    public String spelTemp;
    public String defaultValue;
    public String valueObject;
    public String validate;
    public String valueSource;
}
