# 反向解析 DSL 优化与落库方案

## 1. 目标
- 把“可映射的业务入参”与“模板常量/结构”拆开，避免常量被当作可映射字段。
- 支持可变列表（如多组 `{role, image_url}`）的抽取、编辑、正向生成。
- DSL 输出即为可直接落库的 `MappingItem` 列表，`ReverseParseServiceImpl` 只需调用 DSL 结果并持久化。

## 2. DSL 字段与约定
- 新增/规范字段
  - `value_source`：`CONST | USER | DERIVED`，区分常量 / 用户输入 / 推导。
  - `node/path`：子字段用相对路径，支持 `[*]` 占位，例如 `content[*].image_url.url`。
  - `value_object`：扩充 `list<string>`、`list<json>`、`map`、`string` 等，用于前端控件与校验。
  - `validate`：保留原有结构，支持 `enum` / `required` / `range`。
- 语义约定
  - 可映射项：`category=key` 且 `value_source=USER`。
  - 模板常量：`value_source=CONST` + `default_value`（如 `type=image_url`）。
  - 列表模板：父项 `category=list` / `value_object=list<json>`，子字段定义放在 `spel_temp`（或新字段 `childItems`），子字段包含常量和可映射项。

## 3. 反向解析算法（列表场景）
1) 识别可重复分组：在数组中发现结构相同的元素（如 `type=image_url`），抽象为“列表模板”。
2) 子字段抽取：
   - 常量字段（`type`、固定 `role` 等）标记为 `CONST`，不暴露给用户。
   - 可映射字段（可变 `role`、`image_url.url` 等）标记为 `USER`，路径用 `content[*].image_url.url`，需要的枚举放入 `validate.enum`。
3) DSL 输出：
   - 父列表项：`key=content`，`category=list`，`value_object=list<json>`，`node=content`，`spel_temp/childItems` 描述子字段模板。
   - 子字段：相对路径 + `value_source` + `category/default_value/validate`，包含常量与可映射项。
4) 正向生成（指引给正向生成端）：对 `content[*]` 采用模板复制，填入用户数组值，常量自动带上。
5) 反向抽取：对数组中满足模板条件的元素聚合为数组型映射项，值形如 `[{role,url}, ...]`。

## 4. 落库规则（dim_ai_model_item）
- DSL 直接输出“可落库的扁平列表”，`ReverseParseServiceImpl` 不再手工组装。
- 扁平化约定：
  - 父列表项一条记录：`category=list`，`value_object=list<json>`，`node` 为父路径，`spel_temp` 存子字段定义（含常量、可映射子项的路径/默认值/校验）。
  - 子字段定义存于父项的 `spel_temp`（保持兼容），必要时可扩展为 `childItems` 字段。
  - 常量字段标记 `value_source=CONST`，避免被当作 `category=key`。
- `value_object` 使用示例：
  - 简单数组：`list<string>`。
  - 对象数组：`list<json>`（配合子字段定义）。

## 5. 代码修改点
- `MappingItem`（模型/VO）：新增 `value_source`，可选增加 `path/childItems`；`value_object` 支持 list。
- `ReverseDslFactory` / `ReverseParser`：
  - 识别列表模板，生成父列表项 + 子字段定义（含 `value_source`）。
  - 对可重复元素使用 `[*]` 路径；对子字段用相对路径。
  - 常量字段赋 `value_source=CONST`，可映射赋 `USER`。
- `ReverseParseServiceImpl`：
  - `generateConfig`：直接调用 `ReverseDsl.build(...).getParamItems()`、`.withHeaders(...).getHeaderItems()`，不再额外拼装基础映射；返回列表已包含落库所需字段。
  - `saveConfig`：按 DSL 返回的 `MappingItem` 列表保存；父列表项的 `spel_temp` 携带子字段定义直接落库。
- 前端正向解析页面：
  - `category=list` + `value_object=list<json>`：渲染为可增删行的数组控件，行内只展示 `value_source=USER` 的输入项，常量隐藏/自动填。
  - `value_object=list<string>`：多行文本/可增删的字符串数组。

## 6. 开发顺序建议
1) 扩展 `MappingItem`（`value_source`、list 类型支持、可选 `childItems`）。
2) `ReverseParser` / `ReverseDslFactory`：识别列表模板，生成父列表项 + 子字段定义，标记常量/可映射。
3) `ReverseParseServiceImpl`：精简为“调用 DSL → 直接落库”。
4) 前端正向解析：识别 `category=list` + `value_object` 渲染数组控件，行内按 `value_source` 判定是否展示输入。
5) 用现有 `chat2` / Ark curl 场景做回归：反向解析 → 落库 → 正向生成，验证可变条目生效。

## 7. 示例（多组 role + image_url）
- 父项：`content`（list），`value_object=list<json>`，`node=content`。
- 子字段模板（存于父项 `spel_temp` 或 `childItems`）：
  - `type`：`value_source=CONST`，`default_value=image_url`。
  - `role`：`value_source=USER`（如有枚举写在 `validate.enum`；若固定则改为 `CONST` + 默认值）。
  - `image_url.url`：`value_source=USER`，必填。
- 前端输入格式：`[{ "role": "reference_image", "url": "https://...img1.png" }, { "role": "mask", "url": "https://...mask.png" }]`。
- 正向生成：对数组逐元素套模板，生成 `content` 中多条 `{type, role, image_url.url}`；常量自动填入。
