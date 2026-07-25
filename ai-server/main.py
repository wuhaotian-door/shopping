"""
码上非遗 AI 服务 — FastAPI 入口

启动: python main.py

架构:
  main.py  →  路由层（HTTP 端点 + SSE 流式 + 中间件）
  agent.py →  核心层（LangGraph ReAct Agent + 会话管理）
  tools.py →  工具层（5 个工具 + 知识库 + 三层搜索）
  bootstrap.py → 数据层（MySQL → SQLite → .md → Chroma）
"""

import json
import logging
import os
import time

from dotenv import load_dotenv
from fastapi import FastAPI, Request
from fastapi.responses import FileResponse, StreamingResponse, JSONResponse
from pydantic import BaseModel

# ---- 加载环境变量 ----
load_dotenv()

# ---- 日志：输出格式 [HH:MM:SS] [LEVEL] module | message ----
logging.basicConfig(
    level=logging.INFO,
    format="[%(asctime)s] [%(levelname)-5s] %(name)s | %(message)s",
    datefmt="%H:%M:%S",
)
logger = logging.getLogger("ai-server")

from agent import UnifiedAgent
from tools import build_all


# ============================================================
# 应用初始化
#   - FastAPI 实例
#   - UnifiedAgent（启动时完成 LLM + Embedding + 数据同步 + RAG）
# ============================================================

app = FastAPI(title="码上非遗 AI 服务")

logger.info(f"PID={os.getpid()} 启动中...")
agent = UnifiedAgent()
logger.info("服务就绪")


# ============================================================
# 中间件：每个 HTTP 请求记录方法、路径、状态码、耗时
# ============================================================

@app.middleware("http")
async def log_req(req: Request, call_next):
    t0 = time.time()
    r = await call_next(req)
    logger.info(f"{req.method} {req.url.path} → {r.status_code} ({time.time() - t0:.2f}s)")
    return r


# ---- 全局异常捕获：未处理异常返回 500 + 错误详情 ----

@app.exception_handler(Exception)
async def on_err(req: Request, exc: Exception):
    logger.error(f"[{req.method} {req.url.path}] {exc}", exc_info=True)
    return JSONResponse(status_code=500, content={"error": str(exc)})


# ============================================================
# 请求模型
# ============================================================

class ChatReq(BaseModel):
    """对话请求"""
    question: str
    thread_id: str = "default"  # 会话 ID，相同 ID 共享 LangGraph 对话记忆
    uid: str = ""               # 用户 UID（role_id），用于会话隔离


# ============================================================
# 端点
# ============================================================

@app.get("/")
async def root():
    """返回前端聊天页面"""
    p = os.path.join(os.path.dirname(__file__), "static", "index.html")
    return FileResponse(p) if os.path.exists(p) else {"msg": "AI 服务运行中"}


@app.post("/ai/chat")
async def chat(req: ChatReq):
    """一次性问答：提交问题 → Agent 推理 → 返回完整答案 + 工具调用记录"""
    return agent.chat(req.question, req.thread_id, uid=req.uid)


@app.post("/ai/chat/stream")
async def chat_stream(req: ChatReq):
    """
    SSE 流式问答：逐 token 推送打字机效果

    SSE 事件类型:
      {"type": "token", "content": "..."}    → 前端逐字追加
      {"type": "tool_start", "name": "..."}  → 前端显示工具调用状态
      {"type": "tool_end",   "name": "..."}
      {"type": "done"}                       → 流结束
    """

    async def gen():
        async for e in agent.chat_stream(req.question, req.thread_id, uid=req.uid):
            yield f"data: {json.dumps(e, ensure_ascii=False)}\n\n"
        yield "data: [DONE]\n\n"

    return StreamingResponse(
        gen(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",        # 禁止缓存
            "X-Accel-Buffering": "no",          # 禁止 nginx 缓冲（直通 SSE）
        },
    )


@app.get("/ai/sessions")
async def list_sessions(uid: str = ""):
    """返回指定 UID 的历史会话列表（前端侧栏），uid 为空时返回空列表"""
    if not uid:
        return {"sessions": []}
    return {"sessions": agent.list_sessions(uid)}


@app.delete("/ai/sessions/{tid}")
async def del_session(tid: str, uid: str = ""):
    """删除指定 UID 的会话"""
    if uid:
        agent.delete_session(uid, tid)
    return {"ok": True}


@app.get("/ai/sessions/{tid}/messages")
async def get_messages(tid: str):
    """获取指定会话的历史消息"""
    return {"messages": agent.get_messages(tid)}


@app.post("/ai/knowledge/reload")
async def reload_kb():
    """手动触发知识库重建（新增/修改 .md 后调用）"""
    build_all()
    return {"ok": True}


@app.get("/health")
async def health():
    """健康检查"""
    return {"status": "ok", "pid": os.getpid()}


# ============================================================
# 启动入口：python main.py 直接运行
# ============================================================

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
