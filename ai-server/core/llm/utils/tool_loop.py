"""
工具调用循环 (executeToolLoop)

LLM 循环：推理 → 判断是否需要工具 → 调用工具 → 接收结果 → 继续推理
"""

import time
from typing import Any, Dict, List

from langchain_core.messages import BaseMessage, ToolMessage
from utils.logger import get_logger

logger = get_logger(__name__)
logger.debug("模块加载: core.llm.utils.tool_loop")

MAX_ROUNDS = 5


def execute_tool_loop(
    llm_with_tools,
    messages: List[BaseMessage],
    tools_by_name: Dict[str, Any],
    max_rounds: int = MAX_ROUNDS,
) -> Dict[str, Any]:
    """工具调用循环

    Returns:
        {"answer": str, "tool_calls": [{"tool": ..., "args": ..., "result": ...}]}
    """
    tool_log = []
    tool_names = list(tools_by_name.keys())

    logger.info(
        f"工具循环开始 | 最多{max_rounds}轮 | "
        f"初始消息{len(messages)}条 | 可用工具: {tool_names}"
    )

    for rnd in range(max_rounds):
        t0 = time.time()
        response = llm_with_tools.invoke(messages)
        messages.append(response)
        llm_time = time.time() - t0

        if response.tool_calls:
            logger.info(
                f"第{rnd+1}/{max_rounds}轮 | LLM决策耗时{llm_time:.2f}s | "
                f"请求调用{len(response.tool_calls)}个工具"
            )

            for i, tc in enumerate(response.tool_calls):
                tool_name = tc["name"]
                tool_args = tc["args"]
                fn = tools_by_name.get(tool_name)

                logger.info(
                    f"  工具[{i+1}] {tool_name} | 参数: "
                    f"{str(tool_args)[:100]}..."
                )

                if not fn:
                    logger.error(f"  工具[{i+1}] {tool_name} → 未注册!")
                    messages.append(ToolMessage(
                        content=f"未知工具: {tool_name}",
                        tool_call_id=tc["id"],
                    ))
                    continue

                t_tool = time.time()
                try:
                    result = str(fn.invoke(tool_args))
                    tool_time = time.time() - t_tool
                    logger.info(
                        f"  工具[{i+1}] {tool_name} 成功 | "
                        f"返回{len(result)}字符 | 耗时{tool_time:.2f}s"
                    )
                except Exception as e:
                    tool_time = time.time() - t_tool
                    result = f"工具执行出错: {type(e).__name__}: {e}"
                    logger.error(
                        f"  工具[{i+1}] {tool_name} 失败 | "
                        f"耗时{tool_time:.2f}s | {type(e).__name__}: {e}",
                        exc_info=True,
                    )

                # 输出裁剪
                result_trimmed = result[:500]
                if len(result) > 500:
                    logger.debug(f"  工具[{i+1}] 输出已裁剪: {len(result)}→500字符")
                    result_trimmed += f"\n...(共{len(result)}字符，已裁剪)"

                tool_log.append({
                    "tool": tool_name,
                    "args": tool_args,
                    "result": result_trimmed,
                })

                messages.append(ToolMessage(
                    content=result_trimmed,
                    tool_call_id=tc["id"],
                ))
        else:
            logger.info(
                f"第{rnd+1}/{max_rounds}轮 | LLM直接输出答案 | "
                f"耗时{llm_time:.2f}s | 答案长度={len(response.content or '')}"
            )
            break
    else:
        logger.warning(
            f"工具循环达到最大轮次{max_rounds}，强制结束 | "
            f"累计{len(tool_log)}次工具调用"
        )

    logger.info(
        f"工具循环结束 | {len(tool_log)}次工具调用 | "
        f"最终答案{len(messages[-1].content or '')}字符"
    )
    return {"answer": messages[-1].content, "tool_calls": tool_log}
