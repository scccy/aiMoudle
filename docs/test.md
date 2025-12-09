# SpEL 模板解析（post_param 驱动版）

## 总览
- 基础模板：`envJson`、`headerTemplateJson`、`postParamJson`（替代手写 `paramTemplateJson`）、`urlTemplate`。`aliasMappingJson` 仅为兼容旧别名，默认可不填。
- 解析流程：读取配置 → 构建上下文（env/frontPayload/builtinContext）→ 按 `post_param` 规则生成最终 POST body → 解析 URL/Header → 发送。
- 三种增强（addkey/addlist/addmap）作为内部实现细节，外部只需关注 `post_param` 的 `category` 与 `node` 来决定合并策略。

## 配置字段（新版）
- `modelName`：模型标识。
- `envJson`：环境 JSON 字符串。
- `frontPayloadJson`：前端入参 JSON（直接使用统一字段 Key）。
- `postParamJson`：`post_param` 配置 JSON（见下）。
- `aliasMappingJson`：旧别名映射 JSON（可选，兼容存量）。
- `headerTemplateJson`：Header 模板 JSON。
- `urlTemplate`：URL 模板。

## post_param 结构
每个元素描述一个字段如何落到最终请求：
- `key`：统一字段 Key（前端入参用它取值）。
- `label`：展示名称。
- `type`：`string/number/float/bool/enum/object/list`。
- `required`：是否必填。
- `defaultValue`：默认值。
- `options`：枚举列表（如 `[{"title":"简单运镜","value":"simple"}]`）。
- `category`：`key`（KV）、`map`（对象合并）、`list`（数组挂载）。
- `node`：目标路径，如 `model`、`camera_control`、`content.list[0].text`。
- `post_param`：目标字段名（当 node 是对象/列表时为键名；或与 node 相同）。
- `spel_temp`：可选，内嵌模板，`{value}` 会替换为 `#payload[key]`，用于文本追加。

## 生成规则
1. 遍历 `post_param`：
   - `category=key`：把 `post_param` 写到 `node`（或顶层）。如有 `spel_temp`，将替换后的字符串附加到目标字段（常用于模型1把 ratio/分辨率拼到 text 尾部）。
   - `category=map`：构造对象后合并到 `node` 对象（内部等价于 addmap）。
   - `category=list`：构造数组/元素后追加或覆盖到 `node`（内部等价于 addlist）。
2. 默认值/枚举/范围校验由前端或校验层完成，模板阶段仅透传。
3. 若提供 `aliasMappingJson`，优先用统一字段 Key；无别名模式下直接用 `key` 取 payload。

## 示例 post_param 配置
```json
{
  "model_name": "model3",
  "post_param": [
    {
      "key": "prompt",
      "label": "提示词",
      "type": "string",
      "required": true,
      "defaultValue": "",
      "category": "key",
      "node": "prompt",
      "post_param": "prompt"
    },
    {
      "key": "aspect_ratio",
      "label": "画幅比例",
      "type": "string",
      "defaultValue": "16:9",
      "category": "key",
      "node": "content.list[0].text",
      "post_param": "text",
      "spel_temp": " --ratio {value}"
    },
    {
      "key": "cfg_scale",
      "label": "CFG",
      "type": "float",
      "defaultValue": 0.7,
      "category": "key",
      "node": "cfg_scale",
      "post_param": "cfg_scale"
    },
    {
      "key": "camera_control.type",
      "label": "运镜类型",
      "type": "enum",
      "defaultValue": "simple",
      "options": [
        { "title": "简单运镜", "value": "simple" },
        { "title": "镜头下压并后退 ➡️ 下移拉远", "value": "down_back" }
      ],
      "category": "map",
      "node": "camera_control",
      "post_param": "type-text"
    }
  ]
}
```

## 生成示例（简化）
- payload 输入：
```json
{
  "prompt": "a cat",
  "aspect_ratio": "16:9",
  "cfg_scale": 0.7,
  "camera_control.type": "simple"
}
```
- 生成后的 Param（示例）：
```json
{
  "prompt": "a cat",
  "cfg_scale": 0.7,
  "camera_control": {
    "type-text": "simple"
  },
  "content": [
    {
      "text": " --ratio 16:9"
    }
  ]
}
```

## 存储与兼容
- 推荐将 `postParamJson`、`envJson`、`headerTemplateJson`、`urlTemplate` 以文本形式存库。
- 需要兼容旧别名时保留 `aliasMappingJson`；新接入可直接用统一字段 Key + `post_param` 生成。 
