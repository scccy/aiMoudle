# FastjsonPlus 合并工具设计草案

## 目标
提供一个基于 Fastjson2 的增强工具，支持在运行期对 `JSONObject`/`JSONArray` 进行增量合并，解决以下场景：
- 数据库读取的模板需要在运行期追加字段、数组元素或嵌套结构；
- 同一配置可能被多方叠加，希望“最后写入”与“递归合并”策略可控；
- 避免各处重复手写 fastjson 遍历逻辑，提升可维护性。

## 核心能力
1. **基础键写入**：提供 `putIfAbsent`、`putOrOverride`、`mergeValue` 等方法，按需决定覆盖或保留已有值。
2. **列表合并**：`appendListItem`、`mergeList` 自动确保目标 key 为 `JSONArray`，必要时将单值升级为数组再追加。
3. **嵌套 Map 合并**：`mergeObject` 面向嵌套 `JSONObject` 递归合并，支持自定义冲突策略。
4. **通用合并入口**：`merge(JSONObject target, Object addition, MergeStrategy strategy)` 根据 addition 类型（对象/数组/基础类型）分派到不同处理路径。
5. **类型容错**：当旧值与新值类型不一致时，可按策略（覆盖/跳过/升级列表）处理，避免抛异常。

## MergeStrategy 建议
- `OVERWRITE`：始终以新值覆盖旧值。
- `KEEP_EXISTING`：若 key 已存在则忽略新值。
- `APPEND`：遇到 `JSONArray` 时执行追加；当旧值为单值且新值为数组，可将旧值与新元素组合为新数组。
- `AUTO`：默认行为；`JSONObject` 递归合并、`JSONArray` 追加、其他类型覆盖。

## API 草案
```java
public final class JsonPlusMerger {
    public static JSONObject merge(JSONObject target, Object addition);
    public static void merge(JSONObject target, JSONObject addition, MergeStrategy strategy);
    public static void appendListItem(JSONObject target, String key, Object item);
    public static void mergeList(JSONObject target, String key, Collection<?> addition);
    public static void mergeObject(JSONObject target, String key, JSONObject addition);

    public enum MergeStrategy { OVERWRITE, KEEP_EXISTING, APPEND, AUTO }
}
```

## 使用示例

### 1. merge：合并对象字段
```java
JSONObject target = JSONObject.parseObject("{\"content\":[{\"type\":\"text\"}],\"watermark\":true}");
JSONObject addition = JSONObject.parseObject("{\"content\":[{\"type\":\"image\"}],\"extra\":{\"tag\":\"demo\"}}");

JsonPlusMerger.add(target, addition);
/*
target =>
{
  "content":[{"type":"text"},{"type":"image"}],
  "watermark":true,
  "extra":{"tag":"demo"}
}
*/
```

### 2. append：单独追加 list 项
```java
JSONObject target = JSONObject.parseObject("{\"tags\":[\"base\"]}");
JsonPlusMerger.appendListItem(target, "tags", "new");
/*
target =>
{
  "tags":["base","new"]
}
*/
```

### 3. mergeList：批量追加 list
```java
JSONObject target = JSONObject.parseObject("{\"values\":[1,2]}");
JsonPlusMerger.mergeList(target, "values", Arrays.asList(3,4));
/*
target =>
{
  "values":[1,2,3,4]
}
*/
```

### 4. mergeObject：递归合并嵌套 map
```java
JSONObject target = JSONObject.parseObject("{\"extra\":{\"tag\":\"demo\"}}");
JSONObject addition = JSONObject.parseObject("{\"extra\":{\"level\":2},\"meta\":{\"traceId\":\"t-1\"}}");

JsonPlusMerger.mergeObject(target, "extra", addition.getJSONObject("extra"));
JsonPlusMerger.add(target, addition);
/*
target =>
{
  "extra":{"tag":"demo","level":2},
  "meta":{"traceId":"t-1"}
}
*/
```

## 后续迭代
- 支持输入 JSON 字符串自动解析并合并。
- 列表合并提供去重 Hook（可按字段去重）。
- 添加单元测试覆盖基础字段、数组、嵌套 Map 等冲突场景。
