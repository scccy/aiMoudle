package com.origin.aimodel.util.jsonplus;

import com.alibaba.fastjson2.JSONObject;

/**
 * 演示 JsonPlusMerger 的使用方式
 */
public class JsonPlusDemo {

    public static void main(String[] args) {
        addDemo();
        keyDemo();
        listDemo();
        mapKvDemo();
        mapKListDemo();
        mapKMapDemo();
    }

    private static void executeDemo(String title, JSONObject target, JSONObject addition, Runnable action) {
        System.out.println("====== " + title + " ======");
        System.out.println("target(before) =\n" + target.toJSONString());
        System.out.println("addition =\n" + addition.toJSONString());
        action.run();
        System.out.println("target(after) =\n" + target.toJSONString());
        System.out.println();
    }

    private static void addDemo() {
        JSONObject target = JSONObject.parseObject("{\"content\":[{\"type\":\"text\",\"text\":\"hello\"}],\"watermark\":true}");
        JSONObject addition = JSONObject.parseObject("{\"content\":[{\"type\":\"image\",\"image_url\":{\"url\":\"https://example.com/demo.png\"}}],\"extra\":{\"tag\":\"demo\"}}");
        executeDemo("add 示例（自动追加）", target, addition, () -> JsonPlusMerger.add(target, addition));
    }

    private static void keyDemo() {
        JSONObject target = JSONObject.parseObject("{\"content\":[{\"type\":\"text\",\"text\":\"hello\"}],\"watermark\":true}");
        JSONObject addition = JSONObject.parseObject("{\"content\":[{\"type\":\"image\",\"image_url\":{\"url\":\"https://example.com/demo.png\"}}],\"watermark\":false}");
        executeDemo("key 覆盖示例（overwrite）", target, addition, () -> JsonPlusMerger.overwrite(target, addition));
    }

    private static void listDemo() {
        JSONObject target = JSONObject.parseObject("{\"tags\":[\"base\"],\"levels\":[1,2]}");
        JSONObject addition = JSONObject.parseObject("{\"tags\":[\"pro\"],\"levels\":[3]}");
        executeDemo("list 追加示例", target, addition, () -> JsonPlusMerger.add(target, addition));
    }

    private static void mapKvDemo() {
        JSONObject target = JSONObject.parseObject("{\"extra\":{\"tag\":\"demo\"}}");
        JSONObject addition = JSONObject.parseObject("{\"extra\":{\"level\":2},\"meta\":{\"traceId\":\"t-1\"}}");
        executeDemo("map KV 追加示例", target, addition, () -> JsonPlusMerger.add(target, addition));
    }

    private static void mapKListDemo() {
        JSONObject target = JSONObject.parseObject("{\"payload\":{\"content\":[{\"type\":\"text\",\"text\":\"hello\"}]}}");
        JSONObject addition = JSONObject.parseObject("{\"payload\":{\"content\":[{\"type\":\"image\",\"image_url\":{\"url\":\"https://example.com/demo.png\"}}]}}");
        executeDemo("map key -> list 示例", target, addition, () -> JsonPlusMerger.add(target, addition));
    }

    private static void mapKMapDemo() {
        JSONObject target = JSONObject.parseObject("{\"payload\":{\"config\":{\"mode\":\"standard\",\"retry\":1}}}");
        JSONObject addition = JSONObject.parseObject("{\"payload\":{\"config\":{\"mode\":\"turbo\",\"timeout\":30}},\"trace\":{\"traceId\":\"t-100\"}}");
        executeDemo("map key -> map 示例", target, addition, () -> JsonPlusMerger.add(target, addition));
    }
}
