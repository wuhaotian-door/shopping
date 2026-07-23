# 统一 Agent 技术设计

## 设计输入

产品需求：`docs/product-specs/ai-agent-智能非遗助手.md`

## 核心思路

**一个 Agent，一组工具，一种界面。**

Agent 持有多个工具（搜索知识库、搜索传承人、搜索项目、推荐内容等）。用户说什么，LLM 自己判断该调哪个工具——不预判流程，把决策权交给 LLM。

## 架构

```
用户 → FastAPI → Agent（LangGraph ReAct）
                    ├── search_knowledge    知识库检索
                    ├── search_inheritors   传承人搜索
                    ├── search_projects     项目活动搜索
                    ├── search_products     商品搜索
                    └── recommend           个性化推荐
                         │
                    Chroma 向量库（多集合）
```

## 模块设计

### 1. 知识库（多集合 Chroma）

不再只有一个 "knowledge" 集合，而是按来源分类：

| 集合 | 内容 | 元数据 |
|------|------|--------|
| `knowledge` | 非遗知识文章 | title, category |
| `inheritors` | 传承人档案 | name, region, craft |
| `projects` | 项目/活动 | title, location, date |
| `products` | 商品 | name, category, price |

每个集合独立建索引，Agent 根据查询目标选择对应集合检索。

### 2. 工具集

| 工具 | 用途 | LLM 何时调用 |
|------|------|------------|
| `search_knowledge` | 搜索非遗知识文章 | 用户问历史/工艺/特点 |
| `search_inheritors` | 搜索传承人 | 用户问"有哪些传承人" |
| `search_projects` | 搜索项目活动 | 用户问"最近有什么活动" |
| `search_products` | 搜索商品 | 用户问文创/商品相关 |
| `recommend_content` | 推荐非遗内容 | 用户请求推荐 |
| `get_user_memory` | 获取用户偏好记忆 | 需要个性化回答时 |

### 3. Agent

基于 LangGraph `create_react_agent`，核心流程：

```
用户消息 → ContextManager 组装上下文（系统提示词 + 对话历史 + 用户记忆）
         → LLM 推理
         → 需要查资料？调用对应工具 → 结果回传
         → 需要推荐？调用 recommend_content
         → 不需要工具？直接回答
         → 输出（流式 SSE）
         → 对话历史持久化到本地文件
```

### 4. 会话持久化

对话记录存为本地 JSON 文件：`sessions/{thread_id}.json`

```json
{
  "id": "session-xxx",
  "title": "用户第一句话",
  "created_at": "...",
  "messages": [
    {"role": "user", "content": "..."},
    {"role": "assistant", "content": "...", "sources": [...]}
  ]
}
```

### 5. 端点

| 端点 | 说明 |
|------|------|
| `GET /` | 聊天页面 |
| `POST /ai/chat` | 一次性问答 |
| `POST /ai/chat/stream` | SSE 流式问答 |
| `GET /ai/sessions` | 会话列表 |
| `DELETE /ai/sessions/{id}` | 删除会话 |

### 6. 前端

一个页面：聊天区 + 会话侧栏。SSE 逐字渲染。非遗主题色。

## 与前版的区别

| | 前版 | 新版 |
|---|------|------|
| 需求 | 4 份独立需求文档 | 1 份统一需求 |
| 模式 | 对话/RAG/Agent 三种模式切换 | 无需切换，Agent 自主判断 |
| 工具 | 仅 search_knowledge + list_documents | 按信息类型分 6 个工具 |
| 知识库 | 单一集合 | 按来源分 4 个集合 |
| 端点 | 12 个 | 5 个 |
| 会话 | 内存 MemorySaver | 本地 JSON 文件持久化 |

## 实现范围

本次仅实现需求 #1（统一 Agent），其他文件保持不动。新文件：

```
ai-server/
├── main.py              # 重写（简洁版）
├── agent.py             # 新建：统一 Agent
├── tools.py             # 新建：6 个工具
├── knowledge.py         # 新建：多集合知识库
├── sessions.py          # 新建：会话持久化
├── static/index.html    # 更新：简化前端
└── docs/product-specs/ai-agent-智能非遗助手.md  # 已写入
```
