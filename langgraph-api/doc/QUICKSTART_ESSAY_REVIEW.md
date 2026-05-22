# 作文批改工作流 - 快速开始

## 📋 概述

这是一个基于 LangGraph4j 实现的教师作文批改工作流，包含以下功能：
- ✅ 教师布置作文作业
- ✅ 学生提交作文
- ✅ 教师批改并**流式输出**评语

## 🚀 快速启动

### 1. 确保配置了 AI 模型

在 `application-local.yml` 中配置你的 StreamingChatModel（如 OpenAI、通义千问等）。

### 2. 启动应用

```bash
cd langgraph-api
mvn spring-boot:run
```

### 3. 测试流式输出

#### 方式一：浏览器测试（推荐）

访问：http://localhost:8080/essay-review.html

1. 输入学生姓名（如：张三）
2. 输入作文题目（如：生活中的美好）
3. 点击"提交批改请求"
4. 观察评语实时流式输出 ✨

#### 方式二：curl 测试

```bash
curl -N "http://localhost:8080/api/essay/review?studentName=张三&essayTopic=生活中的美好"
```

#### 方式三：POST 请求

```bash
curl -X POST http://localhost:8080/api/essay/review \
  -H "Content-Type: application/json" \
  -d '{
    "studentName": "张三",
    "essayTopic": "生活中的美好"
  }'
```

## 📊 工作流程

```
┌─────────┐
│  START  │
└────┬────┘
     │
     ▼
┌─────────────────┐
│  assign_essay   │  教师布置作文作业
│  (AssignEssay)  │  生成作业要求和写作建议
└────┬────────────┘
     │
     ▼
┌─────────────────┐
│  submit_essay   │  学生提交作文
│  (SubmitEssay)  │  模拟学生完成作文
└────┬────────────┘
     │
     ▼
┌─────────────────┐
│  review_essay   │  教师批改作文 ⭐
│  (ReviewEssay)  │  流式输出评语
└────┬────────────┘
     │
     ▼
┌─────────┐
│   END   │
└─────────┘
```

## 🔧 核心代码

### 流式输出节点

```java
// ReviewEssayNode.java
public class ReviewEssayNode implements NodeAction<EssayReviewState> {
    
    private final StreamingChatModel streamingChatModel;
    
    @Override
    public Map<String, Object> apply(EssayReviewState state) {
        // 构建批改请求
        ChatRequest chatRequest = buildReviewRequest(state);
        
        // 创建流式生成器
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
        
        // 调用流式聊天
        streamingChatModel.chat(chatRequest, generator.handler());
        
        // 关键：返回流式消息
        return LangGraphStreaming.streamingMessages(generator);
    }
}
```

### 工作流执行

```java
// EssayReviewApp.java
for (NodeOutput<EssayReviewState> output : graph.stream(initState)) {
    if (output instanceof StreamingOutput<EssayReviewState> streamingOutput
            && ReviewEssayNode.REVIEW_ESSAY.equals(streamingOutput.node())) {
        String chunk = streamingOutput.chunk();
        if (StringUtils.isNotEmpty(chunk)) {
            sink.next(chunk); // 实时推送
        }
    }
}
```

## 📝 自定义修改

### 修改评语风格

编辑 `ReviewEssayNode.java` 中的 `SYSTEM_PROMPT`：

```java
private static final String SYSTEM_PROMPT = """
        你是一位经验丰富、富有爱心的语文教师...
        
        评语要求：
        1. ...（自定义你的要求）
        """;
```

### 修改示例作文

编辑 `SubmitEssayNode.java` 中的作文生成逻辑。

### 添加真实作文数据

将 `SubmitEssayNode` 中的模拟数据替换为从数据库或 API 获取真实作文。

## 🎯 状态字段

| 字段 | 类型 | 说明 |
|------|------|------|
| studentName | String | 学生姓名 |
| essayTopic | String | 作文题目 |
| essayContent | String | 作文内容 |
| teacherComment | String | 教师评语 |
| essayGrade | String | 作文等级（优秀/良好/中等/需改进） |

## 💡 扩展建议

1. **添加作文查重节点**
   - 在批改前检查作文原创性

2. **添加语法检查节点**
   - 自动检测语法错误和错别字

3. **添加多维度评分节点**
   - 从内容、结构、语言等多维度评分

4. **连接真实数据库**
   - 存储学生信息和作文记录

5. **添加前端界面**
   - 学生端：提交作文
   - 教师端：查看和修改评语

## ⚠️ 注意事项

1. 确保配置了有效的 StreamingChatModel
2. 流式输出需要客户端支持 SSE
3. 生产环境需要添加异常处理和日志
4. 建议添加请求限流和超时控制

## 📚 相关文件

- [EssayReviewState.java](./src/main/java/com/moli/langgraph/state/EssayReviewState.java) - 状态定义
- [AssignEssayNode.java](./src/main/java/com/moli/langgraph/nodes/essay/AssignEssayNode.java) - 布置作业
- [SubmitEssayNode.java](./src/main/java/com/moli/langgraph/nodes/essay/SubmitEssayNode.java) - 提交作文
- [ReviewEssayNode.java](./src/main/java/com/moli/langgraph/nodes/essay/ReviewEssayNode.java) - 批改作文（流式）
- [EssayReviewApp.java](./src/main/java/com/moli/langgraph/workflow/EssayReviewApp.java) - 工作流应用
- [EssayReviewController.java](./src/main/java/com/moli/langgraph/controller/EssayReviewController.java) - API 接口
- [essay-review.html](./src/main/resources/static/essay-review.html) - 测试页面

## 🤝 技术支持

如有问题，请参考：
- [LangGraph4j 文档](https://github.com/bsorrentino/langgraph4j)
- [详细文档](./ESSAY_REVIEW_README.md)
