package com.scccy.aimodel.util.dsl;

import com.scccy.aimodel.domain.mp.DimAiModelItemMp;
import com.scccy.aimodel.domain.mp.DimAiModelMp;
import com.scccy.aimodel.domain.vo.ForwardGenerateRequest;
import com.scccy.aimodel.domain.vo.ForwardRequestResult;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 轻量正向生成器：基于 item 配置 (node/item_key/post_param/spel_temp/default_value) 构造 URL/header/body。
 * 支持路径形态：name、name[]、name[].map{1}、name[].map{*}。
 * 同一 node 的多条配置按传入顺序追加（用于 spel_temp 追加）。
 */
public final class ForwardBuilder {

    private ForwardBuilder() {
    }

    public static ForwardRequestResult build(ForwardGenerateRequest request,
                                             DimAiModelMp model,
                                             List<DimAiModelItemMp> items) {
        Map<String, Object> payload = CollectionUtils.isEmpty(request.getPayload())
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(request.getPayload());
        mergeModelBaseInfo(model, payload);

        List<Item> paramItems = toItems(items, "param");
        List<Item> headerItems = toItems(items, "header");

        Map<String, Object> body = buildBody(paramItems, payload);
        Map<String, Object> headers = buildHeaders(headerItems, payload);
        String url = buildUrl(payload);

        ForwardRequestResult result = new ForwardRequestResult();
        result.setBody(body);
        result.setHeaders(headers);
        result.setUrl(url);
        return result;
    }

    private static void mergeModelBaseInfo(DimAiModelMp model, Map<String, Object> payload) {
        if (model == null) {
            return;
        }
        if (StringUtils.hasText(model.getBaseUrl())) {
            payload.putIfAbsent("base_url", model.getBaseUrl());
        }
        if (StringUtils.hasText(model.getPoint())) {
            payload.putIfAbsent("point", model.getPoint());
        }
        if (StringUtils.hasText(model.getAuthorization())) {
            payload.putIfAbsent("Authorization", model.getAuthorization());
        }
        if (StringUtils.hasText(model.getModelName())) {
            payload.putIfAbsent("model", model.getModelName());
        }
    }

    private static List<Item> toItems(List<DimAiModelItemMp> raw, String type) {
        if (raw == null) {
            return List.of();
        }
        return raw.stream()
                .filter(i -> type.equalsIgnoreCase(i.getItemType()))
                .map(i -> new Item(
                        i.getItemKey(),
                        i.getNode(),
                        i.getPostParam(),
                        i.getSpelTemp(),
                        i.getDefaultValue(),
                        i.getSortOrder() == null ? 0 : i.getSortOrder()
                ))
                .collect(Collectors.toList());
    }

    private static Map<String, Object> buildHeaders(List<Item> items, Map<String, Object> payload) {
        Map<String, Object> headers = new LinkedHashMap<>();
        for (Item item : items) {
            Object value = payload.get(item.key);
            if (value == null) {
                value = getByNode(payload, item.node);
            }
            if (value == null && StringUtils.hasText(item.defaultValue)) {
                value = item.defaultValue;
            }
            if (value == null) {
                continue;
            }
            Object rendered = render(item.spelTemp, value);
            String name = StringUtils.hasText(item.postParam) ? item.postParam : leafName(item.node);
            headers.put(name, rendered == null ? "" : rendered.toString());
        }
        return headers;
    }

    private static Map<String, Object> buildBody(List<Item> items, Map<String, Object> payload) {
        Map<String, Object> root = new LinkedHashMap<>();
        for (Item item : items) {
            Object value = payload.get(item.key);
            if (value == null) {
                value = getByNode(payload, item.node);
            }
            if (value == null && StringUtils.hasText(item.defaultValue)) {
                value = item.defaultValue;
            }
            if (value == null) {
                continue;
            }
            Object rendered = render(item.spelTemp, value);
            applyNode(root, item.node, rendered);
        }
        return root;
    }

    private static void applyNode(Map<String, Object> root, String node, Object value) {
        if (!StringUtils.hasText(node)) {
            return;
        }
        String[] parts = node.split("\\.");
        Map<String, Object> current = root;
        for (int i = 0; i < parts.length; i++) {
            Token token = Token.parse(parts[i]);
            boolean isLast = i == parts.length - 1;
            if (isLast) {
                String field = leafName(token.name);
                if (token.hasIndex && !token.mapPlaceholder) {
                    @SuppressWarnings("unchecked")
                    List<Object> list = (List<Object>) current.computeIfAbsent(field, k -> new ArrayList<>());
                    int idx = token.wildcard ? 0 : Math.max(0, token.index);
                    while (list.size() <= idx) {
                        list.add(new LinkedHashMap<String, Object>());
                    }
                    Object slot = list.get(idx);
                    if (!(slot instanceof Map)) {
                        slot = new LinkedHashMap<String, Object>();
                        list.set(idx, slot);
                    }
                    @SuppressWarnings("unchecked")
                    Map<String, Object> mapSlot = (Map<String, Object>) slot;
                    mapSlot.put(field, value);
                } else {
                    Object existing = current.get(field);
                    if (existing instanceof String && value instanceof String) {
                        String a = (String) existing;
                        String b = (String) value;
                        if (!a.contains(b)) {
                            String combined = a + ((a.endsWith(" ") || b.startsWith(" ")) ? "" : " ") + b;
                            current.put(field, combined);
                        }
                    } else {
                        current.put(field, value);
                    }
                }
            } else {
                if (token.mapPlaceholder) {
                    // 占位，不创建键，保持在当前 map
                    continue;
                }
                if (token.hasIndex) {
                    @SuppressWarnings("unchecked")
                    List<Object> list = (List<Object>) current.computeIfAbsent(token.name, k -> new ArrayList<>());
                    int idx = token.wildcard ? 0 : Math.max(0, token.index);
                    while (list.size() <= idx) {
                        list.add(new LinkedHashMap<String, Object>());
                    }
                    Object slot = list.get(idx);
                    if (!(slot instanceof Map)) {
                        slot = new LinkedHashMap<String, Object>();
                        list.set(idx, slot);
                    }
                    @SuppressWarnings("unchecked")
                    Map<String, Object> mapSlot = (Map<String, Object>) slot;
                    current = mapSlot;
                } else {
                    Object container = current.get(token.name);
                    if (!(container instanceof Map)) {
                        container = new LinkedHashMap<String, Object>();
                        current.put(token.name, container);
                    }
                    @SuppressWarnings("unchecked")
                    Map<String, Object> mapSlot = (Map<String, Object>) container;
                    current = mapSlot;
                }
            }
        }
    }

    private static String leafName(String tokenName) {
        if (tokenName == null) {
            return "";
        }
        String name = tokenName.replace("[]", "");
        if (name.startsWith("map{") && name.endsWith("}")) {
            return "map";
        }
        return name;
    }

    private static Object render(String spelTemp, Object value) {
        if (!StringUtils.hasText(spelTemp)) {
            return value;
        }
        return spelTemp.replace("{value}", value == null ? "" : value.toString());
    }

    private static Object getByNode(Map<String, Object> payload, String node) {
        if (!StringUtils.hasText(node)) {
            return null;
        }
        String[] parts = node.split("\\.");
        Object current = payload;
        for (String part : parts) {
            Token token = Token.parse(part);
            if (token.mapPlaceholder) {
                continue;
            }
            if (current instanceof Map<?, ?> map) {
                current = map.get(token.name);
            } else if (current instanceof List<?> list) {
                if (list.isEmpty()) {
                    current = null;
                } else {
                    int idx = token.wildcard ? 0 : Math.min(token.index, list.size() - 1);
                    current = list.get(idx);
                }
            } else {
                current = null;
            }
            if (current == null) {
                break;
            }
        }
        return current;
    }

    private static String buildUrl(Map<String, Object> payload) {
        String base = stringVal(payload, "base_url");
        String point = stringVal(payload, "point");
        if (!StringUtils.hasText(base) && !StringUtils.hasText(point)) {
            return "";
        }
        if (!StringUtils.hasText(base)) {
            return point;
        }
        if (!StringUtils.hasText(point)) {
            return base;
        }
        if (base.endsWith("/") && point.startsWith("/")) {
            return base + point.substring(1);
        }
        if (!base.endsWith("/") && !point.startsWith("/")) {
            return base + "/" + point;
        }
        return base + point;
    }

    private static String stringVal(Map<String, Object> map, String key) {
        if (map == null) {
            return null;
        }
        Object v = map.get(key);
        if (v == null) {
            v = map.get(key.toLowerCase());
        }
        return v != null ? v.toString() : null;
    }

    private record Item(String key,
                        String node,
                        String postParam,
                        String spelTemp,
                        String defaultValue,
                        int sortOrder) {
    }

    /** 路径 token 解析，仅支持 name、name[]、map{1}/map{*}。 */
    static class Token {
        final String raw;
        final String name;
        final boolean hasIndex;
        final boolean wildcard;
        final int index;
        final boolean mapPlaceholder;

        private Token(String raw, String name, boolean hasIndex, boolean wildcard, int index, boolean mapPlaceholder) {
            this.raw = raw;
            this.name = name;
            this.hasIndex = hasIndex;
            this.wildcard = wildcard;
            this.index = index;
            this.mapPlaceholder = mapPlaceholder;
        }

        static Token parse(String token) {
            if (token.endsWith("[]")) {
                String name = token.substring(0, token.length() - 2);
                return new Token(token, name, true, true, -1, false);
            }
            if (token.startsWith("map{") && token.endsWith("}")) {
                String inside = token.substring(4, token.length() - 1);
                if ("*".equals(inside) || inside.isEmpty()) {
                    return new Token(token, "map", true, true, -1, true);
                }
                int idx = Integer.parseInt(inside);
                return new Token(token, "map", true, false, idx, true);
            }
            return new Token(token, token, false, false, -1, false);
        }
    }
}
