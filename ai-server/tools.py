"""
工具定义 + 知识库 — 工具层

5 个 LangChain Tool，被 Agent 按需调用。
知识库使用 Chroma 存储 4 个独立集合，每个集合对应一种业务数据类型。

搜索策略 — 三层回退:
  ① Chroma 语义搜索  → 向量相似度匹配（适合段落/描述类查询）
  ② 关键词文件匹配  → 直接扫描 .md 文件（适合精确人名/地名）
  ③ SQLite 兜底     → 直接查数据库（前两层都失败时）
"""

import glob as _glob
import logging
import os
import sqlite3
import threading
import time
from typing import Dict, List, Optional

from langchain_community.document_loaders import DirectoryLoader, TextLoader
from langchain_community.vectorstores import Chroma
from langchain_core.tools import tool
from langchain_huggingface import HuggingFaceEmbeddings
from langchain_text_splitters import RecursiveCharacterTextSplitter

logger = logging.getLogger("ai-server.tools")

# ---- 路径 ----
BASE = os.path.dirname(os.path.abspath(__file__))
DATA = os.path.join(BASE, "data")                   # .md 文件根目录
CHROMA_DIR = os.path.join(BASE, "chroma_db")         # Chroma 持久化目录

# 4 个集合：子目录名 → Chroma collection 名
COLLECTIONS = {
    "knowledge": "knowledge",
    "inheritors": "inheritors",
    "projects": "projects",
    "products": "products",
}

# ---- 全局单例（懒加载 + 双重检查锁） ----
_embeddings: Optional[HuggingFaceEmbeddings] = None
_stores: Dict[str, Chroma] = {}
_lock = threading.Lock()


# ============================================================
# Embedding 模型 — 文本 → 向量
# ============================================================

def _get_embeddings() -> HuggingFaceEmbeddings:
    """
    获取 Embedding 模型（单例、懒加载、线程安全）

    paraphrase-multilingual-MiniLM-L12-v2:
      - 384 维向量，支持中文
      - 本地 CPU 推理，首次加载约 15 秒（下载 ~400MB）
      - 归一化向量（normalize_embeddings=True）提高余弦相似度精度
    """
    global _embeddings
    if _embeddings is None:
        logger.info("加载 Embedding 模型: paraphrase-multilingual-MiniLM-L12-v2")
        t0 = time.time()
        _embeddings = HuggingFaceEmbeddings(
            model_name="paraphrase-multilingual-MiniLM-L12-v2",
            model_kwargs={"device": "cpu"},
            encode_kwargs={"normalize_embeddings": True},
        )
        logger.info(f"Embedding 模型加载完成 | {time.time() - t0:.1f}s")
    return _embeddings


# ============================================================
# 知识库构建 — .md 文件 → 分块 → 向量化 → Chroma
# ============================================================

def build_all():
    """
    重建所有集合的知识库。

    流程:
      ① 遍历 data/{knowledge,inheritors,projects,products}/ 下的 .md 文件
      ② RecursiveCharacterTextSplitter 分块（500 字/块，50 字重叠）
      ③ HuggingFaceEmbeddings 向量化
      ④ 存入 Chroma（落盘到 chroma_db/）
    """
    # 确保数据目录存在
    for d in COLLECTIONS:
        os.makedirs(os.path.join(DATA, d), exist_ok=True)

    emb = _get_embeddings()
    splitter = RecursiveCharacterTextSplitter(
        chunk_size=500,        # 每块 500 字：够 LLM 推理，够精准匹配
        chunk_overlap=50,      # 相邻块重叠 50 字：避免切断上下文
        separators=["\n\n", "\n", "。", "，", " ", ""],  # 中文友好的分割优先级
    )

    total = 0
    for sub, col in COLLECTIONS.items():
        path = os.path.join(DATA, sub)
        files = [f for f in os.listdir(path) if f.endswith(".md")] if os.path.exists(path) else []
        if not files:
            continue

        logger.info(f"[{col}] {len(files)}篇 → {col}")

        # 加载文档
        loader = DirectoryLoader(
            path, glob="*.md",
            loader_cls=TextLoader,
            loader_kwargs={"encoding": "utf-8"},
        )
        docs = loader.load()
        chunks = splitter.split_documents(docs)

        # 向量化 + 存入 Chroma
        store = Chroma.from_documents(
            documents=chunks,
            embedding=emb,
            collection_name=col,
            persist_directory=CHROMA_DIR,
        )
        with _lock:
            _stores[col] = store
        total += len(chunks)

        logger.info(f"[{col}] {len(chunks)}片段 向量化完成")

    logger.info(f"知识库构建完成: {total}个向量")


# ============================================================
# 获取集合 — 懒加载已有 Chroma 索引
# ============================================================

def _get_store(col: str) -> Chroma:
    """获取指定集合的向量存储（懒加载 + 线程安全）"""
    if col not in _stores:
        with _lock:
            if col not in _stores:
                _stores[col] = Chroma(
                    collection_name=col,
                    persist_directory=CHROMA_DIR,
                    embedding_function=_get_embeddings(),
                )
    return _stores[col]


# ============================================================
# 第 ① 层：Chroma 语义搜索
# ============================================================

def _semantic_search(query: str, collection: str, top_k: int) -> List[Dict[str, str]]:
    """Chroma similarity_search，按向量余弦相似度返回 Top-K"""
    try:
        store = _get_store(collection)
        docs = store.similarity_search(query, k=top_k)
        return [
            {
                "content": d.page_content,
                "source": d.metadata.get("source", ""),
                "collection": collection,
            }
            for d in docs
        ]
    except Exception as e:
        logger.warning(f"语义搜索异常: {e}")
        return []


# ============================================================
# 第 ② 层：关键词匹配（解决人名/地名精确搜索）
# ============================================================

def _keyword_search(query: str, collection: str, top_k: int) -> List[Dict[str, str]]:
    """
    直接扫描 data/{collection}/ 下的 .md 文件，
    任一关键词命中即视为匹配。

    适用场景: 精确人名（"张德明"）、地名（"宜兴"）——Chroma 语义搜索
    对这些短查询效果差，关键词匹配可以兜底。
    """
    data_dir = os.path.join(DATA, collection)
    if not os.path.exists(data_dir):
        return []

    keywords = query.strip().split()
    results = []
    for fpath in _glob.glob(os.path.join(data_dir, "*.md")):
        try:
            with open(fpath, "r", encoding="utf-8") as f:
                content = f.read()
            if any(kw in content for kw in keywords):
                results.append({
                    "content": content[:1000],
                    "source": fpath,
                    "collection": collection,
                })
        except Exception:
            continue

    return results[:top_k]


# ============================================================
# 第 ③ 层：SQLite 兜底（前两层都失败时）
# ============================================================

def _sqlite_fallback(query: str, collection: str, top_k: int) -> List[Dict[str, str]]:
    """
    直接查 data/ai_data.db —— 这是启动时从主后端 MySQL 同步来的
    结构化数据，确保任何情况下都能找到内容。
    """
    db_path = os.path.join(DATA, "ai_data.db")
    if not os.path.exists(db_path):
        return []

    # collection 名 → SQLite 表名 映射
    table_map = {
        "knowledge": "knowledge_article",
        "inheritors": "inheritor",
        "projects": "heritage_project",
        "products": "product",
    }
    table = table_map.get(collection)
    if not table:
        return []

    keywords = query.strip().split()
    try:
        conn = sqlite3.connect(db_path)
        cur = conn.cursor()
        cur.execute(f"SELECT title, content FROM {table}")
        rows = cur.fetchall()
        conn.close()

        results = []
        for title, content in rows:
            text = (title or "") + " " + (content or "")
            if any(kw in text for kw in keywords):
                results.append({
                    "content": (content or "")[:1000],
                    "source": title or "",
                    "collection": collection,
                })
        return results[:top_k]
    except Exception as e:
        logger.warning(f"SQLite兜底失败: {e}")
        return []


# ============================================================
# 统一搜索入口 — 三层级联
# ============================================================

def search(query: str, collection: str = "knowledge", top_k: int = 3) -> List[Dict[str, str]]:
    """
    三层搜索:
      ① Chroma 语义搜索
      ② 关键词文件匹配（始终执行，与语义结果合并去重，精确匹配优先）
      ③ SQLite 兜底（前两层都无结果时）

    返回: [{"content": str, "source": str, "collection": str}, ...]
    """
    logger.info(f"搜索 [{collection}] query='{query[:60]}'")
    t0 = time.time()

    # ① 语义
    results = _semantic_search(query, collection, top_k)
    logger.info(f"  语义: {len(results)}条 | {time.time() - t0:.2f}s")

    # ② 关键词（始终执行，与语义结果合并）
    kw = _keyword_search(query, collection, top_k)
    if kw:
        seen = {r["source"] for r in results}
        for k in kw:
            if k["source"] not in seen:
                results.insert(0, k)  # 精确匹配插在最前面
                seen.add(k["source"])
        logger.info(f"  关键词合并: +{len(kw)}条 → 共{len(results)}条")

    # ③ SQLite
    if not results:
        results = _sqlite_fallback(query, collection, top_k)
        if results:
            logger.info(f"  SQLite兜底: {len(results)}条")

    logger.info(f"  搜索完成: {len(results)}条 | {time.time() - t0:.2f}s")
    return results


def search_all(query: str, top_k: int = 2) -> List[Dict[str, str]]:
    """跨所有 4 个集合搜索，合并去重后返回"""
    results = []
    for c in COLLECTIONS.values():
        try:
            results.extend(search(query, c, top_k))
        except Exception:
            pass
    # 按内容长度粗排（长内容通常更相关）
    results.sort(key=lambda r: len(r["content"]), reverse=True)
    return results[:top_k * 2]


# ============================================================
# 结果格式化 — 将搜索结果转成 LLM 可读的文本
# ============================================================

def _fmt(results: list) -> str:
    """
    格式化搜索结果。

    开头加指令提示语，强制 LLM 注意这是真实数据。
    每条结果标注来源和集合类型，方便 LLM 引用。

    示例输出:
      【以下内容必须据此回答用户，这是平台真实知识库数据，找到 3 条】
      ---
      [资料1] 来源: 紫砂壶.md | 类型: knowledge
      紫砂壶是中国江苏宜兴特产...
      ---
      [资料2] ...
    """
    if not results:
        return ""

    header = f"【以下内容必须据此回答用户，这是平台真实知识库数据，找到 {len(results)} 条】"
    parts = [header]

    for i, r in enumerate(results, 1):
        src = r["source"].replace("\\", "/").split("/")[-1] if r.get("source") else "?"
        col = r.get("collection", "?")
        parts.append(
            f"---\n[资料{i}] 来源: {src} | 类型: {col}\n{r['content']}"
        )

    return "\n".join(parts)


# ============================================================
# 工具定义 — 5 个 LangChain Tool
#
# @tool 装饰器将 Python 函数包装为 LLM 可调用的工具:
#   - 函数名 → 工具名
#   - docstring → 工具描述（LLM 据此判断何时调用）
#   - 参数类型 → JSON Schema（LLM 据此传参）
#
# 返回格式约定:
#   ✅ 搜到内容 → 带指令头的格式化文本
#   ❌ 没搜到  → 明确的"未找到"消息（LLM 据此判断是否如实告知用户）
# ============================================================

@tool
def search_knowledge(query: str) -> str:
    """搜索非遗知识文章。用户问历史/工艺/特点时调用。例: 紫砂壶的历史"""
    r = search(query, "knowledge", 3)
    logger.info(f"search_knowledge: {len(r)}条")
    return _fmt(r) if r else "❌ 知识库中确实未找到相关信息。请告知用户平台暂未收录此内容。"


@tool
def search_inheritors(query: str) -> str:
    """搜索传承人。用户问传承人/老师/匠人时调用。例: 苏州有哪些刺绣传承人"""
    r = search(query, "inheritors", 3)
    logger.info(f"search_inheritors: {len(r)}条")
    return _fmt(r) if r else "❌ 传承人库中确实未找到此人的信息。请告知用户平台暂未收录该传承人。"


@tool
def search_projects(query: str) -> str:
    """搜索项目活动。用户问活动/体验/课程时调用。例: 最近有什么陶艺体验"""
    r = search(query, "projects", 3)
    logger.info(f"search_projects: {len(r)}条")
    return _fmt(r) if r else "❌ 项目库中确实未找到匹配的活动。请告知用户平台暂未收录该项目。"


@tool
def search_products(query: str) -> str:
    """搜索文创商品。用户问商品/礼物/购买时调用。例: 适合送长辈的非遗礼物"""
    r = search(query, "products", 3)
    logger.info(f"search_products: {len(r)}条")
    return _fmt(r) if r else "❌ 商品库中确实未找到匹配的商品。请告知用户平台暂未收录该商品。"


@tool
def recommend_content(preference: str = "") -> str:
    """
    推荐非遗内容。用户说"推荐"或模糊意图时调用。
    有偏好时搜索相关类别，无偏好时从各集合取样作为热门推荐。
    """
    r = search_all(preference, 2) if preference else search_all("", 1)
    logger.info(f"recommend_content: {len(r)}条")
    if r:
        return "为您推荐以下非遗内容：\n\n" + _fmt(r)
    return "❌ 推荐引擎确实无内容可推荐。请告知用户平台内容还在建设中。"


# ---- 工具注册表（create_react_agent 使用） ----
ALL_TOOLS = [
    search_knowledge,
    search_inheritors,
    search_projects,
    search_products,
    recommend_content,
]
