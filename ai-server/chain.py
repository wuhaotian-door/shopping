"""
Level 1 Day 1 — 第一条 LCEL 链: prompt | model | parser

学习要点:
- ChatPromptTemplate.from_messages() 构建提示词
- ChatOpenAI 配置模型参数（model, temperature, max_tokens）
- StrOutputParser 字符串输出解析
- LCEL pipe `|` 运算符串联各组件
- .invoke() 同步调用
"""

import os
from dotenv import load_dotenv
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.output_parsers import StrOutputParser
from langchain_openai import ChatOpenAI

load_dotenv()

# Day 1 关键点 1: API Key 从环境变量读取，绝不硬编码
model = ChatOpenAI(
    model="deepseek-chat",
    base_url=os.getenv("DEEPSEEK_BASE_URL", "https://api.deepseek.com"),
    api_key=os.getenv("DEEPSEEK_API_KEY"),
    temperature=0.7,        # 0=确定性, 1=创造性, 非遗问答需要一定创造性
    max_tokens=512,          # 限制回答长度，控制成本
)

# Day 1 关键点 2: ChatPromptTemplate — 用 system 设定角色，用 user 接收输入
prompt = ChatPromptTemplate.from_messages([
    ("system", "你是中国非物质文化遗产领域的专家，精通各类非遗项目的历史、技艺和文化内涵。请用简洁专业的语言回答问题。"),
    ("user", "{question}"),
])

# Day 1 关键点 3: StrOutputParser — 把模型输出转成纯字符串
parser = StrOutputParser()

# Day 1 关键点 4: LCEL pipe — 用 | 把组件串联起来
chain = prompt | model | parser


# Day 1 关键点 5: invoke() 同步调用链
def ask(question: str) -> str:
    """调用链，返回 AI 回答"""
    return chain.invoke({"question": question})
