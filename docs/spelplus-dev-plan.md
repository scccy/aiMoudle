# SpEL/JsonPlus 增强开发计划（基于讨论稿）

## 目标
- 在不改动现有 `loadModelTemplateConfig` 的前提下，提供“增量拼装”能力：
  - SpEL 层：支持 base+plus 模板合并，追加/覆盖模型、content、callback 等字段。
  - JsonPlus 层：提供模板编解码、合并与反转义能力，复用到 SpEL 解析。

## SpEL 增强计划（SpEL JSON 追加）
1) **新增配置字段（字符串）**
   - `SpelTemplateConfig` 增加 `aliasMappingJsonPlus`、`paramTemplateJsonPlus`，默认空。
2) **Config 便捷 API**
   - `addAliasMappingPlus(String alias, String realKey)`：解析 plus 映射→追加→回串。
   - `addParamTemplatePlus(String key, String spelExpr)` / `setParamTemplateJsonPlus(String json)`：解析→合并→回串。
3) **引擎合并策略**
   - 解析 base alias/param → 若存在 plus 则用 JsonPlusMerger 合并（默认 AUTO，数组追加，标量覆盖，可扩展覆盖策略）。
   - 解析输出后可选再次合并 plus patch（用于静态补丁），默认关闭。
4) **示例覆盖**
   - 按讨论稿中的增强1~4 提供示例配置（字符串形态），确保能生成预期 param。
5) **测试**
   - 单测覆盖 base、base+plus 覆盖模型、content 追加 image_url（含 role/base64）、callback 覆盖、alias 合并。

## JsonPlus 增强计划
1) **模板编解码**
   - 提供 `JsonPlusTemplateCodec`（已有基础版），用于模板字符串→Map 解析（含 SpEL 转义容错）与 Map→字符串编码。
   - 在 SpEL 引擎中统一调用 Codec，去除重复转义逻辑。
2) **合并能力**
   - 复用 `JsonPlusMerger` 进行 JSONObject 合并，支持策略 AUTO/OVERWRITE/KEEP/APPEND。
   - 如需更细的数组策略（全量覆盖 vs 追加），暴露策略配置。
3) **便捷入口**
   - 提供静态方法：`mergeTemplate(String baseJson, String plusJson)` 返回合并后的 JSON 字符串，供 SpEL/业务复用。
4) **测试**
   - 针对模板编解码的反转义、宽松解析、数组追加/覆盖、别名合并等场景补充单测。

## 验收标准
- 现有 `loadModelTemplateConfig` 行为不变（plus 为空时完全兼容）。
- 通过讨论稿中 4 类增强示例配置，能输出预期 param。
- 对异常转义的模板字符串仍能被 Codec 容错或给出清晰错误提示。
