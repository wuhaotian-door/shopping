"""
Level 1 Day 1 + Day 3 — LCEL 链: invoke / stream / batch

学习要点:
  Day 1:
  - ChatPromptTemplate.from_messages() 构建提示词
  - ChatOpenAI 配置模型参数（model, temperature, max_tokens）
  - StrOutputParser 字符串输出解析
  - LCEL pipe `|` 运算符串联各组件
  - .invoke() 同步调用

  Day 3:
  - .stream() 流式输出（打字机效果，逐 token 返回）
  - .batch()  批量并发处理（多输入同时调用）
  - stream vs batch vs invoke 各自适用场景
"""

import os
from typing import Any, Dict, Iterator, List

from dotenv import load_dotenv
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.output_parsers import StrOutputParser
from langchain_core.runnables import RunnableParallel, RunnablePassthrough, RunnableLambda
from langchain_openai import ChatOpenAI

load_dotenv()

# ============================================================
# 模型 & 链（不变的部分）
# ============================================================

model = ChatOpenAI(
    model="deepseek-chat",
    base_url=os.getenv("DEEPSEEK_BASE_URL", "https://api.deepseek.com"),
    api_key=os.getenv("DEEPSEEK_API_KEY"),
    temperature=0.7,
    max_tokens=512,
)

prompt = ChatPromptTemplate.from_messages([
    ("system", "你是中国非物质文化遗产领域的专家，精通各类非遗项目的历史、技艺和文化内涵。请用简洁专业的语言回答问题。"),
    ("user", "{question}"),
])

parser = StrOutputParser()

chain = prompt | model | parser


# ============================================================
# Day 1: invoke — 等全部完成后一次性返回
# ============================================================

def ask(question: str) -> str:
    """invoke: 同步调用，等模型生成完再返回完整结果"""
    return chain.invoke({"question": question})


# ============================================================
# Day 3 关键点 1: stream — 逐 token 返回，打字机效果
# ============================================================

def ask_stream(question: str) -> Iterator[str]:
    """stream: 流式输出，每生成一个 token 就 yield 出去

    适用场景:
      - 聊天机器人（打字机效果，用户不用等）
      - 长文生成（边生成边阅读）
      - 需要实时展示进度的场景

    与 invoke 对比:
      invoke: 快但用户干等 → 适合后台/批量
      stream: 首 token 快 → 适合前端交互
    """
    for chunk in chain.stream({"question": question}):
        yield chunk


# ============================================================
# Day 3 关键点 2: batch — 多个问题并发处理
# ============================================================

def ask_batch(questions: List[str]) -> List[str]:
    """batch: 批量并发调用，多个输入一起处理

    适用场景:
      - 批量翻译/摘要/分类
      - 对多篇文章做同样操作
      - 离线批处理任务

    与循环 invoke 对比:
      for q in questions: ask(q)       # 串行，慢
      ask_batch(questions)             # 并发，快
    """
    inputs = [{"question": q} for q in questions]
    return chain.batch(inputs)


# ============================================================
# Day 6 关键点 1: RunnableParallel — 多条链并发执行
# ============================================================

# 子链 A: 提取关键词
keyword_prompt = ChatPromptTemplate.from_messages([
    ("system", "从用户问题中提取 3-5 个非遗相关关键词，用中文顿号分隔，不要输出其他内容。"),
    ("user", "{question}"),
])
keyword_chain = keyword_prompt | model | StrOutputParser()

# 子链 B: 推荐相关非遗项目
suggest_prompt = ChatPromptTemplate.from_messages([
    ("system", "根据用户问题，推荐 3 个相关的中国非物质文化遗产项目，每行一个，用编号列出，格式: 1. 项目名 — 简介。"),
    ("user", "{question}"),
])
suggest_chain = suggest_prompt | model | StrOutputParser()

# ============================================================
# RunnableParallel — 三条子链同时跑，互不等待
#
# 结构:
#   RunnableParallel(
#       answer    = 主问答链,            # 子链 A
#       keywords  = 关键词提取链,         # 子链 B
#       suggests  = 推荐链,              # 子链 C
#       question  = RunnablePassthrough(), # ← 透传原始输入
#   )
#
# 数据流:
#   {"question": "什么是景泰蓝？"}
#       ├─→ answer    → model → "景泰蓝是..."
#       ├─→ keywords  → model → "景泰蓝、珐琅、掐丝"
#       ├─→ suggests  → model → "1. 景泰蓝 — ..."
#       └─→ question  → RunnablePassthrough() → "什么是景泰蓝？"  (原样透传)
#
# 时间线（3 条子链并发）:
#   answer:    [=== 3s ===]
#   keywords:  [== 2s ==]
#   suggests:  [==== 4s ====]
#   总耗时 ≈ 4s（最慢那条的时间），而不是 9s
# ============================================================

enhanced_chain = RunnableParallel(
    answer=RunnablePassthrough() | chain,                    # 主问答
    keywords=RunnablePassthrough() | keyword_chain,          # 关键词
    suggests=RunnablePassthrough() | suggest_chain,          # 推荐
    question=RunnablePassthrough(),                          # 原问题透传
)

# ============================================================
# Day 6 关键点 2: RunnableLambda — 在链中插入自定义 Python 函数
# ============================================================

def _parse_keywords(text: str) -> List[str]:
    """把 '景泰蓝、珐琅、掐丝' 转成 ['景泰蓝', '珐琅', '掐丝']"""
    return [k.strip() for k in text.replace("、", "，").replace(",", "，").split("，") if k.strip()]


def _parse_suggests(text: str) -> List[Dict[str, str]]:
    """把 '1. 景泰蓝 — 简介' 转成 [{"name": "景泰蓝", "desc": "简介"}, ...]"""
    items = []
    for line in text.strip().split("\n"):
        line = line.strip()
        if not line:
            continue
        # 去掉编号前缀如 "1. " "1、" "1)"
        for sep in [". ", ".", "、", ") "]:
            idx = line.find(sep)
            if 0 < idx <= 3:
                line = line[idx + len(sep):]
                break
        # 按 — 分割 name 和 desc
        if "—" in line:
            name, desc = line.split("—", 1)
        elif "：" in line:
            name, desc = line.split("：", 1)
        else:
            name, desc = line, ""
        items.append({"name": name.strip(), "desc": desc.strip()})
    return items


# 在 enhanced_chain 输出之上做后处理：keywords 从字符串变成列表，suggests 变成结构化数据
enhanced_chain_structured = enhanced_chain | RunnableLambda(lambda data: {
    **data,
    "keywords": _parse_keywords(data["keywords"]),
    "suggests": _parse_suggests(data["suggests"]),
})


def ask_enhanced(question: str) -> Dict[str, Any]:
    """增强问答: 一次请求，并发获取答案 + 关键词 + 推荐"""
    return enhanced_chain_structured.invoke({"question": question})
