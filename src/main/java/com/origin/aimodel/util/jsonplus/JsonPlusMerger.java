package com.origin.aimodel.util.jsonplus;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import java.util.Collection;
/**
 * 基于 Fastjson2 的增量合并工具，提供 Map/List 追加与覆盖能力
 */
public final class JsonPlusMerger {

    private JsonPlusMerger() {
    }

    public enum MergeStrategy {
        AUTO,
        OVERWRITE,
        KEEP_EXISTING,
        APPEND
    }

    public static JSONObject add(JSONObject target, Object addition) {
        return merge(target, addition, MergeStrategy.AUTO);
    }

    public static JSONObject overwrite(JSONObject target, Object addition) {
        return merge(target, addition, MergeStrategy.OVERWRITE);
    }

    public static JSONObject merge(JSONObject target, Object addition, MergeStrategy strategy) {
        if (target == null) {
            target = new JSONObject();
        }
        if (addition == null) {
            return target;
        }
        if (addition instanceof JSONObject) {
            mergeJSONObject(target, (JSONObject) addition, strategy);
            return target;
        }
        if (addition instanceof JSONArray) {
            JSONArray array = strategy == MergeStrategy.OVERWRITE
                    ? JSONArray.parseArray(((JSONArray) addition).toJSONString())
                    : target.getJSONArray("_");
            if (array == null) {
                array = new JSONArray();
                target.put("_", array);
            }
            if (strategy == MergeStrategy.OVERWRITE) {
                target.put("_", array);
            } else {
                mergeArray(array, (JSONArray) addition);
            }
            return target;
        }
        // 非 JSON 对象，使用策略写入一个临时 key（调用方可自行处理）
        if (strategy != MergeStrategy.KEEP_EXISTING) {
            target.put("_value", cloneValue(addition));
        }
        return target;
    }

    public static void merge(JSONObject target, JSONObject addition, MergeStrategy strategy) {
        if (target == null || addition == null) {
            return;
        }
        mergeJSONObject(target, addition, strategy);
    }

    public static void appendListItem(JSONObject target, String key, Object item) {
        if (target == null || key == null || item == null) {
            return;
        }
        JSONArray array = target.getJSONArray(key);
        if (array == null) {
            array = new JSONArray();
            target.put(key, array);
        }
        array.add(cloneValue(item));
    }

    public static void mergeList(JSONObject target, String key, Collection<?> addition) {
        if (target == null || key == null || addition == null || addition.isEmpty()) {
            return;
        }
        addition.forEach(item -> appendListItem(target, key, item));
    }

    public static void mergeObject(JSONObject target, String key, JSONObject addition) {
        if (target == null || key == null || addition == null) {
            return;
        }
        JSONObject existed = target.getJSONObject(key);
        if (existed == null) {
            target.put(key, JSONObject.parseObject(addition.toJSONString()));
        } else {
            mergeJSONObject(existed, addition, MergeStrategy.AUTO);
        }
    }

    private static void mergeJSONObject(JSONObject target, JSONObject addition, MergeStrategy strategy) {
        for (String key : addition.keySet()) {
            Object newValue = addition.get(key);
            mergeValue(target, key, newValue, strategy);
        }
    }

    private static void mergeValue(JSONObject target, String key, Object newValue, MergeStrategy strategy) {
        if (key == null) {
            return;
        }
        Object existing = target.get(key);
        if (existing == null) {
            target.put(key, cloneValue(newValue));
            return;
        }
        if (existing instanceof JSONObject && newValue instanceof JSONObject) {
            mergeJSONObject((JSONObject) existing, (JSONObject) newValue, strategy);
            return;
        }

        if (existing instanceof JSONArray) {
            JSONArray existingArray = (JSONArray) existing;
            if (strategy == MergeStrategy.OVERWRITE) {
                target.put(key, cloneValue(newValue));
                return;
            }
            if (newValue instanceof JSONArray) {
                mergeArray(existingArray, (JSONArray) newValue);
            } else if (newValue != null) {
                existingArray.add(cloneValue(newValue));
            }
            return;
        }

        if (newValue instanceof JSONArray) {
            if (strategy == MergeStrategy.OVERWRITE) {
                target.put(key, cloneValue(newValue));
            } else {
                JSONArray combined = new JSONArray();
                combined.add(cloneValue(existing));
                mergeArray(combined, (JSONArray) newValue);
                target.put(key, combined);
            }
            return;
        }

        if (strategy == MergeStrategy.KEEP_EXISTING) {
            return;
        }
        if (strategy == MergeStrategy.APPEND) {
            JSONArray array = new JSONArray();
            array.add(cloneValue(existing));
            if (newValue != null) {
                array.add(cloneValue(newValue));
            }
            target.put(key, array);
            return;
        }
        target.put(key, cloneValue(newValue));
    }

    private static void mergeArray(JSONArray target, JSONArray addition) {
        if (target == null || addition == null) {
            return;
        }
        for (Object value : addition) {
            target.add(cloneValue(value));
        }
    }

    private static Object cloneValue(Object value) {
        if (value instanceof JSONObject) {
            return JSONObject.parseObject(((JSONObject) value).toJSONString());
        }
        if (value instanceof JSONArray) {
            return JSONArray.parseArray(((JSONArray) value).toJSONString());
        }
        return value;
    }
}
