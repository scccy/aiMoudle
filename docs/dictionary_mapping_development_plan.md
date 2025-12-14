# 字典映射功能开发计划

## 一、需求概述

创建一个字典映射表，用于存储 `key` 与 `post_param` 的对应关系，作为字典映射表。支持在侧边栏的字典查询 tab 中查询和展示映射关系，以及关联的 item 设置详情。

## 二、数据库设计

### 2.1 新建表：字典映射表

**表名：** `dim_dictionary_mapping`

**表说明：** 字典映射表，存储 key 与 post_param 的对应关系

**字段设计：**

| 字段名 | 类型 | 长度 | 是否必填 | 说明 | 备注 |
|--------|------|------|----------|------|------|
| `key` | VARCHAR | 255 | 是 | 业务入参的 key 名称 | 联合主键之一 |
| `post_param` | VARCHAR | 255 | 是 | 写入的目标字段名 | 联合主键之一 |
| `description` | VARCHAR | 500 | 否 | 中文说明 | 用于描述该映射关系的用途 |
| `item_id` | BIGINT | - | 否 | 关联的 item ID | 关联 `dim_ai_model_item.id` |
| `created_by` | VARCHAR | 100 | 否 | 创建人 | 自动填充 |
| `created_time` | DATETIME | - | 否 | 创建时间 | 自动填充 |
| `updated_by` | VARCHAR | 100 | 否 | 更新人 | 自动填充 |
| `updated_time` | DATETIME | - | 否 | 更新时间 | 自动填充 |
| `del_flag` | TINYINT | - | 否 | 删除标志 | 0-未删除，1-已删除，逻辑删除 |

**索引设计：**

- 主键：`PRIMARY KEY (key, post_param)` - 联合主键
- 普通索引：`INDEX idx_item_id (item_id)` - 用于关联查询
- 普通索引：`INDEX idx_key (key)` - 用于按 key 查询
- 普通索引：`INDEX idx_post_param (post_param)` - 用于按 post_param 查询

**建表 SQL：**

```sql
CREATE TABLE dim_dictionary_mapping (
    key VARCHAR(255) NOT NULL COMMENT '业务入参的 key 名称',
    post_param VARCHAR(255) NOT NULL COMMENT '写入的目标字段名',
    description VARCHAR(500) COMMENT '中文说明',
    item_id BIGINT COMMENT '关联的 item ID，关联 dim_ai_model_item.id',
    created_by VARCHAR(100) COMMENT '创建人',
    created_time DATETIME COMMENT '创建时间',
    updated_by VARCHAR(100) COMMENT '更新人',
    updated_time DATETIME COMMENT '更新时间',
    del_flag TINYINT DEFAULT 0 COMMENT '删除标志：0-未删除，1-已删除',
    PRIMARY KEY (key, post_param),
    INDEX idx_item_id (item_id),
    INDEX idx_key (key),
    INDEX idx_post_param (post_param)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典映射表';
```

## 三、代码结构设计

### 3.1 实体类（Entity）

**文件路径：** `src/main/java/com/origin/aimodel/domain/mp/DictionaryMappingMp.java`

**说明：** 
- 继承 `Model<DictionaryMappingMp>`（MyBatis-Plus ActiveRecord 模式）
- 实现 `Serializable`
- 使用 `@TableName("dim_dictionary_mapping")` 指定表名
- 使用 `@TableId` 标注联合主键（需要自定义主键策略）
- 字段使用 `@TableField` 标注，支持自动填充

**主要字段：**
- `key` (String) - 联合主键之一
- `postParam` (String) - 联合主键之一
- `description` (String) - 中文说明
- `itemId` (Long) - 关联的 item ID
- 继承公共字段（createdBy, createdTime, updatedBy, updatedTime, delFlag）

### 3.2 Mapper 接口

**文件路径：** `src/main/java/com/origin/aimodel/dao/mapper/DictionaryMappingMapper.java`

**说明：**
- 继承 `BaseMapper<DictionaryMappingMp>`（MyBatis-Plus）
- 可定义自定义查询方法（如多条件查询）

**主要方法：**
- 基础 CRUD 方法（由 MyBatis-Plus 提供）
- 自定义查询方法（如按 key、postParam、description、itemId 查询）

### 3.3 Mapper XML（可选）

**文件路径：** `src/main/resources/mapperxml/DictionaryMappingMapper.xml`

**说明：**
- 如果使用自定义 SQL，需要创建此文件
- 否则可以仅使用 MyBatis-Plus 的注解方式

### 3.4 MyBatis-Plus Service 接口

**文件路径：** `src/main/java/com/origin/aimodel/dao/service/DictionaryMappingMpService.java`

**说明：**
- 继承 `IService<DictionaryMappingMp>`（MyBatis-Plus）
- 提供基础的 CRUD 服务方法

### 3.5 MyBatis-Plus Service 实现类

**文件路径：** `src/main/java/com/origin/aimodel/dao/service/impl/DictionaryMappingMpServiceImpl.java`

**说明：**
- 继承 `ServiceImpl<DictionaryMappingMapper, DictionaryMappingMp>`（MyBatis-Plus）
- 实现 `DictionaryMappingMpService` 接口

### 3.6 业务 Service 接口

**文件路径：** `src/main/java/com/origin/aimodel/service/DictionaryService.java`

**说明：**
- 业务层接口，定义业务逻辑方法
- 不直接依赖 MyBatis-Plus，封装业务逻辑

**主要方法：**
- `searchDictionary(DictionarySearchRequest request)` - 查询字典映射关系
- `getItemDetail(Long itemId)` - 获取 Item 详情
- `createDictionary(DictionaryCreateRequest request)` - 创建字典映射
- `updateDictionary(DictionaryUpdateRequest request)` - 更新字典映射
- `deleteDictionary(String key, String postParam)` - 删除字典映射

### 3.7 业务 Service 实现类

**文件路径：** `src/main/java/com/origin/aimodel/service/impl/DictionaryServiceImpl.java`

**说明：**
- 实现 `DictionaryService` 接口
- 注入 `DictionaryMappingMpService` 和 `DimAiModelItemMpService`
- 实现业务逻辑，包括：
  - 查询字典映射关系（支持多条件组合查询）
  - 获取关联的 item 详情
  - 创建/更新/删除字典映射

### 3.8 Controller

**文件路径：** `src/main/java/com/origin/aimodel/controller/DictionaryController.java`

**说明：**
- RESTful API 控制器
- 使用 `@RestController` 和 `@RequestMapping("/api/dictionary")`
- 注入 `DictionaryService`

**主要接口：**

1. **查询字典映射**
   - 路径：`GET /api/dictionary/search`
   - 参数：`DictionarySearchRequest`（Query 参数）
   - 返回：`ResultData<DictionarySearchResponse>`

2. **获取 Item 详情**
   - 路径：`GET /api/dictionary/item/{itemId}`
   - 参数：`itemId`（Path 变量）
   - 返回：`ResultData<DictionaryItemDetailResponse>`

3. **创建字典映射**
   - 路径：`POST /api/dictionary/create`
   - 参数：`DictionaryCreateRequest`（RequestBody）
   - 返回：`ResultData<String>`

4. **更新字典映射**
   - 路径：`PUT /api/dictionary/update`
   - 参数：`DictionaryUpdateRequest`（RequestBody）
   - 返回：`ResultData<String>`

5. **删除字典映射**
   - 路径：`DELETE /api/dictionary/delete`
   - 参数：`key` 和 `postParam`（Query 参数）
   - 返回：`ResultData<String>`

### 3.9 VO 类（Value Object）

**文件路径：** `src/main/java/com/origin/aimodel/domain/vo/`

**需要创建的 VO 类：**

1. **DictionarySearchRequest.java**
   - 查询请求参数
   - 字段：`key` (String, 可选)、`postParam` (String, 可选)、`description` (String, 可选)、`itemId` (Long, 可选)

2. **DictionarySearchResponse.java**
   - 查询响应结果
   - 字段：`mappingList` (List<DictionaryMappingVO>)

3. **DictionaryMappingVO.java**
   - 字典映射 VO
   - 字段：`key` (String)、`postParam` (String)、`description` (String)、`itemId` (Long)

4. **DictionaryItemDetailResponse.java**
   - Item 详情响应
   - 字段：`id` (Long)、`key` (String)、`category` (String)、`node` (String)、`postParam` (String)、`valueObject` (String)、`defaultValue` (String)、`spelTemp` (String)、`validate` (String) 等

5. **DictionaryCreateRequest.java**
   - 创建请求参数
   - 字段：`key` (String, 必填)、`postParam` (String, 必填)、`description` (String, 可选)、`itemId` (Long, 可选)

6. **DictionaryUpdateRequest.java**
   - 更新请求参数
   - 字段：`key` (String, 必填)、`postParam` (String, 必填)、`description` (String, 可选)、`itemId` (Long, 可选)

## 四、开发步骤

### 4.1 数据库层

1. ✅ 创建数据库迁移脚本（SQL）
2. ⏳ 执行 SQL 创建表
3. ⏳ 使用 MyBatis-Plus 代码生成器生成实体类、Mapper、Service（或手动创建）

### 4.2 实体层

1. ⏳ 创建 `DictionaryMappingMp` 实体类
2. ⏳ 配置联合主键（需要自定义主键策略）

### 4.3 数据访问层

1. ⏳ 创建 `DictionaryMappingMapper` 接口
2. ⏳ 创建 `DictionaryMappingMapper.xml`（如果需要自定义 SQL）
3. ⏳ 创建 `DictionaryMappingMpService` 接口
4. ⏳ 创建 `DictionaryMappingMpServiceImpl` 实现类

### 4.4 业务层

1. ⏳ 创建 VO 类（Request/Response）
2. ⏳ 创建 `DictionaryService` 接口
3. ⏳ 创建 `DictionaryServiceImpl` 实现类
4. ⏳ 实现查询逻辑（多条件组合查询）
5. ⏳ 实现获取 Item 详情逻辑（关联查询 `dim_ai_model_item` 表）

### 4.5 控制层

1. ⏳ 创建 `DictionaryController`
2. ⏳ 实现所有 RESTful API 接口
3. ⏳ 添加异常处理和日志记录

### 4.6 测试

1. ⏳ 单元测试（Service 层）
2. ⏳ 接口测试（Controller 层）
3. ⏳ 前端联调测试

## 五、注意事项

### 5.1 联合主键处理

- MyBatis-Plus 默认不支持联合主键，需要自定义主键策略
- 可以使用 `@TableId(type = IdType.INPUT)` 并手动设置主键值
- 或者在 Mapper XML 中自定义主键处理逻辑

### 5.2 关联查询

- 获取 Item 详情时，需要关联查询 `dim_ai_model_item` 表
- 可以使用 `DimAiModelItemMpService` 的 `getById` 方法
- 或者使用 JOIN 查询（在 Mapper XML 中）

### 5.3 逻辑删除

- 使用 MyBatis-Plus 的逻辑删除功能
- 查询时自动过滤已删除的记录（`del_flag = 1`）

### 5.4 自动填充

- 使用 `MyBatisPlusConfig.MetaObjectHandlerImpl` 自动填充创建时间、更新时间等字段

### 5.5 查询优化

- 多条件查询时，使用 `QueryWrapper` 动态构建查询条件
- 为空的条件不加入查询，避免全表扫描

## 六、API 接口详细设计

### 6.1 查询字典映射

**接口：** `GET /api/dictionary/search`

**请求参数（Query）：**
```
key: string (可选)
postParam: string (可选)
description: string (可选)
itemId: number (可选)
```

**响应示例：**
```json
{
  "code": 200,
  "message": "SUCCESS",
  "data": {
    "mappingList": [
      {
        "key": "content",
        "postParam": "content",
        "description": "内容字段映射",
        "itemId": 123
      }
    ]
  }
}
```

### 6.2 获取 Item 详情

**接口：** `GET /api/dictionary/item/{itemId}`

**路径参数：**
- `itemId`: number (必填)

**响应示例：**
```json
{
  "code": 200,
  "message": "SUCCESS",
  "data": {
    "id": 123,
    "key": "content",
    "category": "key",
    "node": "content",
    "postParam": "content",
    "valueObject": "string",
    "defaultValue": "",
    "spelTemp": "",
    "validate": ""
  }
}
```

### 6.3 创建字典映射

**接口：** `POST /api/dictionary/create`

**请求体：**
```json
{
  "key": "content",
  "postParam": "content",
  "description": "内容字段映射",
  "itemId": 123
}
```

**响应示例：**
```json
{
  "code": 200,
  "message": "SUCCESS",
  "data": "创建成功"
}
```

### 6.4 更新字典映射

**接口：** `PUT /api/dictionary/update`

**请求体：**
```json
{
  "key": "content",
  "postParam": "content",
  "description": "内容字段映射（已更新）",
  "itemId": 124
}
```

**响应示例：**
```json
{
  "code": 200,
  "message": "SUCCESS",
  "data": "更新成功"
}
```

### 6.5 删除字典映射

**接口：** `DELETE /api/dictionary/delete?key=content&postParam=content`

**请求参数（Query）：**
- `key`: string (必填)
- `postParam`: string (必填)

**响应示例：**
```json
{
  "code": 200,
  "message": "SUCCESS",
  "data": "删除成功"
}
```

## 七、前端对接

前端已实现（`DictionaryQuery.vue`），需要对接的接口：

1. ✅ `GET /api/dictionary/search` - 查询字典映射
2. ✅ `GET /api/dictionary/item/{itemId}` - 获取 Item 详情

前端 API 文件：`ai-vue/src/api/dictionary.js`

## 八、后续扩展（可选）

1. 批量导入字典映射
2. 字典映射的导入/导出功能
3. 字典映射的统计功能
4. 字典映射的版本管理

