"""
Level 3 LangGraph — ReAct Agent（推理 + 行动循环）

核心概念:
  ① StateGraph — 用状态图编排多步骤工作流
  ② Nodes     — 图的节点：LLM 节点（思考）、Tool 节点（执行）
  ③ Edges     — 边：条件边（LLM 决定去 Tool 还是 END）
  ④ State     — 状态对象：携带对话历史，节点间传递
  ⑤ MemorySaver — 对话记忆：记住上下文，支持多轮对话
  ⑥ astream_events — 流式输出 Agent 每一步的执行状态

数据流:
  User → [LLM 节点] → 需要工具? → [Tool 节点] → 回到 [LLM 节点]
                  ↘ 直接回答 → END

与传统 Agent 循环对比:
  传统循环: while + if + tool_calls（手写控制流）
  LangGraph: 声明式定义图结构，框架自动调度

关键 API:
  - create_react_agent(model, tools)    → 一行创建 ReAct Agent
  - agent.invoke({"messages": [...]})   → 同步调用
  - agent.astream_events(...)           → 流式输出每一步事件
  - MemorySaver()                       → 对话状态持久化
"""

import os
from typing import AsyncIterator

from dotenv import load_dotenv
from langchain_core.messages import HumanMessage
from langchain_openai import ChatOpenAI
from langgraph.checkpoint.memory import MemorySaver
from langgraph.prebuilt import create_react_agent

from tools import TOOLS

load_dotenv()

# ============================================================
# Day 3 关键点 1: LLM + MemorySaver
# ============================================================
# MemorySaver: 在内存中保存对话状态
# 每次 agent.invoke() 后用 thread_id 区分不同会话
# 相同 thread_id 的请求会延续之前的对话上下文

checkpointer = MemorySaver()

# ============================================================
# Day 3 关键点 2: create_react_agent — 一行创建 ReAct Agent
# ============================================================
# 内部等价于:
#   StateGraph(AgentState)
#     .add_node("agent", llm_node)          ← LLM 思考 + 决定工具
#     .add_node("tools", tool_node)          ← 执行工具
#     .add_edge(START, "agent")              ← 入口
#     .add_conditional_edges("agent", ...)   ← LLM 输出判断: tool? → tools, answer? → END
#     .add_edge("tools", "agent")            ← 工具执行完回到 LLM
#   .compile(checkpointer=MemorySaver())

model = ChatOpenAI(
    model="deepseek-chat",
    base_url=os.getenv("DEEPSEEK_BASE_URL", "https://api.deepseek.com"),
    api_key=os.getenv("DEEPSEEK_API_KEY"),
    temperature=0.3,
    max_tokens=512,
)

agent = create_react_agent(
    model=model,
    tools=TOOLS,
    checkpointer=checkpointer,
    prompt=(
        "你是非遗知识助手。使用工具获取信息后回答用户问题。"
        "如果知识库中没有相关信息，如实说明。"
    ),
)


# ============================================================
# 调用接口
# ============================================================
def graph_ask(question: str, thread_id: str = "default") -> dict:
    """同步调用 LangGraph Agent

    Args:
        question:  用户问题
        thread_id: 会话ID（相同 ID 共享记忆）
    """
    config = {"configurable": {"thread_id": thread_id}}
    result = agent.invoke(
        {"messages": [HumanMessage(content=question)]},
        config=config,
    )

    # 提取最终答案（最后一条 AI 消息）
    messages = result["messages"]
    answer = ""
    tool_calls_log = []

    for msg in messages:
        # 收集所有工具调用记录
        if hasattr(msg, "tool_calls") and msg.tool_calls:
            for tc in msg.tool_calls:
                tool_calls_log.append({
                    "tool": tc["name"],
                    "args": tc["args"],
                })
        # 最后一条有 content 的 AI 消息就是最终答案
        if msg.type == "ai" and msg.content:
            answer = msg.content

    return {
        "answer": answer,
        "tool_calls": tool_calls_log,
        "thread_id": thread_id,
    }


async def graph_ask_stream(question: str, thread_id: str = "default") -> AsyncIterator[dict]:
    """流式调用 LangGraph Agent — 输出每一步事件

    astream_events 会产生多种事件类型:
      - on_chat_model_stream: LLM 逐 token 生成
      - on_tool_start:       工具开始执行
      - on_tool_end:         工具执行完成
      - on_chain_end:        Agent 完成一轮

    前端可以根据事件类型渲染不同的 UI:
      - token → 打字机效果
      - tool_start/end → 展示工具调用状态
    """
    config = {"configurable": {"thread_id": thread_id}}

    async for event in agent.astream_events(
        {"messages": [HumanMessage(content=question)]},
        config=config,
        version="v2",
    ):
        kind = event.get("event")

        # LLM 生成的 token
        if kind == "on_chat_model_stream":
            data = event.get("data", {})
            chunk = data.get("chunk")
            if chunk and chunk.content:
                yield {"type": "token", "content": chunk.content}

        # 工具开始执行
        elif kind == "on_tool_start":
            yield {
                "type": "tool_start",
                "name": event.get("name", "unknown"),
                "input": event.get("data", {}).get("input", {}),
            }

        # 工具执行完成
        elif kind == "on_tool_end":
            yield {
                "type": "tool_end",
                "name": event.get("name", "unknown"),
                "output": str(event.get("data", {}).get("output", ""))[:200],
            }
