# Agent 后端技术设计

## 设计输入

- 产品需求：`docs/product-specs/` 下 4 份需求文档
- 架构设计：`ARCHITECTURE.md`
- 参考文档：`docs/references/基于上下文工程的Agent后端设计`

## 需求 → 技术方案映射

| 产品需求 | 核心模块 | 关键端点 | 技术方案 |
|---------|---------|---------|---------|
| AI 智能对话 | SimpleAgent + ConversationContext | POST /ai/chat, /ai/chat/stream | LangGraph ReAct Agent + MemorySaver + SSE |
| 非遗知识库检索 | RAG 管道 + SearchKnowledgeTool | POST /ai/rag, /ai/rag/stream | Chroma 向量检索 + DeepSeek 生成 + 来源标注 |
| 语义搜索 | 查询转换 + 跨类型检索 | POST /ai/search | LLM 查询改写 + 向量检索 + 元数据过滤 |
| 个性化推荐 | MemoryContext + 推荐引擎 | GET /ai/recommend | 用户行为记忆 + LLM 推荐 + 冷启动热门 |

---

## 模块设计

### 1. LLM 服务层 (core/llm/)

```
core/llm/
├── factory.py              # LLM 工厂函数
├── services/
│   └── deepseek_service.py # DeepSeek 服务类
└── utils/
    └── tool_loop.py        # 工具调用循环
```

**factory.py** — `create_llm(model, temperature, max_tokens)` → 返回对应服务类实例

**deepseek_service.py** — `DeepSeekService` 类：
- `complete(messages)` — 核心方法，接收完整消息列表，返回完整响应
- `simple_chat(question)` — 轻量对话（不带复杂上下文）
- `generate(messages)` — 流式生成器，逐 token yield

**tool_loop.py** — `execute_tool_loop(llm, tools, messages)` — LLM 循环调用工具直到输出最终答案

### 2. 上下文管理系统 (core/context/)

```
core/context/
├── ContextManager.py        # 统一上下文管理器
└── modules/
    ├── BaseContext.py             # 基础上下文抽象
    ├── SystemPromptContext.py     # 系统提示词
    ├── ConversationContext.py     # 会话历史
    ├── MemoryContext.py           # 用户长期记忆
    ├── ToolContext.py             # 工具定义上下文
    └── StructuredOutputContext.py # 结构化输出约束
```

**ContextManager.assemble()** 流程：
1. 收集所有已注册的 Context 模块
2. 逐个调用 `render()` 获取上下文文本
3. 检查总 token 数，超出限制时压缩 ConversationContext（裁剪早期轮次）
4. 拼接为最终上下文

**各 Context 职责**：
- `BaseContext` — 抽象基类，定义 `render() -> str` 和 `token_count() -> int`
- `SystemPromptContext` — 渲染系统提示词（角色设定 + 行为规则）
- `ConversationContext` — 管理最近 N 轮对话，超出阈值裁剪最早轮次
- `MemoryContext` — 存储用户偏好、行为特征、常问话题，跨会话持久化
- `ToolContext` — 将工具列表渲染为 function calling schema 或文本描述
- `StructuredOutputContext` — 注入 JSON 输出格式约束

### 3. 工具系统 (core/tool/)

```
core/tool/
├── ToolManager.py           # 工具管理器
├── SearchKnowledgeTool.py   # 搜索知识库
├── ListDocumentsTool.py     # 列出文档
└── AddKnowledgeTool.py      # 添加知识（需审批）
```

**ToolManager** 职责：
- 工具注册：`register(tool)` 
- 工具发现：`get_tool(name)` / `list_tools()`
- 输出裁剪：执行结果超过阈值自动截断
- 审批标记：`requires_approval(tool_name) -> bool`
- LLM 绑定：`bind_to_llm(llm)` → 返回 `llm.bind_tools(tools)`

**工具定义规范**（每个工具文件）：
- `name` — 工具名称
- `description` — 给 LLM 看的描述
- `parameters` — JSON Schema 参数定义
- `execute(**kwargs)` — 执行函数
- `requires_approval` — 是否需要审批

### 4. Agent 执行器 (core/agent/)

```
core/agent/
├── SimpleAgent.py           # 单智能体 ReAct
└── MultiAgent.py            # 多智能体 Supervisor + Workers
```

**SimpleAgent**：
- 基于 LangGraph `create_react_agent`
- 集成 ContextManager → 每次调用前组装上下文
- MemorySaver 持久化对话状态（按 thread_id 隔离）
- 流式输出：`astream_events()` 逐事件 yield

**MultiAgent**：
- Supervisor 节点：分析用户意图 → 路由到 Researcher / Writer / END
- Researcher Worker：持有 search_knowledge_base + list_documents
- Writer Worker：持有 add_knowledge（需审批）
- 基于 LangGraph StateGraph 自定义构建

### 5. 提示词管理 (core/promptManager/)

```
core/promptManager/
└── PromptManager.py
```

- 管理所有提示词模板
- 支持变量插值
- 分类：系统提示词、RAG 提示词、推荐提示词、评估提示词

### 6. 应用入口 (main.py)

**端点设计**：

| 端点 | 方法 | 说明 |
|------|------|------|
| `/` | GET | 聊天页面 |
| `/ai/chat` | POST | 一次性问答 |
| `/ai/chat/stream` | POST | SSE 流式问答 |
| `/ai/rag` | POST | RAG 检索增强问答 |
| `/ai/rag/stream` | POST | RAG 流式问答 |
| `/ai/search` | POST | 语义搜索（跨类型） |
| `/ai/knowledge/reload` | POST | 重建知识库 |
| `/ai/agent` | POST | Tool Calling Agent |
| `/ai/agent/stream` | POST | Agent 流式 |
| `/ai/agent/multi` | POST | Multi-Agent |
| `/ai/recommend` | GET | 个性化推荐 |
| `/ai/usage` | GET | Token 用量统计 |

### 7. 前端 (static/index.html)

- 纯 HTML/CSS/JS，非遗主题样式
- SSE EventSource 消费 `/ai/chat/stream`
- 会话管理：新建、切换、删除（localStorage 持久化）
- 响应式布局

---

## 数据流

### 智能问答流
```
用户问题 → main.py → SimpleAgent.run()
  → ContextManager.assemble()
    → SystemPromptContext.render()
    → ConversationContext.render()  (历史对话)
    → ToolContext.render()          (可用工具)
  → LLM 推理 → 需要工具？→ ToolManager.execute() → 结果回传
  → 最终答案 → SSE 逐 token 输出
```

### RAG 检索流
```
用户问题 → main.py → ContextManager.assemble()
  → SystemPromptContext.render()
  → Chroma 向量检索（Top-K 片段）
  → 拼入上下文 → LLM 生成 → {answer, sources}
```

## 关键设计决策

1. **层间解耦**：ContextManager 不直接依赖 LLM；LLM 不直接依赖 Tool。通过 Agent 层协调
2. **上下文先组装再注入**：所有 Context 在 Agent 调用前通过 ContextManager 统一组装
3. **内存级会话**：MemorySaver 存对话状态，后续可替换为 SQLite/Postgres Saver
4. **工具审批流**：ToolManager 标记审批工具，Agent 执行前检查
