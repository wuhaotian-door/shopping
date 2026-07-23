"""
日志系统 — 同时输出到控制台和文件

级别: DEBUG < INFO < WARNING < ERROR
设置环境变量 LOG_LEVEL=DEBUG 可开启调试日志
日志文件: logs/app.log
"""

import logging
import os
import sys
from datetime import datetime

# 日志目录
LOG_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "logs")
os.makedirs(LOG_DIR, exist_ok=True)

# 日志级别
_LEVEL_NAME = os.getenv("LOG_LEVEL", "INFO").upper()
_LEVEL = getattr(logging, _LEVEL_NAME, logging.INFO)

# 根 logger 配置
_ROOT_LOGGER = logging.getLogger("ai-server")
_ROOT_LOGGER.setLevel(_LEVEL)

if not _ROOT_LOGGER.handlers:
    # 控制台 handler
    ch = logging.StreamHandler(sys.stdout)
    ch.setLevel(_LEVEL)
    ch.setFormatter(logging.Formatter(
        "[%(asctime)s] [%(levelname)-5s] %(name)s | %(message)s",
        datefmt="%H:%M:%S",
    ))
    _ROOT_LOGGER.addHandler(ch)

    # 文件 handler
    _LOG_FILE = os.path.join(LOG_DIR, f"app-{datetime.now().strftime('%Y%m%d')}.log")
    fh = logging.FileHandler(_LOG_FILE, encoding="utf-8")
    fh.setLevel(logging.DEBUG)  # 文件始终记录 DEBUG
    fh.setFormatter(logging.Formatter(
        "[%(asctime)s] [%(levelname)-5s] %(name)s | %(message)s",
        datefmt="%Y-%m-%d %H:%M:%S",
    ))
    _ROOT_LOGGER.addHandler(fh)
    _ROOT_LOGGER.info(f"日志系统初始化: 级别={_LEVEL_NAME}, 文件={_LOG_FILE}")


def get_logger(name: str) -> logging.Logger:
    """获取模块 logger，继承根 logger 配置"""
    return _ROOT_LOGGER.getChild(name)
