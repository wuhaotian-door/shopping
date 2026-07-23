"""
DeepSeek LLM 服务类

三个核心方法:
  complete   — 接收完整消息列表 → 返回完整响应（核心入口）
  simple_chat — 单轮问答，不携带复杂上下文
  generate   — 流式生成，逐 token yield
"""

import time
from typing import Iterator, List

from langchain_core.messages import BaseMessage, HumanMessage, SystemMessage
from langchain_openai import ChatOpenAI
from utils.logger import get_logger

logger = get_logger(__name__)
logger.debug("模块加载: core.llm.services.deepseek_service")


class DeepSeekService:
    """DeepSeek LLM 服务"""

    def __init__(self, llm: ChatOpenAI):
        self.llm = llm
        logger.debug(f"DeepSeekService.__init__ | model={llm.model_name}")

    # ============================================================
    # complete — 核心入口
    # ============================================================

    def complete(self, messages: List[BaseMessage]) -> str:
        """完整调用：输入消息列表，返回完整响应"""
        msg_count = len(messages)
        total_chars = sum(
            len(m.content) if hasattr(m, 'content') and m.content else 0
            for m in messages
        )
        roles = [m.type for m in messages]

        logger.info(
            f"complete 开始 | {msg_count} 条消息 | {total_chars} 字符 | "
            f"角色分布: {dict((r, roles.count(r)) for r in set(roles))}"
        )
        logger.debug(f"complete 首条消息预览: {str(messages[0])[:200]}...")

        t0 = time.time()
        try:
            response = self.llm.invoke(messages)
            elapsed = time.time() - t0
            resp_len = len(response.content) if response.content else 0

            # 检查 token 用量
            usage = ""
            if hasattr(response, 'response_metadata'):
                meta = response.response_metadata
                token_usage = meta.get('token_usage', {})
                if token_usage:
                    usage = (
                        f" | input_tokens={token_usage.get('prompt_tokens', '?')}"
                        f" output_tokens={token_usage.get('completion_tokens', '?')}"
                        f" total={token_usage.get('total_tokens', '?')}"
                    )

            logger.info(
                f"complete 成功 | {resp_len} 字符 | {elapsed:.2f}s{usage}"
            )
            logger.debug(f"complete 响应前100字: {response.content[:100]}...")
            return response.content

        except Exception as e:
            elapsed = time.time() - t0
            logger.error(
                f"complete 失败 | {elapsed:.2f}s | {type(e).__name__}: {e}",
                exc_info=True,
            )
            raise

    # ============================================================
    # simple_chat — 轻量对话
    # ============================================================

    def simple_chat(self, question: str, system_prompt: str = "") -> str:
        """单轮轻量对话"""
        logger.info(
            f"simple_chat 开始 | 问题长度={len(question)} | "
            f"提示词长度={len(system_prompt)} | "
            f"问题预览: {question[:80]}..."
        )

        messages = []
        if system_prompt:
            messages.append(SystemMessage(content=system_prompt))
        messages.append(HumanMessage(content=question))

        t0 = time.time()
        try:
            response = self.llm.invoke(messages)
            elapsed = time.time() - t0
            logger.info(
                f"simple_chat 成功 | {len(response.content)} 字符 | {elapsed:.2f}s"
            )
            return response.content
        except Exception as e:
            elapsed = time.time() - t0
            logger.error(
                f"simple_chat 失败 | {elapsed:.2f}s | {type(e).__name__}: {e}",
                exc_info=True,
            )
            raise

    # ============================================================
    # generate — 流式
    # ============================================================

    def generate(self, messages: List[BaseMessage]) -> Iterator[str]:
        """流式生成，逐 token yield"""
        total_chars = sum(
            len(m.content) if hasattr(m, 'content') and m.content else 0
            for m in messages
        )
        logger.info(
            f"generate 开始 | {len(messages)} 条消息 | {total_chars} 字符"
        )

        t0 = time.time()
        token_count = 0
        first_token_time = None

        try:
            for chunk in self.llm.stream(messages):
                if chunk.content:
                    token_count += 1
                    if first_token_time is None:
                        first_token_time = time.time() - t0
                    yield chunk.content

            elapsed = time.time() - t0
            ttft = first_token_time or 0
            logger.info(
                f"generate 成功 | {token_count} tokens | 总耗时{elapsed:.2f}s | "
                f"首token延迟{ttft:.2f}s | 平均{elapsed/token_count*1000:.0f}ms/token"
            )

        except Exception as e:
            elapsed = time.time() - t0
            logger.error(
                f"generate 失败 | 已生成{token_count}tokens | "
                f"{elapsed:.2f}s | {type(e).__name__}: {e}",
                exc_info=True,
            )
            raise

    # ============================================================
    # bind_tools — LLM 工具绑定
    # ============================================================

    def bind_tools(self, tools):
        """绑定工具到 LLM"""
        tool_names = [getattr(t, 'name', '?') for t in tools]
        logger.debug(f"bind_tools: {len(tools)} 工具 → {tool_names}")
        return self.llm.bind_tools(tools)

    @property
    def raw(self) -> ChatOpenAI:
        """获取底层 ChatOpenAI（供 LangGraph 使用）"""
        return self.llm
