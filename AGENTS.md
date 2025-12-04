# Repository Guidelines

## 项目结构与模块组织
主服务位于 `src/main/java/com/origin/aimodel`，按职责拆分 `controller`（REST 接口）、`service`/`service/executor`（任务编排与执行器）、`dao` 与 `dao/mapper`（MyBatis-Plus 映射）、`domain`（VO/MP 实体）以及 `util`（SpEL、JSON 工具）。配置与 SQL 映射存放在 `src/main/resources`，其中 `application.yml` 提供线程池、数据源等参数，`mapperxml` 放置自定义查询。集成与产品文档位于 `docs`，示例与脚本放在 `k1` 与 `HELP.md`。测试代码集中在 `src/test/java`，默认入口是 `AiModelCallApplicationTests`。

## 构建、测试与开发命令
- `./mvnw clean verify`：清理输出并执行编译、单测与打包。
- `./mvnw test`：仅运行测试套件，加速本地验证。
- `./mvnw spring-boot:run`：使用配置文件启动服务，便于调试控制器或执行器。
- `./mvnw dependency:tree`：排查依赖冲突，尤其在升级 MyBatis-Plus、Fastjson2 时使用。

## 代码风格与命名约定
Java 代码保持 4 空格缩进，遵循 Spring 与 MyBatis-Plus 默认风格；公共 DTO/VO 使用 `PascalCase` 命名，内部变量与 Bean 字段使用 `camelCase`。包路径沿业务语义划分，避免创建无意义的 util 合集。使用 Lombok 的类需显式 import 注解，并优先通过 `@Builder` 表达复杂入参。JSON 模板、SpEL 表达式置于配置中心或 `docs` 示例中，命名规则为 `场景-模型-版本`。

## 测试指南
测试框架采用 Spring Boot Test + JUnit 5，文件命名遵循 `*Tests.java`。`service/executor` 的关键分支应提供限流、重试、SpEL 渲染等用例，可通过 Mock RocketMQ 客户端与 OkHttp 调用来隔离外部依赖。建议在提交前运行 `./mvnw test` 并查看 `target/surefire-reports`，确保 0 失败。对复杂 SpEL 模板，优先编写参数对照表并在测试中覆盖异常路径。

## 提交与 PR 规范
遵循 Conventional Commits，例如 `feat(spelplus): 新增模板缓存` 或 `fix(executor): 调整限流日志`，保持中文动宾结构描述。PR 需说明变更意图、受影响模块、验证方式，并在涉及接口或配置变更时附带 `docs/` 更新与接口示例；如果修改线程池、队列或数据库脚本，请附上回滚方案。截图仅在前端或可视化监控变更时要求，后端逻辑以日志或测试输出说明。

## 安全与配置提示
`application.yml` 中仅应包含本地占位符，生产密钥通过环境变量或配置中心注入；提交前检查 `.gitignore`，避免上传真实凭证。修改外部模型或 RocketMQ 配置时，请同步更新 `docs` 说明并在 PR 中标记需要重新发布的环境。任何涉及 Token 配额或租户隔离的变更，都应评估并记录潜在风险。
