# 作文批改系统 - 多轮对话使用指南

## 🎯 架构设计

### 核心思想：分离工作流和对话管理

```
┌─────────────────────────────────────┐
│   EssayConversationManager          │
│   （对话管理器）                     │
│                                     │
│   - 管理会话状态                     │
│   - 管理对话记忆 (ChatMemory)       │
│   - 判断首次请求 vs 追问            │
│   - 协调工作流执行                  │
└────────────┬────────────────────────┘
             │
             ↓ (首次请求)
┌─────────────────────────────────────┐
│   EssayReviewApp                    │
│   （工作流应用）                     │
│                                     │
│   - 执行前置节点                     │
│   - 图外桥接流式输出                │
│   - 返回完整评语                     │
└─────────────────────────────────────┘
             ↓ (追问)
┌─────────────────────────────────────┐
│   StreamingChatModel                │
│   （直接对话）                       │
│                                     │
│   - 使用对话历史                     │
│   - 流式输出回答                     │
└─────────────────────────────────────┘
```

## 📁 新增文件

### 1. EssaySession.java
会话上下文，包含：
- 会话 ID
- 学生信息
- 对话记忆（MessageWindowChatMemory）
- 会话状态（INITIAL/SUBMITTED/REVIEWED/FOLLOWING_UP）

### 2. EssayConversationManager.java
对话管理器，负责：
- 创建和管理会话
- 判断是首次请求还是追问
- 首次请求：调用 EssayReviewApp 执行工作流
- 追问：使用对话历史直接对话

### 3. EssayReviewController.java（已更新）
新增接口：
- `POST /api/essay/chat` - 支持多轮对话的新接口
- `GET /api/essay/session/{sessionId}` - 获取会话信息
- `DELETE /api/essay/session/{sessionId}` - 清除会话

保留旧接口（向后兼容）：
- `GET /api/essay/review` - 旧接口
- `POST /api/essay/review` - 旧接口

## 🚀 使用方法

### 1. 首次请求（批改作文）

```bash
curl -X POST http://localhost:8080/api/essay/chat \
  -H "Content-Type: application/json" \
  -d '{
    "studentName": "张三",
    "essayTopic": "生活中的美好",
    "message": "请批改这篇作文"
  }'
```

**响应**：
- 执行完整工作流（布置作业 → 提交作文 → 批改）
- 流式输出评语
- 自动保存评语到会话记忆
- 返回 sessionId（用于后续追问）

### 2. 追问（多轮对话）

```bash
curl -X POST http://localhost:8080/api/essay/chat \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": "上一步返回的sessionId",
    "message": "能再详细点吗？"
  }'
```

**响应**：
- 使用对话历史（包含之前的评语）
- 流式输出追问回答
- 自动保存回答到会话记忆

### 3. 继续追问

```bash
curl -X POST http://localhost:8080/api/essay/chat \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": "同一个sessionId",
    "message": "这篇文章的优点是什么？"
  }'
```

### 4. 查看会话信息

```bash
curl http://localhost:8080/api/essay/session/{sessionId}
```

**响应示例**：
```json
{
  "sessionId": "abc-123-def",
  "studentName": "张三",
  "essayTopic": "生活中的美好",
  "essayContent": "...",
  "teacherComment": "...",
  "essayGrade": "优秀",
  "state": "REVIEWED",
  "createdAt": "2024-05-21T13:25:00",
  "updatedAt": "2024-05-21T13:30:00"
}
```

### 5. 清除会话

```bash
curl -X DELETE http://localhost:8080/api/essay/session/{sessionId}
```

## 💡 工作流程

### 首次请求流程

```
用户发送请求
    ↓
EssayConversationManager.handleMessage()
    ↓
创建新会话 (EssaySession)
    ↓
添加用户消息到记忆
    ↓
判断：isFirstRequest() = true
    ↓
调用 EssayReviewApp.reviewEssay()
    ↓
执行工作流：
  1. assign_essay（布置作业）
  2. submit_essay（提交作文）
  3. 图外桥接流式输出评语
    ↓
收集完整评语
    ↓
保存到会话：
  - session.setTeacherComment()
  - session.setEssayGrade()
  - session.setState(REVIEWED)
  - session.addAiMessage(评语)
    ↓
流式输出完成
```

### 追问流程

```
用户发送追问
    ↓
EssayConversationManager.handleMessage()
    ↓
获取已有会话
    ↓
添加追问消息到记忆
    ↓
判断：isFollowUp() = true
    ↓
构建包含对话历史的请求：
  ChatRequest(messages = memory.messages())
    ↓
直接调用 streamingChatModel.chat()
    ↓
流式输出回答
    ↓
保存回答到记忆：
  - session.addAiMessage(回答)
  - session.setState(REVIEWED)
    ↓
流式输出完成
```

## 🔧 技术细节

### 1. 对话记忆（ChatMemory）

使用 LangChain4j 的 `MessageWindowChatMemory`：
- 最多保留 20 条消息
- 自动滚动窗口
- 支持多轮对话上下文

```java
@Builder.Default
private MessageWindowChatMemory memory = MessageWindowChatMemory.withMaxMessages(20);
```

### 2. 会话状态机

```java
public enum SessionState {
    INITIAL,      // 初始状态，等待用户输入
    SUBMITTED,    // 作文已提交，等待批改
    REVIEWED,     // 批改完成
    FOLLOWING_UP  // 追问中
}
```

### 3. 会话存储

当前使用 `ConcurrentHashMap`（内存存储）：
```java
private final Map<String, EssaySession> sessionStore = new ConcurrentHashMap<>();
```

**生产环境建议**：
- 使用 Redis 存储会话
- 设置过期时间（如 24 小时）
- 支持分布式部署

### 4. 意图判断

当前使用简单判断：
```java
public boolean isFirstRequest() {
    return state == SessionState.INITIAL;
}
```

**可以升级为**：
- 使用 LLM 判断用户意图
- 关键词匹配
- 分类模型

## 📊 数据流示例

### 完整对话示例

#### 第 1 轮：批改作文
```
用户：{
  "studentName": "张三",
  "essayTopic": "生活中的美好",
  "message": "请批改这篇作文"
}

系统：（执行工作流，流式输出评语）
"这篇作文写得不错...（完整评语）"

会话状态：REVIEWED
记忆：[UserMessage, AiMessage(评语)]
```

#### 第 2 轮：追问
```
用户：{
  "sessionId": "abc-123",
  "message": "能再详细点吗？"
}

系统：（使用记忆中的评语，流式输出回答）
"当然可以。这篇文章的优点是...（详细分析）"

会话状态：REVIEWED
记忆：[UserMessage, AiMessage(评语), UserMessage(追问), AiMessage(回答)]
```

#### 第 3 轮：继续追问
```
用户：{
  "sessionId": "abc-123",
  "message": "这篇文章的主题立意如何？"
}

系统：（使用完整对话历史，流式输出回答）
"主题立意方面...（深入分析）"

会话状态：REVIEWED
记忆：[UserMessage, AiMessage, UserMessage, AiMessage, UserMessage, AiMessage]
```

## 🎨 前端集成示例

### TypeScript/JavaScript

```typescript
class EssayChatClient {
  private sessionId: string | null = null;

  // 首次请求
  async startReview(studentName: string, essayTopic: string) {
    const response = await fetch('/api/essay/chat', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        studentName,
        essayTopic,
        message: '请批改这篇作文'
      })
    });

    // 获取 sessionId（从响应头或首次响应中）
    this.sessionId = response.headers.get('X-Session-Id');
    
    // 处理流式响应
    return this.handleStream(response);
  }

  // 追问
  async followUp(message: string) {
    if (!this.sessionId) {
      throw new Error('No active session');
    }

    const response = await fetch('/api/essay/chat', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        sessionId: this.sessionId,
        message
      })
    });

    return this.handleStream(response);
  }

  // 处理流式响应
  private async handleStream(response: Response) {
    const reader = response.body?.getReader();
    const decoder = new TextDecoder();
    let content = '';

    while (true) {
      const { done, value } = await reader!.read();
      if (done) break;

      const chunk = decoder.decode(value);
      content += chunk;
      
      // 实时更新 UI
      this.onChunk(content);
    }

    return content;
  }

  private onChunk(content: string) {
    // 更新 UI
    console.log('Received:', content);
  }
}

// 使用示例
const client = new EssayChatClient();

// 首次请求
await client.startReview('张三', '生活中的美好');

// 追问
await client.followUp('能再详细点吗？');

// 继续追问
await client.followUp('这篇文章的主题立意如何？');
```

## 🔒 生产环境建议

### 1. 使用 Redis 存储会话

```java
@Service
public class RedisSessionStore {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    public void saveSession(String sessionId, EssaySession session) {
        String key = "essay:session:" + sessionId;
        redisTemplate.opsForValue().set(key, session, 24, TimeUnit.HOURS);
    }
    
    public EssaySession getSession(String sessionId) {
        String key = "essay:session:" + sessionId;
        return (EssaySession) redisTemplate.opsForValue().get(key);
    }
    
    public void deleteSession(String sessionId) {
        String key = "essay:session:" + sessionId;
        redisTemplate.delete(key);
    }
}
```

### 2. 添加会话认证

```java
@PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<String> chat(
        @RequestHeader("Authorization") String token,
        @RequestBody ChatRequest request) {
    
    // 验证 token
    String userId = authenticate(token);
    
    // 确保 sessionId 属于当前用户
    String sessionId = userId + ":" + request.getSessionId();
    
    return conversationManager.handleMessage(...);
}
```

### 3. 限流和配额

```java
@Service
public class RateLimiter {
    
    private final Map<String, Integer> requestCounts = new ConcurrentHashMap<>();
    
    public boolean allowRequest(String sessionId) {
        int count = requestCounts.getOrDefault(sessionId, 0);
        if (count >= 100) { // 最多 100 轮对话
            return false;
        }
        requestCounts.put(sessionId, count + 1);
        return true;
    }
}
```

## 📝 总结

### 优势

1. **职责分离**：工作流和对话管理各司其职
2. **支持多轮**：使用 ChatMemory 保留上下文
3. **状态管理**：清晰的会话状态机
4. **向后兼容**：保留旧接口
5. **易于扩展**：可以添加更多功能

### 未来优化

1. 使用 Redis 持久化会话
2. 添加意图识别（LLM 判断）
3. 支持多模态（图片、语音）
4. 添加会话分析功能
5. 支持并发会话管理
