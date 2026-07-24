"""
Level 5 高级 LangGraph — Send API 动态并行

之前所有 Agent 的图都是静态的：固定节点，固定边。
Send API 让图在运行时动态决定"并行执行几次、每次传什么参数"。

模式: Map-Reduce
  Map:   一个任务拆成 N 个子任务，并行执行
  Reduce: 汇总所有子任务结果，生成最终输出

本例:
  用户: "对比分析紫砂壶、景泰蓝和蜀绣的艺术特点"
    → parser:  拆出 3 个 topic
    → Send ×3: 3 个 searcher 并行查知识库
    → synthesizer: 汇总 3 份结果 → 对比总结

关键 API:
  from langgraph.types import Send

  def continue_to_nodes(state):
      return [Send("searcher", {"topic": t}) for t in state["topics"]]

  Send("节点名", {"字段": 值}) → 运行时用 arg 覆盖 state 的对应字段
  返回多个 Send → LangGraph 自动并行执行，结果通过 add_messages 合并
"""

import os as _os
import time
from operator import add
from typing import Annotated, Any, Dict, List

from dotenv import load_dotenv
from langchain_core.messages import (AIMessage, BaseMessage, HumanMessage,
                                      SystemMessage, ToolMessage)
from langchain_openai import ChatOpenAI
from langgraph.checkpoint.memory import MemorySaver
from langgraph.graph import END, START, StateGraph
from langgraph.graph.message import add_messages
from langgraph.types import Send
from typing_extensions import TypedDict

from tools import TOOLS
from logger_config import get_logger

logger = get_logger(__name__)

load_dotenv()

# ============================================================
# ① State — trace 字段用 add reducer 实现累加
# ============================================================
# operator.add 对 list 的效果就是拼接：["a"] + ["b"] = ["a", "b"]
# 每个节点返回 {"trace": [{...}]} → 自动拼到 trace 列表末尾

class FanOutState(TypedDict):
    messages: Annotated[List[BaseMessage], add_messages]
    topic: str                           # 单个 topic，由 Send 的 arg 设置
    topics: List[str]                     # topic 列表，由 parser 解析后设置
    trace: Annotated[List[dict], add]     # ★ 步骤日志，每个节点往里面追加


def _now() -> str:
    return time.strftime("%H:%M:%S")


# ============================================================
# ② Parser — 把用户问题拆成子任务
# ============================================================
parser_model = ChatOpenAI(
    model="deepseek-chat",
    base_url=_os.getenv("DEEPSEEK_BASE_URL", "https://api.deepseek.com"),
    api_key=_os.getenv("DEEPSEEK_API_KEY"),
    temperature=0,
    max_tokens=128,
)

PARSER_PROMPT = """你是任务拆分员。用户想要对比/分析/了解多个非遗项目时，
把项目名称提取出来，用逗号分隔。

示例:
  用户: "对比紫砂壶和景泰蓝"        → 紫砂壶, 景泰蓝
  用户: "紫砂壶、蜀绣、京剧的特点"   → 紫砂壶, 蜀绣, 京剧
  用户: "紫砂壶是什么"               → 紫砂壶

只回复项目名称，逗号分隔，不要其他内容。"""


def parser_node(state: FanOutState) -> Dict[str, Any]:
    """解析用户问题 → 提取 topic 列表"""
    user_msg = state["messages"][-1].content

    logger.info("Parser: 输入=%.80s", user_msg)

    response = parser_model.invoke([
        SystemMessage(content=PARSER_PROMPT),
        HumanMessage(content=f"用户问: {user_msg}"),
    ])

    raw = response.content.strip()
    topics = [t.strip() for t in raw.replace("、", ",").split(",") if t.strip()]

    logger.debug("Parser: LLM 原始输出='%s'", raw)
    logger.info("Parser: 拆出 %d 个 topics: %s", len(topics), topics)

    return {
        "topics": topics,
        "messages": [AIMessage(content=f"已识别 {len(topics)} 个非遗项目: {', '.join(topics)}")],
        "trace": [{
            "step": "① Parser: 问题拆分",
            "time": _now(),
            "input": user_msg[:100],
            "output": f"拆出 {len(topics)} 个 topic: {', '.join(topics)}",
            "detail": raw,
        }],
    }


# ============================================================
# ③ Searcher — 单 topic 搜索（被 Send 并行调用 N 次）
# ============================================================
searcher_model = ChatOpenAI(
    model="deepseek-chat",
    base_url=_os.getenv("DEEPSEEK_BASE_URL", "https://api.deepseek.com"),
    api_key=_os.getenv("DEEPSEEK_API_KEY"),
    temperature=0.3,
    max_tokens=512,
)

SEARCHER_PROMPT = """你是非遗研究员。根据 topic 字段指定的项目，查询知识库并整理信息。

当前任务: 查询「{topic}」的相关信息。

规则:
1. 用 search_knowledge_base 搜索
2. 整理 3-5 个要点（历史、工艺、特点、产地等）
3. 标注信息来源"""

searcher_with_tools = searcher_model.bind_tools(TOOLS)


def searcher_node(state: FanOutState) -> Dict[str, Any]:
    """★ 被 Send API 并行调用的节点

    state["topic"] 由 Send 的 arg 传入，每个并行实例拿到不同的 topic。
    返回的 trace 和 messages 通过各自的 reducer 自动合并。
    """
    topic = state.get("topic", "")
    t_start = time.time()

    logger.info("Searcher[%s]: 开始搜索...", topic)

    sys_prompt = SEARCHER_PROMPT.replace("{topic}", topic)

    # 第 1 轮: LLM 决定搜索
    response1 = searcher_with_tools.invoke([
        SystemMessage(content=sys_prompt),
        HumanMessage(content=f"请查询「{topic}」的相关信息"),
    ])

    if not response1.tool_calls:
        elapsed = round(time.time() - t_start, 1)
        logger.info("Searcher[%s]: 无工具调用，直接返回 (耗时 %.1fs)", topic, elapsed)
        return {
            "messages": [response1],
            "trace": [{
                "step": f"🔍 Searcher: {topic}",
                "time": _now(),
                "tool_calls": "无（LLM 直接回答）",
                "output": response1.content[:200],
                "elapsed_s": elapsed,
            }],
        }

    # 第 2 轮: 执行工具
    logger.debug("Searcher[%s]: LLM 决定调用工具: %s", topic, [tc['name'] for tc in response1.tool_calls])
    tool_msgs = []
    tools_by_name = {t.name: t for t in TOOLS}
    tool_log = []

    for tc in response1.tool_calls:
        tool_func = tools_by_name.get(tc["name"])
        if tool_func:
            logger.debug("Searcher[%s]: 执行工具 %s(%s)", topic, tc['name'], tc['args'])
            result = tool_func.invoke(tc["args"])
            logger.debug("Searcher[%s]: 工具返回 %d 字符", topic, len(str(result)))
            tool_msgs.append(ToolMessage(content=str(result), tool_call_id=tc["id"]))
            tool_log.append({
                "tool": tc["name"],
                "args": tc["args"],
                "result_preview": str(result)[:150],
            })

    # 第 3 轮: 总结
    logger.debug("Searcher[%s]: LLM 总结中...", topic)
    response2 = searcher_with_tools.invoke([
        SystemMessage(content=sys_prompt),
        HumanMessage(content=f"请查询「{topic}」的相关信息"),
        response1,
        *tool_msgs,
    ])

    elapsed = round(time.time() - t_start, 1)
    summary = response2.content
    logger.info("Searcher[%s]: 完成 (耗时 %.1fs), 总结长度: %d 字符", topic, elapsed, len(summary))

    return {
        "messages": [AIMessage(content=f"## {topic}\n{summary}")],
        "trace": [{
            "step": f"🔍 Searcher: {topic}",
            "time": _now(),
            "tool_calls": tool_log,
            "output": summary[:300],
            "elapsed_s": elapsed,
        }],
    }


# ============================================================
# ④ Synthesizer — 汇总所有并行结果
# ============================================================
synth_model = ChatOpenAI(
    model="deepseek-chat",
    base_url=_os.getenv("DEEPSEEK_BASE_URL", "https://api.deepseek.com"),
    api_key=_os.getenv("DEEPSEEK_API_KEY"),
    temperature=0.3,
    max_tokens=1024,
)

SYNTH_PROMPT = """你是非遗研究专家。上面已经并行查询了多个非遗项目的信息（每项以 ## 项目名 开头），
请基于这些信息写一份对比分析总结。

要求:
1. 每个项目简要介绍（2-3 句）
2. 比较它们的异同点
3. 总结各项目的独特价值

不要添加知识库中没有的信息。"""


def synthesizer_node(state: FanOutState) -> Dict[str, Any]:
    """汇总所有 searcher 的并行结果，生成对比总结"""
    t_start = time.time()

    # 收集所有 searcher 的摘要
    searcher_msgs = [
        msg for msg in state["messages"]
        if isinstance(msg, AIMessage) and msg.content.startswith("## ")
    ]
    logger.info("Synthesizer: 汇总中，收到 %d 份搜索结果", len(searcher_msgs))
    for msg in searcher_msgs:
        lines = msg.content.split("\n")
        title = lines[0] if lines else "(空)"
        logger.debug("Synthesizer:   - %s (%d 字符)", title, len(msg.content))

    response = synth_model.invoke([
        SystemMessage(content=SYNTH_PROMPT),
        *state["messages"],
    ])

    elapsed = round(time.time() - t_start, 1)
    logger.info("Synthesizer: 完成 (耗时 %.1fs)", elapsed)

    return {
        "messages": [response],
        "trace": [{
            "step": "② Synthesizer: 汇总对比",
            "time": _now(),
            "input": f"收到 {len(searcher_msgs)} 份搜索结果",
            "output": response.content[:500],
            "elapsed_s": elapsed,
        }],
    }


# ============================================================
# ⑤ Send 路由 — 创建并行任务
# ============================================================

def fan_out_router(state: FanOutState):
    """条件边: 根据 topics 列表返回 Send 对象

    单个 topic → 一个 Send
    多个 topic → Send 列表，LangGraph 自动并行
    """
    topics = state.get("topics", [])
    count = len(topics)

    if count >= 2:
        logger.info("FanOut: ★ 创建 %d 个并行 Send → searcher 同时跑", count)
        return [Send("searcher", {"topic": t}) for t in topics]

    elif count == 1:
        logger.info("FanOut: 单 topic → 1 个 Send")
        return Send("searcher", {"topic": topics[0]})

    else:
        logger.info("FanOut: 无 topic → 直接到 synthesizer")
        return "synthesizer"


# ============================================================
# ⑥ 组装图
# ============================================================
builder = StateGraph(FanOutState)

builder.add_node("parser", parser_node)
builder.add_node("searcher", searcher_node)
builder.add_node("synthesizer", synthesizer_node)

builder.add_edge(START, "parser")
builder.add_conditional_edges("parser", fan_out_router)
builder.add_edge("searcher", "synthesizer")
builder.add_edge("synthesizer", END)

checkpointer = MemorySaver()
fan_out_agent = builder.compile(checkpointer=checkpointer)


# ============================================================
# ⑦ 调用接口
# ============================================================

def advanced_agent_ask(question: str, thread_id: str = "default") -> dict:
    """调用 Send 并行 Agent，返回答案 + 完整执行 trace"""
    config = {"configurable": {"thread_id": thread_id}}

    t_total = time.time()
    result = fan_out_agent.invoke(
        {"messages": [HumanMessage(content=question)]},
        config=config,
    )
    total_elapsed = round(time.time() - t_total, 1)

    # 提取最终答案（最后一条 AIMessage，且不是搜索结果摘要）
    answer = ""
    for msg in reversed(result["messages"]):
        if isinstance(msg, AIMessage) and msg.content:
            # 跳过中间步骤消息
            if not msg.content.startswith("## ") and "已识别" not in msg.content:
                answer = msg.content
                break

    # 从 messages 中取各 searcher 的初步结果
    searcher_results = [
        msg.content[:300]
        for msg in result["messages"]
        if isinstance(msg, AIMessage) and msg.content.startswith("## ")
    ]

    trace = result.get("trace", [])

    logger.info("advanced_agent: 总耗时 %.1fs, trace 条目: %d", total_elapsed, len(trace))
    for t in trace:
        logger.debug("  %s (%.1fs)", t['step'], t.get('elapsed_s', 0))

    return {
        "answer": answer,
        "thread_id": thread_id,
        "messages_count": len(result["messages"]),
        "total_elapsed_s": total_elapsed,
        "trace": trace,
        "searcher_results": searcher_results,
    }
