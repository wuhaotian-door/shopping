"""
统一 Agent — 核心层

基于 LangGraph create_react_agent：
  用户提问 → LLM 推理 → 需要工具? → 调用对应工具 → 结果回传 → LLM 整理答案 → 输出

会话管理:
  - LangGraph MemorySaver 负责对话记忆（按 thread_id 隔离）
  - sessions/index.json 负责会话元数据（标题、时间）供前端列表展示
"""

import json
import logging
import os
import time
from typing import AsyncIterator

from dotenv import load_dotenv
from langchain_core.messages import HumanMessage, SystemMessage
from langchain_openai import ChatOpenAI
from langgraph.checkpoint.memory import MemorySaver
from langgraph.prebuilt import create_react_agent

from tools import ALL_TOOLS, _get_embeddings, build_all

load_dotenv()
logger = logging.getLogger("ai-server.agent")

# ---- 路径 ----
BASE = os.path.dirname(os.path.abspath(__file__))
SESSION_INDEX = os.path.join(BASE, "sessions", "index.json")


# ============================================================
# 系统提示词 — 定义 Agent 行为
# ============================================================

SYSTEM_PROMPT = """你是"码上非遗"智能助手，可以用工具搜索平台知识库。

【最高优先级】工具返回的内容就是平台真实资料。工具返回了结果就必须用来回答用户。
工具说"❌"才表示真的没找到。绝不允许工具返回了内容你却对用户说"知识库没有"！

工具选择:
1. 问历史/工艺/特点 → search_knowledge
2. 问传承人/老师 → search_inheritors
3. 问活动/体验/课程 → search_projects
4. 问商品/礼物 → search_products
5. 用户说"推荐"或不确定 → recommend_content
6. 闲聊直接回答

回答: 中文、简洁、标注来源"""


class UnifiedAgent:
    """
    统一智能助手

    初始化流程:
      1. 创建 ChatOpenAI（DeepSeek）
      2. 创建 MemorySaver（LangGraph 对话记忆）
      3. 调用 create_react_agent 组装 ReAct Agent
      4. 预加载 Embedding 模型（避免首次请求冷启动）
      5. 执行 bootstrap: MySQL → SQLite → .md → Chroma
    """

    def __init__(self):
        # ---- 1. 从环境变量读取 LLM 配置 ----
        api_key = os.getenv("DEEPSEEK_API_KEY", "")
        base_url = os.getenv("DEEPSEEK_BASE_URL", "https://api.deepseek.com")
        model = os.getenv("DEEPSEEK_MODEL", "deepseek-chat")
        temperature = float(os.getenv("LLM_TEMPERATURE", "0.4"))
        max_tokens = int(os.getenv("LLM_MAX_TOKENS", "2048"))

        logger.info(f"初始化 Agent | model={model} | base_url={base_url}")

        # ---- 2. 创建 LLM 实例（langchain-openai 兼容 DeepSeek API） ----
        self.llm = ChatOpenAI(
            model=model,
            base_url=base_url,
            api_key=api_key,
            temperature=temperature,
            max_tokens=max_tokens,
        )

        # ---- 3. LangGraph ReAct Agent ----
        # MemorySaver: 按 thread_id 记住对话上下文
        # create_react_agent: 自动处理"LLM → 工具调用 → 结果回传 → LLM"循环
        self._checkpointer = MemorySaver()
        self._agent = create_react_agent(
            model=self.llm,
            tools=ALL_TOOLS,
            checkpointer=self._checkpointer,
            prompt=SYSTEM_PROMPT,
        )

        # ---- 4. 初始化基础设施 ----
        # 4a. 预加载 Embedding 模型（首次搜索不再等 15s）
        try:
            _get_embeddings()
        except Exception as e:
            logger.warning(f"Embedding 预加载失败: {e}")

        # 4b. 从主后端 MySQL 同步数据 → 构建 Chroma 索引
        try:
            from bootstrap import bootstrap
            bootstrap()
        except Exception as e:
            logger.warning(f"数据同步失败: {e}")

        logger.info("UnifiedAgent 就绪")

    # ============================================================
    # chat — 同步问答
    # ============================================================

    def chat(self, question: str, thread_id: str = "default") -> dict:
        """
        一次性问答（非流式）

        流程:
          1. 组装消息: SystemPrompt + 用户问题
          2. Agent.invoke → LLM 推理 → 可能多次工具调用 → 最终答案
          3. 提取答案 + 工具调用记录
          4. 记录会话到 index.json

        Returns:
          {"answer": str, "tool_calls": [...], "thread_id": str}
        """
        t0 = time.time()
        logger.info(f"[{thread_id}] 问答: {question[:60]}...")

        # 调用 Agent — LangGraph 自动管理工具循环
        result = self._agent.invoke(
            {"messages": [SystemMessage(content=SYSTEM_PROMPT), HumanMessage(content=question)]},
            config={"configurable": {"thread_id": thread_id}},
        )

        # 从返回的消息列表中提取最终答案和工具调用记录
        answer, tool_calls = "", []
        for msg in result["messages"]:
            # AI 消息中的 tool_calls 记录了"LLM 决定调用哪个工具"
            if hasattr(msg, "tool_calls") and msg.tool_calls:
                for tc in msg.tool_calls:
                    tool_calls.append({"tool": tc["name"], "args": tc["args"]})
            # 最后一条 AI 消息的 content 是最终答案
            if msg.type == "ai" and msg.content:
                answer = msg.content

        # 记录到会话索引（前端侧栏展示用）
        self._record_session(thread_id, question)

        elapsed = time.time() - t0
        logger.info(
            f"[{thread_id}] 完成: {len(answer)}字, "
            f"{len(tool_calls)}工具, {elapsed:.2f}s"
        )
        return {"answer": answer, "tool_calls": tool_calls, "thread_id": thread_id}

    # ============================================================
    # chat_stream — 流式问答（SSE）
    # ============================================================

    async def chat_stream(self, question: str, thread_id: str = "default") -> AsyncIterator[dict]:
        """
        SSE 流式问答

        使用 LangGraph 的 astream_events 逐事件推送:
          - on_chat_model_stream  → LLM 生成 token → yield {"type":"token",...}
          - on_tool_start         → 工具开始执行   → yield {"type":"tool_start",...}
          - on_tool_end           → 工具执行完成   → yield {"type":"tool_end",...}

        前端 EventSource 接收事件后:
          - token      → 逐字追加到 AI 气泡
          - tool_start → 显示"🔧 正在搜索..."
        """
        t0 = time.time()
        logger.info(f"[{thread_id}] 流式: {question[:60]}...")

        answer_parts = []       # 累积 token
        tool_count = 0          # 工具调用计数
        first_token_time = None # 首 token 延迟（衡量响应速度）

        async for event in self._agent.astream_events(
            {"messages": [SystemMessage(content=SYSTEM_PROMPT), HumanMessage(content=question)]},
            config={"configurable": {"thread_id": thread_id}},
            version="v2",
        ):
            kind = event.get("event")

            # LLM 正在生成文本 → 逐 token 推送
            if kind == "on_chat_model_stream":
                chunk = event.get("data", {}).get("chunk")
                if chunk and chunk.content:
                    if first_token_time is None:
                        first_token_time = time.time() - t0
                    answer_parts.append(chunk.content)
                    yield {"type": "token", "content": chunk.content}

            # 工具开始执行 → 通知前端显示"搜索中"
            elif kind == "on_tool_start":
                tool_count += 1
                yield {
                    "type": "tool_start",
                    "name": event.get("name", "?"),
                    "input": event.get("data", {}).get("input", {}),
                }

            # 工具执行完成
            elif kind == "on_tool_end":
                yield {"type": "tool_end", "name": event.get("name", "?")}

        # 记录会话 + 打印统计
        self._record_session(thread_id, question)
        full_answer = "".join(answer_parts)
        elapsed = time.time() - t0

        logger.info(
            f"[{thread_id}] 流式完成: {len(full_answer)}字, "
            f"{tool_count}工具, 首token{first_token_time or 0:.2f}s, "
            f"总{elapsed:.2f}s"
        )
        yield {"type": "done"}

    # ============================================================
    # 会话管理（JSON 索引 — 记录标题/时间，供前端展示）
    # ============================================================

    def _load_index(self) -> dict:
        """加载会话索引文件"""
        os.makedirs(os.path.dirname(SESSION_INDEX), exist_ok=True)
        if os.path.exists(SESSION_INDEX):
            with open(SESSION_INDEX, "r", encoding="utf-8") as f:
                return json.load(f)
        return {}

    def _save_index(self, data: dict):
        """保存会话索引文件"""
        os.makedirs(os.path.dirname(SESSION_INDEX), exist_ok=True)
        with open(SESSION_INDEX, "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)

    def _record_session(self, tid: str, question: str):
        """
        每次对话后调用，记录会话元数据:
          - 首次出现 → 创建记录（标题 = 第一条问题的前30字）
          - 已存在   → 更新标题（用最新问题）
          - 超过50个  → 删除最旧的
        """
        idx = self._load_index()
        idx[tid] = {
            "title": question[:30],
            "created_at": idx.get(tid, {}).get("created_at")
            or time.strftime("%Y-%m-%d %H:%M:%S"),
        }
        # 保留最近 50 个会话
        if len(idx) > 50:
            keys = sorted(
                idx.keys(),
                key=lambda k: idx[k].get("created_at", ""),
                reverse=True,
            )
            idx = {k: idx[k] for k in keys[:50]}
        self._save_index(idx)

    def list_sessions(self) -> list:
        """列出所有会话（按创建时间倒序）"""
        idx = self._load_index()
        sessions = [
            {
                "id": k,
                "title": v.get("title", ""),
                "created_at": v.get("created_at", ""),
            }
            for k, v in idx.items()
        ]
        sessions.sort(key=lambda s: s["created_at"], reverse=True)
        return sessions

    def delete_session(self, tid: str):
        """从索引中删除会话"""
        idx = self._load_index()
        idx.pop(tid, None)
        self._save_index(idx)
        logger.info(f"会话已删除: {tid}")
