package com.scccy.aimodel.util.dsl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.scccy.aimodel.domain.vo.MappingItem;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 反向解析工具（精简版）：仅基于 node 生成 MappingItem。
 * category 固定 key，postParam 与 key 默认一致。
 */
public class ReverseParser {

    private ReverseParser() {
    }

    /** 从请求 body + 映射配置生成 paramItem 列表。 */
    public static List<MappingItem> parseParamItems(Map<String, Object> bodyMap, JSONObject mappingConfig) {
        List<MappingItem> baseItems = ReverseDslFactory.generateBasicParamMapping(bodyMap);
        if (mappingConfig == null) {
            return baseItems;
        }
        JSONArray mappingItems = mappingConfig.getJSONArray("paramItem");
        if (mappingItems == null || mappingItems.isEmpty()) {
            return baseItems;
        }
        List<MappingItem> result = new ArrayList<>();
        for (int i = 0; i < mappingItems.size(); i++) {
            JSONObject cfg = mappingItems.getJSONObject(i);
            String key = cfg.getString("key");
            String node = cfg.getString("node");
            String postParam = cfg.getString("post_param");
            MappingItem base = findBase(baseItems, key, node, postParam);
            result.add(buildItem(key, node, postParam, base));
        }
        return result;
    }

    /** 从请求 header + 映射配置生成 headerItem 列表。 */
    public static List<MappingItem> parseHeaderItems(Map<String, String> requestHeaders, JSONObject mappingConfig) {
        List<MappingItem> baseItems = ReverseDslFactory.generateBasicHeaderMapping(requestHeaders);
        if (mappingConfig == null) {
            return baseItems;
        }
        JSONArray mappingItems = mappingConfig.getJSONArray("headerItem");
        if (mappingItems == null || mappingItems.isEmpty()) {
            return baseItems;
        }
        List<MappingItem> result = new ArrayList<>();
        for (int i = 0; i < mappingItems.size(); i++) {
            JSONObject cfg = mappingItems.getJSONObject(i);
            String key = cfg.getString("key");
            String node = cfg.getString("node");
            String postParam = cfg.getString("post_param");
            MappingItem base = findBase(baseItems, key, node, postParam);
            result.add(buildItem(key, node, postParam, base));
        }
        return result;
    }

    private static MappingItem buildItem(String key, String node, String postParam, MappingItem base) {
        MappingItem item = new MappingItem();
        item.key = key;
        item.category = "key";
        item.postParam = StringUtils.hasText(postParam) ? postParam : (StringUtils.hasText(node) ? node : key);
        item.node = StringUtils.hasText(node) ? node : (base != null && StringUtils.hasText(base.node) ? base.node : item.postParam);
        item.valueObject = base != null ? base.valueObject : "string";
        item.valueSource = "USER";
        item.defaultValue = base != null ? base.defaultValue : null;
        return item;
    }

    private static MappingItem findBase(List<MappingItem> baseItems, String key, String node, String postParam) {
        if (baseItems == null) {
            return null;
        }
        for (MappingItem item : baseItems) {
            if (StringUtils.hasText(node) && node.equals(item.node)) {
                return item;
            }
            if (StringUtils.hasText(postParam) && postParam.equals(item.postParam)) {
                return item;
            }
            if (StringUtils.hasText(key) && key.equals(item.key)) {
                return item;
            }
        }
        return null;
    }
}
