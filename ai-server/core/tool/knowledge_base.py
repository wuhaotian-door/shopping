"""
多集合知识库

4 个 Chroma 集合: knowledge | inheritors | projects | products
文档数据在 data/ 目录对应子目录下。
"""

import os
import threading
import time
from typing import Dict, List, Optional

from langchain_community.document_loaders import DirectoryLoader, TextLoader
from langchain_community.embeddings import HuggingFaceEmbeddings
from langchain_community.vectorstores import Chroma
from langchain_text_splitters import RecursiveCharacterTextSplitter
from utils.logger import get_logger

logger = get_logger(__name__)
logger.debug("模块加载: core.tool.knowledge_base")

BASE = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
DATA = os.path.join(BASE, "data")
CHROMA = os.path.join(BASE, "chroma_db")

COLLECTIONS = {
    "knowledge": "knowledge",
    "inheritors": "inheritors",
    "projects": "projects",
    "products": "products",
}

# 全局单例
_embeddings: Optional[HuggingFaceEmbeddings] = None
_stores: Dict[str, Chroma] = {}
_lock = threading.Lock()

logger.info(
    f"知识库路径: data={DATA} | chroma={CHROMA} | "
    f"集合: {list(COLLECTIONS.keys())}"
)

# ============================================================
# Embedding
# ============================================================

def _get_embeddings() -> HuggingFaceEmbeddings:
    global _embeddings
    if _embeddings is None:
        logger.info("首次加载 Embedding 模型: paraphrase-multilingual-MiniLM-L12-v2")
        t0 = time.time()
        _embeddings = HuggingFaceEmbeddings(
            model_name="paraphrase-multilingual-MiniLM-L12-v2",
            model_kwargs={"device": "cpu"},
            encode_kwargs={"normalize_embeddings": True},
        )
        logger.info(f"Embedding 模型加载完成 | {time.time()-t0:.1f}s")
    return _embeddings

# ============================================================
# 构建
# ============================================================

def build_all():
    """(重新)构建所有集合的知识库"""
    logger.info("=" * 40)
    logger.info("知识库构建开始...")

    # 确保数据目录存在
    for d in COLLECTIONS:
        path = os.path.join(DATA, d)
        os.makedirs(path, exist_ok=True)
        logger.debug(f"  数据目录: {path}")

    emb = _get_embeddings()
    splitter = RecursiveCharacterTextSplitter(
        chunk_size=500, chunk_overlap=50,
        separators=["\n\n", "\n", "。", "，", " ", ""],
    )

    total_chunks = 0
    for sub_dir, collection_name in COLLECTIONS.items():
        path = os.path.join(DATA, sub_dir)
        files = [f for f in os.listdir(path) if f.endswith(".md")] if os.path.exists(path) else []

        if not files:
            logger.warning(f"[{collection_name}] 无 .md 文档，跳过")
            continue

        logger.info(
            f"[{collection_name}] 开始处理 | {len(files)}篇文档: {files}"
        )

        t0 = time.time()
        loader = DirectoryLoader(
            path, glob="*.md", loader_cls=TextLoader,
            loader_kwargs={"encoding": "utf-8"},
        )
        docs = loader.load()
        chunks = splitter.split_documents(docs)
        logger.info(
            f"[{collection_name}] {len(docs)}篇→{len(chunks)}片段 | "
            f"{time.time()-t0:.1f}s"
        )

        t1 = time.time()
        store = Chroma.from_documents(
            documents=chunks, embedding=emb,
            collection_name=collection_name,
            persist_directory=CHROMA,
        )
        with _lock:
            _stores[collection_name] = store
        total_chunks += len(chunks)
        logger.info(
            f"[{collection_name}] 向量化完成 | "
            f"{len(chunks)}片段 | {time.time()-t1:.1f}s"
        )

    logger.info(
        f"知识库构建完成 | {len(COLLECTIONS)}集合 | "
        f"共{total_chunks}个向量片段 | 存储: {CHROMA}"
    )
    logger.info("=" * 40)

# ============================================================
# 检索
# ============================================================

def _get_store(collection: str) -> Chroma:
    """获取指定集合的向量存储（懒加载）"""
    if collection not in _stores:
        with _lock:
            if collection not in _stores:
                logger.debug(f"懒加载集合: {collection}")
                t0 = time.time()
                _stores[collection] = Chroma(
                    collection_name=collection,
                    persist_directory=CHROMA,
                    embedding_function=_get_embeddings(),
                )
                logger.debug(f"集合{collection}加载完成 | {time.time()-t0:.1f}s")
    return _stores[collection]


def search(
    query: str,
    collection: str = "knowledge",
    top_k: int = 3,
) -> List[Dict[str, str]]:
    """搜索指定集合，语义搜索 + 关键词回退"""
    logger.info(
        f"知识库搜索 | collection={collection} | "
        f"top_k={top_k} | query='{query[:60]}...'"
    )

    t0 = time.time()
    results = []

    # ---- 1. Chroma 语义搜索 ----
    try:
        store = _get_store(collection)
        docs = store.similarity_search(query, k=top_k)
        results = [
            {
                "content": d.page_content,
                "source": d.metadata.get("source", ""),
                "collection": collection,
            }
            for d in docs
        ]
        logger.info(
            f"语义搜索: {len(results)}条 | {time.time()-t0:.2f}s"
        )
    except Exception as e:
        logger.warning(f"语义搜索异常: {e}")

    # ---- 2. 关键词匹配（始终执行，与语义结果合并去重） ----
    kw_results = _keyword_search(query, collection, top_k)
    if kw_results:
        # 去重：按 source 去重，关键词结果优先放在前面
        seen = {r["source"] for r in results}
        for kr in kw_results:
            if kr["source"] not in seen:
                results.insert(0, kr)  # 精确匹配放最前面
                seen.add(kr["source"])
        logger.info(f"合并关键词结果: +{len(kw_results)}条, 去重后共{len(results)}条")

    # ---- 3. SQLite 兜底 ----
    if len(results) == 0:
        results = _sqlite_fallback(query, collection, top_k)

    logger.info(
        f"搜索完成 | 最终{len(results)}条 | {time.time()-t0:.2f}s"
    )
    return results


def _keyword_search(query: str, collection: str, top_k: int) -> List[Dict[str, str]]:
    """关键词匹配：扫描 data/{collection}/ 下 .md 文件，找包含 query 的"""
    import glob as _glob
    data_dir = os.path.join(DATA, collection)
    if not os.path.exists(data_dir):
        return []

    keywords = query.strip().split()
    results = []
    for fpath in _glob.glob(os.path.join(data_dir, "*.md")):
        try:
            with open(fpath, "r", encoding="utf-8") as f:
                content = f.read()
            # 任一关键词命中即可
            if any(kw in content for kw in keywords):
                results.append({
                    "content": content[:1000],
                    "source": fpath,
                    "collection": collection,
                })
        except Exception:
            continue

    logger.info(f"关键词回退: {len(results)}条匹配")
    return results[:top_k]


def _sqlite_fallback(query: str, collection: str, top_k: int) -> List[Dict[str, str]]:
    """SQLite 兜底：直接查 data/ai_data.db"""
    sqlite_path = os.path.join(DATA, "ai_data.db")
    if not os.path.exists(sqlite_path):
        return []

    import sqlite3
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
        conn = sqlite3.connect(sqlite_path)
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
        logger.info(f"SQLite兜底: {len(results)}条匹配")
        return results[:top_k]
    except Exception as e:
        logger.warning(f"SQLite兜底失败: {e}")
        return []


def search_all(query: str, top_k: int = 2) -> List[Dict[str, str]]:
    """跨所有集合搜索"""
    logger.info(f"search_all | query='{query[:60]}...' | top_k={top_k}")
    results = []
    for col in COLLECTIONS.values():
        try:
            results.extend(search(query, col, top_k))
        except Exception as e:
            logger.warning(f"search_all: 集合{col}搜索跳过 | {e}")

    # 按内容长度粗排（实际应按 similarity score）
    results.sort(key=lambda r: len(r["content"]), reverse=True)
    merged = results[:top_k * 2]
    logger.info(
        f"search_all 完成 | 候选{len(results)}条 → 合并后{len(merged)}条"
    )
    return merged
