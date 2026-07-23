"""
Level 5 Multi-Agent — Supervisor + Workers 多智能体协作

核心理念:
  一个大 Agent → 拆成多个小 Agent，各司其职。
  每个 Agent 只有自己需要的工具和 prompt，出错概率更低。
  加新能力只需加新 Agent，不改现有逻辑。

架构:
  Supervisor (调度员, 不带工具)
    ├→ Researcher (研究员, search_knowledge_base + list_documents)
    └→ Writer    (创作者, add_knowledge + 内容写作)

流程:
  User → Supervisor → "查询资料" → Researcher → 查完 → Supervisor
                    → "保存内容" → Writer    → 写完 → Supervisor
                    → "可以直接回答" → END

三个角色对比:
  |            | 工具                          | 职责          |
  | Supervisor | 无                            | 路由判断      |
  | Researcher | search_knowledge, list_docs   | 查 + 整理信息 |
  | Writer     | add_knowledge                 | 写 + 保存内容 |

关键 API:
  - 每个 Worker 是独立 StateGraph，add_node 嵌入父图
  - State 扩展 next 字段做路由
  - Supervisor 输出路由决策 → 条件边分发
"""

import os as _os
from typing import Annotated, Any, Dict, List, Literal

from dotenv import load_dotenv
from langchain_core.messages import (AIMessage, BaseMessage, HumanMessage,
                                      SystemMessage, ToolMessage)
from langchain_openai import ChatOpenAI
from langgraph.checkpoint.memory import MemorySaver
from langgraph.graph import END, START, StateGraph
from langgraph.graph.message import add_messages
from langgraph.types import Command, interrupt
from typing_extensions import TypedDict

from tools import (ALL_TOOLS, ALL_TOOLS_BY_NAME, TOOLS,
                   APPROVAL_REQUIRED, search_knowledge_base, list_documents,
                   add_knowledge)

load_dotenv()

# ============================================================
# ① 扩展的 State — 加 next 字段做 Agent 间路由
# ============================================================
class MultiAgentState(TypedDict):
    messages: Annotated[List[BaseMessage], add_messages]
    next: str   # "researcher" | "writer" | "finish" — supervisor 设置


# ============================================================
# ② Supervisor — 不带工具，只做路由判断
# ============================================================
supervisor_model = ChatOpenAI(
    model="deepseek-chat",
    base_url=_os.getenv("DEEPSEEK_BASE_URL", "https://api.deepseek.com"),
    api_key=_os.getenv("DEEPSEEK_API_KEY"),
    temperature=0,
    max_tokens=128,
)

SUPERVISOR_PROMPT = """你是任务调度员。根据用户请求，决定由哪个角色处理：

可选项:
- researcher — 需要查询、搜索、列出非遗资料。例如"紫砂壶是什么""知识库有哪些文档"
- writer    — 需要保存、添加、创建非遗内容。例如"帮我保存一下""把这个信息记录进去"
- FINISH    — 可以直接回答，或上一个角色已经完成任务

规则:
1. 如果用户问具体知识/资料，派给 researcher
2. 如果用户说"保存""添加""记录""新建"，派给 writer
3. 如果 researcher 或 writer 已经完成了任务，回复 FINISH
4. 只回复角色名称，不要解释"""


def supervisor_node(state: MultiAgentState) -> Dict[str, Any]:
    """调度节点: 读消息，决定下一站"""
    messages = state["messages"]

    # 构建 supervisor 的消息列表（加 system prompt）
    supervisor_messages = [
        SystemMessage(content=SUPERVISOR_PROMPT),
        *messages,
    ]
    response = supervisor_model.invoke(supervisor_messages)
    decision = response.content.strip().lower()

    print(f"[Supervisor] 路由决策: '{decision}'")

    # 解析决策
    if "researcher" in decision:
        next_agent = "researcher"
        # supervisor 只做路由，不把决策消息加进对话
        return {"next": next_agent}
    elif "writer" in decision:
        next_agent = "writer"
        return {"next": next_agent}
    else:
        # FINISH: 把最终答案加入对话
        return {"next": "finish"}


# ============================================================
# ③ Researcher — 带搜索工具，管"查"
# ============================================================
researcher_model = ChatOpenAI(
    model="deepseek-chat",
    base_url=_os.getenv("DEEPSEEK_BASE_URL", "https://api.deepseek.com"),
    api_key=_os.getenv("DEEPSEEK_API_KEY"),
    temperature=0.3,
    max_tokens=512,
)

RESEARCHER_PROMPT = """你是非遗研究员，负责查询和整理非遗资料。

工具:
- search_knowledge_base: 搜索非遗知识库
- list_documents: 列出知识库中的所有文档

规则:
1. 根据用户问题选择合适的工具
2. 查到信息后，用简洁的语言整理回答
3. 标注信息来源
4. 如果知识库没有，如实告知"""

researcher_with_tools = researcher_model.bind_tools(TOOLS)


def researcher_node(state: MultiAgentState) -> Dict[str, Any]:
    """研究员节点: 完整的 ReAct 循环（思考 → 工具 → 总结）"""
    messages = state["messages"]
    sys = SystemMessage(content=RESEARCHER_PROMPT)

    # 第 1 轮: LLM 思考 + 决定是否调工具
    response1 = researcher_with_tools.invoke([sys, *messages])
    print(f"[Researcher] 第 1 轮: tool_calls={bool(response1.tool_calls)}, "
          f"content={response1.content[:80] if response1.content else '(空)'}")

    # 如果没有工具调用，直接返回
    if not response1.tool_calls:
        return {"messages": [response1], "next": "supervisor"}

    # 第 2 轮: 执行工具 + LLM 再总结
    tool_msgs = []
    for tc in response1.tool_calls:
        tool_func = TOOLS.get(tc["name"]) if isinstance(TOOLS, dict) else \
                    {t.name: t for t in TOOLS}.get(tc["name"])
        if tool_func:
            result = tool_func.invoke(tc["args"])
            tool_msgs.append(ToolMessage(content=str(result), tool_call_id=tc["id"]))

    response2 = researcher_with_tools.invoke(
        [sys, *messages, response1, *tool_msgs]
    )

    return {
        "messages": [response1, *tool_msgs, response2],
        "next": "supervisor",
    }


# ============================================================
# ④ Writer — 带 add_knowledge，管"写"
# ============================================================
writer_model = ChatOpenAI(
    model="deepseek-chat",
    base_url=_os.getenv("DEEPSEEK_BASE_URL", "https://api.deepseek.com"),
    api_key=_os.getenv("DEEPSEEK_API_KEY"),
    temperature=0.5,
    max_tokens=1024,
)

WRITER_PROMPT = """你是非遗内容创作者，负责撰写和保存非遗知识。

工具:
- add_knowledge(title, content): 往知识库添加新文档

规则:
1. 当用户要求保存/记录/添加内容时，整理内容后用 add_knowledge 保存
2. 标题简洁，内容结构清晰
3. 如果用户只是问问题（不涉及保存），告知这不是你的职责范围
4. 对于已由 researcher 查到的信息，可以帮忙整理润色后保存"""

writer_model_with_tools = writer_model.bind_tools([add_knowledge])


def writer_node(state: MultiAgentState) -> Dict[str, Any]:
    """创作者节点: 写作 + 保存"""
    messages = state["messages"]
    sys = SystemMessage(content=WRITER_PROMPT)

    # 第 1 轮: LLM 思考
    response1 = writer_model_with_tools.invoke([sys, *messages])
    print(f"[Writer] 第 1 轮: tool_calls={bool(response1.tool_calls)}, "
          f"content={response1.content[:80] if response1.content else '(空)'}")

    if not response1.tool_calls:
        return {"messages": [response1], "next": "supervisor"}

    # 第 2 轮: 执行工具
    tool_msgs = []
    for tc in response1.tool_calls:
        if tc["name"] == "add_knowledge":
            # add_knowledge 需要审批，这里用 interrupt
            print(f"[Writer] add_knowledge 需要审批: {tc['args']}")
            approval = interrupt({
                "message": "Writer Agent 想要保存新文档",
                "tool_name": tc["name"],
                "tool_args": tc["args"],
            })
            if not approval or not approval.get("approved"):
                tool_msgs.append(ToolMessage(
                    content=f"用户拒绝了保存操作。",
                    tool_call_id=tc["id"],
                ))
                continue

        tool_func = {t.name: t for t in [add_knowledge]}.get(tc["name"])
        if tool_func:
            result = tool_func.invoke(tc["args"])
            tool_msgs.append(ToolMessage(content=str(result), tool_call_id=tc["id"]))

    response2 = writer_model_with_tools.invoke(
        [sys, *messages, response1, *tool_msgs]
    )

    return {
        "messages": [response1, *tool_msgs, response2],
        "next": "supervisor",
    }


# ============================================================
# ⑤ 路由 + 组装
# ============================================================

def router(state: MultiAgentState) -> Literal["researcher", "writer", "__end__"]:
    """根据 supervisor 设置的 next 字段路由"""
    nxt = state.get("next", "finish")
    if nxt == "researcher":
        return "researcher"
    elif nxt == "writer":
        return "writer"
    else:
        return END


builder = StateGraph(MultiAgentState)

builder.add_node("supervisor", supervisor_node)
builder.add_node("researcher", researcher_node)
builder.add_node("writer", writer_node)

builder.add_edge(START, "supervisor")
builder.add_conditional_edges("supervisor", router, {
    "researcher": "researcher",
    "writer": "writer",
    END: END,
})
builder.add_edge("researcher", "supervisor")
builder.add_edge("writer", "supervisor")

checkpointer = MemorySaver()
multi_agent = builder.compile(checkpointer=checkpointer)


# ============================================================
# ⑥ 调用接口
# ============================================================

def multi_agent_start(question: str, thread_id: str = "default") -> dict:
    """启动 Multi-Agent 协作

    writer 中的 interrupt() 会向上传播。
    """
    config = {"configurable": {"thread_id": thread_id}}

    print("[DEBUG multi] 开始 invoke...")
    multi_agent.invoke(
        {"messages": [HumanMessage(content=question)], "next": ""},
        config=config,
    )
    print("[DEBUG multi] invoke 返回")

    state = multi_agent.get_state(config)

    if state and state.interrupts:
        interrupts_data = []
        for iv in state.interrupts:
            if hasattr(iv, 'value'):
                interrupts_data.append(iv.value)
            elif isinstance(iv, dict):
                interrupts_data.append(iv.get('value', iv))
            else:
                interrupts_data.append(iv)
        return {
            "status": "interrupted",
            "thread_id": thread_id,
            "interrupts": interrupts_data,
        }

    answer = ""
    agent_trace = []
    messages = state.values.get("messages", []) if state and state.values else []
    for msg in messages:
        if hasattr(msg, "tool_calls") and msg.tool_calls:
            for tc in msg.tool_calls:
                agent_trace.append({"tool": tc["name"], "args": tc["args"]})
        if isinstance(msg, AIMessage) and msg.content:
            answer = msg.content

    return {
        "status": "done",
        "answer": answer,
        "agent_trace": agent_trace,
        "thread_id": thread_id,
    }


def multi_agent_resume(thread_id: str, approved: bool) -> dict:
    """审批后恢复"""
    config = {"configurable": {"thread_id": thread_id}}

    result = multi_agent.invoke(
        Command(resume={"approved": approved}),
        config=config,
    )

    answer = ""
    agent_trace = []
    for msg in result["messages"]:
        if hasattr(msg, "tool_calls") and msg.tool_calls:
            for tc in msg.tool_calls:
                agent_trace.append({"tool": tc["name"], "args": tc["args"]})
        if isinstance(msg, AIMessage) and msg.content:
            answer = msg.content

    return {
        "status": "done",
        "answer": answer,
        "agent_trace": agent_trace,
        "thread_id": thread_id,
    }


def multi_agent_get_state(thread_id: str) -> dict:
    config = {"configurable": {"thread_id": thread_id}}
    state = multi_agent.get_state(config)
    if state is None:
        return {"status": "not_found"}
    return {
        "status": "interrupted" if state.interrupts else "idle",
        "next": state.values.get("next", "") if state.values else "",
        "message_count": len(state.values.get("messages", [])) if state.values else 0,
    }
