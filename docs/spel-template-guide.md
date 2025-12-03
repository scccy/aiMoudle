# SpEL 模板工具指南（基于 AiModelMp）

`AiModelMp` 表承担了模型接入的所有配置，因此本文围绕该实体讲解如何把数据库字段映射到 `SpelTemplateConfig`，并通过 `SpelTemplateEngine` 生成最终的请求 URL/Header/Param。文末保留 `seedream-3.0` 样例，帮助你核对存量数据。

## 1. AiModelMp 字段与 SpEL 配置映射

> 表结构以 `AiModelMp` 实体为准，如有新增列可按同样思路扩展。以下建议在建表/录入时保持 JSON 的可读性与可验证性。

| AiModelMp 字段 | 含义 / 建议内容 | 对应的 SpEL 配置字段 |
|----------------|----------------|----------------------|
| `model_name` | 模型唯一标识，业务检索 & 日志打印用 | `SpelTemplateConfig.modelName` |
| `origin_name` | 可选：底层模型或厂商名 | 作为 `env.origin` 或接入日志字段 |
| `base_url` | 模型服务根地址，例如 `https://ark.cn-beijing.volces.com/api/v3/` | `envJson.baseUrl` |
| `point` | 具体路径或资源位，如 `generations/tasks` | `envJson.endpoint`，并用于 `urlTemplate` |
| `authorization` | Bearer Token 或其他鉴权信息 | `envJson.authorization`，进而注入 Header |
| `in_parameter` | 前端别名参数的 JSON 示例（`{"a1":"text",...}`）或默认值 | `frontPayloadJson`；调试阶段也可放空对象 `{}` |
| `template_attribute_mapping` | 别名到真实字段的映射 JSON，例如 `{"a1":"text"}` | `aliasMappingJson` |
| `template_header` | Header 模板 JSON，value 允许写 SpEL 表达式 | `headerTemplateJson` |
| `template_spel` | Param 模板 JSON，value 书写 SpEL 表达式 | `paramTemplateJson` |
| `out_parameter` | 预留字段：可记录期望返回体结构 | 暂未注入 SpEL，可用于调用方做断言 |

根据上表即可直接构建 `SpelTemplateConfig`，不再需要手写配置。若未来要补充 `contextVariable` 或 `contextData` 等高级能力，建议在 AiModelMp 中新增 JSON 列（例如 `template_context_variable`），再挂接到 `SpelTemplateConfig` 的对应字段。

## 2. 从 AiModelMp 构造 SpelTemplateConfig

```java
SpelTemplateConfig config = new SpelTemplateConfig()
        .setModelName(aiModel.getModelName())
        .setUrlTemplate("#{#env['baseUrl']}#{#env['endpoint']}")
        .setEnvJson(JsonUtils.toJson(Map.of(
                "baseUrl", aiModel.getBasUrl(),
                "endpoint", aiModel.getPoint(),
                "authorization", aiModel.getAuthorization(),
                "origin", aiModel.getOriginName())))
        .setFrontPayloadJson(defaultIfBlank(aiModel.getInParameter(), "{}"))
        .setAliasMappingJson(aiModel.getTemplateAttributeMapping())
        .setHeaderTemplateJson(aiModel.getTemplateHeader())
        .setParamTemplateJson(aiModel.getTemplateSpel());
```

> `JsonUtils` 仅作示例，使用任何可靠的 JSON 序列化工具均可。`defaultIfBlank` 可替换为自有工具，确保字段为空时仍返回合法 JSON。

### 2.1 数据流
1. **读取配置**：`AiModelMpService` 根据 `model_name` 查询一行记录。
2. **构造配置对象**：按照上方代码注入 env/payload/alias/template 等 JSON。
3. **执行引擎**：`SpelTemplateEngine#evaluate(config)` 会完成 JSON 解析、上下文注册与模板求值。
4. **执行请求**：`SpelEvaluationResult` 返回 `resolvedUrl`、`resolvedHeader`、`resolvedParam`，直接交给 HTTP 客户端。

### 2.2 调试与排查
- `result.getContextDataSource()`：查看最终注入 SpEL 的变量，确认别名映射是否生效。
- 在 `template_spel` 中引用静态方法（如 `T(java.lang.System).currentTimeMillis()`) 时，应在单测/沙箱环境提前覆盖。
- 若某个字段未取到值，优先检查 `template_attribute_mapping` 是否遗漏对应别名。

## 3. 字段校验清单

| 维度 | 校验点 | 说明 |
|------|--------|------|
| URL | `base_url` 与 `point` 必须能组成合法地址，结尾 `/` 需统一 | 建议 `base_url` 以 `/` 结尾，`point` 不带 `/` 前缀，避免重复斜杠 |
| Header | `template_header` 的 value 必须是可执行的 SpEL 字符串 | 例如 `"Authorization":"#{#env['authorization']}"` |
| Param | `template_spel` 必须是 JSON 字符串，value 允许调用工具类 | 推荐将复杂字符串拼接抽到 Java 工具类，模板内保持可读性 |
| Payload | `in_parameter` 仅用于示例/默认值，真实入参由前端请求覆盖 | 当字段缺失时，SpEL 求值会返回 `null`，可在模板里用 `?:` 做兜底 |
| 别名 | `template_attribute_mapping` 需覆盖所有前端可见别名 | 缺失映射会导致 `payload` 中取不到真实字段 |

## 4. seedream-3.0 实际数据示例

> 以下内容直接引用 `docs/test.md` 中的存量数据，可按此落库。注意 `template_spel` 的 `content` 借助 Fastjson2 解析字符串。

- **env_json（拆分自多列）**
  ```json
  {
    "model": "seedream-3.0",
    "origin": "doubaoseedream-3-0-t2i-250415",
    "baseUrl": "https://ark.cn-beijing.volces.com/api/v3/",
    "endpoint": "generations/tasks",
    "authorization": "Bearer 166ed6aa"
  }
  ```

- **template_attribute_mapping**
  ```json
  {"a1": "text", "a2": "resolution", "a3": "ratio", "a4": "duration", "a5": "frames", "a6": "framesPerSecond", "a7": "seed", "a8": "cameraFixed", "a9": "watermark"}
  ```

- **template_header**
  ```json
  {"Content-Type": "application/json", "Authorization": "#{#env['authorization']}"}
  ```

- **template_spel**
  ```json
  {
    "model": "#{#env['model']}",
    "content": "#{T(com.alibaba.fastjson2.JSON).parseArray('[{\"type\":\"text\",\"text\":\"'"
        + #payload['text']
        + (#payload['resolution'] != null ? ' --resolution ' + #payload['resolution'] : '')
        + (#payload['ratio'] != null ? ' --ratio ' + #payload['ratio'] : '')
        + (#payload['duration'] != null ? ' --duration ' + #payload['duration'] : '')
        + (#payload['frames'] != null ? ' --frames ' + #payload['frames'] : '')
        + (#payload['framesPerSecond'] != null ? ' --framespersecond ' + #payload['framesPerSecond'] : '')
        + (#payload['seed'] != null ? ' --seed ' + #payload['seed'] : '')
        + (#payload['cameraFixed'] != null ? ' --camerafixed ' + #payload['cameraFixed'] : '')
        + (#payload['watermark'] != null ? ' --watermark ' + #payload['watermark'] : '')
        + "\"}]')}",
    "resolution": "#{#payload['resolution']}",
    "ratio": "#{#payload['ratio']}",
    "duration": "#{#payload['duration']}",
    "frames": "#{#payload['frames']}",
    "frames_per_second": "#{#payload['framesPerSecond']}",
    "seed": "#{#payload['seed']}",
    "camera_fixed": "#{#payload['cameraFixed']}",
    "watermark": "#{#payload['watermark']}"
  }
  ```

### 4.1 解析结果核对
- `resolvedUrl`：`https://ark.cn-beijing.volces.com/api/v3/generations/tasks`
- `resolvedHeader`：`{"Content-Type":"application/json","Authorization":"Bearer 166ed6aa"}`
- `resolvedParam`：`content` 通过字符串拼接 + Fastjson2 解析生成，其它字段按别名映射直接取值。

### 4.2 实施提示
1. 更新 token 或 endpoint 时，仅需修改 `authorization`、`point` 等列，无须代码变更。
2. 拼接长字符串时建议配合单元测试校验引号与转义，避免 JSON 不合法。
3. 若未来希望提升可读性，可改用专用工具类（例如 `ContentAssembler`）替换 `content` 内的大段表达式。

---

通过以上规范，可以让 `AiModelMp` 成为所有模型接入的单一事实来源：DB 负责配置，SpEL 负责解析，业务层只做查询与执行，后续新增模型只需插入一行数据即可完成“无代码”集成。
