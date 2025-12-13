# AI Model Reverse Parse Frontend

Vue3 前端项目，用于 AI 模型反向解析配置生成工具。

## 功能特性

- 📝 输入原始请求 JSON（Body 和 Headers）
- 🔧 配置字段映射关系
- ✅ 配置字段校验规则（maxLength、range、enum）
- 📊 实时生成 paramItem 和 headerItem 配置
- 🎨 响应式设计，支持移动端

## 技术栈

- Vue 3 (Composition API)
- Vite
- Axios

## 安装依赖

```bash
pnpm install
```

## 开发运行

```bash
pnpm dev
```

访问 http://localhost:3000

## 构建生产版本

```bash
pnpm build
```

## 项目结构

```
ai-vue/
├── src/
│   ├── components/          # 组件目录
│   │   ├── RequestInput.vue      # 请求输入组件
│   │   ├── MappingConfig.vue    # 映射配置组件
│   │   └── ResultDisplay.vue   # 结果显示组件
│   ├── api/                # API 接口
│   │   └── reverseParse.js
│   ├── App.vue            # 主组件
│   ├── main.js            # 入口文件
│   └── style.css          # 全局样式
├── index.html
├── vite.config.js
└── package.json
```

## 使用说明

1. **输入原始请求数据**
   - 在左侧输入框中填写 Request Body（JSON 格式）
   - 可选填写 Request Headers（JSON 格式）

2. **配置字段映射**
   - 在右侧添加字段映射关系
   - 每个映射包含：
     - `key`: 业务字段名
     - `postParam`: 目标字段名
     - `validate`: 校验规则（可选）

3. **生成配置**
   - 点击"生成配置"按钮
   - 系统会自动从请求数据中推断 node 路径
   - 生成 paramItem 和 headerItem 配置

4. **查看结果**
   - 在页面下方查看生成的配置项
   - 每个配置项以 JSON 格式显示

## 校验规则示例

- **maxLength**: `{"maxLength":2000}` - 最大长度
- **range**: `{"range":[0.0,2.0]}` - 数值范围
- **enum**: `{"enum":["text","image_url"]}` - 枚举值

## 后端 API

默认代理到 `http://localhost:40000/api`，可在 `vite.config.js` 中修改。

