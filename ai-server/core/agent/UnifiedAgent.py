"""
统一 Agent (UnifiedAgent)

一个 Agent + 一组工具。用户说什么，LLM 自主判断该调哪个工具。
基于 LangGraph create_react_agent + MemorySaver。
"""

import json
import os
import time
from typing import AsyncIterator, Dict, List

from dotenv import load_dotenv
from langchain_core.messages import AIMessage, HumanMessage, SystemMessage
from langgraph.checkpoint.memory import MemorySaver
from langgraph.prebuilt import create_react_agent

from core.llm.factory import create_llm
from core.tool.ToolManager import ToolManager
from core.tool.agent_tools import ALL_TOOLS
from config.env import get_config
from utils.logger import get_logger

load_dotenv()
logger = get_logger(__name__)
logger.debug("模块加载: core.agent.UnifiedAgent")

# 会话文件目录
SESSIONS_DIR = os.path.join(
    os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))),
    "sessions",
)

SYSTEM_PROMPT = """你是"码上非遗"智能助手，可以用工具搜索平台知识库。

【最高优先级】工具返回的内容就是平台真实资料。工具返回了结果就必须用来回答用户。
工具说"未找到"才能告诉用户"平台暂无收录"。绝不允许工具返回了内容你却对用户说"知识库没有"！

工具选择：
1. 问历史/工艺/特点 → search_knowledge
2. 问传承人/老师 → search_inheritors
3. 问活动/体验/课程 → search_projects
4. 问商品/礼物 → search_products
5. 用户说"推荐"或不确定 → recommend_content
6. 闲聊直接回答

回答：中文、简洁、标注来源如[来源1]"""


class UnifiedAgent:
    """统一智能助手

    初始化顺序: LLM → ToolManager → LangGraph Agent → 知识库
    """

    def __init__(self):
        logger.info("=" * 40)
        logger.info("UnifiedAgent 初始化开始...")

        api_key = os.getenv("DEEPSEEK_API_KEY", "")
        base_url = os.getenv("DEEPSEEK_BASE_URL", "https://api.deepseek.com")

        # ---- 1. LLM ----
        logger.info("--- 初始化 LLM ---")
        self.llm = create_llm(base_url=base_url, api_key=api_key)

        # ---- 2. 工具管理器 ----
        logger.info("--- 初始化工具管理器 ---")
        self.tool_mgr = ToolManager()
        for t in ALL_TOOLS:
            self.tool_mgr.register(t)
        logger.info(f"工具管理器就绪: {len(self.tool_mgr.list_tools())}个工具")

        # ---- 3. LangGraph Agent ----
        logger.info("--- 初始化 LangGraph Agent ---")
        t0 = time.time()
        self._checkpointer = MemorySaver()
        self._agent = create_react_agent(
            model=self.llm.raw,
            tools=ALL_TOOLS,
            checkpointer=self._checkpointer,
            prompt=SYSTEM_PROMPT,
        )
        logger.info(f"LangGraph Agent 创建完成 | {time.time()-t0:.2f}s")

        # ---- 4. 知识库（从主后端 MySQL 同步数据 → SQLite → RAG） ----
        logger.info("--- 数据同步 & 知识库构建 ---")
        try:
            from data.bootstrap import bootstrap
            cfg = get_config()
            bootstrap(cfg.get("mysql", {}))
        except Exception as e:
            logger.error(f"数据同步失败: {e}", exc_info=True)

        # ---- 5. 预加载 Embedding 模型 ----
        logger.info("--- 预加载 Embedding 模型 ---")
        try:
            from core.tool.knowledge_base import _get_embeddings
            _get_embeddings()
            logger.info("Embedding 模型预加载完成")
        except Exception as e:
            logger.error(f"Embedding 预加载失败: {e}", exc_info=True)

        # ---- 6. 会话目录 ----
        os.makedirs(SESSIONS_DIR, exist_ok=True)
        logger.info(f"会话存储目录: {SESSIONS_DIR}")

        logger.info("UnifiedAgent 初始化完成")
        logger.info("=" * 40)

    # ============================================================
    # 会话持久化
    # ============================================================

    def _session_path(self, tid: str) -> str:
        return os.path.join(SESSIONS_DIR, f"{tid}.json")

    def _load_session(self, tid: str) -> dict:
        """加载会话，不存在则返回新会话"""
        path = self._session_path(tid)
        if os.path.exists(path):
            try:
                with open(path, "r", encoding="utf-8") as f:
                    s = json.load(f)
                logger.debug(
                    f"加载会话: {tid} | {len(s.get('messages',[]))}条消息 | "
                    f"标题='{s.get('title','')}'"
                )
                return s
            except Exception as e:
                logger.error(f"加载会话失败 {tid}: {e}", exc_info=True)

        logger.debug(f"创建新会话: {tid}")
        return {
            "id": tid,
            "title": "新会话",
            "created_at": time.strftime("%Y-%m-%d %H:%M:%S"),
            "messages": [],
        }

    def _save_session(self, s: dict):
        """保存会话到文件"""
        path = self._session_path(s["id"])
        try:
            with open(path, "w", encoding="utf-8") as f:
                json.dump(s, f, ensure_ascii=False, indent=2)
            logger.debug(
                f"保存会话: {s['id']} | {len(s.get('messages',[]))}条消息"
            )
        except Exception as e:
            logger.error(f"保存会话失败 {s['id']}: {e}", exc_info=True)

    def _append_msg(self, tid: str, role: str, content: str):
        """追加消息到会话"""
        s = self._load_session(tid)

        # 第一条用户消息作为会话标题
        if s["title"] == "新会话" and role == "user":
            s["title"] = content[:30] + ("…" if len(content) > 30 else "")
            logger.debug(f"会话{tid}标题更新: '{s['title']}'")

        s["messages"].append({"role": role, "content": content})
        self._save_session(s)
        logger.debug(
            f"消息追加: {tid} | role={role} | {len(content)}字符 | "
            f"会话共{len(s['messages'])}条消息"
        )

    def list_sessions(self) -> list:
        """列出所有会话"""
        os.makedirs(SESSIONS_DIR, exist_ok=True)
        sessions = []
        file_count = 0
        for f in sorted(os.listdir(SESSIONS_DIR), reverse=True):
            if not f.endswith(".json"):
                continue
            file_count += 1
            try:
                with open(os.path.join(SESSIONS_DIR, f), encoding="utf-8") as fp:
                    s = json.load(fp)
                sessions.append({
                    "id": s["id"],
                    "title": s.get("title", ""),
                    "created_at": s.get("created_at", ""),
                    "msg_count": len(s.get("messages", [])),
                })
            except Exception as e:
                logger.error(f"读取会话文件 {f} 失败: {e}")

        logger.info(
            f"list_sessions: {len(sessions)}个会话 "
            f"(扫描{file_count}个文件)"
        )
        return sessions

    def delete_session(self, tid: str):
        """删除会话"""
        path = self._session_path(tid)
        if os.path.exists(path):
            os.remove(path)
            logger.info(f"会话已删除: {tid}")
        else:
            logger.warning(f"删除会话失败: {tid} 不存在")

    # ============================================================
    # 对话
    # ============================================================

    def chat(self, question: str, thread_id: str = "default") -> dict:
        """同步问答"""
        t0 = time.time()
        logger.info(f"[{thread_id}] === 问答开始 ===")
        logger.info(f"[{thread_id}] 问题: {question[:100]}...")

        # 加载历史
        session = self._load_session(thread_id)
        history = []
        for m in session.get("messages", [])[-10:]:
            if m["role"] == "user":
                history.append(HumanMessage(content=m["content"]))
            else:
                history.append(AIMessage(content=m["content"]))

        total_msg_count = len(history) + 2  # system + user
        logger.info(
            f"[{thread_id}] 上下文: {len(history)}条历史消息 | "
            f"总消息{total_msg_count}条"
        )

        # 组装消息
        messages = (
            [SystemMessage(content=SYSTEM_PROMPT)]
            + history
            + [HumanMessage(content=question)]
        )

        # 调用 Agent
        logger.info(f"[{thread_id}] 调用 Agent...")
        t_agent = time.time()
        try:
            result = self._agent.invoke(
                {"messages": messages},
                config={"configurable": {"thread_id": thread_id}},
            )
            agent_time = time.time() - t_agent
        except Exception as e:
            agent_time = time.time() - t_agent
            logger.error(
                f"[{thread_id}] Agent 调用失败 | "
                f"{agent_time:.2f}s | {type(e).__name__}: {e}",
                exc_info=True,
            )
            raise

        # 提取答案
        answer = ""
        tool_calls = []
        msg_count = len(result.get("messages", []))

        for msg in result["messages"]:
            if hasattr(msg, "tool_calls") and msg.tool_calls:
                for tc in msg.tool_calls:
                    tool_calls.append({
                        "tool": tc["name"],
                        "args": tc["args"],
                    })
            if msg.type == "ai" and msg.content:
                answer = msg.content

        # 持久化
        self._append_msg(thread_id, "user", question)
        self._append_msg(thread_id, "assistant", answer)

        elapsed = time.time() - t0
        logger.info(
            f"[{thread_id}] === 问答完成 === | "
            f"{len(answer)}字符 | {len(tool_calls)}次工具调用 | "
            f"Agent耗时{agent_time:.2f}s | 总耗时{elapsed:.2f}s | "
            f"Agent返回{msg_count}条消息"
        )
        return {
            "answer": answer,
            "tool_calls": tool_calls,
            "thread_id": thread_id,
        }

    async def chat_stream(
        self, question: str, thread_id: str = "default"
    ) -> AsyncIterator[dict]:
        """流式问答（SSE）"""
        t0 = time.time()
        logger.info(f"[{thread_id}] === 流式开始 ===")
        logger.info(f"[{thread_id}] 问题: {question[:100]}...")

        # 加载历史
        session = self._load_session(thread_id)
        history = []
        for m in session.get("messages", [])[-10:]:
            if m["role"] == "user":
                history.append(HumanMessage(content=m["content"]))
            else:
                history.append(AIMessage(content=m["content"]))

        logger.info(f"[{thread_id}] 上下文: {len(history)}条历史")

        messages = (
            [SystemMessage(content=SYSTEM_PROMPT)]
            + history
            + [HumanMessage(content=question)]
        )

        answer_parts = []
        tool_count = 0
        token_count = 0
        first_token_time = None

        try:
            async for event in self._agent.astream_events(
                {"messages": messages},
                config={"configurable": {"thread_id": thread_id}},
                version="v2",
            ):
                kind = event.get("event")

                if kind == "on_chat_model_stream":
                    chunk = event.get("data", {}).get("chunk")
                    if chunk and chunk.content:
                        token_count += 1
                        if first_token_time is None:
                            first_token_time = time.time() - t0
                        answer_parts.append(chunk.content)
                        yield {"type": "token", "content": chunk.content}

                elif kind == "on_tool_start":
                    tool_count += 1
                    name = event.get("name", "?")
                    inp = event.get("data", {}).get("input", {})
                    logger.info(
                        f"[{thread_id}] 流式工具[{tool_count}]: {name} | "
                        f"参数: {str(inp)[:80]}..."
                    )
                    yield {
                        "type": "tool_start",
                        "name": name,
                        "input": inp,
                    }

                elif kind == "on_tool_end":
                    name = event.get("name", "?")
                    logger.debug(f"[{thread_id}] 流式工具[{tool_count}]完成: {name}")
                    yield {"type": "tool_end", "name": name}

        except Exception as e:
            elapsed = time.time() - t0
            logger.error(
                f"[{thread_id}] 流式中断 | "
                f"已生成{token_count}tokens | "
                f"{elapsed:.2f}s | {type(e).__name__}: {e}",
                exc_info=True,
            )
            raise

        full = "".join(answer_parts)
        self._append_msg(thread_id, "user", question)
        self._append_msg(thread_id, "assistant", full)

        elapsed = time.time() - t0
        ttft = first_token_time or 0
        logger.info(
            f"[{thread_id}] === 流式完成 === | "
            f"{len(full)}字符({token_count}tokens) | "
            f"{tool_count}次工具调用 | "
            f"首token{ttft:.2f}s | 总耗时{elapsed:.2f}s"
        )
        yield {"type": "done"}
