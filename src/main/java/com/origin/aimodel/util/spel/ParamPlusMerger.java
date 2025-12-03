package com.origin.aimodel.util.spel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 将 paramPlus JSON 描述的额外结构合并到最终请求的工具
 */
public final class ParamPlusMerger {

    private ParamPlusMerger() {
    }

    @SuppressWarnings("unchecked")
    public static void apply(Map<String, Object> target, Object addition) {
        if (target == null || addition == null) {
            return;
        }
        if (addition instanceof Map) {
            mergeMap(target, (Map<String, Object>) addition);
        }
    }

    @SuppressWarnings("unchecked")
    private static void mergeMap(Map<String, Object> target, Map<String, Object> addition) {
        addition.forEach((key, value) -> {
            if (value == null) {
                return;
            }
            Object existing = target.get(key);
            if (existing instanceof Map && value instanceof Map) {
                mergeMap((Map<String, Object>) existing, (Map<String, Object>) value);
            } else if (existing instanceof List && value instanceof List) {
                mergeList((List<Object>) existing, (List<?>) value);
            } else if (value instanceof List) {
                List<Object> list = existing instanceof List
                        ? new ArrayList<>((List<Object>) existing)
                        : new ArrayList<>();
                if (existing != null && !(existing instanceof List)) {
                    list.add(existing);
                }
                mergeList(list, (List<?>) value);
                target.put(key, list);
            } else {
                target.put(key, value);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private static void mergeList(List<Object> target, List<?> addition) {
        addition.forEach(item -> {
            if (item instanceof Map) {
                target.add(new LinkedHashMap<>((Map<String, Object>) item));
            } else if (item instanceof List) {
                List<Object> nested = new ArrayList<>();
                mergeList(nested, (List<?>) item);
                target.add(nested);
            } else {
                target.add(item);
            }
        });
    }
}
