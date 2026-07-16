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
from typing import Iterator, List

from dotenv import load_dotenv
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.output_parsers import StrOutputParser
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
