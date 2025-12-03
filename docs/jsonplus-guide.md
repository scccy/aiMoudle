# JsonPlus 合并能力说明

本文总结 `JsonPlusMerger`（位于 `com.origin.aimodel.util.jsonplus` 包）提供的能力、使用方式与示例输出，便于在 SpEL 或其它动态配置场景中复用。

## 1. 核心能力
- **add**：默认增量合并。对 `JSONObject` 递归处理、对 `JSONArray` 追加、对普通字段覆盖；当旧值为单值，新值为数组时会自动升级为数组并合并所有元素。
- **overwrite**：强制覆盖。无论旧值类型为何，直接用新值替换，适合“数据库配置优先”或“临时参数覆盖”的场景。
- **appendListItem / mergeList**：面向列表字段追加单个或多个元素，缺失时自动创建 `JSONArray`，内部始终 clone 新元素后写入。
- **mergeObject**：在指定 key 上递归合并嵌套 `JSONObject`，内部默认采用 `add` 策略。

## 2. Demo 输出（参考 `JsonPlusDemo`）
所有示例均打印 `target(before)`、`addition`、`target(after)` 便于观察。

| 示例 | 调用片段 | 效果说明 |
|------|----------|----------|
| add | `JsonPlusMerger.add(target, addition);` | `content` 列表追加 image、`extra` 被新增 |
| key 覆盖 | `JsonPlusMerger.overwrite(target, addition);` | `watermark` 被覆盖为 `false`，`content` 用新数组替换 |
| list 追加 | `JsonPlusMerger.add(target, addition);` | `tags`、`levels` 均在原列表基础上追加新值 |
| map KV | 同上 | `extra` 新增 `level`，并引入新的 `meta` |
| map key -> list | 同上 | `payload.content` 里的 `JSONArray` 在嵌套结构中被追加 |
| map key -> map | 同上 | `payload.config` 的字段递归合并，`trace` 新增 |

## 3. 使用建议
1. **按照场景选策略**：默认使用 `add`，若需要明确覆盖可切换为 `overwrite`。
2. **类型容错**：`add` 会在必要时自动把单值升级为数组；`overwrite` 则直接替换，避免类型冲突。
3. **输入安全**：工具内部会 clone `JSONObject`/`JSONArray` 后再写入，`addition` 不会被修改，可复用。
4. **快速验证**：运行 `JsonPlusDemo` 或复制其中的示例到单测里即可查看实际行为。

## 4. 后续扩展方向
- 支持列表去重 Hook（例如根据某个字段去重）。
- 提供 JSON 字符串直接合并的便捷入口。
- 将 Demo 中的示例沉淀为 JUnit 单测，保障后续迭代稳定性。
