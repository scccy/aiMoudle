# SpEL Plus 升级计划（讨论稿）

## 已知请求形态（按增强类型归纳）
- **基础**：单段 text，固定模型
  ```json
  {"model":"doubao-seedance-1-0-pro-250528","content":[{"type":"text","text":"… --ratio 16:9"}]}
  ```
- **增强1**：切换模型 + text 参数增强 + 追加 image_url（网络图）
  ```json
  {"model":"doubao-seedance-1-0-pro-fast-251015","content":[{"type":"text","text":"… --ratio adaptive --dur 5"},{"type":"image_url","image_url":{"url":"https://…/i2v_foxrgirl.png"}}]}
  ```
- **增强2**：原模型 + text + 首/尾帧占位（image_url 带 role: first_frame/last_frame）
  ```json
  {"model":"doubao-seedance-1-0-pro-250528","content":[{"type":"text","text":"360度环绕运镜"},{"type":"image_url","image_url":{"url":"https://…/seepro_first_frame.jpeg"},"role":"first_frame"},{"type":"image_url","image_url":{"url":"https://…/seepro_last_frame.jpeg"},"role":"last_frame"}]}
  ```
- **增强3**：切换 lite 模型 + text + base64 image_url
  ```json
  {"model":"doubao-seedance-1-0-lite-i2v-250428","content":[{"type":"text","text":"… --ratio adaptive --dur 5"},{"type":"image_url","image_url":{"url":"data:image/png;base64,aHR0******cG5n"}}]}
  ```
- **增强4**：切换 lite 模型 + text + callback_url
  ```json
  {"model":"doubao-seedance-1-0-lite-t2v-250428","content":[{"type":"text","text":"… --ratio 16:9"}],"callback_url":"https://****"}
  ```


## 背景
- 当前 `loadModelTemplateConfig` 以字符串存储模板（env/header/aliasMapping/param），SpEL 引擎解析后得到请求参数。
- 需求：在不破坏原有模板的前提下，按“增量”方式覆盖/追加模型名与 content 等字段，例如：
  - base param: `{"model":"doubao-seedance-1-0-pro-250528","content":[{"type":"text","text":"… --ratio 16:9"}]}`
  - plus param: `{"model":"doubao-seedance-1-0-pro-fast-251015","content":[{"type":"text","text":"… --ratio adaptive --dur 5"},{"type":"image_url","image_url":{"url":"https://…/i2v_foxrgirl.png"}}]}`



## 增强思路
- 在 `SpelTemplateConfig` 新增增量字段（字符串形态），默认空：
  - `aliasMappingJsonPlus`：追加/覆盖别名→真实字段映射。
  - `paramTemplateJsonPlus`：追加/覆盖 param SpEL 模板。
- 引擎侧先解析 base，再用 jsonPlus 做合并：
  1) 解析 base alias/param 为 Map
  2) 若存在 plus，则 `merge(base, plus, AUTO/OVERWRITE)` 得到最终模板 Map
  3) 继续 SpEL evaluate，产出 header/param
- 可选：输出后再合并一次 plus patch（如需要静态内容追加），同样通过 jsonPlus。

### 差异字段与合并策略（对应已知形态）
- `model`：可被 plus 覆盖（增强1/3/4 切换模型）。
- `content`：数组，plus 需支持追加/覆盖：
  - text 追加或替换（增强1/3/4）。
  - `image_url` 追加（网络/首尾帧/base64），`role` 可选（增强2）。
  - 数组合并默认追加；若需全量替换可用策略 `OVERWRITE`。
- 其他顶层字段：`callback_url`（增强4）按覆盖策略写入。
- alias 映射：为新增字段补充别名，如 `textPlus`、`imageUrl`、`imageUrlFirst`、`imageUrlLast`、`callbackUrl` 等，plus 合并到 base。

## API 预期
- `SpelTemplateConfig` 增加：
  - `setAliasMappingJsonPlus(String json)` / `addAliasMappingPlus(String alias, String realKey)`
  - `setParamTemplateJsonPlus(String json)` / `addParamTemplatePlus(String key, String spelExpr)`
- `SpelTemplateEngine` 调用处改用 `JsonPlusTemplateCodec.decodeTemplate` + `JsonPlusMerger.merge` 完成 base+plus 合并。

## 示例片段（字符串配置）
- `aliasMappingJsonPlus`：
  ```json
  {"a11":"textPlus","a12":"imageUrl","a13":"model_plus"}
  ```
- `paramTemplateJsonPlus`：
  ```json
  {
    "model":"#{#env['model_plus'] ?: 'doubao-seedance-1-0-pro-fast-251015'}",
    "content":"#{T(com.alibaba.fastjson2.JSON).parseArray('[{\"type\":\"text\",\"text\":\"' + #payload['textPlus'] + ' --ratio adaptive --dur 5\"},{\"type\":\"image_url\",\"image_url\":{\"url\":\"' + #payload['imageUrl'] + '\"}}]')}"
  }
  ```
- base `paramTemplateJson` 保持原有 seedance 逻辑；合并后模型与 content 以 plus 为主。

## 预期输出示例 & 所需模板片段
### 增强1（模型切换 + 文本强化 + image_url 追加）
- **输出 param（期望）**
  ```json
  {
    "model": "doubao-seedance-1-0-pro-fast-251015",
    "content": [
      {
        "type": "text",
        "text": "女孩抱着狐狸… --ratio adaptive --dur 5"
      },
      {
        "type": "image_url",
        "image_url": {"url": "https://ark-project.tos-cn-beijing.volces.com/doc_image/i2v_foxrgirl.png"}
      }
    ]
  }
  ```
- **需要追加的 alias 映射（jsonPlus 片段）**
  ```json
  {"a11":"textPlus","a12":"imageUrl","a13":"model_plus"}
  ```
- **paramTemplateJsonPlus（字符串）**
  ```json
  {
    "model":"#{#env['model_plus'] ?: 'doubao-seedance-1-0-pro-fast-251015'}",
    "content":"#{T(com.alibaba.fastjson2.JSON).parseArray('[{\"type\":\"text\",\"text\":\"' + #payload['textPlus'] + ' --ratio adaptive --dur 5\"},{\"type\":\"image_url\",\"image_url\":{\"url\":\"' + #payload['imageUrl'] + '\"}}]')}"
  }
  ```

### 增强2（首/尾帧占位）
- **输出 param（期望）**
  ```json
  {
    "model": "doubao-seedance-1-0-pro-250528",
    "content": [
      {"type":"text","text":"360度环绕运镜"},
      {"type":"image_url","image_url":{"url":"https://…/seepro_first_frame.jpeg"},"role":"first_frame"},
      {"type":"image_url","image_url":{"url":"https://…/seepro_last_frame.jpeg"},"role":"last_frame"}
    ]
  }
  ```
- **aliasMappingJsonPlus**
  ```json
  {"a11":"textPlus","a12":"imageUrlFirst","a13":"imageUrlLast"}
  ```
- **paramTemplateJsonPlus**
  ```json
  {
    "content":"#{T(com.alibaba.fastjson2.JSON).parseArray('[{\"type\":\"text\",\"text\":\"' + #payload['textPlus'] + '\"},{\"type\":\"image_url\",\"image_url\":{\"url\":\"' + #payload['imageUrlFirst'] + '\"},\"role\":\"first_frame\"},{\"type\":\"image_url\",\"image_url\":{\"url\":\"' + #payload['imageUrlLast'] + '\"},\"role\":\"last_frame\"}]')}"
  }
  ```

### 增强3（base64 image_url + 模型切换）
- **输出 param（期望）**
  ```json
  {
    "model": "doubao-seedance-1-0-lite-i2v-250428",
    "content": [
      {"type":"text","text":"… --ratio adaptive --dur 5"},
      {"type":"image_url","image_url":{"url":"data:image/png;base64,aHR0******cG5n"}}
    ]
  }
  ```
- **aliasMappingJsonPlus**
  ```json
  {"a11":"textPlus","a12":"imageUrlBase64","a13":"model_plus"}
  ```
- **paramTemplateJsonPlus**
  ```json
  {
    "model":"#{#env['model_plus'] ?: 'doubao-seedance-1-0-lite-i2v-250428'}",
    "content":"#{T(com.alibaba.fastjson2.JSON).parseArray('[{\"type\":\"text\",\"text\":\"' + #payload['textPlus'] + ' --ratio adaptive --dur 5\"},{\"type\":\"image_url\",\"image_url\":{\"url\":\"' + #payload['imageUrlBase64'] + '\"}}]')}"
  }
  ```

### 增强4（callback_url 追加 + 模型切换）
- **输出 param（期望）**
  ```json
  {
    "model": "doubao-seedance-1-0-lite-t2v-250428",
    "content": [
      {"type":"text","text":"写实风格… --ratio 16:9"}
    ],
    "callback_url": "https://****"
  }
  ```
- **aliasMappingJsonPlus**
  ```json
  {"a11":"textPlus","a12":"callbackUrl","a13":"model_plus"}
  ```
- **paramTemplateJsonPlus**
  ```json
  {
    "model":"#{#env['model_plus'] ?: 'doubao-seedance-1-0-lite-t2v-250428'}",
    "content":"#{T(com.alibaba.fastjson2.JSON).parseArray('[{\"type\":\"text\",\"text\":\"' + #payload['textPlus'] + ' --ratio 16:9\"}]')}",
    "callback_url":"#{#payload['callbackUrl']}"
  }
  ```
## 兼容性与策略
- plus 为空时行为不变；有值时按策略（默认 AUTO）合并。
- alias/param 合并顺序：先 base，再 plus；数组采用追加，标量采用覆盖（可扩展策略）。
- 保持字符串存储/解析，适配现有数据库/配置形态。

## 测试要点
- 单元测试覆盖：仅 base；base+plus 覆盖 model；base+plus 追加 image_url；aliasMapping 合并新键；非法转义仍能被 JsonPlusTemplateCodec 容错。
