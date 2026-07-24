"""
Level 3 Human-in-the-loop — 关键操作暂停，人工审批后才执行

核心概念:
  ① interrupt()  — 图节点内暂停执行，等待外部指令
  ② Command(resume=...) — 外部传入审批结果，恢复执行
  ③ checkpointer  — 暂停期间状态持久化，不丢失上下文

数据流:
  用户: "把苏绣的资料加到知识库"
    → LLM 决定调用 add_knowledge("苏绣", "...")
    → tools 节点检查: add_knowledge 需要审批
    → interrupt() 暂停
    → 前端展示审批框 → 用户点"批准"
    → Command(resume={"approved": True}) → 工具执行 → LLM → 答案
"""

import os as _os
from typing import Annotated, Any, Dict, List

from dotenv import load_dotenv
from langchain_core.messages import (AIMessage, BaseMessage, HumanMessage,
                                      SystemMessage, ToolMessage)
from langchain_openai import ChatOpenAI
from langgraph.checkpoint.memory import MemorySaver
from langgraph.graph import END, START, StateGraph
from langgraph.graph.message import add_messages
from langgraph.types import Command, interrupt
from typing_extensions import TypedDict

from tools import ALL_TOOLS, ALL_TOOLS_BY_NAME, APPROVAL_REQUIRED
from logger_config import get_logger

logger = get_logger(__name__)

load_dotenv()

# ============================================================
# ① 定义状态 — 关键: add_messages reducer 自动追加消息
# ============================================================
class AgentState(TypedDict):
    messages: Annotated[List[BaseMessage], add_messages]
    # add_messages: 节点返回 {"messages": [msg]} 时自动追加到列表末尾
    # 而不是覆盖整个 messages 列表

# ============================================================
# ② 带工具的模型
# ============================================================
model = ChatOpenAI(
    model="deepseek-chat",
    base_url=_os.getenv("DEEPSEEK_BASE_URL", "https://api.deepseek.com"),
    api_key=_os.getenv("DEEPSEEK_API_KEY"),
    temperature=0.3, max_tokens=512,
)

SYSTEM_MSG = SystemMessage(content=(
    "你是非遗知识助手。使用工具获取信息后回答。"
    "当用户要求记录、保存或添加信息时，使用 add_knowledge。"
))

model_with_tools = model.bind_tools(ALL_TOOLS)

# ============================================================
# ③ 图的节点
# ============================================================

def llm_node(state: AgentState) -> Dict[str, Any]:
    """LLM 节点: 思考 + 决定是否调工具"""
    messages = state["messages"]

    # 只在消息列表为空或首条不是 system 时添加 system prompt
    if not messages or not isinstance(messages[0], SystemMessage):
        messages = [SYSTEM_MSG] + list(messages)

    response = model_with_tools.invoke(messages)
    return {"messages": [response]}


def tool_node(state: AgentState) -> Dict[str, Any]:
    """工具节点: 执行工具，执行前检查是否需要审批"""
    last_msg = state["messages"][-1]

    if not (hasattr(last_msg, "tool_calls") and last_msg.tool_calls):
        logger.debug("tool_node: 没有 tool_calls，跳过")
        return {"messages": []}

    logger.debug("tool_node: tool_calls=%s", [tc['name'] for tc in last_msg.tool_calls])

    tool_messages = []

    for tc in last_msg.tool_calls:
        # ========================================================
        # ④ interrupt() — 需要审批的工具在此暂停
        # ========================================================
        if tc["name"] in APPROVAL_REQUIRED:
            logger.info("tool_node: %s 需要审批，调用 interrupt()...", tc['name'])
            approval = interrupt({
                "message": f"Agent 想要执行「{tc['name']}」",
                "tool_name": tc["name"],
                "tool_args": tc["args"],
            })
            logger.debug("tool_node: interrupt 返回: %s", approval)

            if not approval or not approval.get("approved"):
                logger.info("tool_node: 审批未通过，跳过 %s", tc['name'])
                tool_messages.append(ToolMessage(
                    content=f"用户拒绝了「{tc['name']}」操作。",
                    tool_call_id=tc["id"],
                ))
                continue

        # 审批通过（或无需审批），执行工具
        logger.debug("tool_node: 执行工具 %s", tc['name'])
        tool_func = ALL_TOOLS_BY_NAME.get(tc["name"])
        if tool_func:
            try:
                result = tool_func.invoke(tc["args"])
                logger.debug("tool_node: 工具结果: %s", str(result)[:100])
            except Exception as e:
                result = f"工具执行失败: {e}"
                logger.error("tool_node: 工具异常: %s", e, exc_info=True)
            tool_messages.append(ToolMessage(
                content=str(result), tool_call_id=tc["id"]
            ))
        else:
            logger.warning("tool_node: 未知工具 %s", tc['name'])
            tool_messages.append(ToolMessage(
                content=f"未知工具: {tc['name']}", tool_call_id=tc["id"]
            ))

    return {"messages": tool_messages}


def router(state: AgentState) -> str:
    """⑤ 条件边: 根据 LLM 输出决定下一步"""
    last_msg = state["messages"][-1]
    if hasattr(last_msg, "tool_calls") and last_msg.tool_calls:
        return "tools"
    return END

# ============================================================
# ⑥ 组装图
# ============================================================
checkpointer = MemorySaver()

builder = StateGraph(AgentState)
builder.add_node("llm", llm_node)
builder.add_node("tools", tool_node)

builder.add_edge(START, "llm")
builder.add_conditional_edges("llm", router)
builder.add_edge("tools", "llm")

agent_with_interrupt = builder.compile(checkpointer=checkpointer)

# ============================================================
# 调用接口
# ============================================================

def human_agent_start(question: str, thread_id: str = "default") -> dict:
    """启动 Human-in-the-loop Agent

    LangGraph 1.2.x 中，interrupt() 在 invoke() 内不会抛异常，
    而是保存状态后正常返回。需要 invoke() 后检查 get_state()。
    """
    config = {"configurable": {"thread_id": thread_id}}

    logger.info("human_agent_start: thread_id=%s, question=%.60s...", thread_id, question)
    agent_with_interrupt.invoke(
        {"messages": [HumanMessage(content=question)]},
        config=config,
    )
    logger.debug("human_agent_start: invoke 返回")

    # 检查是否被 interrupt() 暂停
    state = agent_with_interrupt.get_state(config)
    logger.debug("human_agent_start: state.interrupts = %s",
                 state.interrupts if state else None)

    if state and state.interrupts:
        interrupts_data = []
        for iv in state.interrupts:
            if hasattr(iv, 'value'):
                interrupts_data.append(iv.value)
            elif isinstance(iv, dict):
                interrupts_data.append(iv.get('value', iv))
            else:
                interrupts_data.append(iv)
        logger.info("human_agent_start: 返回 interrupted，%d 个中断", len(interrupts_data))
        return {
            "status": "interrupted",
            "thread_id": thread_id,
            "interrupts": interrupts_data,
        }

    # 没触发 interrupt()，正常完成，从 state 提取最终答案
    logger.info("human_agent_start: 无 interrupt，正常完成")
    answer = ""
    tool_calls_log = []
    messages = state.values.get("messages", []) if state and state.values else []
    for msg in messages:
        if hasattr(msg, "tool_calls") and msg.tool_calls:
            for tc in msg.tool_calls:
                tool_calls_log.append({"tool": tc["name"], "args": tc["args"]})
        if isinstance(msg, AIMessage) and msg.content:
            answer = msg.content

    return {
        "status": "done",
        "answer": answer,
        "tool_calls": tool_calls_log,
        "thread_id": thread_id,
    }


def human_agent_resume(thread_id: str, approved: bool) -> dict:
    """审批后恢复 Agent 执行

    ⑦ Command(resume=...) — 把审批结果传回给 interrupt()
    """
    config = {"configurable": {"thread_id": thread_id}}

    result = agent_with_interrupt.invoke(
        Command(resume={"approved": approved}),
        config=config,
    )

    answer = ""
    tool_calls_log = []
    for msg in result["messages"]:
        if hasattr(msg, "tool_calls") and msg.tool_calls:
            for tc in msg.tool_calls:
                tool_calls_log.append({"tool": tc["name"], "args": tc["args"]})
        if isinstance(msg, AIMessage) and msg.content:
            answer = msg.content

    return {
        "status": "done",
        "answer": answer,
        "tool_calls": tool_calls_log,
        "thread_id": thread_id,
    }


def human_agent_get_state(thread_id: str) -> dict:
    """查询 Agent 当前状态"""
    config = {"configurable": {"thread_id": thread_id}}
    state = agent_with_interrupt.get_state(config)

    if state is None:
        return {"status": "not_found", "thread_id": thread_id}

    return {
        "status": "interrupted" if state.interrupts else "idle",
        "thread_id": thread_id,
        "interrupts": state.interrupts,
        "message_count": len(state.values.get("messages", [])) if state.values else 0,
    }
