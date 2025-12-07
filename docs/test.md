# SpEL 模板解析与三种增强能力

## 总览
- 基础模板：`envJson`、`aliasMappingJson`、`headerTemplateJson`、`paramTemplateJson`、`urlTemplate`。
- 三种增强：`addkey`、`addlist`、`addmap`，分别补充 KV、List、Map 到最终 `param`。
- 解析流程：读取配置 → 构建上下文 → SpEL 求值 → 按序合并 KV → Map → List。

## 配置字段（重命名版）
- 基础字段
  - `modelName`：模型标识。
  - `envJson`：环境 JSON 字符串。
  - `frontPayloadJson`：前端别名参数 JSON 字符串。
  - `aliasMappingJson`：别名映射 JSON。
  - `headerTemplateJson`：Header 模板 JSON（值为 SpEL）。
  - `paramTemplateJson`：Param 模板 JSON（值为 SpEL）。
  - `urlTemplate`：URL 模板（单字符串 SpEL）。

- addkey（KV 补丁）
  - `addKeyAliasPatchJson`：别名补丁 JSON。
  - `addKeyParamPatchJson`：KV 补丁模板 JSON（值为 SpEL）。

- addlist（数组补丁）
  - `addListAliasPatchJson`：别名补丁 JSON（可选）。
  - `addListTemplateJson`：列表模板（输出 `JSONArray` 或单对象）。
  - `addListTargetKey`：目标数组键，默认 `content`。
  - `addListStrategy`：`APPEND` 或 `OVERWRITE`，默认 `APPEND`。

- addmap（对象补丁）
  - `addMapAliasPatchJson`：别名补丁 JSON（可选）。
  - `addMapTemplateJson`：对象模板（输出 `JSONObject`）。
  - `addMapTargetKey`：挂载到的键（为空表示顶层）。
  - `addMapStrategy`：`AUTO` 或 `OVERWRITE`，默认 `AUTO`。

## 解析顺序与合并策略
- 顺序：`paramTemplateJson` → `addKeyParamPatchJson` → `addMapTemplateJson` → `addListTemplateJson`。
- 策略：
  - KV/Map 使用 `AUTO/OVERWRITE/KEEP_EXISTING/APPEND`（按 `addMapStrategy`）。
  - List 使用 `APPEND/OVERWRITE`（按 `addListStrategy`）。

## 示例
```java
SpelTemplateConfig cfg = new SpelTemplateConfig()
    .setModelName("seedream-3.0")
    .setEnvJson("{\"baseUrl\":\"https://ark.cn-beijing.volces.com/api/v3/\",\"endpoint\":\"contents/generations/tasks\",\"authorization\":\"Bearer 166ed6aa\"}")
    .setAliasMappingJson("{\"a1\":\"text\"}")
    .setHeaderTemplateJson("{\"Authorization\":\"#{#env['authorization']}\"}")
    .setParamTemplateJson("{\"model\":\"#{#env['model']}\"}")
    .setAddKeyAliasPatchJson("{\"a12\":\"refImages\"}")
    .setAddKeyParamPatchJson("{\"watermark\":false}")
    .setAddListTemplateJson("#{T(com.alibaba.fastjson2.JSON).parseArray(T(com.alibaba.fastjson2.JSON).toJSONString(#payload['refImages']))}")
    .setAddListTargetKey("content")
    .setAddListStrategy("APPEND")
    .setAddMapTemplateJson("{\"extra\":{\"level\":2}}")
    .setAddMapStrategy("AUTO");

SpelTemplateEngine engine = new SpelTemplateEngine();
SpelEvaluationResult r = engine.evaluate(cfg);
```

## 存储建议
- 将以上各字段作为文本存储于数据库，保持 JSON 原样；解析器在运行期完成合并。
- 需要兼容旧字段时，可同时保存旧字段与新字段；解析器优先读取重命名字段。