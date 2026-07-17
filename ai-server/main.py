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
"""

from typing import Any, Dict, List

from fastapi import FastAPI
from fastapi.staticfiles import StaticFiles
from fastapi.responses import FileResponse, StreamingResponse
from pydantic import BaseModel

from chain import ask, ask_batch, ask_enhanced, ask_stream

app = FastAPI(title="码上非遗 AI 服务")

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
    """Day 6: enhanced chain — RunnableParallel 并发获取答案+关键词+推荐

    响应示例:
    {
      "question": "什么是景泰蓝？",
      "answer":   "景泰蓝，学名铜胎掐丝珐琅...",
      "keywords": ["景泰蓝", "珐琅", "掐丝", "北京"],
      "suggests": [
        {"name": "景泰蓝制作技艺", "desc": "北京市特产的..."},
        ...
      ]
    }
    """
    return ask_enhanced(req.question)
