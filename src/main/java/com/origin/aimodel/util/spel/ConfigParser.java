package com.origin.aimodel.util.spel;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 负责 headerItem/paramItem 的行级解析，产出 MappingItem 列表。
 * 独立 parseBaseInfo，返回 Map（大小写兼容）。
 */
public class ConfigParser {
    
    private ConfigParser() {
    }
    
    /** 解析 headerItem/paramItem 配置字符串，提取字段属性。 */
    public static List<MappingItem> parseConfig(String config) {
        // 优先使用 JSON 解析，兼容 DB 中直接存储的 JSON 对象/数组
        List<MappingItem> fromJson = tryParseJson(config);
        if (fromJson != null) {
            return fromJson;
        }
        
        // 兼容旧的行级解析（含转义的字符串）
        List<MappingItem> list = new ArrayList<>();
        String[] lines = config.split("\\R");
        MappingItem current = null;
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.startsWith("\"key\"")) {
                if (current != null) {
                    list.add(current);
                }
                current = new MappingItem();
                current.key = extractValue(line);
            } else if (line.startsWith("\"category\"") && current != null) {
                current.category = extractValue(line);
            } else if (line.startsWith("\"node\"") && current != null) {
                current.node = extractValue(line);
            } else if (line.startsWith("\"post_param\"") && current != null) {
                current.postParam = extractValue(line);
            } else if (line.startsWith("\"spel_temp\"") && current != null) {
                current.spelTemp = extractValue(line);
            } else if (line.startsWith("\"default_value\"") && current != null) {
                current.defaultValue = extractValue(line);
            } else if (line.startsWith("\"value_object\"") && current != null) {
                current.valueObject = extractValue(line);
            } else if (line.startsWith("\"validate\"") && current != null) {
                current.validate = extractValue(line);
            }
        }
        if (current != null) {
            list.add(current);
        }
        return list;
    }

    private static List<MappingItem> tryParseJson(String config) {
        if (config == null || config.isBlank()) {
            return new ArrayList<>();
        }
        try {
            JSONObject root = JSONObject.parseObject(config);
            JSONArray arr = root.getJSONArray("paramItem");
            if (arr == null) {
                arr = root.getJSONArray("headerItem");
            }
            if (arr == null) {
                return null;
            }
            List<MappingItem> list = new ArrayList<>();
            for (int i = 0; i < arr.size(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                MappingItem entry = new MappingItem();
                entry.key = obj.getString("key");
                entry.category = obj.getString("category");
                entry.node = obj.getString("node");
                entry.postParam = obj.getString("post_param");
                entry.spelTemp = safeJsonValue(obj.get("spel_temp"));
                entry.defaultValue = safeJsonValue(obj.get("default_value"));
                entry.valueObject = obj.getString("value_object");
                entry.validate = safeJsonValue(obj.get("validate"));
                list.add(entry);
            }
            return list;
        } catch (Exception ignore) {
            return null;
        }
    }

    private static String safeJsonValue(Object v) {
        if (v == null) return null;
        if (v instanceof String) return (String) v;
        return JSONObject.toJSONString(v);
    }
    
    /** 解析 baseInfo JSON 字符串为 Map，兼容大小写 key。 */
    public static Map<String, Object> parseBaseInfo(String baseInfo) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (baseInfo == null || baseInfo.isBlank()) {
            return map;
        }
        String json = baseInfo.replace("\n", "").replace("\r", "").trim();
        if (json.startsWith("{") && json.endsWith("}")) {
            json = json.substring(1, json.length() - 1);
        }
        String[] pairs = json.split(",");
        for (String pair : pairs) {
            String[] kv = pair.split(":", 2);
            if (kv.length == 2) {
                String key = kv[0].trim().replace("\"", "");
                String value = kv[1].trim().replace("\"", "");
                map.put(key, value);
                map.put(key.toLowerCase(), value);
            }
        }
        return map;
    }
    
    private static String extractValue(String line) {
        int colon = line.indexOf(':');
        if (colon < 0) {
            return "";
        }
        String valuePart = line.substring(colon + 1).trim();
        // 去掉结尾逗号
        if (valuePart.endsWith(",")) {
            valuePart = valuePart.substring(0, valuePart.length() - 1).trim();
        }
        // 去掉包裹的引号
        if (valuePart.startsWith("\"") && valuePart.endsWith("\"") && valuePart.length() >= 2) {
            return valuePart.substring(1, valuePart.length() - 1);
        }
        return valuePart;
    }
    
    
    static class Token {
        final String name;
        final boolean hasIndex;
        final boolean wildcard; // [] 占位
        final int index;

        private Token(String name, boolean hasIndex, boolean wildcard, int index) {
            this.name = name;
            this.hasIndex = hasIndex;
            this.wildcard = wildcard;
            this.index = index;
        }

        static Token parse(String token) {
            int l = token.indexOf('[');
            int r = token.indexOf(']');
            if (l > 0 && r > l) {
                String name = token.substring(0, l);
                String inside = token.substring(l + 1, r);
                if (inside.isEmpty()) {
                    return new Token(name, true, true, -1);
                }
                int idx = Integer.parseInt(inside);
                return new Token(name, true, false, idx);
            }
            return new Token(token, false, false, -1);
        }
    }
}
