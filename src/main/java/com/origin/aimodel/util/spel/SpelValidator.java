package com.origin.aimodel.util.spel;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.val;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 读取 ParamConfigEntry 的 validate 字段（可扩展 maxLength/range/enum 等）。
 * 暴露 validate(entries, payload)，返回 ValidationResult。
 * Runner.validate() 直接委托到这里。
 */
public class SpelValidator {
    
    // 移除了硬编码的枚举规则，所有校验应基于配置中的 validate 字段
    
    private SpelValidator() {
    }
    
    /** 校验 payload 与配置的匹配度（长度/数值范围/枚举）。 */
    public static ValidationResult validateParam(SpelDslConfig config, Map<String, Object> payload) {
        List<SpelConfigParser.ParamConfigEntry> entries = SpelConfigParser.parseConfig(config.paramItem());
        Map<String, Object> merged = new LinkedHashMap<>(SpelConfigParser.parseBaseInfo(config.baseInfo()));
        if (payload != null) {
            merged.putAll(payload);
        }
        return validateEntries(entries, merged);
    }
    
    private static ValidationResult validateEntries(List<SpelConfigParser.ParamConfigEntry> entries, Map<String, Object> payload) {
        List<String> errors = new ArrayList<>();
        for (SpelConfigParser.ParamConfigEntry entry : entries) {
            Object val = payload.get(entry.key);
            if (val == null) {
                continue;
            }
            
            // 如果有 validate 规则，则根据规则校验
            if (entry.validate != null && !entry.validate.isEmpty()) {
                try {
                    JSONObject validateObj = JSON.parseObject(entry.validate);
                    for (String ruleKey : validateObj.keySet()) {
                        switch (ruleKey) {
                            case "maxLength":
                                Integer maxLength = validateObj.getInteger("maxLength");
                                if (maxLength != null && val instanceof String str && str.length() > maxLength) {
                                    errors.add(entry.key + " length exceeds " + maxLength);
                                }
                                break;
                            case "range":
                                JSONArray rangeArray = validateObj.getJSONArray("range");
                                if (rangeArray != null && rangeArray.size() == 2) {
                                    double min = rangeArray.getDoubleValue(0);
                                    double max = rangeArray.getDoubleValue(1);
                                    Double num = parseNumber(val);
                                    if (num != null && (num < min || num > max)) {
                                        errors.add(entry.key + " out of range [" + min + "," + max + "]");
                                    }
                                }
                                break;
                            case "enum":
                                JSONArray enumArray = validateObj.getJSONArray("enum");
                                if (enumArray != null) {
                                    boolean match = false;
                                    for (int i = 0; i < enumArray.size(); i++) {
                                        if (enumArray.getString(i).equals(val.toString())) {
                                            match = true;
                                            break;
                                        }
                                    }
                                    if (!match) {
                                        errors.add(entry.key + " not in enum " + enumArray.toString());
                                    }
                                }
                                break;
                        }
                    }
                } catch (Exception e) {
                    // JSON解析失败，使用旧的校验逻辑作为后备
                    fallbackValidation(entry, val, errors);
                }
            } else {
                // 如果没有 validate 规则，则使用旧的校验逻辑作为后备
                fallbackValidation(entry, val, errors);
            }
        }
        return new ValidationResult(errors);
    }
    
    private static void fallbackValidation(SpelConfigParser.ParamConfigEntry entry, Object val, List<String> errors) {
        // 对于没有 validate 配置的情况，直接跳过校验
        // 可以选择添加最基本的类型检查，但避免任何硬编码的业务规则
        String type = entry.valueObject != null ? entry.valueObject : "string";
        
        if (val instanceof String s) {
            // 只保留最基本的长度检查，防止过长字符串
            if ("string".equals(type) && s.length() > 2500) {
                errors.add(entry.key + " length exceeds 2500");
            }
            // 移除了对硬编码枚举规则的检查
        }
        
        // 移除了硬编码的数值范围检查，因为这应该是可配置的
        if (type.equals("int") || type.equals("double") || type.equals("float")) {
            Double num = parseNumber(val);
            if (num == null) {
                errors.add(entry.key + " is not a number");
            }
            // 移除了硬编码的 [0,1] 范围检查，让配置决定有效范围
        }
    }
    
    private static Double parseNumber(Object val) {
        if (val instanceof Number n) {
            return n.doubleValue();
        }
        try {
            return Double.parseDouble(val.toString());
        } catch (Exception e) {
            return null;
        }
    }
    
    /** 校验结果承载类。 */
    public static class ValidationResult {
        private final List<String> errors;

        ValidationResult(List<String> errors) {
            this.errors = errors;
        }

        public boolean isValid() {
            return errors.isEmpty();
        }

        public List<String> getErrors() {
            return errors;
        }
    }
}