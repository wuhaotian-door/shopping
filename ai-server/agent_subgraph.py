"""
Level 3 Subgraphs — 把复杂节点封装为可复用的子图

核心概念:
  ① Subgraph = 一个独立 StateGraph，有自己的 State / Nodes / Edges
  ② 父图用 builder.add_node("tools", compiled_subgraph) 把子图当节点用
  ③ 子图内的 interrupt() 会向上传播到父图的 invoke()，审批流程正常工作
  ④ 好处: 模块化、可复用、可独立测试、关注点分离

本例:
  父图 (ReAct Agent):  llm → tools_subgraph → llm → ...
  子图 (Tool Executor): check → [request_approval(interrupt)] → execute

对比 agent_human.py:
  旧: tool_node 是一个 50+ 行的函数，审批逻辑、执行逻辑全混在一起
  新: tool_node 被拆成 3 个节点的子图，每个节点职责单一

关键点:
  - 父子图使用相同的 AgentState schema，所以能直接嵌入
  - 子图内的 interrupt() 自动传播到父图的 get_state()
  - Parent: add_node("tools", compiled_subgraph) — 子图作为节点
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
# ① State — 父图和子图共用同一 schema
# ============================================================
# 关键设计: 父子图使用相同的 AgentState
# 这样 builder.add_node("tools", compiled_subgraph) 时状态无缝传递
# 子图内部的 interrupt() 也会自动传播到父图

class AgentState(TypedDict):
    messages: Annotated[List[BaseMessage], add_messages]


# ============================================================
# ② 模型
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
# ③ 子图: Tool Executor — 3 个节点替代原来的 tool_node 函数
# ============================================================

def check_approval_node(state: AgentState) -> Dict[str, Any]:
    """子图节点 1: 检查是否有 tool_calls 以及是否需要审批

    只做检测，不做任何修改。把决策留给下一个节点。
    这就是"关注点分离"——检查逻辑和审批逻辑不在同一个函数里。
    """
    last_msg = state["messages"][-1]

    if not (hasattr(last_msg, "tool_calls") and last_msg.tool_calls):
        logger.debug("子图 check: 没有 tool_calls")
        return {"messages": []}

    names = [tc["name"] for tc in last_msg.tool_calls]
    dangerous = [n for n in names if n in APPROVAL_REQUIRED]
    logger.debug("子图 check: tool_calls=%s, 需审批=%s", names, dangerous if dangerous else "无")
    return {}  # state 不变，信息已存在于 messages 中


def request_approval_node(state: AgentState) -> Dict[str, Any]:
    """子图节点 2: 对危险工具调用 interrupt() 请求审批

    只在有需要审批的工具时才被执行（通过条件边控制）
    interrupt() 暂停执行 → 前端展示审批框 → 用户批准/拒绝
    """
    last_msg = state["messages"][-1]

    for tc in last_msg.tool_calls:
        if tc["name"] in APPROVAL_REQUIRED:
            logger.info("子图 approve: %s 需要审批，调用 interrupt()...", tc['name'])
            approval = interrupt({
                "message": f"Agent 想要执行「{tc['name']}」",
                "tool_name": tc["name"],
                "tool_args": tc["args"],
            })
            logger.debug("子图 approve: interrupt 返回: %s", approval)

            if not approval or not approval.get("approved"):
                logger.info("子图 approve: 审批未通过，跳过 %s", tc['name'])
                # 返回拒绝消息 — 注意这里直接返回 messages
                return {"messages": [ToolMessage(
                    content=f"用户拒绝了「{tc['name']}」操作。",
                    tool_call_id=tc["id"],
                )]}

    return {}  # 审批通过，继续执行


def execute_tools_node(state: AgentState) -> Dict[str, Any]:
    """子图节点 3: 执行所有工具调用并返回 ToolMessage

    前置条件: 所有需要审批的工具都已通过审批
    输出: ToolMessage 列表，被 add_messages reducer 追加到对话历史
    """
    last_msg = state["messages"][-1]
    tool_messages = []

    for tc in last_msg.tool_calls:
        tool_name = tc["name"]
        tool_args = tc["args"]

        # 跳过已处理过的（审批被拒的在前一步已替换为拒绝消息）
        tool_func = ALL_TOOLS_BY_NAME.get(tool_name)
        if not tool_func:
            tool_messages.append(ToolMessage(
                content=f"未知工具: {tool_name}",
                tool_call_id=tc["id"],
            ))
            continue

        try:
            logger.debug("子图 execute: 执行 %s(%s)", tool_name, tool_args)
            result = tool_func.invoke(tool_args)
            logger.debug("子图 execute: 结果: %s", str(result)[:100])
            tool_messages.append(ToolMessage(
                content=str(result),
                tool_call_id=tc["id"],
            ))
        except Exception as e:
            logger.error("子图 execute: 异常: %s", e, exc_info=True)
            tool_messages.append(ToolMessage(
                content=f"执行失败: {e}",
                tool_call_id=tc["id"],
            ))

    return {"messages": tool_messages}


# ============================================================
# ④ 子图的路由
# ============================================================

def subgraph_router(state: AgentState) -> str:
    """子图条件边: check 之后去哪？

    有 tool_calls 且其中有需要审批的 → approve 节点（会 interrupt）
    有 tool_calls 但都不需要审批     → execute 节点（直接执行）
    没有 tool_calls                  → END（空操作）
    """
    last_msg = state["messages"][-1]
    if not (hasattr(last_msg, "tool_calls") and last_msg.tool_calls):
        return END

    has_dangerous = any(tc["name"] in APPROVAL_REQUIRED for tc in last_msg.tool_calls)
    if has_dangerous:
        return "approve"
    return "execute"


# ============================================================
# ⑤ 组装子图
# ============================================================
subgraph_builder = StateGraph(AgentState)

subgraph_builder.add_node("check", check_approval_node)
subgraph_builder.add_node("approve", request_approval_node)
subgraph_builder.add_node("execute", execute_tools_node)

subgraph_builder.add_edge(START, "check")
subgraph_builder.add_conditional_edges(
    "check", subgraph_router,
    {"approve": "approve", "execute": "execute", END: END}
)
subgraph_builder.add_edge("approve", "execute")
subgraph_builder.add_edge("execute", END)

# ★ compile() 返回 CompiledGraph — 可作为另一个图的节点
tool_exec_subgraph = subgraph_builder.compile()


# ============================================================
# ⑥ 父图: ReAct Agent — 子图直接作为 tools 节点
# ============================================================

def llm_node(state: AgentState) -> Dict[str, Any]:
    """父图 LLM 节点 — 和 agent_human.py 完全一样"""
    messages = state["messages"]
    if not messages or not isinstance(messages[0], SystemMessage):
        messages = [SYSTEM_MSG] + list(messages)
    response = model_with_tools.invoke(messages)
    return {"messages": [response]}


def parent_router(state: AgentState) -> str:
    """父图条件边: LLM 输出决定下一步"""
    last_msg = state["messages"][-1]
    if hasattr(last_msg, "tool_calls") and last_msg.tool_calls:
        return "tools"
    return END


# 组装父图
checkpointer = MemorySaver()

parent_builder = StateGraph(AgentState)
parent_builder.add_node("llm", llm_node)

# ★★★ 关键: 子图直接作为工具节点 ★★★
# 对比 agent_human.py 的 builder.add_node("tools", tool_node) ← 普通函数
# 这里 add_node("tools", tool_exec_subgraph) ← 编译后的子图
# 子图内的 interrupt() 自动传播到父图
parent_builder.add_node("tools", tool_exec_subgraph)

parent_builder.add_edge(START, "llm")
parent_builder.add_conditional_edges("llm", parent_router)
parent_builder.add_edge("tools", "llm")

agent_with_subgraph = parent_builder.compile(checkpointer=checkpointer)


# ============================================================
# ⑦ 调用接口 — 和 agent_human.py 完全相同的 API
# ============================================================

def subgraph_agent_start(question: str, thread_id: str = "default") -> dict:
    """启动 Subgraph Agent

    和 human_agent_start 相同: invoke() → get_state() 检查中断
    子图内的 interrupt() 会传播到父图，get_state() 能检测到
    """
    config = {"configurable": {"thread_id": thread_id}}

    logger.info("subgraph_agent_start: thread_id=%s, question=%.60s...", thread_id, question)
    agent_with_subgraph.invoke(
        {"messages": [HumanMessage(content=question)]},
        config=config,
    )
    logger.debug("subgraph_agent_start: invoke 返回")

    state = agent_with_subgraph.get_state(config)
    logger.debug("subgraph_agent_start: state.interrupts = %s",
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
        logger.info("subgraph_agent_start: 返回 interrupted，%d 个中断", len(interrupts_data))
        return {
            "status": "interrupted",
            "thread_id": thread_id,
            "interrupts": interrupts_data,
        }

    # 无 interrupt，提取最终答案
    logger.info("subgraph_agent_start: 无 interrupt，正常完成")
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


def subgraph_agent_resume(thread_id: str, approved: bool) -> dict:
    """审批后恢复 Subgraph Agent

    Command(resume=...) → 子图内的 interrupt() 收到值 → 继续执行
    """
    config = {"configurable": {"thread_id": thread_id}}

    result = agent_with_subgraph.invoke(
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


def subgraph_agent_get_state(thread_id: str) -> dict:
    """查询 Agent 当前状态"""
    config = {"configurable": {"thread_id": thread_id}}
    state = agent_with_subgraph.get_state(config)

    if state is None:
        return {"status": "not_found", "thread_id": thread_id}

    return {
        "status": "interrupted" if state.interrupts else "idle",
        "thread_id": thread_id,
        "interrupts": str(state.interrupts) if state.interrupts else None,
        "message_count": len(state.values.get("messages", [])) if state.values else 0,
    }
