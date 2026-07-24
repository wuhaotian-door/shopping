"""
Level 2 RAG — 检索增强生成

流程:
  ① 加载文档 (DirectoryLoader)
  ② 文本分割 (RecursiveCharacterTextSplitter)  ← chunk_size=500, overlap=50
  ③ 向量化   (HuggingFaceEmbeddings)           ← 本地模型，免费
  ④ 存入向量库 (Chroma)                        ← 落盘到 chroma_db/
  ⑤ RAG 链 (检索 → 拼装 prompt → LLM → 输出)

学习要点:
  - 文档加载与切分策略
  - Embedding 向量化的原理
  - 向量相似度检索
  - RAG chain 的组装（检索 + 生成串联）
"""

import os
import threading
from typing import Iterator, List

from dotenv import load_dotenv
from langchain_community.document_loaders import DirectoryLoader, TextLoader
from langchain_community.embeddings import HuggingFaceEmbeddings
from langchain_community.vectorstores import Chroma
from langchain_core.output_parsers import StrOutputParser
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.runnables import RunnablePassthrough
from langchain_openai import ChatOpenAI
from langchain_text_splitters import RecursiveCharacterTextSplitter
from tenacity import retry, stop_after_attempt, wait_exponential

from logger_config import get_logger

logger = get_logger(__name__)

load_dotenv()

# ============================================================
# 配置
# ============================================================
KNOWLEDGE_DIR = os.path.join(os.path.dirname(__file__), "knowledge")
CHROMA_DIR = os.path.join(os.path.dirname(__file__), "chroma_db")

model = ChatOpenAI(
    model="deepseek-chat",
    base_url=os.getenv("DEEPSEEK_BASE_URL", "https://api.deepseek.com"),
    api_key=os.getenv("DEEPSEEK_API_KEY"),
    temperature=0.3,       # RAG 场景用低温度，减少幻觉
    max_tokens=512,
)

RETRY = retry(stop=stop_after_attempt(3), wait=wait_exponential(min=1, max=30), reraise=True)

# ============================================================
# ③ Embedding 模型 — 把文本转成向量
# ============================================================
# sentence-transformers 本地模型，首次运行会自动下载 (~400MB)
# paraphrase-multilingual-MiniLM-L12-v2: 支持中文，效果好，体积适中
embeddings = HuggingFaceEmbeddings(
    model_name="paraphrase-multilingual-MiniLM-L12-v2",
    model_kwargs={"device": "cpu"},       # 无 GPU 用 CPU
    encode_kwargs={"normalize_embeddings": True},  # 归一化，提高相似度精度
)

# ============================================================
# ① + ② + ④: 加载 → 分割 → 向量化 → 存入 Chroma
# ============================================================
def build_knowledge_base():
    """(重新)构建知识库 — 加载 knowledge/ 下所有 md 文件"""
    logger.info("开始构建知识库...")
    loader = DirectoryLoader(
        KNOWLEDGE_DIR,
        glob="*.md",
        loader_cls=TextLoader,
        loader_kwargs={"encoding": "utf-8"},
    )
    docs = loader.load()
    logger.info("加载了 %d 篇文档", len(docs))

    # Day 2 关键点: RecursiveCharacterTextSplitter
    # chunk_size=500: 每个片段 500 字
    # chunk_overlap=50: 相邻片段重叠 50 字（避免切断上下文）
    splitter = RecursiveCharacterTextSplitter(
        chunk_size=500,
        chunk_overlap=50,
        separators=["\n\n", "\n", "。", "，", " ", ""],  # 中文友好的分割优先级
    )
    chunks = splitter.split_documents(docs)
    logger.info("切分为 %d 个片段", len(chunks))

    # 向量化 + 存入 Chroma
    logger.info("向量化中（首次需要下载模型，约400MB）...")
    vector_store = Chroma.from_documents(
        documents=chunks,
        embedding=embeddings,
        persist_directory=CHROMA_DIR,
    )
    logger.info("知识库构建完成，数据存于 %s", CHROMA_DIR)
    return vector_store


def load_knowledge_base():
    """加载已有知识库（不重建）"""
    if not os.path.exists(CHROMA_DIR):
        return build_knowledge_base()
    return Chroma(
        persist_directory=CHROMA_DIR,
        embedding_function=embeddings,
    )


# 启动时加载（加锁，防止并行访问时多线程同时初始化 Chroma）
_vector_store = None
_vector_store_lock = threading.Lock()

def get_vector_store():
    global _vector_store
    if _vector_store is None:
        with _vector_store_lock:
            # 双重检查：拿到锁后再次确认（可能其他线程已经初始化了）
            if _vector_store is None:
                _vector_store = load_knowledge_base()
    return _vector_store


# ============================================================
# ⑤ RAG 链: 检索 → 组装 prompt → LLM → 输出
# ============================================================
def get_rag_chain():
    """构建 RAG 链"""
    vector_store = get_vector_store()
    retriever = vector_store.as_retriever(search_kwargs={"k": 3})  # 检索 Top 3

    # 只拼接 page_content，不含元数据，减少 token 消耗
    def format_docs(docs):
        return "\n\n".join(
            f"【资料 {i+1}】{d.page_content}" for i, d in enumerate(docs)
        )

    prompt = ChatPromptTemplate.from_messages([
        ("system",
         "你是中国非物质文化遗产领域的专家。请严格根据以下资料回答用户问题。"
         "如果资料中没有相关信息，请如实说'资料中未提及'，不要编造。\n\n"
         "{context}"),
        ("user", "{question}"),
    ])

    # {"context": ..., "question": ...} 是 RunnableParallel 的 dict 简写
    rag_chain = (
        {"context": retriever | format_docs, "question": RunnablePassthrough()}
        | prompt
        | model
        | StrOutputParser()
    )

    return rag_chain, retriever


# ============================================================
# RAG 调用接口
# ============================================================
@RETRY
def rag_ask(question: str):
    """RAG 问答: 检索 + 生成，返回 (答案, 来源文档列表)"""
    rag_chain, retriever = get_rag_chain()

    # 先获取检索到的文档，用于展示来源
    docs = retriever.invoke(question)

    # 执行 RAG 链
    answer = rag_chain.invoke(question)

    sources = [
        {"content": d.page_content[:200], "source": d.metadata.get("source", "未知")}
        for d in docs
    ]
    return {"answer": answer, "sources": sources, "question": question}


def rag_ask_stream(question: str) -> Iterator[str]:
    """RAG 流式问答"""
    rag_chain, _ = get_rag_chain()
    for chunk in rag_chain.stream(question):
        yield chunk


def rag_get_sources(question: str) -> List[dict]:
    """只检索，不生成答案（调试用）"""
    _, retriever = get_rag_chain()
    docs = retriever.invoke(question)
    return [
        {"content": d.page_content, "source": d.metadata.get("source", "未知")}
        for d in docs
    ]
