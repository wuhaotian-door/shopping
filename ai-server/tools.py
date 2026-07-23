"""
Level 2 Tool Calling — 让 LLM 自主调用工具函数

核心理念:
  传统的链: 你预先决定了"检索→生成"的流程
  Tool Calling: LLM 自己决定什么时候、调用哪个函数

流程:
  用户问 → LLM 判断 → 需要查资料? → 调用 search_knowledge_base()
         → 需要列举?   → 调用 list_documents()
         → 直接能答?   → 直接输出答案

关键 API:
  - @tool 装饰器: 把 Python 函数变成 LLM 可调用的工具
  - model.bind_tools(): 把工具列表绑定给模型
  - tool_calls: 模型返回的"我想调用这个函数"信号
  - ToolMessage: 把工具执行结果传回模型
"""

import os
import re
from typing import List

from dotenv import load_dotenv
from langchain_core.messages import AIMessage, HumanMessage, SystemMessage, ToolMessage
from langchain_core.tools import tool
from langchain_openai import ChatOpenAI

from rag import get_vector_store

load_dotenv()

# ============================================================
# 定义工具: @tool 把普通函数变成 LLM 可调用的工具
# ============================================================

@tool
def search_knowledge_base(query: str) -> str:
    """搜索非遗知识库，获取与 query 相关的文档内容。
    当用户询问非遗项目的具体信息时（历史、工艺、特点等），使用此工具查询。
    返回最相关的3条文档片段。

    Args:
        query: 搜索关键词，例如"紫砂壶制作工艺"或"京剧历史"
    """
    vector_store = get_vector_store()
    docs = vector_store.similarity_search(query, k=3)
    if not docs:
        return "知识库中未找到相关信息。"
    return "\n\n---\n\n".join(
        f"[来源: {d.metadata.get('source', '未知')}]\n{d.page_content}"
        for d in docs
    )


@tool
def list_documents(query: str = "") -> str:
    """列出知识库中所有非遗文档及其涵盖内容。
    当用户询问"有哪些资料"、"知识库里有什么"或"涵盖哪些项目"时使用。
    也适用于用户想浏览或需要知道知识库范围的情况。

    Args:
        query: 可选的过滤关键词，留空则列出所有文档
    """
    import os as _os
    knowledge_dir = _os.path.join(_os.path.dirname(__file__), "knowledge")
    files = sorted(
        f for f in _os.listdir(knowledge_dir) if f.endswith(".md")
    )

    if not files:
        return "知识库为空，尚未添加文档。"

    lines = ["知识库包含以下文档："]
    for f in files:
        path = _os.path.join(knowledge_dir, f)
        with open(path, "r", encoding="utf-8") as fp:
            first_line = fp.readline().lstrip("#").strip()
        lines.append(f"- {f} — {first_line}")

    if query:
        lines.append(f"\n（已按关键词「{query}」过滤）")

    return "\n".join(lines)


# 工具注册表
TOOLS = [search_knowledge_base, list_documents]

# ============================================================
# Human-in-the-loop: 需要审批的"危险"工具
# ============================================================

@tool
def add_knowledge(title: str, content: str) -> str:
    """往知识库添加新的非遗文档。此操作需要人工审批。
    当用户说"记录一下"、"添加到知识库"、"保存这个信息"时使用。

    Args:
        title: 文档标题，例如"苏绣"
        content: 文档正文内容
    """
    import os as _os
    knowledge_dir = _os.path.join(_os.path.dirname(__file__), "knowledge")
    filepath = _os.path.join(knowledge_dir, f"{title}.md")

    if _os.path.exists(filepath):
        return f"文档「{title}」已存在，请使用其他标题或先告知用户。"

    with open(filepath, "w", encoding="utf-8") as f:
        f.write(f"# {title}\n\n{content}")

    return f"文档「{title}」已成功添加到知识库。需要调用 /ai/knowledge/reload 重建索引后才能被检索到。"


# 完整工具集（含需要审批的工具）
ALL_TOOLS = TOOLS + [add_knowledge]
ALL_TOOLS_BY_NAME = {t.name: t for t in ALL_TOOLS}

# 标记哪些工具需要审批
APPROVAL_REQUIRED = {"add_knowledge"}

TOOLS_BY_NAME = {t.name: t for t in TOOLS}  # 保留旧的兼容 tools.py Agent

# ============================================================
# 带工具的模型
# ============================================================
agent_model = ChatOpenAI(
    model="deepseek-chat",
    base_url=os.getenv("DEEPSEEK_BASE_URL", "https://api.deepseek.com"),
    api_key=os.getenv("DEEPSEEK_API_KEY"),
    temperature=0.3,
    max_tokens=512,
)

# bind_tools: 告诉模型"你可以调用这些函数"
model_with_tools = agent_model.bind_tools(TOOLS)

# ============================================================
# Agent 循环: 人机对话 + 工具调用
# ============================================================

SYSTEM_PROMPT = """你是非遗知识助手。你可以使用以下工具来获取信息：

1. search_knowledge_base — 搜索非遗知识库
2. list_documents — 列出知识库中的文档

规则：
- 当用户问具体非遗项目（历史、工艺等），用 search_knowledge_base 查询
- 当用户问"有哪些资料"，用 list_documents
- 如果知识库中没有，如实告知并建议用户提供资料
- 回答时注明信息来源"""


def agent_chat(question: str) -> dict:
    """同步 Tool Calling: 模型决定是否调工具，最多循环 3 轮

    返回 {"answer": str, "tool_calls": [...]}
    tool_calls 展示 LLM 调用了哪些工具，帮助理解内部决策过程
    """
    messages = [
        SystemMessage(content=SYSTEM_PROMPT),
        HumanMessage(content=question),
    ]

    tool_calls_log = []  # 记录所有工具调用，用于前端展示

    # 最多循环 3 轮（防止无限循环）
    for _ in range(3):
        response = model_with_tools.invoke(messages)
        messages.append(response)

        # 如果模型返回了工具调用请求
        if response.tool_calls:
            for tc in response.tool_calls:
                tool_name = tc["name"]
                tool_args = tc["args"]
                tool_func = TOOLS_BY_NAME.get(tool_name)

                if tool_func:
                    result = tool_func.invoke(tool_args)
                    tool_calls_log.append({
                        "tool": tool_name,
                        "args": tool_args,
                        "result": str(result)[:300],
                    })
                    # 把工具执行结果作为 ToolMessage 加回对话
                    messages.append(ToolMessage(
                        content=str(result),
                        tool_call_id=tc["id"],
                    ))
                else:
                    messages.append(ToolMessage(
                        content=f"错误: 未知工具 {tool_name}",
                        tool_call_id=tc["id"],
                    ))
        else:
            # 没有工具调用，直接返回答案
            break

    return {
        "answer": messages[-1].content,
        "tool_calls": tool_calls_log,
    }
