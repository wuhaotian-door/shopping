"""
Level 4 Production — 把 AI 服务推向生产环境所需的横切关注点

三个模块:

① LLM 缓存 (LangChain SQLiteCache)
  相同问题命中缓存后直接返回，不调用 API。节省 90%+ 的重复请求费用。
  原理: LangChain 全局 set_llm_cache() → 所有 ChatOpenAI 实例自动生效
  命中逻辑: prompt + model 完全相同时命中缓存

② Token 追踪 (Callback)
  每次 LLM 调用后自动记录输入/输出 token 数，前端可展示用量。
  原理: LangChain BaseCallbackHandler.on_llm_end() 钩子
  累计的 token 消耗通过 get_usage() 查询

③ 频率限制 (FastAPI Middleware)
  基于 IP 的简易限流器，防止滥用。
  原理: 每个请求检查 IP 最近 N 秒内的请求次数，超标返回 429

使用方式:
  from production import setup
  setup(app)  # 在 main.py 中调用一次
"""

import time
from collections import defaultdict
from typing import Any, Dict, List, Optional, Tuple

from langchain_core.callbacks import BaseCallbackHandler
from langchain_core.outputs import LLMResult
from langchain_openai import ChatOpenAI

# ============================================================
# ① LLM 缓存 — SQLite 持久化，重启不丢失
# ============================================================

_cache_stats: Dict[str, int] = {"hits": 0, "misses": 0}


def setup_cache(db_path: str = ".cache.db") -> None:
    """启用 LLM 缓存

    放在链调用之前执行，全局生效——所有 ChatOpenAI 实例自动使用缓存。

    工作方式:
      第一次问"紫砂壶是什么？" → 调 API, 存入缓存
      第二次问"紫砂壶是什么？" → 从 SQLite 读取, 不调 API  ← 省钱

    缓存键 = (prompt 文本 + model 名) 的哈希
    所以同样的问题在同样的 model 下才会命中。
    """
    from langchain_community.cache import SQLiteCache
    from langchain_core.globals import set_llm_cache

    import os as _os
    cache_path = _os.path.join(_os.path.dirname(__file__), db_path)
    set_llm_cache(SQLiteCache(database_path=cache_path))
    print(f"[Production] LLM 缓存已启用: {cache_path}")


def get_cache_stats() -> dict:
    """返回缓存命中/未命中统计"""
    from langchain_core.globals import get_llm_cache
    cache = get_llm_cache()
    return {
        ** _cache_stats,
        "enabled": cache is not None,
    }


# ============================================================
# ② Token 追踪 — 每次 LLM 调用自动记录消耗
# ============================================================

_token_records: List[Dict[str, Any]] = []  # 最近 200 条调用记录


class TokenTracker(BaseCallbackHandler):
    """每次 LLM 调用结束自动触发 on_llm_end"""

    def on_llm_end(self, response: LLMResult, **kwargs) -> None:
        usage = _extract_usage(response)
        if not usage:
            return

        record = {
            "time": time.strftime("%H:%M:%S"),
            "model": usage.get("model", "unknown"),
            "input_tokens": usage.get("input_tokens", 0),
            "output_tokens": usage.get("output_tokens", 0),
            "total_tokens": usage.get("total_tokens", 0),
        }
        _token_records.append(record)

        # 只保留最近 200 条
        if len(_token_records) > 200:
            _token_records.pop(0)


def _extract_usage(response: LLMResult) -> Optional[dict]:
    """从 LLMResult 提取 token 用量

    不同模型返回的字段不同:
      - OpenAI/DeepSeek: response.llm_output["token_usage"]
      - 也可能在 generations[i].generation_info 里
    """
    if not response:
        return None

    # 路径 1: llm_output.token_usage (OpenAI/DeepSeek 通常在这里)
    llm_output = response.llm_output or {}
    usage = llm_output.get("token_usage") or llm_output.get("usage")
    if usage:
        return {
            "input_tokens": usage.get("prompt_tokens", 0) or usage.get("input_tokens", 0),
            "output_tokens": usage.get("completion_tokens", 0) or usage.get("output_tokens", 0),
            "total_tokens": usage.get("total_tokens", 0),
        }

    # 路径 2: generations[0].generation_info
    for gen_list in response.generations:
        for gen in gen_list:
            info = getattr(gen, "generation_info", None) or {}
            usage = info.get("token_usage") or info.get("usage")
            if usage:
                return {
                    "input_tokens": usage.get("prompt_tokens", 0) or usage.get("input_tokens", 0),
                    "output_tokens": usage.get("completion_tokens", 0) or usage.get("output_tokens", 0),
                    "total_tokens": usage.get("total_tokens", 0),
                }

    return None


def get_usage() -> dict:
    """返回累计用量和最近调用记录

    前端可以调用这个来展示:
      - 当前会话消耗了多少 token
      - 估算花了多少钱
    """
    total_in = sum(r["input_tokens"] for r in _token_records)
    total_out = sum(r["output_tokens"] for r in _token_records)

    # DeepSeek 价格 (RMB): 输入 1元/百万token, 输出 2元/百万token
    cost_in = total_in / 1_000_000 * 1.0
    cost_out = total_out / 1_000_000 * 2.0

    return {
        "total_input_tokens": total_in,
        "total_output_tokens": total_out,
        "total_tokens": total_in + total_out,
        "estimated_cost_rmb": round(cost_in + cost_out, 6),
        "recent_calls": _token_records[-10:],  # 最近 10 条
        "call_count": len(_token_records),
    }


# ============================================================
# ③ 频率限制 — 基于 IP 的简易限流器
# ============================================================

class RateLimiter:
    """简易内存限流器: 每个 IP 在时间窗口内最多允许 N 次请求

    不是生产级方案（生产用 Redis + 滑动窗口），但展示了原理。
    API 足够简单，后续换成 Redis 实现不需要改接口。
    """

    def __init__(self, max_requests: int = 30, window_seconds: int = 60):
        """
        Args:
            max_requests: 时间窗口内最大请求数
            window_seconds: 时间窗口（秒）
        """
        self.max_requests = max_requests
        self.window_seconds = window_seconds
        self._storage: Dict[str, List[float]] = defaultdict(list)
        # IP → [timestamp1, timestamp2, ...]

    def is_allowed(self, ip: str) -> Tuple[bool, int]:
        """检查 IP 是否允许访问

        Returns:
            (allowed, remaining)
        """
        now = time.time()
        cutoff = now - self.window_seconds

        # 清理过期记录
        self._storage[ip] = [t for t in self._storage[ip] if t > cutoff]

        count = len(self._storage[ip])
        if count >= self.max_requests:
            return False, 0

        self._storage[ip].append(now)
        return True, self.max_requests - count - 1


# 默认: 60 秒内最多 30 次请求
_default_limiter = RateLimiter(max_requests=30, window_seconds=60)


def create_rate_limit_middleware(limiter: RateLimiter = None):
    """创建 FastAPI 限流中间件

    用法:
        app.add_middleware(BaseHTTPMiddleware, dispatch=create_rate_limit_middleware())
    """
    if limiter is None:
        limiter = _default_limiter

    async def dispatch(request, call_next):
        from fastapi.responses import JSONResponse

        # 仅对 /ai/ 路径限流
        if request.url.path.startswith("/ai/"):
            ip = request.client.host if request.client else "unknown"
            allowed, remaining = limiter.is_allowed(ip)
            if not allowed:
                return JSONResponse(
                    status_code=429,
                    content={"detail": "请求太频繁，请稍后再试", "code": 0},
                )

        response = await call_next(request)
        return response

    return dispatch


# ============================================================
# ④ 初始化入口 — main.py 调用一次即可
# ============================================================

# ============================================================
# ⑤ Model 工厂 — 创建带追踪的 ChatOpenAI
# ============================================================

_tracker = TokenTracker()


def create_model(**kwargs) -> ChatOpenAI:
    """创建 ChatOpenAI，自动附带 token 跟踪

    替代直接 ChatOpenAI(...)，加上 tracking:
      from production import create_model
      model = create_model(model="deepseek-chat", ...)

    TokenTracker 是单例，所有 model 共享一个追踪器。
    """
    callbacks = list(kwargs.pop("callbacks", [])) if "callbacks" in kwargs else []
    callbacks.append(_tracker)
    return ChatOpenAI(callbacks=callbacks, **kwargs)


def setup(cache: bool = True) -> None:
    """启用生产级功能（缓存）

    在 main.py 的 app 创建后调用:
        from production import setup
        setup()

    token 追踪通过 create_model() 工厂函数启用，
    不需要在这里单独配置。
    """
    if cache:
        setup_cache()

    print("[Production] 生产级功能初始化完成")
