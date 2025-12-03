# SpEL Plus 增强开发计划

## 目标
在现有 `SpelTemplateEngine` 的基础上，新增 “SpEL Plus” 能力，用于在模板求值后对结果参数进行二次增强（追加/覆盖）。能力依赖 `JsonPlusMerger`，实现“基础请求 + 增强请求”组合输出。

## 背景
- 当前 `SpelTemplateConfig` 可通过 SpEL 生成基础请求，但遇到模型切换、content 扩充等需求时，需要业务层手动写代码追加。
- 我们已实现 `JsonPlusMerger`，具备 add/overwrite/list/map 增强能力，可复用于参数叠加。

## 开发范围
1. **配置层**：在 `SpelTemplateConfig` 中新增字段（暂定 `spelPlusJson`），用于保存增强 JSON；允许通过 DB 或运行期追加。
2. **引擎层**：在 `SpelTemplateEngine#evaluate` 求值完成后，解析 `spelPlusJson`，调用 `JsonPlusMerger.add` 与 `resolvedParam` 合并；支持可选覆盖策略。
3. **结果层**：`SpelEvaluationResult` 需要暴露增强后的最终参数，以及增强前的基础参数（便于调试）。
4. **文档层**：更新 `docs/spel-template-guide.md`、`docs/jsonplus-guide.md`，描述 SpEL Plus 的配置字段、合并流程及示例。
5. **Demo/测试**：在 `SpelDemo` 或单测中新增“增强请求”示例，验证基础 + 增强的行为。

## 实施步骤
1. 设计配置字段：定义 `spelPlusJson` 的结构、来源（DB 字段、运行期 set）。
2. 接入 JsonPlusMerger：在 `SpelTemplateEngine` 中解析增强 JSON，调用 add/overwrite 生成最终 param。
3. Demo 验证：让 `SpelDemo` 读入基础/增强两组 JSON，输出合并结果。
4. 文档更新：同步更新指南与 JSONPlus 文档，说明如何配置增强模块。
5. 测试与回归：针对基础/增强分别做单测或日志对比，确保不影响现有模板。

## 风险与注意事项
- 增强 JSON 必须是合法、可解析的结构，需提供兜底日志。
- 需明确生效顺序：基础模板 → 增强 (add/overwrite) → 最终请求。
- 注意与 `builtinContext`、`payloadContext` 的数据隔离，避免增强过程污染上下文。

## 下一步
- 评审字段命名与 DB 对应关系。
- 确定默认策略（add 或 overwrite）。
- 实施开发并同步 Demo/文档。
