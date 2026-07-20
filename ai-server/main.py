"""
码上非遗 AI 服务 — FastAPI + LangChain

启动方式:
  cd ai-server
  uvicorn main:app --reload --port 8000

API 端点:
  GET  /              — 聊天页面
  GET  /hello/{name}  — 测试
  POST /ai/chat       — 非遗知识问答（一次性返回）
  POST /ai/chat/stream— 非遗知识问答（SSE 流式，打字机效果）
  POST /ai/batch      — 批量并发问答
  POST /ai/chat/v2    — 增强问答（答案 + 关键词 + 推荐，RunnableParallel）
  POST /ai/rag         — RAG 检索增强问答（答案 + 来源引用）
  POST /ai/rag/stream  — RAG 流式问答
  POST /ai/knowledge/reload — 重建知识库
  POST /ai/agent       — Agent 工具调用（LLM 自主决定查资料还是列举）
  POST /ai/agent/graph — LangGraph ReAct Agent（状态图 + 对话记忆）
  POST /ai/agent/graph/stream — LangGraph 流式（展示每步执行状态）
  POST /ai/agent/human/start  — Human-in-the-loop: 启动（可能暂停要审批）
  POST /ai/agent/human/resume — Human-in-the-loop: 审批后恢复
  POST /ai/agent/subgraph/start  — Subgraph Agent: 启动（子图封装审批流）
  POST /ai/agent/subgraph/resume — Subgraph Agent: 审批后恢复
  POST /ai/eval/rag         — RAGAS 评估（Faithfulness/Relevancy/Precision/Recall）
  GET  /ai/usage            — Token 用量统计（Level 4）
  GET  /ai/cache/stats      — 缓存命中统计（Level 4）
"""

from typing import Any, Dict, List

from fastapi import FastAPI
from fastapi.staticfiles import StaticFiles
from fastapi.responses import FileResponse, StreamingResponse
from pydantic import BaseModel

from chain import ask, ask_batch, ask_enhanced, ask_stream
from rag import build_knowledge_base, rag_ask, rag_ask_stream
from tools import agent_chat
from agent_graph import graph_ask, graph_ask_stream
from agent_human import human_agent_start, human_agent_resume, human_agent_get_state
from agent_subgraph import subgraph_agent_start, subgraph_agent_resume, subgraph_agent_get_state
from agent_multi import multi_agent_start, multi_agent_resume, multi_agent_get_state
from production import setup_cache, create_rate_limit_middleware, get_usage, get_cache_stats

app = FastAPI(title="码上非遗 AI 服务")

# ---- Level 4: 生产级功能 ----

# ① 启用 LLM 缓存（SQLite）
setup_cache()

# ② 添加频率限制中间件
from starlette.middleware.base import BaseHTTPMiddleware
app.add_middleware(BaseHTTPMiddleware, dispatch=create_rate_limit_middleware())

# 托管静态文件（前端聊天页面）
app.mount("/static", StaticFiles(directory="static"), name="static")


# ============================================================
# 请求/响应模型
# ============================================================

class ChatRequest(BaseModel):
    question: str


class ChatResponse(BaseModel):
    question: str
    answer: str


class BatchRequest(BaseModel):
    questions: List[str]


class BatchResponse(BaseModel):
    results: List[ChatResponse]


# ============================================================
# 端点
# ============================================================

@app.get("/")
async def root():
    """聊天页面"""
    return FileResponse("static/index.html")


@app.get("/hello/{name}")
async def say_hello(name: str):
    return {"message": f"Hello {name}"}


@app.post("/ai/chat", response_model=ChatResponse)
async def ai_chat(req: ChatRequest):
    """Day 1: invoke — 等模型生成完，一次性返回完整结果"""
    answer = ask(req.question)
    return ChatResponse(question=req.question, answer=answer)


@app.post("/ai/chat/stream")
async def ai_chat_stream(req: ChatRequest):
    """Day 3 关键点: stream — SSE 流式返回，前端逐字渲染打字机效果

    SSE 格式:
      data: {"token": "景"}    # 每生成一个 token 就发一条
      data: {"token": "泰"}    # 前端累积拼接渲染
      ...
      data: [DONE]             # 结束标记
    """

    def generate():
        for token in ask_stream(req.question):
            # SSE 协议: data: <json> + 双换行
            yield f"data: {{\"token\": {__import__('json').dumps(token)}}}\n\n"
        yield "data: [DONE]\n\n"

    return StreamingResponse(
        generate(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "X-Accel-Buffering": "no",   # 禁用 nginx 缓冲
        },
    )


@app.post("/ai/batch", response_model=BatchResponse)
async def ai_batch(req: BatchRequest):
    """Day 3 关键点: batch — 多个问题并发处理

    与逐个调用 /ai/chat 的区别:
      - 串行: 10 个问题各 3s → 30s
      - batch: 10 个问题并发 → ~3s
    """
    answers = ask_batch(req.questions)
    results = [
        ChatResponse(question=q, answer=a)
        for q, a in zip(req.questions, answers)
    ]
    return BatchResponse(results=results)


@app.post("/ai/chat/v2")
async def ai_chat_v2(req: ChatRequest) -> Dict[str, Any]:
    """Day 6: enhanced chain — RunnableParallel 并发获取答案+关键词+推荐"""
    return ask_enhanced(req.question)


# ============================================================
# Level 2: RAG 端点
# ============================================================

@app.post("/ai/rag")
async def ai_rag(req: ChatRequest):
    """RAG 检索增强问答 — 先查知识库，再基于文档回答

    响应:
      {
        "question": "紫砂壶是什么？",
        "answer":   "紫砂壶是宜兴特产的手工陶土茶壶...",
        "sources":  [
          {"content": "紫砂壶起源于明代...", "source": "紫砂壶.md"},
          ...
        ]
      }
    """
    return rag_ask(req.question)


@app.post("/ai/rag/stream")
async def ai_rag_stream(req: ChatRequest):
    """RAG 流式问答 — SSE 逐 token 返回"""

    def generate():
        for token in rag_ask_stream(req.question):
            yield f"data: {{\"token\": {__import__('json').dumps(token)}}}\n\n"
        yield "data: [DONE]\n\n"

    return StreamingResponse(
        generate(),
        media_type="text/event-stream",
        headers={"Cache-Control": "no-cache", "X-Accel-Buffering": "no"},
    )


@app.post("/ai/knowledge/reload")
async def ai_knowledge_reload():
    """重建知识库 — 新增/修改文档后调用此接口"""
    build_knowledge_base()
    return {"message": "知识库重建完成"}


@app.post("/ai/agent")
async def ai_agent(req: ChatRequest):
    """Tool Calling Agent — LLM 自主决定调用哪个工具

    与 /ai/rag 的区别:
      RAG:  始终执行"检索 → 生成"流程（你替 LLM 做了决策）
      Agent: LLM 自己判断要不要调工具、调哪个工具

    响应示例:
    {
      "answer": "知识库中有以下文档...",
      "tool_calls": [
        {"tool": "list_documents", "args": {}, "result": "..."}
      ]
    }
    """
    return agent_chat(req.question)


# ============================================================
# Level 3: LangGraph Agent 端点
# ============================================================

class ThreadRequest(BaseModel):
    question: str
    thread_id: str = "default"


@app.post("/ai/agent/graph")
async def ai_agent_graph(req: ThreadRequest):
    """LangGraph ReAct Agent — 状态图 + 对话记忆

    thread_id 相同的请求共享对话历史:
      POST {"question": "紫砂壶怎么制作的？", "thread_id": "abc"}
      POST {"question": "刚才提到的步骤能详细说说吗？", "thread_id": "abc"}
      → Agent 记得上一轮说过什么
    """
    return graph_ask(req.question, req.thread_id)


@app.post("/ai/agent/graph/stream")
async def ai_agent_graph_stream(req: ThreadRequest):
    """LangGraph 流式 — 输出每一步执行状态

    SSE 事件类型:
      {"type":"token","content":"..."}      → 打字机文字
      {"type":"tool_start","name":"...","input":{...}} → 工具开始
      {"type":"tool_end","name":"...","output":"..."}  → 工具完成
    """

    async def generate():
        async for event in graph_ask_stream(req.question, req.thread_id):
            yield f"data: {__import__('json').dumps(event, ensure_ascii=False)}\n\n"
        yield "data: [DONE]\n\n"

    return StreamingResponse(
        generate(),
        media_type="text/event-stream",
        headers={"Cache-Control": "no-cache", "X-Accel-Buffering": "no"},
    )


# ============================================================
# Level 3: Human-in-the-loop 端点
# ============================================================

class ResumeRequest(BaseModel):
    thread_id: str
    approved: bool


@app.post("/ai/agent/human/start")
async def ai_agent_human_start(req: ThreadRequest):
    """启动 Human-in-the-loop Agent

    可能返回两种状态:
      - status="done":       Agent 直接完成了
      - status="interrupted": Agent 暂停，返回 interrupt 信息，等 /resume 审批
    """
    return human_agent_start(req.question, req.thread_id)


@app.post("/ai/agent/human/resume")
async def ai_agent_human_resume(req: ResumeRequest):
    """审批后恢复 Agent"""
    return human_agent_resume(req.thread_id, req.approved)


@app.get("/ai/agent/human/state/{thread_id}")
async def ai_agent_human_state(thread_id: str):
    """查询 Agent 当前状态"""
    return human_agent_get_state(thread_id)


# ============================================================
# Level 3: Subgraph Agent 端点
# ============================================================

@app.post("/ai/agent/subgraph/start")
async def ai_agent_subgraph_start(req: ThreadRequest):
    """启动 Subgraph Agent — tool_node 内部调用 tool_exec_subgraph 子图

    和 /ai/agent/human/start 功能相同（审批流），但实现方式不同:
      human: tool_node 是一个 50+ 行的函数，审批逻辑混在一起
      subgraph: tool_node 调用子图，子图内部拆成 check → approve → execute 三个节点
    """
    return subgraph_agent_start(req.question, req.thread_id)


@app.post("/ai/agent/subgraph/resume")
async def ai_agent_subgraph_resume(req: ResumeRequest):
    """审批后恢复 Subgraph Agent"""
    return subgraph_agent_resume(req.thread_id, req.approved)


@app.get("/ai/agent/subgraph/state/{thread_id}")
async def ai_agent_subgraph_state(thread_id: str):
    """查询 Subgraph Agent 当前状态"""
    return subgraph_agent_get_state(thread_id)


# ============================================================
# Level 3: RAGAS 评估端点
# ============================================================

class EvalRequest(BaseModel):
    test_questions: List[Dict[str, str]]  # [{"question": "...", "ground_truth": "..."}, ...]
    evaluate_retrieval: bool = True       # 是否评估检索质量
    evaluate_generation: bool = True      # 是否评估生成质量


@app.post("/ai/eval/rag")
async def ai_eval_rag(req: EvalRequest):
    """RAGAS 评估 RAG 管道

    输入一组测试问题 + 标准答案，输出 Faithfulness / Relevancy / Precision / Recall 等指标。

    指标说明:
      - Faithfulness:       答案是否100%源自检索到的文档（不编造）
      - Answer Relevancy:   答案是否紧扣用户问题
      - Context Precision:  检索到的文档中，相关的占多少（信噪比）
      - Context Recall:     标准答案涉及的信息，检索到了多少（查全率）
    """
    from eval_ragas import evaluate_rag
    return evaluate_rag(
        test_questions=req.test_questions,
        evaluate_retrieval=req.evaluate_retrieval,
        evaluate_generation=req.evaluate_generation,
    )


# ============================================================
# Level 4: 生产级端点 — 用量 / 缓存统计
# ============================================================

@app.get("/ai/usage")
async def ai_usage():
    """Token 用量统计

    返回累计 token 消耗 + 估算费用 + 最近 10 条调用记录。

    DeepSeek 定价参考 (2024):
      - 输入: 1 RMB / 百万 token
      - 输出: 2 RMB / 百万 token
    """
    return get_usage()


@app.get("/ai/cache/stats")
async def ai_cache_stats():
    """LLM 缓存统计 — 命中率等"""
    return get_cache_stats()


# ============================================================
# Level 5: Multi-Agent 端点
# ============================================================

@app.post("/ai/agent/multi/start")
async def ai_agent_multi_start(req: ThreadRequest):
    """启动 Multi-Agent 协作

    Supervisor 自动判断交给 Researcher 还是 Writer:
      - Researcher: 查资料（search_knowledge_base / list_documents）
      - Writer:    保存内容（add_knowledge，需审批）
    """
    return multi_agent_start(req.question, req.thread_id)


@app.post("/ai/agent/multi/resume")
async def ai_agent_multi_resume(req: ResumeRequest):
    """审批后恢复 Multi-Agent"""
    return multi_agent_resume(req.thread_id, req.approved)


@app.get("/ai/agent/multi/state/{thread_id}")
async def ai_agent_multi_state(thread_id: str):
    """查询 Multi-Agent 当前状态"""
    return multi_agent_get_state(thread_id)
