## SpEL Param Plus 升级开发计划

### 1. 背景
现有模板可以替换 url、header、param，但在追加多段 `content`（text + image_url）或嵌套结构时不够灵活，需要一个“参数增强层”统一描述追加的 key/list/map。

### 2. 核心目标
| 目标 | 说明 |
| --- | --- |
| ParamPlus 通用表达 | `SpelTemplateConfig` 提供 `paramPlusJson` 统一入口，后续可通过链式方法追加 key/list/map。 |
| 解析与合并 | `SpelTemplateEngine` 解析 `paramPlusJson`，并在 SpEL 求值后由 `ParamPlusMerger` 深度合并到 `resolvedParam`。 |
| Content 组装 | 引入 `ContentAssembler`，负责把文本指令和额外内容协议化输出，支持 text、image_url 等类型。 |
| 数据库存储 | `AiModelMp` 新增 `template_param_plus` 字段；服务读取后直接传入 `SpelTemplateConfig`，实现配置化。 |
| 前端解耦 | 请求 VO 使用 `Map<String,Object> params`，别名与真实字段由 `aliasMappingJson` 控制；`paramPlus` 仅承担固定追加内容。 |

### 3. 计划步骤
1. **配置层**：在 `SpelTemplateConfig` 新增 `paramPlusJson` 及若干辅助方法，默认值为空字符串。  
2. **解析层**：`SpelTemplateEngine` 解析 `paramPlusJson`，生成通用对象；`ParamPlusMerger` 实现深度合并，确保不会覆盖已有字段。  
3. **内容组装**：`ContentAssembler` 把文本 + 命令拼接成 `text`，并从 `paramPlus` 中取额外列表（如 image_url）追加。  
4. **数据库与示例**：实体 `AiModelMp` 增加 `template_param_plus`；文档补充 text + image_url 的 JSON 示例。  
5. **测试验证**：在 `AiModelCallApplicationTests` 或新测试中模拟 `paramPlus`，验证生成的请求体符合文档格式。

### 4. 风险提示
1. 深度合并要区分覆盖与追加，避免BREAK；`ParamPlusMerger` 需要精确控制 Map/List 合并逻辑。  
2. SpEL 表达式中引用 `ContentAssembler` 需使用完整类名，并保证打包后可访问。  
3. 数据库存储的 JSON 必须合法；必要时在解析失败时添加日志或回退策略。


Param Plus 实施清单

类新增

ContentAssembler（src/main/java/com/origin/aimodel/util/spel/ContentAssembler.java）：构建 content 数组，当前已支持将文本命令与 paramPlus 中的额外内容（如 image_url）拼接输出；功能已完成。
ParamPlusMerger（src/main/java/com/origin/aimodel/util/spel/ParamPlusMerger.java）：负责把 paramPlusJson 解析出的结构深度合并进最终请求体；功能已完成。
现有类/方法改造

SpelTemplateConfig
新增字段 paramPlusJson 和默认上下文设置，允许 DB 配置附加结构；已完成（src/main/java/com/origin/aimodel/util/spel/SpelTemplateConfig.java）。
SpelTemplateEngine
在解析阶段读取 paramPlusJson；在 SpEL 求值后由 ParamPlusMerger 合并结果，并提供 parseJsonGeneric 助手；已完成（src/main/java/com/origin/aimodel/util/spel/SpelTemplateEngine.java）。
AiModelMp
新增 templateParamPlus 字段，DB 可直接存储 paramPlus JSON；已完成（src/main/java/com/origin/aimodel/domain/mp/AiModelMp.java）。
SpelDemo
读取 templateParamPlus 并传给 SpelTemplateConfig；taskStart 接收前端 params 填充 payload；已完成（src/main/java/com/origin/aimodel/util/spel/SpelDemo.java）。
AiTaskQuery
新增 Map<String,Object> params，用于承载 b1/b2… 等前端字段；已完成（src/main/java/com/origin/aimodel/domain/vo/AiTaskQuery.java）。
目标对照

ParamPlus 通用表达 → 通过 paramPlusJson + 链式配置实现，状态：完成。
解析/合并机制 → SpelTemplateEngine + ParamPlusMerger 已落地，状态：完成。
Content 组装 → ContentAssembler.buildContent 已替代原有字符串拼接，支持 text + image_url 等；状态：完成。
数据库扩展 → AiModelMp 新增列，SpelDemo 读取；状态：完成。
前端解耦 → AiTaskQuery.params、aliasMappingJson 控制映射，paramPlus 由 DB 定义；状态：完成。
若后续还需额外的 paramPlus 重载或更多 type（audio 等），可在 ContentAssembler 和 ParamPlusMerger 中继续扩展。
