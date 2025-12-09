# SpEL 模板解析（post_param 驱动版）

## 1. 概述

SpEL 模板解析引擎是一个基于 Spring Expression Language (SpEL) 的参数映射和转换工具，专门用于将业务参数转换为各种 AI 模型 API 所需的请求格式。该引擎通过 `post_param` 配置驱动，支持动态生成复杂的 JSON 结构并实现参数拼接。

## 2. 核心特性

- **动态结构生成**：根据配置自动创建所需的 JSON 结构
- **灵活参数映射**：支持多种参数映射和转换方式
- **模板化拼接**：通过模板实现参数值的灵活拼接
- **路径定位**：通过 JSON 路径语法精确定位字段位置
- **向后兼容**：保留并兼容原有的配置方式

## 3. 工作原理

### 3.1 配置驱动

引擎通过 `post_param` JSON 配置驱动整个转换过程，每条配置项定义了一个参数如何映射到目标结构中。

### 3.2 结构预生成

在参数填充前，引擎会根据所有配置项的 `node` 路径预生成所需的 JSON 结构，确保后续操作的目标位置存在。

### 3.3 参数填充与拼接

遍历所有配置项，根据 `category` 类型和 `spel_temp` 模板进行参数填充和拼接。

## 4. 配置详解

### 4.1 PostParamItem 配置项

每个配置项都是一个 JSON 对象，包含以下字段：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| key | String | 是 | 参数的键名，用于从输入参数中获取值 |
| category | String | 是 | 处理类型，支持 key/map/list |
| node | String | 是 | JSON 路径，表示参数在目标结构中的位置 |
| post_param | String | 否 | 目标字段名，如果不填则直接设置节点值 |
| spel_temp | String | 否 | 轻量模板，用于参数值的转换和拼接 |
| default_value | String | 否 | 默认值，当参数不存在时使用 |
| value_object | String | 否 | 值对象类型。key 时支持 string/int/double，list 时支持 list<string>/list<json> |

#### 4.1.1 value_object 字段详解

`value_object` 用于指定参数值的数据类型，帮助引擎在写入或拼接时做正确的类型处理：

1. **key 类型**：
   - `string`：字符串
   - `int`：整数
   - `double`：浮点数

2. **list 类型**：
   - `list<string>`：字符串列表
   - `list<json>`：JSON 对象列表

3. **map 类型**：
   - 通常不需要显式指定 `value_object`，直接按对象合并

### 4.2 Node 路径语法

使用标准 JSON 路径语法定义字段位置：

- `field` - 根级别字段
- `nested.field` - 嵌套对象字段
- `array[0].field` - 数组元素字段
- `deeply.nested[1].field` - 深层嵌套结构

### 4.3 轻量模板（spel_temp）

`spel_temp` 目前是一个轻量字符串模板，占位符 `{value}` 会被当前参数值替换，适合做前后缀拼接。暂不支持完整的 SpEL 语法。

- `{value}` - 直接使用参数值
- `prefix {value}` - 添加前缀
- `{value} suffix` - 添加后缀
- `prefix {value} suffix` - 添加前后缀

### 4.4 SpEL 扩展模式（设计对齐）

如需更灵活的表达式（与 `spel-template-guide.md`/`spelplus-dev-plan.md` 保持一致），可以按“扩展模式”解析 `spel_temp`：将 `#{ ... }` 视为完整 SpEL 表达式，提供上下文 `#payload`（入参 Map）、`#env`（配置环境）、`#defaultValue` 等变量。示例：

```json
{
  "key": "prompt",
  "category": "key",
  "node": "content[0].text",
  "post_param": "text",
  "spel_temp": "#{#payload['prompt'] + (#payload['ratio'] != null ? ' --ratio ' + #payload['ratio'] : ' --ratio 16:9')}",
  "value_object": "string"
}
```

- 轻量模式：仍然使用 `{value}` 占位符，不需要 SpEL 解析器。
- 扩展模式：当 `spel_temp` 以 `#{` 开头时，进入 SpEL 解析，支持 `?:`、三目、静态方法等表达式。未启用扩展时，按普通字符串处理。

## 5. 处理类型（Category）

### 5.1 key 类型

最常见的处理类型，用于设置普通字段值：

```json
{
  "key": "model",
  "category": "key",
  "node": "model",
  "post_param": "model_name"
}
```

### 5.2 map 类型

用于将参数值合并到指定对象结构中。多个 map 类型的配置项可以共同作用于同一个对象，每个配置项负责设置该对象的一个字段：

```json
{
  "key": "type",
  "category": "map",
  "node": "camera_control",
  "post_param": "type"
}
```

此配置表示：
- 从输入参数中获取 `type` 的值
- 将该值作为 `type` 字段添加到 `camera_control` 对象中

如果有多个类似的配置项共同作用于 `camera_control` 对象，最终会生成一个完整的嵌套对象。

### 5.3 list 类型

用于向数组添加元素：

```json
{
  "key": "image_item",
  "category": "list",
  "node": "content",
}
```

## 6. 示例实现与调试

为了更好地理解和实现 SpEL 模板引擎，我们将通过具体的示例来逐步演示其功能。

### 示例一：基础参数映射

#### 6.1.1 输入参数

```
{
  "model": "doubao-seedance-1-0-pro-250528",
  "contentType": "text",
  "prompt": "多个镜头。一名侦探进入一间光线昏暗的房间。他检查桌上的线索，手里拿起桌上的某个物品。镜头转向他正在思索。",
  "ratio": "16:9"
}
```

#### 6.1.2 配置项

```
[
  {
    "key": "model",
    "category": "key",
    "node": "model",
    "post_param": "model_name",
    "value_object": "string"
  },
  {
    "key": "contentType",
    "category": "key",
    "node": "content[0]",
    "post_param": "type",
    "value_object": "string"
  },
  {
    "key": "prompt",
    "category": "key",
    "node": "content[0].text",
    "post_param": "text",
    "spel_temp": "{value}",
    "value_object": "string"
  },
  {
    "key": "ratio",
    "category": "key",
    "node": "content[0].text",
    "post_param": "text",
    "spel_temp": " --ratio {value}",
    "value_object": "string"
  }
]
```

#### 6.1.3 处理过程

1. 首先根据 `node` 路径预生成结构，确保 `model`、`content[0]`、`content[0].text` 等节点存在
2. 然后按照配置顺序依次填充各个字段：
   - 将 `model` 参数值填充到 `model.model_name` 字段
   - 将 `contentType` 参数值填充到 `content[0].type` 字段
   - 将 `prompt` 参数值通过模板 `{value}` 填充到 `content[0].text` 字段
   - 将 `ratio` 参数值通过模板 ` --ratio {value}` 拼接到 `content[0].text` 字段

#### 6.1.4 期望输出结果

```
{
  "model": "doubao-seedance-1-0-pro-250528",
  "content": [
    {
      "type": "text",
      "text": "多个镜头。一名侦探进入一间光线昏暗的房间。他检查桌上的线索，手里拿起桌上的某个物品。镜头转向他正在思索。 --ratio 16:9"
    }
  ]
}
```

### 示例二：复杂嵌套结构映射

#### 6.2.1 输入参数

```
{
  "model_name": "sdxl-v1.2",
  "prompt": "a futuristic city at night with neon lights",
  "negative_prompt": "low resolution, blurry, distorted face",
  "cfg_scale": 7,
  "mode": "image",
  "type": "track",
  "config": "smooth",
  "horizontal": 10,
  "vertical": 5,
  "pan": 3,
  "tilt": 2,
  "roll": 0,
  "zoom": 1.2,
  "aspect_ratio": "16:9",
  "duration": 5,
  "callback_url": "http://example.com/callback",
  "external_task_id": "task-123456"
}
```

#### 6.2.2 配置项

```
[
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
]
```

#### 6.2.3 处理过程

1. 首先根据 `node` 路径预生成结构，确保 `model_name`、`prompt`、`camera_control` 等节点存在
2. 然后按照配置顺序依次填充各个字段：
   - 前5个配置项分别填充到根级别的字段
   - 接下来的12个配置项通过 `camera_control.xxx` 路径填充到 `camera_control` 对象的相应字段中
   - 最后2个配置项填充到根级别的字段

#### 6.2.4 期望输出结果

```
{
  "model_name": "sdxl-v1.2",
  "prompt": "a futuristic city at night with neon lights",
  "negative_prompt": "low resolution, blurry, distorted face",
  "cfg_scale": 7,
  "mode": "image",
  "camera_control": {
    "type": "track",
    "config": "smooth",
    "horizontal": 10,
    "vertical": 5,
    "pan": 3,
    "tilt": 2,
    "roll": 0,
    "zoom": 1.2
  },
  "aspect_ratio": "16:9",
  "duration": 5,
  "callback_url": "http://example.com/callback",
  "external_task_id": "task-123456"
}
```

### 示例三：数组结构映射

#### 6.3.1 输入参数

```
{
  "image1": "http://example.com/image1.jpg",
  "image2": "http://example.com/image2.jpg",
  "image3": "http://example.com/image3.jpg"
}
```

#### 6.3.2 配置项

```
[
  {
    "key": "image1",
    "category": "list",
    "node": "images",
    "value_object": "list<string>"
  },
  {
    "key": "image2",
    "category": "list",
    "node": "images",
    "value_object": "list<string>"
  },
  {
    "key": "image3",
    "category": "list",
    "node": "images",
    "value_object": "list<string>"
  }
]
```

#### 6.3.3 处理过程

1. 首先根据 `node` 路径预生成结构，确保 `images` 数组节点存在
2. 然后按照配置顺序依次将各个参数值添加到 `images` 数组中

#### 6.3.4 期望输出结果

```
{
  "images": [
    "http://example.com/image1.jpg",
    "http://example.com/image2.jpg",
    "http://example.com/image3.jpg"
  ]
}
```

## 7. 最佳实践

### 7.1 结构设计原则

1. **明确路径**：使用清晰的 node 路径定义结构位置
2. **合理分组**：将操作同一节点的配置项放在一起
3. **模板复用**：对于相似的拼接模式，使用统一的模板格式

### 7.2 参数处理顺序

配置项的处理顺序很重要，特别是对于需要拼接的字段，应按照拼接顺序排列配置项。

### 7.3 错误处理

1. **参数校验**：确保必填参数存在
2. **默认值设置**：为可选参数提供合理的默认值
3. **类型一致性**：确保拼接的参数值类型一致，推荐统一使用字符串

### 7.4 数据类型处理

`value_object` 帮助引擎正确处理数据类型：

1. **字符串类型**：默认类型，适用于大多数文本数据
2. **数值类型**：对于需要数学运算的场景，如 `cfg_scale`、`duration` 等
3. **列表类型**：`list<string>`/`list<json>`，确保数组元素类型明确

引擎会根据 `value_object` 指定的类型对输入值进行适当转换，确保输出 JSON 符合 API 要求。

## 8. 注意事项

1. **处理顺序**：配置项的顺序会影响最终结果，特别是拼接操作
2. **类型一致性**：拼接时确保所有值都是字符串类型
3. **路径存在性**：确保 node 路径中的所有父级节点都能正确创建
4. **特殊字符转义**：注意参数值中的特殊字符可能需要转义处理
