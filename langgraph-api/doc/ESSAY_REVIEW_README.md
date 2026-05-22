# 教师作文批改工作流

基于 LangGraph4j 实现的教师布置作文、学生提交、教师批改的完整工作流，支持流式输出评语。

## 功能特点

✨ **完整的工作流程**
- 教师布置作文作业
- 学生提交作文
- 教师批改并流式输出评语

🚀 **流式输出**
- 最后一个节点（教师批改）使用流式输出
- 实时显示评语生成过程
- 提升用户体验

📊 **状态管理**
- 使用 `EssayReviewState` 管理工作流状态
- 清晰的字段定义和访问方法

## 文件结构

```
langgraph-api/src/main/java/com/moli/langgraph/
├── state/
│   └── EssayReviewState.java          # 工作流状态定义
├── nodes/
│   └── essay/
│       ├── AssignEssayNode.java       # 布置作文节点
│       ├── SubmitEssayNode.java       # 提交作文节点
│       └── ReviewEssayNode.java       # 批改作文节点（流式输出）
├── workflow/
│   ├── EssayReviewApp.java            # 工作流应用
│   └── EssayReviewWorkflowTest.java   # 测试类
└── controller/
    └── EssayReviewController.java     # REST API 控制器
```

## 工作流设计

```
START → assign_essay → submit_essay → review_essay (流式) → END
```

### 节点说明

1. **AssignEssayNode（布置作文）**
   - 根据题目生成作文要求
   - 提供写作建议

2. **SubmitEssayNode（提交作文）**
   - 模拟学生提交作文
   - 实际应用中可从数据库获取

3. **ReviewEssayNode（批改作文）⭐**
   - 使用 `StreamingChatModel` 流式生成评语
   - 使用 `StreamingChatGenerator` 包装
   - 通过 `_streaming_messages` 键返回流式数据
   - 自动提取评分等级

## 使用方法

### 1. 启动应用

```bash
cd langgraph-api
mvn spring-boot:run
```

### 2. API 接口

#### GET 方式
```bash
curl -N http://localhost:8080/api/essay/review?studentName=张三&essayTopic=生活中的美好
```

#### POST 方式
```bash
curl -X POST http://localhost:8080/api/essay/review \
  -H "Content-Type: application/json" \
  -d '{
    "studentName": "张三",
    "essayTopic": "生活中的美好"
  }'
```

### 3. 浏览器测试

访问：`http://localhost:8080/essay-review.html`

填写学生姓名和作文题目，点击提交即可看到流式输出的评语。

## 核心技术实现

### 流式输出实现（真正的打字机效果）

**关键：LLM 返回一个 token/chunk 就立即推送到前端，不在后端缓存**

```java
// ReviewEssayNode.java - 节点内使用 StreamingChatGenerator
var generator = StreamingChatGenerator.<EssayReviewState>builder()
        .mapResult(response -> {
            String fullComment = response.aiMessage().text();
            String grade = extractGrade(fullComment);
            return Map.of(
                    EssayReviewState.TEACHER_COMMENT_KEY, fullComment,
                    EssayReviewState.ESSAY_GRADE_KEY, grade
            );
        })
        .startingNode(REVIEW_ESSAY)
        .startingState(state)
        .build();

streamingChatModel.chat(chatRequest, generator.handler());

// 关键：使用 _streaming_messages 键
return LangGraphStreaming.streamingMessages(generator);
```

```java
// EssayReviewApp.java - 立即推送 chunk，不缓存
AsyncGenerator<NodeOutput<EssayReviewState>> stream = graph.stream(initState);

for (NodeOutput<EssayReviewState> output : stream) {
    if (output instanceof StreamingOutput<EssayReviewState> streamingOutput
            && ReviewEssayNode.REVIEW_ESSAY.equals(streamingOutput.node())) {
        String chunk = streamingOutput.chunk();
        if (chunk != null && !chunk.isEmpty()) {
            sink.next(chunk); // LLM 返回一个就立即推送一个
        }
    }
}
```

### ⚠️ 常见错误

**错误 1**：在图外调用 LLM（MarketReportApp 的方式）
- 破坏了 LangGraph 的工作流完整性
- 违背了自包含工作流的设计初衷

**错误 2**：使用 graph.stream() 但缓存 chunk（MarketReportAppV1 的方式）
- 后端缓存所有 chunk，最后 1 毫秒内一次性刷出
- 前端无法实现真正的打字机效果

**正确做法**：参考 [STREAMING_BEST_PRACTICES.md](./STREAMING_BEST_PRACTICES.md)

## 状态字段

| 字段 | 说明 |
|------|------|
| studentName | 学生姓名 |
| essayTopic | 作文题目 |
| essayContent | 作文内容 |
| teacherComment | 教师评语 |
| essayGrade | 作文等级 |

## 自定义扩展

### 修改评语风格

编辑 `ReviewEssayNode.java` 中的 `SYSTEM_PROMPT`：

```java
private static final String SYSTEM_PROMPT = """
        你是一位...（自定义你的教师人设）
        
        评语要求：
        1. ...（自定义要求）
        """;
```

### 修改作文示例

编辑 `SubmitEssayNode.java` 生成不同的示例作文。

### 添加更多节点

可以在工作流中添加更多节点，如：
- 作文查重
- 语法检查
- 自动打分

## 注意事项

1. 确保配置了有效的 `StreamingChatModel`（如 OpenAI、通义千问等）
2. 流式输出需要客户端支持 SSE（Server-Sent Events）
3. 实际应用中，作文内容应从数据库或前端获取

## 参考

- [LangGraph4j 文档](https://github.com/bsorrentino/langgraph4j)
- [LangGraphStreaming.java](../graph/LangGraphStreaming.java) - 流式输出工具类
- [MarketReportApp.java](./MarketReportApp.java) - 类似的工作流实现
