# SpEL Plus 扩展计划（一）：指定格式 List 追加到 Map

## 背景与目标
- 现有 plus 方案通过整段 `paramTemplateJsonPlus` 拼接数组，模板可读性和可复用性较差。
- 目标：提供一个通用的“向 Map 中指定数组字段追加元素列表”能力，适配 reference_image、多帧、附件等场景。

## 字段命名统一与兼容策略
- 现有字段与新命名（保持兼容，优先读取新字段）：
  - `aliasMappingJsonPlus` → `aliasPatchJson`
  - `paramTemplateJsonPlus` → `paramPatchTemplateJson`
- 新增数组追加能力（原“指定格式 List 追加到 Map”）：
  - `addListAliasMappingJsonPlus` → `arrayPatchAliasJson`
  - `addListPlusTemplateJson` → `arrayPatchTemplateJson`
  - `addListTargetKey` → `arrayPatchTargetKey`（默认 `content`）
  - `addListMergeStrategy` → `arrayPatchStrategy`（默认 `APPEND`，支持 `OVERWRITE`）
- 新增 Map 追加/覆盖能力（map add map）：
  - `mapPatchTemplateJson`：要 merge/覆盖到目标 Map 的模板（SpEL 字符串，输出 JSONObject）
  - `mapPatchTargetKey`：目标 Map key，默认顶层（直接 merge 到 resolvedParam）
  - `mapPatchStrategy`：默认 `AUTO`（JSONObject 深度合并，标量覆盖），可选 `OVERWRITE`。

## 提议的配置字段（数组追加场景）
- `arrayPatchAliasJson`：追加/覆盖 alias → 实际字段映射，解决前端新入参的映射问题。
- `arrayPatchTemplateJson`：定义要追加的列表模板（SpEL 字符串），解析后得到 JSONArray 或单个对象；支持引用 payload/env。
- `arrayPatchTargetKey`：可选，指定要追加的目标数组 key，默认 `content`。
- `arrayPatchStrategy`：可选，默认 `APPEND`（数组追加）；可扩展 `OVERWRITE`（整段替换）。

## 引擎侧处理流程（草案）
1. 按当前流程解析 base alias/param，得到 `resolvedParam`（包含数组字段）。
2. 若存在 `addListAliasMappingJsonPlus`，先合并 alias（保持与现有 merge 行为一致）。
3. 若存在 `addListPlusTemplateJson`：
   - 解析模板 → `additionList`（JSONArray；若是单对象则包一层数组）。
   - 取 `targetKey = addListTargetKey`（默认 content），确保 `resolvedParam` 中该 key 为数组（不存在则创建空数组）。
   - 按 `addListMergeStrategy` 将 `additionList` 写入目标数组（默认尾部追加）。
4. 继续后续流程/日志输出，行为与 base 兼容。

## 数据与示例（预期形态）
- alias 片段：
  ```json
  {"a12":"refImage1","a13":"refImage2","a14":"refImage3"}
  ```
- addList 模板示例：
  ```json
  {
    "addListPlusTemplateJson": "#{T(com.alibaba.fastjson2.JSON).parseArray('[{\"type\":\"image_url\",\"image_url\":{\"url\":\"' + #payload['refImage1'] + '\"},\"role\":\"reference_image\"},{\"type\":\"image_url\",\"image_url\":{\"url\":\"' + #payload['refImage2'] + '\"},\"role\":\"reference_image\"}]')}"
  }
  ```

## 兼容性与落地步骤
- plus 字段为空时，行为与现有逻辑一致。
- 新增字段均为可选，旧配置无需调整；引擎优先使用统一后的字段名。
- 实施步骤：定义配置字段 → SpelTemplateConfig 补字段 → SpelTemplateEngine 增加 array/map patch 逻辑 → 增加单元测试覆盖默认/追加/覆盖策略。

## 验证要点
- plus 为空时输出不变。
- targetKey 不存在时自动创建数组。
- APPEND 模式保持 base 顺序，OVERWRITE 模式替换整个目标数组。
- alias 合并后，payload 新字段能够被解析到追加模板中。
- map patch 覆盖时，不影响未声明字段；AUTO 策略对嵌套对象深度合并。
