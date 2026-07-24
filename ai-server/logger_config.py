"""
集中式日志配置模块

功能:
  - 双重输出: 控制台（带颜色级别标记）+ 文件（按日期轮转）
  - 统一格式: 时间 | 级别 | 模块:函数:行号 | 消息
  - 请求 ID 追踪: 通过上下文变量注入

使用方式:
  from logger_config import get_logger
  logger = get_logger(__name__)
  logger.info("xxx")
  logger.debug("xxx")
  logger.warning("xxx")
  logger.error("xxx", exc_info=True)
"""

import logging
import os
import sys
from logging.handlers import RotatingFileHandler
from contextvars import ContextVar

# ============================================================
# 请求 ID 上下文 — 每个请求一个唯一 ID，自动注入日志
# ============================================================
_request_id_var: ContextVar[str] = ContextVar("request_id", default="")

LOG_DIR = os.path.join(os.path.dirname(__file__), "logs")

# ============================================================
# 日志格式
# ============================================================
# 控制台格式 — 简洁，带颜色标记
CONSOLE_FORMAT = (
    "\033[90m%(asctime)s\033[0m "      # 灰色时间
    "| %(levelname)-7s "
    "| \033[36m%(name)-20s\033[0m "    # 青色模块名
    "| \033[2m%(funcName)s:%(lineno)d\033[0m "  # 暗色函数:行号
    "| %(req_id)s"                     # 请求 ID
    "%(message)s"
)

# 文件格式 — 完整，不含颜色码
FILE_FORMAT = (
    "%(asctime)s"
    "| %(levelname)-7s "
    "| %(name)-20s "
    "| %(funcName)s:%(lineno)d"
    "| %(req_id)s"
    "%(message)s"
)


class _RequestIdFilter(logging.Filter):
    """日志过滤器: 自动注入当前请求 ID"""
    def filter(self, record):
        rid = _request_id_var.get()
        record.req_id = f"[{rid}] " if rid else ""
        return True


# ============================================================
# 初始化（模块导入时自动执行一次）
# ============================================================

_initialized = False


def setup_logging(
    level: int = logging.DEBUG,
    log_to_file: bool = True,
    max_bytes: int = 5 * 1024 * 1024,  # 5MB 单文件上限
    backup_count: int = 5,              # 保留 5 个历史文件
) -> None:
    """初始化日志系统（幂等，多次调用不重复注册）

    Args:
        level: 日志级别，默认 DEBUG（开发环境）
        log_to_file: 是否写文件
        max_bytes: 单个日志文件最大字节数
        backup_count: 保留的备份文件数
    """
    global _initialized
    if _initialized:
        return

    root_logger = logging.getLogger()
    root_logger.setLevel(level)

    _request_id_filter = _RequestIdFilter()

    # ---- 控制台 Handler ----
    console_handler = logging.StreamHandler(sys.stdout)
    console_handler.setLevel(level)
    console_handler.setFormatter(logging.Formatter(
        CONSOLE_FORMAT,
        datefmt="%H:%M:%S",
    ))
    console_handler.addFilter(_request_id_filter)
    root_logger.addHandler(console_handler)

    # ---- 文件 Handler（RotatingFileHandler 自动轮转） ----
    if log_to_file:
        os.makedirs(LOG_DIR, exist_ok=True)
        log_path = os.path.join(LOG_DIR, "app.log")
        file_handler = RotatingFileHandler(
            log_path,
            maxBytes=max_bytes,
            backupCount=backup_count,
            encoding="utf-8",
        )
        file_handler.setLevel(logging.DEBUG)  # 文件始终记录 DEBUG 及以上
        file_handler.setFormatter(logging.Formatter(
            FILE_FORMAT,
            datefmt="%Y-%m-%d %H:%M:%S",
        ))
        file_handler.addFilter(_request_id_filter)
        root_logger.addHandler(file_handler)

    # 降低第三方库的日志噪音
    for noisy in ["httpx", "httpcore", "openai", "urllib3", "chromadb", "sentence_transformers"]:
        logging.getLogger(noisy).setLevel(logging.WARNING)

    _initialized = True


def get_logger(name: str) -> logging.Logger:
    """获取 logger 实例

    用法: logger = get_logger(__name__)
    """
    if not _initialized:
        setup_logging()
    return logging.getLogger(name)


def set_request_id(rid: str) -> None:
    """设置当前请求的 request_id"""
    _request_id_var.set(rid)


def get_request_id() -> str:
    """获取当前请求的 request_id"""
    return _request_id_var.get()
