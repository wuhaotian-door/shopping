"""
Level 1 LCEL 全技能 — chain.py

Day 1: invoke | Day 3: stream + batch | Day 6: Parallel + Passthrough + Lambda | Day 7: tenacity retry
"""

import os
from typing import Any, Dict, Iterator, List

from dotenv import load_dotenv
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.output_parsers import StrOutputParser
from langchain_core.runnables import RunnableParallel, RunnablePassthrough, RunnableLambda
from tenacity import retry, stop_after_attempt, wait_exponential

# Level 4: 使用生产级模型工厂（自带 token 跟踪）
from production import create_model

load_dotenv()

# ============================================================
# Day 7: tenacity 重试配置
# ============================================================
# stop_after_attempt(3) → 最多试 3 次（1 次正常 + 2 次重试）
# wait_exponential(min=1, max=30) → 重试间隔指数增长: 1s → 2s → 4s → ... 封顶 30s
# reraise=True → 全部失败后抛出原始异常，而不是 RetryError

RETRY = retry(
    stop=stop_after_attempt(3),
    wait=wait_exponential(multiplier=1, min=1, max=30),
    reraise=True,
)

# ============================================================
# 模型 & 链（不变的部分）
# ============================================================

model = create_model(
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

@RETRY
def ask(question: str) -> str:
    """invoke: 同步调用，失败自动重试最多 3 次"""
    return chain.invoke({"question": question})


# ============================================================
# Day 3 关键点 1: stream — 逐 token 返回，打字机效果
# ============================================================

def ask_stream(question: str) -> Iterator[str]:
    """stream: 流式输出，失败自动重试整个流

    注意: 流式重试只能重试"建立连接"阶段的失败。
    一旦开始接收 token 后中断，只能整体重来。
    """

    @RETRY
    def _start_stream():
        # 返回一个 eagr 的 stream iterator
        # 如果 API 调用本身失败，retry 会捕获并重试
        return chain.stream({"question": question})

    stream = _start_stream()
    for chunk in stream:
        yield chunk


# ============================================================
# Day 3 关键点 2: batch — 多个问题并发处理
# ============================================================

@RETRY
def ask_batch(questions: List[str]) -> List[str]:
    """batch: 批量并发调用，失败自动重试"""
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


@RETRY
def ask_enhanced(question: str) -> Dict[str, Any]:
    """增强问答: 并发获取答案+关键词+推荐，失败自动重试"""
    return enhanced_chain_structured.invoke({"question": question})
