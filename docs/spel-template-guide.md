# SpEL 模板工具指南

## 数据流概览

1. **数据库配置阶段**  
   - `envJson`、`frontPayloadJson`：存储环境变量与前端别名参数，均为 JSON 字符串。  
   - `aliasMappingJson`：描述别名 → 真实字段映射，例如 `{"a1":"k1"}`。  
   - `headerTemplateJson` / `paramTemplateJson`：存储 SpEL 模板，值里可使用 `#{#env[...]}` 等表达式。  
   - `contextVariableJson`：数组结构 `[{"varName":"env","source":"env"}]`，定义要注册到 SpEL 的变量名与上下文 key。  
   - `contextDataJson`：字典结构 `{"env":"env","payload":"payload"}`，从内置上下文中挑选需要暴露给 SpEL 的对象。  
   - `paramPlusJson`：可选字段，描述额外需要合并到最终参数中的 key/list/map 结构（如额外 `content`）。  
   - 可选 `urlTemplate` 字段，用于拼接最终请求地址。  
   > 对应字段及 JSON 的 key/value 语义见下方“数据库字段说明”章节，便于直接映射为表结构。

2. **业务调用阶段**  
   - 通过 `aiModelMpServiceImpl` 读取上述 JSON，构建 `SpelTemplateConfig`（支持 `@Accessors(chain = true)` 链式调用）。  
   - 使用 `SpelTemplateEngine#evaluate` 完成：JSON 解析 → 上下文重映射 → 变量注册 → SpEL 求值。  
   - `evaluate` 返回 `SpelEvaluationResult`，包含 `resolvedUrl`、`resolvedHeader`、`resolvedParam` 及运行期上下文，供 OKHttp 或其它客户端直接使用。

## 工具类使用步骤

1. **组装配置**
   ```java
   SpelTemplateConfig config = new SpelTemplateConfig()
           .setModelName("demo-model")
           .setUrlTemplate("#{#env['baseUrl']}/invoke/#{#env['traceId']}")
           .setEnvJson("{\"baseUrl\":\"https://example.com\",\"traceId\":\"t-1\"}")
           .setFrontPayloadJson("{\"a1\":\"hello\"}")
           .setAliasMappingJson("{\"a1\":\"k1\"}")
           .setHeaderTemplateJson("{\"Authorization\":\"Bearer #{#env['token']}\"}")
           .setParamTemplateJson("{\"k1\":\"#{#payload['k1']}\"}")
           .setContextVariableJson("[{\"varName\":\"env\",\"source\":\"env\"},{\"varName\":\"payload\",\"source\":\"payload\"}]")
           .setContextDataJson("{\"env\":\"env\",\"payload\":\"payload\"}")
           .addBuiltinContext("token", "demo-token")
           .addParamPlusEntry("watermark", false)
           .addParamPlusListItem("content", "{\"type\":\"image_url\",\"image_url\":{\"url\":\"https://example.com/demo.png\"}}");
   ```

2. **解析模板**
   ```java
   SpelTemplateEngine engine = new SpelTemplateEngine();
   SpelEvaluationResult result = engine.evaluate(config);
   log.info("URL={}, header={}, param={}", result.getResolvedUrl(), result.getResolvedHeader(), result.getResolvedParam());
   ```

3. **后续动作**  
   - 通过 `result.getResolvedHeader()`、`result.getResolvedParam()` 发起 HTTP 请求；  
   - 若需要调试，可读取 `result.getContextDataSource()` 查看最终注册到 SpEL 的上下文。

## 数据库字段说明

| 字段               | JSON 内部 key   | 含义 / 作用                                                                                             |
|--------------------|-----------------|---------------------------------------------------------------------------------------------------------|
| `env_json`         | `baseUrl`       | HTTP 基础地址，供 `urlTemplate` 和 Header 中引用。                                                       |
|                    | `traceId`       | 请求追踪号，可拼入 URL 或 Header。                                                                      |
|                    | `token`         | 认证 token，通常用于 `Authorization` 模板。                                                             |
| `front_payload`    | `a1`、`a2` 等   | 前端传入的别名参数（字符串、数值均可）。                                                                |
| `alias_mapping`    | `a1 -> k1` 等   | 说明别名 `a1` 的值应映射到真实字段 `k1`，供 payloadContext 重建。                                        |
| `header_template`  | `Authorization` | Header 名称；value 是 SpEL 表达式，如 `Bearer #{#env['token']}`。                                        |
|                    | `X-Trace-Id`    | 其它 Header；value 同样是 SpEL 表达式。                                                                 |
| `param_template`   | `k1`、`k2`      | 需要提交的真实参数键；value 为 SpEL 表达式，如 `#{#payload['k1']}` 或 `#{T(java.lang.System).currentTimeMillis()}`。 |
| `context_variable` | `varName`       | 给 SpEL 注册的变量名，例如 `env`。                                                                      |
|                    | `source`        | 来源上下文 key，例如 `payload`，需要与 `context_data` 或内置上下文匹配。                                |
| `context_data`     | `env`、`payload`| 指定在 EvaluationContext 中存放的对象来源，如果 value 为 `env` 表示使用 `env_json` 解析结果。             |
| `param_plus`       | `content` 等     | 追加配置，例如 `{"content":[{"type":"image_url","image_url":{"url":"..."}}]}`，会在模板求值后自动合并。         |
| `url_template`     | -               | 单个字符串，SpEL 模板形式的 URL，如 `#{#env['baseUrl']}/invoke/#{#task.modelName}`。                     |

## 数据库存储示例

| 字段               | 类型 | 示例                                                                                                                   |
|--------------------|------|------------------------------------------------------------------------------------------------------------------------|
| `env_json`         | JSON | `{"baseUrl":"https://mock-ai.service.com","traceId":"trace-1700","token":"demo-token"}`                                |
| `front_payload`    | JSON | `{"a1":"请以教师身份回答问题","a2":9527}`                                                                              |
| `alias_mapping`    | JSON | `{"a1":"k1","a2":"k2"}`                                                                                                |
| `header_template`  | JSON | `{"Authorization":"Bearer #{#env['token']}","X-Trace-Id":"#{#env['traceId']}"}`                                        |
| `param_template`   | JSON | `{"k1":"#{#payload['k1']}","k2":"#{T(java.lang.System).currentTimeMillis()}","userId":"#{#payload['k2']}"}`           |
| `context_variable` | JSON | `[{"varName":"env","source":"env"},{"varName":"payload","source":"payload"}]`                                           |
| `context_data`     | JSON | `{"env":"env","payload":"payload","task":"task"}`                                                                       |
| `param_plus`       | JSON | `{"content":[{"type":"image_url","image_url":{"url":"https://…/fox.png"}}]}`                                            |
| `url_template`     | 文本 | `#{#env['baseUrl']}/invoke/#{#task.modelName}?trace=#{#env['traceId']}&user=#{#payload['k2']}`                          |

> 备注：表字段命名可根据业务调整，核心是保证配置可以完整覆盖上下文、模板与变量映射。

这样即可在数据库中完全定义模板，业务层只需读取并交给 `SpelTemplateEngine`，实现“无侵入”的动态替换能力。
