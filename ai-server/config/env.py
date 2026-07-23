"""
环境配置

加载 .env 文件，导出配置字典。
模块加载时即验证关键配置项。
"""

import os
from dotenv import load_dotenv
from utils.logger import get_logger

logger = get_logger(__name__)
logger.debug("模块加载: config.env")

# 加载 .env
_loaded = load_dotenv()
if _loaded:
    logger.info(f".env 加载成功")
else:
    logger.warning(".env 文件未找到，将使用默认值")

# 关键配置验证
_api_key = os.getenv("DEEPSEEK_API_KEY", "")
_base_url = os.getenv("DEEPSEEK_BASE_URL", "https://api.deepseek.com")
_model = os.getenv("DEEPSEEK_MODEL", "deepseek-chat")
_temp = float(os.getenv("LLM_TEMPERATURE", "0.4"))
_max_tok = int(os.getenv("LLM_MAX_TOKENS", "2048"))

if _api_key:
    logger.info(f"LLM 配置: model={_model}, url={_base_url}, temp={_temp}, max_tokens={_max_tok}")
    logger.info(f"DeepSeek API Key: {_api_key[:8]}...{_api_key[-4:]}")
else:
    logger.error("DEEPSEEK_API_KEY 未设置！所有 LLM 调用将失败！")

# RAG 配置
_rag_chunk = int(os.getenv("RAG_CHUNK_SIZE", "500"))
_rag_overlap = int(os.getenv("RAG_CHUNK_OVERLAP", "50"))
_rag_topk = int(os.getenv("RAG_TOP_K", "3"))
logger.info(f"RAG 配置: chunk_size={_rag_chunk}, overlap={_rag_overlap}, top_k={_rag_topk}")


# MySQL（主后端数据库）
_mysql_host = os.getenv("MYSQL_HOST", "localhost")
_mysql_port = int(os.getenv("MYSQL_PORT", "3306"))
_mysql_db = os.getenv("MYSQL_DATABASE", "code1st")
_mysql_user = os.getenv("MYSQL_USER", "root")
_mysql_pwd = os.getenv("MYSQL_PASSWORD", "Ajpenpb120180614")
logger.info(f"MySQL 配置: {_mysql_user}@{_mysql_host}:{_mysql_port}/{_mysql_db}")


def get_config() -> dict:
    """返回应用配置字典"""
    return {
        "llm": {
            "model": _model,
            "base_url": _base_url,
            "api_key": _api_key,
            "temperature": _temp,
            "max_tokens": _max_tok,
        },
        "rag": {
            "chunk_size": _rag_chunk,
            "chunk_overlap": _rag_overlap,
            "top_k": _rag_topk,
        },
        "mysql": {
            "host": _mysql_host,
            "port": _mysql_port,
            "database": _mysql_db,
            "user": _mysql_user,
            "password": _mysql_pwd,
        },
    }
