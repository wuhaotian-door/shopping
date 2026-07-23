# 统一 Agent 技术设计 (v2)

## 设计输入

- 架构: `ARCHITECTURE.md` (上下文工程四大模块)
- 需求: `docs/product-specs/ai-agent-智能非遗助手.md`

## 核心思路

保留 `core/llm` `core/context` `core/tool` `core/promptManager` 分层架构，但将 `core/agent/` 中两个 Agent 合并为一个 `UnifiedAgent`。

用户不选模式——Agent 持有多个工具，LLM 自主判断调哪个。

## 模块

### core/llm/ — 不变
factory.py → DeepSeekService
tool_loop.py → 工具调用循环

### core/context/ — 不变
ContextManager 统一管理 6 种上下文

### core/tool/ — 工具扩展
ToolManager + 6 个工具:
- search_knowledge
- search_inheritors
- search_projects
- search_products
- recommend_content
- get_user_memory

知识库按数据来源分 4 个 Chroma 集合。

### core/agent/ — UnifiedAgent
基于 LangGraph create_react_agent，一个 Agent 处理所有场景。

### 端点 (5个)
GET  /                  → 前端页面
POST /ai/chat           → 问答
POST /ai/chat/stream    → SSE 流式
GET  /ai/sessions       → 会话列表
DELETE /ai/sessions/{id} → 删除会话
POST /ai/knowledge/reload → 重建知识库

### 前端
Vue 3, 单一对话页, SSE 流式渲染, 会话侧栏
