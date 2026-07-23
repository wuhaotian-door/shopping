"""
码上非遗 AI 服务 — FastAPI 入口

启动: python main.py
"""

import json
import os
import time

from dotenv import load_dotenv
from fastapi import FastAPI, Request
from fastapi.responses import FileResponse, StreamingResponse, JSONResponse
from pydantic import BaseModel

load_dotenv()

from core.agent.UnifiedAgent import UnifiedAgent
from core.tool.knowledge_base import build_all
from utils.logger import get_logger

logger = get_logger(__name__)

# ============================================================
app = FastAPI(title="码上非遗 AI 服务")
logger.info(f"进程 PID: {os.getpid()}")

agent = UnifiedAgent()
logger.info("服务就绪")

# ============================================================
@app.middleware("http")
async def log_req(req: Request, call_next):
    t0 = time.time(); r = await call_next(req)
    logger.info(f"{req.method} {req.url.path} → {r.status_code} ({time.time()-t0:.2f}s)")
    return r

@app.exception_handler(Exception)
async def on_err(req: Request, exc: Exception):
    logger.error(f"[{req.method} {req.url.path}] {exc}", exc_info=True)
    return JSONResponse(status_code=500, content={"error": str(exc)})

# ============================================================
class ChatReq(BaseModel):
    question: str
    thread_id: str = "default"

@app.get("/")
async def root():
    p = os.path.join(os.path.dirname(__file__), "static", "index.html")
    return FileResponse(p) if os.path.exists(p) else {"msg": "AI 服务运行中"}

@app.post("/ai/chat")
async def chat(req: ChatReq):
    return agent.chat(req.question, req.thread_id)

@app.post("/ai/chat/stream")
async def chat_stream(req: ChatReq):
    async def gen():
        async for e in agent.chat_stream(req.question, req.thread_id):
            yield f"data: {json.dumps(e, ensure_ascii=False)}\n\n"
        yield "data: [DONE]\n\n"
    return StreamingResponse(gen(), media_type="text/event-stream",
                             headers={"Cache-Control": "no-cache", "X-Accel-Buffering": "no"})

@app.get("/ai/sessions")
async def list_sessions():
    return {"sessions": agent.list_sessions()}

@app.delete("/ai/sessions/{tid}")
async def del_session(tid: str):
    agent.delete_session(tid)
    return {"ok": True}

@app.post("/ai/knowledge/reload")
async def reload_kb():
    build_all()
    return {"ok": True}

@app.get("/health")
async def health():
    return {"status": "ok", "pid": os.getpid()}

# ============================================================
if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
