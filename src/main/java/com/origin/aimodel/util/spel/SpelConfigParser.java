package com.origin.aimodel.util.spel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 负责 headerItem/paramItem 的行级解析，产出 ParamConfigEntry 列表。
 * 独立 parseBaseInfo，返回 Map（大小写兼容）。
 */
public class SpelConfigParser {
    
    private SpelConfigParser() {
    }
    
    /** 解析 headerItem/paramItem 配置字符串，提取字段属性。 */
    public static List<ParamConfigEntry> parseConfig(String config) {
        List<ParamConfigEntry> list = new ArrayList<>();
        String[] lines = config.split("\\R");
        ParamConfigEntry current = null;
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.startsWith("\"key\"")) {
                if (current != null) {
                    list.add(current);
                }
                current = new ParamConfigEntry();
                current.key = extractQuotedValue(line);
            } else if (line.startsWith("\"category\"") && current != null) {
                current.category = extractQuotedValue(line);
            } else if (line.startsWith("\"node\"") && current != null) {
                current.node = extractQuotedValue(line);
            } else if (line.startsWith("\"post_param\"") && current != null) {
                current.postParam = extractQuotedValue(line);
            } else if (line.startsWith("\"spel_temp\"") && current != null) {
                current.spelTemp = extractQuotedValue(line);
            } else if (line.startsWith("\"default_value\"") && current != null) {
                current.defaultValue = extractQuotedValue(line);
            } else if (line.startsWith("\"value_object\"") && current != null) {
                current.valueObject = extractQuotedValue(line);
            } else if (line.startsWith("\"validate\"") && current != null) {
                current.validate = extractQuotedValue(line);
            }
        }
        if (current != null) {
            list.add(current);
        }
        return list;
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
    
    private static String extractQuotedValue(String line) {
        int colon = line.indexOf(':');
        int start = colon >= 0 ? line.indexOf('"', colon) : line.indexOf('"');
        int end = line.lastIndexOf('"');
        if (start >= 0 && end > start) {
            return line.substring(start + 1, end);
        }
        return "";
    }
    
    /** 配置项承载类。 */
    public static class ParamConfigEntry {
        public String key;
        public String category;
        public String node;
        public String postParam;
        public String spelTemp;
        public String defaultValue;
        public String valueObject;
        public String validate;
    }
    
    static class Token {
        final String name;
        final boolean hasIndex;
        final int index;

        private Token(String name, boolean hasIndex, int index) {
            this.name = name;
            this.hasIndex = hasIndex;
            this.index = index;
        }

        static Token parse(String token) {
            int l = token.indexOf('[');
            int r = token.indexOf(']');
            if (l > 0 && r > l) {
                String name = token.substring(0, l);
                int idx = Integer.parseInt(token.substring(l + 1, r));
                return new Token(name, true, idx);
            }
            return new Token(token, false, -1);
        }
    }
}
