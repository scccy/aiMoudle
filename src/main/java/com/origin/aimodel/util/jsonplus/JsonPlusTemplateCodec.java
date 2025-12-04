package com.origin.aimodel.util.jsonplus;

import com.alibaba.fastjson2.JSONObject;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 为包含 SpEL 的 JSON 模板提供编解码能力，统一处理转义和宽松容错。
 */
public final class JsonPlusTemplateCodec {

    private JsonPlusTemplateCodec() {
    }

    /**
     * 将模板 JSON 字符串解析为 Map。内置多层容错：
     * 1) 直接 fastjson 解析；
     * 2) 失败则尝试自动补全 SpEL 中未转义的双引号；
     * 3) 再失败则宽松按顶层 key:value 拆分；
     * 4) 最后去掉残留的转义符，便于后续 SpEL 求值。
     */
    public static Map<String, String> decodeTemplate(String templateJson) {
        Map<String, String> template = new LinkedHashMap<>();
        if (!StringUtils.hasText(templateJson)) {
            return template;
        }
        JSONObject jsonObject;
        String toParse = templateJson;
        try {
            jsonObject = JSONObject.parseObject(toParse);
        } catch (Exception ex) {
            toParse = escapeSpelQuotes(toParse);
            try {
                jsonObject = JSONObject.parseObject(toParse);
            } catch (Exception retryEx) {
                jsonObject = lenientFlatParse(toParse);
                if (jsonObject == null) {
                    throw new IllegalArgumentException("模板 JSON 解析失败，检查转义/引号是否正确: " + templateJson, retryEx);
                }
            }
        }
        if (jsonObject != null) {
            jsonObject.forEach((key, value) -> template.put(key, value == null ? null : String.valueOf(value)));
        }
        template.replaceAll((k, v) -> unescapeTemplateValue(v));
        return template;
    }

    /**
     * 将 Map 编码为 JSON 字符串，保持 key 顺序。
     */
    public static String encodeTemplate(Map<String, String> template) {
        if (template == null || template.isEmpty()) {
            return "{}";
        }
        return JSONObject.toJSONString(new LinkedHashMap<>(template));
    }

    /**
     * 合并两个模板 JSON 字符串，返回 Map（plus 覆盖 base，同层次追加）。
     */
    public static Map<String, String> mergeTemplateToMap(String baseJson, String plusJson) {
        Map<String, String> base = decodeTemplate(baseJson);
        Map<String, String> plus = decodeTemplate(plusJson);
        if (plus != null) {
            plus.forEach(base::put);
        }
        return base;
    }

    /**
     * 合并两个模板 JSON 字符串，返回合并后的 JSON 字符串。
     */
    public static String mergeTemplate(String baseJson, String plusJson) {
        return encodeTemplate(mergeTemplateToMap(baseJson, plusJson));
    }

    private static String escapeSpelQuotes(String raw) {
        StringBuilder sb = new StringBuilder(raw.length() + 16);
        int len = raw.length();
        int i = 0;
        boolean inSpelValue = false;
        while (i < len) {
            char c = raw.charAt(i);
            if (!inSpelValue) {
                if (c == ':') {
                    sb.append(c);
                    int j = i + 1;
                    while (j < len && Character.isWhitespace(raw.charAt(j))) {
                        sb.append(raw.charAt(j));
                        j++;
                    }
                    if (j + 2 < len && raw.charAt(j) == '"' && raw.charAt(j + 1) == '#' && raw.charAt(j + 2) == '{') {
                        sb.append('"').append('#').append('{');
                        i = j + 3;
                        inSpelValue = true;
                        continue;
                    }
                } else {
                    sb.append(c);
                }
                i++;
                continue;
            }

            if (c == '"' && raw.charAt(i - 1) != '\\') {
                sb.append('\\').append('"');
            } else {
                sb.append(c);
            }
            if (c == '}') {
                int j = i + 1;
                while (j < len && Character.isWhitespace(raw.charAt(j))) {
                    j++;
                }
                if (j < len && raw.charAt(j) == '"') {
                    inSpelValue = false;
                }
            }
            i++;
        }
        return sb.toString();
    }

    /**
     * 宽松解析：仅支持扁平 key:value 结构，按顶层逗号拆分后取字符串值。
     */
    private static JSONObject lenientFlatParse(String raw) {
        String text = raw == null ? "" : raw.trim();
        if (text.startsWith("{") && text.endsWith("}")) {
            text = text.substring(1, text.length() - 1);
        }
        List<String> pairs = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"' && (i == 0 || text.charAt(i - 1) != '\\')) {
                inQuotes = !inQuotes;
            }
            if (c == ',' && !inQuotes) {
                pairs.add(current.toString());
                current.setLength(0);
                continue;
            }
            current.append(c);
        }
        if (current.length() > 0) {
            pairs.add(current.toString());
        }
        JSONObject obj = new JSONObject();
        for (String pair : pairs) {
            if (!StringUtils.hasText(pair)) {
                continue;
            }
            int colonIdx = pair.indexOf(':');
            if (colonIdx <= 0) {
                continue;
            }
            String keyPart = pair.substring(0, colonIdx).trim();
            String valPart = pair.substring(colonIdx + 1).trim();
            String key = trimQuotes(keyPart);
            String value = trimQuotes(valPart);
            obj.put(key, value);
        }
        return obj;
    }

    private static String trimQuotes(String text) {
        if (text == null) {
            return null;
        }
        String t = text.trim();
        if (t.length() >= 2 && t.charAt(0) == '"' && t.charAt(t.length() - 1) == '"' && t.charAt(t.length() - 2) != '\\') {
            return t.substring(1, t.length() - 1);
        }
        return t;
    }

    private static String unescapeTemplateValue(String v) {
        if (v == null) {
            return null;
        }
        String fixed = v;
        while (fixed.contains("\\\\")) {
            fixed = fixed.replace("\\\\", "\\");
        }
        if (fixed.contains("\\\"")) {
            fixed = fixed.replace("\\\"", "\"");
        }
        return fixed;
    }
}
