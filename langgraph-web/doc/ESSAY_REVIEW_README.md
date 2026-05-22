# 作文批改系统 - Vue 前端

基于 Vue 3 + TypeScript 实现的作文批改流式输出界面。

## 📁 文件结构

```
langgraph-web/src/
├── types/
│   └── essayReview.ts          # 类型定义
├── api/
│   └── essayReview.ts          # API 调用（SSE 流式处理）
├── components/
│   └── EssayReviewChat.vue     # 作文批改主组件
└── App.vue                     # 应用入口（包含标签页切换）
```

## ✨ 功能特性

- 🎨 **美观界面**：现代化的聊天式布局
- ⚡ **流式输出**：实时显示教师评语生成过程
- 🔄 **工作流展示**：清晰展示三步工作流过程
- 🛑 **停止生成**：支持中途停止生成
- 📱 **响应式设计**：适配不同屏幕尺寸
- 🎯 **标签切换**：可在"市场简报"和"作文批改"之间切换

## 🚀 使用方法

### 1. 安装依赖

```bash
cd langgraph-web
npm install
```

### 2. 配置代理（开发环境）

确保 `vite.config.ts` 中配置了 API 代理：

```typescript
export default defineConfig({
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
```

### 3. 启动开发服务器

```bash
npm run dev
```

### 4. 访问应用

打开浏览器访问：http://localhost:5173

默认会显示"作文批改"标签页。

## 📊 界面说明

### 顶部标签栏
- 📊 **市场简报**：原有的市场简报生成功能
- 📝 **作文批改**：新增的作文批改功能

### 作文批改界面

#### 欢迎页面
首次进入时显示欢迎信息和工作流程说明：
1. 布置作文作业
2. 学生提交作文
3. 教师流式输出评语

#### 输入表单
- **学生姓名**：输入学生姓名（如：张三）
- **作文题目**：输入作文题目（如：生活中的美好）

#### 操作按钮
- **提交批改**：开始批改流程
- **停止生成**：中途中止生成（仅在生成时可用）

#### 消息展示区
- **用户消息**：右侧显示，蓝色背景
- **教师评语**：左侧显示，白色背景，带流式光标动画
- **提示信息**：居中显示，蓝色边框
- **错误信息**：全宽显示，红色边框

## 🔧 技术实现

### SSE 流式处理

复用现有的 SSE 处理工具：
- `utils/sseStream.ts`：SSE 帧解析
- `api/essayReview.ts`：流式数据读取和拼接

### 组件设计

```vue
<script setup lang="ts">
// 响应式状态
const studentName = ref('张三')
const essayTopic = ref('生活中的美好')
const messages = ref<EssayMessage[]>([])
const isGenerating = ref(false)

// 流式消息处理
await generateEssayReview(request, {
  signal: abortController.signal,
  onEvent: (data) => {
    assistant.content = data  // 实时更新消息内容
  },
})
</script>
```

### 类型定义

```typescript
export interface EssayReviewRequest {
  studentName: string
  essayTopic: string
}

export interface EssayMessage {
  id: string
  role: 'user' | 'assistant' | 'error' | 'info'
  content: string
  streaming?: boolean
}
```

## 🎨 样式变量

使用 CSS 变量保持与整体主题一致：

```css
--bg: 背景色
--panel: 面板色
--text: 文本色
--muted: 次要文本色
--border: 边框色
--accent: 强调色
--user: 用户消息背景
--assistant: 助手消息背景
--error: 错误色
```

## 📝 与后端的交互

### 请求

```typescript
POST /api/essay/review
Content-Type: application/json
Accept: text/event-stream

{
  "studentName": "张三",
  "essayTopic": "生活中的美好"
}
```

### 响应

```
Content-Type: text/event-stream

data: 这是评语的第一个字...
data: 这是评语的第二个字...
...
```

## 🔄 工作流程

```
用户提交表单
    ↓
发送 POST 请求
    ↓
后端执行工作流：
  1. AssignEssayNode（布置作业）
  2. SubmitEssayNode（提交作文）
  3. ReviewEssayNode（流式批改）⭐
    ↓
前端接收 SSE 流
    ↓
实时更新界面
    ↓
显示完整评语
```

## 🛠️ 开发指南

### 修改默认值

```typescript
// EssayReviewChat.vue
const studentName = ref('张三')      // 修改默认学生姓名
const essayTopic = ref('生活中的美好') // 修改默认作文题目
```

### 自定义样式

在 `<style scoped>` 中修改样式，或覆盖 CSS 变量。

### 添加新功能

1. 在 `types/essayReview.ts` 中添加类型
2. 在 `api/essayReview.ts` 中添加 API 方法
3. 在 `EssayReviewChat.vue` 中添加 UI

## 🐛 故障排除

### 无法连接后端

检查：
1. 后端是否启动（`http://localhost:8080`）
2. Vite 代理配置是否正确
3. 浏览器控制台是否有 CORS 错误

### 流式输出不工作

检查：
1. 后端是否返回 `Content-Type: text/event-stream`
2. 浏览器是否支持 SSE
3. 查看 Network 面板中的响应流

### 编译错误

确保：
1. 已运行 `npm install`
2. TypeScript 版本正确（~5.7.2）
3. 使用 `vue-tsc` 而不是 `tsc`

## 📦 构建生产版本

```bash
npm run build
```

构建产物在 `dist/` 目录。

## 🔗 相关文档

- [后端工作流文档](../../langgraph-api/ESSAY_REVIEW_README.md)
- [快速开始指南](../../langgraph-api/QUICKSTART_ESSAY_REVIEW.md)
- [Vue 3 文档](https://vuejs.org/)
- [TypeScript 文档](https://www.typescriptlang.org/)
