# ARCHITECTURE.md — 系统整体架构

## 1. 设计来源

本文档完全基于 `docs/references/基于上下文工程的Agent后端设计` 编写。

---

## 2. 技术栈

| 层级 | 技术 | 版本 | 用途 |
|------|------|------|------|
| 语言 | Python | 3.11.15 | 主开发语言 |
| Web 框架 | FastAPI | — | REST API + SSE 流式端点 |
| LLM 编排 | LangChain | — | Chain 组装、RAG 管道、Tool Calling |
| Agent 图引擎 | LangGraph | — | 状态图编排、ReAct Agent、Human-in-the-loop、多智能体协作 |
| LLM 服务 | DeepSeek (via langchain-openai) | — | 对话生成、工具决策、评估裁判 |
| 向量数据库 | Chroma | — | 知识文档向量存储与检索 |
| Embedding | HuggingFace sentence-transformers | — | paraphrase-multilingual-MiniLM-L12-v2，本地 CPU 推理 |
| RAG 评估 | RAGAS | — | Faithfulness / Relevancy / Precision / Recall 自动评分 |
| 重试 | tenacity | — | API 调用指数退避重试 |
| LLM 缓存 | LangChain SQLiteCache | — | 相同请求命中缓存，节省 API 调用 |
| 前端 | 原生 HTML / CSS / JavaScript | — | 聊天 UI，SSE EventSource 消费 |

---

## 3. 开发与运行环境

### 3.1 环境管理

使用 **Miniconda3** 管理 Python 虚拟环境：

```bash
# 创建环境
conda create -n ai-server python=3.11.15

# 激活环境
conda activate ai-server

# 安装依赖
pip install -r requirements.txt
```

### 3.2 依赖清单

```
# LLM 编排
langchain
langchain-core
langchain-openai
langchain-community
langgraph

# RAG
langchain-text-splitters
chromadb
sentence-transformers

# 评估
ragas
datasets

# Web 框架
fastapi
uvicorn
python-multipart

# 工具
python-dotenv
tenacity

# 生产级（可选）
# redis  # 多进程部署时替换内存限流/缓存
```

### 3.3 环境变量

在 `ai-server/` 目录下创建 `.env` 文件：

```env
# DeepSeek API 配置
DEEPSEEK_API_KEY=your_api_key_here
DEEPSEEK_BASE_URL=https://api.deepseek.com
```

### 3.4 启动命令

```bash
# 开发模式（热重载）
cd ai-server
uvicorn main:app --reload --port 8000

# 生产模式
uvicorn main:app --host 0.0.0.0 --port 8000 --workers 4
```

---

## 4. 系统总览

系统由四大核心模块组成：

```
┌─────────────────────────────────────────┐
│            Agent 执行器                   │
│   工作流 · 单智能体 · 多智能体              │
├─────────────────────────────────────────┤
│  ┌──────────┐  ┌──────────────────────┐ │
│  │ LLM 模块  │  │     上下文管理         │ │
│  │ 工厂函数   │  │  ContextManager      │ │
│  │ 服务类    │  │  6种上下文统一调度     │ │
│  └──────────┘  └──────────────────────┘ │
├─────────────────────────────────────────┤
│            工具模块和管理                  │
│   工具定义 · 工具参数 · 执行函数 · 审核    │
└─────────────────────────────────────────┘
```

四大模块的协作关系：

- **工具模块**提供 Agent 可调用的工具能力（MCP/Skill），并管理工具的输出裁剪和执行审核流程
- **上下文管理**是启动 Agent 的关键——整理注入给 LLM 的上下文，核心工作包括上下文压缩、裁剪，并解决上下文污染、干扰、冲突、混淆四类问题
- **LLM 模块**是 Agent 的核心引擎，接收上下文、驱动推理
- **Agent 执行器**是 Agent 的运行形态：工作流、单智能体、多智能体

---

## 5. LLM 模块

### 3.1 设计思路

LLM 模块由两层组成：

| 层 | 职责 |
|---|------|
| LLM 工厂函数 | 根据传入参数判断实例化哪一个 LLM 服务类 |
| LLM 服务类 | 封装具体 LLM 提供商的调用逻辑 |

### 3.2 LLM 服务类

每个 LLM 服务类至少提供三个方法：

| 方法 | 用途 |
|------|------|
| `complete` | 核心方法，作为上下文输入的统一入口，接收完整上下文后返回完整结果 |
| `simpleChat` | 轻量对话，不携带完整上下文 |
| `generate` | 流式生成，逐 token 输出 |

### 3.3 目录结构

```
llm/
├── services/
│   ├── DeepSeekService.ts    # 具体 LLM 服务类
│   └── DoubaoService.ts
├── utils/
├── types/
│   └── index.ts              # LLM 模块类型约束
├── factory.ts                # LLM 工厂函数
└── index.ts
```

---

## 6. 工具模块和管理

### 4.1 设计要点

工具模块解决三个核心问题：

1. **工具的定义** — 定义一个工具对象需要哪些属性
2. **提供给 LLM 的工具参数** — 可以是 tools 参数（function calling），也可以是提示词文本
3. **工具的执行函数** — 实际执行逻辑

### 4.2 工具管理

工具管理器统一负责：
- 工具注册与发现
- 工具输出裁剪（控制返回给 LLM 的结果长度）
- 工具执行审核流程（敏感操作需审批）

### 4.3 目录结构

```
tool/
├── FileReadTool/
│   ├── FileReadTool.ts       # 工具定义
│   └── prompt.ts             # 工具描述
├── GrepTool/
│   ├── GrepTool.ts
│   └── prompt.ts
├── index.ts
├── ToolManager.ts            # 工具管理函数
└── types.ts                  # 工具模块类型
```

---

## 7. 上下文管理

### 5.1 核心设计

> 开发的重心在上下文的获取和整理，应用的关键核心是 LLM。

上下文管理将所有种类的上下文统一管理，让 LLM 只需要直接和上下文输入联系。

Agent 上下文面临四类典型问题：

| 问题 | 描述 | 解决方向 |
|------|------|---------|
| 上下文污染 | 无关信息混入上下文 | 上下文裁剪 |
| 上下文干扰 | 矛盾信息同时存在 | 上下文冲突检测 |
| 上下文冲突 | 新旧信息相互矛盾 | 优先级与版本管理 |
| 上下文混淆 | 信息量过大导致 LLM 迷失 | 上下文压缩 |

### 5.2 上下文类型

ContextManager 统一管理六种上下文：

| 上下文类 | 职责 |
|---------|------|
| `BaseContext` | 基础上下文类，定义通用接口 |
| `ConversationContext` | 会话历史记录 |
| `MemoryContext` | 用户长期记忆 |
| `StructuredOutputContext` | 结构化输出约束 |
| `SystemPromptContext` | 系统提示词 |
| `ToolContext` | 工具定义与管理 |

### 5.3 目录结构

```
context/
├── base/
│   └── BaseContext.ts             # 基础上下文类
├── modules/
│   ├── ConversationContext.ts     # 会话历史记录
│   ├── MemoryContext.ts           # 用户记忆
│   ├── StructuredOutputContext.ts # 结构化输出
│   ├── SystemPromptContext.ts     # 系统提示词
│   └── ToolContext.ts             # 工具管理
├── ContextManager.ts              # 上下文管理器
├── index.ts
└── types.ts                       # 类型约束
```

---

## 8. Agent 执行器

### 6.1 执行形态

Agent 执行器是 Agent 的运行载体，支持三种形态：

| 形态 | 说明 |
|------|------|
| 工作流 | 预定义步骤的有向图，按固定流程执行 |
| 单智能体 | 一个 LLM 自主循环调用工具完成任务 |
| 多智能体 | 多个 Agent 协作（Supervisor + Workers 等模式） |

### 6.2 目录结构

```
agent/
├── SimpleAgent.ts    # 简单 Agent 实现（ReAct 循环）
└── MultiAgent.ts     # 多 Agent 编排
```

---

## 9. 项目目录结构

```
my-ai-app/
├── core/                     # 核心系统模块
│   ├── llm/                  # LLM 服务层
│   │   ├── services/         # LLM 提供商实现
│   │   ├── utils/            # 工具函数（executeToolLoop）
│   │   └── factory.ts        # LLM 服务工厂
│   ├── context/              # 上下文管理系统
│   │   ├── modules/          # 6 种上下文模块
│   │   └── ContextManager.ts # 统一管理器
│   ├── tool/                 # 工具管理系统
│   │   ├── ReadFile/
│   │   ├── ListFiles/
│   │   └── ToolManager.ts    # 工具管理器
│   ├── agent/                # Agent 编排
│   │   ├── SimpleAgent.ts
│   │   └── MultiAgent.ts
│   └── promptManager/        # 提示词管理
├── evaluation/               # 测试评估系统
│   ├── EventBus.ts           # 事件总线
│   ├── evaluate.ts           # 评估器
│   └── example.ts            # 评估示例
├── utils/                    # 工具函数
│   └── logger.ts             # 日志工具
├── config/                   # 配置管理
│   └── env.ts                # 环境变量加载
├── examples/                 # 使用示例
│   ├── simple-chat.ts
│   └── multi-chat.ts
└── docs/                     # 项目文档
    └── 架构设计-上下文工程.md
```

---

## 10. RAG 策略

### 8.1 基本流程

```
文本分割成块 → Embedding 向量化 → 存入索引
                                    ↓
用户查询 → 向量检索 → 取 Top-K 片段 → 拼入 Prompt → LLM 生成
```

### 8.2 分块

分块的核心权衡：**足够长**以提供充足推理上下文 × **足够具体**以保证向量语义精准。

选择依据：具体场景需求 + 嵌入模型的最大 Token 数。

### 8.3 向量化

选择嵌入模型需考虑两个维度：
- **向量维度大小** — 维度过低导致语义丢失，过高增加存储和计算成本
- **模型语言支持** — 确保覆盖目标文本语言

### 8.4 搜索索引

#### 向量存储索引

| 索引类型 | 原理 | 适用场景 |
|---------|------|---------|
| 平面索引 | 暴力计算查询向量与所有片段距离 | 小数据量 |
| 向量索引 (ANN) | 近似最近邻：聚类/树结构/HNSW | 海量数据 |

同时保存元数据（日期、来源等）辅助过滤。

#### 分层索引

核心思路：**先大再小，先粗筛再细筛**。
- 第一步：通过摘要索引过滤出相关文档
- 第二步：在相关文档内部进行片段级检索

#### 假设性问题与 HyDE

| 策略 | 源头 | 做法 |
|------|------|------|
| 假设性问题 | 文档片段 | 对每个文档块，让 LLM 生成几个可能的问题，向量化问题建立索引。检索时用户问题匹配"文档问题"→ 找到文档块 |
| 假设性文档 (HyDE) | 用户问题 | 让 LLM 生成假设性答案文档，拿这个"答案"去检索，同时也可合并原始问题的检索结果 |

### 8.5 上下文增强

核心原则：**检索时取更小片段提升精度，输入 LLM 时补充周围上下文保证推理质量。**

#### 句子扩展
检索时保持小文档块，输入 LLM 前进行上下文扩展，补充周围文本。

#### 父文档检索
文档分割为两层：父级片段（大块，用于最终输入 LLM）+ 子片段（小块，用于检索）。检索命中子片段 → 回溯到对应父级片段 → 父级片段输入 LLM。

### 8.6 融合检索

结合关键词传统搜索和语义向量搜索，互补各自不足。

### 8.7 排序和过滤

检索结果使用**重排序（re-ranking）模型**进行二次筛选优化。

### 8.8 查询转换

#### 通用查询转换
让 LLM 生成更通用的查询 → 检索更高层次的上下文 → 为原始查询提供语义基础。

#### 查询重写
利用 LLM 对初始查询进行改写或重试，提升检索效果。

### 8.9 引用来源

两种实现方式：
1. 在提示词中要求 LLM 在答案中标注来源 ID
2. 将 LLM 响应与向量数据库中的原始文本块进行匹配

### 8.10 查询路由

由 LLM 驱动的决策步骤：根据用户查询判断下一步做什么——是检索、是直接回答、还是调用特定工具。

---

## 11. 搜索代理

### 9.1 核心理念

> LLM 使用各种搜索或检索工具，动态地、按需地获取相关上下文。

本质上是 **LLM 在自主循环中使用工具**。模型能力对搜索代理效果影响很大。

适用场景：**上下文密度要求高、动态上下文多**。

### 9.2 实现要点

搜索代理由三部分组成：

| 模块 | 职责 |
|------|------|
| 工具实现 | 定义每个工具的执行逻辑 |
| 工具管理 | 工具注册、提供给 LLM、接收 LLM 工具调用指令后执行对应函数、结果返回 LLM |
| 工具调用循环 | **LLM 循环执行并判断**：是否需要工具 → 调用工具 → 接收结果 → 继续思考或输出答案 |

---

## 12. Skills 系统

Skills 开发的核心步骤：**发现 → 解析 → 使用 → 管理**。
