"""
Agent 工具集 — 5 个 LangChain Tool

Agent 按用户意图自主选择调用哪个工具。
"""

from langchain_core.tools import tool

from core.tool.knowledge_base import search, search_all
from utils.logger import get_logger

logger = get_logger(__name__)
logger.debug("模块加载: core.tool.agent_tools")


# ============================================================
# 格式化
# ============================================================

def _fmt(results: list) -> str:
    """将检索结果格式化为 LLM 可读文本"""
    if not results:
        return ""
    parts = [f"【以下内容必须据此回答用户，这是平台真实知识库数据，找到 {len(results)} 条】"]
    for i, r in enumerate(results, 1):
        src = r["source"].replace("\\", "/").split("/")[-1] if r.get("source") else "?"
        col = r.get("collection", "?")
        parts.append(
            f"---\n[资料{i}] 来源: {src} | 类型: {col}\n{r['content']}"
        )
    return "\n\n---\n\n".join(parts)


# ============================================================
# 工具定义
# ============================================================

@tool
def search_knowledge(query: str) -> str:
    """搜索非遗知识文章。当用户询问非遗项目的历史、工艺、特点、文化内涵时调用。
    例如: 紫砂壶的历史、蜀绣的针法特点"""
    logger.info(f"工具调用: search_knowledge('{query[:60]}...')")
    r = search(query, "knowledge", top_k=3)
    if r:
        logger.info(f"search_knowledge: {len(r)}条结果")
        return _fmt(r)
    logger.info(f"search_knowledge: 无结果")
    return "❌ 知识库中确实未找到相关信息。请告知用户平台暂未收录此内容。"


@tool
def search_inheritors(query: str) -> str:
    """搜索非遗传承人信息。当用户询问传承人、老师、匠人、大师时调用。
    例如: 苏州有哪些刺绣传承人、谁教紫砂壶制作"""
    logger.info(f"工具调用: search_inheritors('{query[:60]}...')")
    r = search(query, "inheritors", top_k=3)
    if r:
        logger.info(f"search_inheritors: {len(r)}条结果")
        return _fmt(r)
    logger.info(f"search_inheritors: 无结果")
    return "❌ 传承人库中确实未找到此人的信息。请告知用户平台暂未收录该传承人。"


@tool
def search_projects(query: str) -> str:
    """搜索非遗项目或活动。当用户询问体验活动、课程、展览、工坊时调用。
    例如: 最近有什么陶艺体验活动、哪里可以学剪纸"""
    logger.info(f"工具调用: search_projects('{query[:60]}...')")
    r = search(query, "projects", top_k=3)
    if r:
        logger.info(f"search_projects: {len(r)}条结果")
        return _fmt(r)
    logger.info(f"search_projects: 无结果")
    return "❌ 项目库中确实未找到匹配的活动。请告知用户平台暂未收录该项目。"


@tool
def search_products(query: str) -> str:
    """搜索非遗文创商品。当用户询问购买、商品、礼物、文创产品时调用。
    例如: 适合送长辈的非遗礼物、有什么紫砂壶可以买"""
    logger.info(f"工具调用: search_products('{query[:60]}...')")
    r = search(query, "products", top_k=3)
    if r:
        logger.info(f"search_products: {len(r)}条结果")
        return _fmt(r)
    logger.info(f"search_products: 无结果")
    return "❌ 商品库中确实未找到匹配的商品。请告知用户平台暂未收录该商品。"


@tool
def recommend_content(preference: str = "") -> str:
    """推荐非遗内容。当用户说"推荐"、"有什么建议"、"帮我找找"等模糊意图时调用。
    可选参数 preference 可指定偏好类别。
    例如: 推荐一些适合新手的非遗、帮我推荐刺绣相关的内容"""
    logger.info(
        f"工具调用: recommend_content(preference='{preference[:60] if preference else '(空)'}')"
    )
    if preference:
        r = search_all(preference, top_k=2)
    else:
        r = search_all("", top_k=1)

    if r:
        logger.info(f"recommend_content: {len(r)}条推荐")
        return "为您推荐以下非遗内容：\n\n" + _fmt(r)
    logger.info(f"recommend_content: 无推荐内容")
    return "❌ 推荐引擎确实无内容可推荐。请告知用户平台内容还在建设中。"


# ============================================================
# 注册表
# ============================================================

ALL_TOOLS = [
    search_knowledge,
    search_inheritors,
    search_projects,
    search_products,
    recommend_content,
]

logger.info(f"Agent 工具集加载完成 | {len(ALL_TOOLS)}个工具: {[t.name for t in ALL_TOOLS]}")
