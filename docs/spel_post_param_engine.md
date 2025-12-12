# SpEL 模板解析引擎使用指南

## 概述

SpEL 模板解析引擎（`SpelDsl`）是一个配置驱动的参数映射工具，用于将业务入参映射为实际 HTTP 请求的 header、body 和 URL。通过简单的 JSON 配置即可完成复杂的参数转换，支持嵌套结构、列表处理、模板拼接等功能。

**核心概念：**
- `baseInfo`：基础配置（模型信息、鉴权、URL 等），通常来自数据库
- `headerItem`：请求头配置列表
- `paramItem`：请求体参数配置列表

## 基础用法

### 1. 创建配置对象

```java
SpelDslConfig config = new SpelDslConfig(baseInfoJson, headerItemJson, paramItemJson);
```

### 2. 使用 Runner 方式（推荐）

```java
Map<String, Object> payload = new HashMap<>();
payload.put("prompt", "hello world");

SpelDsl.Runner runner = SpelDsl.Runner(config, payload);
String header = runner.headerPreviewNew();  // 生成请求头预览
String body = runner.paramPreviewNew();      // 生成请求体预览
String url = runner.urlPreviewNew();         // 生成 URL 预览
```

### 3. 静态方法调用

```java
String header = SpelDsl.getHeaderPreviewNew(config, payload);
String body = SpelDsl.getParamPreviewNew(config, payload);
String url = SpelDsl.getUrlPreviewNew(config, payload);
```

## paramItem 配置说明

### 配置项字段说明

每个 `paramItem` 配置项包含以下字段：

| 字段 | 必填 | 说明 | 示例 |
|------|------|------|------|
| `key` | 是 | 业务入参的 key 名称 | `"prompt"` |
| `category` | 是 | 处理类型：`key`、`map`、`list` | `"key"` |
| `node` | 是 | 目标路径，支持点号和数组索引 | `"content[0].text"` |
| `post_param` | 否 | 写入的目标字段名，为空则取 node 尾段 | `"text"` |
| `spel_temp` | 否 | 模板字符串，`{value}` 会被替换为实际值 | `" --ratio {value}"` |
| `default_value` | 否 | 缺省值，当入参不存在时使用 | `"text"` |
| `value_object` | 否 | 类型提示：`string`、`int`、`double`、`list<string>`、`list<json>`、`map` | `"string"` |

### 使用场景

#### 场景 1：添加一个普通 key（category: key）

**需求：** 将业务入参 `prompt` 映射到请求体的 `prompt` 字段

**配置：**
```json
{
  "key": "prompt",
  "category": "key",
  "node": "prompt",
  "post_param": "prompt",
  "value_object": "string"
}
```

**输入：** `payload.put("prompt", "hello")`  
**输出：** `{"prompt": "hello"}`

---

#### 场景 2：在嵌套对象中添加 key（category: key）

**需求：** 将业务入参 `type` 映射到 `camera_control.type` 字段

**配置：**
```json
{
  "key": "type",
  "category": "key",
  "node": "camera_control.type",
  "post_param": "type",
  "value_object": "string"
}
```

**输入：** `payload.put("type", "track")`  
**输出：** `{"camera_control": {"type": "track"}}`

---

#### 场景 3：在数组指定位置添加 key（category: key）

**需求：** 将业务入参 `prompt` 映射到 `content[0].text` 字段

**配置：**
```json
{
  "key": "prompt",
  "category": "key",
  "node": "content[0].text",
  "post_param": "text",
  "value_object": "string"
}
```

**输入：** `payload.put("prompt", "hello")`  
**输出：** `{"content": [{"text": "hello"}]}`

---

#### 场景 4：使用模板拼接字符串（category: key + spel_temp）

**需求：** 将 `ratio` 参数拼接到 `content[0].text` 字段的末尾

**配置：**
```json
{
  "key": "ratio",
  "category": "key",
  "node": "content[0].text",
  "post_param": "text",
  "spel_temp": " --ratio {value}",
  "value_object": "string"
}
```

**输入：** `payload.put("ratio", "16:9")`  
**输出：** `{"content": [{"text": " --ratio 16:9"}]}`  
**注意：** 如果 `text` 字段已存在，会进行字符串拼接

---

#### 场景 5：在 list 中添加一个 map 对象（category: list）

**需求：** 将业务入参的 `image_list` 数组转换为 `content` 数组中的多个对象

**配置：**
```json
{
  "key": "image_list",
  "category": "list",
  "node": "content",
  "spel_temp": "{\"paramItem\":[{\"key\":\"type\",\"node\":\"type\",\"post_param\":\"type\",\"default_value\":\"image_url\"},{\"key\":\"url\",\"node\":\"image_url.url\",\"post_param\":\"url\"},{\"key\":\"role\",\"node\":\"role\",\"post_param\":\"role\",\"default_value\":\"reference_image\"}]}",
  "value_object": "list<json>"
}
```

**输入：**
```java
List<Map<String, Object>> imageList = new ArrayList<>();
Map<String, Object> image1 = new HashMap<>();
image1.put("url", "https://example.com/image1.png");
image1.put("role", "reference_image");
imageList.add(image1);
payload.put("image_list", imageList);
```

**输出：**
```json
{
  "content": [
    {
      "type": "image_url",
      "image_url": {
        "url": "https://example.com/image1.png"
      },
      "role": "reference_image"
    }
  ]
}
```

**说明：** 
- `spel_temp` 中定义嵌套的 `paramItem`，描述列表中每个元素的结构
- 嵌套 `paramItem` 的 `node` 路径是相对路径（如 `image_url.url`），不需要包含数组索引
- 如果 `content` 数组已存在其他元素，会自动合并

---

#### 场景 6：直接添加一个 map 对象（category: map）

**需求：** 将业务入参的 `camera_control` 对象直接映射到请求体

**配置：**
```json
{
  "key": "camera_control",
  "category": "map",
  "node": "camera_control",
  "post_param": "camera_control",
  "value_object": "map"
}
```

**输入：** `payload.put("camera_control", map)`  
**输出：** `{"camera_control": {...}}`

## baseInfo 配置

`baseInfo` 是可选的 JSON 字符串，通常从数据库读取，包含模型基础信息：

```json
{
  "model": "doubao-seedance-1-0-lite-i2v-250428",
  "Authorization": "Bearer token123",
  "base_url": "https://api.example.com",
  "point": "/api/v3/tasks"
}
```

**说明：**
- `baseInfo` 会先合并到 payload，外部 payload 会覆盖相同 key
- `model` 字段有特殊处理：如果配置项存在但 payload 未提供，会回退到 baseInfo 的 model
- URL 预览 = `base_url` + `point`（自动处理斜杠）

## headerItem 配置

`headerItem` 配置格式与 `paramItem` 相同，用于生成 HTTP 请求头：

```json
{ "headerItem":
  {
    "key": "contentTypeHeader",
    "category": "key",
    "node": "Content-Type",
    "post_param": "Content-Type",
    "default_value": "application/json",
    "value_object": "string"
  }
}
```

## 处理逻辑要点

1. **合并策略：** baseInfo → payload，外部 payload 覆盖 baseInfo
2. **缺省值：** 入参缺失时使用 `default_value`，两者都无则跳过该字段
3. **模板替换：** `spel_temp` 中的 `{value}` 会被替换为实际值
4. **字符串拼接：** 如果目标字段已有字符串值，再次写入字符串时会拼接（累加）
5. **数组合并：** 当多个配置项指向同一个数组（如 `content`）时，会自动合并元素
6. **嵌套处理：** `category: list` 配合嵌套 `paramItem` 可处理复杂的列表结构

## 注意事项

1. **配置格式：** 配置 JSON 需保持规范，工具不做严格校验
2. **默认值：** `default_value` 仅用于确需兜底的字段，无值即跳过字段
3. **模板限制：** `{value}` 模板仅适用于字符串类型
4. **转义字符：** `spel_temp` 中的 JSON 字符串需要正确转义（`\"`）
5. **路径索引：** 数组索引从 0 开始，如 `content[0]` 表示第一个元素
6. **嵌套 paramItem：** `node` 路径写相对路径即可，无需包含父级路径

## 完整示例

参考代码中的示例：
- `SpelDemo`：基础文生视频示例
- `SpelDemoNew`：复杂嵌套对象示例
- `SpelDemoThreeNew`：图生视频（列表处理）示例
