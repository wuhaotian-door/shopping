"""
码上非遗 AI 服务 — FastAPI + LangChain

启动方式:
  cd ai-server
  uvicorn main:app --reload --port 8000

API 端点:
  GET  /              — 聊天页面
  GET  /hello/{name}  — 测试
  POST /ai/chat       — AI 非遗知识问答
"""

from fastapi import FastAPI
from fastapi.staticfiles import StaticFiles
from fastapi.responses import FileResponse
from pydantic import BaseModel

from chain import ask

app = FastAPI(title="码上非遗 AI 服务")

# 托管静态文件（前端聊天页面）
app.mount("/static", StaticFiles(directory="static"), name="static")


# --- 请求/响应模型 ---

class ChatRequest(BaseModel):
    question: str


class ChatResponse(BaseModel):
    question: str
    answer: str


# --- 端点 ---

@app.get("/")
async def root():
    """聊天页面"""
    return FileResponse("static/index.html")


@app.get("/hello/{name}")
async def say_hello(name: str):
    return {"message": f"Hello {name}"}


@app.post("/ai/chat", response_model=ChatResponse)
async def ai_chat(req: ChatRequest):
    """Level 1 Day 1: 第一条 LCEL 链 — 非遗知识问答"""
    answer = ask(req.question)
    return ChatResponse(question=req.question, answer=answer)
