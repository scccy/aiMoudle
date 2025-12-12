# SpEL 模板解析（post_param 驱动版，当前实现说明）

## 1. 概述

本实现提供一个轻量级配置驱动的参数映射工具（`SpelDsl`），用于把业务入参（含可选的 `baseInfo`）映射为实际请求的 header、body 以及 URL 预览。工具不做真实 SpEL 解析，仅支持 `{value}` 字符串模板。

核心对象：
- `SpelDslConfig`：承载 `baseInfo`（可选）、`headerItem`、`paramItem` 三段 JSON 配置。
- `SpelDsl`：提供静态方法与实例化 `Runner`，根据配置与 payload 生成预览。

## 2. 配置结构

### 2.1 headerItem/paramItem
格式均为对象，内部是同构的配置项列表（示例用对象包裹数组）：
```json
{ "paramItem":
  {
    "key": "model",
    "category": "key",
    "node": "model",
    "post_param": "model_name",
    "default_value": "xxx",
    "spel_temp": "{value}",
    "value_object": "string"
  }
}
```

字段含义（两类列表通用）：
- `key`：入参 key。
- `category`：处理类型，当前支持 `key`/`map`/`list`。
- `node`：目标路径，支持 `a.b`、`arr[0].field`。
- `post_param`：写入的目标字段名（若为空则取 node 尾段）。
- `spel_temp`：轻量模板，仅 `{value}` 替换。
- `default_value`：缺省值（无入参时使用）。
- `value_object`：类型提示，支持 string/int/double/list<string>/list<json>/map。
- `validate`（可选，推荐新增）：校验规则描述，支持长度/区间/枚举等约束，供引擎或调用方执行校验。

### 2.2 baseInfo
可选的 JSON 字符串，通常来自 DB，示例：
```json
{
  "model": "chat1",
  "Authorization": "Bearer ...",
  "base_url": "https://api.example.com",
  "point": "/v1/task"
}
```
解析时会同时写入原始 key 与小写 key，便于大小写混用。

## 3. 使用方式

### 3.1 构造配置
```java
SpelDslConfig config = new SpelDslConfig(baseInfoJson, headerItemJson, paramItemJson);
```

### 3.2 静态方法直接调用
```java
Map<String, Object> payload = new HashMap<>();
payload.put("prompt", "hello");

String header = SpelDsl.getHeaderPreview(config, payload);
String body = SpelDsl.getParamPreview(config, payload);
String url = SpelDsl.getUrlPreview(config, payload); // base_url + point
```

### 3.3 Runner 方式
```java
var runner = SpelDsl.newRunner(config, payload);
runner.headerPreview();
runner.paramPreview();
runner.urlPreview();
```

## 4. 处理逻辑要点

1) **合并 payload**：`baseInfo` 首先解析为 Map 并合并入 payload，后续外部 payload 覆盖相同 key。对于 `model`，如果配置项存在且 payload 未提供，会回退到 baseInfo 的 model。

2) **缺省策略**：若入参缺失且配置有 `default_value`，使用缺省值；若两者都无，则跳过该字段（可选字段不会被示例值覆盖）。

3) **模板拼接**：仅对字符串应用 `spel_temp`，将 `{value}` 替换为实际值；若目标已有字符串且再次写入字符串，则做拼接（累加）。

4) **list/map 存储**：为便于后续 fastjson2 统一处理，遇到 Map/List 时先转为紧凑 JSON 字符串后再写入目标。

5) **URL 生成**：`getUrlPreview`/`getUrl` 按 `base_url + point` 拼接，自动处理斜杠冗余。

6) **解析容错**：`parseConfig` 为行级简单解析，假定配置 JSON 结构规范；`parseBaseInfo` 为简易解析，仅用于示例/测试。

## 5. 示例：text-to-video 配置片段

```json
{ "paramItem":
  {
    "key": "model_name",
    "category": "key",
    "node": "model_name",
    "post_param": "model_name",
    "value_object": "string"
  },
  {
    "key": "prompt",
    "category": "key",
    "node": "prompt",
    "post_param": "prompt",
    "value_object": "string"
  },
  {
    "key": "negative_prompt",
    "category": "key",
    "node": "negative_prompt",
    "post_param": "negative_prompt",
    "value_object": "string"
  },
  {
    "key": "cfg_scale",
    "category": "key",
    "node": "cfg_scale",
    "post_param": "cfg_scale",
    "value_object": "double"
  },
  {
    "key": "mode",
    "category": "key",
    "node": "mode",
    "post_param": "mode",
    "value_object": "string"
  },
  {
    "key": "type",
    "category": "key",
    "node": "camera_control.type",
    "post_param": "type",
    "value_object": "string"
  },
  {
    "key": "config",
    "category": "key",
    "node": "camera_control.config",
    "post_param": "config",
    "value_object": "string"
  },
  {
    "key": "horizontal",
    "category": "key",
    "node": "camera_control.horizontal",
    "post_param": "horizontal",
    "value_object": "int"
  },
  {
    "key": "vertical",
    "category": "key",
    "node": "camera_control.vertical",
    "post_param": "vertical",
    "value_object": "int"
  },
  {
    "key": "pan",
    "category": "key",
    "node": "camera_control.pan",
    "post_param": "pan",
    "value_object": "int"
  },
  {
    "key": "tilt",
    "category": "key",
    "node": "camera_control.tilt",
    "post_param": "tilt",
    "value_object": "int"
  },
  {
    "key": "roll",
    "category": "key",
    "node": "camera_control.roll",
    "post_param": "roll",
    "value_object": "int"
  },
  {
    "key": "zoom",
    "category": "key",
    "node": "camera_control.zoom",
    "post_param": "zoom",
    "value_object": "double"
  },
  {
    "key": "aspect_ratio",
    "category": "key",
    "node": "aspect_ratio",
    "post_param": "aspect_ratio",
    "value_object": "string"
  },
  {
    "key": "duration",
    "category": "key",
    "node": "duration",
    "post_param": "duration",
    "value_object": "int"
  },
  {
    "key": "callback_url",
    "category": "key",
    "node": "callback_url",
    "post_param": "callback_url",
    "value_object": "string"
  },
  {
    "key": "external_task_id",
    "category": "key",
    "node": "external_task_id",
    "post_param": "external_task_id",
    "value_object": "string"
  }
}
```

## 6. 注意事项
- 工具不做真实 JSON 校验，配置需保持规范。
- `default_value` 建议仅用于确需兜底的字段；无值即跳过字段。
- `{value}` 模板仅适用于字符串；列表/对象会被转为 JSON 字符串后写入。 

### 6.1 校验规则示例（validate 字段）
```json
{
  "key": "prompt",
  "category": "key",
  "node": "prompt",
  "post_param": "prompt",
  "value_object": "string",
  "validate": {
    "maxLength": 2500
  }
},
{
  "key": "cfg_scale",
  "category": "key",
  "node": "cfg_scale",
  "post_param": "cfg_scale",
  "value_object": "double",
  "validate": {
    "range": [0, 1]
  }
},
{
  "key": "movement",
  "category": "key",
  "node": "movement",
  "post_param": "movement",
  "value_object": "string",
  "validate": {
    "enum": ["simple", "down_back", "forward_up", "right_turn_forward", "left_turn_forward"]
  }
}
```

约定：
- `maxLength`：仅对字符串生效。
- `range`：数组表示闭区间 `[min, max]`，适用于数字类型。
- `enum`：枚举值列表，仅对字符串生效。

引擎或调用方可读取 `validate` 执行校验；未配置则视为可选或按业务默认处理。 
